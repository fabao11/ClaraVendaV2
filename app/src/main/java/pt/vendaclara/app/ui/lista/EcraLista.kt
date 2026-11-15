package pt.vendaclara.app.ui.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pt.vendaclara.app.dados.EstadoArtigo
import pt.vendaclara.app.dados.ItemLista
import pt.vendaclara.app.dados.Repositorio
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaViewModel(repo: Repositorio) : ViewModel() {
    /** null = ainda a carregar */
    val artigos: StateFlow<List<ItemLista>?> =
        repo.artigos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { ListaViewModel(Repositorio.obter(this[APPLICATION_KEY]!!)) }
        }
    }
}

@Composable
fun EcraLista(
    onNovoArtigo: () -> Unit,
    onAbrirArtigo: (Long) -> Unit,
    onDefinicoes: () -> Unit,
    vm: ListaViewModel = viewModel(factory = ListaViewModel.Factory)
) {
    val artigos by vm.artigos.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovoArtigo,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Novo artigo") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        val lista = artigos
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Cabecalho(lista, onDefinicoes) }
            when {
                lista == null -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                lista.isEmpty() -> item { EstadoVazio() }
                else -> items(lista, key = { it.artigo.id }) { item ->
                    CartaoArtigo(item, onClick = { onAbrirArtigo(item.artigo.id) })
                }
            }
        }
    }
}

@Composable
private fun Cabecalho(lista: List<ItemLista>?, onDefinicoes: () -> Unit) {
    val gradiente = Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF134E4A)))
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(gradiente).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Venda Clara",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDefinicoes) {
                    Icon(Icons.Default.Settings, contentDescription = "Definições", tint = Color.White)
                }
            }
            Text(
                "Tira as fotografias e a IA prepara o anúncio. Tu revês, editas e copias.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            if (!lista.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstatisticaPilula(lista.count { it.artigo.estado == EstadoArtigo.POR_PUBLICAR.name }, "por publicar")
                    EstatisticaPilula(lista.count { it.artigo.estado == EstadoArtigo.ANUNCIADO.name }, "anunciados")
                    EstatisticaPilula(lista.count { it.artigo.estado == EstadoArtigo.VENDIDO.name }, "vendidos")
                }
            }
        }
    }
}

@Composable
private fun EstatisticaPilula(n: Int, rotulo: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.16f)) {
        Text(
            "$n $rotulo",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EstadoVazio() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🛍️", fontSize = 56.sp)
        Text("Ainda não tens artigos", style = MaterialTheme.typography.titleLarge)
        Text(
            "Toca em «Novo artigo», adiciona fotografias e deixa a IA preparar o anúncio.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CartaoArtigo(item: ItemLista, onClick: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val estado = EstadoArtigo.entries.firstOrNull { it.name == item.artigo.estado }
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cores.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
            if (item.primeiraFoto != null) {
                AsyncImage(
                    model = File(item.primeiraFoto),
                    contentDescription = "Fotografia do artigo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(cores.primaryContainer), contentAlignment = Alignment.Center) {
                    Text("📷", fontSize = 40.sp)
                }
            }
            Pilula(
                texto = estado?.rotulo ?: item.artigo.estado,
                cor = when (estado) {
                    EstadoArtigo.ANUNCIADO -> cores.primaryContainer
                    EstadoArtigo.VENDIDO -> cores.secondaryContainer
                    else -> cores.tertiaryContainer
                },
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
            )
            val n = item.numeroFotos
            Pilula(
                texto = if (n == 1) "1 fotografia" else "$n fotografias",
                cor = cores.surfaceContainerLowest,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.artigo.titulo.ifBlank { "Sem título" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            val data = SimpleDateFormat("d MMM yyyy", Locale("pt", "PT")).format(Date(item.artigo.atualizadoEm))
            val pronto = item.artigo.titulo.isNotBlank() && item.artigo.descricao.isNotBlank()
            Text(
                text = if (pronto) "✨ Anúncio preparado · $data" else "Rascunho · $data",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Pilula(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = cor, modifier = modifier) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
