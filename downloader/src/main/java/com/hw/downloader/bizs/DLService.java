package com.hw.downloader.bizs;


import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;


import com.hw.downloader.DownloadUtils;
import com.hw.downloader.interfaces.IDListener;

import java.io.File;
import java.util.Locale;

import static com.hw.downloader.bizs.DLError.ERROR_REPEAT_URL;


/**
 * 执行下载的Service
 *
 * @author AigeStudio 2015-05-18
 */
@SuppressWarnings("unused")
public class DLService extends Service {
    public static final String EXTRA_KEY_STR_URL = "url";
    public static final String EXTRA_KEY_STR_DIR_FULL_PATH = "dirPath";
    public static final String EXTRA_KEY_BOL_ENABLE_NOTIFICATION = "是否启用Notification进度显示";
    public static final String BROADCAST_ACTION_NAME = ".DLService_Broadcast";

    public static void registerReceiver(Context context, DLBroadcastReceiver receiver) {
        context.registerReceiver(receiver, new IntentFilter(BROADCAST_ACTION_NAME));
    }

    /**
     * @param context 不使用调用者所在进程的Context目的是为了防止进程被杀掉了后,此时DLService重启后就找不到上下文了.
     *                而假如使用DLService的Context则能保证只要DLService是存活的,就能正常更新通知栏进度.
     *                ProgressDialog则不能使用 DLService的Context 因为需要依附于 UI 进程.
     */
    private static void showDownloadNotification(final Context context, final String url, final String dirPath) {
        final int id = url.hashCode() & dirPath.hashCode();

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        //通过反射获取APP启动图标资源
        int resId = context.getResources().getIdentifier("ic_launcher", "mipmap", context.getPackageName());
        if (resId > 0)
            builder.setSmallIcon(resId);

        registerReceiver(context, new DLBroadcastReceiver() {
            private NotificationManager nm;
            private int maxLength;

            private NotificationManager getNotificationManager() {
                if (nm == null)
                    nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                return nm;
            }

            @Override
            public void onError(final int status, final String error) {
                //重复的下载地址,则不取消当前正在下载的通知栏.
                if (status == ERROR_REPEAT_URL)
                    return;
                getNotificationManager().cancel(id);
                unregisterReceiver(context, this);
            }

            @Override
            public void onFinish(File file) {
                getNotificationManager().cancel(id);
                unregisterReceiver(context, this);
            }

            @Override
            public void onPrepare() {
                builder.setProgress(100, 0, true);
                getNotificationManager().notify(id, builder.build());
            }

            @Override
            public void onProgress(int progress) {
                double maxMb = maxLength / 1024d / 1024d;
                double curMb = progress / 1024d / 1024d;
                double percent = progress * 100d / maxLength;
                builder.setContentInfo(String.format(Locale.ENGLISH, "%.2fMB/%.2fMB[%.2f%%]", curMb, maxMb, percent));
                builder.setProgress(maxLength, progress, false);
                getNotificationManager().notify(id, builder.build());
            }

            @Override
            public void onStart(String fileName, String realUrl, int fileLength) {
                builder.setContentTitle(fileName);
                maxLength = fileLength;
            }

            @Override
            public void onStop(final int progress) {
                getNotificationManager().cancel(id);
                unregisterReceiver(context, this);
            }
        });
    }

    public static void unregisterReceiver(Context context, DLBroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String url = intent.getStringExtra(EXTRA_KEY_STR_URL);
        final String path = intent.getStringExtra(EXTRA_KEY_STR_DIR_FULL_PATH);
        DLManager.getInstance(this).dlStart(url, path, DownloadUtils.APK_NAME, null, new IDListener() {
            private int maxLength;

            @Override
            public void onError(final int status, final String error) {
                Intent i = new Intent(BROADCAST_ACTION_NAME);
                i.putExtra(EXTRA_KEY_STR_URL, url);
                i.putExtra(EXTRA_KEY_STR_DIR_FULL_PATH, path);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_STR_METHOD_NAME, "onError");
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_ONERROR_ARG_STATUS, status);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_ONERROR_ARG_ERROR, error);
                sendBroadcast(i);
            }

            @Override
            public void onFinish(File file) {
                Intent i = new Intent(BROADCAST_ACTION_NAME);
                i.putExtra(EXTRA_KEY_STR_URL, url);
                i.putExtra(EXTRA_KEY_STR_DIR_FULL_PATH, path);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_STR_METHOD_NAME, "onFinish");
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_ONFINISH_ARG_1_FILE_FULL_PATH, file.getAbsolutePath());
                sendBroadcast(i);
            }

            @Override
            public void onPrepare() {
                Intent i = new Intent(BROADCAST_ACTION_NAME);
                i.putExtra(EXTRA_KEY_STR_URL, url);
                i.putExtra(EXTRA_KEY_STR_DIR_FULL_PATH, path);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_STR_METHOD_NAME, "onPrepare");
                sendBroadcast(i);
            }

            @Override
            public void onProgress(int progress) {
                Intent i = new Intent(BROADCAST_ACTION_NAME);
                i.putExtra(EXTRA_KEY_STR_URL, url);
                i.putExtra(EXTRA_KEY_STR_DIR_FULL_PATH, path);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_STR_METHOD_NAME, "onProgress");
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_INT_PROGRESS, progress);
                sendBroadcast(i);
            }

            @Override
            public void onStart(String fileName, String realUrl, int fileLength) {
                Intent i = new Intent(BROADCAST_ACTION_NAME);
                i.putExtra(EXTRA_KEY_STR_URL, url);
                i.putExtra(EXTRA_KEY_STR_DIR_FULL_PATH, path);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_STR_METHOD_NAME, "onStart");
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_ONSTART_ARG_1_FILE_NAME, fileName);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_ONSTART_ARG_2_REAL_URL, realUrl);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_ONSTART_ARG_3_FILE_LENGTH, fileLength);
                sendBroadcast(i);
            }

            @Override
            public void onStop(final int progress) {
                Intent i = new Intent(BROADCAST_ACTION_NAME);
                i.putExtra(EXTRA_KEY_STR_URL, url);
                i.putExtra(EXTRA_KEY_STR_DIR_FULL_PATH, path);
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_STR_METHOD_NAME, "onStop");
                i.putExtra(DLBroadcastReceiver.EXTRA_KEY_INT_PROGRESS, progress);
                sendBroadcast(i);
            }
        });

        if (intent.getBooleanExtra(EXTRA_KEY_BOL_ENABLE_NOTIFICATION, false))
            showDownloadNotification(this, url, path);

        return START_REDELIVER_INTENT;
    }
}
