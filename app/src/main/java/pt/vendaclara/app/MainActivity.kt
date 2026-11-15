package pt.vendaclara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pt.vendaclara.app.ui.VendaClaraApp
import pt.vendaclara.app.ui.VendaClaraTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VendaClaraTema {
                VendaClaraApp()
            }
        }
    }
}
