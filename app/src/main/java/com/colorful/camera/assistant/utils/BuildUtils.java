package com.colorful.camera.assistant.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Created by liwei on 2015/11/25.
 * <p/>
 * 此类提供获取编译信息的静态方法
 */
public class BuildUtils {

    private BuildUtils() {
    }

    public static int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static int getVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public static String getSDKVersionName() {
        return Build.VERSION.RELEASE;
    }

    public static boolean thanJellyBeanMr2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean thanKitkat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean thanLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean thanMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean lessMarshmallow() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }

    public static boolean thanNougat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static String getPhoneModel() {
        return Build.MODEL;
    }


}
