package pt.vendaclara.app.ia

import org.json.JSONArray
import org.json.JSONObject

data class Detalhe(val valor: String = "", val confirmado: Boolean = false)

object Campos {
    val lista = listOf(
        "tipo" to "Tipo de artigo",
        "cor" to "Cor",
        "marca" to "Marca",
        "tamanho" to "Tamanho",
        "material" to "Material",
        "estado" to "Estado geral",
        "defeitos" to "Defeitos visíveis",
        "etiquetas" to "Etiquetas legíveis"
    )
    val chaves = lista.map { it.first }
}

sealed interface EstadoIA {
    data object Parado : EstadoIA
    data object ACarregar : EstadoIA
    data class Erro(val mensagem: String) : EstadoIA
}

class ErroIA(mensagem: String) : Exception(mensagem)

data class RespostaIA(
    val detalhes: Map<String, String>?,
    val perguntas: List<String>?,
    val titulo: String?,
    val descricao: String?
)

object Conversor {
    fun detalhesParaJson(m: Map<String, Detalhe>): String {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k, JSONObject().put("v", v.valor).put("c", v.confirmado)) }
        return o.toString()
    }

    fun detalhesDeJson(s: String): Map<String, Detalhe> = try {
        val o = JSONObject(s)
        val r = mutableMapOf<String, Detalhe>()
        o.keys().forEach { k ->
            val d = o.optJSONObject(k)
            if (d != null) r[k] = Detalhe(d.optString("v"), d.optBoolean("c"))
        }
        r
    } catch (e: Exception) {
        emptyMap()
    }

    fun perguntasParaJson(l: List<String>): String = JSONArray(l).toString()

    fun perguntasDeJson(s: String): List<String> = try {
        val a = JSONArray(s)
        (0 until a.length()).map { a.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}
