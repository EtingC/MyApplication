package com.lbest.rm.view.fragment;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.R;
import com.lbest.rm.account.broadlink.BLHttpErrCode;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.utils.CommonUtils;
import com.lbest.rm.view.InputTextView;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.param.account.BLRegistParam;
import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.account.BLLoginResult;

/**
 * Created by dell on 2017/10/30.
 */

public class SignUpByEmailFragmnet extends BaseFragment{
    private TextView mErrorHintView;

    private LinearLayout getCodeLayout;
    private LinearLayout inputPasswordLayout;
    private Button mNextBtn;
    private Button bt_sure;
    private InputTextView password;
    private InputTextView password2;
    private InputTextView mEmailView;
    private InputTextView mCodeView;
    private Button mGetCode;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_up_by_eamil_layout, container, false);

        findView(view);

        setListener();

        initView();

        return view;
    }

    private void findView(View view){
        mEmailView = (InputTextView) view.findViewById(R.id.account_email_view);

        mNextBtn = (Button) view.findViewById(R.id.btn_next);

        mErrorHintView = (TextView) view.findViewById(R.id.err_hint_view);
        mCodeView = (InputTextView) view.findViewById(R.id.it_verification_code);
        getCodeLayout= (LinearLayout) view.findViewById(R.id.layout1);
        inputPasswordLayout= (LinearLayout) view.findViewById(R.id.layout2);
        mGetCode = (Button) view.findViewById(R.id.btn_get_code);
        bt_sure = (Button) view.findViewById(R.id.btn_next2);
        password= (InputTextView) view.findViewById(R.id.it_password);
        password2= (InputTextView) view.findViewById(R.id.it_password2);
    }

    private void setListener(){
        mEmailView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(mErrorHintView.getText().toString())) {
                    mErrorHintView.setText(null);
                    mEmailView.setBackgroundResource(R.drawable.input_bg_round_tran_gray);
                }
                String code = mCodeView.getTextString();
                if (s.length() > 0 && !TextUtils.isEmpty(code)) {
                    mNextBtn.setEnabled(true);
                } else {
                    mNextBtn.setEnabled(false);
                }
            }
        });

        mCodeView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(mErrorHintView.getText().toString())) {
                    mErrorHintView.setText(null);
                    mEmailView.setBackgroundResource(R.drawable.input_bg_round_tran_gray);
                }
                String phone = mEmailView.getTextString();
                if (s.length() > 0 && !TextUtils.isEmpty(phone)) {
                    mNextBtn.setEnabled(true);
                } else {
                    mNextBtn.setEnabled(false);
                }
            }
        });


        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCodeLayout.setVisibility(View.GONE);
                inputPasswordLayout.setVisibility(View.VISIBLE);
            }
        });

        mGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailView.getTextString();
                if(!TextUtils.isEmpty(email)&&CommonUtils.isEmail(email)){
                    new GetVerifyCodeTask().execute(email);
                }else{
                    Toast.makeText(getActivity(),getResources().getString(R.string.str_register_errorphone),Toast.LENGTH_LONG).show();
                }
            }
        });


        bt_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(password.getTextString().equals(password2.getTextString())){
                    new signupTask().execute(mEmailView.getTextString(),mCodeView.getTextString(),password.getTextString());
                }else{
                    Toast.makeText(getActivity(), getResources().getString(R.string.str_newpassworddifferent), Toast.LENGTH_LONG).show();
                }
            }
        });


        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password_2 = password2.getTextString();
                if (s.length() > 0 && !TextUtils.isEmpty(password_2)) {
                    bt_sure.setEnabled(true);
                } else {
                    bt_sure.setEnabled(false);
                }
            }
        });
        password2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password_1 = password.getTextString();
                if (s.length() > 0 && !TextUtils.isEmpty(password_1)) {
                    bt_sure.setEnabled(true);
                } else {
                    bt_sure.setEnabled(false);
                }
            }
        });

    }

    private void initView(){
        mEmailView.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mEmailView.setTextHint(R.string.str_settings_safety_email);
        bt_sure.setEnabled(false);
        password.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTextHint(getResources().getString(R.string.str_singup_password));
        password2.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password2.setTextHint(getResources().getString(R.string.str_singup_password2));
        getCodeLayout.setVisibility(View.VISIBLE);
        inputPasswordLayout.setVisibility(View.GONE);
    }

    //获取验证码
    private class GetVerifyCodeTask extends AsyncTask<String, Void, BLBaseResult> {
        private String email;
        private BLProgressDialog blProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog = BLProgressDialog.createDialog(getActivity(), null);
            blProgressDialog.show();
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            //短信验证码
            email = params[0];
            return BLLet.Account.sendRegVCode(email);
        }

        @Override
        protected void onPostExecute(BLBaseResult result) {
            super.onPostExecute(result);
            blProgressDialog.dismiss();
            if (result != null && result.getError() == BLHttpErrCode.SUCCESS) {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_sendcode_success), Toast.LENGTH_LONG).show();
            } else if (result != null) {
                mEmailView.setBackgroundResource(R.drawable.input_bg_round_tran_red);
                mErrorHintView.setText(BLHttpErrCode.getErrorMsg(getActivity(), result.getError()));
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_err_network), Toast.LENGTH_LONG).show();
            }
        }
    }



    private class signupTask extends AsyncTask<String, Void, BLLoginResult> {
        private String email;
        private String code;
        private String password;
        private BLProgressDialog blProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog = BLProgressDialog.createDialog(getActivity(), null);
            blProgressDialog.show();
        }

        @Override
        protected BLLoginResult doInBackground(String... params) {
            //短信验证码
            email = params[0];
            code = params[1];
            password = params[2];
            BLRegistParam param=new BLRegistParam();
            param.setCode(code);
            param.setPhoneOrEmail(email);
            param.setPassword(password);
            param.setNickname(email);
            param.setSex("female");
            return BLLet.Account.regist(param,null);
        }

        @Override
        protected void onPostExecute(BLLoginResult result) {
            super.onPostExecute(result);
            blProgressDialog.dismiss();
            if (result != null && result.getError() == BLHttpErrCode.SUCCESS) {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_singup_success), Toast.LENGTH_LONG).show();
                getActivity().finish();
            } else if (result != null) {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_singup_fail)+":"+result.getMsg()+"  "+result.getError(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_err_network), Toast.LENGTH_LONG).show();
            }
        }
    }
}
