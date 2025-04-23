package me.timschneeberger.onyxtweaks.mods.launcher

import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.LifecycleBroadcastHook

@TargetPackages(LAUNCHER_PACKAGE)
class LauncherLifecycleHook : LifecycleBroadcastHook()