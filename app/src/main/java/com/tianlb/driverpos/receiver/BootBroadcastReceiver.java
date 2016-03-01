package com.tianlb.driverpos.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tianlb.driverpos.service.UpPositionService;

/**
 * 接收开机启动消息
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //  开启定时上报当前位置服务
        Intent service = new Intent(context, UpPositionService.class);
        context.startService(service);
    }

}
