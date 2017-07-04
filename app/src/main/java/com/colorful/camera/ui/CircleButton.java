package com.colorful.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.colorful.camera.R;

/**
 * Created by sg on 2017/5/25.
 */

public class CircleButton extends View {

    private Paint mPaint;
    //    private int mWidth = 100;
//    private int mHeight = 100;
    private int mCircleColor = Color.parseColor("#ffffff");
    private int mCenterCircleRadius = 70;
    private int mOuterCircleRadius = 100;
    private int mOuterCircleWidth = 20;
    private boolean mNeedCenterCircle;
    private int mProgressColor = Color.parseColor("#FF0000");
    private long mMaxValue = 0;
    private long mProgressValue = 0;

    public CircleButton(Context context) {
        super(context);
    }

    public CircleButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.circleButton, 0, 0);
//        mWidth = typedArray.getDimensionPixelSize(R.styleable.circleButton_btn_width, mWidth);
//        mHeight = typedArray.getDimensionPixelSize(R.styleable.circleButton_btn_height, mHeight);
        mCenterCircleRadius = typedArray.getDimensionPixelSize(R.styleable.circleButton_center_circle_radius, mCenterCircleRadius);
        mOuterCircleRadius = typedArray.getDimensionPixelSize(R.styleable.circleButton_outer_circle_radius, mOuterCircleRadius);
        mOuterCircleWidth = typedArray.getDimensionPixelSize(R.styleable.circleButton_outer_circle_width, mOuterCircleWidth);
        mNeedCenterCircle = typedArray.getBoolean(R.styleable.circleButton_need_center_circle, true);
        mProgressColor = typedArray.getColor(R.styleable.circleButton_progress_color, mProgressColor);
        typedArray.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //中心圆
        int center = getWidth() / 2;
        if (mNeedCenterCircle) {
            //填充
            mPaint.setColor(mCircleColor);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(center, center, mCenterCircleRadius, mPaint);
        }

        //静态圆环
        mPaint.setColor(mCircleColor);
        mPaint.setStyle(Paint.Style.STROKE);
        //设置圆环的宽度
        mPaint.setStrokeWidth(mOuterCircleWidth);
        canvas.drawCircle(center, center, mOuterCircleRadius, mPaint);

        Log.e("sg", "max==" + mMaxValue + "--progressValue==" + mProgressValue);
        //动态进度圆环
        if (mMaxValue > 0) {
            mPaint.setStrokeWidth(mOuterCircleWidth);
            mPaint.setColor(mProgressColor);
            // 用于定义的圆弧的形状和大小的界限
            RectF oval = new RectF(center - mOuterCircleRadius,
                    center - mOuterCircleRadius,
                    center + mOuterCircleRadius,
                    center + mOuterCircleRadius);
            canvas.drawArc(oval, -90, 360 * mProgressValue / mMaxValue, false, mPaint);
        }
    }

    public synchronized void setMax(long max) {
        if (max <= 0) {
            return;
        }

        this.mMaxValue = max;
    }

    public synchronized void setProgress(long progress) {
        if (progress < 0) {
            return;
        }

        if (progress > mMaxValue) {
            progress = mMaxValue;
        }

        if (progress <= mMaxValue) {
            this.mProgressValue = progress;
            postInvalidate();
        }
    }


}
