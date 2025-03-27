package me.timschneeberger.onyxtweaks.ui.model

import android.content.Context
import kotlinx.serialization.Serializable
import me.timschneeberger.onyxtweaks.mods.global.PerActivityRefreshModes
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.getAppName

@Serializable
data class ActivityRule(
    val packageName: String,
    val appName: String,
    val activityClass: String?,
    val activityName: String?,
    val updateMode: PerActivityRefreshModes.UpdateOption,
) {
    companion object {
        fun fromActivityInfo(context: Context, activity: ActivityInfo): ActivityRule {
            return ActivityRule(
                activity.packageName,
                (context.getAppName(activity.packageName)
                    ?: activity.packageName.split('.').lastOrNull()
                    ?: activity.packageName).toString(),
                activity.activityClass,
                activity.activityName,
                PerActivityRefreshModes.UpdateOption.DEFAULT
            )
        }

        fun fromApp(packageName: String, appName: String): ActivityRule {
            return ActivityRule(
                packageName,
                appName,
                null,
                null,
                PerActivityRefreshModes.UpdateOption.DEFAULT
            )
        }
    }
}