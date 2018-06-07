package com.lbest.rm.common;

import android.app.Dialog;
import android.content.Context;
import android.widget.Button;

/**
 * Created by dell on 2017/11/29.
 */

public class BLAlert {

    private BLAlert(){};

    public static Dialog showDilog(Context context, int titleId,int messageId, int positiveButtonNameId,
                                   int negativeButtonNameId, final DialogOnClickListener clickListener){
        return showDilog(context,
                titleId != 0 ? context.getString(titleId) : null,
                messageId != 0 ? context.getString(messageId) : null,
                positiveButtonNameId != 0 ? context.getString(positiveButtonNameId) : null,
                negativeButtonNameId != 0 ? context.getString(negativeButtonNameId) : null,
                clickListener);

    }


    public static Dialog showDilog(Context context, String title, String message, String positiveButtonName,
                                   String negativeButtonName, final DialogOnClickListener clickListener) {
        return BLStyleDialog.Builder(context).setTitle(title).setMessage(message)
                .setCacelButton(negativeButtonName, new BLStyleDialog.OnBLDialogBtnListener() {
                    @Override
                    public void onClick(Button btn) {
                        if(clickListener != null){
                            clickListener.onNegativeClick();
                        }
                    }
                })
                .setConfimButton(positiveButtonName, new BLStyleDialog.OnBLDialogBtnListener() {
                    @Override
                    public void onClick(Button btn) {
                        if(clickListener != null){
                            clickListener.onPositiveClick();
                        }
                    }
                }).show();

    }


    public static Dialog showAlert(Context context, String title, String messageId,
                                   String confimButtonText, BLStyleDialog.OnBLDialogBtnListener confimBtnListener,
                                   String cancleButtonText, BLStyleDialog.OnBLDialogBtnListener cacenlBtnListener) {
        return BLStyleDialog.Builder(context).setTitle(title).setMessage(messageId)
                .setCacelButton(confimButtonText, confimBtnListener)
                .setConfimButton(cancleButtonText, cacenlBtnListener).show();
    }

    public interface DialogOnClickListener {
        void onPositiveClick();

        void onNegativeClick();
    }
}
