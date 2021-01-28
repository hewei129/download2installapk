package com.hw.downloader.bizs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author hewei(David)
 * @date 2021/1/20  4:30 PM
 * @Copyright ©  Shanghai Xinke Digital Technology Co., Ltd.
 * @description
 */

public class ApkUtil {

    /**
     * 打开安装包
     *
     * @param mContext
     * @param apkFile
     */
    public static void openAPKFile(Activity mContext, File apkFile) {
//        DataEmbeddingUtil.dataEmbeddingAPPUpdate(fileUri);
        // 核心是下面几句代码
        if (null != apkFile && apkFile.exists()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
//                File apkFile = new File(filePath);
                //兼容7.0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileProvider", apkFile);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    //兼容8.0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        boolean hasInstallPermission = mContext.getPackageManager().canRequestPackageInstalls();
                        if (!hasInstallPermission) {
//                            ToastUtil.makeText(MyApplication.getContext(), MyApplication.getContext().getString(R.string.string_install_unknow_apk_note), false);
                            startInstallPermissionSettingActivity(mContext);
                            return;
                        }
                    }
                } else {
                    intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                if (mContext.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                    mContext.startActivity(intent);
                }
            } catch (Throwable e) {
                e.printStackTrace();
//                DataEmbeddingUtil.dataEmbeddingAPPUpdate(e.toString());
//                CommonUtils.makeEventToast(MyApplication.getContext(), MyApplication.getContext().getString(R.string.download_hint), false);
            }
        }
    }

    /**
     * 跳转到设置-允许安装未知来源-页面
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void startInstallPermissionSettingActivity(Activity mContext)
    {
        //注意这个是8.0新API
//        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //注意这个是8.0新API
        Uri packageURI = Uri.parse("package:" +mContext.getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityForResult(intent, 1111);

    }


    private static final String TAG = "test-test";

    private static final int TIME_OUT = 60 * 1000;

    private static String[] SH_PATH = {
            "/system/bin/sh",
            "/system/xbin/sh",
            "/system/sbin/sh"
    };

    public static boolean executeInstallCommand(String filePath) {
        String command = "pm install -r " + filePath;
        Process process = null;
        DataOutputStream os = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        try {
            process = runWithEnv(getSuPath(), null);
            if (process == null) {
                return false;
            }

            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("echo \"rc:\" $?\n");
            os.writeBytes("exit\n");
            os.flush();

            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }

            // Handle a requested timeout, or just use waitFor() otherwise.
            if (TIME_OUT > 0) {
                long finish = System.currentTimeMillis() + TIME_OUT;
                while (true) {
                    Thread.sleep(300);
                    if (!isProcessAlive(process)) {
                        break;
                    }

                    if (System.currentTimeMillis() > finish) {
                        Log.w(TAG, "Process doesn't seem to stop on it's own, assuming it's hanging");
                        // Note: 'finally' will call destroy(), but you might still see zombies.
                        return true;
                    }
                }
            } else {
                process.waitFor();
            }

            // In order to consider this a success, we require to things: a) a proper exit value, and ...
            if (process.exitValue() != 0) {
                return false;
            }

            return true;

        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to run command, " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.w(TAG, "Failed to run command, " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Log.w(TAG, "Failed to run command, " + e.getMessage());
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (process != null) {
                try {
                    // Yes, this really is the way to check if the process is still running.
                    process.exitValue();
                } catch (IllegalThreadStateException e) {
                    process.destroy();
                }
            }
        }
    }

    private static Process runWithEnv(String command, String[] customEnv) throws IOException {
        List<String> envList = new ArrayList<String>();
        Map<String, String> environment = System.getenv();
        if (environment != null) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                envList.add(entry.getKey() + "=" + entry.getValue());
            }
        }

        if (customEnv != null) {
            for (String value : customEnv) {
                envList.add(value);
            }
        }

        String[] arrayEnv = null;
        if (envList.size() > 0) {
            arrayEnv = new String[envList.size()];
            for (int i = 0; i < envList.size(); i++) {
                arrayEnv[i] = envList.get(i);
            }
        }

        Process process = Runtime.getRuntime().exec(command, arrayEnv);
        return process;
    }

    /**
     * Check whether a process is still alive. We use this as a naive way to implement timeouts.
     */
    private static boolean isProcessAlive(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /** Get the SU file path if it exist */
    private static String getSuPath() {
        for (String p : SH_PATH) {
            File sh = new File(p);
            if (sh.exists()) {
                return p;
            }
        }
        return "su";
    }

}
