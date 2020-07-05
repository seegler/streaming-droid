package com.slabs.android.action;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public abstract class PermittedActivityAction {
    private int permissionRequestId;
    protected String[] permissions;

    protected PermittedActivityAction(int permissionRequestId, String[] permissions){
        this.permissionRequestId = permissionRequestId;
        this.permissions = permissions;
    }
    protected int getPermissionRequestId(){
        return permissionRequestId;
    }

    protected boolean verifyAndRequestPermisssions(Activity activity){
        for(String p : permissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{p}, permissionRequestId);
                return false;
            }
        }
        return true;
    }
    public void activityAction(Activity activity){
        if(!verifyAndRequestPermisssions(activity))
                return;
        doActivityAction(activity);
    }

    protected abstract void doActivityAction(Activity activity);
}
