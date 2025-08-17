package com.fsoft.FP_sDraw.menu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.view.MotionEvent;
import android.widget.SeekBar;

import com.fsoft.FP_sDraw.common.Data;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 18.11.2015.
 */
public class SmoothSeekBar extends SeekBar {
    Paint linePaint = new Paint();
    Paint selectedLinePaint = new Paint();
    Paint circlePaint = new Paint();
    Timer timerRefresher = null;

    public SmoothSeekBar(Context context) {
        super(context);
        setWillNotDraw(false);
    }
    @Override public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(l);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {

        if(!Data.tools.isAllowedDeviceForUi(event))
            return false;
        runAnimation();
        super.onTouchEvent(event);
        return true;
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        linePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(h / 8f);

        selectedLinePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
        selectedLinePaint.setColor(Color.rgb(128, 203, 196));
        selectedLinePaint.setStrokeWidth(h/5f);

        circlePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStyle(Paint.Style.FILL);

    }
    @Override protected synchronized void onDraw(Canvas canvas) {
        float paddingR = getPaddingRight();
        float paddingL = getPaddingLeft();
        float xPos = paddingL + (getWidth() - paddingL - paddingR) * x/getMax();
        canvas.drawLine(xPos, getHeight()/2f, getWidth() - paddingR, getHeight()/2f, linePaint);
        canvas.drawLine(paddingL, getHeight() / 2f, xPos, getHeight() / 2f, selectedLinePaint);
        canvas.drawCircle(
                xPos,
                getHeight()/2f,
                getHeight()*0.5f,
                circlePaint);
        //super.onCanvasDraw(canvas);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runAnimation();
            }
        }, 200);
    }
    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        finishAnimation();
    }
    void runAnimation(){
        if(timerRefresher == null) {
            timerRefresher = new Timer();
            timerRefresher.schedule(new TimerTask() {
                @Override
                public void run() {
                    animate_view();
                }
            }, 15, 15);
        }
    }
    void  finishAnimation(){
        x = getProgress();
        if(timerRefresher != null){
            timerRefresher.cancel();
            timerRefresher = null;
        }
    }

    float x = 0;

    void animate_view(){
        float aim = getProgress();      //0...w
        float d = aim - x;              //-w...w
        x += d*0.2f;

        if(Math.abs(x-getProgress()) < 1f)
            finishAnimation();
        postInvalidate();
    }
}
