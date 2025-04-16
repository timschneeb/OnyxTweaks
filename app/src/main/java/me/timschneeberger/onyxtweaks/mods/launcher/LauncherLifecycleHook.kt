package me.timschneeberger.onyxtweaks.mods.launcher

import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.LifecycleBroadcastHook
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages

@TargetPackages(LAUNCHER_PACKAGE)
class LauncherLifecycleHook : LifecycleBroadcastHook()