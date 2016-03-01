package com.tianlb.driverpos;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tianlb.driverpos.common.Constant;
import com.tianlb.driverpos.common.UserSharedPreferences;
import com.tianlb.driverpos.common.util.CommonUtils;
import com.tianlb.driverpos.receiver.BootBroadcastReceiver;
import com.tianlb.driverpos.service.IUpPositionAidlInterface;
import com.tianlb.driverpos.service.UpPositionService;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.Locale;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends RxAppCompatActivity {

    /**
     * Logcat Tag
     */
    private static final String TAG = "LoginActivity";

    /**
     * 用户信息区域
     */
    private View userInfoLayout;

    /**
     * 司机名
     */
    private EditText driverName;

    /**
     * 手机号
     */
    private EditText phoneNum;

    /**
     * 车号
     */
    private EditText carNum;

    /**
     * 开始按扭
     */
    private Button runButton;

    /**
     * 定时上报当前位置服务
     */
    private IUpPositionAidlInterface upPositionAidlInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 查找画面中需要处理的View对象
        findAllViews();

        // 绑定View事件处理
        runButton.setOnClickListener(onClickListener);

        // 监听UpPositionService运行时发出的广播
        IntentFilter upPositionServiceFilter = new IntentFilter(Constant.UP_POSITION_SERVICE_ACTION);
        registerReceiver(broadcastReceiver, upPositionServiceFilter);

        // 开启定时上报当前位置服务
        startUpPositionService();

        // 加载画面之前录入的数据
        loadData();

        // 更新画面状态
        updateUiStatus();
    }
//  tianlb comment for android 19
//    @Override
//    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
//        super.onSaveInstanceState(outState, outPersistentState);
//        saveData();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    /**
     * 查找画面中需要处理的View对象
     */
    private void findAllViews() {
        userInfoLayout = findViewById(R.id.user_info_layout);
        driverName = (EditText) findViewById(R.id.driver_name);
        phoneNum = (EditText) findViewById(R.id.phone_num);
        carNum = (EditText) findViewById(R.id.car_num);
        runButton = (Button) findViewById(R.id.btn_login);
    }

    /**
     * 加载画面之前录入的数据
     */
    private void loadData() {
        driverName.setText(UserSharedPreferences.getInstance().getDriverName());
        phoneNum.setText(UserSharedPreferences.getInstance().getPhoneNum());
        carNum.setText(UserSharedPreferences.getInstance().getCarNum());
    }

    /**
     * 保存画面录入的数据
     */
    private void saveData() {
        UserSharedPreferences.getInstance().setDriverName(driverName.getText().toString().trim());
        UserSharedPreferences.getInstance().setPhoneNum(phoneNum.getText().toString().trim());
        UserSharedPreferences.getInstance().setCarNum(carNum.getText().toString().trim().toUpperCase());
    }

    /**
     * 开启定时上报当前位置服务
     */
    private void startUpPositionService() {
        try {
            if (upPositionAidlInterface == null) {
                //  开启定时上报当前位置服务
                Intent service = new Intent(this, UpPositionService.class);
                startService(service);

                bindService(service, serviceConnection, BIND_AUTO_CREATE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 更新画面状态
     */
    private void updateUiStatus() {
        boolean blnEnabled = false;
        try {
            if (upPositionAidlInterface != null && upPositionAidlInterface.isLoginDone()) {
                runButton.setText(getString(R.string.stop));
            } else {
                blnEnabled = true;
                runButton.setText(getString(R.string.start));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        userInfoLayout.setEnabled(blnEnabled);
        driverName.setEnabled(blnEnabled);
        phoneNum.setEnabled(blnEnabled);
        carNum.setEnabled(blnEnabled);
    }

    /**
     * 检查画面输入内容是否正确，并更新UpPositionService中的用户信息
     */
    private void checkRunUserInfo() {
        final String driver_name = driverName.getText().toString().trim();
        final String phone_num = phoneNum.getText().toString().trim();
        final String car_num = carNum.getText().toString().trim().toUpperCase(Locale.ENGLISH);

        if (TextUtils.isEmpty(driver_name)) {
            CommonUtils.showToast(this, getResources().getString(R.string.driver_name_cannot_be_empty), Toast.LENGTH_SHORT);
            driverName.requestFocus();
            return;
        } else if (TextUtils.isEmpty(phone_num)) {
            CommonUtils.showToast(this, getResources().getString(R.string.phone_num_cannot_be_empty), Toast.LENGTH_SHORT);
            phoneNum.requestFocus();
            return;
        } else if (phone_num.length() != 11) {
            CommonUtils.showToast(this, getResources().getString(R.string.phone_num_incorrect), Toast.LENGTH_SHORT);
            return;
        } else if (TextUtils.isEmpty(car_num)) {
            CommonUtils.showToast(this, getResources().getString(R.string.car_num_cannot_be_empty), Toast.LENGTH_SHORT);
            carNum.requestFocus();
            return;
        } else if (car_num.length() != 5 && car_num.length() != 7) {
            CommonUtils.showToast(this, getResources().getString(R.string.car_num_incorrect), Toast.LENGTH_SHORT);
            return;
        }

        try {
            // 开启定时上报当前位置服务
            startUpPositionService();

            if (upPositionAidlInterface != null) {
                // 重新设置UpPositionService中的用户信息
                upPositionAidlInterface.startUpPosition(driver_name, phone_num, car_num);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始按扭点击事件处理
     */
    private View.OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                if (upPositionAidlInterface != null && upPositionAidlInterface.isLoginDone()) {
                    upPositionAidlInterface.stopUpPosition();
                } else {
                    saveData();
                    checkRunUserInfo();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * UpPositionService连接管理
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            upPositionAidlInterface = IUpPositionAidlInterface.Stub.asInterface(service);
            updateUiStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
            upPositionAidlInterface = null;
            updateUiStatus();
        }
    };

    /**
     * 监听UpPositionService运行时发出的广播
     */
    private BroadcastReceiver broadcastReceiver = new BootBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);

            if (Constant.UP_POSITION_SERVICE_ACTION.equals(intent.getAction())) {
                int type = intent.getIntExtra("type", 0);
                updateUiStatus();
                if (type == 3005) {
                    finish();
                }
            }
        }
    };
}

