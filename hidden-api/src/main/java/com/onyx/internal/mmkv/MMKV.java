package com.onyx.internal.mmkv;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import kotlin.Deprecated;

public class MMKV implements SharedPreferences, SharedPreferences.Editor {
    // Onyx-related stubs
    public String[] allKeys() { throw new UnsupportedOperationException("Stub!"); }

    public int getValueActualSize(String key) { throw new UnsupportedOperationException("Stub!"); }

    // Overrides for SharedPreferences

    @Override
    @Deprecated(message = "Not supported by MMKV. Use allKeys() instead")
    public Map<String, ?> getAll() {
        return Collections.emptyMap();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return "";
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return Collections.emptySet();
    }

    @Override
    public int getInt(String key, int defValue) {
        return 0;
    }

    @Override
    public long getLong(String key, long defValue) {
        return 0;
    }

    @Override
    public float getFloat(String key, float defValue) {
        return 0;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return false;
    }

    @Override
    public boolean contains(String key) {
        return false;
    }

    @Override
    public Editor edit() {
        return null;
    }

    @Override
    @Deprecated(message = "Not supported by MMKV")
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    @Deprecated(message = "Not supported by MMKV")
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    public Editor putString(String key, @Nullable String value) {
        return null;
    }

    @Override
    public Editor putStringSet(String key, @Nullable Set<String> values) {
        return null;
    }

    @Override
    public Editor putInt(String key, int value) {
        return null;
    }

    @Override
    public Editor putLong(String key, long value) {
        return null;
    }

    @Override
    public Editor putFloat(String key, float value) {
        return null;
    }

    @Override
    public Editor putBoolean(String key, boolean value) {
        return null;
    }

    @Override
    public Editor remove(String key) {
        return null;
    }

    @Override
    public Editor clear() {
        return null;
    }

    @Override
    public boolean commit() {
        return false;
    }

    @Override
    public void apply() {

    }
}