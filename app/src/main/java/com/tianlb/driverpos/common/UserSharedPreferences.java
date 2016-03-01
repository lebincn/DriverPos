package com.tianlb.driverpos.common;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tianlb.driverpos.DriverPosApplication;

/**
 * 保存APP中相关数据
 */
public class UserSharedPreferences {

    private static UserSharedPreferences instance;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    private UserSharedPreferences() {
        instance = this;
        settings =PreferenceManager.getDefaultSharedPreferences(DriverPosApplication.getInstance());
        //settings = DriverPosApplication.getInstance().getSharedPreferences("user_info", Context.MODE_PRIVATE);
        editor = settings.edit();
        editor.apply();
    }

    public static synchronized UserSharedPreferences getInstance() {
        if (instance == null) {
            instance = new UserSharedPreferences();
        }
        return instance;
    }

    public String getDriverName() {
        String dd = "";
        if (settings != null  ) {
            dd=settings.getString("driverName", "");
        }

        return dd;//settings.getString("driverName", "");
    }

    public void setDriverName(String driverName) {
        editor.putString("driverName", driverName);
        editor.apply();
    }


    public String getPhoneNum() {
        return settings.getString("phoneNum", "");
    }

    public void setPhoneNum(String phoneNum) {
        editor.putString("phoneNum", phoneNum);
        editor.apply();
    }

    public String getCarNum() {
        return settings.getString("carNum", "");
    }

    public void setCarNum(String carNum) {
        editor.putString("carNum", carNum);
        editor.apply();
    }
}
