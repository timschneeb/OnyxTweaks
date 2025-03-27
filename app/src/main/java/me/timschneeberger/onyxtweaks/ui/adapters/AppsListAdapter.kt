package me.timschneeberger.onyxtweaks.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.model.AppInfo

class AppsListAdapter: BaseListAdapter<AppInfo>() {
    inner class ViewHolder(rootView: ViewGroup): BaseListAdapter<AppInfo>.ViewHolder(rootView) {
        override var data: AppInfo? = null
            set(value) {
                field = value
                value ?: return
                titleView.text = value.appName
                summaryView.text = value.packageName
                iconView.setImageDrawable(value.icon)
            }
    }

    override var dataList: List<AppInfo> = emptyList()
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

    override fun filterByString(item: AppInfo, constraint: CharSequence?) =
        constraint?.let { item.appName.contains(it, true) } == true
}