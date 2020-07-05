package com.slabs.android.streamaw.stream;

import android.content.SharedPreferences;

public class AACEncoder {

    SharedPreferences mPreferences;
    private void reset() {

    }

    public static AACEncoder create(SharedPreferences prefs){
        AACEncoder encode = new AACEncoder(prefs);
        //encode.printEndingPreferecnes();
        return encode;
    }
    private AACEncoder(SharedPreferences prefs){
        mPreferences =prefs;
        reset();
    }
}
