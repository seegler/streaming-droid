package com.slabs.android.action;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import com.slabs.android.log.LogUtil;

import java.util.HashMap;
import java.util.Map;

public class PermissionRequiredActivityHelper {
    private static final String TAG = PermissionRequiredActivityHelper.class.getName();

    private Map<String, Integer> permissionMap =new HashMap<>();
    Activity activity;
    public PermissionRequiredActivityHelper(Activity activity, String... permissions){
        this.activity = activity;
        for( int i=0; i<permissions.length;i++){
            permissionMap.put(permissions[i], i);
        }
    }

    public boolean verifyAndRequestPermisssion(String p){
        Integer id = permissionMap.get(p);
        if(id == null){
            LogUtil.d(TAG," Permisssion %s not listed", p);
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{p}, id); //some id
            return false;
        }
        return true;
    }
    public boolean verifyAndRequestPermisssions(){
        for(String p : permissionMap.keySet()) {
            if(verifyAndRequestPermisssion(p)) continue;
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (grantResults.length < 1) {
            Toast.makeText(activity, "No grant result received", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(activity, "Permission granted result received " + requestCode, Toast.LENGTH_SHORT).show();
        }
        //verifyAndRequestPermisssions();
    }
}
