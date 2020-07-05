package com.slabs.android.log;

import android.util.Log;

public class LogUtil {

    public static void e(String t, String p, Object... args){
        Log.e(t, String.format(p, args));
    }
    public static void i(String t, String p, Object... args){
        Log.i(t, String.format(p, args));
    }
    public static void d(String t, String p, Object... args){
        Log.d(t, String.format(p, args));
    }
    public static void e(String t, Throwable th, String p, Object... args){
        Log.e(t, String.format(p, args),th);
    }

    public static void i(String t, Throwable th, String p, Object... args){
        Log.i(t, String.format(p, args),th);
    }
    public static void d(String t, Throwable th, String p, Object... args){
        Log.d(t, String.format(p, args),th);
    }

}
