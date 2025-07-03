package app.revanced.manager.plugin.utils

import com.reandroid.apk.APKLogger
import com.reandroid.apk.ApkBundle
import com.reandroid.apk.ApkModule
import com.reandroid.app.AndroidManifest
import java.io.Closeable
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Merger {
    companion object Factory {
        suspend fun merge(apkDir: Path, arscLogger: APKLogger): ApkModule {
            val closeables = mutableSetOf<Closeable>()
            try {
                // Merge splits
                val merged = withContext(Dispatchers.Default) {
                    with(ApkBundle()) {
                        setAPKLogger(arscLogger)
                        loadApkDirectory(apkDir.toFile())
                        closeables.addAll(modules)
                        mergeModules().also(closeables::add)
                    }
                }
                merged.androidManifest.apply {
                    arrayOf(
                        AndroidManifest.ID_isSplitRequired,
                        AndroidManifest.ID_extractNativeLibs
                    ).forEach {
                        applicationElement.removeAttributesWithId(it)
                        manifestElement.removeAttributesWithId(it)
                    }

                    arrayOf(
                        AndroidManifest.NAME_requiredSplitTypes,
                        AndroidManifest.NAME_splitTypes
                    ).forEach(manifestElement::removeAttributesWithName)

                    val pattern = "^com\\.android\\.(stamp|vending)\\.".toRegex()
                    applicationElement.removeElementsIf { element ->
                        if (element.name != AndroidManifest.TAG_meta_data) return@removeElementsIf false
                        val nameAttr =
                            element.getAttributes { it.nameId == AndroidManifest.ID_name }
                                .asSequence().single()

                        pattern.containsMatchIn(nameAttr.valueString)
                    }

                    refresh()
                }

                return merged
            } finally {
                closeables.forEach(Closeable::close)
            }
        }
    }
}