package me.timschneeberger.onyxtweaks.mods.systemui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.systemui.StatusBarClock.AmPmStyle.entries
import me.timschneeberger.onyxtweaks.mods.systemui.StatusBarClock.ClockPosition.entries
import me.timschneeberger.onyxtweaks.mods.utils.StringFormatter
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createBeforeHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.sendHookExceptionEvent
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

// Adapted from: https://github.com/siavash79/PixelXpert/

@SuppressLint("DiscouragedApi")
@TargetPackages(SYSTEM_UI_PACKAGE)
class StatusBarClock : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR_CLOCK

    private var clockPosition = ClockPosition.LEFT
    private var amPmStyle = AmPmStyle.GONE
    private var formatBefore = ""
    private var formatAfter = ""
    private var smallBefore = true
    private var smallAfter = true

    private var leftClockPadding = 0
    private var rightClockPadding = 0

    private var notificationIconAreaInner: ViewGroup? = null
    private var centeredIconArea: View? = null
    private var systemIconArea: LinearLayout? = null
    private var fullStatusbar: FrameLayout? = null
    private var clockView: View? = null
    private var clockViewParent: ViewGroup? = null

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        rightClockPadding = appContext.resources.getDimensionPixelSize(
            appContext.resources.getIdentifier("status_bar_clock_starting_padding", "dimen", SYSTEM_UI_PACKAGE)
        )
        leftClockPadding = appContext.resources.getDimensionPixelSize(
            appContext.resources.getIdentifier("status_bar_left_clock_end_padding", "dimen", SYSTEM_UI_PACKAGE)
        )

        findClass("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment")
            .methodFinder()
            .filterByParamTypes(View::class.java, Bundle::class.java)
            .firstByName("onViewCreated")
            .createAfterHookCatching<StatusBarClock> { param ->
                // Clock view
                clockView = getObjectField(param.thisObject, "mClockView") as View
                clockViewParent = clockView?.parent as ViewGroup

                // Status bar needed for center position calculation
                fullStatusbar = (getObjectField(param.thisObject, "mStatusBar") as View).let {
                    it.parent as FrameLayout
                }

                // Left, center and right status bar areas
                notificationIconAreaInner = getObjectField(param.thisObject, "mNotificationIconAreaInner") as ViewGroup
                systemIconArea = getObjectField(param.thisObject, "mSystemIconArea") as LinearLayout
                centeredIconArea = try {
                    (getObjectField(param.thisObject, "mCenteredIconArea") as View).parent as View
                } catch (ex: Throwable) {
                    appContext.sendHookExceptionEvent<StatusBarClock>(ex, "Failed to find centered icon area", null, isWarning = true)
                    Log.ex(ex, "Failed to find mCenteredIconArea. Falling back to parent.systemIconArea")

                    LinearLayout(appContext).apply {
                        layoutParams = LinearLayout.LayoutParams(-2, -1)
                        (systemIconArea?.parent as ViewGroup).addView(this, 2)
                    }
                }

                loadSettings()
            }

        findClass("com.android.systemui.statusbar.policy.Clock")
            .methodFinder()
            .firstByName("getSmallTime")
            .run {
                createBeforeHookCatching<StatusBarClock> { param ->
                    setObjectField(param.thisObject, "mAmPmStyle", AmPmStyle.GONE.value)
                }

                createAfterHookCatching<StatusBarClock> { param ->
                    if (param.thisObject != clockView) return@createAfterHookCatching

                    val clockText = SpannableStringBuilder.valueOf(param.result as CharSequence)
                    param.result = SpannableStringBuilder().apply {
                        append(getStyledSpan(formatBefore, smallBefore))
                        append(clockText)
                        if (amPmStyle != AmPmStyle.GONE)
                            append(getStyledSpan("\$Ga", amPmStyle == AmPmStyle.SMALL))
                        append(getStyledSpan(formatAfter, smallAfter))
                    }
                }
            }
    }

    private fun loadSettings() {
        tuneCenterArea()

        // Clock settings
        clockPosition = ClockPosition.valueOf(
            preferences.get<String>(R.string.key_status_bar_clock_position).toInt()
        )
        amPmStyle = AmPmStyle.valueOf(
            preferences.get<String>(R.string.key_status_bar_clock_am_pm_style).toInt()
        )

        formatBefore = preferences.get<String>(R.string.key_status_bar_date_custom_date_before)
        formatAfter = preferences.get<String>(R.string.key_status_bar_date_custom_date_after)
        smallBefore = preferences.get<Boolean>(R.string.key_status_bar_date_small_before)
        smallAfter = preferences.get<Boolean>(R.string.key_status_bar_date_small_after)

        if ((formatBefore + formatAfter).trim().isEmpty()) {
            val dayOfWeekFormat = preferences.get<String>(R.string.key_status_bar_day_week).toInt()

            when (dayOfWeekFormat) {
                0 -> {
                    formatAfter = ""
                    formatBefore = ""
                }

                1 -> {
                    formatBefore = "\$GEEE "
                    formatAfter = ""
                    smallBefore = false
                }

                2 -> {
                    formatBefore = "\$GEEE "
                    formatAfter = ""
                    smallBefore = true
                }

                3 -> {
                    formatBefore = ""
                    formatAfter = " \$GEEE"
                    smallAfter = false
                }

                4 -> {
                    formatBefore = ""
                    formatAfter = " \$GEEE"
                    smallAfter = true
                }
            }
        }

        if (clockView == null) {
            Log.wx("Clock view not initialized yet")
            sendEvent(ModEvents.REQUEST_RESTART, Bundle().apply {
                putString(ModEvents.ARG_PACKAGE, SYSTEM_UI_PACKAGE)
            })
            return
        }

        placeClock()
    }

    private fun placeClock() {
        clockView?.parent?.let { parent ->
            val targetArea = when (clockPosition) {
                ClockPosition.LEFT -> {
                    clockView?.setPadding(0, 0, leftClockPadding, 0)
                    clockViewParent
                }
                ClockPosition.CENTER -> {
                    clockView?.setPadding(rightClockPadding, 0, rightClockPadding, 0)
                    centeredIconArea as ViewGroup?
                }
                ClockPosition.RIGHT -> {
                    clockView?.setPadding(rightClockPadding, 0, 0, 0)

                    appContext.resources.getIdentifier("system_icons", "id", SYSTEM_UI_PACKAGE).let { id ->
                        systemIconArea?.findViewById<LinearLayout>(id)
                    }
                }
                ClockPosition.GONE -> null
            }

            (parent as ViewGroup).removeView(clockView)
            targetArea?.let { area ->
                when (clockPosition) {
                    ClockPosition.LEFT -> area.addView(clockView, 0)
                    else -> area.addView(clockView)
                }
            }
        }
    }

    private fun tuneCenterArea() {
        try {
            val screenWidth = fullStatusbar?.measuredWidth ?: return
            val notificationWidth = screenWidth / 2

            notificationIconAreaInner?.parent?.parent?.parent?.let { parent ->
                (parent as ViewGroup).layoutParams.width = notificationWidth
            }

            systemIconArea?.layoutParams?.width = screenWidth - notificationWidth
        } catch (ex: Exception) {
            val msg = "Failed to adjust center status bar area"
            Log.ex(ex, msg)
            appContext.sendHookExceptionEvent<StatusBarClock>(ex, msg)
        }
    }

    private fun getStyledSpan(dateFormat: String, small: Boolean): CharSequence {
        if (dateFormat.isEmpty()) return ""

        val formatted = SpannableStringBuilder(StringFormatter.formatString(dateFormat))
        if (small) {
            formatted.setSpan(RelativeSizeSpan(0.7f), 0, formatted.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return formatted
    }

    enum class ClockPosition(val value: Int) {
        LEFT(0), CENTER(1), RIGHT(2), GONE(3);
        companion object {
            fun valueOf(value: Int) = entries.first { it.value == value }
        }
    }

    enum class AmPmStyle(val value: Int) {
        NORMAL(0), SMALL(1), GONE(2);
        companion object {
            fun valueOf(value: Int) = entries.first { it.value == value }
        }
    }

}