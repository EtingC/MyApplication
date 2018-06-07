package com.lbest.rm.utils;

import android.util.Log;

/**
 * Created by dell on 2017/7/28.
 */

public class Logutils {

    private static String logTag="logTag";

    public static void setLogTag(String logTag) {
        Logutils.logTag = logTag;
    }

    public static void log_d(String msg){
        log_d(logTag,msg);
    }
    public static void log_d(String tag,String msg){
        Log.d(tag,msg);
    }
    public static void log_d(String msg,Exception e){
        log_d(logTag,msg,e);
    }
    public static void log_d(String tag,String msg,Exception e){
        Log.d(tag,msg,e);
    }


    public static void log_i(String msg){
        log_i(logTag,msg);
    }
    public static void log_i(String tag,String msg){
        Log.i(tag,msg);
    }
    public static void log_i(String msg,Exception e){
        log_i(logTag,msg,e);
    }
    public static void log_i(String tag,String msg,Exception e){
        Log.i(tag,msg,e);
    }



    public static void log_w(String msg){
        log_w(logTag,msg);
    }
    public static void log_w(String tag,String msg){
        Log.w(tag,msg);
    }
    public static void log_w(String msg,Exception e){
        log_w(logTag,msg,e);
    }
    public static void log_w(String tag,String msg,Exception e){
        Log.w(tag,msg,e);
    }



    public static void log_e(String msg){
        log_e(logTag,msg);
    }
    public static void log_e(String tag,String msg){
        Log.e(tag,msg);
    }
    public static void log_e(String msg,Exception e){
        log_e(logTag,msg,e);
    }
    public static void log_e(String tag,String msg,Exception e){
        Log.e(tag,msg,e);
    }
}
