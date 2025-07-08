package app.revanced.manager.plugin.downloader.play.store.data

import com.aurora.gplayapi.data.models.PlayResponse
import com.aurora.gplayapi.network.IHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import java.io.OutputStream

object Http : IHttpClient {
    val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            socketTimeoutMillis = 10000
        }
    }

    private val _responseCode = MutableStateFlow(100)
    override val responseCode = _responseCode.asStateFlow()

    suspend inline fun download(
        outputStream: OutputStream,
        block: HttpRequestBuilder.() -> Unit
    ) = client.prepareGet(block).execute {
        it.bodyAsChannel().copyTo(outputStream)
    }

    private fun playRequest(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        builder: suspend HttpRequestBuilder.() -> Unit = {}
    ) = runBlocking {
        val response = client.request(url) {
            this.method = method
            this.headers {
                headers.forEach { (name, value) ->
                    append(name, value)
                }
            }
            builder()
        }

        _responseCode.apply {
            // State flows will not emit the same value twice
            value = 0
            value = response.status.value
        }

        val success = response.status.value in 200..299
        PlayResponse(
            code = response.status.value,
            isSuccessful = success,
            responseBytes = response.body(),
            errorString = if (!success) response.status.description else ""
        )
    }

    override fun get(url: String, headers: Map<String, String>) =
        playRequest(HttpMethod.Get, url, headers)

    override fun get(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ) = playRequest(HttpMethod.Get, url, headers) {
        params.forEach { (name, value) ->
            parameter(name, value)
        }
    }

    override fun get(url: String, headers: Map<String, String>, paramString: String) = playRequest(
        HttpMethod.Get, url + paramString, headers
    )

    override fun getAuth(url: String) = PlayResponse(
        isSuccessful = false,
        code = 444
    )

    override fun post(url: String, headers: Map<String, String>, body: ByteArray) = playRequest(
        HttpMethod.Post, url, headers
    ) {
        setBody(body)
    }

    override fun post(
        url: String,
        headers: Map<String, String>,
        params: Map<String, String>
    ) = playRequest(HttpMethod.Post, url, headers) {
        params.forEach { (name, value) ->
            parameter(name, value)
        }
    }

    override fun postAuth(url: String, body: ByteArray) = getAuth(url)
}