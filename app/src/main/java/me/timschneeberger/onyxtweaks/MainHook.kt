package me.timschneeberger.onyxtweaks

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.XModuleResources
import android.content.res.XResources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import me.timschneeberger.onyxtweaks.BuildConfig
import me.timschneeberger.onyxtweaks.R
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LayoutInflated
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Arrays

class MainHook : IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

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
                        val args = StringBuilder("(")
                        for (arg in param.args) {
                            if (arg == null) {
                                args.append("null,")
                                continue
                            }

                            args.append(arg.toString()).append(",")
                        }
                        if (args[args.length - 1] == ',') args.deleteCharAt(args.length - 1)
                        args.append(")")

                        if ( //OK! method.getName().equals("isRotateScreenshotForTransparentDream") ||
                            method.name == "isSupportChangeFunctionBarLocation" ||  // OK! But it disables the app menu
                            method.name == "isEnableDesktopWidget"
                        ) {                            // OK! method.getName().equals("isEnableKeyboardSetting") //||
                            //OK! method.getName().equals("isConfigFunctionBarSpace")
                            param.result = true
                        } else if (method.name == "getAppsFilter") {
                            // OK!
                            param.result = ArrayList<String>()
                        } else if (method.name == "getFilterWidgets") {
                            param.result = ArrayList<String>()
                        } else if (method.name == "getDefaultWidgets" || method.name == "getConfigWidgets") {
                            val map = LinkedHashMap<Int, String>()
                            map[-1] =
                                "com.onyx.common.applications.appwidget.widget.QuickLauncherProvider"
                            map[-2] =
                                "com.onyx.common.applications.appwidget.widget.LibraryRecentlyReadProvider"
                            map[-3] =
                                "com.onyx.common.applications.appwidget.widget.ShopRecommendProvider"
                            map[-4] = "com.onyx.mail.calendar.widget.CurrentDayMemoWidgetProvider"
                            map[-6] =
                                "com.onyx.common.applications.appwidget.widget.StatisticsWidgetProvider"

                            param.result = map
                        } else if (method.name == "getDefaultQuickLauncherFunctions") {
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
                            val list = cls.getMethod("getConfigExtraApps")
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
                            map["com.onyx.common.applications.appwidget.widget.RecentApps4x1WidgetProvider"] =
                                list
                            param.result = map
                        } else if (method.name == "getSettingCategory") {
                            val origSettingCategory = XposedBridge.invokeOriginalMethod(
                                method,
                                param.thisObject,
                                param.args
                            )
                            val settingCategoryCls = Class.forName(
                                "com.onyx.android.sdk.kcb.setting.model.SettingCategory",
                                true,
                                cls.classLoader
                            )
                            val configItems = XposedBridge.invokeOriginalMethod(
                                settingCategoryCls.getMethod("getItemList"),
                                origSettingCategory,
                                arrayOfNulls(0)
                            ) as MutableList<Any>
                            configItems.add(
                                createSettingsEntry(
                                    cls.classLoader,
                                    "system_status_bar",
                                    "status_bar_setting",
                                    "ic_setting_status_bar"
                                )
                            )
                            configItems.add(
                                createSettingsEntry(
                                    cls.classLoader,
                                    "setting_application_management",
                                    "application_management",
                                    "ic_setting_application"
                                )
                            )
                            configItems.add(
                                createSettingsEntry(
                                    cls.classLoader,
                                    "frozen_app_settings",
                                    "app_freeze",
                                    "ic_freeze_manager"
                                )
                            )
                            configItems.add(
                                createSettingsEntry(
                                    cls.classLoader,
                                    "launcher_screensaver_setting_title",
                                    "launcher_screensaver_setting",
                                    "ic_item_screen_saver"
                                )
                            )
                            // not useful? configItems.add(createSettingsEntry(cls.getClassLoader(), "app_widget_quick_launcher", "quick_launcher_setting", "ic_widget_quick_settings_vector"));
                            configItems.add(
                                createSettingsEntry(
                                    cls.classLoader,
                                    "child_app_usage_statistics",
                                    "child_app_usage_statistics",
                                    "ic_item_usage_statistics"
                                )
                            )

                            param.result = origSettingCategory
                        } else if (method.name == "getFunctionConfig") {
                            val origSettingCategory = XposedBridge.invokeOriginalMethod(
                                method,
                                param.thisObject,
                                param.args
                            )
                            val settingCategoryCls = Class.forName(
                                "com.onyx.reader.main.model.FunctionConfig",
                                true,
                                cls.classLoader
                            )
                            val configItems = XposedBridge.invokeOriginalMethod(
                                settingCategoryCls.getMethod("getItemList"),
                                origSettingCategory,
                                arrayOfNulls(0)
                            ) as MutableList<Any>

                            configItems.clear()
                            configItems.add(
                                createBarEntry(
                                    cls.classLoader,
                                    "library",
                                    "home_library"
                                )
                            )
                            configItems.add(createBarEntry(cls.classLoader, "shop", "home_shop"))
                            //configItems.add(createBarEntry(cls.getClassLoader(), "note", "home_note"));
                            configItems.add(
                                createBarEntry(
                                    cls.classLoader,
                                    "storage",
                                    "home_storage"
                                )
                            )
                            configItems.add(createBarEntry(cls.classLoader, "apps", "home_apps"))
                            configItems.add(
                                createBarEntry(
                                    cls.classLoader,
                                    "setting",
                                    "home_setting"
                                )
                            )

                            param.result = origSettingCategory
                        } else if (method.name == "getUserAppConfig") {
                            val userAppConf = XposedBridge.invokeOriginalMethod(
                                method,
                                param.thisObject,
                                param.args
                            )
                            XposedHelpers.setObjectField(
                                userAppConf,
                                "enableWallpaperFeature",
                                true
                            )
                            param.result = userAppConf
                        } else if (method.name == "isFixNotificationIconColor") {
                            // only for CFA/color devices
                            // OK param.setResult(false);
                        } else if (method.name == "getQSNumColumns") {
                            param.result = 3
                        } else if (method.name == "isDisableHeadsUpPinnedNotification") {
                            // OK param.setResult(false);
                        } else if (method.name == "isDisableNotificationListenForHeadsUp") {
                            // OK param.setResult(false);
                        } else if (method.name == "isWallpaperFeatureEnabled") {
                            // not working
                            param.result = true
                        } else if (method.name == "isNotificationManagerItemStayOnTop") {
                            //param.setResult(false);
                        } else if (method.name == "isShowUserSwitch") {
                            param.result = true
                        }
                    }
                })
        )
    }

    @Throws(IllegalAccessException::class, InstantiationException::class)
    private fun createSettingsEntry(
        clsLoader: ClassLoader,
        title: String,
        name: String,
        image: String
    ): Any {
        val configItemCls = XposedHelpers.findClassIfExists(
            "com.onyx.android.sdk.kcb.setting.model.SettingCategory\$ConfigItem",
            clsLoader
        )

        val configItemInstance = configItemCls.getDeclaredConstructor().newInstance()
        XposedHelpers.setObjectField(configItemInstance, "title", title)
        XposedHelpers.setObjectField(configItemInstance, "name", name)
        XposedHelpers.setObjectField(configItemInstance, "image", image)
        return configItemInstance
    }

    @Throws(IllegalAccessException::class, InstantiationException::class)
    private fun createBarEntry(clsLoader: ClassLoader, name: String, image: String): Any {
        val configItemCls = XposedHelpers.findClassIfExists(
            "com.onyx.reader.main.model.FunctionConfig\$ConfigItem",
            clsLoader
        )

        val configItemInstance = configItemCls.getDeclaredConstructor().newInstance()
        XposedHelpers.setObjectField(configItemInstance, "name", name)
        XposedHelpers.setObjectField(configItemInstance, "image", image)
        return configItemInstance
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val lTAG = TAG + ":" + lpparam.packageName

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
            val eacConf = XposedHelpers.findClassIfExists(
                "android.onyx.config.EACConfig",
                lpparam.classLoader
            )
            val userDataProv = XposedHelpers.findClassIfExists(
                "com.onyx.reader.main.model.NormalUserDataProvider",
                lpparam.classLoader
            )
            val sysUiUtils = XposedHelpers.findClassIfExists(
                "com.android.systemui.util.Utils",
                lpparam.classLoader
            )
            val sbarUtils = XposedHelpers.findClassIfExists(
                "android.onyx.utils.StatusBarUtils",
                lpparam.classLoader
            )

            val sbarIconCtrl = XposedHelpers.findClassIfExists(
                "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
                lpparam.classLoader
            )
            val panelCtrl = XposedHelpers.findClassIfExists(
                "com.android.systemui.statusbar.phone.NotificationPanelViewController",
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

            if (eacConf != null) {
                hookClass(eacConf, lTAG)
            }

            if (userDataProv != null) {
                hookClass(userDataProv, lTAG)
            }

            if (sysUiUtils != null) {
                hookClass(sysUiUtils, lTAG)
            }
            if (sbarIconCtrl != null) {
                hookClass(sbarIconCtrl, lTAG)
            }

            if (sbarUtils != null) {
                hookClass(sbarUtils, lTAG)
            }

            if (panelCtrl != null) {
                hookClass(panelCtrl, lTAG)
            }


            if (sbarIconCtrl != null) {
                val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
                for (method in sbarIconCtrl.declaredMethods) if (method.name == "setIconVisibility") {
                    unhooks.add(XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val str = param.args[0] as String
                            if (str == "refresh_mode")  // || str.equals("tp_touch_mode"))
                                param.args[1] = true
                        }
                    }))
                }
            } else {
                XposedBridge.log("E/$lTAG ==========================================> sbarIconCtrl is null")
            }



            if (lpparam.packageName == "android") {
                XposedHelpers.findAndHookMethod(
                    "android.os.UserManager", lpparam.classLoader, "isUserSwitcherEnabled",
                    Boolean::class.javaPrimitiveType, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })

                XposedHelpers.findAndHookMethod(
                    "android.os.UserManager",
                    lpparam.classLoader,
                    "supportsMultipleUsers",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })
            }


            val footerView = XposedHelpers.findClassIfExists(
                "com.android.systemui.qs.QSFooterView",
                lpparam.classLoader
            )
            if (footerView != null) {
                val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
                for (method in footerView.declaredMethods) if (method.name == "setVisibility") {
                    unhooks.add(XposedBridge.hookMethod(method, object : XC_MethodReplacement() {
                        @Throws(Throwable::class)
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            if (param.args[0] == View.VISIBLE) return null

                            val superMethod = View::class.java.getDeclaredMethod(
                                "setVisibility",
                                Int::class.javaPrimitiveType
                            )
                            superMethod.invoke(param.thisObject, View.VISIBLE)

                            return null
                        }
                    }))
                }
            }
            val footerView2 = XposedHelpers.findClassIfExists(
                "com.android.systemui.statusbar.NotificationShelf",
                lpparam.classLoader
            )
            if (footerView2 != null) {
                val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
                for (method in footerView2.declaredMethods) if (method.name == "setVisibility") {
                    unhooks.add(XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            XposedBridge.log("D/" + lTAG + " CALLED: " + method.name)
                            param.args[0] = View.VISIBLE
                        }
                    }))
                }
            }

            if (lpparam.packageName == "com.android.systemui") {
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.qs.QSPanel",
                    lpparam.classLoader,
                    "initTabletTitleBar",
                    "android.view.View",
                    object : XC_MethodHook() {
                        @SuppressLint("DiscouragedApi")
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val settings =
                                XposedHelpers.getObjectField(param.thisObject, "settings") as View
                            settings.visibility = View.VISIBLE

                            val ctx = settings.context

                            /*

                                <LinearLayout
                        android:id="@+id/onyx_user_switcher"
                        android:background="@drawable/button_focus_background_dot"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="12dp">
                                <ImageView
                        android:layout_gravity="center_vertical"
                        android:layout_width="@dimen/ic_edit_qs_tile_icon_size"
                        android:layout_height="@dimen/ic_edit_qs_tile_icon_size"
                        android:layout_margin="@dimen/onyx_clock_row_button_padding"
                        android:src="@drawable/ic_qs_settings"
                        android:scaleType="fitCenter"/>
                                </LinearLayout>
                         */
                            val userSwitcher = LinearLayout(ctx)
                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.marginStart = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                12f,
                                ctx.resources.displayMetrics
                            ).toInt()
                            userSwitcher.layoutParams = params
                            userSwitcher.setOnClickListener { v: View? ->
                                val sysUiFac = XposedHelpers.findClass(
                                    "com.android.systemui.SystemUIFactory",
                                    lpparam.classLoader
                                )
                                val facInstance =
                                    XposedHelpers.callStaticMethod(sysUiFac, "getInstance")
                                val sysUiComp =
                                    XposedHelpers.callMethod(facInstance, "getSysUIComponent")

                                val userSwitchDialogProvider = XposedHelpers.getObjectField(
                                    sysUiComp,
                                    "userSwitchDialogControllerProvider"
                                )
                                val userSwitchDialog =
                                    XposedHelpers.callMethod(userSwitchDialogProvider, "get")
                                XposedHelpers.callMethod(
                                    userSwitchDialog,
                                    "showDialog",
                                    userSwitcher
                                )
                            }

                            val backgroundId = ctx.resources.getIdentifier(
                                "button_focus_background_dot",
                                "drawable",
                                "com.android.systemui"
                            )
                            userSwitcher.background = ContextCompat.getDrawable(ctx, backgroundId)

                            val image = ImageView(ctx)
                            val iconSize = ctx.resources.getDimensionPixelSize(
                                ctx.resources.getIdentifier(
                                    "ic_edit_qs_tile_icon_size",
                                    "dimen",
                                    "com.android.systemui"
                                )
                            )
                            val padding = ctx.resources.getDimensionPixelSize(
                                ctx.resources.getIdentifier(
                                    "onyx_clock_row_button_padding",
                                    "dimen",
                                    "com.android.systemui"
                                )
                            )
                            val imageParams = LinearLayout.LayoutParams(iconSize, iconSize)
                            imageParams.setMargins(padding, padding, padding, padding)
                            image.layoutParams = imageParams
                            image.scaleType = ImageView.ScaleType.FIT_CENTER
                            image.setImageDrawable(
                                ctx.createPackageContext(
                                    BuildConfig.APPLICATION_ID,
                                    Context.CONTEXT_IGNORE_SECURITY
                                ).getDrawable(R.drawable.ic_account)
                            )
                            image.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    ctx,
                                    android.R.color.black
                                )
                            )
                            image.imageTintMode = PorterDuff.Mode.SRC_IN
                            userSwitcher.addView(image)


                            (settings.parent as ViewGroup).addView(userSwitcher)
                        }
                    })

                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.qs.QSPanel",
                    lpparam.classLoader,
                    "startOnyxSettings",
                    "android.view.View",
                    object : XC_MethodReplacement() {
                        @Throws(Throwable::class)
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            val dependencyClass = XposedHelpers.findClassIfExists(
                                "com.android.systemui.Dependency",
                                lpparam.classLoader
                            )
                            val activityStarterClass = XposedHelpers.findClassIfExists(
                                "com.android.systemui.plugins.ActivityStarter",
                                lpparam.classLoader
                            )
                            if (dependencyClass == null || activityStarterClass == null) {
                                XposedBridge.log("E/$lTAG ==========================================> dependencyClass or activityStarterClass is null")
                                return null
                            }
                            val activityStarter = dependencyClass.getDeclaredMethod(
                                "get",
                                Class::class.java
                            ).invoke(null, activityStarterClass)
                            activityStarterClass
                                .getMethod(
                                    "postStartActivityDismissingKeyguard",
                                    Intent::class.java,
                                    Int::class.javaPrimitiveType
                                )
                                .invoke(activityStarter, Intent(Settings.ACTION_SETTINGS), 0)

                            // XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                            return null
                        }
                    })

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
                val fvCtrl = XposedHelpers.findClass(
                    "com.android.systemui.qs.QSFooterViewController",
                    lpparam.classLoader
                )
                XposedBridge.hookMethod(
                    Arrays.stream(fvCtrl.declaredMethods)
                        .filter { m: Method -> m.name == "setVisibility" }.findFirst().get(),
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.args[0] = 0
                        }
                    })
                XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.MultiUserSwitchController",
                    lpparam.classLoader,
                    "isMultiUserEnabled",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })
            }


            if (lpparam.packageName == "com.onyx") {
                XposedHelpers.findAndHookMethod(
                    "com.onyx.common.common.manager.BookshopManager",
                    lpparam.classLoader,
                    "isShowShopSelectUI",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })

                XposedHelpers.findAndHookMethod(
                    "com.onyx.android.sdk.data.cluster.ClusterFeatures",
                    lpparam.classLoader,
                    "supportChinaBookShops",
                    "com.onyx.android.sdk.data.cluster.model.ClusterInfo",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })


                /*XposedHelpers.findAndHookMethod("com.onyx.common.applications.appwidget.utils.AppWidgetUtils", lpparam.classLoader, "getSecondaryScreenWidgetsJsonFromMMKV", new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        printCall(param, lTAG);
                        Class<?> JSONUtilsClass = XposedHelpers.findClassIfExists("com.onyx.android.sdk.utils.JSONUtils", lpparam.classLoader);
                        //result: (LinkedHashMap) JSONUtils.parseObject(secondaryScreenWidgetsJsonFromMMKV, new b(), new JSONReader.Feature[0]);


                        LinkedHashMap<String, Integer> result = JSON.parseObject(
                                (String)param.getResult(),
                                new StringIntMapRef()
                        );

                        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
                        map.put(-1, "com.onyx.common.applications.appwidget.widget.QuickLauncherProvider");
                        map.put(-2, "com.onyx.common.applications.appwidget.widget.LibraryRecentlyReadProvider");
                        map.put(-3, "com.onyx.common.applications.appwidget.widget.ShopRecommendProvider");
                        map.put(-4, "com.onyx.mail.calendar.widget.CurrentDayMemoWidgetProvider");
                        map.put(-5, "com.onyx.common.applications.appwidget.widget.NoteGridWidgetProvider");
                        map.put(-6, "com.onyx.common.applications.appwidget.widget.StatisticsWidgetProvider");

                        param.setResult(JSON.toJSONString(map));

                        if(result == null || result.isEmpty()){//if(param.getResult() == "" || param.getResult() == "[]") {
                            XposedBridge.log("D/" + lTAG + " => getSecondaryScreenWidgetsJsonFromMMKV is empty");
                            //param.setResult(null);
                        }
                    }
                });*/

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
                XposedHelpers.findAndHookMethod(
                    "com.onyx.common.applications.appwidget.ui.AppWidgetSettingsFragment",
                    lpparam.classLoader,
                    "initView",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            XposedBridge.log("D/" + lTAG + " =============================> CALLED: " + param.method.name)
                        }
                    })


                // TODO method name is obfuscated
                XposedHelpers.findAndHookMethod(
                    "com.onyx.common.applications.appwidget.action.LoadSettingWidgetModelsAction",
                    lpparam.classLoader,
                    "m",
                    object : XC_MethodReplacement() {
                        @Throws(Throwable::class)
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            // Get installed widgets
                            val widgets = ArrayList<Any?>()
                            val appWidgetUtilsClass = XposedHelpers.findClass(
                                "com.onyx.common.applications.appwidget.utils.AppWidgetUtils",
                                lpparam.classLoader
                            )
                            val getInstalledWidgetsMethod =
                                appWidgetUtilsClass.getMethod("getInstalledWidgets")
                            val installedWidgets =
                                getInstalledWidgetsMethod.invoke(null) as List<AppWidgetProviderInfo>

                            for (appWidgetProviderInfo in installedWidgets) {
                                XposedBridge.log("D/$lTAG => Installed widget: $appWidgetProviderInfo")

                                // Create ViewModel
                                val viewModelClass = XposedHelpers.findClass(
                                    "com.onyx.common.applications.appwidget.model.AppWidgetItemViewModel",
                                    lpparam.classLoader
                                )
                                val constructor = viewModelClass.getConstructor(
                                    XposedHelpers.findClass(
                                        "android.appwidget.AppWidgetProviderInfo",
                                        lpparam.classLoader
                                    )
                                )
                                val viewModel = constructor.newInstance(appWidgetProviderInfo)

                                // Invoke label/icon methods
                                viewModelClass.getMethod("getLabel").invoke(viewModel)
                                viewModelClass.getMethod("getIcon").invoke(viewModel)

                                val appWidgetBundle = XposedHelpers.callStaticMethod(
                                    XposedHelpers.findClass(
                                        "com.onyx.common.applications.appwidget.model.AppWidgetBundle",
                                        lpparam.classLoader
                                    ),
                                    "getInstance"
                                )

                                val getWidgetsMethod =
                                    appWidgetBundle.javaClass.getMethod("getWidgets")
                                val widgetsMap =
                                    getWidgetsMethod.invoke(appWidgetBundle) as LinkedHashMap<*, *>

                                // Get provider string
                                val provider = appWidgetProviderInfo.provider.flattenToString()

                                // Check if widgetsMap contains the provider string
                                if (widgetsMap.containsValue(provider)) {
                                    for ((key, value) in widgetsMap) {
                                        if (value == provider) {
                                            viewModelClass.getMethod(
                                                "setAppWidgetId",
                                                Int::class.javaPrimitiveType
                                            ).invoke(
                                                viewModel,
                                                key
                                            )
                                        }
                                    }
                                }

                                widgets.add(viewModel)
                            }
                            return widgets
                        }
                    })

                XposedHelpers.findAndHookMethod(
                    "com.onyx.common.applications.appwidget.model.AppWidgetItemViewModel",
                    lpparam.classLoader,
                    "getLabel",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val label = param.result as String
                            val info = XposedHelpers.callMethod(
                                param.thisObject,
                                "getInfo"
                            ) as AppWidgetProviderInfo
                            XposedBridge.log("D/" + lTAG + " => getLabel: " + info.provider.packageName)

                            if (info.provider.packageName.contains("com.onyx")) {
                                XposedBridge.log("D/" + lTAG + " => OK!: " + info.provider.className)

                                val parts = info.provider.className.split("\\.".toRegex())
                                    .dropLastWhile { it.isEmpty() }.toTypedArray()

                                val shortProviderName =
                                    if (parts.isNotEmpty()) parts[parts.size - 1].replace(
                                        "Provider",
                                        ""
                                    ) else "builtin"
                                param.result = String.format("%s (%s)", label, shortProviderName)
                            }
                        }
                    })


                // TODO also hook the setters to prevent overridden values to be saved?
                XposedHelpers.findAndHookMethod(
                    "com.onyx.common.applications.model.AppSettings",
                    lpparam.classLoader,
                    "isDraggingProjectionEnable",
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    })

                XposedHelpers.findAndHookMethod(
                    "com.onyx.common.applications.action.DesktopOptionProcessImpl",
                    lpparam.classLoader,
                    "onWallpaperSelection",
                    object : XC_MethodReplacement() {
                        @Throws(Throwable::class)
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            val dopCls = XposedHelpers.findClass(
                                "com.onyx.common.applications.action.DesktopOptionProcessImpl",
                                lpparam.classLoader
                            )
                            val activity = Arrays.stream(dopCls.declaredFields)
                                .filter { f: Field -> f.type.name == "android.app.Activity" }
                                .map { f: Field ->
                                    XposedHelpers.getObjectField(
                                        param.thisObject,
                                        f.name
                                    ) as Activity
                                }
                                .findFirst()

                            val intent = Intent()
                            intent.setAction("android.intent.action.MAIN")
                            intent.setClassName(
                                "com.android.settings",
                                "com.android.settings.wallpaper.WallpaperSuggestionActivity"
                            )
                            XposedHelpers.callMethod(activity.get(), "startActivity", intent)
                            return null
                        }
                    })


                val dovCls = XposedHelpers.findClassIfExists(
                    "com.onyx.common.applications.view.DesktopOptionView",
                    lpparam.classLoader
                )
                val fastAdapterOnClick = XposedHelpers.findClassIfExists(
                    "com.mikepenz.fastadapter.listeners.OnClickListener",
                    lpparam.classLoader
                )
                val onClick = Arrays.stream(
                    XposedHelpers.findMethodsByExactParameters(
                        dovCls,
                        Void.TYPE,
                        fastAdapterOnClick
                    )
                )
                    .findFirst().get()
                val resManager = XposedHelpers.findClass(
                    "com.onyx.android.sdk.utils.ResManager",
                    lpparam.classLoader
                )
                val getIdMethod = resManager.getDeclaredMethod(
                    "getIdentifier",
                    String::class.java,
                    String::class.java
                )

                val dovInstances = ArrayList<WeakReference<Any?>>()
                XposedBridge.hookAllConstructors(dovCls, object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        dovInstances.add(WeakReference(param.thisObject))
                        XposedBridge.log("D/$lTAG => DesktopOptionView constructor")
                    }
                })

                val proxy = Proxy.newProxyInstance(
                    lpparam.classLoader,
                    arrayOf(fastAdapterOnClick),
                    LauncherSettingsClickHandler(dovInstances)
                )


                XposedBridge.hookMethod(onClick, object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("D/$lTAG => DesktopOptionView.onClick")
                        val fastAdapter = XposedHelpers.findClass(
                            "com.mikepenz.fastadapter.commons.adapters.FastItemAdapter",
                            lpparam.classLoader
                        )
                        val arrayFastAdapter: Class<*> =
                            java.lang.reflect.Array.newInstance(fastAdapter, 0).javaClass

                        val adapters = XposedHelpers.findFirstFieldByExactType(
                            dovCls,
                            arrayFastAdapter
                        )[param.thisObject] as Array<*>

                        val createItem = Arrays.stream(
                            XposedHelpers.findMethodsByExactParameters(
                                dovCls, null,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType, fastAdapterOnClick
                            )
                        )
                            .findFirst().get()

                        val items = fastAdapter.getDeclaredMethod("getAdapterItems")
                            .invoke(adapters[1]) as List<*>


                        val newItemCount = items.size + 1

                        val paddingResId = getIdMethod.invoke(
                            null,
                            "desktop_option_bottom_item_horizontal_padding",
                            "dimen"
                        ) as Int

                        XposedBridge.log("D/$lTAG => DesktopOptionItem: PaddingResId: $paddingResId")

                        val padding = resManager.getDeclaredMethod(
                            "getDimensionPixelSize",
                            Int::class.javaPrimitiveType
                        )
                            .invoke(null, paddingResId) as Int

                        XposedBridge.log("D/$lTAG => DesktopOptionItem: Padding: $padding")

                        val newWidth = (XposedHelpers.callMethod(
                            param.thisObject,
                            "getWidth"
                        ) as Int - (padding * (newItemCount - 1))) / newItemCount

                        XposedBridge.log("D/$lTAG => DesktopOptionItem: New width: $newWidth")

                        // Update width of existing items
                        for (item in items) {
                            XposedHelpers.setIntField(item, "width", newWidth)
                        }

                        val settingsItem = createItem.invoke(
                            param.thisObject,
                            getIdMethod.invoke(null, "ic_setting_vector", "drawable"),
                            getIdMethod.invoke(null, "settings", "string"),
                            newWidth,
                            proxy
                        )

                        fastAdapter.getDeclaredMethod("add", MutableList::class.java)
                            .invoke(adapters[1], settingsItem?.let { listOf(it) })
                    }
                })


                Arrays.stream<Class<*>>(dovCls.declaredClasses)
                    .filter { c: Class<*> -> c.isAssignableFrom(fastAdapterOnClick) }
                    .forEach { c: Class<*>? ->
                        XposedBridge.hookAllMethods(c, "onClick", object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val item = param.args[2]
                                val itemClass = XposedHelpers.findClass(
                                    "com.onyx.common.applications.utils.item.IconLabelItem",
                                    lpparam.classLoader
                                )
                                val stringId = XposedHelpers.callMethod(
                                    item,
                                    "getIdentifier",
                                    itemClass
                                ) as Int
                                if (stringId == getIdMethod.invoke(
                                        null,
                                        "settings",
                                        "string"
                                    ) as Int
                                ) {
                                    XposedBridge.log("D/$lTAG => DesktopOptionItem: Injected settings button clicked")

                                    val listener = XposedHelpers.findFirstFieldByExactType(
                                        dovCls,
                                        XposedHelpers.findClass(
                                            "com.onyx.common.applications.view.DesktopOptionViewListener",
                                            lpparam.classLoader
                                        )
                                    )[param.thisObject]

                                    XposedHelpers.callMethod(listener, "onLaunchSettings")
                                }
                            }
                        })
                    }


                val currentWallpaperReceiver = arrayOf<BroadcastReceiver?>(null)
                XposedHelpers.findAndHookConstructor(
                    "com.onyx.common.applications.view.Workspace",
                    lpparam.classLoader,
                    "android.content.Context",
                    "android.util.AttributeSet",
                    object : XC_MethodHook() {
                        @RequiresPermission(anyOf = ["android.permission.READ_WALLPAPER_INTERNAL", Manifest.permission.MANAGE_EXTERNAL_STORAGE])
                        @Throws(
                            Throwable::class
                        )
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val context = param.args[0] as Context

                            // if(currentWallpaperReceiver[0] != null)
                            //   context.unregisterReceiver(currentWallpaperReceiver[0]);
                            currentWallpaperReceiver[0] = object : BroadcastReceiver() {
                                @RequiresPermission(anyOf = ["android.permission.READ_WALLPAPER_INTERNAL", Manifest.permission.MANAGE_EXTERNAL_STORAGE])
                                override fun onReceive(context: Context, intent: Intent) {
                                    val image = WallpaperManager.getInstance(context).drawable
                                    try {
                                        param.thisObject.javaClass
                                            .getMethod("setBackground", Drawable::class.java)
                                            .invoke(param.thisObject, image)
                                    } catch (e: Exception) {
                                        XposedBridge.log(e)
                                    }

                                    XposedBridge.log("D/$lTAG => Received broadcast: ${intent.action}")
                                }
                            }

                            val image = WallpaperManager.getInstance(context).drawable
                            param.thisObject.javaClass
                                .getMethod("setBackground", Drawable::class.java)
                                .invoke(param.thisObject, image)

                            // Register WALLPAPER_CHANGED broadcast receiver
                            val filter = IntentFilter()
                            filter.addAction("com.onyx.action.WALLPAPER_CHANGED")
                            filter.addAction("android.intent.action.WALLPAPER_CHANGED")

                            ContextCompat.registerReceiver(
                                context,
                                currentWallpaperReceiver[0], filter, ContextCompat.RECEIVER_EXPORTED
                            )

                            XposedBridge.log("D/$lTAG => Workspace constructor: $image")
                        }
                    })
            }


            // logClassCalls(XposedHelpers.findClassIfExists("com.android.systemui.qs.FooterActionsController", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.android.systemui.qs.FooterActionsView", lpparam.classLoader), lTAG);
            logClassCalls(
                XposedHelpers.findClassIfExists(
                    "com.android.systemui.qs.QSFooterViewController",
                    lpparam.classLoader
                ), lTAG
            )
            logClassCalls(
                XposedHelpers.findClassIfExists(
                    "com.android.systemui.statusbar.phone.MultiUserSwitch",
                    lpparam.classLoader
                ), lTAG
            )
            logClassCalls(
                XposedHelpers.findClassIfExists(
                    "com.android.systemui.statusbar.phone.MultiUserSwitchController",
                    lpparam.classLoader
                ), lTAG
            )


            //logClassCalls(XposedHelpers.findClassIfExists("me.bakumon.statuslayoutmanager.library.ReplaceLayoutHelper", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.ui.AppWidgetSettingsFragment", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.ui.AppWidgetListFragment", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.adapter.AppWidgetBaseAdapter", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.action.LoadWidgetModelsAction", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.action.LoadSettingWidgetModelsAction", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.model.AppWidgetBundle", lpparam.classLoader), lTAG, List.of("getInstance", "getDataManager", "getAppWidgetManager", "getRecentReadModels", "getAppContext"));
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.utils.AppWidgetUtils", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.android.sdk.utils.Debug", lpparam.classLoader), lTAG, List.of("d", "e", "i", "v", "w"));
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.adapter.AppWidgetResidentFunctionAdapter", lpparam.classLoader), lTAG);
            //logClassCalls(XposedHelpers.findClassIfExists("com.onyx.common.applications.appwidget.adapter.AppWidgetSettingsAdapter", lpparam.classLoader), lTAG);
        } catch (e: Exception) {
            XposedBridge.log(e)
        }
    }

    private fun printCall(param: MethodHookParam, lTAG: String) {
        val method = param.method as Method
        val cls = method.declaringClass

        val args = StringBuilder("(")
        for (arg in param.args) {
            if (arg == null) {
                args.append("null,")
                continue
            }

            args.append(arg).append(",")
        }
        if (args[args.length - 1] == ',') args.deleteCharAt(args.length - 1)
        args.append(")")

        val isVoid = method.returnType == Void.TYPE
        XposedBridge.log("D/$lTAG [${cls.simpleName}] ${method.name}$args => ${if (isVoid) "<void>" else param.result}")
    }


    private fun logClassCalls(
        cls: Class<*>,
        lTAG: String,
        methodFilter: List<String> = ArrayList()
    ) {
        val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
        for (method in cls.declaredMethods) {
            if (methodFilter.isNotEmpty() && methodFilter.contains(method.name)) continue

            unhooks.add(XposedBridge.hookMethod(method, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    //if(cls.getName().contains("MultiUserSwitch"))
                    //    XposedBridge.log(new Exception("Dump state"));
                    printCall(param, lTAG)
                }
            }))
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
                    "integer",
                    "quick_settings_max_columns",
                    3
                )

                resparam.res.setReplacement(
                    "com.android.systemui",
                    "integer",
                    "quick_settings_min_num_tiles",
                    1
                )

                resparam.res.setReplacement(
                    "com.android.systemui",
                    "integer",
                    "quick_settings_min_rows",
                    1
                )


                resparam.res.setReplacement(
                    "com.android.systemui",
                    "bool",
                    "is_phone_layout",
                    false
                )

                resparam.res.setReplacement(
                    "com.android.systemui",
                    "bool",
                    "config_showActivity",
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

                val stockId = resparam.res.getIdentifier(
                    "quick_settings_tiles_stock",
                    "string",
                    "com.android.systemui"
                )
                val stockQs = resparam.res.getString(stockId) + ",bw_mode"

                resparam.res.setReplacement(
                    "com.android.systemui",
                    "string",
                    "quick_settings_tiles_stock",
                    stockQs
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


                resparam.res.hookLayout(
                    "com.android.systemui",
                    "layout",
                    "quick_status_bar_expanded_header",
                    object : XC_LayoutInflated() {
                        @Throws(Throwable::class)
                        override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                        }
                    })
            } catch (e: Exception) {
                XposedBridge.log("E/$TAG RESOURCE HOOK => $e")
            }
        } else if (resparam.packageName == "com.onyx") {
            try {
                // Hide top border
                resparam.res.setReplacement(
                    "com.onyx",
                    "color",
                    "main_activity_top_border_color",
                    android.R.color.transparent
                )
            } catch (e: Exception) {
                XposedBridge.log("E/$TAG RESOURCE HOOK => $e")
            }
        }

        XResources.setSystemWideReplacement(
            "android",
            "bool",
            "config_enableWallpaperService",
            true
        )
    }

    override fun initZygote(startupParam: StartupParam) {
        modRes = XModuleResources.createInstance(startupParam.modulePath, null)
    }

    companion object {
        const val TAG: String = "OnyxTweaks"

        var modRes: XModuleResources? = null
    }
}
