package com.hw.downloader.bizs;

interface IDLThreadListener {
    void onFinish(DLThreadInfo threadInfo);

    void onProgress(int progress);

    void onStop(DLThreadInfo threadInfo);
}