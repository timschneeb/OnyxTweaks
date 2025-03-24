package me.timschneeberger.onyxtweaks.ui.utils

import android.os.Parcel
import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.ui.services.marshallToPipe
import me.timschneeberger.onyxtweaks.ui.services.unmarshallFromPipe
import kotlin.reflect.KClass

object MMKVUtils {
    val knownTypes: Map<String, KnownTypes>
        get() = mapOf<String, KnownTypes>(
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
            "forceComputeImeBoundsSystemComponents" to KnownTypes.STRING_SET, // String set of pkg/component names
            "ignoreComputeImeBoundsSystemComponents" to KnownTypes.STRING_SET, // String set of pkg/component names

            /* ==== COMMON @ (including kcb_feedback) ==== */
            "search_history_key" to KnownTypes.STRING_JSON, // JSON string list

            /* ==== Launcher @ com.onyx ==== */
            "build_preset_book_jd_version" to KnownTypes.INT,
            "child_last_select_usage_duration" to KnownTypes.LONG,
            "child_use_deadline" to KnownTypes.LONG,
            "CHINA_SELECT_ALL_SHOP_ON_UI" to KnownTypes.BOOLEAN,
            "CHINA_SHOP_VERSION" to KnownTypes.INT,
            "custom_navigation_bar_functions" to KnownTypes.STRING_JSON, // List<FunctionConfig.ConfigItem>
            "doc_encryption_type" to KnownTypes.INT,
            "doc_encryption_digest" to KnownTypes.STRING,
            "doc_auto_encryption" to KnownTypes.BOOLEAN,
            "EAC_TUTORIAL_COMPLETED" to KnownTypes.BOOLEAN,
            "ENABLED_SHOP_LIST" to KnownTypes.STRING_JSON, // List<Shop>
            "first_into_child_mode" to KnownTypes.BOOLEAN,
            "gc_invalidate_interval_key" to KnownTypes.INT,
            "hide_library_tab_menu_list_config_key" to KnownTypes.STRING,
            "HK_ENABLED_SHOP_LIST" to KnownTypes.STRING_JSON, // List<Shop>
            "HK_SELECT_ALL_SHOP_ON_UI" to KnownTypes.BOOLEAN,
            "HK_SHOP_VERSION" to KnownTypes.INT,
            "home_page_refresh" to KnownTypes.BOOLEAN,
            "INTERNATIONAL_ENABLED_SHOP_LIST" to KnownTypes.STRING_JSON, // List<Shop>
            "INTERNATIONAL_SELECT_ALL_SHOP_ON_UI" to KnownTypes.BOOLEAN,
            "INTERNATIONAL_SHOP_VERSION" to KnownTypes.INT,
            "last_navigation_type_key" to KnownTypes.INT,
            "library_first_tab_type_key" to KnownTypes.INT,
            "library_need_show_export_annotation_guide" to KnownTypes.BOOLEAN,
            "library_need_show_favorite_guide" to KnownTypes.BOOLEAN,
            "library_need_show_library_tab_guide" to KnownTypes.BOOLEAN,
            "library_need_show_net_disk_guide" to KnownTypes.BOOLEAN,
            "library_need_show_view_style_guide" to KnownTypes.BOOLEAN,
            "library_progress_style_key" to KnownTypes.STRING, // ProgressStyle enum
            "library_tab_menu_list_config_key" to KnownTypes.STRING,
            "locale_and_cluster_combined_key" to KnownTypes.STRING_JSON, // LocaleClusterBean
            "main_function_config" to KnownTypes.STRING_JSON, // List<FunctionConfig.ConfigItem>
            "migrate_account_to_ksync" to KnownTypes.BOOLEAN,
            "migrate_netdisk_books_to_library" to KnownTypes.BOOLEAN,
            "navigation_bar_combination" to KnownTypes.INT,
            "navigation_type_key" to KnownTypes.INT,
            "need_load_preset_book_jd" to KnownTypes.BOOLEAN,
            "normal_user_config_adb_status" to KnownTypes.BOOLEAN,
            "normal_user_config_float_button_status" to KnownTypes.INT,
            "normal_user_config_screen_off_value" to KnownTypes.INT,
            "onyx_cert_info_cache_key" to KnownTypes.STRING,
            "onyx_current_home_tab_key" to KnownTypes.STRING, // FunctionConfig.Function enum
            "settings_more_display_function_bar_right_display_key" to KnownTypes.BOOLEAN,
            "show_paste_local_file_to_netdisk_guid" to KnownTypes.BOOLEAN,
            "preinstall_filter_shortcuts" to KnownTypes.STRING_JSON, // JSON string list
            "policy_agree_status_key" to KnownTypes.BOOLEAN,
            "key_secondary_screen_widgets" to KnownTypes.STRING_JSON, // Map<Integer, String>
            "key_quick_launcher_functions" to KnownTypes.STRING_JSON, // List<QuickLauncherModel>
            "key_bilibili_comic_tutorial_status" to KnownTypes.INT,

            // Account
            "ONYX_ACCOUNT_AREA_CODE_KEY" to KnownTypes.STRING,
            "ONYX_ACCOUNT_EMAIL_INPUT_KEY" to KnownTypes.STRING,
            "ONYX_ACCOUNT_LOGIN_TYPE_KEY" to KnownTypes.INT,
            "ONYX_ACCOUNT_PHONE_INPUT_KEY" to KnownTypes.STRING,
            "SHOW_ACCOUNT_AUTO_LOGOUT_DIALOG" to KnownTypes.BOOLEAN,

            // Feedback
            "key_unread_feedback_msg_count" to KnownTypes.INT,
            "send_feedback_with_log" to KnownTypes.BOOLEAN,
            "key_feedback_current_tab" to KnownTypes.STRING,
            "key_new_feedback_msg" to KnownTypes.BOOLEAN,
            "key_show_feedback_notification_tips" to KnownTypes.BOOLEAN,

            // Dream
            "key_clock_refresh_interval" to KnownTypes.LONG,
            "key_carousel_images" to KnownTypes.STRING_JSON,
            "key_clock_typeface" to KnownTypes.STRING,
            "key_applied_dream" to KnownTypes.STRING,
            "key_wake_up_type" to KnownTypes.STRING,
            "key_default_motto_source" to KnownTypes.STRING,
            "key_default_poem_source" to KnownTypes.STRING,
            "key_default_word_source" to KnownTypes.STRING,
            "key_image_signature" to KnownTypes.STRING,
            "key_motto_source" to KnownTypes.STRING,
            "key_poem_source" to KnownTypes.STRING,
            "key_transparent_status_bar_style" to KnownTypes.STRING,
            "key_transparent_sign_position" to KnownTypes.STRING,
            "key_transparent_style" to KnownTypes.STRING,
            "key_word_source" to KnownTypes.STRING,

            // Shutdown Image
            "SHUTDOWN_VERSION" to KnownTypes.INT,
            "key_custom_display_text" to KnownTypes.STRING,
            "key_default_shutdown_image_path" to KnownTypes.STRING,
            "key_display_text_type" to KnownTypes.STRING,
            "key_shutdown_image_path" to KnownTypes.STRING,
            "key_shutdown_image_scale_type" to KnownTypes.STRING,
            "key_text_color" to KnownTypes.STRING,
            "key_text_position_type" to KnownTypes.STRING,
            "key_last_tab_selection" to KnownTypes.STRING,
            "key_build_in_poem_config" to KnownTypes.BOOLEAN,
            "key_build_in_word_config" to KnownTypes.BOOLEAN,
            "key_show_border" to KnownTypes.BOOLEAN,
            "key_show_bottom_status_bar" to KnownTypes.BOOLEAN,
            "key_show_next_alarm" to KnownTypes.BOOLEAN,
            "key_show_screensaver_notification" to KnownTypes.BOOLEAN,

            // OTA
            "key_next_ota_reminder_time" to KnownTypes.LONG,
            "key_ota_reminder_disabled" to KnownTypes.BOOLEAN,
            "key_ota_reminder_firmware" to KnownTypes.STRING,
            "key_last_firmware" to KnownTypes.STRING_JSON,
            "key_new_firmware" to KnownTypes.STRING_JSON,
            "key_ota_beta_testing" to KnownTypes.BOOLEAN,

            // Font Configuration
            "import_cjk_font" to KnownTypes.STRING_JSON, // List<FontInfo>
            "import_latin_font" to KnownTypes.STRING_JSON, // List<FontInfo>
            "import_font" to KnownTypes.STRING_JSON, // List<FontInfo>
            "import_font_show_tip" to KnownTypes.BOOLEAN,

            // AppSettings class
            "pref_key__app_init" to KnownTypes.BOOLEAN,
            "pref_key__desktop_columns" to KnownTypes.INT,
            "pref_key__desktop_lock" to KnownTypes.BOOLEAN,
            "pref_key__desktop_page_count" to KnownTypes.INT,
            "pref_key__desktop_rows" to KnownTypes.INT,
            "pref_key__dock_columns" to KnownTypes.INT,
            "pref_key__dock_rows" to KnownTypes.INT,
            "pref_key__enable_dragging_projection" to KnownTypes.BOOLEAN,
            "pref_key__replenish_position" to KnownTypes.BOOLEAN,
            "pref_key__set_main_page" to KnownTypes.INT,
            "pref_key__shake_replenish_position" to KnownTypes.BOOLEAN,
            "pref_key__show_eac_badge" to KnownTypes.BOOLEAN,
            "pref_key__show_freeze_badge" to KnownTypes.BOOLEAN,
            "pref_key__smart_assistant" to KnownTypes.BOOLEAN,
            "pref_key__support_dark_text" to KnownTypes.BOOLEAN,

            // Misc
            "gms_direct_login_init" to KnownTypes.BOOLEAN,
            "first_open" to KnownTypes.BOOLEAN,
            "key_migrate_simple_apps" to KnownTypes.BOOLEAN,
            "region_id" to KnownTypes.STRING,
            "onyx_first_display_privacy_policy_key" to KnownTypes.BOOLEAN,
            "key_available_gift"  to KnownTypes.BOOLEAN,
            "key_config_default_side_key_function" to KnownTypes.BOOLEAN,
            "key_show_account_bind_more_warning" to KnownTypes.BOOLEAN,
            "init_gestures_tag" to KnownTypes.BOOLEAN,
            "SHOW_PHYSICAL_KEYBOARD_TIPS_DIALOG_KEY" to KnownTypes.BOOLEAN,
            "device_storage_low" to KnownTypes.BOOLEAN,
            "usb_device_first_attached" to KnownTypes.BOOLEAN,
            "init_notification_default_config_tag" to KnownTypes.BOOLEAN,
            "child_time_managers_key" to KnownTypes.INT
        )

    val knownTypesForPrefixes = mapOf<String, KnownTypes>(
        /* ==== System config @ /onyxconfig/mmkv ==== */
        // KeyboardMapping
        "keymapping_" to KnownTypes.STRING_JSON, // JSON KeyboardMapping object for a specific device name
        // EACAppConfig
        "eac_app_" to KnownTypes.STRING_JSON,  // JSON EACAppConfig object for a specific package

        /* ==== Launcher @ com.onyx ==== */
        "auto_create_netdisk_library-" to KnownTypes.BOOLEAN,
        "auto_upload_embedded_file-" to KnownTypes.BOOLEAN,
        "auto_upload_embedded_file_size-" to KnownTypes.INT,
        "auto_upload_config_migrate_finished-" to KnownTypes.BOOLEAN,
        "default_start_app_" to KnownTypes.STRING,
        "DEVICE_PUSH_TIMESTAMP_ID_PREFIX_" to KnownTypes.STRING_JSON,
        "agreement_version_key_" to KnownTypes.LONG,
        "privacy_policy_version_key_" to KnownTypes.LONG,
        "key_config_status_" to KnownTypes.STRING,
        "dream_" to KnownTypes.STRING_JSON,
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