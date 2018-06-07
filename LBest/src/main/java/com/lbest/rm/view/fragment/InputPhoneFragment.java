package com.lbest.rm.view.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lbest.rm.R;
import com.lbest.rm.utils.CommonUtils;

/**
 * Created by dell on 2017/10/26.
 */

public class InputPhoneFragment extends BaseFragment {

    private EditText account_view;
    private TextView bt_next;
    private onNextClickListener clickListener;

    public void setClickListener(onNextClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface onNextClickListener{
        public void onClick(String data);
    }
    public InputPhoneFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.inputphone_fragment, container, false);

        findview(mView);
        setListener();
        initView();
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    private void findview(View rootView) {
        account_view = (EditText) rootView.findViewById(R.id.account_view);
        bt_next = (TextView) rootView.findViewById(R.id.bt_next);

    }

    private void setListener() {
        bt_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(clickListener!=null){
                    String data=account_view.getText().toString();
                    clickListener.onClick(data);
                }
            }
        });


        account_view.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    bt_next.setEnabled(true);
                } else {
                    bt_next.setEnabled(false);
                }
            }
        });
    }

    private void initView() {

    }
}
