package com.lbest.rm;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLConstants;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.data.DeviceDetailsInfo;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.data.db.FamilyDeviceModuleData;
import com.lbest.rm.productDevice.AliDeviceController;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.InputTextView;

import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.family.BLModuleControlResult;

public class ModifyDeviceNameActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;

    private InputTextView inputTextView;
    private FamilyDeviceModuleData deviceDetailsInfo;
    private BLAcountToAli blAcountToAli;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_device_name);
        initData();
        findview();
        initView();
        setListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.modifydevicename_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    private void initData(){
        blAcountToAli=BLAcountToAli.getInstance();
        deviceDetailsInfo = (FamilyDeviceModuleData) getIntent().getSerializableExtra(Constants.INTENT_DEVICE);
    }

    private void findview(){
        inputTextView=(InputTextView) findViewById(R.id.ip_devicename);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

    }

    private void initView(){
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_editedevicename));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        if(deviceDetailsInfo!=null){
            inputTextView.setText(deviceDetailsInfo.getModuleName());
        }
    }

    private void setListener(){
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModifyDeviceNameActivity.this.finish();
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_finish:
                        if(TextUtils.isEmpty(inputTextView.getTextString())){
                            Toast.makeText(ModifyDeviceNameActivity.this,getResources().getString(R.string.str_nullmodifydevicename),Toast.LENGTH_LONG).show();
                        }else{
                            if(deviceDetailsInfo!=null){
                                    new ModifyNameTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,inputTextView.getTextString());
                            }
                        }
                        break;
                    default:
                }
                return false;
            }
        });
    }

    class ModifyNameTask extends AsyncTask<String,Void,BLModuleControlResult>{
        private BLProgressDialog blProgressDialog;
        private String newName;
        private RefreshTokenResult refreshTokenResult=null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog=BLProgressDialog.createDialog(ModifyDeviceNameActivity.this);
            blProgressDialog.show();
        }

        @Override
        protected BLModuleControlResult doInBackground(String... params) {
            newName=params[0];
            BLModuleControlResult result=AliDeviceController.moditfyDeviceNameV2(deviceDetailsInfo,newName);
            if(result==null||!result.succeed()){
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("ModifyDeviceNameTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                if(refreshTokenResult.isSuccess()){
                    return  AliDeviceController.moditfyDeviceNameV1(deviceDetailsInfo,newName);
                }
                return null;
            }else{
                Logutils.log_d("ModifyDeviceNameTask success");
                return result;
            }
        }

        @Override
        protected void onPostExecute(BLModuleControlResult blModuleControlResult) {
            super.onPostExecute(blModuleControlResult);

            if(ModifyDeviceNameActivity.this.isFinishing())
                return;

            blProgressDialog.dismiss();

            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(ModifyDeviceNameActivity.this,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ModifyDeviceNameActivity.this,ModifyDeviceNameActivity.this.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(ModifyDeviceNameActivity.this,ModifyDeviceNameActivity.this.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(ModifyDeviceNameActivity.this,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(ModifyDeviceNameActivity.this, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    ModifyDeviceNameActivity.this.finish();
                }
                return;
            }

            if(blModuleControlResult!=null&&blModuleControlResult.getStatus()==Constants.BLErrorCode.SUCCESS_CODE){
                deviceDetailsInfo.setModuleName(inputTextView.getTextString());
                Intent intent=new Intent();
                intent.putExtra(BLConstants.INTENT_DEVICE,deviceDetailsInfo);
                setResult(RESULT_OK,intent);
                ModifyDeviceNameActivity.this.finish();
            }else{
                if(blModuleControlResult!=null){
                    Toast.makeText(ModifyDeviceNameActivity.this,getResources().getString(R.string.str_modifydevicename_fail)+":"+blModuleControlResult.getStatus()+"    "+blModuleControlResult.getMsg(),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(ModifyDeviceNameActivity.this,getResources().getString(R.string.str_modifydevicename_fail),Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
