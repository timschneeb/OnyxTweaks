package me.timschneeberger.onyxtweaks.ui.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class AppItemViewModel : ItemViewModel<AppInfo>()
class ActivityItemViewModel : ItemViewModel<ActivityInfo>()

open class ItemViewModel<T> : ViewModel() {
    private val mutableSelectedItem = MutableLiveData<T>()
    val selectedItem: LiveData<T> get() = mutableSelectedItem

    fun selectItem(item: T) {
        mutableSelectedItem.value = item!!
    }
}