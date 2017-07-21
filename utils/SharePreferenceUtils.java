package com.example.TestTaxi.Hy_Bluetooth.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 缓存数据工具类
 */
public class SharePreferenceUtils {

    private final static String SHARE_PREFERENCE = "com.example.testtaxi.SHARE_PREFERENCE";
    public final static String SHARE_REFRESH_IS_MANUAL = "com.csym.bluetoothlib.SHARE_REFRESH_IS_MANUAL";//S是否为手动断开蓝牙


    private static SharePreferenceUtils INSTANCE = null;
    private SharedPreferences preferences;
    private static Context context;

    private SharePreferenceUtils() {
        preferences = context.getSharedPreferences(SHARE_PREFERENCE, 0);
    }

    public static SharePreferenceUtils getInstance(Context context) {
        SharePreferenceUtils.context = context;
        if (INSTANCE == null) {
            synchronized (SharePreferenceUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SharePreferenceUtils();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 根据唯一标识key删除数据
     *
     * @param key 唯一标识
     */
    public void remove(String key) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }

    /**
     * 获取boolean类型的数据
     *
     * @param key 唯一标识
     * @return boolean 默认false
     */
    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    /**
     * 获取String 类型数据
     *
     * @param key 唯一标识
     * @return String 默认null
     */
    public String getString(String key) {
        return preferences.getString(key, null);
    }

    /**
     * 获取int类型数据
     *
     * @param key 唯一标识
     * @return int 默认-1
     */
    public int getInt(String key) {
        return preferences.getInt(key, -1);
    }

    /**
     * 保存数据，可以根据相应的类型扩展
     *
     * @param key   唯一标识
     * @param value 需要保存的数据
     */
    public void save(String key, Object value) {
        SharedPreferences.Editor editor = preferences.edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else {
            Log.e(getClass().getCanonicalName(), "Unexpected type:" + key + "=" + value);
        }
        editor.apply();
    }
}
