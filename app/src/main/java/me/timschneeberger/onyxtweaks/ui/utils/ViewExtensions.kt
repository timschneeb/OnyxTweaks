package me.timschneeberger.onyxtweaks.ui.utils

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import me.timschneeberger.onyxtweaks.utils.SdkCheck
import kotlin.reflect.KClass
import kotlin.reflect.cast

fun View.setBackgroundFromAttribute(@AttrRes attrRes: Int) {
    val a = TypedValue()
    context.theme.resolveAttribute(attrRes, a, true)
    if (SdkCheck.is10 && a.isColorType) {
        setBackgroundColor(a.data)
    } else {
        background = ResourcesCompat.getDrawable(context.resources, a.resourceId, context.theme)
    }
}

fun <T : View> ViewGroup.getViewsByType(tClass: KClass<T>): List<T> {
    return mutableListOf<T?>().apply {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            (child as? ViewGroup)?.let {
                addAll(child.getViewsByType(tClass))
            }
            if (tClass.isInstance(child))
                add(tClass.cast(child))
        }
    }.filterNotNull()
}