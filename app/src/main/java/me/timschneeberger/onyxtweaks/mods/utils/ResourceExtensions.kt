package me.timschneeberger.onyxtweaks.mods.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.Log

@SuppressLint("DiscouragedApi")
fun Resources.getResourceIdByName(name: String, type: String, packageName: String? = null) =
    getIdentifier(name, type, packageName ?: hostPackageName).let { drawableId ->
        if (drawableId == 0) {
            Log.ex("Resource $type/$name not found in $packageName")
            null
        }
        else {
            drawableId
        }
    }

fun Resources.getDrawableByName(name: String, packageName: String? = null) =
    getResourceIdByName(name, "drawable", packageName)?.let { drawableId ->
        ResourcesCompat.getDrawable(this, drawableId, null)
    }

fun Context.inflateLayoutByName(root: ViewGroup?, name: String, packageName: String? = null) =
    resources.getResourceIdByName(name, "layout", packageName)?.let { layoutId ->
        LayoutInflater.from(this).inflate(layoutId, root, false)
    }

fun Resources.getDimensionPxByName(name: String, packageName: String? = null) =
    getResourceIdByName(name, "dimen", packageName)?.let { dimenId ->
        getDimensionPixelSize(dimenId)
    }
