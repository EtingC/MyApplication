package com.lbest.rm;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.account.broadlink.BLHttpErrCode;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.utils.CommonUtils;
import com.lbest.rm.view.InputTextView;
import com.lbest.rm.view.fragment.GetBackPasswordResultFragment;
import com.lbest.rm.view.fragment.InputPasswordFragment;
import com.lbest.rm.view.fragment.InputPhoneFragment;

import java.util.Timer;
import java.util.TimerTask;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.result.account.BLBaseResult;

public class GetBackPasswordActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;
    private View divider;
    private FragmentManager fragmentManager;


    private final String PhoneFragment_Tag="PhoneFragment_Tag";
    private final String PasswordFragment_Tag="PasswordFragment_Tag";
    private final String PasswordResultFragment_Tag="PasswordResultFragment_Tag";
    private InputPhoneFragment inputPhoneFragment;
    private InputPasswordFragment inputPasswordFragment;
    private GetBackPasswordResultFragment getBackPasswordResultFragment;
    private String phone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_back_password);
        fragmentManager=getSupportFragmentManager();
        findView();
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void findView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        divider=findViewById(R.id.divider);
    }

    private void setListener() {

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetBackPasswordActivity.this.finish();
            }
        });

        inputPhoneFragment.setClickListener(new InputPhoneFragment.onNextClickListener() {
            @Override
            public void onClick(String data) {
                phone=data;
                if(!TextUtils.isEmpty(phone)){
                    new GetVerifyCodeTask().execute(phone);
                }else{
                    Toast.makeText(GetBackPasswordActivity.this,getResources().getString(R.string.str_register_errorphoneoremail),Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    private void initView() {
        toolbar.setNavigationIcon(R.drawable.icon_back);
        toolbar.setBackgroundColor(Color.parseColor("#FFFFFF"));
        toolbar_title.setVisibility(View.VISIBLE);
        toolbar_title.setText(getResources().getString(R.string.str_getbackpassword));
        toolbar_title.setTextColor(getResources().getColor(R.color.tabgray_selected));
        showPhoneFragment();
        divider.setVisibility(View.INVISIBLE);
    }


    private void showPhoneFragment(){
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        inputPhoneFragment =(InputPhoneFragment)fragmentManager.findFragmentByTag(PhoneFragment_Tag);
        if(inputPhoneFragment==null){
            inputPhoneFragment=new InputPhoneFragment();
            transaction.add(R.id.fl_fragmentcontainer,inputPhoneFragment,PhoneFragment_Tag);
        }else{
            transaction.show(inputPhoneFragment);
        }
        transaction.commit();
    }

    private void removePhoneFragment(){
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        inputPhoneFragment =(InputPhoneFragment)fragmentManager.findFragmentByTag(PhoneFragment_Tag);
        if(inputPhoneFragment!=null){
            transaction.remove(inputPhoneFragment);
            transaction.commit();
        }
    }

    private void showPasswordFragment(){
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        inputPasswordFragment =(InputPasswordFragment)fragmentManager.findFragmentByTag(PasswordFragment_Tag);
        if(inputPasswordFragment==null){
            inputPasswordFragment=new InputPasswordFragment();
            transaction.add(R.id.fl_fragmentcontainer,inputPasswordFragment,PasswordFragment_Tag);
        }
        inputPasswordFragment.setPhone(phone);

        inputPasswordFragment.setClickListener(new InputPasswordFragment.onNextClickListener() {
            @Override
            public void onClick(String code, String password) {
                if(!TextUtils.isEmpty(code)&&!TextUtils.isEmpty(password)){
                    new retrievePasswordTask().execute(phone,code,password);
                }else{
                    Toast.makeText(GetBackPasswordActivity.this, getResources().getString(R.string.str_getbackpassword_errordata), Toast.LENGTH_LONG).show();
                }
            }
        });

        transaction.show(inputPasswordFragment);
        transaction.commit();
    }

    private void removePasswordFragment(){
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        inputPasswordFragment =(InputPasswordFragment)fragmentManager.findFragmentByTag(PasswordFragment_Tag);
        if(inputPasswordFragment!=null){
            transaction.remove(inputPasswordFragment);
            transaction.commit();
        }
    }


    private void showPasswordResultFragment(){
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        getBackPasswordResultFragment =(GetBackPasswordResultFragment)fragmentManager.findFragmentByTag(PhoneFragment_Tag);
        if(getBackPasswordResultFragment==null){
            getBackPasswordResultFragment=new GetBackPasswordResultFragment();
            transaction.add(R.id.fl_fragmentcontainer,getBackPasswordResultFragment,PasswordResultFragment_Tag);
        }else{
            transaction.show(getBackPasswordResultFragment);
        }
        transaction.commit();
    }

    private void removePasswordResultFragment(){
        FragmentTransaction transaction=fragmentManager.beginTransaction();
        getBackPasswordResultFragment =(GetBackPasswordResultFragment)fragmentManager.findFragmentByTag(PhoneFragment_Tag);
        if(getBackPasswordResultFragment!=null){
            transaction.remove(getBackPasswordResultFragment);
            transaction.commit();
        }
    }

    //获取手机验证码
    private class GetVerifyCodeTask extends AsyncTask<String, Void, BLBaseResult> {
        private String phoneoremail;
        private BLProgressDialog blProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog = BLProgressDialog.createDialog(GetBackPasswordActivity.this, null);
            blProgressDialog.show();
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            //短信验证码
            phoneoremail = params[0];
            return BLLet.Account.sendRetrieveVCode(phoneoremail);
        }

        @Override
        protected void onPostExecute(BLBaseResult result) {
            super.onPostExecute(result);
            blProgressDialog.dismiss();
            if (result != null && result.getError() == BLHttpErrCode.SUCCESS) {
                removePhoneFragment();
                showPasswordFragment();
            } else if (result != null) {
                Toast.makeText(GetBackPasswordActivity.this, BLHttpErrCode.getErrorMsg(GetBackPasswordActivity.this, result.getError()), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(GetBackPasswordActivity.this, getResources().getString(R.string.str_err_network), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class retrievePasswordTask extends AsyncTask<String, Void, BLBaseResult> {
        private String phoneoremail;
        private String code;
        private String newpassword;
        private BLProgressDialog blProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog = BLProgressDialog.createDialog(GetBackPasswordActivity.this, null);
            blProgressDialog.show();
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            //短信验证码
            phoneoremail = params[0];
            code = params[1];
            newpassword = params[2];
            return BLLet.Account.retrievePassword(phoneoremail,code,newpassword);
        }

        @Override
        protected void onPostExecute(BLBaseResult result) {
            super.onPostExecute(result);
            blProgressDialog.dismiss();
            if (result != null && result.getError() == BLHttpErrCode.SUCCESS) {
                //Toast.makeText(GetBackPasswordActivity.this, getResources().getString(R.string.str_getbackpassword_success), Toast.LENGTH_LONG).show();
                //GetBackPasswordActivity.this.finish();
                removePasswordFragment();
                showPasswordResultFragment();
            } else if (result != null) {
                Toast.makeText(GetBackPasswordActivity.this, getResources().getString(R.string.str_getbackpassword_fail)+":"+result.getMsg()+"  "+result.getError(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(GetBackPasswordActivity.this, getResources().getString(R.string.str_err_network), Toast.LENGTH_LONG).show();
            }
        }
    }
}
