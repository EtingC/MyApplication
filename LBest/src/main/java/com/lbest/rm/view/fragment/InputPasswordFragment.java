package com.lbest.rm.view.fragment;

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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.lbest.rm.GetBackPasswordActivity;
import com.lbest.rm.R;
import com.lbest.rm.utils.CommonUtils;

/**
 * Created by dell on 2017/10/26.
 */

public class InputPasswordFragment extends BaseFragment {
    private boolean hasCreate = false;
    private TextView tv_hassendcode;
    private EditText et_code;
    private EditText et_password1;
    private EditText et_password2;

    private ImageButton iv_password1;
    private ImageButton iv_password2;

    private Button btn_next;
    private onNextClickListener clickListener;

    private String phone;

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setClickListener(onNextClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface onNextClickListener{
        public void onClick(String code,String password);
    }
    public InputPasswordFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.inputpassword_fragment, container, false);

        findview(mView);
        setListener();
        initView();

        hasCreate = true;
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (hasCreate) {
            if (isVisibleToUser) {

            } else {

            }
        }
    }

    private void findview(View rootView) {
        tv_hassendcode= (TextView) rootView.findViewById(R.id.tv_hassendcode);
        et_code = (EditText) rootView.findViewById(R.id.et_code);
        et_password1 = (EditText) rootView.findViewById(R.id.et_password1);
        et_password2 = (EditText) rootView.findViewById(R.id.et_password2);

        iv_password1= (ImageButton) rootView.findViewById(R.id.iv_password1);
        iv_password2= (ImageButton) rootView.findViewById(R.id.iv_password2);
        btn_next = (Button) rootView.findViewById(R.id.btn_next);
    }

    private void setListener() {
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password1 = et_password1.getText().toString();
                String password2 = et_password2.getText().toString();
                if( password1.equals(password2)){
                    String code=et_code.getText().toString();
                   if(clickListener!=null){
                       clickListener.onClick(code,password1);
                   }
                }else{
                    Toast.makeText(getContext(), getResources().getString(R.string.str_newpassworddifferent), Toast.LENGTH_SHORT).show();
                }
            }
        });


        et_password1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String  code= et_code.getText().toString();
                String password2 = et_password2.getText().toString();
                if (s.length() > 0&& !TextUtils.isEmpty(code) && !TextUtils.isEmpty(password2)) {
                    btn_next.setEnabled(true);
                } else {
                    btn_next.setEnabled(false);
                }
            }
        });


        et_password2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password1 = et_password1.getText().toString();
                String  code= et_code.getText().toString();
                if (s.length() > 0&& !TextUtils.isEmpty(password1) && !TextUtils.isEmpty(code)) {
                    btn_next.setEnabled(true);
                } else {
                    btn_next.setEnabled(false);
                }
            }
        });


        et_code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String password1 = et_password1.getText().toString();
                String password2 = et_password2.getText().toString();
                if (s.length() > 0&& !TextUtils.isEmpty(password1) && !TextUtils.isEmpty(password2)) {
                    btn_next.setEnabled(true);
                } else {
                    btn_next.setEnabled(false);
                }
            }
        });


        iv_password1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputtype=et_password1.getInputType();

                if (inputtype== InputType.TYPE_CLASS_TEXT) {
                    iv_password1.setImageResource(R.drawable.password_invisiable);
                    et_password1.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);

                } else {
                    iv_password1.setImageResource(R.drawable.password_visiable);
                    et_password1.setInputType(InputType.TYPE_CLASS_TEXT);
                }
            }
        });


        iv_password2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputtype=et_password2.getInputType();

                if (inputtype==InputType.TYPE_CLASS_TEXT) {
                    iv_password2.setImageResource(R.drawable.password_invisiable);
                    et_password2.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_CLASS_TEXT);

                } else {
                    iv_password2.setImageResource(R.drawable.password_visiable);
                    et_password2.setInputType(InputType.TYPE_CLASS_TEXT);
                }
            }
        });

    }

    private void initView() {
        if(!TextUtils.isEmpty(phone)){
            tv_hassendcode.setText(getResources().getString(R.string.str_password_sendphone,settingphone(phone)));
        }
    }

    public String settingphone(String phone) {
        String phone_s = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        return phone_s;
    }
}
