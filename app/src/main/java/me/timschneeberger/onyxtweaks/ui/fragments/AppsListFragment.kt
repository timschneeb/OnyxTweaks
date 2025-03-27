package me.timschneeberger.onyxtweaks.ui.fragments

import android.content.pm.ApplicationInfo
import androidx.fragment.app.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import me.timschneeberger.onyxtweaks.ui.adapters.AppsListAdapter
import me.timschneeberger.onyxtweaks.ui.model.AppInfo
import me.timschneeberger.onyxtweaks.ui.model.AppItemViewModel
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getInstalledApplicationsCompat


class AppsListFragment() : BaseListFragment<AppInfo, AppsListAdapter>() {
    override lateinit var adapter: AppsListAdapter
    private val appCache = mutableMapOf<String, AppInfo>()

    private val viewModel: AppItemViewModel by viewModels({requireParentFragment()})

    override var dataList: List<AppInfo>
        get() = adapter.dataList
        set(value) { adapter.dataList = value }

    override fun createAdapter() = AppsListAdapter().apply {
        setOnItemClickListener {
            viewModel.selectItem(it)
            dismiss()
        }
    }

    override suspend fun createList(): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            requireContext().packageManager.getInstalledApplicationsCompat(0)
                .filterNot { (it.flags and ApplicationInfo.FLAG_INSTALLED) == 0 }
                .mapIndexed { idx, it ->
                    if (idx % 5 == 0) yield()

                    appCache.getOrPut(it.packageName) {
                        AppInfo(
                            it.loadLabel(requireContext().packageManager).toString(),
                            it.packageName,
                            it.loadIcon(requireContext().packageManager),
                            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                        )
                    }
                }
                .sortedWith(compareBy({ !it.isSystem }, { it.appName }))
        }
    }
}