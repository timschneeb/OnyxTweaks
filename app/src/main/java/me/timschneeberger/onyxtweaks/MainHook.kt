package me.timschneeberger.onyxtweaks

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    // TODO: general: add fallbacks for resources & some extern method calls
    private fun hookClass(cls: Class<*>, lTAG: String) {
        if(cls.classLoader == null)
            return;

        val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
        for (method in cls.declaredMethods) unhooks.add(
            XposedBridge.hookMethod(
                method,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (method.name == "getDefaultQuickLauncherFunctions") {
                            // TODO

                            val modelCls = XposedHelpers.findClassIfExists(
                                "com.onyx.common.applications.appwidget.model.QuickLauncherModel",
                                cls.classLoader
                            )

                            val model = XposedHelpers.newInstance(modelCls)
                            XposedHelpers.callMethod(model, "setApp", true)

                            val appDataInfoCls = XposedHelpers.findClassIfExists(
                                "com.onyx.android.sdk.data.AppDataInfo",
                                cls.classLoader
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

                            XposedBridge.log("D/$lTAG => getDefaultQuickLauncherFunctions: $model")

                            param.result = listOf(model)
                        } else if (method.name == "getHotSeatApps") {
                            val list = cls.getMethod("getConfigExtraApps") // TODO replace with actual method
                                .invoke(param.thisObject) as? List<*>
                            if (list == null) {
                                XposedBridge.log("E/$lTAG => getConfigExtraApps returned null (or cast failed)")
                                return
                            }

                            param.result = list
                        } else if (method.name == "getExtraClusterAppListMap") {
                            val map = HashMap<String, List<Any>>()
                            val list: MutableList<Any> = ArrayList()
                            list.add("com.onyx.common.applications.appwidget.widget.RecentApps4x1WidgetProvider")
                            list.add("com.onyx.common.applications.appwidget.widget.QuickSettingsWidget2X2Provider")
                            map["com.onyx.common.applications.appwidget.widget.RecentApps4x1WidgetProvider"] = list
                            param.result = map
                        } else if (method.name == "isNotificationManagerItemStayOnTop") {
                            //param.setResult(false);
                        }
                    }
                })
        )
    }


    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val lTAG = "OnyxTweaks:" + lpparam.packageName

        try {
            XposedBridge.log("--------- D/$lTAG handleLoadPackage")

            val cls = XposedHelpers.findClassIfExists(
                "android.onyx.systemui.SystemUIConfig",
                lpparam.classLoader
            )
            val sysUiConf = XposedHelpers.findClassIfExists(
                "android.onyx.config.SysUIConfig",
                lpparam.classLoader
            )

            val devConf = XposedHelpers.findClassIfExists(
                "com.onyx.common.common.model.DeviceConfig",
                lpparam.classLoader
            )
            if (devConf != null) {
                hookClass(devConf, lTAG)
            }

            if (sysUiConf != null) {
                hookClass(sysUiConf, lTAG)
            }

            if (cls != null) {
                hookClass(cls, lTAG)
            }

            if (lpparam.packageName == "com.android.systemui") {

                /*XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.StatusBarIconView", lpparam.classLoader, "setNotification", "android.service.notification.StatusBarNotification", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedHelpers.setObjectField(param.thisObject, "mNotifRoundPaint", null);
                }
            });

            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconAreaController", lpparam.classLoader, "resetIconConfig", "com.android.systemui.statusbar.StatusBarIconView", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return param.args[0];
                }
            });*/
                /*
                XposedHelpers.findAndHookMethod("com.android.systemui.recents.OnyxRecentsActivity$TabletManagerAdapter", lpparam.classLoader, "onPageCreateViewHolder", "android.view.ViewGroup", int.class, new XC_MethodReplacement() {
                    @SuppressLint("DiscouragedApi")
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        Context ctx = ((ViewGroup) param.args[0]).getContext();
                        int id = ctx.getResources().getIdentifier("recents_task_view_phone", "layout", "com.android.systemui");

                        View view = LayoutInflater.from(ctx).inflate(
                                id,
                                (ViewGroup) param.args[0],
                                false
                        );

                        Class<?> recentItemViewHolder = XposedHelpers.findClassIfExists("com.android.systemui.recents.OnyxRecentsActivity$RecentItemViewHolder", lpparam.classLoader);
                        Constructor<?> ctor = recentItemViewHolder.getDeclaredConstructor(View.class);
                        ctor.setAccessible(true);
                        return ctor.newInstance(view);
                    }
                });
*/

                /*XposedHelpers.findAndHookMethod("com.android.systemui.recents.OnyxRecentsActivity", lpparam.classLoader, "getRow", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    boolean isPortrait = (boolean) XposedHelpers.callMethod(param.thisObject, "isPortrait");
                    if(!isPortrait) {
                        return 8;
                    }
                    return 5;
                }
            });
            XposedHelpers.findAndHookMethod("com.android.systemui.recents.OnyxRecentsActivity", lpparam.classLoader, "getColumn", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    boolean isPortrait = (boolean) XposedHelpers.callMethod(param.thisObject, "isPortrait");
                    if(!isPortrait) {
                        return 8;
                    }
                    return 5;
                }
            });*/
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

        } catch (e: Exception) {
            XposedBridge.log(e)
        }
    }



    @SuppressLint("DiscouragedApi")
    @Throws(Throwable::class)
    override fun handleInitPackageResources(resparam: InitPackageResourcesParam) {
        // Replace navBar layout


        if (resparam.packageName == "com.android.systemui") {
            try {
                resparam.res.setReplacement(
                    "com.android.systemui",
                    "bool",
                    "is_phone_layout",
                    false
                )

                // these two need to be both on
                resparam.res.setReplacement(
                    "com.android.systemui",
                    "bool",
                    "qs_panel_height_custom",
                    true
                )
                resparam.res.setReplacement(
                    "com.android.systemui",
                    "bool",
                    "qs_panel_custom_bg",
                    true
                )

                resparam.res.setReplacement(
                    "com.android.systemui",
                    "integer",
                    "onyx_notification_container_max_icons",
                    5
                )
                resparam.res.setReplacement(
                    "com.android.systemui",
                    "integer",
                    "onyx_recent_item_space_count",
                    30
                )
            } catch (e: Exception) {
                XposedBridge.log("E/ RESOURCE HOOK => $e")
            }
        }
    }
}
