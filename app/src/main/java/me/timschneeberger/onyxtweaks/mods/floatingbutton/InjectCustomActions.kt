package me.timschneeberger.onyxtweaks.mods.floatingbutton

import android.content.Context
import android.onyx.ViewUpdateHelper
import android.provider.Settings
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.FLOATING_BUTTON_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import java.lang.invoke.MethodHandles

/**
 * This mod pack injects additional actions into the floating button app.
 *
 * It injects new enum entries into FunctionType and adds the corresponding handler code.
 * Synthetic methods such as Enum.values and Enum.valueOf must also be patched to include the new entries.
 * Depends on [me.timschneeberger.onyxtweaks.mods.framework.FixBwModePersistence].
 */
@TargetPackages(FLOATING_BUTTON_PACKAGE)
class InjectCustomActions : ModPack() {
    override val group = PreferenceGroups.FLOATING_BUTTON

    // List of custom actions to be injected
    private enum class CustomActions(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
        BW_MODE(R.string.floating_button_bw_mode, R.drawable.ic_bw_mode),
    }

    private val funcTypeCls by lazy { findClass("com.onyx.floatingbutton.setting.data.FunctionType") }
    private val customFuncTypeInstances by lazy {
        CustomActions.entries.mapIndexed { index, customAction ->
            // Dynamically create new enum values
            customAction to newFunctionTypeEnumValue(customAction.name, index + originalFunctionTypeSize)
        }
    }

    /**
     * Creates a new instance of a FunctionType enum value with the given name and ordinal value.
     */
    private fun newFunctionTypeEnumValue(name: String, index: Int) =
        MethodHandles.lookup().unreflectConstructor(
            ConstructorFinder
                .fromClass(funcTypeCls)
                .filterByParamCount(2)
                .first()
        ).invokeWithArguments(CUSTOM_ACTIONS_PREFIX + name, index)

    @Suppress("UNCHECKED_CAST")
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if(!preferences.get<Boolean>(R.string.key_floating_button_show_bw_function))
            return

        // Get the original size of the FunctionType enum
        originalFunctionTypeSize = (funcTypeCls
            .methodFinder()
            .firstByName("values")
            .invoke(null) as Array<*>)
            .size

        // Workaround: Add our module asset path to the context of the ImageView
        findClass("androidx.appcompat.widget.AppCompatImageHelper")
            .constructorFinder()
            .filterByParamTypes(ImageView::class.java)
            .first()
            .createAfterHookCatching<InjectCustomActions> { param ->
                /*
                 * This is a workaround for the issue where our module's asset path is not injected properly
                 * into the context used by AppCompatImageView.
                 * Otherwise, a resource not found exception occurs when trying to load the icon
                 * for the custom action. However, only the main settings screen is affected.
                 * Neither the floating button nor the action selector need this workaround.
                 * Normally, the asset path is already injected by the EzXHelper module.
                 * Not sure why this Context instance is separate.
                 */
                EzXHelper.addModuleAssetPath((param.args.first() as ImageView).context)
            }

        // Inject titles and icons for custom actions
        findClass("com.onyx.floatingbutton.setting.data.FunctionDataMap").apply {
            methodFinder()
                .filterByParamCount(0)
                .firstByName("getFunctionTitleMap")
                .createAfterHookCatching<InjectCustomActions> { param ->
                    val map = param.result as MutableMap<Any, String>
                    customFuncTypeInstances.forEach { (action, fcType) ->
                        map.put(fcType, moduleRes.getString(action.labelRes))
                    }
                }

            methodFinder()
                .filterByParamCount(0)
                .firstByName("getFunctionIconMap")
                .createAfterHookCatching<InjectCustomActions> { param ->
                    val map = param.result as MutableMap<Any, Int>
                    customFuncTypeInstances.forEach { (action, fcType) ->
                        map.put(fcType, action.iconRes)
                    }
                }
        }

        // Inject custom actions into the synthetic methods of the FunctionType enum
        findClass("com.onyx.floatingbutton.setting.data.FunctionType").apply {
            methodFinder()
                .firstByName("valueOf")
                .createReplaceHookCatching<InjectCustomActions> { param ->
                    val name = param.args[0] as String
                    val customAction = customFuncTypeInstances.find {
                        CUSTOM_ACTIONS_PREFIX + it.first.name == name
                    }
                    when {
                        customAction != null -> customAction.second
                        else -> param.invokeOriginalMethod()
                    }
                }

            methodFinder()
                .firstByName("values")
                .createReplaceHookCatching<InjectCustomActions> { param ->
                    param.invokeOriginalMethod().let {
                        // Arrays of FunctionType enum values used here
                        val customActions = java.lang.reflect.Array.newInstance(funcTypeCls, customFuncTypeInstances.size) as Array<Any>
                        customFuncTypeInstances.map { it.second }.forEachIndexed { index, value ->
                            customActions[index] = value
                        }

                        val origValues = it as Array<Any>
                        origValues.copyOf(origValues.size + customActions.size).apply {
                            System.arraycopy(customActions, 0, this, origValues.size, customActions.size)
                        }
                    }
                }
        }

        // Patch available device-specific function list to include our custom actions
        findClass("com.onyx.floatingbutton.util.DeviceConfig")
            .methodFinder()
            .firstByName("getFunctionItems")
            .createAfterHookCatching<InjectCustomActions> { param ->
                param.result = (param.result as List<String>).toMutableList().apply {
                    addAll(customFuncTypeInstances.map { (it.second as Enum<*>).name })
                }
            }

        // Patch the handler to handle our custom actions
        findClass("com.onyx.floatingbutton.service.FloatButtonFunctionHandler").apply {
            methodFinder()
                .filterByParamTypes(Context::class.java, funcTypeCls, Int::class.java)
                .firstByName("handleFloatButtonFunction")
                .createReplaceHookCatching<InjectCustomActions> { param ->
                    val functionType = param.args[1] as Enum<*>
                    val functionTypeName = functionType.name

                    // Check if the function type is one of the custom actions
                    if (functionTypeName.startsWith(CUSTOM_ACTIONS_PREFIX)) {
                        Log.dx("Custom action triggered: $functionTypeName")
                        handleCustomAction(
                            CustomActions.valueOf(functionTypeName.replace(CUSTOM_ACTIONS_PREFIX, ""))
                        )
                    }
                    else {
                        param.invokeOriginalMethod()
                    }
                }
        }
    }

    private fun handleCustomAction(action: CustomActions) {
        when (action) {
            CustomActions.BW_MODE -> {
                val newState = if(Settings.Global.getInt(appContext.contentResolver, "view_update_bw_mode", 0) == 1) 0 else 1
                ViewUpdateHelper.setBWMode(newState)
                Settings.Global.putInt(appContext.contentResolver, "view_update_bw_mode", newState)
            }
        }
    }

    companion object {
        private var originalFunctionTypeSize = 0

        private const val CUSTOM_ACTIONS_PREFIX = "OT_CUSTOM_ACTION_"
    }
}