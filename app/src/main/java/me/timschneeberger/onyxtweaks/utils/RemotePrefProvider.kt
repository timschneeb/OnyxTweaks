package me.timschneeberger.onyxtweaks.utils

import com.crossbowffs.remotepreferences.RemotePreferenceFile
import com.crossbowffs.remotepreferences.RemotePreferenceProvider
import me.timschneeberger.onyxtweaks.BuildConfig

class RemotePrefProvider : RemotePreferenceProvider(
    BuildConfig.APPLICATION_ID,
    PreferenceGroups
        .entries
        .map { group -> RemotePreferenceFile(group.prefName, true) }
        .toTypedArray()
)


