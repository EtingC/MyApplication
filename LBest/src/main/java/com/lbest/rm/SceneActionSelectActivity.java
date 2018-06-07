package com.lbest.rm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lbest.rm.data.BaseDeviceInfo;
import com.lbest.rm.data.ParamData;
import com.lbest.rm.data.ParamsMap;
import com.lbest.rm.data.TimeScene;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.productDevice.SceneFactory;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.SAContinueSelectPopwindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SceneActionSelectActivity extends AppCompatActivity implements View.OnClickListener{
    private Toolbar toolbar;
    private TextView toolbar_title;

    private LinearLayout ll_container;
    private TimeScene scene;
    private BaseDeviceInfo baseDeviceInfo;
    private ParamsMap paramsMap;

    private LayoutInflater layoutInflater;
    private int sceneAction=1;//第几个动作

    private View selected_view;
    private HashMap<Integer,ActionData> viewList;

    private SAContinueSelectPopwindow saContinueSelectPopwindow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_action_select);
        layoutInflater = LayoutInflater.from(SceneActionSelectActivity.this);

        initData();
        findview();
        initView();
        setListener();
    }

    @Override
    public void onClick(final View v) {
        int id=v.getId();
        final ActionData ad=viewList.get(id);
        if(ad!=null){
            if(ad.paramData.getType()==ParamData.TYPE_Enum){
                JSONObject jsonObject=scene.getJsonValues();
                if(jsonObject==null){
                    jsonObject=new JSONObject();
                    jsonObject.put("deviceUuid",baseDeviceInfo.getUuid());
                    jsonObject.put("timeZone","GMT+8");
                }
                if(sceneAction==TimeScene.FIRSTACTION){
                    jsonObject.remove("firstActionParams");
                    JSONObject firstAction=new JSONObject();
                    JSONArray firstArray=new JSONArray();
                    firstArray.add(ad.paramData.getKey());
                    firstAction.put("attrSet",firstArray);
                    JSONObject firstjobj=new JSONObject();
                    firstjobj.put("value",ad.value);
                    firstAction.put(ad.paramData.getKey(),firstjobj);
                    jsonObject.put("firstActionParams",firstAction);
                }else if(sceneAction==TimeScene.SECONDACTION){
                    jsonObject.remove("secondActionParams");
                    JSONObject secondAction=new JSONObject();
                    JSONArray secondArray=new JSONArray();
                    secondArray.add(ad.paramData.getKey());
                    secondAction.put("attrSet",secondArray);
                    JSONObject secondjobj=new JSONObject();
                    secondjobj.put("value",ad.value);
                    secondAction.put(ad.paramData.getKey(),secondjobj);
                    jsonObject.put("secondActionParams",secondAction);
                }
                scene.setJsonValues(jsonObject);
                refreshView(ad,v);
                selected_view=v;
            }else  if(ad.paramData.getType()==ParamData.TYPE_Continuous){
                if(saContinueSelectPopwindow==null){
                    saContinueSelectPopwindow=new SAContinueSelectPopwindow(SceneActionSelectActivity.this);
                }
                saContinueSelectPopwindow.setParamData(ad.paramData);
                saContinueSelectPopwindow.setClickListener(new SAContinueSelectPopwindow.onClickListener() {
                    @Override
                    public void onClick(String value) {
                        ad.value=value;

                        JSONObject jsonObject=scene.getJsonValues();
                        if(jsonObject==null){
                            jsonObject=new JSONObject();
                            jsonObject.put("deviceUuid",baseDeviceInfo.getUuid());
                            jsonObject.put("timeZone","GMT+8");
                        }
                        if(sceneAction==TimeScene.FIRSTACTION){
                            jsonObject.remove("firstActionParams");
                            JSONObject firstAction=new JSONObject();
                            JSONArray firstArray=new JSONArray();
                            firstArray.add(ad.paramData.getKey());
                            firstAction.put("attrSet",firstArray);
                            JSONObject firstjobj=new JSONObject();
                            firstjobj.put("value",ad.value);
                            firstAction.put(ad.paramData.getKey(),firstjobj);
                            jsonObject.put("firstActionParams",firstAction);
                        }else if(sceneAction==TimeScene.SECONDACTION){
                            jsonObject.remove("secondActionParams");
                            JSONObject secondAction=new JSONObject();
                            JSONArray secondArray=new JSONArray();
                            secondArray.add(ad.paramData.getKey());
                            secondAction.put("attrSet",secondArray);
                            JSONObject secondjobj=new JSONObject();
                            secondjobj.put("value",ad.value);
                            secondAction.put(ad.paramData.getKey(),secondjobj);
                            jsonObject.put("secondActionParams",secondAction);
                        }
                        scene.setJsonValues(jsonObject);
                        refreshView(ad,v);
                        selected_view=v;
                    }
                });
                saContinueSelectPopwindow.showWindow(getWindow().getDecorView());
            }
        }
    }

    private void initData() {
        scene = (TimeScene) getIntent().getSerializableExtra(Constants.INTENT_SCENETIME);
        baseDeviceInfo = getIntent().getParcelableExtra(Constants.INTENT_DEVICE);
        sceneAction = getIntent().getIntExtra(Constants.INTENT_SCENEACTION,0);
        if (baseDeviceInfo != null) {
            paramsMap = Product.getParamsMapByModel(baseDeviceInfo.getModel());
        }

        viewList=new HashMap<>();
    }

    private void findview() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        ll_container= (LinearLayout) findViewById(R.id.ll_container);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent=new Intent();
        intent.putExtra(Constants.INTENT_SCENETIME,scene);
        setResult(RESULT_OK,intent);
        SceneActionSelectActivity.this.finish();
    }

    private void setListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.putExtra(Constants.INTENT_SCENETIME,scene);
                intent.putExtra(Constants.INTENT_SCENEACTION,sceneAction);
                setResult(RESULT_OK,intent);
                SceneActionSelectActivity.this.finish();
            }
        });
    }

    private void initView() {
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_sceneaction));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));


        try {
            if (paramsMap != null) {
                String param=null;
                String value=null;
                try {
                    if(scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_OFF)||scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON)){
                        param = (String) scene.getJsonValues().getJSONObject("firstActionParams").getJSONArray("attrSet").get(0);
                        value = (String) scene.getJsonValues().getJSONObject("firstActionParams").getJSONObject(param).get("value");
                    }else  if(scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON_OFF)){
                        if(sceneAction==TimeScene.FIRSTACTION){
                            param = (String) scene.getJsonValues().getJSONObject("firstActionParams").getJSONArray("attrSet").get(0);
                            value = (String) scene.getJsonValues().getJSONObject("firstActionParams").getJSONObject(param).get("value");
                        }else if(sceneAction==TimeScene.SECONDACTION){
                            param = (String) scene.getJsonValues().getJSONObject("secondActionParams").getJSONArray("attrSet").get(0);
                            value = (String) scene.getJsonValues().getJSONObject("secondActionParams").getJSONObject(param).get("value");
                        }
                    }
                }catch (Exception e){}
                ArrayList<ParamData> paramDataList = paramsMap.getParams_ali();
                int index=0;
                for (ParamData paramData : paramDataList) {
                    boolean isSelectParam=false;
                    if (paramData.getKey().equals(param)) {
                        isSelectParam=true;
                    }
                    if(paramData.getAct().equals(ParamData.ACT_WRITE)||paramData.getAct().equals(ParamData.ACT_READWRITE)){
                        if (paramData.getType() == ParamData.TYPE_Enum) {
                            View rl_paramname = layoutInflater.inflate(R.layout.sceneaction_paramname, null);
                            TextView tv_paramName=(TextView) rl_paramname.findViewById(R.id.tv_paramname);
                            tv_paramName.setText(paramData.getName());
                            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                            if(index==0){
                                layoutParams.setMargins(0,0,0,20);
                            }else{
                                layoutParams.setMargins(0,80,0,20);
                            }
                            ll_container.addView(rl_paramname,layoutParams);

                            List<ParamData.Enum> enumList = paramData.getValue_range1();
                            for (ParamData.Enum enum_data : enumList) {
                                View rl_item_enum = layoutInflater.inflate(R.layout.sceneaction_item_enum, null);
                                TextView tv_enumname = (TextView) rl_item_enum.findViewById(R.id.tv_enumname);
                                ImageView iv_select = (ImageView) rl_item_enum.findViewById(R.id.iv_select);
                                tv_enumname.setText(enum_data.name);
                                rl_item_enum.setTag(enum_data);
                                ActionData ad=new ActionData();
                                ad.paramData=paramData;
                                ad.value=enum_data.value;
                                int viewID=ad.getID();
                                viewList.put(viewID,ad);
                                rl_item_enum.setId(viewID);
                                rl_item_enum.setOnClickListener(SceneActionSelectActivity.this);
                                if(isSelectParam){
                                    if (enum_data.value.equals(value)) {
                                        selected_view=rl_item_enum;
                                        iv_select.setImageResource(R.drawable.icon_selected);
                                    } else {
                                        iv_select.setImageResource(R.drawable.icon_noselected);
                                    }
                                }else{
                                    iv_select.setImageResource(R.drawable.icon_noselected);
                                }
                                ll_container.addView(rl_item_enum);
                            }
                        } else if (paramData.getType() == ParamData.TYPE_Continuous) {

                            View rl_paramname = layoutInflater.inflate(R.layout.sceneaction_paramname, null);
                            TextView tv_paramName=(TextView) rl_paramname.findViewById(R.id.tv_paramname);
                            tv_paramName.setText(paramData.getName());
                            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                            if(index==0){
                                layoutParams.setMargins(0,0,0,20);
                            }else{
                                layoutParams.setMargins(0,80,0,20);
                            }
                            ll_container.addView(rl_paramname,layoutParams);

                            View rl_item_continue = layoutInflater.inflate(R.layout.sceneaction_item_continues, null);
                            TextView tv_paramname = (TextView) rl_item_continue.findViewById(R.id.tv_paramname);
                            TextView tv_value = (TextView) rl_item_continue.findViewById(R.id.tv_value);
                            tv_paramname.setText(getResources().getString(R.string.str_set)+paramData.getName());
                            if(isSelectParam){
                                tv_value.setText(value+" "+paramData.getUnit());
                            }

                            ActionData ad=new ActionData();
                            ad.paramData=paramData;
                            int viewID=ad.getID();
                            if(isSelectParam){
                                selected_view=rl_item_continue;
                                ad.value=value;
                            }
                            viewList.put(viewID,ad);
                            rl_item_continue.setId(viewID);
                            rl_item_continue.setOnClickListener(SceneActionSelectActivity.this);

                            ll_container.addView(rl_item_continue);
                        }
                    }
                    index++;
                }
            }
        } catch (Exception e) {
            Logutils.log_e("param error:", e);
        }
    }


    private void refreshView(ActionData ad,View view){
        if(selected_view!=null){
            ActionData ad_last=viewList.get(selected_view.getId());
            if(ad_last.paramData.getType()==ParamData.TYPE_Enum){
                ImageView iv_select_last = (ImageView) selected_view.findViewById(R.id.iv_select);
                iv_select_last.setImageResource(R.drawable.icon_noselected);
            }else  if(ad_last.paramData.getType()==ParamData.TYPE_Continuous){
                TextView tv_value_last = (TextView) selected_view.findViewById(R.id.tv_value);
                tv_value_last.setText("");
            }
        }
        if(ad.paramData.getType()==ParamData.TYPE_Enum){
            ImageView iv_select_current = (ImageView) view.findViewById(R.id.iv_select);
            iv_select_current.setImageResource(R.drawable.icon_selected);
        }else  if(ad.paramData.getType()==ParamData.TYPE_Continuous){
            TextView tv_value_current = (TextView) view.findViewById(R.id.tv_value);
            tv_value_current.setText(ad.value+" "+ad.paramData.getUnit());
        }
    }

    class ActionData{
        ParamData paramData;
        String value;
        int getID(){
            String IDString=paramData.getKey()+paramData.getType()+value;
            int id=IDString.hashCode();
            Logutils.log_d("getViewID:"+IDString+"   "+id);
            return id;
        }
    }
}
