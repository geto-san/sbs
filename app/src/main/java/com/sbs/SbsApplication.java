package com.sbs;

import android.app.Application;

import com.sbs.data.AppRepository;
import com.sbs.data.SyncScheduler;

public final class SbsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppRepository.getInstance(this);
        SyncScheduler.startConnectivityMonitoring(this);
        SyncScheduler.scheduleConfiguredSync(this);
    }
}
