package me.timschneeberger.onyxtweaks.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.topjohnwu.superuser.Shell
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_SETTINGS_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getApplicationInfoCompat

object ContextExtensions {
    fun Context.killPackage(pkgName: String) {
        toast(getString(R.string.toast_killing_process, pkgName))
        killPackageSilently(pkgName)
    }

    fun Context.restartLauncher() {
        toast(R.string.toast_launcher_restarting)
        killPackageSilently(LAUNCHER_PACKAGE)
    }

    fun Context.restartSettings() {
        toast(R.string.toast_system_settings_restarting)
        killPackageSilently(SYSTEM_SETTINGS_PACKAGE)
    }

    fun Context.restartSystemUi() {
        toast(R.string.toast_system_ui_restarting)
        killPackageSilently(SYSTEM_UI_PACKAGE)
    }

    fun Context.restartPackage(pkgName: String) {
        toast(R.string.toast_generic_component_restarting)
        killPackageSilently(pkgName)
    }

    fun Context.restartZygote() {
        toast(R.string.toast_zygote_restarting)
        runAsRoot("kill $(pidof zygote)", "kill $(pidof zygote64)")
    }

    private fun Context.runAsRoot(vararg commands: String) {
        Shell.getShell().let { shell ->
            if (!shell.isRoot) {
                showAlert(
                    getString(R.string.error_no_root_access),
                    getString(R.string.error_no_root_access_message),
                )
                return
            }

            shell.newJob().add(*commands).exec()
        }
    }

    private fun Context.killPackageSilently(pkgName: String) = runAsRoot("killall $pkgName")

    fun Context.toast(@StringRes message: Int, long: Boolean = true) = Toast.makeText(this, getString(message),
        if(long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    fun Context.toast(message: String, long: Boolean = true) = Toast.makeText(this, message,
        if(long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    fun Context.getAppName(): String = applicationInfo.loadLabel(packageManager).toString()

    fun Context.getAppName(packageName: String): CharSequence? {
        return try {
            packageManager.getApplicationInfoCompat(packageName)
        } catch (_: Exception) {
            null
        }?.let {
            packageManager.getApplicationLabel(it)
        }
    }
}