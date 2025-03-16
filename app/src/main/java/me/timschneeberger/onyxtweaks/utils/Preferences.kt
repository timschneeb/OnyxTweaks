package me.timschneeberger.onyxtweaks.utils

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import com.crossbowffs.remotepreferences.RemotePreferences
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import kotlin.reflect.KClass

enum class PreferenceGroups(@XmlRes val xmlRes: Int, val prefName: String) {
    ROOT(R.xml.app_preferences, "app_preferences"),
    LAUNCHER(R.xml.app_launcher_preferences, "app_launcher_preferences"),
    STATUS_BAR(R.xml.app_status_bar_preferences, "app_status_bar_preferences"),
    QS(R.xml.app_qs_preferences, "app_qs_preferences"),
    RECENTS(R.xml.app_recents_preferences, "app_recents_preferences"),
    EINK(R.xml.app_eink_opt_preferences, "app_eink_opt_preferences"),
    MISC(R.xml.app_misc_preferences, "app_misc_preferences"),
    ABOUT(R.xml.app_about_preferences, "app_about_preferences")
}

/**
 * @remarks This class is used to access preferences with read-only without an app context using XSharedPreferences.
 *          It should be always used when writing preferences is not needed.
 */
class Preferences(group: PreferenceGroups) : BasePreferences(group) {
    override val prefs: SharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID, group.prefName).also { it ->
        it.makeWorldReadable()


        XposedBridge.log("Preferences INIT: ${group.prefName}")
        XposedBridge.log(it.file.toString())
        XposedBridge.log("readable?: ${it.file.canRead()}")

        if (!it.file.canRead()) {
            XposedBridge.log("CRITICAL: Preferences file not readable")
        }

        it.reload()
        it.all.forEach { t, u ->
            XposedBridge.log("---> $t: $u")
        }
        try {
            @Suppress("DEPRECATION")
            it.registerOnSharedPreferenceChangeListener(this)
        }
        catch (e: UnsupportedOperationException) {
            XposedBridge.log(e)
        }
    }
}

/**
 * @remarks This class is used to write preferences. It should be avoided if possible, as it requires an app context.
 *          EzXHelper.appContext must be initialized before using this class. Therefore it should not be used in zygote or resource hooks.
 */
class WritablePreferences(group: PreferenceGroups) : BasePreferences(group) {
    override val prefs: SharedPreferences = RemotePreferences(appContext, BuildConfig.APPLICATION_ID, group.prefName, true).also { it ->
        it.registerOnSharedPreferenceChangeListener(this)
    }
}

abstract class BasePreferences(val group: PreferenceGroups) : SharedPreferences.OnSharedPreferenceChangeListener {
    var onPreferencesChanged: ((String?) -> Unit)? = null

    private val defaultCache: HashMap<String, Any> = hashMapOf()
    protected abstract val prefs: SharedPreferences

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadEverything(key)
    }

    private fun loadEverything(vararg key: String?) {
        key.filterNotNull().forEach {
            onPreferencesChanged?.invoke(it)
        }
    }

    /**
     * @remarks This function takes a StringRes pointing to a preference key.
     *          There MUST be a default value with the same key name in defaults.xml,
     *          otherwise an exception will be thrown when loading the default value.
     *
     * @return Returns the current value of the preference or the default value if none is set
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(@StringRes nameRes: Int, default: T? = null, type: KClass<T>): T {
        val key = moduleRes.getString(nameRes)

        XposedBridge.log("=====> GET $nameRes")
        XposedBridge.log("get -> $key")

        val defValue = default ?: getDefault(nameRes, type)

        XposedBridge.log("  default -> $defValue")

        return when(type) {
            Boolean::class -> prefs.getBoolean(key, defValue as Boolean) as T
            String::class -> prefs.getString(key, defValue as String) as T
            Int::class -> prefs.getInt(key, defValue as Int) as T
            Long::class -> prefs.getLong(key, defValue as Long) as T
            Float::class -> prefs.getFloat(key, defValue as Float) as T
            else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
        }.also {
            // CrashlyticsImpl.setCustomKey("${namespace}_$key", it.toString())
            XposedBridge.log("  value -> $it")
        }
    }

    inline fun <reified T : Any> get(@StringRes nameRes: Int): T {
        return get<T>(nameRes, null, T::class)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getDefault(@StringRes nameRes: Int, type: KClass<T>): T {
        val key = moduleRes.getString(nameRes)
        return if(defaultCache.containsKey(key)) {
            defaultCache[key] as T
        }
        else {
            @SuppressLint("DiscouragedApi")
            val defaultRes = moduleRes.getIdentifier(
                "default_$key",
                when(type)
                {
                    Boolean::class -> "bool"
                    String::class -> "string"
                    Int::class -> "integer"
                    Long::class -> "integer"
                    Float::class -> "integer"
                    else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
                },
                BuildConfig.APPLICATION_ID
            )

            if(defaultRes == 0) {
                throw IllegalStateException("Preference key '$key' has no default set")
            }

            (when(type) {
                Boolean::class -> moduleRes.getBoolean(defaultRes)
                String::class -> moduleRes.getString(defaultRes)
                Int::class -> moduleRes.getInteger(defaultRes)
                Long::class -> moduleRes.getInteger(defaultRes).toLong()
                Float::class -> moduleRes.getInteger(defaultRes).toFloat()
                else -> throw IllegalArgumentException("Unknown type")
            } as T).also {
                defaultCache[key] = it as Any
            }
        }
    }

    inline fun <reified T : Any> getDefault(@StringRes nameRes: Int): T {
        return getDefault(nameRes, T::class)
    }

    @SuppressLint("ApplySharedPref")
    fun <T : Any> reset(@StringRes nameRes: Int, async: Boolean = true, type: KClass<T>) {
        set(nameRes, getDefault(nameRes, type), async, type)
    }

    inline fun <reified T : Any> reset(@StringRes nameRes: Int, async: Boolean = true) {
        return reset(nameRes, async, T::class)
    }

    @SuppressLint("ApplySharedPref")
    fun <T : Any> set(@StringRes nameRes: Int, value: T, async: Boolean = true, type: KClass<T>) {
        val key = moduleRes.getString(nameRes)
        val edit = prefs.edit()
        // CrashlyticsImpl.setCustomKey("${namespace}_$key", value.toString())

        when(type) {
            Boolean::class -> edit.putBoolean(key, value as Boolean)
            String::class -> edit.putString(key, value as String)
            Int::class -> edit.putInt(key, value as Int)
            Long::class -> edit.putLong(key, value as Long)
            Float::class -> edit.putFloat(key, value as Float)
            else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
        }.run {
            if(async)
                apply()
            else
                commit()
        }
    }

    inline fun <reified T : Any> set(@StringRes nameRes: Int, value: T, async: Boolean = true) {
        set(nameRes, value, async, T::class)
    }
}