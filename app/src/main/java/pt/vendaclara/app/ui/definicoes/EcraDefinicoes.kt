@file:OptIn(ExperimentalMaterial3Api::class)

package pt.vendaclara.app.ui.definicoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pt.vendaclara.app.ia.Definicoes
import pt.vendaclara.app.ia.ErroIA
import pt.vendaclara.app.ia.ServicoIA

@Composable
fun EcraDefinicoes(onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val def = remember { Definicoes.obter(ctx) }
    val ia = remember { ServicoIA(def) }
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(def.urlServidor) }
    var codigo by remember { mutableStateOf(def.codigoAcesso) }
    var consentimento by remember { mutableStateOf(def.consentimentoIA) }
    var resultado by remember { mutableStateOf<String?>(null) }
    var aTestar by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Definições") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ligação à IA", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Escreve o endereço do servidor e o código de acesso que recebeste de quem o gere. " +
                            "A chave do fornecedor de IA nunca está nesta app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; def.urlServidor = it; resultado = null },
                        label = { Text("Endereço do servidor (https://…)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it; def.codigoAcesso = it; resultado = null },
                        label = { Text("Código de acesso") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                aTestar = true
                                resultado = try {
                                    ia.testar()
                                    "Ligação estabelecida com sucesso."
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: ErroIA) {
                                    e.message
                                } catch (e: Exception) {
                                    "Ocorreu um erro inesperado."
                                }
                                aTestar = false
                            }
                        },
                        enabled = !aTestar,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (aTestar) "A testar…" else "Testar ligação") }
                    resultado?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Privacidade e custos", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Consentir o envio de fotografias e detalhes para a IA",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = consentimento,
                            onCheckedChange = { consentimento = it; def.consentimentoIA = it }
                        )
                    }
                    Text(
                        "Sem consentimento, nada é enviado: a app pergunta sempre antes da primeira utilização. " +
                            "A análise por IA e o alojamento do servidor podem ter custos, pagos por quem gere o servidor. " +
                            "As fotografias são enviadas sem localização.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "A app nunca pede dados de acesso da Vinted, nunca publica anúncios e nunca envia mensagens por ti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
