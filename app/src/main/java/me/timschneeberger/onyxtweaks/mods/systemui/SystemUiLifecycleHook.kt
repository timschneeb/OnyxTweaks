package me.timschneeberger.onyxtweaks.mods.systemui

import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.LifecycleBroadcastHook
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages

@TargetPackages(SYSTEM_UI_PACKAGE)
class SystemUiLifecycleHook : LifecycleBroadcastHook()