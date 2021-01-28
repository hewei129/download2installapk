package com.hw.downloader.bizs;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import com.hw.downloader.interfaces.IDListener;

import java.io.File;


/**
 *
 */
@SuppressWarnings("unused")
public abstract class DLBroadcastReceiver extends BroadcastReceiver implements IDListener {
    public static final String EXTRA_KEY_STR_METHOD_NAME = "MethodName";
    public static final String EXTRA_KEY_ONERROR_ARG_STATUS = "onError参数1";
    public static final String EXTRA_KEY_ONERROR_ARG_ERROR = "onError参数2";
    public static final String EXTRA_KEY_ONFINISH_ARG_1_FILE_FULL_PATH = "onFinish方法参数1对应的绝对路径";
    public static final String EXTRA_KEY_INT_PROGRESS = "当前进度值";
    public static final String EXTRA_KEY_ONSTART_ARG_1_FILE_NAME = "onStart方法参数1";
    public static final String EXTRA_KEY_ONSTART_ARG_2_REAL_URL = "onStart方法参数2";
    public static final String EXTRA_KEY_ONSTART_ARG_3_FILE_LENGTH = "onStart方法参数3";

    @Override
    public void onError(final int status, final String error) {
    }

    @Override
    public void onFinish(final File file) {
    }

    @Override
    public void onPrepare() {
    }

    @Override
    public void onProgress(final int progress) {
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String url = intent.getStringExtra(DLService.EXTRA_KEY_STR_URL);
        final String dirFullPath = intent.getStringExtra(DLService.EXTRA_KEY_STR_DIR_FULL_PATH);
        final String methodName = intent.getStringExtra(EXTRA_KEY_STR_METHOD_NAME);
        switch (methodName) {
            case "onError":
                onError(intent.getIntExtra(EXTRA_KEY_ONERROR_ARG_STATUS, -1), intent.getStringExtra(EXTRA_KEY_ONERROR_ARG_ERROR));
                break;
            case "onFinish":
                onFinish(new File(intent.getStringExtra(EXTRA_KEY_ONFINISH_ARG_1_FILE_FULL_PATH)));
                break;
            case "onPrepare":
                onPrepare();
                break;
            case "onProgress":
                onProgress(intent.getIntExtra(EXTRA_KEY_INT_PROGRESS, 0));
                break;
            case "onStart":
                final String fileName = intent.getStringExtra(EXTRA_KEY_ONSTART_ARG_1_FILE_NAME);
                final String realUrl = intent.getStringExtra(EXTRA_KEY_ONSTART_ARG_2_REAL_URL);
                final int fileLength = intent.getIntExtra(EXTRA_KEY_ONSTART_ARG_3_FILE_LENGTH, 0);
                onStart(fileName, realUrl, fileLength);
                break;
            case "onStop":
                onStop(intent.getIntExtra(EXTRA_KEY_INT_PROGRESS, 0));
                break;
        }
    }

    @Override
    public void onStart(final String fileName, final String realUrl, final int fileLength) {
    }

    @Override
    public void onStop(final int progress) {
    }
}
