package me.timschneeberger.onyxtweaks.mods

import android.os.Build
import me.timschneeberger.onyxtweaks.mods.global.PerActivityRefreshModes
import me.timschneeberger.onyxtweaks.mods.launcher.AddFunctionBarSpacer
import me.timschneeberger.onyxtweaks.mods.launcher.AddLauncherSettingsMenu
import me.timschneeberger.onyxtweaks.mods.launcher.AddSettingCategories
import me.timschneeberger.onyxtweaks.mods.launcher.ChangeFunctionBarLocation
import me.timschneeberger.onyxtweaks.mods.launcher.DesktopGridSize
import me.timschneeberger.onyxtweaks.mods.launcher.DisableAppFilter
import me.timschneeberger.onyxtweaks.mods.launcher.EnableDesktopWidgets
import me.timschneeberger.onyxtweaks.mods.launcher.EnableDock
import me.timschneeberger.onyxtweaks.mods.launcher.EnableKeyboardSettings
import me.timschneeberger.onyxtweaks.mods.launcher.HideAppLabels
import me.timschneeberger.onyxtweaks.mods.launcher.HideFunctionBarItems
import me.timschneeberger.onyxtweaks.mods.launcher.HideTopBorder
import me.timschneeberger.onyxtweaks.mods.launcher.LauncherLifecycleHook
import me.timschneeberger.onyxtweaks.mods.launcher.ShowAppsToolbar
import me.timschneeberger.onyxtweaks.mods.launcher.ShowChineseBookStore
import me.timschneeberger.onyxtweaks.mods.settings.ShowHiddenSettings
import me.timschneeberger.onyxtweaks.mods.shared.AddUserSwitcherToQs
import me.timschneeberger.onyxtweaks.mods.shared.EnableWallpaper
import me.timschneeberger.onyxtweaks.mods.shared.RemoveRegalModeRestriction
import me.timschneeberger.onyxtweaks.mods.shared.UseNotificationIconColors
import me.timschneeberger.onyxtweaks.mods.systemui.AddQsTiles
import me.timschneeberger.onyxtweaks.mods.systemui.AddSettingsButtonToQs
import me.timschneeberger.onyxtweaks.mods.systemui.CompactQsPanel
import me.timschneeberger.onyxtweaks.mods.systemui.CustomizeRecents
import me.timschneeberger.onyxtweaks.mods.systemui.EnableHeadsUpNotifications
import me.timschneeberger.onyxtweaks.mods.systemui.FixStatusBarIconColor
import me.timschneeberger.onyxtweaks.mods.systemui.HideNotificationIconBorders
import me.timschneeberger.onyxtweaks.mods.systemui.MoveNotificationHeaderToFooter
import me.timschneeberger.onyxtweaks.mods.systemui.QuickTileGridSize
import me.timschneeberger.onyxtweaks.mods.systemui.SetMaxNotificationIcons
import me.timschneeberger.onyxtweaks.mods.systemui.ShowAdditionalStatusIcons
import me.timschneeberger.onyxtweaks.mods.systemui.ShowWifiActivityIndicators
import me.timschneeberger.onyxtweaks.mods.systemui.StatusBarClock
import me.timschneeberger.onyxtweaks.mods.systemui.SystemUiLifecycleHook

object ModPacks {
    val testedModels = arrayOf("GoColor7")
    val testedAndroidVersions = arrayOf(Build.VERSION_CODES.S_V2)

    val available = arrayOf(
        // Global
        PerActivityRefreshModes::class,

        // Launcher
        AddFunctionBarSpacer::class,
        AddLauncherSettingsMenu::class,
        AddSettingCategories::class,
        ChangeFunctionBarLocation::class,
        DesktopGridSize::class,
        DisableAppFilter::class,
        EnableDesktopWidgets::class,
        EnableDock::class,
        EnableKeyboardSettings::class,
        HideAppLabels::class,
        HideFunctionBarItems::class,
        HideTopBorder::class,
        ShowAppsToolbar::class,
        ShowChineseBookStore::class,

        // Settings
        ShowHiddenSettings::class,

        // Shared
        AddUserSwitcherToQs::class,
        EnableWallpaper::class,
        RemoveRegalModeRestriction::class,
        UseNotificationIconColors::class,

        // SystemUI
        AddQsTiles::class,
        AddSettingsButtonToQs::class,
        CompactQsPanel::class,
        CustomizeRecents::class,
        EnableHeadsUpNotifications::class,
        FixStatusBarIconColor::class,
        HideNotificationIconBorders::class,
        MoveNotificationHeaderToFooter::class,
        QuickTileGridSize::class,
        SetMaxNotificationIcons::class,
        ShowAdditionalStatusIcons::class,
        ShowWifiActivityIndicators::class,
        StatusBarClock::class,

        // Lifecycle hooks
        LauncherLifecycleHook::class,
        SystemUiLifecycleHook::class
    )
}