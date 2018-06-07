package com.lbest.rm.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.lbest.rm.R;

/**
 * Created by dell on 2017/11/2.
 */

public class FloatGuideView extends View {
    /***手势向上**/
    public static final int HANDER_TOP = 0;
    /***手势向左**/
    public static final int HANDER_LEFT = 1;
    /***手势向右**/
    public static final int HANDER_RIGHT = 2;
    /***手势向下**/
    public static final int HANDER_BOTTOM = 3;

    /***文本提示位置 上**/
    public static final int HINT_TEXT_TOP = 0;
    /***文本提示位置 左**/
    public static final int HINT_TEXT_LEFT = 1;
    /***文本提示位置 右*/
    public static final int HINT_TEXT_RIGHT = 2;
    /***文本提示位置 下**/
    public static final int HINT_TEXT_BOTTOM = 3;

    private int mWindth, mHeight;

    private Rect mFloatRect = new Rect();

    //浮层坐标和大小
    private int mFloatX, mFloatY;
    private int mFloatWindth, mFloatHeight;
    //浮层模型
    private Matrix mMatrix = new Matrix();
    //手势模型
    private Matrix mHanderMatrix = new Matrix();

    //浮层 Bitmap
    private Bitmap mFloatBitmap;
    //浮层 手势图标
    private Bitmap mHandBitmap;

    /**是否可以点击阴影部分消失**/
    private boolean mShadowTouched = false;

    //浮层区域点击事件
    private OnFloatViewClickListener mOnFloatViewClickListener;

    private OnFloatViewGoneListener mOnFloatViewGoneListener;

    //文本上下左右padding
    private int mTextPadding;
    //文本行间距
    private int mTextLineSpace;
    //文字大小
    private int mTextSize;
    //背景圆角尺寸
    private int mHintBgRoundSize;
    //文本框 最多宽度
    private int TEXT_MAX_WIDTH;

    private String mHintText = "请详细阅读配置指南， 点击这里，还是配置。";

    private int mHintPosition;
    //箭头偏移
    private int mArrowOffset;

    public FloatGuideView(Context context) {
        super(context);
        init(context);
    }

    public FloatGuideView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatGuideView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        mFloatBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.guide_float);
        mHandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.guide_hand);

        initTextViewSize(context);
    }

    private void initTextViewSize(Context context){
        mTextPadding = dip2px(context, 10);
        mTextLineSpace = dip2px(context, 1);
        mHintBgRoundSize = dip2px(context, 5);
        mTextSize = sp2px(context, 13);
        mArrowOffset = dip2px(context, 25);
        TEXT_MAX_WIDTH = dip2px(context, 180);
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     *            （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    private int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.e("FloatGuideView", "onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWindth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        Log.e("FloatGuideView", "onMeasure Windth:" + mWindth);
        Log.e("FloatGuideView", "onMeasure Height:" + mHeight);
    }

    public void setOnFloatViewClickListener(OnFloatViewClickListener onFloatViewClickListener){
        mOnFloatViewClickListener = onFloatViewClickListener;
    }

    public void setOnFloatViewGoneListener(OnFloatViewGoneListener onFloatViewGoneListener){
        mOnFloatViewGoneListener = onFloatViewGoneListener;
    }

    /**
     * 设置浮层位置
     * @param x
     *           浮层x坐标
     * @param y
     *           浮层y坐标
     * @param width
     *            宽度
     * @param height
     *            高度
     * @param handerDirection
     *            手势方向
     * @param hintText
     *          提示文字
     * @param hintPosition
     *          显示位置
     */
    public void setFloatLocation(int x, int y, int width, int height, int handerDirection, String hintText, int hintPosition){
        mFloatX = x;
        mFloatY = y;
        mFloatHeight = height;
        mFloatWindth = width;
        mHintPosition = hintPosition;

        float sx = (((float) width) / mFloatBitmap.getWidth());
        float sy = (((float) height) / mFloatBitmap.getHeight());
        Log.d("FloatGuideView", "sx" + sx);
        Log.d("FloatGuideView", "sy" + sy);

        mMatrix.postScale(sx, sy);
        mMatrix.postTranslate(x, y);

        mFloatRect.left = x;
        mFloatRect.top = y;
        mFloatRect.right = x + width;
        mFloatRect.bottom = y + height;

        int handerX = 0, handerY = 0;
        int handerWidth = mHandBitmap.getWidth(), handerHeight = mHandBitmap.getHeight();

        if(handerDirection == HANDER_LEFT){
            handerWidth = mHandBitmap.getHeight();
            handerHeight = mHandBitmap.getWidth();
            handerX = (int) (mFloatRect.right - (handerWidth / 2f));
            if(handerX + handerWidth > mWindth){
                handerX = mWindth - handerWidth;
            }
            handerY = y + (height - handerHeight) / 2;

            mHanderMatrix.postRotate(270, (float)mHandBitmap.getWidth()/2, (float)mHandBitmap.getHeight()/2);
        }else if(handerDirection == HANDER_RIGHT){
            handerWidth = mHandBitmap.getHeight();
            handerHeight = mHandBitmap.getWidth();
            mHanderMatrix.postRotate(90, (float)mHandBitmap.getWidth()/2, (float)mHandBitmap.getHeight()/2);

            handerX = (int) (mFloatRect.left - (handerWidth / 2f));
            if(handerX < 0){
                handerX = 0;
            }

            handerY = y + (height - handerHeight) / 2;
        }else if(handerDirection == HANDER_BOTTOM){
            mHanderMatrix.postRotate(180, (float)mHandBitmap.getWidth()/2, (float)mHandBitmap.getHeight()/2);
            handerX = x + (width  - handerWidth) / 4;
            handerY = y + height / 2 - handerHeight;
        }else{
            handerX = x + width / 2;
            handerY = y + height / 2;
        }
        mHanderMatrix.postTranslate(handerX, handerY);

        mHintText = hintText;

        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Log.d("FloatGuideView", "draw");
        canvas.drawColor(Color.TRANSPARENT);
        int sc = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null, Canvas.ALL_SAVE_FLAG);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xb3333333);
        canvas.drawRect(0, 0, mWindth, mHeight, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        canvas.drawBitmap(mFloatBitmap, mMatrix, paint);

        //还原
        paint.setXfermode(null);

        canvas.drawBitmap(mHandBitmap, mHanderMatrix, paint);

        //显示提示框
        if(mHintText != null){
            Bitmap hintBitMap = createHitBitmap(mHintText);
            Bitmap arrowBitmap = getArrowBitmap();

            int[][] location = initHintPostion(hintBitMap, arrowBitmap);

            canvas.drawBitmap(hintBitMap, location[0][0], location[0][1], paint);

            canvas.drawBitmap(arrowBitmap, location[1][0], location[1][1], paint);
        }

        canvas.restoreToCount(sc);
    }

    private Bitmap getArrowBitmap(){
        if(mHintPosition == HINT_TEXT_TOP){
            return BitmapFactory.decodeResource(getResources(), R.drawable.guid_arrow_bottom);
        }else if(mHintPosition == HINT_TEXT_BOTTOM){
            return BitmapFactory.decodeResource(getResources(), R.drawable.guid_arrow_top);
        }else if(mHintPosition == HINT_TEXT_LEFT){
            return  BitmapFactory.decodeResource(getResources(), R.drawable.guid_arrow_right);
        }else{
            return BitmapFactory.decodeResource(getResources(), R.drawable.guid_arrow_left);
        }
    }

    private int[][] initHintPostion(Bitmap hintBitMap, Bitmap arrowBitmap){
        int[][] location = new int[2][2];
        int[] hintLocation = new int[2];
        int[] arrowLocation = new int[2];

        if(mHintPosition == HINT_TEXT_TOP){
            hintLocation[0] = mFloatX + mFloatWindth / 2;
            hintLocation[1] = mFloatY - hintBitMap.getHeight();
        }else if(mHintPosition == HINT_TEXT_BOTTOM){
            hintLocation[0] = mFloatX + mFloatWindth / 2;
            hintLocation[1] = mFloatY + mFloatHeight;
        }else if(mHintPosition == HINT_TEXT_LEFT){
            hintLocation[0] = mFloatX - hintBitMap.getWidth();
            hintLocation[1] = mFloatY;
        }else if(mHintPosition == HINT_TEXT_RIGHT){
            hintLocation[0] = mFloatX + mFloatWindth;
            hintLocation[1] = mFloatY + mFloatHeight / 4;
        }

        if(hintLocation[0] + hintBitMap.getWidth() > mWindth){
            hintLocation[0] = mWindth - hintBitMap.getWidth();
        }

        if(hintLocation[0] < 0){
            hintLocation[0] = 0;
        }

        if(hintLocation[1] + hintBitMap.getHeight() > mHeight){
            hintLocation[1] = mHeight - hintBitMap.getHeight();
        }

        if(hintLocation[1] < 0){
            hintLocation[1] = 0;
        }

        if(mHintPosition == HINT_TEXT_TOP){
            arrowLocation[0] = hintLocation[0] + mArrowOffset;
            arrowLocation[1] = hintLocation[1] + hintBitMap.getHeight();
        }else if(mHintPosition == HINT_TEXT_BOTTOM){
            arrowLocation[0] = hintLocation[0] + mArrowOffset;
            arrowLocation[1] = hintLocation[1] - arrowBitmap.getHeight();
        }else if(mHintPosition == HINT_TEXT_LEFT){
            arrowLocation[0] = hintLocation[0] + hintBitMap.getWidth();
            arrowLocation[1] = hintLocation[1] + mArrowOffset / 3;
        }else if(mHintPosition == HINT_TEXT_RIGHT){
            arrowLocation[0] = hintLocation[0] - arrowBitmap.getWidth();
            arrowLocation[1] = hintLocation[1] + mArrowOffset / 3;
        }

        location[0] = hintLocation;
        location[1] = arrowLocation;

        return location;
    }

    private Bitmap createHitBitmap(String hintText){
        TextPaint textPaint = new TextPaint();// 设置画笔
        textPaint.setTextSize(mTextSize);// 字体大小
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);// 采用默认的宽度
        textPaint.setColor(0xff333333);

//        float itemTextWidth = textPaint.measureText(String.valueOf(hintText.charAt(0)));

        int width = 10, hight = 10;
        int maxWidth = (int) (textPaint.measureText(hintText) + mTextPadding * 2);
        width = maxWidth < TEXT_MAX_WIDTH ? maxWidth : TEXT_MAX_WIDTH;

        //计算一个文字的高度
        Rect rect = new Rect();
        textPaint.getTextBounds(hintText, 0, hintText.length(), rect);
        int textHeight = rect.height();

        Log.d("FloatGuideVie", "Text Height：" + rect.height());


        //文本区域的宽度;
        int textBoundWith = width - mTextPadding;

        int line = 1;
        if(!hintText.contains(" ")){
            if(maxWidth > TEXT_MAX_WIDTH){ //计算需要显示多少行
                line = (maxWidth / TEXT_MAX_WIDTH) + ((maxWidth % TEXT_MAX_WIDTH) > 0 ? 1 : 0);
            }
        }else{
            float maxwidth = 0;
            String[] itemTexts = hintText.split(" ");
            float measureSpaceWidth = textPaint.measureText(" ");
            float textX = mTextPadding;
            for (int i = 0; i < itemTexts.length; i++) {
                float cacheWidth = textX + textPaint.measureText(itemTexts[i]);

                if(cacheWidth > textBoundWith){
                    if(textX > maxwidth){
                        maxwidth = textX;
                    }
                    textX = mTextPadding;
                    line ++;
                }else{
                    textX = cacheWidth + measureSpaceWidth;
                }
            }

            textBoundWith = (int) maxwidth;
            width = textBoundWith + mTextPadding;
        }

        hight = line * (textHeight + mTextLineSpace) + mTextPadding * 2;

        Bitmap icon = Bitmap.createBitmap(width, hight, Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        //创建画布画背景
        RectF bgRectF = new RectF();
        bgRectF.right = width;
        bgRectF.bottom = hight;
        Canvas canvas = new Canvas(icon);
        canvas.drawRoundRect(bgRectF, mHintBgRoundSize, mHintBgRoundSize, paint);

        //画文字内容
        float textX = mTextPadding; float textY = textHeight + mTextPadding;

        if(!hintText.contains(" ")){
            for (int i = 0; i < hintText.length(); i++) {
                String item = String.valueOf(hintText.charAt(i));
                float itemTextWidth = textPaint.measureText(item);
                if ((textX + itemTextWidth) > textBoundWith) {
                    textX = mTextPadding;
                    textY = textY + mTextLineSpace + textHeight;
                }

                canvas.drawText(item, textX, textY, textPaint);
                textX = textX + itemTextWidth;
            }
        }else{
            String[] itemTexts = hintText.split(" ");
            float measureSpaceWidth = textPaint.measureText(" ");
            for (int i = 0; i < itemTexts.length; i++) {
                String itemStr = itemTexts[i];
                float cacheWidth = textX + textPaint.measureText(itemStr);
                if(cacheWidth > textBoundWith){
                    textX = mTextPadding;
                    textY = textY + mTextLineSpace + textHeight;
                }

                for (int i1 = 0; i1 < itemStr.length(); i1++) {
                    String item = String.valueOf(itemStr.charAt(i1));
                    float itemTextWidth = textPaint.measureText(item);
                    canvas.drawText(item, textX, textY, textPaint);
                    textX = textX + itemTextWidth;
                }

                canvas.drawText(" ", textX, textY, textPaint);
                textX = textX + measureSpaceWidth;
            }
        }

        return icon;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            int eventX = (int) event.getX();
            int eventY = (int) event.getY();
            Log.d("FloatGuideView", "onTouchEventX:" + eventX);
            Log.d("FloatGuideView", "onTouchEventY:" + eventY);

            if(mFloatRect.contains(eventX, eventY) || mShadowTouched){
                setVisibility(GONE);
                if(mOnFloatViewGoneListener != null){
                    mOnFloatViewGoneListener.onViewGone();
                }
                if(mOnFloatViewClickListener != null){
                    //是否点击在浮层的区域
                    if(mFloatRect.contains(eventX, eventY)){
                        mOnFloatViewClickListener.onClick();
                    }else{
                        mOnFloatViewClickListener.onFloatShadowClick();
                    }
                }
            }
        }
        return true;
    }

    public void setShadowTouched(boolean shadowTouched){
        mShadowTouched = shadowTouched;
    }

    public interface OnFloatViewClickListener{
        //浮层高亮部分点击
        void onClick();
        //浮层阴影点击
        void onFloatShadowClick();
    }

    public interface OnFloatViewGoneListener{
        void onViewGone();
    }
}