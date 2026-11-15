package pt.vendaclara.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.vendaclara.app.ui.artigo.EcraArtigo
import pt.vendaclara.app.ui.definicoes.EcraDefinicoes
import pt.vendaclara.app.ui.lista.EcraLista

@Composable
fun VendaClaraApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "lista") {
        composable("lista") {
            EcraLista(
                onNovoArtigo = { nav.navigate("artigo/0") },
                onAbrirArtigo = { id -> nav.navigate("artigo/$id") },
                onDefinicoes = { nav.navigate("definicoes") }
            )
        }
        composable(
            route = "artigo/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            EcraArtigo(
                onVoltar = { nav.popBackStack() },
                onDefinicoes = { nav.navigate("definicoes") }
            )
        }
        composable("definicoes") {
            EcraDefinicoes(onVoltar = { nav.popBackStack() })
        }
    }
}
