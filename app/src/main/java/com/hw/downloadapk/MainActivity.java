package com.hw.downloadapk;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.hw.downloader.DownloadUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DownloadUtils.startDownloadApkAndIntall(this, "http://ssh.dianzeai.com:40002/f/f3d9520f6b6a4470abe2/?dl=1", DownloadUtils.INSTALL_SILENT);
    }

}