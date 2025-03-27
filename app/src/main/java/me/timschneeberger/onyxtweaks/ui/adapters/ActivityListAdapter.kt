package me.timschneeberger.onyxtweaks.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.model.ActivityInfo

class ActivityListAdapter : BaseListAdapter<ActivityInfo>() {
    inner class ViewHolder(rootView: ViewGroup): BaseListAdapter<ActivityInfo>.ViewHolder(rootView) {
        override var data: ActivityInfo? = null
            set(value) {
                field = value
                value ?: return
                titleView.text = value.activityClass
                summaryView.text = value.activityName
                iconView.isVisible = false
            }
    }

    override var dataList: List<ActivityInfo> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            filteredDataList = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_list, parent, false) as ViewGroup
        )

    override fun filterByString(item: ActivityInfo, constraint: CharSequence?) =
        constraint?.let { item.activityClass.contains(it, true) } == true
}