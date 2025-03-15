package me.timschneeberger.onyxtweaks.ui.utils

import android.content.res.Resources
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import me.timschneeberger.onyxtweaks.utils.SdkCheck

fun View.setBackgroundFromAttribute(@AttrRes attrRes: Int) {
    val a = TypedValue()
    context.theme.resolveAttribute(attrRes, a, true)
    if (SdkCheck.isQ && a.isColorType) {
        setBackgroundColor(a.data)
    } else {
        background = ResourcesCompat.getDrawable(context.resources, a.resourceId, context.theme)
    }
}

fun String.asHtml(): Spanned = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)

/** Converts to px from dp using the system's density. */
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
