package com.slabs.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;


public class AndroidHelper {

    private static final String TAG = AndroidHelper.class.getName();

    public static final Activity getActivity(Context context) {
        while (!(context instanceof Activity)) {
            if (!(context instanceof ContextWrapper)) {
                context = null;
            }
            ContextWrapper contextWrapper = (ContextWrapper) context;
            if (contextWrapper == null) {
                return null;
            }
            context = contextWrapper.getBaseContext();
            if (context == null) {
                return null;
            }
        }
        return (Activity) context;
    }



}
