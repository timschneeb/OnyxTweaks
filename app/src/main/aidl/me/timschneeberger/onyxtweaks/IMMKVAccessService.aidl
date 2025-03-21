// IMMKVAccessService.aidl
package me.timschneeberger.onyxtweaks;

interface IMMKVAccessService {
    String[] findDataStoresForPackage(String packageName);

    String open(String packageName, String mmapId); // returns handle
    String openSystem(); // returns handle
    void close(String handle);

    int getValueActualSize(String handle, String key);
    boolean contains(String handle, String key);
    void remove(String handle, String key);
    void sync(String handle);

    String[] allKeys(String handle);

    boolean getBoolean(String handle, String key);
    int getInt(String handle, String key);
    long getLong(String handle, String key);
    float getFloat(String handle, String key);
    // IPC cannot directly pass large data. Only use these methods for fast display/preview purposes
    String getTruncatedString(String handle, String key, int maxSize);
    String getTruncatedStringSet(String handle, String key, int maxSize);

    void putBoolean(String handle, String key, boolean value);
    void putInt(String handle, String key, int value);
    void putLong(String handle, String key, long value);
    void putFloat(String handle, String key, float value);

    // AIDL can only pass 1MB of data at a time, so we need to use ParcelFileDescriptor for large strings
    ParcelFileDescriptor getLargeString(String handle, String key);
    ParcelFileDescriptor getLargeStringSet(String handle, String key);
    void putLargeString(String handle, String key, in ParcelFileDescriptor valueFd);
    void putLargeStringSet(String handle, String key, in ParcelFileDescriptor valueFd);
}