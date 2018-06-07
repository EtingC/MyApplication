package com.lbest.rm.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dell on 2017/10/20.
 */

public class CommonUtils {

    /**
     * 将byte数组转为十六进制字符串
     *
     * @param byteArray
     *            byte[]
     * @return String 十六进制字符串
     */
    public static String bytesToHexString(byte[] byteArray) {
        StringBuffer re = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            re.append(to16(byteArray[i]));
        }

        return re.toString();
    }

    public static String to16(int b) {
        String hexString = Integer.toHexString(b);
        int lenth = hexString.length();
        if (lenth == 1) {
            hexString = "0" + hexString;
        }
        if (lenth > 2) {
            hexString = hexString.substring(lenth - 2, lenth);
        }
        return hexString;
    }


    /**
     * 字符串转为 btye 数组
     *
     * @param dataString 转化的16进制字符串
     * @return byte数组
     */
    public static byte[] parseStringToByte(String dataString) {
        int subPosition = 0;
        int byteLenght = dataString.length() / 2;

        byte[] result = new byte[byteLenght];

        for (int i = 0; i < byteLenght; i++) {
            String s = dataString.substring(subPosition, subPosition + 2);
            result[i] = (byte) Integer.parseInt(s, 16);
            subPosition = subPosition + 2;
        }

        return result;
    }

    /**
     * 验证手机格式
     * @param phone
     * @return
     */
    public static boolean isPhone(String phone) {
        String regex = "[0-9]+";
        return match(regex, phone);
    }
    /**
     *
     * 是否是email
     *
     * @param str
     *
     * @return boolean正确的邮箱格式
     *
     */
    public static boolean isEmail(String str) {
        String regex = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        return match(regex, str);
    }
    /**
     * @param regex
     *            正则表达式字符串
     * @param str
     *            要匹配的字符串
     * @return 如果str 符合 regex的正则表达式格式,返回true, 否则返回 false;
     */
    private static boolean match(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }


    /**
     * 获取手机设置的地区
     * @return
     */
    public static String getCountry() {
        Locale locale = Locale.getDefault();
        return locale.getCountry();
    }


    public static int getStatusBarHeight(Context context) {
        try {
            Class<?> cls = Class.forName("com.android.internal.R$dimen");
            Object obj = cls.newInstance();
            Field field = cls.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
        }
        return 0;
    }
    /**
     * 获取手机语言
     * @return
     * <br>zh_Hant 中文繁体
     * <br>zh_Hans 中文简体
     * <br>en 英文
     */
    public static String getLanguage() {
        Locale locale = Locale.getDefault();
        String country = locale.getCountry();

        StringBuffer language = new StringBuffer(locale.getLanguage());
        language.append("-");
        language.append(country);
        return language.toString().toLowerCase();
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }



    /**
     * 手机是否链接网络
     *
     * @return boolean
     *
     */
    public static boolean checkNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return ((connectivityManager.getActiveNetworkInfo() != null && connectivityManager
                .getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || telephonyManager
                .getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);
    }

    /***
     * 字符传是否包含双字节字符
     * @param str
     * @return
     */
    public static boolean strContainCNChar(String str) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        for (int i = 0; i < str.length(); i++) {
            if (p.matcher(String.valueOf(str.charAt(i))).matches()) {
                return true;
            }
        }
        return false;
    }

    public static String getWIFISSID(Context context){
        String ssid = "";
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            WifiInfo info = wifi.getConnectionInfo();
            String CurInfoStr = info.toString() + "";
            String CurSsidStr = info.getSSID().toString() + "";
            if (CurInfoStr.contains(CurSsidStr)) {
                ssid = CurSsidStr;
            } else if(CurSsidStr.startsWith("\"") && CurSsidStr.endsWith("\"")){
                ssid = CurSsidStr.substring(1, CurSsidStr.length() - 1);
            } else {
                ssid = CurSsidStr;
            }
        } catch (Exception e) {
        }

        return ssid;
    }

}
