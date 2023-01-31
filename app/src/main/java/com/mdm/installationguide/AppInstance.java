package com.mdm.installationguide;

import android.app.Application;
import android.content.Context;

public class AppInstance extends Application {

    AppInstance instance;


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;


    }

}