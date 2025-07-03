package app.revanced.manager.plugin.downloader.play.store.data

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.opengl.EGL14
import android.opengl.GLES10
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import android.os.Build
import android.os.Parcelable
import androidx.core.content.getSystemService
import kotlinx.parcelize.Parcelize
import java.util.Properties
import javax.microedition.khronos.egl.EGLDisplay

@Parcelize
data class ParcelProperties(val value: Properties) : Parcelable

object PropertiesProvider {
    fun createDeviceProperties(context: Context) = with(context) {
        Properties().apply {
            //Build Props
            setProperty("UserReadableName", "${Build.DEVICE}-default")
            setProperty("Build.HARDWARE", Build.HARDWARE)
            setProperty("Build.RADIO", Build.getRadioVersion() ?: "unknown")
            setProperty("Build.FINGERPRINT", Build.FINGERPRINT)
            setProperty("Build.BRAND", Build.BRAND)
            setProperty("Build.DEVICE", Build.DEVICE)
            setProperty("Build.VERSION.SDK_INT", "${Build.VERSION.SDK_INT}")
            setProperty("Build.VERSION.RELEASE", Build.VERSION.RELEASE)
            setProperty("Build.MODEL", Build.MODEL)
            setProperty("Build.MANUFACTURER", Build.MANUFACTURER)
            setProperty("Build.PRODUCT", Build.PRODUCT)
            setProperty("Build.ID", Build.ID)
            setProperty("Build.BOOTLOADER", Build.BOOTLOADER)

            val config = resources.configuration
            setProperty("TouchScreen", "${config.touchscreen}")
            setProperty("Keyboard", "${config.keyboard}")
            setProperty("Navigation", "${config.navigation}")
            setProperty("ScreenLayout", "${config.screenLayout and 15}")
            setProperty("HasHardKeyboard", "${config.keyboard == Configuration.KEYBOARD_QWERTY}")
            setProperty(
                "HasFiveWayNavigation",
                "${config.navigation == Configuration.NAVIGATIONHIDDEN_YES}"
            )

            //Display Metrics
            val metrics = resources.displayMetrics
            setProperty("Screen.Density", "${metrics.densityDpi}")
            setProperty("Screen.Width", "${metrics.widthPixels}")
            setProperty("Screen.Height", "${metrics.heightPixels}")


            //Supported Platforms
            setProperty("Platforms", Build.SUPPORTED_ABIS.commaSeparated())
            //Supported Features
            setProperty("Features", features.commaSeparated())
            //Shared Locales
            setProperty("Locales", locales.commaSeparated())
            //Shared Libraries
            setProperty("SharedLibraries", sharedLibraries.commaSeparated())
            //GL Extensions
            setProperty(
                "GL.Version",
                getSystemService<ActivityManager>()!!.deviceConfigurationInfo.reqGlEsVersion.toString()
            )
            setProperty(
                "GL.Extensions",
                getEglExtensions().commaSeparated()
            )

            //Google Related Props
            setProperty("Client", "android-google")
            setProperty("GSF.version", "203615037")
            setProperty("Vending.version", "82201710")
            setProperty("Vending.versionString", "22.0.17-21 [0] [PR] 332555730")

            //MISC
            setProperty("Roaming", "mobile-notroaming")
            setProperty("TimeZone", "UTC-10")

            //Telephony (USA 3650 AT&T)
            setProperty("CellOperator", "310")
            setProperty("SimOperator", "38")
        }
    }

    private fun Array<String>.commaSeparated() = joinToString(separator = ",")
    private fun Iterable<String>.commaSeparated() = joinToString(separator = ",")

    private val Context.features
        get() = packageManager.systemAvailableFeatures.map { it.name }
            .filterNot(String::isNullOrEmpty)
    private val Context.locales
        get() = assets.locales.filter(String::isNullOrEmpty).map { it.replace("-", "_") }
    private val Context.sharedLibraries
        get() = packageManager.systemSharedLibraryNames.orEmpty().filterNotNull()

    private fun getEglExtensions() = buildSet {
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        egl.eglInitialize(display, IntArray(2))
        try {
            val numConfigsContainer = IntArray(1)
            if (!egl.eglGetConfigs(display, null, 0, numConfigsContainer)) return@buildSet
            val numConfigs = numConfigsContainer[0]
            val configs = arrayOfNulls<EGLConfig>(numConfigs).apply {
                if (!egl.eglGetConfigs(
                        display,
                        this,
                        numConfigs,
                        numConfigsContainer
                    )
                ) return@buildSet
            }.requireNoNulls()

            configs.forEach {
                val resultContainer = IntArray(1)
                egl.eglGetConfigAttrib(display, it, EGL10.EGL_CONFIG_CAVEAT, resultContainer)
                if (resultContainer[0] == EGL10.EGL_SLOW_CONFIG) return@forEach

                egl.eglGetConfigAttrib(display, it, EGL10.EGL_SURFACE_TYPE, resultContainer)
                if (EGL10.EGL_PBUFFER_BIT and resultContainer[0] == 0) return@forEach

                egl.eglGetConfigAttrib(display, it, EGL10.EGL_RENDERABLE_TYPE, resultContainer)
                if (EGL14.EGL_OPENGL_ES_BIT and resultContainer[0] != 0) addEglExtensions(
                    egl,
                    display,
                    it,
                    null,
                    this
                )
                if (EGL14.EGL_OPENGL_ES2_BIT and resultContainer[0] != 0) addEglExtensions(
                    egl,
                    display,
                    it,
                    openGlEs2AttribList,
                    this
                )
            }
        } finally {
            egl.eglTerminate(display)
        }
    }

    private fun addEglExtensions(
        egl: EGL10,
        eglDisplay: EGLDisplay,
        eglConfig: EGLConfig,
        eglContextAttribList: IntArray?,
        target: MutableSet<String>
    ) {
        val eglContext =
            egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, eglContextAttribList)
        if (eglContext === EGL10.EGL_NO_CONTEXT) return

        val eglSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig, eglSurfaceAttribList)
        if (eglSurface === EGL10.EGL_NO_SURFACE) {
            egl.eglDestroyContext(eglDisplay, eglContext)
            return
        }

        egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        val extensionsString = GLES10.glGetString(GLES10.GL_EXTENSIONS)
        if (!extensionsString.isNullOrEmpty()) {
            val extensions = extensionsString.split(" ".toRegex())
            target.addAll(extensions)
        }
        egl.eglMakeCurrent(
            eglDisplay,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT
        )
        egl.eglDestroySurface(eglDisplay, eglSurface)
        egl.eglDestroyContext(eglDisplay, eglContext)
    }

    private val eglSurfaceAttribList
        get() = intArrayOf(
            EGL10.EGL_WIDTH,
            EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_HEIGHT,
            EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_NONE
        )

    private val openGlEs2AttribList
        get() = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION,
            EGL10.EGL_PIXMAP_BIT,
            EGL10.EGL_NONE
        )
}