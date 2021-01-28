package com.hw.downloader.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.hw.downloader.DownloadUtils;
import com.hw.downloader.util.FileUtil;

import java.io.File;

/**
 * @author hewei(David)
 * @date 2021/1/28  3:48 PM
 * @Copyright ©  Shanghai Xinke Digital Technology Co., Ltd.
 * @description
 */

public class UpdateReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
//            Logger.i("install" + "安装了完成:" + "包名的程序");

            String APK_FILE_PATH = FileUtil.getBasePath(context) + DownloadUtils.APK_NAME;
            Log.e("install", "install-111-APK_FILE_PATH-"+APK_FILE_PATH);
            File file = new File(APK_FILE_PATH);
            if (file.exists() && file.isFile()) {
//                Logger.i("install" + "删除了文件:" + APK_FILE_PATH);
                file.delete();
            } else {
//                Logger.i("install" + "文件不存在:" + APK_FILE_PATH);
            }
//            SPUtils.getInstance(context).saveData(HAS_UPDATE, HAS_UPDATE);
            Log.e("install", "install-111-finish-"+intent.getDataString());
            Intent intent1 = context.getPackageManager().getLaunchIntentForPackage(intent.getDataString());
            context.startActivity(intent1);
        }
        //接收安装广播
        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            String packageName = intent.getDataString();
            Log.e("install", "install--add-"+packageName);

        }
        //接收卸载广播
        if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
            String packageName = intent.getDataString();
            Log.e("install", "install--remove-"+packageName);
        }
    }
}

