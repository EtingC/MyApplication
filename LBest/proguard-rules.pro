# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\eclipse\SDK\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-------------------
# 打包时忽略以下类的警告
#-------------------
-dontwarn java.awt.**
-dontwarn android.test.**


# Java
-keep class * implements java.io.Serializable{*;}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
}

# ALinkApp Buiness
## ALinkBusiness
-keep class com.aliyun.alink.business.alink.ALinkConstant{*;}
-keep enum com.aliyun.alink.business.alink.ALinkEnv{*;}
-keep class com.aliyun.alink.business.alink.ALinkConfigure{*;}
-keep class com.aliyun.alink.business.alink.ALinkBusiness{
    public <fields>;
    public <methods>;
}
-keep class com.aliyun.alink.business.alink.ALinkBusinessEx{*;}
-keep public interface com.aliyun.alink.business.alink.ALinkBusiness$IListener{*;}
-keep public interface com.aliyun.alink.business.alink.ALinkBusinessEx$IListener{*;}
-keep class com.aliyun.alink.business.alink.ALinkRequest{
    public <methods>;
}
-keep class com.aliyun.alink.business.alink.ALinkResponse{*;}
-keep class com.aliyun.alink.business.alink.ALinkResponse$Result{*;}
-keep class com.aliyun.alink.business.alink.IWSFConnectWrapper{*;}
-keep interface com.aliyun.alink.business.alink.IRequestWatcher{*;}
-keep class com.aliyun.alink.business.alink.DefaultGlobalRequestWatcher{*;}

## LoginBusiness
-keep class com.aliyun.alink.business.login.IAlinkLoginAdaptor{*;}
-keep class com.aliyun.alink.business.login.AlinkLoginBusiness{*;}
-keep class com.aliyun.alink.business.login.IAlinkLoginCallback{*;}
-keep class com.aliyun.alink.business.login.IAlinkLoginStatusListener{*;}

# ALinkApp SDK
#-keep class com.aliyun.alink.sdk.net.anet.api.INet{*;}
#-keep class com.aliyun.alink.sdk.net.anet.api.AConnect{*;}
#-keep class com.aliyun.alink.sdk.net.anet.PersistentNet.AConnectResponseRunnable{
#    public <fields>;
#    public <methods>;
#}

##helper
-keep class com.aliyun.alink.business.helper.AlinkSenderHelper{*;}
-keep class com.aliyun.alink.business.helper.ChannelBindHelper{
    public <methods>;
}

##account
-keep class com.aliyun.alink.business.account.OALoginBusiness{*;}
-keep class com.aliyun.alink.business.account.TaobaoLoginBusiness{*;}

##downstream
-keep class com.aliyun.alink.business.downstream.DeviceBusiness{*;}
-keep class com.aliyun.alink.business.downstream.DownStreamBusiness{*;}
-keep class com.aliyun.alink.business.downstream.IDownstreamCommandListener{*;}
-keep class com.aliyun.alink.business.downstream.DeviceData{*;}

##anet
-keep class com.aliyun.alink.sdk.net.anet.api.persistentnet.PersistentRequest{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.persistentnet.IOnPushListener{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.persistentnet.EventDispatcher{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.persistentnet.INetSessionStateListener{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.persistentnet.IConnectionStateListener{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryRequest{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryResponse{*;}

-keep class com.aliyun.alink.sdk.net.anet.api.AConnect{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.AError{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.ARequest{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.AResponse{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.IOnCallListener{*;}
-keep class com.aliyun.alink.sdk.net.anet.api.INet{*;}


## WSFNet
-keep class com.aliyun.alink.sdk.net.anet.wsf.WSFConfigure{*;}
-keep class com.aliyun.alink.sdk.net.anet.wsf.WSFNet{
    public <methods>;
}
-keep interface com.aliyun.alink.sdk.net.anet.wsf.IWSFNetDownstreamCommandListener{*;}
-keep class com.aliyun.alink.sdk.net.anet.wsf.IWSFNetDownstreamCommandListener{*;}

# ALinkApp Utils
-keep class com.aliyun.alink.tool.ALog{
    public <fields>;
    public <methods>;
}
-keep class com.aliyun.alink.tool.ThreadTools{
    public <fields>;
    public <methods>;
}
-keep class com.aliyun.alink.tool.NetTools{
    public <fields>;
    public <methods>;
}

# SDK Entry
-keep class com.aliyun.alink.LinkSDK{
    public <fields>;
    public <methods>;
}

# Mtop
-keep class com.aliyun.alink.business.mtop.MTopBusiness{*;}
-keep interface com.aliyun.alink.business.mtop.MTopBusiness$IListener{*;}
-keep interface com.aliyun.alink.business.mtop.IMTopRequest{*;}
-keep class com.aliyun.alink.business.mtop.MTopResponse{*;}

-keep public class * implements mtopsdk.mtop.domain.IMTOPDataObject {*;}
-keep public class mtopsdk.mtop.domain.MtopResponse
-keep public class mtopsdk.mtop.domain.MtopRequest
-keep class mtopsdk.mtop.domain.**{*;}
-keep class mtopsdk.common.util.**{*;}
-keep class com.taobao.tao.connectorhelper.*
-keep public class org.android.spdy.**{*; }
-keep class mtop.sys.newDeviceId.Request{*;}

#push
-keepclasseswithmembernames class ** {
    native <methods>;
}
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }
-keep class com.taobao.** {*;}
-keep class com.alibaba.** {*;}
-keep class com.alipay.** {*;}
-keep class com.ut.** {*;}
-keep class com.ta.** {*;}
-keep class anet.**{*;}
-keep class anetwork.**{*;}
-keep class org.android.spdy.**{*;}
-keep class org.android.agoo.**{*;}
-keep class android.os.**{*;}
-dontwarn com.taobao.**
-dontwarn com.alibaba.**
-dontwarn com.alipay.**
-dontwarn anet.**
-dontwarn org.android.spdy.**
-dontwarn org.android.agoo.**
-dontwarn anetwork.**
-dontwarn com.ut.**
-dontwarn com.ta.**

# account
-dontwarn  com.aliyun.alink.linksdk.*

# device center
-dontwarn com.aliyun.alink.business.devicecenter.**
-keep class com.aliyun.alink.business.devicecenter.**{*;}
