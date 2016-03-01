package com.tianlb.driverpos;

import android.app.Application;
import android.util.Log;

/**
 * 司机端Application
 */
public class DriverPosApplication extends Application {
    private static final String TAG = "DriverPosApplication";

    private static DriverPosApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        instance = this;
    }

    public static DriverPosApplication getInstance() {
        return instance;
    }

}
