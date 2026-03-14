package app.revanced.manager.downloaders.play.store.data

import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

fun saveData(output: OutputStream, credentials: Credentials, properties: Properties) {
    val str = JSONObject().apply {
        put("email", credentials.email)
        put("aasToken", credentials.aasToken)

        val props = JSONObject()
        properties.forEach { (key, value) ->
            props.put(key as String, value as String)
        }
        put("properties", props)
    }.toString()
    output.writer().use {
        it.write(str)
    }
}

fun loadData(input: InputStream): Pair<Credentials, Properties> = input.reader().use {
    val obj = JSONObject(it.readText())

    val creds = Credentials(
        obj.getString("email"),
        obj.getString("aasToken")
    )
    val props = Properties().apply {
        val jsonProps = obj.getJSONObject("properties")
        jsonProps.keys().forEach { key ->
            put(key, jsonProps.getString(key))
        }
    }
    creds to props
}