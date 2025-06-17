package com.bfr.helloworld.utils;

import android.util.Log;

/**
 * Utilitaire de logging centralisé pour l'application
 */
public class Logger {
    private static final String APP_TAG = "MathQuizBuddy";

    public static void i(String tag, String message) {
        Log.i(APP_TAG + "_" + tag, message);
    }

    public static void d(String tag, String message) {
        Log.d(APP_TAG + "_" + tag, message);
    }

    public static void w(String tag, String message) {
        Log.w(APP_TAG + "_" + tag, message);
    }

    public static void e(String tag, String message) {
        Log.e(APP_TAG + "_" + tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(APP_TAG + "_" + tag, message, throwable);
    }
}
