package com.hw.downloader;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.hw.downloader.bizs.ApkUtil;
import com.hw.downloader.bizs.DLBroadcastReceiver;
import com.hw.downloader.bizs.DLError;
import com.hw.downloader.bizs.DLProgressUIMode;
import com.hw.downloader.bizs.DLService;
import com.hw.downloader.util.FileUtil;
import com.hw.downloader.util.RootUtils;

import java.io.File;

import static com.hw.downloader.bizs.DLError.ERROR_REPEAT_URL;
import static com.hw.downloader.bizs.DLService.EXTRA_KEY_BOL_ENABLE_NOTIFICATION;
import static com.hw.downloader.bizs.DLService.EXTRA_KEY_STR_DIR_FULL_PATH;
import static com.hw.downloader.bizs.DLService.EXTRA_KEY_STR_URL;
import static com.hw.downloader.bizs.DLService.registerReceiver;
import static com.hw.downloader.bizs.DLService.unregisterReceiver;


/**
 *
 */
@SuppressWarnings("unused")
public class DownloadUtils {
    public final static int INSTALL_NORMAL = 1;
    public final static int INSTALL_SILENT = 2;
    public static String APK_NAME = "/app.apk";
    private static void showDownloadProgressDialog(final Context context, String url, String dirPath) {
        //显示对话框
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("下载中…");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        registerReceiver(context, new DLBroadcastReceiver() {
            @Override
            public void onError(final int status, final String error) {
                super.onError(status, error);
                //重复的下载地址,则不取消当前正在下载的通知栏.
                if (status == ERROR_REPEAT_URL)
                    return;
                unregisterReceiver(context, this);
                progressDialog.dismiss();
            }

            @Override
            public void onFinish(final File file) {
                super.onFinish(file);
                unregisterReceiver(context, this);
                progressDialog.dismiss();
            }

            @Override
            public void onPrepare() {
                super.onPrepare();
            }

            @Override
            public void onProgress(final int progress) {
                super.onProgress(progress);
                progressDialog.setProgress(progress / 1024);
            }

            @Override
            public void onStart(final String fileName, final String realUrl, final int fileLength) {
                super.onStart(fileName, realUrl, fileLength);
                progressDialog.setIndeterminate(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    progressDialog.setProgressNumberFormat("%1$s KB/%2$s KB");
                progressDialog.setMax(fileLength / 1024);
                progressDialog.setProgress(0);
            }

            @Override
            public void onStop(final int progress) {
                super.onStop(progress);
                unregisterReceiver(context, this);
                progressDialog.dismiss();
            }
        });
    }

    public static boolean start(Context context, String url, String dirPath, DLProgressUIMode uiMode) {
        Intent intent = new Intent(context, DLService.class);
        intent.putExtra(EXTRA_KEY_STR_URL, url);
        intent.putExtra(EXTRA_KEY_STR_DIR_FULL_PATH, dirPath);
        switch (uiMode) {
            case None:
                break;
            case ProgressDialog:
                showDownloadProgressDialog(context, url, dirPath);
                break;
            case Notification:
                intent.putExtra(EXTRA_KEY_BOL_ENABLE_NOTIFICATION, true);
                break;
            case ProgressDialogAndNotification:
                intent.putExtra(EXTRA_KEY_BOL_ENABLE_NOTIFICATION, true);
                showDownloadProgressDialog(context, url, dirPath);
                break;
        }
        context.startService(intent);
        return false;
    }

    /**
     * 默认下载到系统默认的SDCard的Download目录下.
     */
    public static boolean start(Context context, String url, DLProgressUIMode uiMode) {
        return start(context, url, FileUtil.getBasePath(context), uiMode);
    }


    public static void deleteFile(String filePath) {

        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }


    /**
     * 开始下载APK文件,并在下载完成后开始安装.
     */
    public static void startDownloadApkAndIntall(final Activity context, String url, int installApkType) {
        if (url == null) return;
//        APK_NAME = "/"+context.getPackageName()+ ".apk";
        deleteFile(FileUtil.getBasePath(context)+ APK_NAME);
        int code = PermissionUtils.requestAllManifestPermissionsIfNecessary(context);
        if (code != 0) {
            Toast.makeText(context, "请进入设置，确保应用权限启用...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean hasInstallPermission = context.getPackageManager().canRequestPackageInstalls();
            if (!hasInstallPermission) {
                ApkUtil.startInstallPermissionSettingActivity(context);
                return;
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
        }

        if (null == url || "".equals(url)) return;
        start(context, url, DLProgressUIMode.ProgressDialogAndNotification);
        registerReceiver(context, new DLBroadcastReceiver() {
            private void installApk(final Activity context, final File file) {
                if(installApkType == INSTALL_NORMAL)
                    ApkUtil.openAPKFile(context, file);
                else if(installApkType == INSTALL_SILENT) {
                    if (null != file && file.exists()) {
                        if(RootUtils.checkRoot()){
                            RootUtils.installPkg(context.getPackageName(), file.getAbsolutePath());
                        }else
                            ApkUtil.openAPKFile(context, file);
//                        ApkUtil.executeInstallCommand(file.getAbsolutePath());
                    }
                }
            }

            @Override
            public void onError(final int status, final String error) {
                super.onError(status, error);
                unregisterReceiver(context, this);
                switch (status) {
                    case DLError.ERROR_NOT_NETWORK:
                        showErrorMessageBox(context, "下载异常", "网络不通", false);
                        break;
                    case DLError.ERROR_CREATE_FILE:
                        showErrorMessageBox(context, "下载异常", "创建文件失败", false);
                        break;
                    case ERROR_REPEAT_URL:
                        Toast.makeText(context, "正在下载中…", Toast.LENGTH_SHORT).show();
                        break;
                    case DLError.ERROR_OPEN_CONNECT:
                        showErrorMessageBox(context, "下载异常", "请检查存储权限", false);
                        break;
                    default:
                        showErrorMessageBox(context, "下载异常", status + ":" + error, false);
                        break;
                }
            }

            @Override
            public void onFinish(final File file) {
                super.onFinish(file);
                unregisterReceiver(context, this);

                Toast.makeText(context, "下载成功,开始安装中……", Toast.LENGTH_SHORT).show();
                if (file.exists())
                    installApk(context, file);
            }

            @Override
            public void onStop(final int progress) {
                super.onStop(progress);
                unregisterReceiver(context, this);
            }

            /**
             * 显示一个错误对话框。
             *
             * @param needCloseOwner 是否需要点击按钮后，关闭当前窗体(owner)。
             */
            public void showErrorMessageBox(final Context owner, final String title, final String message, final boolean needCloseOwner) {
                new AlertDialog.Builder(owner)//
                        .setTitle(title)//
                        .setMessage(message)//
                        .setCancelable(false)//
                        .setIcon(android.R.drawable.ic_dialog_alert)//
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                // 出现错误了，直接结束当前窗体。
                                if (needCloseOwner)
                                    ((Activity) owner).finish();
                            }
                        })//
                        .create()//
                        .show();
            }
        });
    }
}
