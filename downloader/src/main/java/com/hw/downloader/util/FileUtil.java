package com.hw.downloader.util;

import android.content.Context;

import java.io.File;

/**
 * @author hewei(David)
 * @date 2021/1/28  3:16 PM
 * @Copyright ©  Shanghai Xinke Digital Technology Co., Ltd.
 * @description
 */

public class FileUtil {

    public static String getBasePath(Context context) {
        if (context == null) {
            return null;
        }

        String path = "/sdcard/";
        try {
            File dataDir = context.getApplicationContext().getExternalFilesDir(null);// 获取外部存储空间
            if (dataDir == null) {
                // 获取内部存储空间
                dataDir = context.getApplicationContext().getFilesDir();
            }

            if (dataDir != null) {
                return dataDir.getAbsolutePath() + File.separator;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

}
