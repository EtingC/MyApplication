package com.lbest.rm.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.lbest.rm.R;

/**
 * Created by dell on 2017/10/26.
 */

public class BLProgressDialog extends Dialog {
    private BLProgressDialog(Context context) {
        super(context);
    }

    private BLProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    public static BLProgressDialog createDialog(Context context) {
        return createDialog(context, null);
    }

    public static BLProgressDialog createDialog(Context context, String message) {
        BLProgressDialog customProgressDialog = new BLProgressDialog(context, R.style.CustomProgressDialog);
        customProgressDialog.setContentView(R.layout.bl_progress_layout);
        WindowManager.LayoutParams layoutParams = customProgressDialog.getWindow().getAttributes();
        layoutParams.gravity = Gravity.CENTER;
        customProgressDialog.setCanceledOnTouchOutside(false);

        if(!TextUtils.isEmpty(message)){
            TextView messageView = (TextView) customProgressDialog.findViewById(R.id.message);
            messageView.setVisibility(View.VISIBLE);
            messageView.setText(message);
        }
        return customProgressDialog;
    }
}
