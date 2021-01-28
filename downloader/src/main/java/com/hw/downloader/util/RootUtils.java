package com.hw.downloader.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @author hewei(David)
 * @date 2021/1/28  4:00 PM
 * @Copyright ©  Shanghai Xinke Digital Technology Co., Ltd.
 * @description
 */

public class RootUtils {
    public static final String[] SU_BINARY_DIRS = {
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin"
    };

    /**
     * 检查设备是否root
     *
     * @return
     */
    public static boolean checkRoot() {
        boolean isRoot = false;
        try {
            for (String dir : SU_BINARY_DIRS) {
                File su = new File(dir, "su");
                if (su.exists()) {
                    isRoot = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
        return isRoot;
    }

    private static void closeIO(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
        }
    }

    /**
     * 运行root命令
     *
     * @return
     */
    @SuppressLint("LogUtilsNotUsed")
    public static boolean runRootCmd(String cmd) {
        boolean grandted;
        DataOutputStream outputStream = null;
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(process.getOutputStream());
            outputStream.writeBytes(cmd + "\n");
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            grandted = true;

            String msg = reader.readLine();
            if (msg != null) {
                Log.e(RootUtils.class.getSimpleName(), msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            grandted = false;

            closeIO(outputStream);
            closeIO(reader);
        }
        return grandted;
    }

    public static boolean installPkg(String packageName, String apkPath) {
        return runRootCmd("pm install -i " + packageName + " --user 0 " + apkPath);
    }

    /**
     * 为app申请root权限
     *
     * @param context
     * @return
     */
    public static boolean grantRoot(Context context) {
        return runRootCmd("chmod 777 " + context.getPackageCodePath());
    }
}


