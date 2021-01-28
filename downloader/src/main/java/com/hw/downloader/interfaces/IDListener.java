package com.hw.downloader.interfaces;

import java.io.File;

/**
 * @author AigeStudio 2015-10-18
 */
public interface IDListener {
    void onError(int status, String error);

    void onFinish(File file);

    void onPrepare();

    void onProgress(int progress);

    void onStart(String fileName, String realUrl, int fileLength);

    void onStop(int progress);
}