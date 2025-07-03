package app.revanced.manager.plugin.downloader.play.store.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.plugin.downloader.play.store.LOG_TAG
import app.revanced.manager.plugin.downloader.play.store.data.Credentials
import app.revanced.manager.plugin.downloader.play.store.data.Http
import app.revanced.manager.plugin.downloader.play.store.data.PropertiesProvider
import app.revanced.manager.plugin.downloader.play.store.data.saveCredentials
import com.aurora.gplayapi.helpers.AuthValidator
import com.kevinnzou.web.AccompanistWebViewClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.StringTokenizer

class AuthActivityViewModel(app: Application) : AndroidViewModel(app) {
    private val cookieManager = CookieManager.getInstance()!!
    private val activityResultCode =
        CompletableDeferred<Pair<Int, Intent?>>(parent = viewModelScope.coroutineContext.job)

    val webViewClient = object : AccompanistWebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)

            val cookieString = cookieManager.getCookie(url) ?: return
            val cookies = buildMap {
                putAll(cookiePattern.findAll(cookieString).map {
                    val (key, value) = it.destructured
                    key to value
                })
            }
            cookies[OAUTH_COOKIE]?.let { token ->
                view.evaluateJavascript(EXTRACT_EMAIL_JS) {
                    val email = it.replace("\"".toRegex(), "")
                    getAndSaveAasToken(email, token)
                }
            }
        }
    }

    init {
        cookieManager.removeAllCookies(null)
    }

    suspend fun awaitActivityResultCode() = activityResultCode.await()
    private fun finishActivity(code: Int, intent: Intent? = null) =
        activityResultCode.complete(code to intent)

    private fun getAndSaveAasToken(email: String, oauthToken: String) = viewModelScope.launch {
        try {
            val response = getAC2DMResponse(email, oauthToken)
            val aasToken = response["Token"] ?: throw Exception("AC2DM did not return a token")
            val credentials = Credentials(email, aasToken)
            val context = getApplication<Application>()
            val deviceProps = PropertiesProvider.createDeviceProperties(context)

            withContext(Dispatchers.IO) {
                val authData = credentials.toAuthData(deviceProps)
                val validator = AuthValidator(authData).using(Http)

                if (!validator.isValid()) throw Exception("Credential validation failed")
                context.saveCredentials(credentials)
            }

            finishActivity(Activity.RESULT_OK)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not get and save token", e)
            finishActivity(AuthActivity.RESULT_FAILED, Intent().apply {
                putExtra(AuthActivity.FAILURE_MESSAGE_KEY, e.message)
            })
        }
    }

    private suspend fun getAC2DMResponse(email: String, oauthToken: String): Map<String, String> {
        val locale = Locale.getDefault()
        val formData = mapOf(
            "lang" to locale.toString().replace("_", "-"),
            "google_play_services_version" to PLAY_SERVICES_VERSION_CODE,
            "sdk_version" to BUILD_VERSION_SDK,
            "device_country" to locale.country.lowercase(Locale.US),
            "Email" to email,
            "service" to "ac2dm",
            "get_accountid" to 1,
            "ACCESS_TOKEN" to 1,
            "callerPkg" to GMS_PACKAGE_NAME,
            "add_account" to 1,
            "Token" to oauthToken,
            "callerSig" to CALLER_SIGNATURE
        ).entries.joinToString(separator = "&") { (key, value) -> "$key=$value" }

        val response = Http.client.post(TOKEN_AUTH_URL) {
            headers {
                set("app", GMS_PACKAGE_NAME)
                set(HttpHeaders.UserAgent, "")
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
            }

            setBody(formData)
        }

        val body = response.bodyAsText()
        return buildMap {
            val st = StringTokenizer(body, "\n\r")
            while (st.hasMoreTokens()) {
                val (key, value) = st.nextToken().split("=", limit = 2)
                put(key, value)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView(webView: WebView) = webView.apply {
        cookieManager.acceptThirdPartyCookies(this)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        settings.apply {
            safeBrowsingEnabled = false
            allowContentAccess = true
            databaseEnabled = true
            domStorageEnabled = true
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
    }


    companion object Constants {
        private const val OAUTH_COOKIE = "oauth_token"
        private const val EXTRACT_EMAIL_JS =
            "(function() { return document.querySelector('[data-profile-identifier]').innerText; })();"
        const val EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup"
        private const val TOKEN_AUTH_URL = "https://android.clients.google.com/auth"
        private const val BUILD_VERSION_SDK = 28
        private const val PLAY_SERVICES_VERSION_CODE = 19629032
        private const val GMS_PACKAGE_NAME = "com.google.android.gms"
        private const val CALLER_SIGNATURE = "38918a453d07199354f8b19af05ec6562ced5788"
        private val cookiePattern = "([^=]+)=([^;]*);?\\s?".toRegex()
    }
}