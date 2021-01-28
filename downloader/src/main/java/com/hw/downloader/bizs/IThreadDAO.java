package com.hw.downloader.bizs;

import java.util.List;

interface IThreadDAO {
    void deleteAllThreadInfo(String url);

    void deleteThreadInfo(String id);

    void insertThreadInfo(DLThreadInfo info);

    List<DLThreadInfo> queryAllThreadInfo(String url);

    DLThreadInfo queryThreadInfo(String id);

    void updateThreadInfo(DLThreadInfo info);
}