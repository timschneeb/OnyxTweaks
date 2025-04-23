package me.timschneeberger.onyxtweaks.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreferenceCompat
import me.timschneeberger.onyxtweaks.R


class MaterialSwitchPreference : SwitchPreferenceCompat {
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceCompatStyle,
        defStyleRes: Int = 0
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        widgetLayoutResource = R.layout.preference_materialswitch
    }
}