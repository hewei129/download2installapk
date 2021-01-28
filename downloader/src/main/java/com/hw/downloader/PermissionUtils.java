package com.hw.downloader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.AppOpsManagerCompat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * 检查权限的工具类
 * 一般在项目Activity基类里的onResume里调用{@link #requestAllManifestPermissionsIfNecessary(Activity)} 即可.
 * 权限判断Utils,在M 23 系统之后,支持检测全部被权限没有被打开的情况下,弹出授权框,并在被一直拒绝的情况下,自动弹到APP设置界面.
 * </pre>
 */
public class PermissionUtils {
    private static final String TAG = "PermissionUtils";

    /**
     * 所有危险的权限黑名单
     */
    private static final String[] PermissionBlackList =
            {
                    //group:android.permission-group.CONTACTS,
//                    Manifest.permission.WRITE_CONTACTS,
//                    Manifest.permission.GET_ACCOUNTS,
//                    Manifest.permission.READ_CONTACTS,

                    //group:android.permission-group.PHONE,
                    "android.permission.READ_CALL_LOG",
//                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE,
                    "android.permission.WRITE_CALL_LOG",
                    "android.permission.USE_SIP",
                    Manifest.permission.PROCESS_OUTGOING_CALLS,
                    "com.android.voicemail.permission.ADD_VOICEMAIL",

                    //group:android.permission-group.CALENDAR,
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,

                    //group:android.permission-group.CAMERA,
                    Manifest.permission.CAMERA,

                    //group:android.permission-group.SENSORS,
                    "android.permission.BODY_SENSORS",

                    //group:android.permission-group.LOCATION,
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION,

                    //group:android.permission-group.STORAGE,
//                    "android.permission.READ_EXTERNAL_STORAGE",
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,

                    //group:android.permission-group.MICROPHONE,
//                    Manifest.permission.RECORD_AUDIO,

                    ///group:android.permission-group.SMS,
//                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_WAP_PUSH,
//                    Manifest.permission.RECEIVE_MMS,
//                    Manifest.permission.RECEIVE_SMS,
//                    Manifest.permission.SEND_SMS,
                    "android.permission.READ_CELL_BROADCASTS"
            };

    private static Application mContext;
    private static String[] mCorrectPermissions;
    private static long mLastRequestTime;
    private static AppOpsManager appOpsManager;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static AppOpsManager getAppOpsManager() {
        if (appOpsManager == null)
            appOpsManager = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        return appOpsManager;
    }

    private static String[] getForbiddenPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return new String[0];
        List<String> lst = new ArrayList<>();
        for (String perm : getManifestPermissions())
            if (!hasPermission(mContext, perm))
                lst.add(perm);
        return lst.toArray(new String[lst.size()]);
    }

    /**
     * <pre>
     * 获取APP清单文件里定义的所有权限,并且过滤掉:
     * 1.非黑名单权限
     * 2.当前API的系统版本没有定义的新权限.(有可能APP清单文件里使用了一些高API系统才新增的危险权限.)
     * </pre>
     */
    @NonNull
    private synchronized static String[] getManifestPermissions() {
        if (mCorrectPermissions != null)
            return mCorrectPermissions;
        try {
            //获取每个API对应的系统版本里已知的权限列表,防止出现一些高版本新增的权限,而APP里的manifest没有定义的.就需要排除.
            Field[] fields = Manifest.permission.class.getFields();
            final List<String> mApiDependedPermissions = new ArrayList<>(fields.length);
            for (Field field : fields)
                try {
                    mApiDependedPermissions.add((String) field.get(""));
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Could not access field", e);
                }

            //获取APP清单文件里定义的需要的权限列表
            final PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), PackageManager.GET_PERMISSIONS);
            List<String> blackList = Arrays.asList(PermissionBlackList);
            final ArrayList<String> correctPermissions = new ArrayList<>(packageInfo.requestedPermissions.length);
            for (String permission : packageInfo.requestedPermissions) {
                if (mApiDependedPermissions.contains(permission)) {
                    //排除掉非黑名单权限(那些非危险权限系统一般都默认赋权了)
                    if (blackList.contains(permission))
                        correctPermissions.add(permission);
                } else
                    Log.e("APP清单文件定义了当前系统没有的权限", permission);
            }
            mCorrectPermissions = correctPermissions.toArray(new String[correctPermissions.size()]);
        } catch (Exception e) {
            mCorrectPermissions = new String[0];
        }
        return mCorrectPermissions;
    }

    public synchronized static boolean hasPermission(Context context, @NonNull String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;

        //在小米手机Android 6.0系统上测试'定位'权限时,单纯使用checkSelfPermission无法检测到正确的值,结合checkOp则刚好可以正确判断.
        int checkOp = getAppOpsManager().checkOp(AppOpsManagerCompat.permissionToOp(permission), Process.myUid(), context.getPackageName());
        return checkOp != AppOpsManager.MODE_IGNORED && ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 一般在自定义Application中的onCreate里初始化<br/>
     * 【不能在Application的构造函数里调用！】<br/>
     * 因为此时应用的String资源尚未初始化完毕，会报Null异常！
     */
    public static synchronized void init(final Application app) {
        //避免重复初始化
        if (mContext != null)
            return;
        mContext = app;
    }

    public static boolean isHadAllPermssions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        for (String perm : getManifestPermissions())
            if (!hasPermission(mContext, perm))
                return false;
        return true;
    }

    /**
     * <p>Intent to show an applications details page in (Settings) com.android.settings</p>
     *
     * @param packageName The package name of the application
     * @return the intent to open the application info screen.
     */
    public static Intent newAppDetailsIntent(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.setData(Uri.parse("package:" + packageName));
            return intent;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("pkg", packageName);
            return intent;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
        intent.putExtra("com.android.settings.ApplicationPkgName", packageName);
        return intent;
    }

    public static synchronized int requestAllManifestPermissionsIfNecessary(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return 0;
        if (isHadAllPermssions())
            return 0;
        long lastTime = SystemClock.elapsedRealtime();
        if (lastTime - mLastRequestTime < 250) {
            return 2;
        }
        mLastRequestTime = lastTime;
        ActivityCompat.requestPermissions(activity, getForbiddenPermissions(), 0);
        return 1;
    }
}
