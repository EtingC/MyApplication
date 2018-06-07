package com.lbest.rm;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.BLAcountToAli;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.data.RefreshTokenResult;
import com.lbest.rm.utils.Logutils;
import com.lbest.rm.view.ChoosePicPopwindow;
import com.lbest.rm.view.InputTextView;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.account.BLBaseResult;

public class ModifyNicknameActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;

    private InputTextView inputTextView;

    private BLAcountToAli blAcountToAli;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_nickname);
        blAcountToAli=BLAcountToAli.getInstance();
        findview();
        initView();
        setListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.modifynickname_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void findview(){
        inputTextView=(InputTextView) findViewById(R.id.ip_nickname);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

    }

    private void initView(){
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_editenickname));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));

        inputTextView.setText(BLAcountToAli.getInstance().getBlUserInfo().getBl_nickname());
        inputTextView.getEditText().setFilters(new InputFilter[]{ new  InputFilter.LengthFilter(30)});

    }

    private void setListener(){
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModifyNicknameActivity.this.finish();
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_finish:
                        if(TextUtils.isEmpty(inputTextView.getTextString())){
                            Toast.makeText(ModifyNicknameActivity.this,getResources().getString(R.string.str_nullmodifynickname),Toast.LENGTH_LONG).show();
                        }else{
                            new modifyNicknameTask().execute(inputTextView.getTextString());
                        }
                        break;
                    default:
                }
                return false;
            }
        });
    }


    class modifyNicknameTask extends AsyncTask<String,Void,BLBaseResult>{
        private BLProgressDialog blProgressDialog;
        private String newname;

        private RefreshTokenResult refreshTokenResult=null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog=BLProgressDialog.createDialog(ModifyNicknameActivity.this);
            blProgressDialog.show();
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            newname=params[0];

            BLBaseResult result=blAcountToAli.modifyUserNickname(newname);
            if(result==null||!result.succeed()){
                String old_refresh_token = blAcountToAli.getBlUserInfo().getRefresh_token();
                String old_access_token = blAcountToAli.getBlUserInfo().getAccess_token();
                Logutils.log_d("modifyNicknameTask refreshAccessToken old_refresh_token:"+old_refresh_token+"   old_access_token:"+old_access_token);
                refreshTokenResult =  blAcountToAli.refreshToken(old_refresh_token);
                if(refreshTokenResult.isSuccess()){
                    return  blAcountToAli.modifyUserNickname(newname);
                }
                return null;
            }else{
                Logutils.log_d("modifyNicknameTask success");
                return result;
            }
        }

        @Override
        protected void onPostExecute(BLBaseResult result) {
            super.onPostExecute(result);

            if(ModifyNicknameActivity.this==null||ModifyNicknameActivity.this.isFinishing())
                return;

            blProgressDialog.dismiss();

            if(refreshTokenResult!=null&&!refreshTokenResult.isSuccess()){
                BLBaseResult baseResult=refreshTokenResult.getResult();
                int code=refreshTokenResult.getCode();
                if(code==-3){
                    if(baseResult!=null){
                        Toast.makeText(ModifyNicknameActivity.this,"error:"+baseResult.getError(),Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ModifyNicknameActivity.this,ModifyNicknameActivity.this.getResources().getString(R.string.err_network),Toast.LENGTH_SHORT).show();
                    }
                }else {
                    if(code==-1){
                        Toast.makeText(ModifyNicknameActivity.this,ModifyNicknameActivity.this.getResources().getString(R.string.str_loginfail),Toast.LENGTH_SHORT).show();
                    }else if(code==-2){
                        Toast.makeText(ModifyNicknameActivity.this,"token is null",Toast.LENGTH_SHORT).show();
                    }
//                    Intent intent = new Intent();
//                    intent.setClass(ModifyNicknameActivity.this, AccountMainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    ModifyNicknameActivity.this.finish();
                }
                return;
            }

            if(result!=null){
                if(result.succeed()){
                    BLAcountToAli.getInstance().saveUserNickname(newname);
                    ModifyNicknameActivity.this.finish();
                }else{
                    Toast.makeText(ModifyNicknameActivity.this,getResources().getString(R.string.str_modifynicknamefail)+":"+result.getError()+" "+result.getMsg(),Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(ModifyNicknameActivity.this,getResources().getString(R.string.str_modifynicknamefail),Toast.LENGTH_LONG).show();
            }
        }
    }
}
