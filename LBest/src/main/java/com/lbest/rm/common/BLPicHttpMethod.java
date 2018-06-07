package com.lbest.rm.common;

import android.content.Context;
import android.graphics.Bitmap;

import com.alibaba.fastjson.JSON;
import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.account.broadlink.BLHttpErrCode;
import com.lbest.rm.data.FileInfo;
import com.lbest.rm.data.HttpHeader;
import com.lbest.rm.data.HttpResponse;
import com.lbest.rm.data.RequestTimestampResult;
import com.lbest.rm.productDevice.FamilyManager;
import com.lbest.rm.utils.CommonUtils;
import com.lbest.rm.utils.EncryptUtils;
import com.lbest.rm.utils.http.HttpPostAccessor;
import com.litesuits.http.HttpConfig;
import com.litesuits.http.LiteHttp;
import com.litesuits.http.concurrent.OverloadPolicy;
import com.litesuits.http.concurrent.SchedulePolicy;
import com.litesuits.http.data.NameValuePair;
import com.litesuits.http.exception.HttpException;
import com.litesuits.http.listener.GlobalHttpListener;
import com.litesuits.http.request.StringRequest;
import com.litesuits.http.request.content.multi.InputStreamPart;
import com.litesuits.http.request.content.multi.MultipartBody;
import com.litesuits.http.request.param.CacheMode;
import com.litesuits.http.request.param.HttpMethods;
import com.litesuits.http.request.query.JsonQueryBuilder;
import com.litesuits.http.response.Response;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.com.broadlink.sdk.BLLet;

/**
 * Created by dell on 2017/11/30.
 */

public class BLPicHttpMethod {

    // 这个第三方库使用是为了上传图片(可以方便添加minetype和filename)
    private static LiteHttp liteHttp;

    private static LiteHttp getLiteHttp(Context context) {
        if (liteHttp == null) {
            GlobalHttpListener globalHttpListener = new GlobalHttpListener() {
                @Override
                public void onSuccess(Object o, Response<?> response) {
                    if (response != null) {
                        response.printInfo();
                    }
                }

                @Override
                public void onFailure(HttpException e, Response<?> response) {
                    if (response != null) {
                        response.printInfo();
                    }
                }
            };

            List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair("cookies", "broadlink"));

            HttpConfig httpConfig = LiteHttp.build(context);// 设置上下文
            // 设置网络
            httpConfig.setDetectNetwork(true);// 设置连接前是否判断网络，若为true，判断无可用网络后直接结束请求。需设置context，才能有效
            httpConfig.setDisableNetworkFlags(HttpConfig.FLAG_NET_DISABLE_NONE);// 设置全局禁用网络类型,这里设置不禁用任何类型
            // 设置重试
            httpConfig.setSocketTimeout(3000);
            httpConfig.setConnectTimeout(3000);
            httpConfig.setRetrySleepMillis(1000);
            httpConfig.setDefaultMaxRetryTimes(1);// 设置全局默认重试最大次数
            httpConfig.setDefaultMaxRedirectTimes(7);// 设置全局默认重定向最大次数
            // 设置队列
            httpConfig.setWaitingQueueSize(128);// 设置全局默认等待队列大小
            httpConfig.setConcurrentSize(4);// 设置全局默认同时并发执行请求数量，建议设置数量为CPU核数
            httpConfig.setSchedulePolicy(SchedulePolicy.FirstInFistRun);// 设置全局默认请求调度策略,先进先执行
            httpConfig.setOverloadPolicy(OverloadPolicy.DiscardNewTaskInQueue);// 设置全局默认满载处理策略,抛弃队列中最新请求
            // 设置缓存
            httpConfig.setMaxMemCacheBytesSize(64 * 1024 * 1024);// 设置闪存缓存的最大空间，当满了时清除内存缓存
            httpConfig.setDefaultCacheMode(CacheMode.NetFirst);// 设置全局缓存方式，默认NetOnly方式
            httpConfig.setDefaultCacheExpireMillis(7 * 24 * 60 * 60 * 1000);// 设置全局默认的缓存超时时间，当Request已设置超时时间时无视全局设置，默认为-1，永久不超时
            //httpConfig.setDefaultCacheDir(CHCHE_PATH_BASE + CHCHE_PATH_CACHE_HTTP);// 设置默认缓存存放文件夹，如果Request单独设置了缓存目录，则无视全局设置
            // 设置请求
            //httpConfig.setGlobalSchemeHost(ImageLibFiled.IMAGE_BASE);// 设置全局默认scheme和host，这样Request只需要设置API的路径Path即可。当Request已自带scheme时无视全局设置
            httpConfig.setDefaultModelQueryBuilder(new JsonQueryBuilder());// 设置全局默认参数构建器
            httpConfig.setDefaultHttpMethod(HttpMethods.Get);// 设置全局默认请求方式，默认GET
            httpConfig.setCommonHeaders(headers);// 设置全局的Header，所有请求都会带上这些参数
            httpConfig.setDefaultCharSet("utf-8");// 设置全局默认编码方式，不设置默认utf-8
            httpConfig.setUserAgent("Mozilla/5.0");// 设置全局默认User-Agent
            httpConfig.setGlobalHttpListener(globalHttpListener);// 设置全局监听器，可以协助监听所有请求的各个过程和结果
            // 设置调试
            httpConfig.setDebugged(false);// 设置网络调试开关
            httpConfig.setDoStatistics(false);// 设置全局是否开启流量、耗时等统计

            liteHttp = httpConfig.create();
        }
        return liteHttp;
    }

    public static HttpHeader getHeader(Context context, String body) {
        // 请求服务器时间戳
        String timestamp;
        RequestTimestampResult timestampResult = BLFamilyTimestampPresenter.getTimestamp(context);
        if (timestampResult != null && timestampResult.isSuccess()) {
            timestamp = timestampResult.getTimestamp();
        } else {
            timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        }

        // 组装头部
        HttpHeader baseHeadParam = new HttpHeader();
        baseHeadParam.setTimestampResult(timestampResult);
        baseHeadParam.setLoginsession(BLAcountToAli.getInstance().getBlUserInfo().getBl_loginsession());
        baseHeadParam.setUserid(BLAcountToAli.getInstance().getBlUserInfo().getBl_userid());
        baseHeadParam.setFamilyid(FamilyManager.getInstance().getCurrentFamilyID());
        baseHeadParam.setLanguage(CommonUtils.getLanguage());
        baseHeadParam.setLicenseid(BLLet.getLicenseId());
        baseHeadParam.setTimestamp(timestamp);
        baseHeadParam.setToken(EncryptUtils.MD5String(body == null ? "" : body + BLConstants.STR_BODY_ENCRYPT + baseHeadParam.getTimestamp() + baseHeadParam.getUserid()));

        return baseHeadParam;
    }


    public static boolean addImgJsonAndFile(Context context, boolean isOffical, File file,  FileInfo fileInfo) {
        String respStr = uploadImage(context, BLImageUrl.getAddImgFileUrl(isOffical), file, file.getName());
        if (respStr != null) {
            while (respStr.contains("_id")) {
                respStr = respStr.replace("_id", "id");
            }
            HttpResponse response = JSON.parseObject(respStr, HttpResponse.class);
            if (response != null && response.getStatus() == BLHttpErrCode.SUCCESS) {
                fileInfo.setUrl(BLImageUrl.getFindImgFileUrlByKey(isOffical, response.getId()));

                String url = BLImageUrl.getAddImgJsonUrl(isOffical);
                HttpHeader header = getHeader(context, JSON.toJSONString(fileInfo));

                HttpPostAccessor postAccessor = new HttpPostAccessor();
                response = postAccessor.execute(url, header, JSON.toJSONString(fileInfo), HttpResponse.class);
                if (response != null && response.getStatus() == BLHttpErrCode.SUCCESS) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String uploadImage(Context context, String url, File file, String fileName) {
        try {
            HttpHeader headerObj = getHeader(context, null);
            headerObj.setEncrypt(file);

            Map<String, String> header = new HashMap<>();
            header.put("Userid", headerObj.getUserid());
            header.put("Familyid", headerObj.getFamilyid());
            header.put("LoginSession", headerObj.getLoginsession());
            header.put("Timestamp", headerObj.getTimestamp());
            header.put("Token", headerObj.getToken());
            header.put("FileMd5", headerObj.getFileMd5());
            header.put("ContentSHA1", headerObj.getContentSHA1());

            MultipartBody body = new MultipartBody();
            // 必须添加minetype和filename
            body.addPart(new InputStreamPart("file", new FileInputStream(file), fileName, "image/png"));

            StringRequest request = new StringRequest(url);
            request.setCacheMode(CacheMode.NetOnly);
            request.setMethod(HttpMethods.Post);
            request.setHeaders(header);
            request.setHttpBody(body);

            return getLiteHttp(context).perform(request);
        } catch (Exception e) {
            return null;
        }
    }
}
