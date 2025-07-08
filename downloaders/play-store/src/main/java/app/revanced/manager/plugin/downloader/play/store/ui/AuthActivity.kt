package app.revanced.manager.plugin.downloader.play.store.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {
    private val vm: AuthActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val (code, intent) = vm.awaitActivityResultCode()
                setResult(code, intent)
                finish()
            }
        }

        setContent {
            val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme) {
                Scaffold { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        val state =
                            rememberWebViewState(url = AuthActivityViewModel.EMBEDDED_SETUP_URL)

                        WebView(
                            modifier = Modifier.fillMaxSize(),
                            state = state,
                            onCreated = vm::setupWebView,
                            client = vm.webViewClient
                        )
                    }
                }
            }

        }
    }

    companion object {
        const val RESULT_FAILED = RESULT_FIRST_USER + 1
        const val FAILURE_MESSAGE_KEY = "FAIL_MESSAGE"
    }
}