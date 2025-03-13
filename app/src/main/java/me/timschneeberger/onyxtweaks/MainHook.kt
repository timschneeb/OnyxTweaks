package me.timschneeberger.onyxtweaks

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {

    // TODO: general: add launcher grid customization

    // TODO: general: add fallbacks for resources & some extern method calls
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val devConf = XposedHelpers.findClassIfExists(
            "com.onyx.common.common.model.DeviceConfig",
            lpparam.classLoader
        )
        if (devConf != null) {
            for (method in devConf.declaredMethods)
                XposedBridge.hookMethod(
                    method,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            // TODO check if this is necessary
                            if (method.name == "getDefaultQuickLauncherFunctions") {

                                val modelCls = XposedHelpers.findClassIfExists(
                                    "com.onyx.common.applications.appwidget.model.QuickLauncherModel",
                                    lpparam.classLoader
                                )

                                val model = XposedHelpers.newInstance(modelCls)
                                XposedHelpers.callMethod(model, "setApp", true)

                                val appDataInfoCls = XposedHelpers.findClassIfExists(
                                    "com.onyx.android.sdk.data.AppDataInfo",
                                    lpparam.classLoader
                                )
                                val appDataInfo = XposedHelpers.newInstance(appDataInfoCls)
                                XposedHelpers.setObjectField(appDataInfo, "packageName", "com.onyx")
                                XposedHelpers.setObjectField(
                                    appDataInfo,
                                    "activityClassName",
                                    "com.onyx.common.library.ui.LibraryActivity"
                                )
                                XposedHelpers.setObjectField(
                                    appDataInfo,
                                    "action",
                                    "com.onyx.action.LIBRARY"
                                )
                                XposedHelpers.setObjectField(appDataInfo, "isSystemApp", true)
                                XposedHelpers.setObjectField(appDataInfo, "isCustomizedApp", true)
                                XposedHelpers.setObjectField(appDataInfo, "labelName", "library")
                                XposedHelpers.setObjectField(appDataInfo, "customName", "library")
                                XposedHelpers.setObjectField(
                                    appDataInfo,
                                    "iconDrawableName",
                                    "app_library"
                                )


                                XposedHelpers.callMethod(model, "setAppDataInfo", appDataInfo)

                                XposedBridge.log("D => getDefaultQuickLauncherFunctions: $model")

                                param.result = listOf(model)
                            }
                        }
                    })
        }

        if (lpparam.packageName == "com.onyx") {


            /* XposedHelpers.findAndHookMethod("com.onyx.common.applications.appwidget.utils.AppWidgetUtils", lpparam.classLoader, "getQuickLauncherFunctionsJsonFromMMKV", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    printCall(param, lTAG);

                    LinkedHashMap<String, Integer> result = JSON.parseObject(
                            (String)param.getResult(),
                            new StringIntMapRef()
                    );

                    Class<?> modelCls = XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.model.QuickLauncherModel", cls.getClassLoader());

                    Object model = XposedHelpers.newInstance(modelCls);
                    XposedHelpers.callMethod(model, "setApp", true);

                    Class<?> appDataInfoCls = XposedHelpers.findClassIfExists("com.onyx.android.sdk.data.AppDataInfo", cls.getClassLoader());
                    Object appDataInfo = XposedHelpers.newInstance(appDataInfoCls);
                    XposedHelpers.setObjectField(appDataInfo, "packageName", "com.onyx");
                    XposedHelpers.setObjectField(appDataInfo, "activityClassName", "com.onyx.common.library.ui.LibraryActivity");
                    XposedHelpers.setObjectField(appDataInfo, "action", "com.onyx.action.LIBRARY");
                    XposedHelpers.setObjectField(appDataInfo, "isSystemApp", true);
                    XposedHelpers.setObjectField(appDataInfo, "isCustomizedApp", true);
                    XposedHelpers.setObjectField(appDataInfo, "labelName", "library");
                    XposedHelpers.setObjectField(appDataInfo, "customName", "library");
                    XposedHelpers.setObjectField(appDataInfo, "iconDrawableName", "app_library");
                    XposedHelpers.callMethod(model, "setAppDataInfo", appDataInfo);

                    param.setResult(JSON.toJSONString(model));

                    if(result == null || result.isEmpty()){//if(param.getResult() == "" || param.getResult() == "[]") {
                        XposedBridge.log("D/" + lTAG + " => getQuickLauncherFunctionsJsonFromMMKV is empty");
                        //param.setResult(null);
                    }
                }
            });*/

            // TODO also hook the setters to prevent overridden values to be saved?
        }
    }
}
