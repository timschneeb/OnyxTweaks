package me.timschneeberger.onyxtweaks.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.PreferenceAppiconBinding
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.getAppName

class AppIconPreference : Preference {
    @Suppress("unused")
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
        defStyleRes: Int = 0,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        layoutResource = R.layout.preference_appicon
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val drawable = context.applicationInfo.loadIcon(context.packageManager)
        val binding = PreferenceAppiconBinding.bind(holder.itemView)
        binding.preferenceAppicon.setImageDrawable(drawable)
        binding.title.text = context.getAppName()
    }
}