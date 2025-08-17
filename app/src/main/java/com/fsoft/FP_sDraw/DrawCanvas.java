package com.fsoft.FP_sDraw;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

/**
 * хрень для отрисовки всего в режиме канвы
 * Created by Dr. Failov on 28.12.2015.
 */
@SuppressLint("ViewConstructor")
public class DrawCanvas extends View {
    public DrawCore draw;
    public Handler uiHandler = new Handler();

    public DrawCanvas(MainActivity context) {
        super(context);
        draw = new DrawCore(context, this);
        draw.addRedrawListener(new DrawCore.OnRedrawListener() {
            @Override
            public void redraw() {
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    // On UI thread.
                    invalidate();
                } else {
                    // Not on UI thread.
                    uiHandler.post(DrawCanvas.this::invalidate);
                }
            }

            @Override
            public void redraw(final int top, final int bottom, final int left, final int right) {
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    // On UI thread.
                    invalidate(left, top, right, bottom);
                } else {
                    // Not on UI thread.
                    uiHandler.post(() -> invalidate(left, top, right, bottom));
                }
            }
        });
    }

    public DrawCore getDrawCore() {
        return draw;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        draw.sizeChanged(w,h);

    }

    @Override public boolean onTouchEvent(MotionEvent event){
        return draw.processEvent(event);
    }
    @Override public boolean onHoverEvent(MotionEvent event) {
        return draw.processEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        draw.drawContent(canvas);
    }

    @Override public void invalidate() {
        super.invalidate();
    }

}
