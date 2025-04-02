package me.timschneeberger.onyxtweaks.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kyuubiran.ezxhelper.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.ui.fragments.ConfigEditorFragment
import me.timschneeberger.onyxtweaks.ui.fragments.ConfigListFragment
import me.timschneeberger.onyxtweaks.ui.services.MMKVAccessService
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.putString
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.putStringSet
import me.timschneeberger.onyxtweaks.utils.ellipsize
import java.io.File

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
            val mode = result.data?.getStringExtra(TextEditorActivity.EXTRA_MODE)?.let {
                MMKVUtils.EditorMode.valueOf(it)
            }

            if (result.resultCode == RESULT_OK && tmpFile != null && key != null && mode != null) {
                File(tmpFile).run {
                    readText().let { text ->
                        try {
                            var previewString: String
                            if(mode == MMKVUtils.EditorMode.LIST) {
                                previewString = text.ellipsize(200)?.split("\n")?.joinToString() ?: "null"
                                mmkvService?.putStringSet(handle, key, text.split("\n").toList())
                            } else {
                                previewString = text.ellipsize(200) ?: "null"
                                mmkvService?.putString(handle, key, text)
                            }
                            mmkvService?.sync(handle)

                            val currentFragment = supportFragmentManager.findFragmentById(R.id.settings)
                            if(currentFragment is ConfigEditorFragment) {
                                currentFragment.refreshByKey(key, previewString)
                            }

                            Log.d("Wrote value to $handle: $key = ${text.ellipsize(100)}")
                        } catch (e: RemoteException) {
                            Log.e("Remote service threw an exception", e)
                            toast(getString(R.string.editor_save_failed))
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
}