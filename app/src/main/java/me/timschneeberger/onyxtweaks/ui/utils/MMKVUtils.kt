package me.timschneeberger.onyxtweaks.ui.utils

import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.ui.services.unmarshallFromPipe
import kotlin.reflect.KClass

object MMKVUtils {
    val knownTypes = mapOf<String, KClass<*>>(
        /* ==== System config @ /onyxconfig/mmkv ==== */

        // OnyxSystemConfig
        "eac_system_version" to Int::class,
        "appFreezeAdditionalList" to Set::class,
        "appFreezeWhiteList" to Set::class,
        "bootCompletedBlackSet" to Set::class,
        "detectedTopPackageSet" to Set::class,
        "dpiNeedFullRelaunchPkgSet" to Set::class,
        "grantPermissionUriAuthorityPackages" to Set::class,
        "grantPermissionUriAuthority" to Set::class,
        "hiddenAPIThirdPartyAppWhitelist" to Set::class,
        "ignoreCastDevicesPackages" to Set::class,
        "maintainPrevPkgRefreshPkgSet" to Set::class,
        "presetEACWhiteList" to Set::class,
        "restrictedInstallPackageSet" to Set::class,
        "showForegroundNotificationPackages" to Set::class,
        "supportForceStopAppSet" to Set::class,
        "supportMultiInstencePackages" to Set::class,

        // KeyboardDeviceConfig
        "keyboard_device_version" to Int::class,
        "keyboard_default_device_version" to Int::class,

        // EACAppConfig
        "eac_default_app_config" to String::class, // JSON EACAppConfig object

        // EACDeviceConfig
        "eac_app_pkg_set" to Set::class, // String set of package names
        "eac_device_json_version" to Int::class,

        // EACDeviceExtraConfig
        "allow_child_mode_apps" to Set::class, // String set of package names
        "allowUseRegalModePkgSet" to Set::class, // String set of package names
        "customOOMAdjPkgSet" to Set::class, // String set of package names
        "eac_extra_config_enable" to Boolean::class,
        "eac_extra_gc_after_scrolling" to Boolean::class,
        "eac_extra_gc_after_scrolling_delay_time" to Int::class,
        "eac_extra_refresh_mode" to Int::class,
        "eac_extra_turbo" to Int::class,
        "gc_after_scrolling_refresh_mode" to Int::class,
        "pre_grant_premission_pkg" to Set::class, // String set of package names

        // NotificationConfig
        "notification_version" to Int::class,
        "notification_pkg_black_list" to Set::class, // String set of package names
        "notification_pkg_white_list" to Set::class, // String set of package names

        // GesturesConfig (SystemUI)
        "gesture_config" to String::class, // JSON GesturesConfig object

        // From the keyboard app?
        "forceComputeImeBoundsSystemComponents" to Set::class // String set of pkg/component names
    )

    val knownTypesForPrefixes = mapOf<String, KClass<*>>(
        /* ==== System config @ /onyxconfig/mmkv ==== */

        // KeyboardMapping
        "keymapping_" to String::class, // JSON KeyboardMapping object for a specific device name

        // EACAppConfig
        "eac_app_" to Set::class,  // JSON EACAppConfig object for a specific package
    )

    val supportedTypes = mapOf(
        String::class to "String",
        Set::class to "String set",
        Int::class to "32-bit integer (int)",
        Long::class to "64-bit integer (long)",
        Float::class to "Floating point number",
        Boolean::class to "Boolean (true/false)"
    )

    fun IMMKVAccessService.getString(handle: String?, key: String): String? {
        getLargeString(handle, key).also { fd ->
            fd.unmarshallFromPipe().let {
                val string = it.readString()
                it.recycle()
                return string
            }
        }
    }

    fun IMMKVAccessService.getStringSet(handle: String?, key: String): List<String> {
        getLargeStringSet(handle, key).also { fd ->
            fd.unmarshallFromPipe().let {
                val list = mutableListOf<String>()
                it.readStringList(list)
                it.recycle()
                return list
            }
        }
    }

    fun isKnownType(key: String): Boolean {
        return knownTypes.containsKey(key) || knownTypesForPrefixes.keys.any { key.startsWith(it) }
    }

    fun IMMKVAccessService.resolveValue(handle: String?, key: String, truncate: Boolean, type: KClass<*>? = null): Any? {
        val type = type ?:
            knownTypes[key]
            ?: knownTypesForPrefixes.entries.find { key.startsWith(it.key) }?.value
            ?: tryResolveType(handle, key)

        return when (type) {
            String::class -> if (!truncate) getString(handle, key) else getTruncatedString(handle, key, 200)
            Set::class -> if (!truncate) getStringSet(handle, key) else getTruncatedStringSet(handle, key, 200)
            Int::class -> getInt(handle, key)
            Long::class -> getLong(handle, key)
            Float::class -> getFloat(handle, key)
            Boolean::class -> getBoolean(handle, key)
            else -> null
        }
    }

    fun IMMKVAccessService.tryResolveType(handle: String?, key: String): KClass<*>? {
        val size = getValueActualSize(handle, key)
        val strVal by lazy { getTruncatedString(handle, key, 2000) }

        return when {
            size == 0 -> null // empty; unknown type
            size <= 4 -> Int::class // warning: could also be a float or boolean
            size <= 8 -> Long::class // warning: could also be a double
            // If it's String-like with control characters, it's likely a set because each
            // string entry has a int32 length field which would may be parsed as non-printable chars
            strVal?.any { ch -> ch.isISOControl() } == true -> Set::class
            // Otherwise, it's likely a string
            else -> String::class
        }
    }
}