package me.timschneeberger.onyxtweaks.utils

import com.topjohnwu.superuser.Shell;

import java.util.Locale

class AppUtils {
    fun restart(what: String) {
        when (what.lowercase(Locale.getDefault())) {
            "systemui" -> Shell.cmd("killall com.android.systemui").exec()
            "system" -> Shell.cmd("am start -a android.intent.action.REBOOT").exec()
            "zygote", "android" -> {
                Shell.cmd("kill $(pidof zygote)").submit()
                Shell.cmd("kill $(pidof zygote64)").submit()
            }

            else -> Shell.cmd(String.format("killall %s", what)).exec()
        }
    }
}

