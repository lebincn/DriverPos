package com.tianlb.driverpos;

import android.app.Application;
import com.easemob.chat.EMChat;

/**
 * Created by tianlb on 2016/2/20.
 */
public class DriverPosApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EMChat.getInstance().init(this);
    }
}
