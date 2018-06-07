package com.lbest.rm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lbest.rm.adapter.SceneListAdapter;
import com.lbest.rm.data.BLControlResult;
import com.lbest.rm.data.BaseDeviceInfo;
import com.lbest.rm.data.TimeScene;
import com.lbest.rm.data.queryDeviceSceneResponse;
import com.lbest.rm.productDevice.SceneFactory;
import com.lbest.rm.productDevice.SceneManager;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.SwipeListItemLayout;

import java.util.HashSet;
import java.util.Set;

public class SceneListActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TextView toolbar_title;

    private ListView lv_scenelist;
    private LinearLayout ll_empty;
    private LinearLayout ll_addscene;
    private Button bt_addscene;
    private TextView tv_addscene;

    private SceneListAdapter adapter;
    private BaseDeviceInfo baseDeviceInfo;
    private static Set<SwipeListItemLayout> sets = new HashSet();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_list);
        initData();
        findView();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrestSceneList();
    }

    private void initData() {
        baseDeviceInfo = getIntent().getParcelableExtra(Constants.INTENT_DEVICE);
    }

    private void findView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

        lv_scenelist = (ListView) findViewById(R.id.lv_scenelist);
        ll_empty = (LinearLayout) findViewById(R.id.ll_empty);
        ll_addscene = (LinearLayout) findViewById(R.id.ll_addscene);

        bt_addscene= (Button) findViewById(R.id.bt_addscene);
        tv_addscene= (TextView) findViewById(R.id.tv_addscene);
    }

    private void setListener() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SceneListActivity.this.finish();
            }
        });

        bt_addscene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(SceneListActivity.this,SceneEditActivity.class);
                intent.putExtra(Constants.INTENT_DEVICE,baseDeviceInfo);
                startActivity(intent);
            }
        });

        tv_addscene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(SceneListActivity.this,SceneEditActivity.class);
                intent.putExtra(Constants.INTENT_DEVICE,baseDeviceInfo);
                startActivity(intent);
            }
        });

        adapter.setOnItemClickListener(new SceneListAdapter.OnItemClickListener() {
            @Override
            public void onClick(View parent, View view, int position) {
                TimeScene scene= (TimeScene) adapter.getItem(position);
                Intent intent=new Intent();
                intent.setClass(SceneListActivity.this,SceneEditActivity.class);
                intent.putExtra(Constants.INTENT_DEVICE,baseDeviceInfo);
                intent.putExtra(Constants.INTENT_SCENETIME,scene);
                startActivity(intent);
            }
        });

        lv_scenelist.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    //当listview开始滑动时，若有item的状态为Open，则Close，然后移除
                    case SCROLL_STATE_TOUCH_SCROLL:
                        if (sets.size() > 0) {
                            for (SwipeListItemLayout s : sets) {
                                s.setStatus(SwipeListItemLayout.Status.Close, true);
                                sets.remove(s);
                            }
                        }
                        break;

                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

            }
        });
    }


    private void initView() {
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_time));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        adapter=new SceneListAdapter(SceneListActivity.this,baseDeviceInfo);
        adapter.setCanSwipe(true);
        lv_scenelist.setAdapter(adapter);
        if(baseDeviceInfo!=null){
            SceneManager.getDeviceAllScene(baseDeviceInfo.getUuid(), null, null, new SceneManager.SceneOperateCallBack(SceneListActivity.this) {
                @Override
                public void callBack(int code, String msg, Object data) {
                    super.callBack(code, msg, data);
                    if(code==Constants.AliErrorCode.SUCCESS_CODE){
                        queryDeviceSceneResponse response= JSON.parseObject((String) data,queryDeviceSceneResponse.class);
                        if(response!=null&&response.getSceneList()!=null&&response.getSceneList().size()>0){
                            ll_addscene.setVisibility(View.VISIBLE);
                            lv_scenelist.setVisibility(View.VISIBLE);
                            ll_empty.setVisibility(View.GONE);
                            adapter.setSceneList(response.getSceneList());
                            adapter.notifyDataSetChanged();

                        }else{
                            lv_scenelist.setVisibility(View.GONE);
                            ll_addscene.setVisibility(View.GONE);
                            ll_empty.setVisibility(View.VISIBLE);
                        }
                    }else{
                        lv_scenelist.setVisibility(View.GONE);
                        ll_addscene.setVisibility(View.GONE);
                        ll_empty.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }


    private void refrestSceneList(){
        SceneManager.getDeviceAllScene(baseDeviceInfo.getUuid(), null, null, new SceneManager.SceneOperateCallBack(SceneListActivity.this) {
            @Override
            public void callBack(int code, String msg, Object data) {
                super.callBack(code, msg, data);
                if(code==Constants.AliErrorCode.SUCCESS_CODE){
                    queryDeviceSceneResponse response= JSON.parseObject((String) data,queryDeviceSceneResponse.class);
                    if(response!=null&&response.getSceneList()!=null&&response.getSceneList().size()>0){
                        ll_addscene.setVisibility(View.VISIBLE);
                        lv_scenelist.setVisibility(View.VISIBLE);
                        ll_empty.setVisibility(View.GONE);
                        adapter.setSceneList(response.getSceneList());
                        adapter.notifyDataSetChanged();

                    }else{
                        lv_scenelist.setVisibility(View.GONE);
                        ll_addscene.setVisibility(View.GONE);
                        ll_empty.setVisibility(View.VISIBLE);
                    }
                }else{
                    lv_scenelist.setVisibility(View.GONE);
                    ll_addscene.setVisibility(View.GONE);
                    ll_empty.setVisibility(View.VISIBLE);
                }
            }
        });
    }


    public static class SwipeListItemOnSlipStatusListener implements SwipeListItemLayout.OnSwipeStatusListener {

        private SwipeListItemLayout slipListLayout;

        public SwipeListItemOnSlipStatusListener(SwipeListItemLayout slipListLayout) {
            this.slipListLayout = slipListLayout;
        }

        @Override
        public void onStatusChanged(SwipeListItemLayout.Status status) {
            if (status == SwipeListItemLayout.Status.Open) {
                //若有其他的item的状态为Open，则Close，然后移除
                if (sets.size() > 0) {
                    for (SwipeListItemLayout s : sets) {
                        s.setStatus(SwipeListItemLayout.Status.Close, true);
                        sets.remove(s);
                    }
                }
                sets.add(slipListLayout);
            } else {
                if (sets.contains(slipListLayout))
                    sets.remove(slipListLayout);
            }
        }

        @Override
        public void onStartCloseAnimation() {

        }

        @Override
        public void onStartOpenAnimation() {

        }
    }

}
