package com.lbest.rm.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import java.util.List;

/**
 * Created by dell on 2017/10/27.
 */

public class NetworkUtils {

    /**
     * 手机是否链接WIFI网络
     *
     * @return boolean true 手机是否链接的是WIFI网络
     */
    public static boolean isWifiConnect(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }


    /**
     * 利用WifiConfiguration.KeyMgmt的管理机制，来判断当前wifi是否需要连接密码
     */
    public static boolean checkWifiHasPassword(Context context, String currentWifiSSID) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            // 得到当前连接的wifi热点的信息
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            // 得到当前WifiConfiguration列表，此列表包含所有已经连过的wifi的热点信息，未连过的热点不包含在此表中
            List<WifiConfiguration> wifiConfiguration = wifiManager.getConfiguredNetworks();
            if (wifiConfiguration != null && wifiConfiguration.size() > 0) {
                for (WifiConfiguration configuration : wifiConfiguration) {
                    if (configuration != null && configuration.status == WifiConfiguration.Status.CURRENT) {
                        String ssid = configuration.SSID;
                        if (!TextUtils.isEmpty(configuration.SSID)) {
                            if (configuration.SSID.startsWith("\"") && configuration.SSID.endsWith("\"")) {
                                ssid = configuration.SSID.substring(1, configuration.SSID.length() - 1);
                            }
                        }
                        if (TextUtils.isEmpty(currentWifiSSID) || currentWifiSSID.equalsIgnoreCase(ssid)) {
                            //KeyMgmt.NONE表示无需密码
                            return (!configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
                        }
                    }
                }
            }
        } catch (Exception e) {
            //do nothing
        }
        //默认为需要连接密码
        return true;
    }


    public static String getWifiSSID(Context context){
        String ssid =null;
        if(isWifiConnect(context)){
            try {
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifi.getConnectionInfo();
                String CurInfoStr = info.toString() + "";
                String CurSsidStr = info.getSSID().toString() + "";
                if (CurInfoStr.contains(CurSsidStr)) {
                    ssid = CurSsidStr;
                } else if (CurSsidStr.startsWith("\"") && CurSsidStr.endsWith("\"")) {
                    ssid = CurSsidStr.substring(1, CurSsidStr.length() - 1);
                } else {
                    ssid = CurSsidStr;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ssid;
    }
}
