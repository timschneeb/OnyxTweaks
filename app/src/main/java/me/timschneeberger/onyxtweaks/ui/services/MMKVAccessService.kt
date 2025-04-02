package me.timschneeberger.onyxtweaks.ui.services

import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.tencent.mmkv.MMKV
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.utils.CustomLogger
import me.timschneeberger.onyxtweaks.utils.ellipsize
import java.io.File

class MMKVAccessService : RootService() {
    var mmkvMap: HashMap<String, MMKV> = HashMap()

    init {
        Log.currentLogger = CustomLogger
        EzXHelper.setLogTag("OT/MMKVAccessService")
    }

    inner class MMKVAccessIPC : IMMKVAccessService.Stub() {
        override fun findDataStoresForPackage(packageName: String): Array<out String?>? {
            val launcherCtx = applicationContext.createPackageContext(packageName, 0)

            if(!launcherCtx.filesDir.exists()) {
                Log.e("Launcher files dir inaccessible. Check MagiskSU namespace isolation setting.")
                return arrayOf()
            }

            return File(launcherCtx.filesDir.absolutePath + "/mmkv")
                .listFiles()
                ?.filterNot { file -> file.name.endsWith(".crc") }
                ?.map { file -> file.name }
                ?.toTypedArray()
                ?: arrayOf()
        }

        override fun openSystem(): String? {
            Log.d("Initializing system_config MMKV")

            val path = "/onyxconfig/mmkv/"
            if(!File(path).exists()) {
                Log.e("System MMKV path inaccessible")
                return null
            }

            mmkvMap[SYSTEM_HANDLE] = MMKV.nameSpace(path).mmkvWithID("onyx_config", MMKV.MULTI_PROCESS_MODE)
            return SYSTEM_HANDLE
        }


        override fun open(packageName: String, mmapId: String): String? {
            Log.d("Initializing MMKV for package $packageName with mmapId $mmapId")

            val launcherCtx = applicationContext.createPackageContext(packageName, 0)

            val handle = "$packageName/$mmapId"
            val ns = MMKV.nameSpace(launcherCtx.filesDir.absolutePath + "/mmkv/")
            mmkvMap[handle] = ns.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE)

            if(!launcherCtx.filesDir.exists()) {
                Log.e("Launcher files dir inaccessible. Check MagiskSU namespace isolation setting.")
                return null
            }
            return handle
        }

        override fun close(handle: String) {
            mmkvMap[handle]?.close()
            mmkvMap.remove(handle)
        }

        override fun getValueActualSize(handle: String, key: String?) = mmkvMap[handle]!!.getValueActualSize(key)
        override fun contains(handle: String, key: String?) = mmkvMap[handle]!!.contains(key)
        override fun remove(handle: String, key: String?) = mmkvMap[handle]!!.removeValueForKey(key)
        override fun sync(handle: String) = mmkvMap[handle]!!.sync()

        override fun allKeys(handle: String) = mmkvMap[handle]!!.allKeys()

        override fun getBoolean(handle: String, key: String?) = mmkvMap[handle]!!.getBoolean(key, false)
        override fun getInt(handle: String, key: String?) = mmkvMap[handle]!!.getInt(key, 0)
        override fun getLong(handle: String, key: String?) = mmkvMap[handle]!!.getLong(key, 0L)
        override fun getFloat(handle: String, key: String?) = mmkvMap[handle]!!.getFloat(key, 0f)
        override fun getTruncatedString(handle: String?, key: String?, maxSize: Int): String? {
            return mmkvMap[handle]!!.getString(key, null)?.ellipsize(maxSize)
        }
        override fun getTruncatedStringSet(handle: String?, key: String?, maxSize: Int): String? {
            return mmkvMap[handle]!!.getStringSet(key, null)?.joinToString()?.ellipsize(maxSize)
        }

        override fun putBoolean(handle: String, key: String?, value: Boolean) { mmkvMap[handle]!!.putBoolean(key, value) }
        override fun putInt(handle: String, key: String?, value: Int) { mmkvMap[handle]!!.putInt(key, value) }
        override fun putLong(handle: String, key: String?, value: Long) { mmkvMap[handle]!!.putLong(key, value) }
        override fun putFloat(handle: String, key: String?, value: Float) { mmkvMap[handle]!!.putFloat(key, value) }

        override fun getLargeString(
            handle: String,
            key: String?
        ): ParcelFileDescriptor? {
            mmkvMap[handle]!!.getString(key, null).let { value ->
                Log.e("getting large string: ${value?.length}")

                return Parcel.obtain().apply {
                    writeString(value)
                }.run {
                    marshallToPipe()
                }
            }
        }

        override fun getLargeStringSet(
            handle: String,
            key: String?
        ): ParcelFileDescriptor? {
            mmkvMap[handle]!!.getStringSet(key, null).let { value ->
                return Parcel.obtain().apply {
                    writeStringList(value?.toList())
                }.run {
                    marshallToPipe()
                }
            }
        }

        override fun putLargeString(
            handle: String,
            key: String?,
            valueFd: ParcelFileDescriptor?
        ) {
            valueFd?.unmarshallFromPipe()?.let {
                mmkvMap[handle]!!.putString(key, it.readString())
                it.recycle()
            }
        }

        override fun putLargeStringSet(handle: String, key: String?, valueFd: ParcelFileDescriptor?) {
            valueFd?.unmarshallFromPipe()?.let {
                val list = mutableListOf<String>()
                it.readStringList(list)
                mmkvMap[handle]!!.putStringSet(key, list.toSet())
                it.recycle()
            }
        }
    }

    override fun onCreate() {
        Log.d("MMKVAccessService created")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d("MMKVAccessService bound")
        return MMKVAccessIPC()
    }

    override fun onDestroy() {
        Log.d("MMKVAccessService destroyed")
    }

    override fun onRebind(intent: Intent) {
        Log.d("MMKVAccessService rebound")
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d("MMKVAccessService unbound")
        return true
    }

    companion object {
        const val SYSTEM_HANDLE = "onyx_config"
    }
}


fun Parcel.marshallToPipe(): ParcelFileDescriptor {
    val raw = marshall()
    recycle()

    ParcelFileDescriptor.createPipe().let { (read, write) ->
        Log.i("Writing to pipe: ${raw.size}")

        CoroutineScope(Dispatchers.IO).launch {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use {
                for (i in 0 until raw.size step 4096) {
                    val end = if (i + 4096 > raw.size) raw.size else i + 4096
                    it.write(raw, i, end - i)
                }
            }
        }

        return read
    }
}

fun ParcelFileDescriptor.unmarshallFromPipe(): Parcel {
    ParcelFileDescriptor.AutoCloseInputStream(this).use { stream ->
        val raw = stream.readBytes()
        return Parcel.obtain().apply {
            unmarshall(raw, 0, raw.size)
            setDataPosition(0)
        }
    }
}