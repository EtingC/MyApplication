package com.lbest.rm.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.lbest.rm.R;

/**
 * Created by dell on 2017/10/30.
 */

public class InputTextView extends LinearLayout {
    private EditText mEditText;
    private String mEditHint;
    private TextWatcher mTextWatcher;
    private ImageView iv_delete;
    public InputTextView(Context context) {
        super(context);
        init(context);
    }

    public InputTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttrs(context,attrs);
        init(context);
    }

    public InputTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttrs(context,attrs);
        init(context);
    }

    private void getAttrs(Context context,AttributeSet attrs){
        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.InputTextView);
        mEditHint = array.getString(R.styleable.InputTextView_hint);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        mEditText = new EditText(context);
        mEditText.setBackgroundResource(0);
        mEditText.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.weight = 1;
        layoutParams.setMargins(-15,0,0,0);
        mEditText.setTextColor(Color.rgb(108, 109, 104));
        mEditText.setSingleLine();
        mEditText.setLayoutParams(layoutParams);
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        if(mEditHint != null && !mEditHint.equals(""))
            mEditText.setHint(mEditHint);
        addView(mEditText);

        final ImageView deleteButton = new ImageView(context);
        deleteButton.setImageResource(R.drawable.btn_editor_delete);
        deleteButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LayoutParams deleteButtonLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        deleteButtonLayoutParams.weight = 0;
        deleteButton.setLayoutParams(deleteButtonLayoutParams);
        deleteButton.setVisibility(View.GONE);
        iv_delete=deleteButton;
        addView(deleteButton);

        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mEditText.setText(null);
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(mTextWatcher != null){
                    mTextWatcher.onTextChanged(s, start, before, count);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(mTextWatcher != null){
                    mTextWatcher.beforeTextChanged(s, start, count, after);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    deleteButton.setVisibility(View.VISIBLE);
                } else {
                    deleteButton.setVisibility(View.GONE);
                }
                if(mTextWatcher != null){
                    mTextWatcher.afterTextChanged(s);
                }
            }
        });
    }

    public void resetView(){
        if(iv_delete!=null){
            iv_delete.setVisibility(View.GONE);
        }
        if(mEditText != null){
            mEditText.setText(null);
        }
    }

    public void addTextChangedListener(TextWatcher watcher){
        mTextWatcher = watcher;
    }

    /**
     * 设置输入提示内容
     * @param hint
     */
    public void setTextHint(String hint){
        if(mEditText != null){
            mEditText.setHint(hint);
        }
    }

    public void setTextHintColor(int hintColor){
        if(mEditText != null){
            mEditText.setHintTextColor(hintColor);
        }
    }
    /**
     * 设置输入提示内容
     * @param hintId
     */
    public void setTextHint(int hintId){
        if(mEditText != null){
            mEditText.setHint(hintId);
        }
    }

    public void setFocusChangeListener(OnFocusChangeListener listener){
        if(mEditText != null){
            mEditText.setOnFocusChangeListener(listener);
        }
    }

    /**
     * 设置输入框的内容
     * @param text
     */
    public void setText(String text){
        if(mEditText != null && text != null){
            mEditText.setText(text);
            mEditText.setSelection(text.length());
        }
    }

    /**
     * 设置最大输入长度
     * @param lenght
     */
    public void setMaxLength(int lenght){
        if(mEditText != null){
            mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(lenght)});
        }
    }

    /**
     * 设置字体颜色
     * @param color
     */
    public void setTextColor(int color){
        if(mEditText != null){
            mEditText.setTextColor(color);
        }
    }

    public boolean requestEditTextViewFocus(){
        if(mEditText != null){
           return mEditText.requestFocus();
        }
        return false;
    }

    public void clearFocus(){
        if(mEditText != null){
            mEditText.clearFocus();
        }
    }

    public void setTouchListener(OnTouchListener l) {
        if(mEditText != null){
            mEditText.setOnTouchListener(l);
        }
    }

    public void setTextSize(float size){
        if(mEditText != null){
            mEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        }
    }

    /**
     * 获取输入框的内容
     * @return String
     */
    public String getTextString(){
        if(mEditText != null){
            return mEditText.getText().toString();
        }
        return null;
    }

    public void setInputType(int type){
        if(mEditText != null){
            mEditText.setInputType(type);
        }
    }

    public int getInputType(){
        return  mEditText.getInputType();
    }

    public EditText getEditText(){
        return mEditText;
    }
}
