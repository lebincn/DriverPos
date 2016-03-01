package com.tianlb.driverpos.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.easemob.EMCallBack;
import com.easemob.EMConnectionListener;
import com.easemob.EMEventListener;
import com.easemob.EMNotifierEvent;
import com.easemob.chat.EMChat;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMCmdMessageBody;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.exceptions.EaseMobException;
import com.easemob.util.EMLog;
import com.tianlb.driverpos.DriverPosApplication;
import com.tianlb.driverpos.common.ChatHelper;
import com.tianlb.driverpos.common.Constant;
import com.tianlb.driverpos.common.util.CommonUtils;

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

/**
 * 定时上报当前位置服务
 */
public class UpPositionService extends Service {

    /**
     * Logcat TAG
     */
    private static final String TAG = "UpPositionService";

    /**
     * 上报当前位置消息
     */
    private static final int UP_POSITION_MSG = 1001;

    /**
     * 开启登录完成上报处理消息
     */
    private static final int START_UP_POSITION_MSG = 1002;

    /**
     * 停止登录完成上报处理消息
     */
    private static final int STOP_UP_POSITION_MSG = 1003;

    /**
     * 定时任务Handler
     */
    private TimeTaskHandler timeTaskHandler;

    /**
     * 百度定位服务
     */
    private LocationClient mLocationClient;

    /**
     * 百度定位监听
     */
    public BDLocationListener bdLocationListener = new LocationListener();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return upPositionAidlInterface;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        // 设定为前台服务
        // startForeground(-1213, new Notification());

        // 创建定时任务
        timeTaskHandler = new TimeTaskHandler(this);

        // 初始化环信服务
        initIMService(this);

        // 初始化定位服务
        initLocation();

        // 运行定时任务
        runTimeTask();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        //如果服务被异常kill掉，系统会自动重启该服务
        flags = START_REDELIVER_INTENT;
        return super.onStartCommand(intent, flags, Service.START_NOT_STICKY);
    }

    @Override
    public void onDestroy() {
        // 停止前台服务
        // stopForeground(true);

        stopLocation();

        if (timeTaskHandler != null) {
            timeTaskHandler.removeMessages(UP_POSITION_MSG);
        }

        EMChatManager.getInstance().unregisterEventListener(emEventListener);

        // 重新开始服务
        Intent service = new Intent(this, UpPositionService.class);
        startService(service);

        super.onDestroy();
    }

    /**
     * 初始化定位服务
     */
    private void initLocation() {

        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(bdLocationListener);

        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系

        option.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(false);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }

    /**
     * 初始化环信服务
     */
    private void initIMService(Context context) {
        EMChat.getInstance().init(context);
        EMChat.getInstance().setDebugMode(true);
        EMChatManager.getInstance().getChatOptions().setShowNotificationInBackgroud(false);
        EMChatManager.getInstance().getChatOptions().setNotifyBySoundAndVibrate(false);
        EMChatManager.getInstance().getChatOptions().setNoticeBySound(false);
        EMChatManager.getInstance().getChatOptions().setNoticedByVibrate(false);
        EMChatManager.getInstance().registerEventListener(emEventListener);
        EMChatManager.getInstance().addConnectionListener(new MyConnectionListener());
        EMChat.getInstance().setAppInited();
    }

    /**
     * 运行定时任务
     */
    private void runTimeTask() {
        if (timeTaskHandler != null) {
            timeTaskHandler.removeMessages(UP_POSITION_MSG);
            // TODO: * 60
            // Release mode 5 minute
            timeTaskHandler.sendEmptyMessageDelayed(UP_POSITION_MSG, getInterval() * 1000 * 5);
//            timeTaskHandler.sendEmptyMessageDelayed(UP_POSITION_MSG, getInterval() * 1000 * 60 * 5);
//  Test mode 1 second
//            timeTaskHandler.sendEmptyMessageDelayed(UP_POSITION_MSG, getInterval() * 1000);
        }
    }

    /**
     * 停止定时任务
     */
    private void stopTimeTask() {
        if (timeTaskHandler != null) {
            timeTaskHandler.removeMessages(UP_POSITION_MSG);
        }
    }

    /**
     * 接收IM服务发送过来的相关消息
     */
    private EMEventListener emEventListener = new EMEventListener() {
        @Override
        public void onEvent(EMNotifierEvent emNotifierEvent) {
            Log.i(TAG, "onEvent:" + emNotifierEvent.toString());
            // TODO: 追加消息处理，接到消息，立即上报，或设置时间间隔
            if (emNotifierEvent.getEvent() == EMNotifierEvent.Event.EventNewCMDMessage) {
                EMMessage message = (EMMessage) emNotifierEvent.getData();
                //获取消息body
                EMCmdMessageBody cmdMsgBody = (EMCmdMessageBody) message.getBody();
                //获取自定义action
                final String action = cmdMsgBody.action;
                EMLog.d(TAG, String.format("透传消息：action:%s,message:%s", action, message.toString()));

                if (Constant.GET_LOCATION_POSITION_CMD.equalsIgnoreCase(action)) {
                    // 发送自己的当前位置给管理员
                    mLocationClient.start();
                } else if (Constant.SET_UP_POS_INTERVAL_CMD.equalsIgnoreCase(action)) {
                    try {
                        long interval = Long.parseLong(message.getStringAttribute("interval"));

                        setInterval(interval);
                    } catch (EaseMobException e) {
                        e.printStackTrace();
                    }

                    // 重新运行定时任务
                    runTimeTask();
                }
            }
        }
    };

    /**
     * 取得发送位置信息的时间间隔(单位分钟)
     */
    private long getInterval() {
        SharedPreferences settings = this.getSharedPreferences("service_info", MODE_PRIVATE);
        return settings.getLong("service_info", 3);
    }

    /**
     * 保存发送位置信息的时间间隔(单位分钟)
     */
    private void setInterval(long interval) {
        SharedPreferences settings = this.getSharedPreferences("service_info", MODE_PRIVATE);
        SharedPreferences.Editor localEditor = settings.edit();
        localEditor.putLong("interval", interval);
        localEditor.commit();
    }

    /**
     * 注册用户
     */
    private Observable<Boolean> register(final String userName) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(Boolean.valueOf(ChatHelper.register(userName)));
            }
        });
    }

    /**
     * 退出登录
     */
    private void logout() {
        if (EMChatManager.getInstance().isConnected()) {
            EMChatManager.getInstance().logout();
        }
    }

    /**
     * 登录环信服务器
     */
    private void login(final String driverName, final String phone_num) {
        register(phone_num)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError");
                        e.printStackTrace();
                        sendAppBroadcast(3009);
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.d(TAG, "onNext");
                        EMChatManager.getInstance().login(phone_num, Constant.DEF_PASSWORD, new EMCallBack() {//回调
                            @Override
                            public void onSuccess() {

                                Log.d(TAG, "longin success");
                                sendAppBroadcast(3000);

                                EMChatManager.getInstance().updateCurrentUserNick(driverName);
                                EMGroupManager.getInstance().loadAllGroups();
                                EMChatManager.getInstance().loadAllConversations();

                                //Observable.
                                Observable.create(new Observable.OnSubscribe<Void>() {
                                    @Override
                                    public void call(Subscriber<? super Void> subscriber) {
                                        Log.d(TAG, "call addAdminContact");
                                        ChatHelper.addAdminContact();
                                        subscriber.onNext(null);
                                    }
                                }).observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Action1<Void>() {
                                            @Override
                                            public void call(Void aVoid) {
                                                Log.d(TAG, "call addAdminContact done");
                                                sendAppBroadcast(3005);
                                                CommonUtils.showToast(DriverPosApplication.getInstance(), "程序已转入后台运行。", Toast.LENGTH_LONG);
//                                                        finish();
                                            }
                                        });

                                //  开启定时上报当前位置服务
                                Intent service = new Intent(UpPositionService.this, UpPositionService.class);
                                startService(service);
                                Log.d(TAG, "登陆聊天服务器成功！");
                            }

                            @Override
                            public void onProgress(int progress, String status) {

                            }

                            @Override
                            public void onError(int code, String message) {
                                Log.d(TAG, "登陆聊天服务器失败！");
                            }
                        });

                    }

                });

    }

    /**
     * 定时任务
     */
    private class TimeTaskHandler extends Handler {
        private WeakReference<UpPositionService> contextWeakReference;

        public TimeTaskHandler(UpPositionService context) {
            contextWeakReference = new WeakReference<UpPositionService>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, "handleMessage : " + msg.what);

            UpPositionService service = contextWeakReference.get();

            try {
                if (msg.what == UP_POSITION_MSG) {
                    //  上报当前位置消息的场合
                    // 发送自己的当前位置给管理员
                    mLocationClient.start();
//                    mLocationClient.requestLocation();

                    // 再次运行定时任务
                    service.runTimeTask();
                } else if (msg.what == START_UP_POSITION_MSG) {
                    // 开启登录完成上报处理消息的场合
                    Bundle data = msg.getData();
                    startUpPostion(data.getString("driverName", ""), data.getString("phoneNum", ""), data.getString("carNum", ""));
                } else if (msg.what == STOP_UP_POSITION_MSG) {
                    // 停止登录完成上报处理消息的场合
                    stopLocation();
                    stopTimeTask();
                    logout();
                    sendAppBroadcast(3008);
                }
            } catch (Exception ex) {
                // nothing
            }
        }
    }

    /**
     * 开启登录完成上报处理
     *
     * @param driverName
     * @param phoneNum
     * @param carNum
     */
    private void startUpPostion(String driverName, String phoneNum, String carNum) {
        stopLocation();
        // 退出登录
        logout();

        // 登录环信服务器
        login(driverName, phoneNum);
    }

    /**
     * 停止定位处理
     */
    private void stopLocation() {
        if (mLocationClient != null) {
            mLocationClient.stop();
        }
    }

    /**
     * 实现ConnectionListener接口,监听连接IM服务器状况
     */
    private class MyConnectionListener implements EMConnectionListener {
        @Override
        public void onConnected() {
            Log.i(TAG, "onConnected");

            // 登录成功的场合
            if (isLogin()) {
                // 运行定时任务
                runTimeTask();

                sendAppBroadcast(3001);
            }
        }

        @Override
        public void onDisconnected(final int error) {
            Log.i(TAG, "onDisconnected : " + error);

            // 停止定时任务
            stopTimeTask();
        }
    }

    /**
     * 是否已经登录成功
     */
    private boolean isLogin() {
        return (EMChatManager.getInstance().isConnected()
                && !TextUtils.isEmpty(EMChatManager.getInstance().getCurrentUser()));
    }

    /**
     * 发送应用内广播
     */
    private void sendAppBroadcast(int type) {
        Intent intent = new Intent(Constant.UP_POSITION_SERVICE_ACTION);
        intent.setPackage(DriverPosApplication.getInstance().getPackageName());
        intent.putExtra("type", type);
        sendBroadcast(intent);
    }

    /**
     * 定位到位置后回调
     */
    private class LocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location != null) {
                Log.i(TAG, location.toString());

                ChatHelper.sendLocationPosInfo(location.getLongitude(), location.getLatitude(), location.getAddrStr());
            }
            mLocationClient.stop();
        }
    }

    /**
     * IUpPositionAidlInterface实现
     */
    private IUpPositionAidlInterface.Stub upPositionAidlInterface = new IUpPositionAidlInterface.Stub() {

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public void startUpPosition(String driverName, String phoneNum, String carNum) throws RemoteException {
            Message msg = timeTaskHandler.obtainMessage(START_UP_POSITION_MSG);
            msg.getData().putString("driverName", driverName);
            msg.getData().putString("phoneNum", phoneNum);
            msg.getData().putString("carNum", carNum);
            timeTaskHandler.sendMessage(msg);
        }

        @Override
        public void stopUpPosition() throws RemoteException {
            timeTaskHandler.sendEmptyMessage(STOP_UP_POSITION_MSG);
        }

        @Override
        public boolean isLoginDone() throws RemoteException {
            return isLogin();
        }
    };

}
