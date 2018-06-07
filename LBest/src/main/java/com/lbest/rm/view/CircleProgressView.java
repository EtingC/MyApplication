package com.lbest.rm.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.lbest.rm.R;

/**
 * Created by dell on 2017/10/27.
 */

public class CircleProgressView extends View {
    private final Context mContext;

    private int circleInnerColor;

    // 画圆所在的距形区域
    private final RectF mRectF;
    private final Paint mPaint;
    private int mCircleLineStrokeWidth = 8;
    private int circleLineColorNo;
    private int circleLineColorDo;

    private int mMaxProgress = 100;
    private int mProgress = 30;
    private int mTxtProgressHint1StrokeWidth = 2;
    private int mTxtProgressHint1Color = 2;

    private String mTxtHint1;
    private int mTxtHint1StrokeWidth = 2;
    private int mTxtHint1Clolor = 2;

    private String mTxtHint2;
    private int mTxtHint2StrokeWidth = 2;
    private int mTxtHint2Clolor = 2;

    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        circleInnerColor = context.getResources().getColor(R.color.eaeae5);
        circleLineColorNo = context.getResources().getColor(R.color.eaeae5);
        circleLineColorDo = context.getResources().getColor(R.color.colorAccent);
        mTxtProgressHint1Color = context.getResources().getColor(R.color.colorAccent);
        mTxtHint1Clolor = context.getResources().getColor(R.color.gray);
        mTxtHint2Clolor = context.getResources().getColor(R.color.gray);

        mRectF = new RectF();
        mPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();

        if (width != height) {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }

        canvas.drawColor(Color.TRANSPARENT);
        // 绘制填充圆
        mPaint.setAntiAlias(true);
        mPaint.setColor(circleInnerColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(mCircleLineStrokeWidth);
        mRectF.left = mCircleLineStrokeWidth / 2.0f; // 左上角x
        mRectF.top = mCircleLineStrokeWidth / 2.0f; // 左上角y
        mRectF.right = width - mCircleLineStrokeWidth / 2.0f; // 左下角x
        mRectF.bottom = height - mCircleLineStrokeWidth / 2.0f; // 右下角y

        // 绘制圆圈，尚未完成的
        canvas.drawArc(mRectF, -90, 360, false, mPaint);
        mPaint.setColor(circleLineColorNo);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mCircleLineStrokeWidth);
        canvas.drawArc(mRectF, ((float) mProgress / mMaxProgress) * 360, 360, false, mPaint);

        // 绘制圆圈，已经完成的
        canvas.drawArc(mRectF, -90, 360, false, mPaint);
        mPaint.setColor(circleLineColorDo);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mCircleLineStrokeWidth);
        canvas.drawArc(mRectF, -90, ((float) mProgress / mMaxProgress) * 360, false, mPaint);

        // 绘制进度文案显示
        mPaint.setColor(mTxtProgressHint1Color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mTxtProgressHint1StrokeWidth);
        String text = mProgress + "%";
        int textHeight = height / 4;
        mPaint.setTextSize(textHeight);
        int textWidth = (int) mPaint.measureText(text, 0, text.length());
        //计算一个文字的高度
        Rect rect = new Rect();
        mPaint.getTextBounds(text, 0, text.length(), rect);
        int textHeightX = rect.height();

        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(text, width / 2.0f - textWidth / 2.0f, height / 2.0f + (textHeightX / 3.0f), mPaint);

        if (!TextUtils.isEmpty(mTxtHint1)) {
            mPaint.setColor(mTxtHint1Clolor);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(mTxtHint1StrokeWidth);
            text = mTxtHint1;
            textHeight = height / 8;
            mPaint.setTextSize(textHeight);
            textWidth = (int) mPaint.measureText(text, 0, text.length());
            canvas.drawText(text, width / 2 - textWidth / 2, height / 4 + textHeight / 2, mPaint);
        }

        if (!TextUtils.isEmpty(mTxtHint2)) {
            mPaint.setColor(mTxtHint2Clolor);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(mTxtHint2StrokeWidth);
            text = mTxtHint2;
            textHeight = height / 8;
            mPaint.setTextSize(textHeight);
            textWidth = (int) mPaint.measureText(text, 0, text.length());
            canvas.drawText(text, width / 2 - textWidth / 2, 3 * height / 4 + textHeight / 2, mPaint);
        }
    }

    public void setProgressNotInUiThread(int progress) {
        this.mProgress = progress;
        this.postInvalidate();
    }

    public int getmCircleLineStrokeWidth() {
        return mCircleLineStrokeWidth;
    }

    public void setmCircleLineStrokeWidth(int mCircleLineStrokeWidth) {
        this.mCircleLineStrokeWidth = mCircleLineStrokeWidth;
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        this.mMaxProgress = maxProgress;
    }

    public int getmProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        this.mProgress = progress;
        this.invalidate();
    }

    public int getmTxtProgressHint1StrokeWidth() {
        return mTxtProgressHint1StrokeWidth;
    }

    public void setmTxtProgressHint1StrokeWidth(int mTxtProgressHint1StrokeWidth) {
        this.mTxtProgressHint1StrokeWidth = mTxtProgressHint1StrokeWidth;
    }

    public String getmTxtHint1() {
        return mTxtHint1;
    }

    public void setmTxtHint1(String mTxtHint1) {
        this.mTxtHint1 = mTxtHint1;
    }

    public int getmTxtHint1StrokeWidth() {
        return mTxtHint1StrokeWidth;
    }

    public void setmTxtHint1StrokeWidth(int mTxtHint1StrokeWidth) {
        this.mTxtHint1StrokeWidth = mTxtHint1StrokeWidth;
    }

    public String getmTxtHint2() {
        return mTxtHint2;
    }

    public void setmTxtHint2(String mTxtHint2) {
        this.mTxtHint2 = mTxtHint2;
    }

    public int getmTxtHint2StrokeWidth() {
        return mTxtHint2StrokeWidth;
    }

    public void setmTxtHint2StrokeWidth(int mTxtHint2StrokeWidth) {
        this.mTxtHint2StrokeWidth = mTxtHint2StrokeWidth;
    }

    public int getCircleInnerColor() {
        return circleInnerColor;
    }

    public void setCircleInnerColor(int circleInnerColor) {
        this.circleInnerColor = circleInnerColor;
    }

    public int getCircleLineColorNo() {
        return circleLineColorNo;
    }

    public void setCircleLineColorNo(int circleLineColorNo) {
        this.circleLineColorNo = circleLineColorNo;
    }

    public int getCircleLineColorDo() {
        return circleLineColorDo;
    }

    public void setCircleLineColorDo(int circleLineColorDo) {
        this.circleLineColorDo = circleLineColorDo;
    }

    public int getmTxtProgressHint1Color() {
        return mTxtProgressHint1Color;
    }

    public void setmTxtProgressHint1Color(int mTxtProgressHint1Color) {
        this.mTxtProgressHint1Color = mTxtProgressHint1Color;
    }

    public int getmTxtHint1Clolor() {
        return mTxtHint1Clolor;
    }

    public void setmTxtHint1Clolor(int mTxtHint1Clolor) {
        this.mTxtHint1Clolor = mTxtHint1Clolor;
    }

    public int getmTxtHint2Clolor() {
        return mTxtHint2Clolor;
    }

    public void setmTxtHint2Clolor(int mTxtHint2Clolor) {
        this.mTxtHint2Clolor = mTxtHint2Clolor;
    }
}
