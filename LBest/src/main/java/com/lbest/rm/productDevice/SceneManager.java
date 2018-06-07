package com.lbest.rm.productDevice;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.alink.business.alink.ALinkBusinessEx;
import com.aliyun.alink.sdk.net.anet.api.AError;
import com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryRequest;
import com.aliyun.alink.sdk.net.anet.api.transitorynet.TransitoryResponse;
import com.lbest.rm.BaseCallback;
import com.lbest.rm.data.SceneModel;
import com.lbest.rm.data.TimeScene;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by dell on 2017/11/1.
 */

public class SceneManager {

    public final static String METHOD_ADDSCENE="mtop.openalink.case.template.case.add";
    public final static String METHOD_NEXTSCENE="mtop.openalink.case.device.snapshot.get";//设备下一次定时促发的时间
    public final static String METHOD_UPDATESCENE="mtop.openalink.case.template.case.update";
    public final static String METHOD_GETDEVICESCENE="mtop.openalink.case.template.case.query";//查询同一设备下所有用户创建的场景

    public final static String METHOD_GETUSERDEVICESCENE="mtop.openalink.case.template.case.get";//查询单条场景
    public final static String METHOD_DELETESCENE="mtop.openalink.case.template.case.delete";
    //回调接口
    public abstract static class SceneOperateCallBack extends BaseCallback {
        public SceneOperateCallBack(Context mContext) {
            super(mContext);
        }
    }

    public static void addNewScene(TimeScene scene, final SceneOperateCallBack sceneCallBack){
        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_ADDSCENE);
        transitoryRequest.putParam("deviceUuid", scene.getDeviceUuid());
        transitoryRequest.putParam("templateId", scene.getTemplateId());
        transitoryRequest.putParam("jsonValues", String.valueOf(scene.getJsonValues()));
        transitoryRequest.putParam("state", scene.getState());
        transitoryRequest.putParam("name", scene.getName());
        transitoryRequest.putParam("sceneGroup", scene.getSceneGroup());

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(sceneCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    sceneCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(sceneCallBack!=null){
                    int code=aError.getCode();
                    String msg=aError.getMsg();
                    sceneCallBack.callBack(code,msg,null);
                }
            }
        });
    }


    public static void updateScene(String uuid,TimeScene scene, final SceneOperateCallBack sceneCallBack){

        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_UPDATESCENE);
        transitoryRequest.putParam("id", scene.getId());
        transitoryRequest.putParam("deviceUuid", uuid);
        transitoryRequest.putParam("jsonValues", String.valueOf(scene.getJsonValues()));
        transitoryRequest.putParam("state", scene.getState());
        transitoryRequest.putParam("name", scene.getName());
        transitoryRequest.putParam("sceneGroup", scene.getSceneGroup());

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(sceneCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    sceneCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(sceneCallBack!=null){
                    int code=aError.getCode();
                    String msg=aError.getMsg();
                    sceneCallBack.callBack(code,msg,null);
                }
            }
        });
    }


    //查询设备下一次定时促发的时间
    public static void getDeviceNextScene(String deviceId, final SceneOperateCallBack sceneCallBack){

        JSONObject jsonObject=new JSONObject();
        jsonObject.put("deviceId",deviceId);

        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_NEXTSCENE);
        transitoryRequest.putParam("condition", jsonObject.toJSONString());

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(sceneCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    sceneCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(sceneCallBack!=null){
                    int code=aError.getCode();
                    String msg=aError.getMsg();
                    sceneCallBack.callBack(code,msg,null);
                }
            }
        });
    }


    public static void getDeviceAllScene(String deviceUuid, String templateId, String sceneGroup,final SceneOperateCallBack sceneCallBack){

        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_GETDEVICESCENE);
        transitoryRequest.putParam("deviceUuid", deviceUuid);
        if(!TextUtils.isEmpty(sceneGroup)){
            transitoryRequest.putParam("sceneGroup", sceneGroup);
        }
        if(!TextUtils.isEmpty(templateId)){
            transitoryRequest.putParam("templateId", templateId);
        }

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(sceneCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    sceneCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(sceneCallBack!=null){
                    int code=aError.getCode();
                    String msg=aError.getMsg();
                    sceneCallBack.callBack(code,msg,null);
                }
            }
        });
    }


    public static void getDeviceSceneDetails(String id, String creator, String deviceUuid ,final SceneOperateCallBack sceneCallBack){

        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_GETUSERDEVICESCENE);
        transitoryRequest.putParam("id", id);
        transitoryRequest.putParam("creator", creator);
        transitoryRequest.putParam("deviceUuid", deviceUuid);


        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(sceneCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    sceneCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(sceneCallBack!=null){
                    int code=aError.getCode();
                    String msg=aError.getMsg();
                    sceneCallBack.callBack(code,msg,null);
                }
            }
        });
    }



    public static void deleteScene(String deviceUuid, String id, String creator,final SceneOperateCallBack sceneCallBack){
        TransitoryRequest transitoryRequest = new TransitoryRequest();
        transitoryRequest.setMethod(METHOD_DELETESCENE);
        transitoryRequest.putParam("deviceUuid", deviceUuid);
        transitoryRequest.putParam("id", id);
        transitoryRequest.putParam("creator", creator);

        ALinkBusinessEx biz = new ALinkBusinessEx();
        biz.request(transitoryRequest, new ALinkBusinessEx.IListener() {
            @Override
            public void onSuccess(TransitoryRequest transitoryRequest, TransitoryResponse transitoryResponse) {
                // do something for success
                if(sceneCallBack!=null){
                    int code=transitoryResponse.getError().getSubCode();
                    String msg=transitoryResponse.getError().getSubMsg();
                    sceneCallBack.callBack(code,msg,transitoryResponse.data);
                }
            }

            @Override
            public void onFailed(TransitoryRequest transitoryRequest, AError aError) {
                // do something for failed
                if(sceneCallBack!=null){
                    int code=aError.getCode();
                    String msg=aError.getMsg();
                    sceneCallBack.callBack(code,msg,null);
                }
            }
        });
    }



}
