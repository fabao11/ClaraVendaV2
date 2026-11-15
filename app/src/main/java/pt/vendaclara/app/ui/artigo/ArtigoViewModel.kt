package pt.vendaclara.app.ui.artigo

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.vendaclara.app.dados.FotoEntity
import pt.vendaclara.app.dados.Repositorio
import pt.vendaclara.app.ia.Campos
import pt.vendaclara.app.ia.Conversor
import pt.vendaclara.app.ia.Definicoes
import pt.vendaclara.app.ia.Detalhe
import pt.vendaclara.app.ia.ErroIA
import pt.vendaclara.app.ia.EstadoIA
import pt.vendaclara.app.ia.RespostaIA
import pt.vendaclara.app.ia.ServicoIA

@OptIn(ExperimentalCoroutinesApi::class)
class ArtigoViewModel(
    private val repo: Repositorio,
    private val handle: SavedStateHandle,
    private val def: Definicoes,
    private val ia: ServicoIA
) : ViewModel() {

    private val mutex = Mutex()
    private val idInicial: Long = handle.get<Long>("id") ?: 0L
    private var acaoPendente = "analisar"

    /** 0 = artigo novo, ainda não guardado. */
    val idAtual: StateFlow<Long> =
        handle.getStateFlow("id", 0L).stateIn(viewModelScope, SharingStarted.Eagerly, idInicial)

    val fotos: StateFlow<List<FotoEntity>> =
        idAtual.flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else repo.fotos(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var titulo by mutableStateOf(""); private set
    var descricao by mutableStateOf(""); private set
    var detalhes by mutableStateOf<Map<String, Detalhe>>(emptyMap()); private set
    var perguntas by mutableStateOf<List<String>>(emptyList()); private set
    var estadoIA by mutableStateOf<EstadoIA>(EstadoIA.Parado); private set
    var pedirConsentimento by mutableStateOf(false); private set
    var aCarregar by mutableStateOf(idInicial != 0L); private set
    var erro by mutableStateOf<String?>(null); private set
    var apagado by mutableStateOf(false); private set

    init {
        if (idInicial != 0L) {
            viewModelScope.launch {
                try {
                    repo.obterArtigo(idInicial)?.let {
                        titulo = it.titulo
                        descricao = it.descricao
                        detalhes = Conversor.detalhesDeJson(it.detalhes)
                        perguntas = Conversor.perguntasDeJson(it.perguntas)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    erro = "Não foi possível abrir o artigo."
                }
                aCarregar = false
            }
        }
    }

    private suspend fun idParaGuardar(criar: Boolean): Long? {
        val atual = handle.get<Long>("id") ?: 0L
        if (atual != 0L) return atual
        if (!criar) return null
        val novo = repo.criarArtigo(titulo)
        handle["id"] = novo
        return novo
    }

    /** Guarda sempre o estado mais recente (lido já dentro do bloqueio). */
    private fun persistir() {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val temConteudo = titulo.isNotBlank() || descricao.isNotBlank() || detalhes.isNotEmpty()
                    val id = idParaGuardar(criar = temConteudo) ?: return@withLock
                    repo.guardarConteudo(
                        id, titulo, descricao,
                        Conversor.detalhesParaJson(detalhes),
                        Conversor.perguntasParaJson(perguntas)
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    erro = "Não foi possível guardar o rascunho. Tenta novamente."
                }
            }
        }
    }

    fun alterarTitulo(novo: String) { titulo = novo; persistir() }
    fun alterarDescricao(novo: String) { descricao = novo; persistir() }

    /** Editar um campo conta como confirmação do utilizador. */
    fun alterarDetalhe(chave: String, valor: String) {
        detalhes = detalhes + (chave to Detalhe(valor, confirmado = valor.isNotBlank()))
        persistir()
    }

    fun alternarConfirmacao(chave: String) {
        val d = detalhes[chave] ?: return
        if (d.valor.isBlank()) return
        detalhes = detalhes + (chave to d.copy(confirmado = !d.confirmado))
        persistir()
    }

    fun adicionarFotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val ok = mutex.withLock {
                try {
                    val id = idParaGuardar(criar = true) ?: return@withLock false
                    repo.adicionarFotos(id, uris)
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    erro = "Não foi possível adicionar as fotografias. Tenta novamente."
                    false
                }
            }
            if (ok) gerarAutomaticamenteSePossivel()
        }
    }

    private fun gerarAutomaticamenteSePossivel() {
        if (!def.configurado()) return
        if (titulo.isNotBlank() || descricao.isNotBlank()) return
        executarIA("analisar")
    }

    fun removerFoto(foto: FotoEntity) {
        viewModelScope.launch {
            try {
                repo.removerFoto(foto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                erro = "Não foi possível remover a fotografia."
            }
        }
    }

    // ---- IA ----
    fun gerarAnuncio() = executarIA("analisar")
    fun atualizarAnuncio() = executarIA("anuncio")
    fun regenerarTitulo() = executarIA("titulo")
    fun regenerarDescricao() = executarIA("descricao")

    fun confirmarConsentimento() {
        def.consentimentoIA = true
        pedirConsentimento = false
        executarIA(acaoPendente)
    }

    fun recusarConsentimento() {
        pedirConsentimento = false
    }

    private fun executarIA(acao: String) {
        if (estadoIA == EstadoIA.ACarregar) return
        if (!def.configurado()) {
            estadoIA = EstadoIA.Erro("A ligação à IA ainda não está configurada. Abre as Definições.")
            return
        }
        if (!def.consentimentoIA) {
            acaoPendente = acao
            pedirConsentimento = true
            return
        }
        estadoIA = EstadoIA.ACarregar
        viewModelScope.launch {
            try {
                val caminhos = if (acao == "analisar") {
                    val id = handle.get<Long>("id") ?: 0L
                    if (id == 0L) emptyList() else repo.caminhosFotos(id).take(8)
                } else emptyList()
                if (acao == "analisar" && caminhos.isEmpty()) throw ErroIA("Adiciona pelo menos uma fotografia.")
                val r = ia.pedir(acao, detalhes, caminhos, titulo)
                aplicar(acao, r)
                estadoIA = EstadoIA.Parado
                persistir()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErroIA) {
                estadoIA = EstadoIA.Erro(e.message ?: "Ocorreu um erro. Tenta novamente.")
            } catch (e: Exception) {
                estadoIA = EstadoIA.Erro("Ocorreu um erro inesperado. Tenta novamente.")
            }
        }
    }

    private fun aplicar(acao: String, r: RespostaIA) {
        if (acao == "analisar") {
            val novos = detalhes.toMutableMap()
            Campos.chaves.forEach { k ->
                // O que o utilizador já confirmou nunca é substituído pela IA.
                if (novos[k]?.confirmado != true) {
                    novos[k] = Detalhe(r.detalhes?.get(k).orEmpty(), confirmado = false)
                }
            }
            detalhes = novos
            perguntas = r.perguntas.orEmpty()
        }
        r.titulo?.let { titulo = it }
        r.descricao?.let { descricao = it }
    }

    fun apagarArtigo() {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val id = handle.get<Long>("id") ?: 0L
                    if (id != 0L) repo.apagarArtigo(id)
                    apagado = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    erro = "Não foi possível apagar o artigo."
                }
            }
        }
    }

    fun limparErro() { erro = null }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val def = Definicoes.obter(app)
                ArtigoViewModel(Repositorio.obter(app), createSavedStateHandle(), def, ServicoIA(def))
            }
        }
    }
}
