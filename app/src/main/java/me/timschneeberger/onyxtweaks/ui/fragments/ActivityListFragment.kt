package me.timschneeberger.onyxtweaks.ui.fragments

import android.content.pm.PackageManager
import androidx.fragment.app.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import me.timschneeberger.onyxtweaks.ui.adapters.ActivityListAdapter
import me.timschneeberger.onyxtweaks.ui.model.ActivityInfo
import me.timschneeberger.onyxtweaks.ui.model.ActivityItemViewModel


class ActivityListFragment(private val packageName: String) : BaseListFragment<ActivityInfo, ActivityListAdapter>() {
    override lateinit var adapter: ActivityListAdapter

    private val viewModel: ActivityItemViewModel by viewModels({requireParentFragment()})

    override var dataList: List<ActivityInfo>
        get() = adapter.dataList
        set(value) { adapter.dataList = value }

    override fun createAdapter() = ActivityListAdapter().apply {
        setOnItemClickListener {
            viewModel.selectItem(it)
            dismiss()
        }
    }

    override suspend fun createList(): List<ActivityInfo> {
        return withContext(Dispatchers.IO) {
            requireContext().packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
            ).run {
                activities?.mapIndexed { idx, it ->
                    if (idx % 5 == 0) yield()

                    ActivityInfo(
                        it.packageName,
                        it.name,
                        it.loadLabel(requireContext().packageManager).toString(),
                    )
                }?.sortedBy(ActivityInfo::activityClass) ?: emptyList()
            }
        }
    }
}