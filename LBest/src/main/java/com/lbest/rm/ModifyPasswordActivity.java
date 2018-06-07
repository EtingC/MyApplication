package com.lbest.rm;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.InputTextView;

import cn.com.broadlink.sdk.result.account.BLBaseResult;

public class ModifyPasswordActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;

    private EditText et_old;
    private EditText et_new;
    private EditText et_new2;

    private ImageView iv_pwd_old;
    private ImageView iv_pwd_new;
    private ImageView iv_pwd_new2;

    private TextView bt_sure;

    private BLAcountToAli blAcountToAli;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_password);
        blAcountToAli=BLAcountToAli.getInstance();
        findview();
        initView();
        setListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.modifypassword_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    private void findview(){
        et_old=(EditText) findViewById(R.id.ip_oldpassword);
        et_new=(EditText) findViewById(R.id.ip_newpassword);
        et_new2=(EditText) findViewById(R.id.ip_newpassword2);

        iv_pwd_old=(ImageView) findViewById(R.id.iv_pwd_old);
        iv_pwd_new=(ImageView) findViewById(R.id.iv_pwd_new);
        iv_pwd_new2=(ImageView) findViewById(R.id.iv_pwd_new2);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        bt_sure = (TextView) findViewById(R.id.bt_sure);
    }

    private void initView(){
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_modifypassword));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));


        et_old.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
        et_new.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
        et_new2.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
    }

    private void setListener(){

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModifyPasswordActivity.this.finish();
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_finish:
                        break;
                }
                return true;
            }
        });


        iv_pwd_old.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_old.getInputType() != InputType.TYPE_CLASS_TEXT) {
                    et_old.setInputType(InputType.TYPE_CLASS_TEXT);
                    iv_pwd_old.setImageResource(R.drawable.password_visiable);
                } else {
                    et_old.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
                    iv_pwd_old.setImageResource(R.drawable.password_invisiable);
                }
            }
        });


        iv_pwd_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_new.getInputType() != InputType.TYPE_CLASS_TEXT) {
                    et_new.setInputType(InputType.TYPE_CLASS_TEXT);
                    iv_pwd_new.setImageResource(R.drawable.password_visiable);
                } else {
                    et_new.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
                    iv_pwd_new.setImageResource(R.drawable.password_invisiable);
                }
            }
        });


        iv_pwd_new2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_new2.getInputType() != InputType.TYPE_CLASS_TEXT) {
                    et_new2.setInputType(InputType.TYPE_CLASS_TEXT);
                    iv_pwd_new2.setImageResource(R.drawable.password_visiable);
                } else {
                    et_new2.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
                    iv_pwd_new2.setImageResource(R.drawable.password_invisiable);
                }
            }
        });

        bt_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String old_passowrd=et_old.getText().toString();
                String new_passowrd=et_new.getText().toString();
                String new2_passowrd=et_new2.getText().toString();

                if(TextUtils.isEmpty(old_passowrd)){
                    Toast.makeText(ModifyPasswordActivity.this,getResources().getString(R.string.str_nulloldpassword),Toast.LENGTH_LONG).show();
                }else  if(TextUtils.isEmpty(new_passowrd)){
                    Toast.makeText(ModifyPasswordActivity.this,getResources().getString(R.string.str_nullnewpassword),Toast.LENGTH_LONG).show();
                }else  if(!new_passowrd.equals(new2_passowrd)){
                    Toast.makeText(ModifyPasswordActivity.this,getResources().getString(R.string.str_newpassworddifferent),Toast.LENGTH_LONG).show();
                }else{
                    new modifyPasswordTask().execute(old_passowrd,new_passowrd);
                }
            }
        });
    }


    class modifyPasswordTask extends AsyncTask<String,Void,BLBaseResult>{
        private BLProgressDialog blProgressDialog;

        private RefreshTokenResult refreshTokenResult=null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog=BLProgressDialog.createDialog(ModifyPasswordActivity.this);
            blProgressDialog.show();
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            String  oldpassword=params[0];
            String newpassword=params[1];
            BLBaseResult result= blAcountToAli.modifyPassword(oldpassword,newpassword);
            if(result==null||!result.succeed()){
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("modifyPasswordTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                if(refreshTokenResult.isSuccess()){
                    return  blAcountToAli.modifyPassword(oldpassword,newpassword);
                }
                return null;
            }else{
                Logutils.log_d("modifyPasswordTask success");
                return result;
            }
        }

        @Override
        protected void onPostExecute(BLBaseResult result) {
            super.onPostExecute(result);
            if(ModifyPasswordActivity.this==null||ModifyPasswordActivity.this.isFinishing())
                return;

            blProgressDialog.dismiss();

            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(ModifyPasswordActivity.this,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ModifyPasswordActivity.this,ModifyPasswordActivity.this.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(ModifyPasswordActivity.this,ModifyPasswordActivity.this.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(ModifyPasswordActivity.this,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(ModifyPasswordActivity.this, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    ModifyPasswordActivity.this.finish();
                }
                return;
            }

            if(result!=null){
                if(result.succeed()){
                    BLAcountToAli.getInstance().cleanUserInfo();
                    Intent intent=new Intent();
                    //intent.setClass(ModifyPasswordActivity.this,LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.setClass(ModifyPasswordActivity.this,AccountMainActivity.class);
                    startActivity(intent);
                    ModifyPasswordActivity.this.finish();
                }else{
                    Toast.makeText(ModifyPasswordActivity.this,getResources().getString(R.string.str_modifypasswordfail)+":"+result.getError()+" "+result.getMsg(),Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(ModifyPasswordActivity.this,getResources().getString(R.string.str_modifypasswordfail),Toast.LENGTH_LONG).show();
            }
        }
    }
}
