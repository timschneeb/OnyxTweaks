package me.timschneeberger.onyxtweaks.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.onyx.optimization.SystemMMKV
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kyuubiran.ezxhelper.Log
import com.onyx.internal.mmkv.MMKV
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.utils.renderToLog
import me.timschneeberger.onyxtweaks.ui.fragments.ConfigListFragment
import me.timschneeberger.onyxtweaks.ui.services.MMKVAccessService
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.putString
import me.timschneeberger.onyxtweaks.utils.ellipsize
import java.io.File
import kotlin.reflect.KClass

class ConfigEditorActivity : BasePreferenceActivity() {
    override val rootTitleRes: Int = R.string.mmkv_editor
    override val rootSubtitleRes: Int = R.string.mmkv_editor_summary
    override val rootIsSubActivity: Boolean = true

    override fun createRootFragment() = ConfigListFragment()

    private var aidlConn: AIDLConnection? = null
    var mmkvService: IMMKVAccessService? = null
        private set

    val textEditorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val tmpFile = result.data?.getStringExtra(TextEditorActivity.EXTRA_TARGET_FILE)
            val key = result.data?.getStringExtra(TextEditorActivity.EXTRA_KEY)
            val handle = result.data?.getStringExtra(TextEditorActivity.EXTRA_HANDLE)

            if (result.resultCode == RESULT_OK && tmpFile != null && key != null) {
                File(tmpFile).run {
                    readText().let { text ->
                        try {
                            mmkvService?.putString(handle, key, text)
                            mmkvService?.sync(handle)
                            Log.d("Wrote value to $handle: $key = ${text.ellipsize(100)}")
                        } catch (e: RemoteException) {
                            Log.e("Remote service threw an exception", e)
                            toast("Failed to write value")
                        }
                    }
                }
            }

            // Remove temporary file
            if (tmpFile != null) {
                File(tmpFile).run {
                    if(exists()) delete()
                }
            }
        }

    override fun onResume() {
        // Launch service if not already running
        if (aidlConn == null) {
            val intent = Intent(this, MMKVAccessService::class.java)
            val task = RootService.bindOrTask(intent, Shell.EXECUTOR, AIDLConnection())
            if(task != null) {
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(5)
                    .build()
                    .execTask(task)
            }
        }

        super.onResume()
    }

    inner class AIDLConnection() : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("Service connected")
            aidlConn = this

            val service = IMMKVAccessService.Stub.asInterface(service)
            mmkvService = service
            try {
                val handle = service.open(LAUNCHER_PACKAGE, "KCB")
                if(handle == null) {
                    Log.e("Failed to initialize MMKV")
                    return
                }

                // service.findDataStoresForPackage(LAUNCHER_PACKAGE).renderToLog("DS")

                //service.allKeys(handle).renderToLog("Test")

            } catch (e: RemoteException) {
                Log.e("Remote service threw an exception", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("Service connection closed")
            aidlConn = null
        }
    }
    override fun onDestroy() {
        if (aidlConn != null) {
            RootService.stop(Intent(this, MMKVAccessService::class.java))
        }
        super.onDestroy()
    }

    fun test() {
        try {
            SystemMMKV.defaultMMKV()
        }
        catch (e: Exception) {
            Log.e("Failed to check SystemMMKV availability", e)
        }

        var knownTypes = mapOf<String, KClass<*>>(
            // OnyxSystemConfig
            "eac_system_version" to Int::class,
            "appFreezeAdditionalList" to Set::class,
            "appFreezeWhiteList" to Set::class,
            "bootCompletedBlackSet" to Set::class,
            "detectedTopPackageSet" to Set::class,
            "dpiNeedFullRelaunchPkgSet" to Set::class,
            "grantPermissionUriAuthorityPackages" to Set::class,
            "grantPermissionUriAuthority" to Set::class,
            "hiddenAPIThirdPartyAppWhitelist" to Set::class,
            "ignoreCastDevicesPackages" to Set::class,
            "maintainPrevPkgRefreshPkgSet" to Set::class,
            "presetEACWhiteList" to Set::class,
            "restrictedInstallPackageSet" to Set::class,
            "showForegroundNotificationPackages" to Set::class,
            "supportForceStopAppSet" to Set::class,
            "supportMultiInstencePackages" to Set::class,

            // KeyboardDeviceConfig
            "keyboard_device_version" to Int::class,
            "keyboard_default_device_version" to Int::class,

            // EACAppConfig
            "eac_default_app_config" to String::class, // JSON EACAppConfig object

            // EACDeviceConfig
            "eac_app_pkg_set" to Set::class, // String set of package names
            "eac_device_json_version" to Int::class,

            // EACDeviceExtraConfig
            "allow_child_mode_apps" to Set::class, // String set of package names
            "allowUseRegalModePkgSet" to Set::class, // String set of package names
            "customOOMAdjPkgSet" to Set::class, // String set of package names
            "eac_extra_config_enable" to Boolean::class,
            "eac_extra_gc_after_scrolling" to Boolean::class,
            "eac_extra_gc_after_scrolling_delay_time" to Int::class,
            "eac_extra_refresh_mode" to Int::class,
            "eac_extra_turbo" to Int::class,
            "gc_after_scrolling_refresh_mode" to Int::class,
            "pre_grant_premission_pkg" to Set::class, // String set of package names

            // NotificationConfig
            "notification_version" to Int::class,
            "notification_pkg_black_list" to Set::class, // String set of package names
            "notification_pkg_white_list" to Set::class, // String set of package names

            // GesturesConfig (SystemUI)
            "gesture_config" to String::class, // JSON GesturesConfig object

            // From the keyboard app?
            "forceComputeImeBoundsSystemComponents" to Set::class // String set of pkg/component names
        )

        var knownTypesForPrefixes = mapOf<String, KClass<*>>(
            // KeyboardMapping
            "keymapping_" to String::class, // JSON KeyboardMapping object for a specific device name

            // EACAppConfig
            "eac_app_" to Set::class,  // JSON EACAppConfig object for a specific package
        )

        Log.i("Checking MMKV keys and types")

        SystemMMKV.defaultMMKV().run {
            val keys = this.allKeys()
            keys.renderToLog()

            keys.forEach { key ->
                var type = knownTypes[key]
                    ?: knownTypesForPrefixes.entries.find { key.startsWith(it.key) }?.value
                    ?: guessType(key)

                Log.i("Key: $key, Type: $type")

            }
        }
    }

    fun MMKV.guessType(key: String): KClass<*>? {
        var size = getValueActualSize(key)
        return when {
            size == 0 -> null // empty; unknown type
            size <= 4 -> Int::class // warning: could also be a float or boolean
            size <= 8 -> Long::class // warning: could also be a double
            // If it's String-like with control characters, it's likely a set because each
            // string entry has a int32 length field which would may be parsed as non-printable chars
            getString(key, "<???>")?.any { ch -> ch.isISOControl() } == true -> Set::class
            // Otherwise, it's likely a string
            else -> String::class
        }
    }
}