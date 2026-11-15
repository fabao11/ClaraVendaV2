package pt.vendaclara.app.ia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Fala com o servidor da app (nunca diretamente com o fornecedor de IA).
 * A chave do fornecedor de IA existe apenas no servidor.
 */
class ServicoIA(private val def: Definicoes) {

    suspend fun testar() = withContext(Dispatchers.IO) {
        enviar(JSONObject().put("acao", "ping"))
        Unit
    }

    suspend fun pedir(
        acao: String,
        detalhes: Map<String, Detalhe>,
        fotos: List<String>,
        tituloAtual: String
    ): RespostaIA = withContext(Dispatchers.IO) {
        val det = JSONObject()
        detalhes.forEach { (k, v) ->
            det.put(k, JSONObject().put("valor", v.valor).put("confirmado", v.confirmado))
        }
        val corpo = JSONObject()
            .put("acao", acao)
            .put("tituloAtual", tituloAtual)
            .put("detalhes", det)
        if (fotos.isNotEmpty()) {
            corpo.put("imagens", JSONArray(fotos.map { prepararImagem(it) }))
        }
        val o = enviar(corpo)
        RespostaIA(
            detalhes = o.optJSONObject("detalhes")?.let { d ->
                Campos.chaves.associateWith { d.optString(it, "").trim() }
            },
            perguntas = o.optJSONArray("perguntas")?.let { a ->
                (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() }
            },
            titulo = o.optString("titulo", "").trim().takeIf { it.isNotEmpty() },
            descricao = o.optString("descricao", "").trim().takeIf { it.isNotEmpty() }
        )
    }

    private fun enviar(corpo: JSONObject): JSONObject {
        if (!def.configurado()) throw ErroIA("A ligação à IA ainda não está configurada. Abre as Definições.")
        val bytes = corpo.toString().toByteArray(Charsets.UTF_8)
        var ligacao: HttpURLConnection? = null
        try {
            ligacao = URL(def.urlServidor.trim().trimEnd('/')).openConnection() as HttpURLConnection
            ligacao.requestMethod = "POST"
            ligacao.connectTimeout = 20_000
            ligacao.readTimeout = 120_000
            ligacao.doOutput = true
            ligacao.setFixedLengthStreamingMode(bytes.size)
            ligacao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            ligacao.setRequestProperty("Authorization", "Bearer ${def.codigoAcesso}")
            ligacao.outputStream.use { it.write(bytes) }
            val codigo = ligacao.responseCode
            if (codigo !in 200..299) throw ErroIA(mensagemParaCodigo(codigo))
            val texto = ligacao.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(texto)
        } catch (e: ErroIA) {
            throw e
        } catch (e: IOException) {
            throw ErroIA("Não foi possível ligar ao servidor. Verifica a internet e o endereço nas Definições.")
        } catch (e: JSONException) {
            throw ErroIA("A resposta veio num formato inesperado. Tenta novamente.")
        } finally {
            ligacao?.disconnect()
        }
    }

    private fun mensagemParaCodigo(codigo: Int): String = when (codigo) {
        401 -> "O código de acesso não é válido. Confirma-o nas Definições."
        429 -> "Foi atingido o limite de utilização da IA. Tenta mais tarde."
        in 500..599 -> "O serviço de IA não está disponível neste momento. Tenta mais tarde."
        else -> "Não foi possível concluir o pedido (código $codigo)."
    }

    /** Reduz e reenvia em JPEG: poupa dados e remove metadados (como a localização). */
    private fun prepararImagem(caminho: String): String {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(caminho, limites)
        var amostra = 1
        while (max(limites.outWidth, limites.outHeight) / amostra > 2000) amostra *= 2
        val bmp = BitmapFactory.decodeFile(caminho, BitmapFactory.Options().apply { inSampleSize = amostra })
            ?: throw ErroIA("Não foi possível ler uma das fotografias.")
        val rodada = rodar(bmp, caminho)
        val escala = min(1f, 1280f / max(rodada.width, rodada.height))
        val final = if (escala < 1f) {
            Bitmap.createScaledBitmap(
                rodada,
                (rodada.width * escala).roundToInt(),
                (rodada.height * escala).roundToInt(),
                true
            )
        } else rodada
        val saida = ByteArrayOutputStream()
        final.compress(Bitmap.CompressFormat.JPEG, 80, saida)
        return Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP)
    }

    private fun rodar(bmp: Bitmap, caminho: String): Bitmap {
        val graus = try {
            when (ExifInterface(caminho).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: IOException) {
            0f
        }
        if (graus == 0f) return bmp
        val m = Matrix().apply { postRotate(graus) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
