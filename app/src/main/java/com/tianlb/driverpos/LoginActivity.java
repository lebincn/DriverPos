package com.tianlb.driverpos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.easemob.EMError;
import com.easemob.chat.EMChatManager;
import com.easemob.exceptions.EaseMobException;

/**
 * Created by tianlb on 2016/2/20.
 *  Register and Login
 */
public class LoginActivity extends Activity {
    //司机名
    private EditText driverName;
    //手机号
    private EditText phoneNum;
    //车号
    private EditText carNum;
/*    //开始
    private Button login;
    //环信用户名 = 手机号
    private EditText username;
    //环信密码 = 车号后五位
    private EditText password;
    //环信昵称 = 司机名+手机号+车号*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        driverName = (EditText) findViewById(R.id.driver_name);
        phoneNum = (EditText) findViewById(R.id.phone_num);
        carNum = (EditText) findViewById(R.id.car_num);

        // 启动时若有用户信息，自动填入
        // 不修改直接启动
    }

    //注册
    public void login(View view) {
        final String driver_name = driverName.getText().toString().trim();
        //TODO:加入字母混淆
        final String phone_num = phoneNum.getText().toString().trim();
        //TODO：应自动转换为大写字母
        final String car_num = carNum.getText().toString().trim();

        if (TextUtils.isEmpty(driver_name)) {
            Toast.makeText(this, getResources().getString(R.string.driver_name_cannot_be_empty), Toast.LENGTH_SHORT).show();
            driverName.requestFocus();
            return;
        } else if (TextUtils.isEmpty(phone_num)) {
            Toast.makeText(this, getResources().getString(R.string.phone_num_cannot_be_empty), Toast.LENGTH_SHORT).show();
            phoneNum.requestFocus();
            return;
        } else if (phone_num.length() != 11) {
            Toast.makeText(this, getResources().getString(R.string.phone_num_incorrect), Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(car_num)) {
            Toast.makeText(this, getResources().getString(R.string.car_num_cannot_be_empty), Toast.LENGTH_SHORT).show();
            carNum.requestFocus();
            return;
        } else if (car_num.length() != 5) {
            Toast.makeText(this, getResources().getString(R.string.car_num_incorrect), Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO: 已经注册的情况，直接登录


        //注册处理
        final String username = phone_num;
        final String password = car_num;

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            final ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage(getResources().getString(R.string.Registering));
            pd.show();

            new Thread(new Runnable() {
                public void run() {
                    try {
                        // 调用sdk注册方法
                        EMChatManager.getInstance().createAccountOnServer(username, password);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (!LoginActivity.this.isFinishing())
                                    pd.dismiss();
                                // 保存用户名
                                // TODO:用户名的保存
                                //DemoHelper.getInstance().setCurrentUserName(username);
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
                                // 注册成功，直接登录
                                realLogin();
//                                finish();
                            }
                        });
                    } catch (final EaseMobException e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (!LoginActivity.this.isFinishing())
                                    pd.dismiss();
                                int errorCode=e.getErrorCode();
                                if(errorCode== EMError.NONETWORK_ERROR){
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                                }else if(errorCode == EMError.USER_ALREADY_EXISTS){
                                    //TODO：已经注册，直接进入后续处理
                                    //登录后转入后台
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                                    realLogin();
                                }else if(errorCode == EMError.UNAUTHORIZED){
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                                }else if(errorCode == EMError.ILLEGAL_USER_NAME){
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name),Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }).start();

        }
    }

    // TODO:进入后台运行
    private void realLogin() {
        // 登录

        // 若为新用户
            // 加老板为好友
            // 加入司机群

        // 启动位置报告服务

        // 转入后台

    }

    //
    // 临时放置，未使用
/*    private void backup(void) {
        //暂定方案
        username = phoneNum;
        password = carNum;

        login = (Button) findViewById(R.id.btn_login);
        //登录按钮处理

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EMChatManager.getInstance().login(username.getText().toString(), password.getText().toString(), new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        //转入后台，开始记录，上传位置信息
                    }

                    @Override
                    public void onError(int i, String s) {
                        //没有注册
                        //使用手机号注册新用户

                        //用户名密码错误
                        //1，输入错误
                        //2，信息变更？
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });
            }
        });
    }*/

}