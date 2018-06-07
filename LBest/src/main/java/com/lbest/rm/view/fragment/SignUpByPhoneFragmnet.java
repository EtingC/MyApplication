package com.lbest.rm.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.alink.linksdk.tools.ThreadTools;
import com.lbest.rm.R;
import com.lbest.rm.account.broadlink.BLHttpErrCode;
import com.lbest.rm.common.BLProgressDialog;
import com.lbest.rm.utils.CommonUtils;
import com.lbest.rm.view.InputTextView;

import java.util.Timer;
import java.util.TimerTask;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.param.account.BLRegistParam;
import cn.com.broadlink.sdk.result.account.BLBaseResult;
import cn.com.broadlink.sdk.result.account.BLLoginResult;

import static android.app.Activity.RESULT_OK;

/**
 * Created by dell on 2017/10/30.
 */

public class SignUpByPhoneFragmnet extends BaseFragment {
    private LinearLayout mPhoneNumLayout;

    private InputTextView mPhoneView;
    private EditText mCodeView;
    private TextView mCountryCodeBtn;
    private TextView mNextBtn,mGetCode;

    private String mCountryCode = "86";

    private SharedPreferences mSharedPreferences;

    private Timer timer;
    private refreshGetCodeButtonTimeTask task;

    private LinearLayout getCodeLayout;
    private LinearLayout inputPasswordLayout;
    private LinearLayout resultLayout;
    private Button bt_sure;
    private InputTextView password;
    private InputTextView password2;
    private TextView tv_hassendcode;

    private ImageView iv_password1;
    private ImageView iv_password2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_up_by_phone_layout, container, false);

        mSharedPreferences = getActivity().getSharedPreferences("getverificationcode", Context.MODE_PRIVATE);
        findView(view);

        setListener();

        initView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        long startTime = mSharedPreferences.getLong("getcodetime", -1);
        if ((System.currentTimeMillis() - startTime) < 60000) {
            startTask(startTime);
            mGetCode.setEnabled(false);
        } else {
            mGetCode.setEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTask();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            long startTime = mSharedPreferences.getLong("getcodetime", -1);
            if ((System.currentTimeMillis() - startTime) < 60000) {
                startTask(startTime);
                mGetCode.setEnabled(false);
            } else {
                mGetCode.setEnabled(true);
            }
        } else {
            stopTask();
        }
    }

    private void findView(View view) {
        mPhoneNumLayout = (LinearLayout) view.findViewById(R.id.phone_num_layout);
        mCodeView = (EditText) view.findViewById(R.id.it_verification_code);
        mPhoneView = (InputTextView) view.findViewById(R.id.phone_view);
        mGetCode = (TextView) view.findViewById(R.id.btn_get_code);

        resultLayout = (LinearLayout) view.findViewById(R.id.layout3);
        mCountryCodeBtn = (TextView) view.findViewById(R.id.btn_country_code);
        mNextBtn = (TextView) view.findViewById(R.id.btn_next);

        tv_hassendcode = (TextView) view.findViewById(R.id.tv_hassendcode);
        getCodeLayout = (LinearLayout) view.findViewById(R.id.layout1);
        inputPasswordLayout = (LinearLayout) view.findViewById(R.id.layout2);

        bt_sure = (Button) view.findViewById(R.id.btn_next2);
        password = (InputTextView) view.findViewById(R.id.it_password);
        password2 = (InputTextView) view.findViewById(R.id.it_password2);

        iv_password1 = (ImageView) view.findViewById(R.id.iv_password1);
        iv_password2 = (ImageView) view.findViewById(R.id.iv_password2);
    }

    private void setListener() {
        mPhoneView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    mNextBtn.setEnabled(true);
                } else {
                    //mNextBtn.setEnabled(false);
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
                if (s.length() > 0) {
                    mNextBtn.setEnabled(true);
                } else {
                    //mNextBtn.setEnabled(false);
                }
            }
        });

        mCountryCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setClass(getActivity(), SelectCountryActivity.class);
//                startActivityForResult(intent, 1);
            }
        });

        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = mPhoneView.getTextString();
                if (!TextUtils.isEmpty(phone) && CommonUtils.isPhone(phone)) {
                    new GetVerifyCodeTask().execute(phone);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.str_register_errorphone), Toast.LENGTH_LONG).show();
                }
            }
        });

        mGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = mPhoneView.getTextString();
                if (!TextUtils.isEmpty(phone) && CommonUtils.isPhone(phone)) {
                    new GetVerifyCodeTask().execute(phone);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.str_register_errorphone), Toast.LENGTH_LONG).show();
                }
            }
        });


        bt_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (password.getTextString().equals(password2.getTextString())) {
                    new signupTask().execute(mPhoneView.getTextString(), mCodeView.getText().toString(), password.getTextString());
                } else {
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



        iv_password1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputtype=password.getInputType();

                if (inputtype== InputType.TYPE_CLASS_TEXT) {
                    iv_password1.setImageResource(R.drawable.password_invisiable);
                    password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);

                } else {
                    iv_password1.setImageResource(R.drawable.password_visiable);
                    password.setInputType(InputType.TYPE_CLASS_TEXT);
                }
            }
        });


        iv_password2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputtype=password2.getInputType();

                if (inputtype==InputType.TYPE_CLASS_TEXT) {
                    iv_password2.setImageResource(R.drawable.password_invisiable);
                    password2.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);

                } else {
                    iv_password2.setImageResource(R.drawable.password_visiable);
                    password2.setInputType(InputType.TYPE_CLASS_TEXT);
                }
            }
        });

    }

    private void initView() {
        mPhoneView.setTextHint(R.string.str_settings_safety_phone_number);
        mPhoneView.setTextColor(Color.parseColor("#333333"));
        mPhoneView.setTextHintColor(Color.parseColor("#bbbbbb"));
        mPhoneView.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        mCodeView.setHint(R.string.str_code);
        try {
            String country = CommonUtils.getCountry();
            int value = getResources().getInteger(getResources().getIdentifier(country.toUpperCase(),
                    "integer", getActivity().getPackageName()));
            mCountryCode = String.valueOf(value);
        } catch (Exception e) {
            mCountryCode = "86";
        }

        mCountryCodeBtn.setText("CN+" + mCountryCode);


        bt_sure.setEnabled(false);
        password.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
        password.setTextColor(Color.parseColor("#333333"));
        password.setTextHintColor(Color.parseColor("#bbbbbb"));
        password2.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);
        password2.setTextColor(Color.parseColor("#333333"));
        password2.setTextHintColor(Color.parseColor("#bbbbbb"));
        getCodeLayout.setVisibility(View.VISIBLE);
        inputPasswordLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.GONE);
    }


    public String settingphone(String phone) {
        String phone_s = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        return phone_s;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK && requestCode == 1) {
//            String countryCode = data.getStringExtra(BLConstants.INTENT_ID);
//            if(!TextUtils.isEmpty(countryCode)){
//                mCountryCode = countryCode;
//                mCountryCodeBtn.setText("+" + mCountryCode);
//            }
//        }
    }

    private void startTask(long time) {
        stopTask();
        timer = new Timer();
        task = new refreshGetCodeButtonTimeTask(time);
        timer.schedule(task, 0, 1000);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    //获取手机验证码
    private class GetVerifyCodeTask extends AsyncTask<String, Void, BLBaseResult> {
        private String phone;
        private BLProgressDialog blProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            blProgressDialog = BLProgressDialog.createDialog(getActivity(), null);
            blProgressDialog.show();
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putLong("getcodetime", System.currentTimeMillis());
            editor.commit();
            startTask(System.currentTimeMillis());
        }

        @Override
        protected BLBaseResult doInBackground(String... params) {
            //短信验证码
            phone = params[0];
            return BLLet.Account.sendRegVCode(phone, "+" + mCountryCode);
        }

        @Override
        protected void onPostExecute(BLBaseResult result) {
            super.onPostExecute(result);
            blProgressDialog.dismiss();
            if (result != null && result.getError() == BLHttpErrCode.SUCCESS) {
                getCodeLayout.setVisibility(View.GONE);
                resultLayout.setVisibility(View.GONE);
                inputPasswordLayout.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(phone)) {
                    tv_hassendcode.setText(getResources().getString(R.string.str_password_sendphone, settingphone(phone)));
                }
                //Toast.makeText(getActivity(), getResources().getString(R.string.str_sendcode_success), Toast.LENGTH_LONG).show();
            } else if (result != null) {
                Toast.makeText(getActivity(), result.getMsg() + ":" + result.getStatus(), Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_err_network), Toast.LENGTH_LONG).show();
            }
        }
    }


    //
    private class signupTask extends AsyncTask<String, Void, BLLoginResult> {
        private String phone;
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
            phone = params[0];
            code = params[1];
            password = params[2];
            BLRegistParam param = new BLRegistParam();
            param.setCode(code);
            param.setPhoneOrEmail(phone);
            param.setPassword(password);
            param.setNickname(phone);
            param.setCountrycode(mCountryCode);
            param.setSex("female");
            return BLLet.Account.regist(param, null);
        }

        @Override
        protected void onPostExecute(BLLoginResult result) {
            super.onPostExecute(result);
            blProgressDialog.dismiss();
            if (result != null && result.getError() == BLHttpErrCode.SUCCESS) {
                //Toast.makeText(getActivity(), getResources().getString(R.string.str_singup_success), Toast.LENGTH_LONG).show();
                //getActivity().finish();
                getCodeLayout.setVisibility(View.GONE);
                resultLayout.setVisibility(View.VISIBLE);
                inputPasswordLayout.setVisibility(View.GONE);
            } else if (result != null) {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_singup_fail) + ":" + result.getMsg() + "  " + result.getError(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.str_err_network), Toast.LENGTH_LONG).show();
            }
        }
    }


    //刷新短信验证时间限制
    class refreshGetCodeButtonTimeTask extends TimerTask {
        private long startTime;

        public refreshGetCodeButtonTimeTask(long time) {
            startTime = time;
        }

        @Override
        public void run() {
            long current = System.currentTimeMillis();
            final long remain = 60 - (current - startTime) / 1000;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (remain <= 0) {
                        mGetCode.setText(getResources().getString(R.string.str_send_verification_code));
                        mGetCode.setEnabled(true);
                    } else {
                        mGetCode.setEnabled(false);
                        mGetCode.setText(getResources().getString(R.string.str_getcoderemain_time, remain));
                    }
                }
            });
        }
    }
}


