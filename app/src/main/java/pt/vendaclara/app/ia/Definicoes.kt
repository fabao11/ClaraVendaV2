package pt.vendaclara.app.ia

import android.content.Context
import pt.vendaclara.app.BuildConfig

/** Definições guardadas apenas neste telemóvel. Nunca contêm dados da Vinted. */
class Definicoes private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("definicoes", Context.MODE_PRIVATE)

    var urlServidor: String
        get() = prefs.getString("url", null) ?: BuildConfig.SERVIDOR_URL
        set(v) = prefs.edit().putString("url", v.trim()).apply()

    var codigoAcesso: String
        get() = prefs.getString("codigo", "") ?: ""
        set(v) = prefs.edit().putString("codigo", v.trim()).apply()

    var consentimentoIA: Boolean
        get() = prefs.getBoolean("consentimento_ia", false)
        set(v) = prefs.edit().putBoolean("consentimento_ia", v).apply()

    fun configurado(): Boolean =
        urlServidor.trim().startsWith("https://") && codigoAcesso.isNotBlank()

    companion object {
        fun obter(context: Context) = Definicoes(context.applicationContext)
    }
}
