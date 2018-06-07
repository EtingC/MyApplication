package com.lbest.rm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lbest.rm.data.TimeScene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SceneRepeatSelectActivity extends AppCompatActivity implements View.OnClickListener{
    private Toolbar toolbar;
    private TextView toolbar_title;
    private RelativeLayout rl_week1;
    private RelativeLayout rl_week2;
    private RelativeLayout rl_week3;
    private RelativeLayout rl_week4;
    private RelativeLayout rl_week5;
    private RelativeLayout rl_week6;
    private RelativeLayout rl_week7;

    private ImageView iv_select1;
    private ImageView iv_select2;
    private ImageView iv_select3;
    private ImageView iv_select4;
    private ImageView iv_select5;
    private ImageView iv_select6;
    private ImageView iv_select7;

    private String weekStr;
    private List<String> weekList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_repeat_select);
        initData();
        findview();
        initView();
        setListener();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent=new Intent();
        StringBuffer sb=new StringBuffer();
        int i = 0;

        Collections.sort(weekList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int number1=Integer.parseInt(o1);
                int number2=Integer.valueOf(o2);
                return number1-number2;
            }
        });

        for(String week:weekList){
            sb.append(week);
            if (i < (weekList.size() - 1)) {
                sb.append(",");
            }
            i++;
        }
        intent.putExtra(Constants.INTENT_SCENEREPEAT,sb.toString());
        setResult(RESULT_OK,intent);
        SceneRepeatSelectActivity.this.finish();
    }

    @Override
    public void onClick(View v) {
        int id=v.getId();
        switch (id){
            case R.id.rl_week1:
                if(weekList.contains(TimeScene.SCENEWEEK_1)){
                    weekList.remove(TimeScene.SCENEWEEK_1);
                    iv_select1.setImageResource(R.drawable.icon_noselected);
                }else{
                    weekList.add(TimeScene.SCENEWEEK_1);
                    iv_select1.setImageResource(R.drawable.icon_selected);
                }
                break;
            case R.id.rl_week2:
                if(weekList.contains(TimeScene.SCENEWEEK_2)){
                    weekList.remove(TimeScene.SCENEWEEK_2);
                    iv_select2.setImageResource(R.drawable.icon_noselected);
                }else{
                    weekList.add(TimeScene.SCENEWEEK_2);
                    iv_select2.setImageResource(R.drawable.icon_selected);
                }
                break;
            case R.id.rl_week3:
                if(weekList.contains(TimeScene.SCENEWEEK_3)){
                    weekList.remove(TimeScene.SCENEWEEK_3);
                    iv_select3.setImageResource(R.drawable.icon_noselected);
                }else{
                    weekList.add(TimeScene.SCENEWEEK_3);
                    iv_select3.setImageResource(R.drawable.icon_selected);
                }
                break;
            case R.id.rl_week4:
                if(weekList.contains(TimeScene.SCENEWEEK_4)){
                    weekList.remove(TimeScene.SCENEWEEK_4);
                    iv_select4.setImageResource(R.drawable.icon_noselected);
                }else{
                    weekList.add(TimeScene.SCENEWEEK_4);
                    iv_select4.setImageResource(R.drawable.icon_selected);
                }
                break;
            case R.id.rl_week5:
                if(weekList.contains(TimeScene.SCENEWEEK_5)){
                    weekList.remove(TimeScene.SCENEWEEK_5);
                    iv_select5.setImageResource(R.drawable.icon_noselected);
                }else{
                    weekList.add(TimeScene.SCENEWEEK_5);
                    iv_select5.setImageResource(R.drawable.icon_selected);
                }
                break;
            case R.id.rl_week6:
                if(weekList.contains(TimeScene.SCENEWEEK_6)){
                    weekList.remove(TimeScene.SCENEWEEK_6);
                    iv_select6.setImageResource(R.drawable.icon_noselected);
                }else{
                    weekList.add(TimeScene.SCENEWEEK_6);
                    iv_select6.setImageResource(R.drawable.icon_selected);
                }
                break;
            case R.id.rl_week7:
                if(weekList.contains(TimeScene.SCENEWEEK_7)){
                    weekList.remove(TimeScene.SCENEWEEK_7);
                    iv_select7.setImageResource(R.drawable.icon_noselected);
                }else{
                    weekList.add(TimeScene.SCENEWEEK_7);
                    iv_select7.setImageResource(R.drawable.icon_selected);
                }
                break;
            default:
        }
    }

    private void  initData(){
        weekStr=getIntent().getStringExtra(Constants.INTENT_SCENEREPEAT);
        weekList=new ArrayList<>();

        if(weekStr!=null){
            String[] weekArray=weekStr.split(",");
            if(weekArray!=null&&weekArray.length>0){
                for (String week : weekArray) {
                    if(TextUtils.isEmpty(week)){

                    }else{
                        weekList.add(week);
                    }
                }
            }
        }
    }

    private void findview() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        rl_week1 = (RelativeLayout) findViewById(R.id.rl_week1);
        rl_week2 = (RelativeLayout) findViewById(R.id.rl_week2);
        rl_week3 = (RelativeLayout) findViewById(R.id.rl_week3);
        rl_week4 = (RelativeLayout) findViewById(R.id.rl_week4);
        rl_week5 = (RelativeLayout) findViewById(R.id.rl_week5);
        rl_week6 = (RelativeLayout) findViewById(R.id.rl_week6);
        rl_week7 = (RelativeLayout) findViewById(R.id.rl_week7);
        iv_select1 = (ImageView) findViewById(R.id.iv_select1);
        iv_select2 = (ImageView) findViewById(R.id.iv_select2);
        iv_select3 = (ImageView) findViewById(R.id.iv_select3);
        iv_select4 = (ImageView) findViewById(R.id.iv_select4);
        iv_select5 = (ImageView) findViewById(R.id.iv_select5);
        iv_select6 = (ImageView) findViewById(R.id.iv_select6);
        iv_select7 = (ImageView) findViewById(R.id.iv_select7);
    }

    private void setListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Collections.sort(weekList, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        int number1=Integer.parseInt(o1);
                        int number2=Integer.parseInt(o2);
                        return number1-number2;
                    }
                });

                Intent intent=new Intent();
                StringBuffer sb=new StringBuffer();
                int i = 0;
                for(String week:weekList){
                    sb.append(week);
                    if (i < (weekList.size() - 1)) {
                        sb.append(",");
                    }
                    i++;
                }
                intent.putExtra(Constants.INTENT_SCENEREPEAT,sb.toString());
                setResult(RESULT_OK,intent);
                SceneRepeatSelectActivity.this.finish();
            }
        });

        rl_week1.setOnClickListener(this);
        rl_week2.setOnClickListener(this);
        rl_week3.setOnClickListener(this);
        rl_week4.setOnClickListener(this);
        rl_week5.setOnClickListener(this);
        rl_week6.setOnClickListener(this);
        rl_week7.setOnClickListener(this);
    }

    private void initView() {
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_scenerepeat));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        if(weekStr!=null){
                for (String week : weekList) {
                    if (week.equals(TimeScene.SCENEWEEK_7)) {
                        iv_select7.setImageResource(R.drawable.icon_selected);
                    } else if (week.equals(TimeScene.SCENEWEEK_1)) {
                        iv_select1.setImageResource(R.drawable.icon_selected);
                    } else if (week.equals(TimeScene.SCENEWEEK_2)) {
                        iv_select2.setImageResource(R.drawable.icon_selected);
                    } else if (week.equals(TimeScene.SCENEWEEK_3)) {
                        iv_select3.setImageResource(R.drawable.icon_selected);
                    } else if (week.equals(TimeScene.SCENEWEEK_4)) {
                        iv_select4.setImageResource(R.drawable.icon_selected);
                    } else if (week.equals(TimeScene.SCENEWEEK_5)) {
                        iv_select5.setImageResource(R.drawable.icon_selected);
                    } else if (week.equals(TimeScene.SCENEWEEK_6)) {
                        iv_select6.setImageResource(R.drawable.icon_selected);
                    }
                }
        }
    }
}
