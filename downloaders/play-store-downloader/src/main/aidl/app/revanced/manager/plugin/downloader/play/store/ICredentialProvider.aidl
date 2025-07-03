// ICredentialProvider.aidl
package app.revanced.manager.plugin.downloader.play.store;

import app.revanced.manager.plugin.downloader.play.store.data.Credentials;
import app.revanced.manager.plugin.downloader.play.store.data.ParcelProperties;

interface ICredentialProvider {
    @nullable Credentials retrieveCredentials();
    ParcelProperties getProperties();
}