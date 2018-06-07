package com.lbest.rm.adapter;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.Constants;
import com.lbest.rm.R;
import com.lbest.rm.SceneListActivity;
import com.lbest.rm.data.BaseDeviceInfo;
import com.lbest.rm.data.SceneModel;
import com.lbest.rm.data.TimeScene;
import com.lbest.rm.productDevice.SceneFactory;
import com.lbest.rm.productDevice.SceneManager;
import com.lbest.rm.view.SwipeListItemLayout;

import java.util.ArrayList;

/**
 * Created by dell on 2017/10/25.
 */

public class SceneListAdapter extends BaseAdapter {
    private Activity mActivity;
    private LayoutInflater mInflater;
    private BaseDeviceInfo baseDeviceInfo;
    private ArrayList<TimeScene> sceneList;
    private boolean canSwipe=false;
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener{
        public void onClick(View parent, View view, int position);
    }

    public void setSceneList(ArrayList<TimeScene> sceneList) {
        this.sceneList = sceneList;
    }

    public void setCanSwipe(boolean canSwipe) {
        this.canSwipe = canSwipe;
    }

    public SceneListAdapter(Activity mActivity, BaseDeviceInfo baseDeviceInfo) {
        this.mActivity = mActivity;
        this.baseDeviceInfo = baseDeviceInfo;
        this.mInflater = LayoutInflater.from(mActivity);
    }

    @Override
    public int getCount() {
        return sceneList==null?0:sceneList.size();
    }

    @Override
    public Object getItem(int position) {
        return sceneList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final ViewHolder viewHolder;
        if(convertView==null){
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.scenelist_item_layout, null);
            viewHolder.slil_item = (SwipeListItemLayout) convertView.findViewById(R.id.slil_item);
            viewHolder.tv_time = (TextView) convertView.findViewById(R.id.tv_time);
            viewHolder.tv_scenedes = (TextView) convertView.findViewById(R.id.tv_scenedes);
            viewHolder.bt_scene_onoff = (Button) convertView.findViewById(R.id.bt_scene_onoff);
            viewHolder.ll_scene = (LinearLayout) convertView.findViewById(R.id.ll_scene);
            viewHolder.tv_action = (TextView) convertView.findViewById(R.id.tv_action);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final TimeScene scene=sceneList.get(position);

        viewHolder.slil_item.setCanSwipe(canSwipe);
        viewHolder.slil_item.setOnSwipeStatusListener(new SceneListActivity.SwipeListItemOnSlipStatusListener(
                viewHolder.slil_item));

        viewHolder.tv_action.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                SceneManager.deleteScene(baseDeviceInfo.getUuid(),scene.getId(), scene.getCreator(), new SceneManager.SceneOperateCallBack(mActivity) {
                    @Override
                    public void callBack(int code, String msg, Object data) {
                        super.callBack(code, msg, data);
                        if(code== Constants.AliErrorCode.SUCCESS_CODE){
                            viewHolder.slil_item.setStatus(SwipeListItemLayout.Status.Close, true);
                            sceneList.remove(position);
                            notifyDataSetChanged();
                            Toast.makeText(mActivity,mActivity.getResources().getString(R.string.str_deletescene_success),Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(mActivity,mActivity.getResources().getString(R.string.str_deletescene_fail)+":"+msg+" "+code,Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        viewHolder.bt_scene_onoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TimeScene sceneTmp=new TimeScene();
                sceneTmp.setId(scene.getId());
                if(scene.getState().equals("1")){
                    sceneTmp.setState("0");
                }else  if(scene.getState().equals("0")){
                    sceneTmp.setState("1");
                }
                sceneTmp.setName(scene.getName());
                sceneTmp.setSceneGroup(scene.getSceneGroup());
                sceneTmp.setDeviceUuid(scene.getJsonValues().getString("deviceUuid"));
                sceneTmp.setCreator(scene.getCreator());
                sceneTmp.setTemplateId(scene.getTemplateId());
                sceneTmp.setJsonValues(scene.getJsonValues());

                SceneManager.updateScene(baseDeviceInfo.getUuid(),sceneTmp, new SceneManager.SceneOperateCallBack(mActivity) {
                    @Override
                    public void callBack(int code, String msg, Object data) {
                        super.callBack(code, msg, data);
                        if(code== Constants.AliErrorCode.SUCCESS_CODE){
                            scene.setState(sceneTmp.getState());
                            notifyDataSetChanged();
                        }else{
                            Toast.makeText(mActivity,mActivity.getResources().getString(R.string.str_fail)+":"+msg+" "+code,Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        viewHolder.ll_scene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onItemClickListener!=null){
                    onItemClickListener.onClick(parent,v,position);
                }
            }
        });


        if(scene.getState().equals(SceneModel.SCENE_ACTIVE)){
            viewHolder.bt_scene_onoff.setBackground(mActivity.getResources().getDrawable(R.drawable.scene_on));
        }else  if(scene.getState().equals(SceneModel.SCENE_UNACTIVE)){
            viewHolder.bt_scene_onoff.setBackground(mActivity.getResources().getDrawable(R.drawable.scene_off));
        }
        String time=null;
        String modelName=null;
        if(scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_OFF)){
            String hour= (String) scene.getJsonValues().get("hour");
            String minute= (String) scene.getJsonValues().get("minute");
            if(TextUtils.isEmpty(hour)||TextUtils.isEmpty(minute)){

            }else{
                time=mActivity.getResources().getString(R.string.str_scene_timeformat,Integer.valueOf(hour),Integer.valueOf(minute));
            }
            modelName=mActivity.getString(R.string.str_scene_close);
        }else if(scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON)){
            String hour= (String) scene.getJsonValues().get("hour");
            String minute= (String) scene.getJsonValues().get("minute");
            if(TextUtils.isEmpty(hour)||TextUtils.isEmpty(minute)){

            }else{
                time=mActivity.getResources().getString(R.string.str_scene_timeformat,Integer.valueOf(hour),Integer.valueOf(minute));
            }
            modelName=mActivity.getString(R.string.str_scene_open);
        }else if(scene.getTemplateId().equals(SceneFactory.SCENE_APPOINT_ON_OFF)){
            String firstTime=null;
            String first_hour= (String) scene.getJsonValues().get("firstHour");
            String first_minute= (String) scene.getJsonValues().get("firstMinute");
            if(TextUtils.isEmpty(first_hour)||TextUtils.isEmpty(first_minute)){

            }else{
               firstTime = mActivity.getResources().getString(R.string.str_scene_timeformat,Integer.valueOf(first_hour),Integer.valueOf(first_minute));
            }

            String secondTime=null;

            String second_hour= (String) scene.getJsonValues().get("secondHour");
            String second_minute= (String) scene.getJsonValues().get("secondMinute");
            if(TextUtils.isEmpty(second_hour)||TextUtils.isEmpty(second_minute)){

            }else{
                secondTime =  mActivity.getResources().getString(R.string.str_scene_timeformat,Integer.valueOf(second_hour),Integer.valueOf(second_minute));
            }
            if(TextUtils.isEmpty(firstTime)||TextUtils.isEmpty(secondTime)){

            }else{
                time=firstTime+","+secondTime;
            }
            modelName=mActivity.getString(R.string.str_scene_openclose);
        }
        String weekStr=(String) scene.getJsonValues().get("week");
        if(TextUtils.isEmpty(weekStr)){
            viewHolder.tv_scenedes.setText(modelName+"  "+mActivity.getString(R.string.str_scene_onlyonece));
        }else{
            String weekArray[]=weekStr.split(",");
            StringBuffer sb=new StringBuffer();
            if(weekArray!=null&&weekArray.length>0){
                String weekArrayRes[] =mActivity.getResources().getStringArray(R.array.weeks);
                int i=0;
                for(String week:weekArray){
                    if(week.equals(TimeScene.SCENEWEEK_7)){
                        sb.append(weekArrayRes[0]);
                    }else if(week.equals(TimeScene.SCENEWEEK_1)){
                        sb.append(weekArrayRes[1]);
                    }else if(week.equals(TimeScene.SCENEWEEK_2)){
                        sb.append(weekArrayRes[2]);
                    }else if(week.equals(TimeScene.SCENEWEEK_3)){
                        sb.append(weekArrayRes[3]);
                    }else if(week.equals(TimeScene.SCENEWEEK_4)){
                        sb.append(weekArrayRes[4]);
                    }else if(week.equals(TimeScene.SCENEWEEK_5)){
                        sb.append(weekArrayRes[5]);
                    }else if(week.equals(TimeScene.SCENEWEEK_6)){
                        sb.append(weekArrayRes[6]);
                    }
                    if(i<(weekArray.length-1)){
                        sb.append(",");
                    }
                    i++;
                }
            }
            viewHolder.tv_scenedes.setText(modelName+"  "+sb.toString());
        }

        viewHolder.tv_time.setText(time);
        viewHolder.tv_action.setText(mActivity.getResources().getString(R.string.str_deletescene));
        return convertView;
    }

    static class ViewHolder{
        public TextView tv_time;
        public TextView tv_scenedes;
        public TextView tv_action;
        public Button bt_scene_onoff;
        public LinearLayout ll_scene;
        public SwipeListItemLayout slil_item;
    }
}