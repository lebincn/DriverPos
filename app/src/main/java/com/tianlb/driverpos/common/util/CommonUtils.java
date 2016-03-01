package com.tianlb.driverpos.common.util;

import android.content.Context;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * App共通处理工具类
 */
public class CommonUtils {
    /**
     * 保存应用中当前正在显示的Toast。
     */
    private static Toast globalToast;

    /**
     * 取消当前正在显示的Toast，立即显示新指定的Toast内容。
     *
     * @param context  上下文(AppicationContext, Activity...)
     * @param text     提示内容
     * @param duration Toast.LENGTH_LONG/Toast.LENGTH_SHORT
     */
    public static void showToast(Context context, CharSequence text, int duration) {
        try {
            if (globalToast != null) {
                globalToast.cancel();
                globalToast = null;
            }

            globalToast = Toast.makeText(context, text, duration);
            globalToast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否全是数字
     */
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);

        return isNum.matches();
    }
}
