package me.timschneeberger.onyxtweaks.ui.utils

import android.os.Parcel
import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.ui.services.marshallToPipe
import me.timschneeberger.onyxtweaks.ui.services.unmarshallFromPipe
import kotlin.reflect.KClass

object MMKVUtils {
    val knownTypes = mapOf<String, KnownTypes>(
        /* ==== System config @ /onyxconfig/mmkv ==== */

        // OnyxSystemConfig
        "eac_system_version" to KnownTypes.INT,
        "appFreezeAdditionalList" to KnownTypes.STRING_SET,
        "appFreezeWhiteList" to KnownTypes.STRING_SET,
        "bootCompletedBlackSet" to KnownTypes.STRING_SET,
        "detectedTopPackageSet" to KnownTypes.STRING_SET,
        "dpiNeedFullRelaunchPkgSet" to KnownTypes.STRING_SET,
        "grantPermissionUriAuthorityPackages" to KnownTypes.STRING_SET,
        "grantPermissionUriAuthority" to KnownTypes.STRING_SET,
        "hiddenAPIThirdPartyAppWhitelist" to KnownTypes.STRING_SET,
        "ignoreCastDevicesPackages" to KnownTypes.STRING_SET,
        "maintainPrevPkgRefreshPkgSet" to KnownTypes.STRING_SET,
        "presetEACWhiteList" to KnownTypes.STRING_SET,
        "restrictedInstallPackageSet" to KnownTypes.STRING_SET,
        "showForegroundNotificationPackages" to KnownTypes.STRING_SET,
        "supportForceStopAppSet" to KnownTypes.STRING_SET,
        "supportMultiInstencePackages" to KnownTypes.STRING_SET,

        // KeyboardDeviceConfig
        "keyboard_device_version" to KnownTypes.INT,
        "keyboard_default_device_version" to KnownTypes.INT,

        // EACAppConfig
        "eac_default_app_config" to KnownTypes.STRING_JSON, // JSON EACAppConfig object

        // EACDeviceConfig
        "eac_app_pkg_set" to KnownTypes.STRING_SET, // String set of package names
        "eac_device_json_version" to KnownTypes.INT,

        // EACDeviceExtraConfig
        "allow_child_mode_apps" to KnownTypes.STRING_SET, // String set of package names
        "allowUseRegalModePkgSet" to KnownTypes.STRING_SET, // String set of package names
        "customOOMAdjPkgSet" to KnownTypes.STRING_SET, // String set of package names
        "eac_extra_config_enable" to KnownTypes.BOOLEAN,
        "eac_extra_gc_after_scrolling" to KnownTypes.BOOLEAN,
        "eac_extra_gc_after_scrolling_delay_time" to KnownTypes.INT,
        "eac_extra_refresh_mode" to KnownTypes.INT,
        "eac_extra_turbo" to KnownTypes.INT,
        "gc_after_scrolling_refresh_mode" to KnownTypes.INT,
        "pre_grant_premission_pkg" to KnownTypes.STRING_SET, // String set of package names

        // NotificationConfig
        "notification_version" to KnownTypes.INT,
        "notification_pkg_black_list" to KnownTypes.STRING_SET, // String set of package names
        "notification_pkg_white_list" to KnownTypes.STRING_SET, // String set of package names

        // GesturesConfig (SystemUI)
        "gesture_config" to KnownTypes.STRING_JSON, // JSON GesturesConfig object

        // From the keyboard app?
        "forceComputeImeBoundsSystemComponents" to KnownTypes.STRING_SET // String set of pkg/component names
    )

    val knownTypesForPrefixes = mapOf<String, KnownTypes>(
        /* ==== System config @ /onyxconfig/mmkv ==== */

        // KeyboardMapping
        "keymapping_" to KnownTypes.STRING_JSON, // JSON KeyboardMapping object for a specific device name

        // EACAppConfig
        "eac_app_" to KnownTypes.STRING_JSON,  // JSON EACAppConfig object for a specific package
    )

    enum class EditorMode {
        PLAIN_TEXT,
        JSON,
        LIST
    }

    enum class KnownTypes(val typeClass: KClass<*>, val editorMode: EditorMode?, val description: String) {
        STRING(String::class, EditorMode.PLAIN_TEXT, "String"),
        STRING_JSON(String::class, EditorMode.JSON, "String (JSON formatted)"),
        STRING_SET(Set::class, EditorMode.LIST, "String list"),
        INT(Int::class, null, "32-bit integer (int)"),
        LONG(Long::class, null, "64-bit integer (long)"),
        FLOAT(Float::class, null, "Floating point number"),
        BOOLEAN(Boolean::class, null, "Boolean (true or false)")
    }

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

    fun IMMKVAccessService.putString(handle: String?, key: String, value: String) {
        Parcel.obtain().apply {
            writeString(value)
        }.run {
            marshallToPipe().also { putLargeString(handle, key, it) }
        }
    }

    fun IMMKVAccessService.putStringSet(handle: String?, key: String, values: List<String>) {
        Parcel.obtain().apply {
            writeStringList(values)
        }.run {
            marshallToPipe().also { putLargeStringSet(handle, key, it) }
        }
    }

    fun isKnownType(key: String): Boolean {
        return knownTypes.containsKey(key) || knownTypesForPrefixes.keys.any { key.startsWith(it) }
    }

    fun IMMKVAccessService.resolveValue(handle: String?, key: String, truncate: Boolean, type: KnownTypes? = null): Any? {
        val resolvedType = type ?: resolveType(handle, key)

        return when (resolvedType?.typeClass) {
            String::class -> if (!truncate) getString(handle, key) else getTruncatedString(handle, key, 200)
            Set::class -> if (!truncate) getStringSet(handle, key) else getTruncatedStringSet(handle, key, 200)
            Int::class -> getInt(handle, key)
            Long::class -> getLong(handle, key)
            Float::class -> getFloat(handle, key)
            Boolean::class -> getBoolean(handle, key)
            else -> null
        }
    }

    fun IMMKVAccessService.resolveType(handle: String?, key: String): KnownTypes? {
        return knownTypes[key]
            ?: knownTypesForPrefixes.entries.find { key.startsWith(it.key) }?.value
            ?: tryGuessType(handle, key)
    }

    fun IMMKVAccessService.tryGuessType(handle: String?, key: String): KnownTypes? {
        val size = getValueActualSize(handle, key)
        val strVal by lazy { getTruncatedString(handle, key, 2000) }

        return when {
            size == 0 -> null // empty; unknown type
            size <= 4 -> KnownTypes.INT // warning: could also be a float or boolean
            size <= 8 -> KnownTypes.LONG // warning: could also be a double
            // If it's String-like with control characters, it's likely a set because each
            // string entry has a int32 length field which would may be parsed as non-printable chars
            strVal?.any { ch -> ch.isISOControl() } == true -> KnownTypes.STRING_SET
            // Otherwise, it's likely a string
            else -> KnownTypes.STRING
        }
    }
}