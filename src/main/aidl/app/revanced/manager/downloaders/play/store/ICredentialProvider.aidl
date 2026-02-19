// ICredentialProvider.aidl
package app.revanced.manager.downloaders.play.store;

import app.revanced.manager.downloaders.play.store.data.Credentials;
import app.revanced.manager.downloaders.play.store.data.ParcelProperties;

interface ICredentialProvider {
    @nullable Credentials retrieveCredentials();
    ParcelProperties getProperties();
}