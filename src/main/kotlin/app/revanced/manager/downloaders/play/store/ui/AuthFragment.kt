package app.revanced.manager.downloaders.play.store.ui

import android.app.Activity.RESULT_FIRST_USER
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class AuthFragment : Fragment() {
    private val vm: AuthFragmentViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().also {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val (code, intent) = vm.awaitActivityResultCode()
                    it.setResult(code, intent)
                    it.finish()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = WebView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = view as WebView

        webView.webViewClient = vm.webViewClient
        vm.setupWebView(webView)
        if (savedInstanceState == null) webView.loadUrl(AuthFragmentViewModel.EMBEDDED_SETUP_URL)
    }

    companion object {
        const val RESULT_FAILED = RESULT_FIRST_USER + 1
        const val FAILURE_MESSAGE_KEY = "FAIL_MESSAGE"
        const val CREDENTIALS_KEY = "CREDENTIALS"
        const val PROPERTIES_KEY = "PROPERTIES"
    }
}