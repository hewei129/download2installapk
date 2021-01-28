package com.hw.downloader.bizs;


import com.hw.downloader.interfaces.IDListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 下载实体类
 * Download entity.
 *
 * @author AigeStudio 2015-05-16
 */
public class DLInfo {
    final List<DLThreadInfo> threads;
    public int totalBytes;
    public int currentBytes;
    public String fileName;
    public String dirPath;
    public String baseUrl;
    public String realUrl;
    int redirect;
    boolean hasListener;
    boolean isResume;
    boolean isStop;
    String mimeType;
    String eTag;
    String disposition;
    String location;
    List<DLHeader> requestHeaders;
    IDListener listener;
    File file;

    DLInfo() {
        threads = new ArrayList<>();
    }

    synchronized void addDLThread(DLThreadInfo info) {
        threads.add(info);
    }

    synchronized void removeDLThread(DLThreadInfo info) {
        threads.remove(info);
    }
}