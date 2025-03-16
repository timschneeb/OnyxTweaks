package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass
import me.timschneeberger.onyxtweaks.utils.invokeOriginalMethod

@TargetPackages(LAUNCHER_PACKAGE)
class DesktopGridSize : ModPack() {
    private var isInitializing = true

    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        return

        /*

        val columns = 3
        val rows = 2

        getClass("com.onyx.common.applications.model.AppSettings").apply {
            methodFinder()
                .firstByName("getDesktopFixCol")
                .replaceWithConstant(columns)
            methodFinder()
                .firstByName("getDesktopFixRow")
                .replaceWithConstant(rows)

            methodFinder()
                .firstByName("calDesktopColumnCount")
                .replaceWithConstant(columns)
            methodFinder()
                .firstByName("calDesktopRowCount")
                .replaceWithConstant(rows)

            methodFinder()
                .firstByName("getDesktopColumnCount")
                .replaceWithConstant(columns)
            methodFinder()
                .firstByName("getDesktopRowCount")
                .replaceWithConstant(rows)

            methodFinder()
                .firstByName("getDockColumnCount")
                .replaceWithConstant(4)
            methodFinder()
                .firstByName("getDockRowCount")
                .replaceWithConstant(1)

        }*/


        getClass("com.onyx.common.applications.model.AppSettings").apply {
            methodFinder()
                .firstByName("isAppInit")
                .createHook {
                    replace { param ->
                        if (isInitializing) {
                            return@replace false
                        }

                        return@replace param.invokeOriginalMethod()
                    }
                }

            methodFinder()
                .filterByParamTypes(Boolean::class.java)
                .firstByName("setAppInit")
                .createBeforeHook { param ->
                    if(param.args[0] == true) {
                        isInitializing = false
                    }
                }
        }


        // Fix out-of-bounds items after downsizing the grid
        /*getClass("com.onyx.common.applications.utils.HomePageHelper").apply {
            methodFinder()
                .filterStatic()
                .filterByParamTypes(getClass("com.onyx.common.applications.utils.item.ItemPosition"))
                .firstByName("loadAllItem")
                .createAfterHook { param ->
                    if (param.args[0].toString() != "Desktop")
                        return@createAfterHook

                    XposedBridge.log("Fixing out-of-bounds items; input type: " + (param.result as List<*>)[0]!!.javaClass)

                    param.result = ListWrapper(param.result, AppItemModelWrapper::class).run {
                        val outOfBoundsItems = items
                            .filter { it.x >= rows || it.y >= columns }
                            .toMutableList()
                       // outOfBoundsItems.renderToLog("OOB items")

                        // Find empty spots on each page and insert.
                        for (page in 0 until items.maxOf { it.page ?: 0 }) {
                            XposedBridge.log("Checking page $page")

                            val pageItems = items.filter { it.page == page }
                            val emptySpots = (0 until columns).flatMap { x ->
                                (0 until rows).map { y ->
                                    x to y
                                }
                            }.filterNot { (x, y) -> pageItems.any { it.x == x && it.y == y } }

                            emptySpots.forEach { (x, y) ->
                                outOfBoundsItems.removeFirstOrNull()?.let { oobItem ->
                                    items.find { it.id == oobItem.id }?.apply {
                                        this.page = page
                                        this.x = x
                                        this.y = y
                                    }?.save()
                                    XposedBridge.log("Inserted OOB item at $x, $y into space in page $page")
                                }
                            }
                        }

                        XposedBridge.log("Remaining OOB items: ${outOfBoundsItems.size}")
                        if (outOfBoundsItems.isNotEmpty()) {
                            var currentPage = items.maxOf { it.page ?: 0 } + 1
                            val emptySpots = (0 until columns).flatMap { x ->
                                (0 until rows).map { y ->
                                    x to y
                                }
                            }
                            while (outOfBoundsItems.isNotEmpty()) {
                                XposedBridge.log("Creating new page: $currentPage")
                                emptySpots.forEach { (x, y) ->
                                    outOfBoundsItems.removeFirstOrNull()?.let { oobItem ->
                                        items.find { it.id == oobItem.id }?.apply {
                                            this.page = currentPage
                                            this.x = x
                                            this.y = y
                                        }?.save()
                                        XposedBridge.log("Inserted OOB item at $x, $y in page $currentPage")
                                    }
                                }
                                currentPage++
                            }
                        }
                        XposedBridge.log("Fixing out-of-bounds items; FINAL type: " + (param.result as List<*>)[0]!!.javaClass)

                        unwrap()
                    }
                    //param.result.renderToLog("UNWRAPPED")
                }
        }*/
    }
}