package me.timschneeberger.onyxtweaks.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.PreferenceDeletableBinding


open class DeletablePreference(
    mContext: Context, attrs: AttributeSet?,
    defStyleAttr: Int, defStyleRes: Int,
) : Preference(mContext, attrs, defStyleAttr, defStyleRes) {

    var onDeleteClicked: ((DeletablePreference) -> Unit)? = null

    @JvmOverloads
    constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyle: Int = androidx.preference.R.attr.preferenceStyle,
    ) : this(context, attrs, defStyle, 0) {
        layoutResource = R.layout.preference_deletable
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val binding = PreferenceDeletableBinding.bind(holder.itemView)
        binding.deleteButton.setOnClickListener { v -> onDeleteClicked?.invoke(this) }
    }
}
