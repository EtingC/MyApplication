package com.lbest.rm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lbest.rm.data.BaseDeviceInfo;
import com.lbest.rm.data.ParamData;
import com.lbest.rm.data.ParamsMap;
import com.lbest.rm.data.SceneModel;
import com.lbest.rm.data.TimeScene;
import com.lbest.rm.productDevice.Product;
import com.lbest.rm.productDevice.SceneFactory;
import com.lbest.rm.productDevice.SceneManager;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.ChooseSceneModelPopwindow;
import com.lbest.rm.view.SceneTimeSelectPopwindow;

import java.util.ArrayList;
import java.util.List;

public class SceneEditActivity extends AppCompatActivity implements View.OnClickListener {
    private Toolbar toolbar;
    private TextView toolbar_title;

    private RelativeLayout rl_scenemodel;
    private RelativeLayout rl_firstaction;
    private RelativeLayout rl_firstactiontime;
    private RelativeLayout rl_secondaction;
    private RelativeLayout rl_secondactiontime;
    private RelativeLayout rl_scenerepeat;

    private TextView tv_modelname;
    private TextView tv_openset;
    private TextView tv_firstaction;
    private TextView tv_firstactiontime;
    private TextView tv_closeset;
    private TextView tv_secondaction;
    private TextView tv_secondactiontime;
    private TextView tv_scenerepeat;

    private Button bt_save;


    private TimeScene scene;
    private BaseDeviceInfo baseDeviceInfo;
    private ParamsMap paramsMap;

    private ChooseSceneModelPopwindow sceneModelPopwindow;
    private SceneTimeSelectPopwindow sceneTimeSelectPopwindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_edit);
        initData();
        findview();
        initView();
        setListener();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == Constants.REQUESTCODE_SCENEACTION) {
            scene = (TimeScene) data.getSerializableExtra(Constants.INTENT_SCENETIME);
            int action = data.getIntExtra(Constants.INTENT_SCENEACTION, 0);
            updateActionView(action);
        } else if (resultCode == RESULT_OK && requestCode == Constants.REQUESTCODE_SCENEREPEAT) {
            String weekStr = data.getStringExtra(Constants.INTENT_SCENEREPEAT);
            JSONObject jsonObject = scene.getJsonValues();
            if (jsonObject == null) {
                jsonObject = new JSONObject();
            }
            jsonObject.put("week", weekStr);
            scene.setJsonValues(jsonObject);
            updateSceneRepeatView();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.rl_scenemodel:
                if (sceneModelPopwindow == null) {
                    sceneModelPopwindow = new ChooseSceneModelPopwindow(SceneEditActivity.this);
                    sceneModelPopwindow.setClickListener(new ChooseSceneModelPopwindow.onItemClickListener() {
                        @Override
                        public void onClick(String value) {
                            if (scene == null) {
                                scene = new TimeScene();
                                scene.setState(SceneModel.SCENE_ACTIVE);
                                scene.setDeviceUuid(baseDeviceInfo.getUuid());
                            }
                            scene.setTemplateId(value);
                            updateViewByModelTemplate(value);
                        }
                    });
                }
                sceneModelPopwindow.showWindow(getWindow().getDecorView());
                break;
            case R.id.rl_firstaction:
                Intent intent1 = new Intent();
                intent1.setClass(SceneEditActivity.this, SceneActionSelectActivity.class);
                intent1.putExtra(Constants.INTENT_DEVICE, baseDeviceInfo);
                if (scene == null) {
                    scene = new TimeScene();
                    scene.setState(SceneModel.SCENE_ACTIVE);
                    scene.setDeviceUuid(baseDeviceInfo.getUuid());
                    scene.setTemplateId(SceneFactory.SCENE_APPOINT_ON);
                }
                intent1.putExtra(Constants.INTENT_SCENETIME, scene);
                intent1.putExtra(Constants.INTENT_SCENEACTION, TimeScene.FIRSTACTION);
                startActivityForResult(intent1, Constants.REQUESTCODE_SCENEACTION);
                break;
            case R.id.rl_firstactiontime:
                if (sceneTimeSelectPopwindow == null) {
                    sceneTimeSelectPopwindow = new SceneTimeSelectPopwindow(SceneEditActivity.this);
                }
                sceneTimeSelectPopwindow.setAction(TimeScene.FIRSTACTION);
                sceneTimeSelectPopwindow.setClickListener(new SceneTimeSelectPopwindow.onClickListener() {
                    @Override
                    public void onClick(int action, String value1, String value2) {
                        if (scene == null) {
                            scene = new TimeScene();
                            scene.setState(SceneModel.SCENE_ACTIVE);
                            scene.setDeviceUuid(baseDeviceInfo.getUuid());
                            scene.setTemplateId(SceneFactory.SCENE_APPOINT_ON);
                        }
                        JSONObject jsonObject = scene.getJsonValues();
                        if (jsonObject == null) {
                            jsonObject = new JSONObject();
                            jsonObject.put("deviceUuid", baseDeviceInfo.getUuid());
                            jsonObject.put("timeZone", "GMT+8");
                        }
                        if (scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON) || scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_OFF)) {
                            jsonObject.put("hour", value1);
                            jsonObject.put("minute", value2);
                        } else if (scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
                            if (action == TimeScene.FIRSTACTION) {
                                jsonObject.put("firstHour", value1);
                                jsonObject.put("firstMinute", value2);
                            } else if (action == TimeScene.SECONDACTION) {
                                jsonObject.put("secondHour", value1);
                                jsonObject.put("secondMinute", value2);
                            }
                        }
                        scene.setJsonValues(jsonObject);
                        updateSceneTimeView();
                    }
                });
                sceneTimeSelectPopwindow.showWindow(getWindow().getDecorView());
                break;
            case R.id.rl_secondaction:
                Intent intent2 = new Intent();
                intent2.setClass(SceneEditActivity.this, SceneActionSelectActivity.class);
                intent2.putExtra(Constants.INTENT_DEVICE, baseDeviceInfo);
                if (scene == null) {
                    scene = new TimeScene();
                    scene.setState(SceneModel.SCENE_ACTIVE);
                    scene.setDeviceUuid(baseDeviceInfo.getUuid());
                    scene.setTemplateId(SceneFactory.SCENE_APPOINT_ON);
                }
                intent2.putExtra(Constants.INTENT_SCENETIME, scene);
                intent2.putExtra(Constants.INTENT_SCENEACTION, TimeScene.SECONDACTION);
                startActivityForResult(intent2, Constants.REQUESTCODE_SCENEACTION);
                break;
            case R.id.rl_secondactiontime:
                if (sceneTimeSelectPopwindow == null) {
                    sceneTimeSelectPopwindow = new SceneTimeSelectPopwindow(SceneEditActivity.this);
                }
                sceneTimeSelectPopwindow.setAction(TimeScene.SECONDACTION);
                sceneTimeSelectPopwindow.setClickListener(new SceneTimeSelectPopwindow.onClickListener() {
                    @Override
                    public void onClick(int action, String value1, String value2) {
                        if (scene == null) {
                            scene = new TimeScene();
                            scene.setState(SceneModel.SCENE_ACTIVE);
                            scene.setDeviceUuid(baseDeviceInfo.getUuid());
                            scene.setTemplateId(SceneFactory.SCENE_APPOINT_ON);
                        }
                        JSONObject jsonObject = scene.getJsonValues();
                        if (jsonObject == null) {
                            jsonObject = new JSONObject();
                            jsonObject.put("deviceUuid", baseDeviceInfo.getUuid());
                            jsonObject.put("timeZone", "GMT+8");
                        }
                        if (scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON) || scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_OFF)) {
                            jsonObject.put("hour", value1);
                            jsonObject.put("minute", value2);
                        } else if (scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
                            if (action == TimeScene.FIRSTACTION) {
                                jsonObject.put("firstHour", value1);
                                jsonObject.put("firstMinute", value2);
                            } else if (action == TimeScene.SECONDACTION) {
                                jsonObject.put("secondHour", value1);
                                jsonObject.put("secondMinute", value2);
                            }
                        }
                        scene.setJsonValues(jsonObject);
                        updateSceneTimeView();
                    }
                });
                sceneTimeSelectPopwindow.showWindow(getWindow().getDecorView());
                break;
            case R.id.rl_scenerepeat:
                Intent intent3 = new Intent();
                if (scene == null) {
                    scene = new TimeScene();
                    scene.setState(SceneModel.SCENE_ACTIVE);
                    scene.setDeviceUuid(baseDeviceInfo.getUuid());
                    scene.setTemplateId(SceneFactory.SCENE_APPOINT_ON);
                }
                JSONObject jsonObject = scene.getJsonValues();
                String weekStr = null;
                if (jsonObject != null) {
                    weekStr = jsonObject.getString("week");
                }
                intent3.putExtra(Constants.INTENT_SCENEREPEAT, weekStr);
                intent3.setClass(SceneEditActivity.this, SceneRepeatSelectActivity.class);
                startActivityForResult(intent3, Constants.REQUESTCODE_SCENEREPEAT);
                break;
            case R.id.bt_save:
                if (checkSceneData()) {
                    if (TextUtils.isEmpty(scene.getId())) {
                        SceneManager.addNewScene(scene, new SceneManager.SceneOperateCallBack(SceneEditActivity.this) {
                            @Override
                            public void callBack(int code, String msg, Object data) {
                                super.callBack(code, msg, data);
                                if (code == Constants.AliErrorCode.SUCCESS_CODE) {
                                    Toast.makeText(SceneEditActivity.this, getResources().getString(R.string.str_scene_savesuccess), Toast.LENGTH_LONG).show();
                                    SceneEditActivity.this.finish();
                                } else {
                                    Toast.makeText(SceneEditActivity.this, getResources().getString(R.string.str_scene_savefail) + ":" + msg + " " + code, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        SceneManager.updateScene(baseDeviceInfo.getUuid(), scene, new SceneManager.SceneOperateCallBack(SceneEditActivity.this) {
                            @Override
                            public void callBack(int code, String msg, Object data) {
                                super.callBack(code, msg, data);
                                if (code == Constants.AliErrorCode.SUCCESS_CODE) {
                                    Toast.makeText(SceneEditActivity.this, getResources().getString(R.string.str_scene_savesuccess), Toast.LENGTH_LONG).show();
                                    SceneEditActivity.this.finish();
                                } else {
                                    Toast.makeText(SceneEditActivity.this, getResources().getString(R.string.str_scene_savefail) + ":" + msg + " " + code, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(SceneEditActivity.this, getResources().getString(R.string.str_scene_dataerror), Toast.LENGTH_LONG).show();
                }
                break;
            default:
        }
    }

    private void initData() {
        scene = (TimeScene) getIntent().getSerializableExtra(Constants.INTENT_SCENETIME);
        baseDeviceInfo = getIntent().getParcelableExtra(Constants.INTENT_DEVICE);
        if (baseDeviceInfo != null) {
            paramsMap = Product.getParamsMapByModel(baseDeviceInfo.getModel());
        }
        Logutils.log_d("scene");
    }

    private void findview() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

        rl_scenemodel = (RelativeLayout) findViewById(R.id.rl_scenemodel);
        rl_firstaction = (RelativeLayout) findViewById(R.id.rl_firstaction);
        rl_firstactiontime = (RelativeLayout) findViewById(R.id.rl_firstactiontime);
        rl_secondaction = (RelativeLayout) findViewById(R.id.rl_secondaction);
        rl_secondactiontime = (RelativeLayout) findViewById(R.id.rl_secondactiontime);
        rl_scenerepeat = (RelativeLayout) findViewById(R.id.rl_scenerepeat);

        tv_modelname = (TextView) findViewById(R.id.tv_modelname);
        tv_openset = (TextView) findViewById(R.id.tv_openset);
        tv_firstaction = (TextView) findViewById(R.id.tv_firstaction);
        tv_firstactiontime = (TextView) findViewById(R.id.tv_firstactiontime);
        tv_closeset = (TextView) findViewById(R.id.tv_closeset);
        tv_secondaction = (TextView) findViewById(R.id.tv_secondaction);
        tv_secondactiontime = (TextView) findViewById(R.id.tv_secondactiontime);
        tv_scenerepeat = (TextView) findViewById(R.id.tv_scenerepeat);

        bt_save = (Button) findViewById(R.id.bt_save);
    }

    private void setListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SceneEditActivity.this.finish();
            }
        });

        rl_scenemodel.setOnClickListener(this);
        rl_firstaction.setOnClickListener(this);
        rl_firstactiontime.setOnClickListener(this);
        rl_secondaction.setOnClickListener(this);
        rl_secondactiontime.setOnClickListener(this);
        rl_scenerepeat.setOnClickListener(this);
        bt_save.setOnClickListener(this);
    }

    private void initView() {
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_sceneset));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        if (scene != null) {

            rl_scenemodel.setEnabled(false);
            String weekStr = scene.getJsonValues().getString("week");
            if (TextUtils.isEmpty(weekStr)) {
                tv_scenerepeat.setText(getString(R.string.str_scene_onlyonece));
            } else {
                String weekArray[] = weekStr.split(",");
                StringBuffer sb = new StringBuffer();
                if (weekArray != null && weekArray.length > 0) {
                    String weekArrayRes[] = getResources().getStringArray(R.array.weeks);
                    int i = 0;
                    for (String week : weekArray) {
                        if (week.equals(TimeScene.SCENEWEEK_7)) {
                            sb.append(weekArrayRes[0]);
                        } else if (week.equals(TimeScene.SCENEWEEK_1)) {
                            sb.append(weekArrayRes[1]);
                        } else if (week.equals(TimeScene.SCENEWEEK_2)) {
                            sb.append(weekArrayRes[2]);
                        } else if (week.equals(TimeScene.SCENEWEEK_3)) {
                            sb.append(weekArrayRes[3]);
                        } else if (week.equals(TimeScene.SCENEWEEK_4)) {
                            sb.append(weekArrayRes[4]);
                        } else if (week.equals(TimeScene.SCENEWEEK_5)) {
                            sb.append(weekArrayRes[5]);
                        } else if (week.equals(TimeScene.SCENEWEEK_6)) {
                            sb.append(weekArrayRes[6]);
                        }
                        if (i < (weekArray.length - 1)) {
                            sb.append(",");
                        }
                        i++;
                    }
                }
                tv_scenerepeat.setText(sb.toString());
            }

            String templateid = scene.getTemplateId();
            if (templateid.equals(SceneFactory.SCENE_APPOINT_OFF)) {
                tv_modelname.setText(getResources().getString(R.string.str_scene_close));
                String time = scene.getJsonValues().get("hour") + ":" + scene.getJsonValues().get("minute");
                tv_firstactiontime.setText(time);

                String action = getFirstAction(scene);
                if (!TextUtils.isEmpty(action)) {
                    tv_firstaction.setText(action);
                }

                tv_openset.setVisibility(View.GONE);
                tv_closeset.setVisibility(View.GONE);
                rl_secondaction.setVisibility(View.GONE);
                rl_secondactiontime.setVisibility(View.GONE);
            } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON)) {
                tv_modelname.setText(getResources().getString(R.string.str_scene_open));
                String hour = (String) scene.getJsonValues().get("hour");
                String minute = (String) scene.getJsonValues().get("minute");
                if (TextUtils.isEmpty(hour) || TextUtils.isEmpty(minute)) {

                } else {
                    String time = getResources().getString(R.string.str_scene_timeformat, Integer.valueOf(hour), Integer.valueOf(minute));
                    tv_firstactiontime.setText(time);
                }

                String action = getFirstAction(scene);
                if (!TextUtils.isEmpty(action)) {
                    tv_firstaction.setText(action);
                }


                tv_openset.setVisibility(View.GONE);
                tv_closeset.setVisibility(View.GONE);
                rl_secondaction.setVisibility(View.GONE);
                rl_secondactiontime.setVisibility(View.GONE);
            } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
                tv_modelname.setText(getResources().getString(R.string.str_scene_openclose));

                String first_hour = (String) scene.getJsonValues().get("firstHour");
                String first_minute = (String) scene.getJsonValues().get("firstMinute");
                if (TextUtils.isEmpty(first_hour) || TextUtils.isEmpty(first_minute)) {

                } else {
                    String firstTime = getResources().getString(R.string.str_scene_timeformat, Integer.valueOf(first_hour), Integer.valueOf(first_minute));
                    tv_firstactiontime.setText(firstTime);
                }

                String second_hour = (String) scene.getJsonValues().get("secondHour");
                String second_minute = (String) scene.getJsonValues().get("secondMinute");
                if (TextUtils.isEmpty(second_hour) || TextUtils.isEmpty(second_minute)) {

                } else {
                    String secondTime = getResources().getString(R.string.str_scene_timeformat, Integer.valueOf(second_hour), Integer.valueOf(second_minute));
                    tv_secondactiontime.setText(secondTime);
                }


                String firstAction = getFirstAction(scene);
                if (!TextUtils.isEmpty(firstAction)) {
                    tv_firstaction.setText(firstAction);
                }


                String secondAction = getSecondAction(scene);
                if (!TextUtils.isEmpty(secondAction)) {
                    tv_secondaction.setText(secondAction);
                }

                tv_openset.setVisibility(View.VISIBLE);
                tv_closeset.setVisibility(View.VISIBLE);
                rl_secondaction.setVisibility(View.VISIBLE);
                rl_secondactiontime.setVisibility(View.VISIBLE);
            }
        } else {
            tv_openset.setVisibility(View.GONE);
            tv_closeset.setVisibility(View.GONE);
            rl_secondaction.setVisibility(View.GONE);
            rl_secondactiontime.setVisibility(View.GONE);
            rl_scenemodel.setEnabled(true);
        }
    }


    private void updateViewByModelTemplate(String templateid) {
        if (templateid.equals(SceneFactory.SCENE_APPOINT_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_close));

            tv_openset.setVisibility(View.GONE);
            tv_closeset.setVisibility(View.GONE);
            rl_secondaction.setVisibility(View.GONE);
            rl_secondactiontime.setVisibility(View.GONE);
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_open));

            tv_openset.setVisibility(View.GONE);
            tv_closeset.setVisibility(View.GONE);
            rl_secondaction.setVisibility(View.GONE);
            rl_secondactiontime.setVisibility(View.GONE);
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_openclose));

            tv_openset.setVisibility(View.VISIBLE);
            tv_closeset.setVisibility(View.VISIBLE);
            rl_secondaction.setVisibility(View.VISIBLE);
            rl_secondactiontime.setVisibility(View.VISIBLE);
        }
    }

    private String getFirstAction(TimeScene scene) {
        String firstAction = null;
        try {
            if (paramsMap != null && scene.getJsonValues() != null) {
                String param = (String) scene.getJsonValues().getJSONObject("firstActionParams").getJSONArray("attrSet").get(0);
                String value = (String) scene.getJsonValues().getJSONObject("firstActionParams").getJSONObject(param).get("value");
                ArrayList<ParamData> paramDataList = paramsMap.getParams_ali();
                for (ParamData paramData : paramDataList) {
                    if (paramData.getKey().equals(param)) {
                        if (paramData.getType() == ParamData.TYPE_Enum) {
                            List<ParamData.Enum> enumList = paramData.getValue_range1();
                            for (ParamData.Enum enum_data : enumList) {
                                if (enum_data.value.equals(value)) {
                                    firstAction = enum_data.name;
                                    break;
                                }
                            }
                        } else if (paramData.getType() == ParamData.TYPE_Continuous) {
                            firstAction = value + "  " + paramData.getUnit();
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Logutils.log_e("param error:", e);
        }
        return firstAction;
    }

    private void updateActionView(int action) {

        String templateid = scene.getTemplateId();
        if (templateid.equals(SceneFactory.SCENE_APPOINT_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_close));
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_open));
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_openclose));
        }

        if (action == TimeScene.FIRSTACTION) {
            String firstAction = getFirstAction(scene);
            if (!TextUtils.isEmpty(firstAction)) {
                tv_firstaction.setText(firstAction);
            }
        } else if (action == TimeScene.SECONDACTION) {
            String secondAction = getSecondAction(scene);
            if (!TextUtils.isEmpty(secondAction)) {
                tv_secondaction.setText(secondAction);
            }
        }
    }


    private void updateSceneTimeView() {

        String templateid = scene.getTemplateId();
        String time = null;
        if (templateid.equals(SceneFactory.SCENE_APPOINT_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_close));
            String hour = (String) scene.getJsonValues().get("hour");
            String minute = (String) scene.getJsonValues().get("minute");
            if (TextUtils.isEmpty(hour) || TextUtils.isEmpty(minute)) {

            } else {
                time = getResources().getString(R.string.str_scene_timeformat, Integer.valueOf(hour), Integer.valueOf(minute));
                tv_firstactiontime.setText(time);
            }
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_open));
            String hour = (String) scene.getJsonValues().get("hour");
            String minute = (String) scene.getJsonValues().get("minute");
            if (TextUtils.isEmpty(hour) || TextUtils.isEmpty(minute)) {

            } else {
                time = getResources().getString(R.string.str_scene_timeformat, Integer.valueOf(hour), Integer.valueOf(minute));
                tv_firstactiontime.setText(time);
            }
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_openclose));
            String first_hour = (String) scene.getJsonValues().get("firstHour");
            String first_minute = (String) scene.getJsonValues().get("firstMinute");
            if (TextUtils.isEmpty(first_hour) || TextUtils.isEmpty(first_minute)) {

            } else {
                String firstTime = getResources().getString(R.string.str_scene_timeformat, Integer.valueOf(first_hour), Integer.valueOf(first_minute));
                tv_firstactiontime.setText(firstTime);
            }

            String second_hour = (String) scene.getJsonValues().get("secondHour");
            String second_minute = (String) scene.getJsonValues().get("secondMinute");
            if (TextUtils.isEmpty(second_hour) || TextUtils.isEmpty(second_minute)) {

            } else {
                String secondTime = getResources().getString(R.string.str_scene_timeformat, Integer.valueOf(second_hour), Integer.valueOf(second_minute));
                tv_secondactiontime.setText(secondTime);
            }
        }
    }

    private void updateSceneRepeatView() {
        String templateid = scene.getTemplateId();
        if (templateid.equals(SceneFactory.SCENE_APPOINT_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_close));
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_open));
        } else if (templateid.equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
            tv_modelname.setText(getResources().getString(R.string.str_scene_openclose));
        }

        String weekStr = scene.getJsonValues().getString("week");
        if (TextUtils.isEmpty(weekStr)) {
            tv_scenerepeat.setText(getString(R.string.str_scene_onlyonece));
        } else {
            String weekArray[] = weekStr.split(",");
            StringBuffer sb = new StringBuffer();
            if (weekArray != null && weekArray.length > 0) {
                String weekArrayRes[] = getResources().getStringArray(R.array.weeks);
                int i = 0;
                for (String week : weekArray) {
                    if (week.equals(TimeScene.SCENEWEEK_7)) {
                        sb.append(weekArrayRes[0]);
                    } else if (week.equals(TimeScene.SCENEWEEK_1)) {
                        sb.append(weekArrayRes[1]);
                    } else if (week.equals(TimeScene.SCENEWEEK_2)) {
                        sb.append(weekArrayRes[2]);
                    } else if (week.equals(TimeScene.SCENEWEEK_3)) {
                        sb.append(weekArrayRes[3]);
                    } else if (week.equals(TimeScene.SCENEWEEK_4)) {
                        sb.append(weekArrayRes[4]);
                    } else if (week.equals(TimeScene.SCENEWEEK_5)) {
                        sb.append(weekArrayRes[5]);
                    } else if (week.equals(TimeScene.SCENEWEEK_6)) {
                        sb.append(weekArrayRes[6]);
                    }
                    if (i < (weekArray.length - 1)) {
                        sb.append(",");
                    }
                    i++;
                }
            }
            tv_scenerepeat.setText(sb.toString());
        }
    }


    private String getSecondAction(TimeScene scene) {
        String secondAction = null;
        try {
            if (paramsMap != null && scene.getJsonValues() != null) {
                String param = (String) scene.getJsonValues().getJSONObject("secondActionParams").getJSONArray("attrSet").get(0);
                String value = (String) scene.getJsonValues().getJSONObject("secondActionParams").getJSONObject(param).get("value");
                ArrayList<ParamData> paramDataList = paramsMap.getParams_ali();
                for (ParamData paramData : paramDataList) {
                    if (paramData.getKey().equals(param)) {
                        if (paramData.getType() == ParamData.TYPE_Enum) {
                            List<ParamData.Enum> enumList = paramData.getValue_range1();
                            for (ParamData.Enum enum_data : enumList) {
                                if (enum_data.value.equals(value)) {
                                    secondAction = enum_data.name;
                                    break;
                                }
                            }
                        } else if (paramData.getType() == ParamData.TYPE_Continuous) {
                            secondAction = value + "  " + paramData.getUnit();
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Logutils.log_e("param error:", e);
        }
        return secondAction;
    }


    private boolean checkSceneData() {
        boolean result = true;
        if (scene != null) {
            JSONObject jsonObject = scene.getJsonValues();
            if (scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_OFF) || scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON)) {
                if (jsonObject != null) {
                    JSONObject firstAction = jsonObject.getJSONObject("firstActionParams");
                    if (firstAction == null) {
                        result = false;
                        return result;
                    }
                    JSONArray attrSet = firstAction.getJSONArray("attrSet");
                    if (attrSet == null) {
                        result = false;
                        return result;
                    }
                    String time_hour = (String) jsonObject.get("hour");
                    String time_minute = (String) jsonObject.get("minute");
                    if (TextUtils.isEmpty(time_hour) || TextUtils.isEmpty(time_minute)) {
                        result = false;
                        return result;
                    }
                } else {
                    result = false;
                    return result;
                }
            } else if (scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON_OFF)) {
                if (jsonObject != null) {
                    JSONObject firstAction = jsonObject.getJSONObject("firstActionParams");
                    if (firstAction == null) {
                        result = false;
                        return result;
                    }

                    JSONArray attrSet = firstAction.getJSONArray("attrSet");
                    if (attrSet == null) {
                        result = false;
                        return result;
                    }

                    String time_hour = (String) jsonObject.get("firstHour");
                    String time_minute = (String) jsonObject.get("firstMinute");
                    if (TextUtils.isEmpty(time_hour) || TextUtils.isEmpty(time_minute)) {
                        result = false;
                        return result;
                    }


                    JSONObject secondAction = jsonObject.getJSONObject("secondActionParams");
                    if (secondAction == null) {
                        result = false;
                        return result;
                    }

                    attrSet = secondAction.getJSONArray("attrSet");
                    if (attrSet == null) {
                        result = false;
                        return result;
                    }

                    time_hour = (String) jsonObject.get("secondHour");
                    time_minute = (String) jsonObject.get("secondMinute");
                    if (TextUtils.isEmpty(time_hour) || TextUtils.isEmpty(time_minute)) {
                        result = false;
                        return result;
                    }
                } else {
                    result = false;
                    return result;
                }
            }
        } else {
            result = false;
            return result;
        }
        return result;
    }
}
