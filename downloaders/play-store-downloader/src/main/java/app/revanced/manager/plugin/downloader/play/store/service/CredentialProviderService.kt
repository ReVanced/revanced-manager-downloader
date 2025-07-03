package app.revanced.manager.plugin.downloader.play.store.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import app.revanced.manager.plugin.downloader.play.store.ICredentialProvider
import app.revanced.manager.plugin.downloader.play.store.data.ParcelProperties
import app.revanced.manager.plugin.downloader.play.store.data.PropertiesProvider
import app.revanced.manager.plugin.downloader.play.store.data.readSavedCredentials

class CredentialProviderService : Service() {
    private val binder = object : ICredentialProvider.Stub() {
        override fun retrieveCredentials() = try {
            readSavedCredentials()
        } catch (e: Exception) {
            Log.e("CredentialService", "Got exception while retrieving credentials", e)
            throw IllegalStateException("An exception was raised when reading credentials")
        }

        override fun getProperties() =
            ParcelProperties(PropertiesProvider.createDeviceProperties(this@CredentialProviderService))
    }

    override fun onBind(intent: Intent): IBinder = binder.asBinder()
}