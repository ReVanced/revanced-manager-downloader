package app.revanced.manager.downloaders.play.store.data

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