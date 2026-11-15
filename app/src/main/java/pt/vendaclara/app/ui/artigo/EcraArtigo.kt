@file:OptIn(ExperimentalMaterial3Api::class)

package pt.vendaclara.app.ui.artigo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.vendaclara.app.dados.FotoEntity
import pt.vendaclara.app.ia.Campos
import pt.vendaclara.app.ia.Definicoes
import pt.vendaclara.app.ia.Detalhe
import pt.vendaclara.app.ia.EstadoIA
import java.io.File

@Composable
fun EcraArtigo(
    onVoltar: () -> Unit,
    onDefinicoes: () -> Unit,
    vm: ArtigoViewModel = viewModel(factory = ArtigoViewModel.Factory)
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val fotos by vm.fotos.collectAsStateWithLifecycle()
    val idAtual by vm.idAtual.collectAsStateWithLifecycle()
    var confirmarApagar by remember { mutableStateOf(false) }
    var configurado by remember { mutableStateOf(Definicoes.obter(ctx).configurado()) }
    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        configurado = Definicoes.obter(ctx).configurado()
    }

    val seletor = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> vm.adicionarFotos(uris) }

    fun copiar(texto: String, rotulo: String) {
        clipboard.setText(AnnotatedString(texto))
        scope.launch { snackbar.showSnackbar("$rotulo copiado.") }
    }

    LaunchedEffect(vm.apagado) { if (vm.apagado) onVoltar() }
    LaunchedEffect(vm.erro) {
        vm.erro?.let {
            snackbar.showSnackbar(it)
            vm.limparErro()
        }
    }

    val aCarregarIA = vm.estadoIA == EstadoIA.ACarregar
    val temAnuncio = vm.titulo.isNotBlank() || vm.descricao.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (idAtual == 0L) "Novo artigo" else "Artigo") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (idAtual != 0L) {
                        IconButton(onClick = { confirmarApagar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Apagar artigo")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (vm.aCarregar) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Fotografias (${fotos.size})", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    BotaoAdicionar {
                        seletor.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                items(fotos, key = { it.id }) { foto ->
                    MiniaturaFoto(foto) { vm.removerFoto(foto) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CartaoIA(
                        configurado = configurado,
                        temFotos = fotos.isNotEmpty(),
                        temAnuncio = temAnuncio,
                        estado = vm.estadoIA,
                        onGerar = vm::gerarAnuncio,
                        onConfigurar = onDefinicoes
                    )
                }
                if (vm.perguntas.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { CartaoPerguntas(vm.perguntas) }
                }
                if (fotos.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CartaoDetalhes(
                            detalhes = vm.detalhes,
                            aCarregar = aCarregarIA,
                            onValor = vm::alterarDetalhe,
                            onAlternar = vm::alternarConfirmacao,
                            onAtualizar = vm::atualizarAnuncio
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CartaoAnuncio(
                        titulo = vm.titulo,
                        descricao = vm.descricao,
                        aCarregar = aCarregarIA,
                        podeRegenerar = configurado && vm.detalhes.values.any { it.valor.isNotBlank() },
                        onTitulo = vm::alterarTitulo,
                        onDescricao = vm::alterarDescricao,
                        onRegenTitulo = vm::regenerarTitulo,
                        onRegenDescricao = vm::regenerarDescricao,
                        onCopiarTitulo = { copiar(vm.titulo, "Título") },
                        onCopiarDescricao = { copiar(vm.descricao, "Descrição") }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Revê sempre o texto antes de o usares. A app não publica nada na Vinted nem envia mensagens: copias e colas tu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (vm.pedirConsentimento) {
        AlertDialog(
            onDismissRequest = vm::recusarConsentimento,
            title = { Text("Enviar para a IA?") },
            text = {
                Text(
                    "Para preparar o anúncio, as fotografias (sem localização) e os detalhes que indicares são enviados, " +
                        "através do servidor da app, para um serviço externo de IA (Anthropic). " +
                        "A app não guarda as fotografias fora do teu telemóvel, mas o fornecedor de IA pode tratá-las " +
                        "de acordo com a sua política.\n\n" +
                        "A análise por IA e o alojamento do servidor podem ter custos, pagos por quem gere o servidor.\n\n" +
                        "Podes retirar este consentimento nas Definições."
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmarConsentimento) { Text("Aceitar e continuar") } },
            dismissButton = { TextButton(onClick = vm::recusarConsentimento) { Text("Agora não") } }
        )
    }

    if (confirmarApagar) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            title = { Text("Apagar artigo?") },
            text = { Text("O artigo e as respetivas fotografias serão apagados deste telemóvel. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarApagar = false
                    vm.apagarArtigo()
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun BotaoAdicionar(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Adicionar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun MiniaturaFoto(foto: FotoEntity, onRemover: () -> Unit) {
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp))) {
        AsyncImage(
            model = File(foto.caminho),
            contentDescription = "Fotografia do artigo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemover,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remover fotografia", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CartaoIA(
    configurado: Boolean,
    temFotos: Boolean,
    temAnuncio: Boolean,
    estado: EstadoIA,
    onGerar: () -> Unit,
    onConfigurar: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("✨ Anúncio com IA", style = MaterialTheme.typography.titleMedium)
            if (estado is EstadoIA.Erro) {
                Text(estado.mensagem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            when {
                !configurado -> {
                    Text(
                        "Funcionalidade ainda não configurada. É preciso ligar a app ao servidor de IA nas Definições.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onConfigurar) { Text("Abrir definições") }
                }
                estado == EstadoIA.ACarregar -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("A analisar as fotografias… pode demorar cerca de um minuto.", style = MaterialTheme.typography.bodyMedium)
                }
                !temFotos -> Text(
                    "Adiciona pelo menos uma fotografia e a IA prepara o título e a descrição.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> {
                    Button(onClick = onGerar, modifier = Modifier.fillMaxWidth()) {
                        Text(if (temAnuncio) "Gerar de novo a partir das fotografias" else "Gerar anúncio")
                    }
                    Text(
                        "As fotografias só são enviadas para a IA com o teu consentimento.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoPerguntas(perguntas: List<String>) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("A IA precisa que confirmes", style = MaterialTheme.typography.titleMedium)
            perguntas.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            Text(
                "Responde nos campos abaixo e carrega em «Atualizar anúncio».",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CartaoDetalhes(
    detalhes: Map<String, Detalhe>,
    aCarregar: Boolean,
    onValor: (String, String) -> Unit,
    onAlternar: (String) -> Unit,
    onAtualizar: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Detalhes do artigo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Confirma ou corrige. Só o que confirmares é tratado como certo no anúncio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Campos.lista.forEach { (chave, rotulo) ->
                val d = detalhes[chave] ?: Detalhe()
                OutlinedTextField(
                    value = d.valor,
                    onValueChange = { onValor(chave, it) },
                    label = { Text(rotulo) },
                    supportingText = {
                        if (d.valor.isNotBlank()) {
                            Text(if (d.confirmado) "Confirmado por ti" else "Sugestão da IA — toca em ✓ para confirmar")
                        }
                    },
                    trailingIcon = {
                        if (d.valor.isNotBlank()) {
                            IconButton(onClick = { onAlternar(chave) }) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = if (d.confirmado) "Retirar confirmação" else "Confirmar",
                                    tint = if (d.confirmado) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FilledTonalButton(onClick = onAtualizar, enabled = !aCarregar, modifier = Modifier.fillMaxWidth()) {
                Text("Atualizar anúncio com estes dados")
            }
        }
    }
}

@Composable
private fun CartaoAnuncio(
    titulo: String,
    descricao: String,
    aCarregar: Boolean,
    podeRegenerar: Boolean,
    onTitulo: (String) -> Unit,
    onDescricao: (String) -> Unit,
    onRegenTitulo: () -> Unit,
    onRegenDescricao: () -> Unit,
    onCopiarTitulo: () -> Unit,
    onCopiarDescricao: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("O teu anúncio", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = titulo,
                onValueChange = { if (it.length <= 100) onTitulo(it) },
                label = { Text("Título") },
                supportingText = { Text("${titulo.length}/100") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRegenTitulo, enabled = podeRegenerar && !aCarregar) { Text("Regenerar") }
                FilledTonalButton(onClick = onCopiarTitulo, enabled = titulo.isNotBlank()) { Text("Copiar título") }
            }
            OutlinedTextField(
                value = descricao,
                onValueChange = { if (it.length <= 2000) onDescricao(it) },
                label = { Text("Descrição") },
                supportingText = { Text("${descricao.length}/2000") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRegenDescricao, enabled = podeRegenerar && !aCarregar) { Text("Regenerar") }
                FilledTonalButton(onClick = onCopiarDescricao, enabled = descricao.isNotBlank()) { Text("Copiar descrição") }
            }
            if (aCarregar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("A trabalhar…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
