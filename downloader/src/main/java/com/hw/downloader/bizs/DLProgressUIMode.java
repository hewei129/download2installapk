package com.hw.downloader.bizs;

/**
 * 下载时进度状态显示的方式(可以显示进度对话或通知栏进度条或者两个都显示)
 */
public enum DLProgressUIMode {
    /**
     * 不需要在界面显示进度状态.
     */
    None,
    /**
     * 使用进度对话框显示进度状态
     */
    ProgressDialog,
    /**
     * 使用通知栏进度条显示进度状态.
     */
    Notification,
    /**
     * 同时使用进度对话框 和 通知栏进度条 显示进度状态.
     */
    ProgressDialogAndNotification
}