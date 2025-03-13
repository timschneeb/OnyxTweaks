package me.timschneeberger.onyxtweaks.mods

import me.timschneeberger.onyxtweaks.mods.framework.UseNotificationIconColors
import me.timschneeberger.onyxtweaks.mods.launcher.AddFunctionBarSpacer
import me.timschneeberger.onyxtweaks.mods.launcher.AddLauncherSettingsMenu
import me.timschneeberger.onyxtweaks.mods.launcher.AddSettingCategories
import me.timschneeberger.onyxtweaks.mods.launcher.ChangeFunctionBarLocation
import me.timschneeberger.onyxtweaks.mods.launcher.DisableAppFilter
import me.timschneeberger.onyxtweaks.mods.launcher.EnableDesktopWidgets
import me.timschneeberger.onyxtweaks.mods.launcher.EnableDock
import me.timschneeberger.onyxtweaks.mods.launcher.EnableKeyboardSettings
import me.timschneeberger.onyxtweaks.mods.launcher.HideFunctionBarItems
import me.timschneeberger.onyxtweaks.mods.launcher.HideTopBorder
import me.timschneeberger.onyxtweaks.mods.launcher.ShowChineseBookStore
import me.timschneeberger.onyxtweaks.mods.shared.AddUserSwitcherToQs
import me.timschneeberger.onyxtweaks.mods.shared.EnableWallpaper
import me.timschneeberger.onyxtweaks.mods.shared.QuickTileGridSize
import me.timschneeberger.onyxtweaks.mods.systemui.AddGrayscaleModeQsTile
import me.timschneeberger.onyxtweaks.mods.systemui.AddSettingsButtonToQs
import me.timschneeberger.onyxtweaks.mods.systemui.CompactQsPanel
import me.timschneeberger.onyxtweaks.mods.systemui.CustomizeRecents
import me.timschneeberger.onyxtweaks.mods.systemui.EnableHeadsUpNotifications
import me.timschneeberger.onyxtweaks.mods.systemui.HideNotificationIconBorders
import me.timschneeberger.onyxtweaks.mods.systemui.MoveNotificationHeaderToFooter
import me.timschneeberger.onyxtweaks.mods.systemui.SetMaxNotificationIcons
import me.timschneeberger.onyxtweaks.mods.systemui.ShowAdditionalStatusIcons
import me.timschneeberger.onyxtweaks.mods.systemui.ShowWifiActivityIndicators

object ModPacks {
    val available = arrayOf(
        // Framework
        UseNotificationIconColors::class,

        // Launcher
        AddFunctionBarSpacer::class,
        AddLauncherSettingsMenu::class,
        AddSettingCategories::class,
        ChangeFunctionBarLocation::class,
        // TODO DisableAppFilter::class,
        EnableDesktopWidgets::class,
        EnableDock::class,
        EnableKeyboardSettings::class,
        HideFunctionBarItems::class,
        HideTopBorder::class,
        ShowChineseBookStore::class,

        // Shared
        AddUserSwitcherToQs::class,
        EnableWallpaper::class,
        QuickTileGridSize::class,

        // SystemUI
        AddGrayscaleModeQsTile::class,
        AddSettingsButtonToQs::class,
        CompactQsPanel::class,
        CustomizeRecents::class,
        EnableHeadsUpNotifications::class,
        MoveNotificationHeaderToFooter::class,
        HideNotificationIconBorders::class,
        SetMaxNotificationIcons::class,
        ShowAdditionalStatusIcons::class,
        ShowWifiActivityIndicators::class,
    )
}