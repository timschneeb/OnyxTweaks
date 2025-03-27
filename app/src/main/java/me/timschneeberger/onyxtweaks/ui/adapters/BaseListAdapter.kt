package me.timschneeberger.onyxtweaks.ui.adapters

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

abstract class BaseListAdapter<T>: RecyclerView.Adapter<BaseListAdapter<T>.ViewHolder>(), Filterable {
    abstract inner class ViewHolder(rootView: ViewGroup): RecyclerView.ViewHolder(rootView), View.OnClickListener {

        init { rootView.setOnClickListener(this) }

        protected val titleView = rootView.findViewById<TextView>(android.R.id.title)
        protected val summaryView = rootView.findViewById<TextView>(android.R.id.summary)
        protected val iconView = rootView.findViewById<ImageView>(android.R.id.icon)

        abstract var data: T?

        override fun onClick(v: View) {
            data?.let {
                onItemClickListener?.onItemClick(it)
            }
        }
    }

    open var dataList: List<T> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            filteredDataList = value
            notifyDataSetChanged()
        }

    protected var filteredDataList: List<T> = emptyList()

    override fun getItemCount(): Int = filteredDataList.size

    override fun onBindViewHolder(holder: BaseListAdapter<T>.ViewHolder, position: Int) {
        if(position >= itemCount)
            return
        holder.data = filteredDataList[position]
    }

    abstract fun filterByString(item: T, constraint: CharSequence?): Boolean

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                filteredDataList =
                    if (charString.isEmpty())
                        dataList
                    else {
                        val filteredList = arrayListOf<T>()
                        dataList
                            .filter {
                                filterByString(it, constraint)
                            }
                            .forEach(filteredList::add)
                        filteredList
                    }
                return FilterResults().apply { values = filteredDataList }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredDataList = if (results?.values == null)
                    emptyList()
                else
                    results.values as List<T>
                notifyDataSetChanged()
            }
        }
    }

    fun interface OnItemClickListener<T> {
        fun onItemClick(item: T)
    }

    private var onItemClickListener: OnItemClickListener<T>? = null

    fun setOnItemClickListener(listener: OnItemClickListener<T>) {
        this.onItemClickListener = listener
    }
}