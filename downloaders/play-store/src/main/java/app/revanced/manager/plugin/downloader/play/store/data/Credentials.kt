package app.revanced.manager.plugin.downloader.play.store.data

import android.content.Context
import android.os.Parcelable
import com.aurora.gplayapi.helpers.AuthHelper
import kotlinx.parcelize.Parcelize
import java.util.Properties

@Parcelize
data class Credentials(val email: String, val aasToken: String) : Parcelable {
    fun toAuthData(deviceProperties: Properties) =
        AuthHelper.using(Http)
            .build(email, aasToken, AuthHelper.Token.AAS, properties = deviceProperties)
}

private fun Context.credentialsSharedPrefs() =
    getSharedPreferences("credentials", Context.MODE_PRIVATE)

fun Context.saveCredentials(credentials: Credentials) {
    with(credentialsSharedPrefs().edit()) {
        putString(EMAIL_KEY, credentials.email)
        putString(AAS_TOKEN_KEY, credentials.aasToken)
        apply()
    }
}

fun Context.readSavedCredentials() = with(credentialsSharedPrefs()) {
    val email = getString(EMAIL_KEY, null) ?: return@with null
    val aasToken = getString(AAS_TOKEN_KEY, null) ?: return@with null

    Credentials(email, aasToken)
}

private const val EMAIL_KEY = "email"
private const val AAS_TOKEN_KEY = "aas_token"