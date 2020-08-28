package com.SMU.DevSec;

import android.content.Context;

public class AppInfo {
    public int permisson;
    Context context;
    //private Drawable icon;// 应用图标
    private String appName;// 应用名称
    private String packageName;// 包名
    private  String path; //包路径

    public AppInfo(String appName, String packageName, String path, int permisson) {
        this.context = context;
        this.appName = appName;
        this.packageName = packageName;
        this.path = path;
        this.permisson = permisson;
        //this.permisson = get_permissons_type();
    }

    //private int get_permissons_type()

    public AppInfo() {
        super();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String packageName) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "AppInfo [appName=" + appName
                + ", packageName=" + packageName +",path="+path+ "]";
    }

}
