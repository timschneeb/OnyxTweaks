package me.timschneeberger.onyxtweaks.ui.model

import android.content.Context
import com.onyx.android.sdk.api.device.epd.UpdateMode
import kotlinx.serialization.Serializable
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.getAppName

@Serializable
data class ActivityRule(
    val packageName: String,
    val appName: String,
    val activityClass: String?,
    val activityName: String?,
    val updateMethod: UpdateMode = UpdateMode.None
) {
    companion object {
        fun fromActivityInfo(context: Context, activity: ActivityInfo): ActivityRule {
            return ActivityRule(
                activity.packageName,
                (context.getAppName(activity.packageName)
                    ?: activity.packageName.split('.').lastOrNull()
                    ?: activity.packageName).toString(),
                activity.activityClass,
                activity.activityName
            )
        }

        fun fromApp(packageName: String, appName: String): ActivityRule {
            return ActivityRule(
                packageName,
                appName,
                null,
                null
            )
        }
    }
}