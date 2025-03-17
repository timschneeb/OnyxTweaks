package me.timschneeberger.onyxtweaks.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R


open class IconPreference(
    mContext: Context, attrs: AttributeSet?,
    defStyleAttr: Int, defStyleRes: Int,
) : Preference(mContext, attrs, defStyleAttr, defStyleRes) {

    @JvmOverloads
    constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyle: Int = androidx.preference.R.attr.preferenceStyle,
    ) : this(context, attrs, defStyle, 0) {
        layoutResource = R.layout.preference_icon
    }
}
