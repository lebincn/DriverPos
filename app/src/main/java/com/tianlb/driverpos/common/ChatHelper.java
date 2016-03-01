package com.tianlb.driverpos.common;

import android.util.Log;

import com.easemob.EMCallBack;
import com.easemob.EMError;
import com.easemob.chat.CmdMessageBody;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMMessage;
import com.easemob.chat.LocationMessageBody;
import com.easemob.chat.TextMessageBody;
import com.easemob.exceptions.EaseMobException;

import java.util.List;

/**
 * 发送消息助手
 */
public class ChatHelper {

    private static final String TAG = "ChatHelper";

    /**
     * 注册用户并登录
     *
     * @param username
     */
    public static boolean register(String username) {
        boolean blnRet = false;
//        try {
//            EMClient.getInstance().createAccount(username, Constant.DEF_PASSWORD);
//            blnRet = true;
//        } catch (HyphenateException e) {
//            e.printStackTrace();
//        }

        try {
            EMChatManager.getInstance().createAccountOnServer(username, Constant.DEF_PASSWORD);
            blnRet = true;
        } catch (EaseMobException e) {
            if (e.getErrorCode() == EMError.USER_ALREADY_EXISTS) {
                blnRet = true;
            }
            e.printStackTrace();
        }

        return blnRet;
    }

    /**
     * 添加管理员为好友(只有管理员不是自己好友时才添加)
     */
    public static void addAdminContact() {
//        try {
//            if (isAdminfriends()) {
//                EMClient.getInstance().contactManager().addContact(Constant.ADMIN_NAME, "我是司机，我要向你定时报告位置信息。");
//            }
//        } catch (HyphenateException e) {
//            e.printStackTrace();
//        }
        Log.i(TAG, "addAdminContact");
        try {
            if (!isAdminfriends()) {
                EMContactManager.getInstance().addContact(Constant.ADMIN_NAME, "我是司机，我要向你定时报告位置信息。");
            }
        } catch (EaseMobException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查管理员是否为自己的好友
     *
     * @return true：管理员为自己的好友，false：管理员不是自己的好友
     */
    public static boolean isAdminfriends() {
        boolean blnRet = false;
        try {
            //List<String> usernames = EMClient.getInstance().contactManager().getAllContactsFromServer();
            List<String> usernames = EMContactManager.getInstance().getContactUserNames();
            if (usernames != null) {
                Log.i(TAG, usernames.toString());
                for (String userName : usernames) {
                    if (Constant.ADMIN_NAME.equalsIgnoreCase(userName)) {
                        blnRet = true;
                        break;
                    }
                }
            }
        } catch (EaseMobException e) {
            e.printStackTrace();
        }

        return blnRet;
    }

    /**
     * 发送自己的当前位置给管理员
     */
    public static void sendLocationPosInfo(double longitude, double latitude, String locationAddress) {
        EMConversation conversation = EMChatManager.getInstance().getConversation(Constant.ADMIN_NAME);

//        System.out.println("****** In sendLocationPosInfo ***********");
//        System.out.println("longitude = " + longitude);
//        System.out.println("latitude = " + latitude);
//        System.out.println("locationAddress = " + locationAddress);

        // 正常处理，发送位置信息
        // TODO: 在上报内容中加入司机信息
        EMMessage message = EMMessage.createSendMessage(EMMessage.Type.LOCATION);
        LocationMessageBody locBody = new LocationMessageBody(locationAddress, latitude, longitude);
        message.addBody(locBody);
        message.setReceipt(Constant.ADMIN_NAME);
        conversation.addMessage(message);

        EMChatManager.getInstance().sendMessage(message, new EMCallBack() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "sendLocationPosInfo onSuccess");
            }

            @Override
            public void onError(int i, String s) {
                Log.i(TAG, "sendLocationPosInfo onError:" + s);
            }

            @Override
            public void onProgress(int i, String s) {
                Log.i(TAG, "sendLocationPosInfo onProgress:" + s);
            }
        });
    }

    /**
     * 发送取得指定用户的位置信息
     *
     * @param toUsername 目标用户名
     */
    public static void sendGetLocationPositionCmd(String toUsername) {
//        EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
//        EMCmdMessageBody cmdBody = new EMCmdMessageBody(Constant.GET_LOCATION_POSITION_CMD);
//        cmdMsg.setReceipt(toUsername);
//        cmdMsg.addBody(cmdBody);
//        EMClient.getInstance().chatManager().sendMessage(cmdMsg);

        EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
        CmdMessageBody cmdBody = new CmdMessageBody(Constant.GET_LOCATION_POSITION_CMD);
        cmdMsg.setReceipt(toUsername);
        cmdMsg.addBody(cmdBody);
        EMChatManager.getInstance().sendMessage(cmdMsg, null);
    }

    /**
     * 发送设置上报位置信息的时间间隔命令
     *
     * @param toUsername 目标用户名
     * @param interval   时间间隔（分钟)
     */
    public static void sendSetUpPosIntervalCmd(String toUsername, long interval) {
//        EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
//        HashMap<String, String> paramsMap = new HashMap<String, String>();
//        paramsMap.put("interval", String.valueOf(interval));
//        EMCmdMessageBody cmdBody = new EMCmdMessageBody(Constant.SET_UP_POS_INTERVAL_CMD, paramsMap);
//        cmdMsg.setReceipt(toUsername);
//        cmdMsg.addBody(cmdBody);
//        EMClient.getInstance().chatManager().sendMessage(cmdMsg);

        EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
        CmdMessageBody cmdBody = new CmdMessageBody(Constant.SET_UP_POS_INTERVAL_CMD);
        cmdMsg.setReceipt(toUsername);
        cmdMsg.addBody(cmdBody);
        cmdMsg.setAttribute("interval", String.valueOf(interval));
        EMChatManager.getInstance().sendMessage(cmdMsg, null);
    }


}
