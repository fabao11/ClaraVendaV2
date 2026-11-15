package pt.vendaclara.app.dados

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ItemLista(
    val artigo: ArtigoEntity,
    val primeiraFoto: String?,
    val numeroFotos: Int
)

class Repositorio private constructor(
    private val context: Context,
    private val dao: ArtigoDao
) {
    fun artigos(): Flow<List<ItemLista>> =
        combine(dao.observarArtigos(), dao.observarTodasFotos()) { artigos, fotos ->
            val porArtigo = fotos.groupBy { it.artigoId }
            artigos.map { a ->
                val lista = porArtigo[a.id].orEmpty()
                ItemLista(a, lista.firstOrNull()?.caminho, lista.size)
            }
        }

    fun fotos(artigoId: Long): Flow<List<FotoEntity>> = dao.observarFotos(artigoId)

    suspend fun obterArtigo(id: Long): ArtigoEntity? = dao.obterArtigo(id)

    suspend fun criarArtigo(titulo: String): Long {
        val agora = System.currentTimeMillis()
        return dao.inserirArtigo(ArtigoEntity(titulo = titulo, criadoEm = agora, atualizadoEm = agora))
    }

    suspend fun guardarConteudo(id: Long, titulo: String, descricao: String, detalhes: String, perguntas: String) {
        val artigo = dao.obterArtigo(id) ?: return
        dao.atualizarArtigo(
            artigo.copy(
                titulo = titulo,
                descricao = descricao,
                detalhes = detalhes,
                perguntas = perguntas,
                atualizadoEm = System.currentTimeMillis()
            )
        )
    }

    suspend fun caminhosFotos(artigoId: Long): List<String> =
        dao.obterFotos(artigoId).map { it.caminho }

    /** Copia as fotografias para o armazenamento privado da app, para não dependerem da galeria. */
    suspend fun adicionarFotos(artigoId: Long, uris: List<Uri>) = withContext(Dispatchers.IO) {
        val pasta = File(context.filesDir, "fotos").apply { mkdirs() }
        var ordem = dao.obterFotos(artigoId).size
        for (uri in uris) {
            val ficheiro = File(pasta, "${UUID.randomUUID()}.jpg")
            try {
                val entrada = context.contentResolver.openInputStream(uri) ?: continue
                entrada.use { i -> ficheiro.outputStream().use { o -> i.copyTo(o) } }
                dao.inserirFoto(FotoEntity(artigoId = artigoId, caminho = ficheiro.absolutePath, ordem = ordem++))
            } catch (e: Exception) {
                ficheiro.delete()
                throw e
            }
        }
        dao.obterArtigo(artigoId)?.let {
            dao.atualizarArtigo(it.copy(atualizadoEm = System.currentTimeMillis()))
        }
    }

    suspend fun removerFoto(foto: FotoEntity) = withContext(Dispatchers.IO) {
        File(foto.caminho).delete()
        dao.apagarFoto(foto.id)
    }

    suspend fun apagarArtigo(id: Long) = withContext(Dispatchers.IO) {
        dao.obterFotos(id).forEach { File(it.caminho).delete() }
        dao.apagarArtigo(id)
    }

    companion object {
        @Volatile
        private var instancia: Repositorio? = null

        fun obter(context: Context): Repositorio =
            instancia ?: synchronized(this) {
                instancia ?: Repositorio(
                    context.applicationContext,
                    BaseDados.obter(context).artigoDao()
                ).also { instancia = it }
            }
    }
}
