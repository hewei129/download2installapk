package com.hw.downloader.bizs;

interface ITaskDAO {
    void deleteTaskInfo(String url);

    void insertTaskInfo(DLInfo info);

    DLInfo queryTaskInfo(String url);

    void updateTaskInfo(DLInfo info);
}