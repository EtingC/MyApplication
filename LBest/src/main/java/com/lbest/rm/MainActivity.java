package com.lbest.rm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lbest.rm.data.TimeScene;
import com.lbest.rm.data.queryDeviceSceneResponse;
import com.lbest.rm.productDevice.SceneManager;
import com.lbest.rm.productDevice.SceneFactory;


public class MainActivity extends Activity implements View.OnClickListener{
    private final int REQUEST_CODE_ADD_DEVICE=1;
    private Button bt_login;
    private Button bt_logout;
    private Button bt_productlist;
    private Button bt_deviceconfig;
    private Button bt_devicelist;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt_login=(Button)findViewById(R.id.bt_login);
        bt_logout=(Button)findViewById(R.id.bt_logout);

        bt_productlist=(Button)findViewById(R.id.bt_productlist);
        bt_deviceconfig=(Button)findViewById(R.id.bt_deviceconfig);
        bt_devicelist=(Button)findViewById(R.id.bt_devicelist);

        bt_login.setOnClickListener(this);
        bt_logout.setOnClickListener(this);
        bt_productlist.setOnClickListener(this);
        bt_deviceconfig.setOnClickListener(this);
        bt_devicelist.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id=v.getId();
        Intent intent=null;
        switch (id){
            case R.id.bt_login:
//                SceneManager.deleteScene("EF55A8A5F292D595F40D119339C9CF4A","58045866", "500001256111165663", new SceneManager.SceneOperateCallBack(MainActivity.this) {
//                    @Override
//                    public void callBack(int code, String msg, Object data) {
//                        super.callBack(code, msg, data);
//                    }
//                });
                break;
            case R.id.bt_logout:
                TimeScene timeScene= (TimeScene) SceneFactory.createScene(SceneFactory.SCENE_APPOINT_ON_OFF);
                timeScene.setDeviceUuid("EF55A8A5F292D595F40D119339C9CF4A");
                timeScene.setState("1");
                timeScene.setSceneGroup("AlinkRouterSubDeviceLimitInternet");
                timeScene.setName("myscene_on_off");

                JSONObject jsonObject=new JSONObject();
                jsonObject.put("deviceUuid","EF55A8A5F292D595F40D119339C9CF4A");
                jsonObject.put("timeZone","GMT+8");
                jsonObject.put("firstHour","13");
                jsonObject.put("firstMinute","55");
                jsonObject.put("secondHour","13");
                jsonObject.put("secondMinute","56");

                JSONObject firstAction=new JSONObject();
                JSONArray firstArray=new JSONArray();
                firstArray.add("Switch");
                firstAction.put("attrSet",firstArray);
                JSONObject firstjobj=new JSONObject();
                firstjobj.put("value","1");
                firstAction.put("Switch",firstjobj);
                jsonObject.put("firstActionParams",firstAction);


                JSONObject secondAction=new JSONObject();
                JSONArray secondArray=new JSONArray();
                secondArray.add("Switch");
                secondAction.put("attrSet",secondAction);
                JSONObject secondjobj=new JSONObject();
                secondjobj.put("value","0");
                secondAction.put("Switch",secondjobj);
                jsonObject.put("secondActionParams",secondAction);

                timeScene.setJsonValues(jsonObject);

                SceneManager.addNewScene(timeScene, new SceneManager.SceneOperateCallBack(getApplicationContext()) {
                    @Override
                    public void callBack(int code, String msg, Object data) {
                        super.callBack(code, msg, data);
                    }
                });
                break;
            case R.id.bt_productlist:
                intent=new Intent();
                intent.setClass(MainActivity.this,ProductListActivity.class);
                startActivity(intent);
                break;
            case R.id.bt_deviceconfig:
                SceneManager.getDeviceAllScene("EF55A8A5F292D595F40D119339C9CF4A", SceneFactory.SCENE_APPOINT_ON_OFF,null,new SceneManager.SceneOperateCallBack(getApplicationContext()) {
                    @Override
                    public void callBack(int code, String msg, Object data) {
                        super.callBack(code, msg, data);
                        if(code==1000){
                            queryDeviceSceneResponse response= JSON.parseObject((String) data,queryDeviceSceneResponse.class);
                        }
                    }
                });


                break;
            case R.id.bt_devicelist:
                   intent=new Intent();
                   intent.setClass(MainActivity.this,HomeActivity.class);
                   startActivity(intent);
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //设备配网回调
        if (REQUEST_CODE_ADD_DEVICE == requestCode) {
            // parse data from data
            // and do your business
            return;
        }
    }
}
