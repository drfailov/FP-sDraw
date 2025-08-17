package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.common.Data;

/**
 * класс занимается рисованием кружочка в месте где обнаружено действие над поверхностью экрана
 * Также отображением базовых меню типа переключения пера и т.п.
 * Created by drfailov on 28.10.15.
 */
public class HoverPointer implements Instrument {
    private final DrawCore draw;

    //drawing pointer
    boolean hovering = false;
    float xHover = 0;
    float yHover = 0;
    float size = 10;
    Paint paintInner = new Paint();
    Paint paintOuter = new Paint();
    //отрисовывать поинтер только в течении короткого промежутка после ивента
    long lastEventTime = 0;
    long pointerTimeout = 200;//ms




    public HoverPointer(DrawCore draw) {
        this.draw = draw;
    }

    @Override
    public String getName() {
        return "hoverPointer";
    }

    @Override
    public String getVisibleName() {
        return "Hover Pointer";
    }

    @Override
    public int getImageResourceID() {
        return 0;
    }

    @Override
    public void onSelect() {
        paintInner.setColor(Color.BLACK);
        paintInner.setStyle(Paint.Style.STROKE);
        paintInner.setStrokeWidth(1f);
        paintOuter.setColor(Color.WHITE);
        paintOuter.setStyle(Paint.Style.STROKE);
        paintOuter.setStrokeWidth(1f);
    }

    @Override
    public void onDeselect() {

    }

    @Override
    public boolean onTouch(MotionEvent event) {
        int action = event.getAction();
        //этот код требовался для изучения поведения сенсора
         //if(action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_HOVER_MOVE)
         //    Logger.log(event.toString());

        if(action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE){
            hovering = true;
            xHover = event.getX();
            yHover = event.getY();
            int gap = 10;
            lastEventTime = System.currentTimeMillis();
            draw.redraw((int)yHover - gap, (int)yHover + gap, (int)xHover - gap, (int)xHover + gap);
        }
        else if(action == MotionEvent.ACTION_HOVER_EXIT || action == MotionEvent.ACTION_DOWN){
            hovering = false;
            int gap = 10;
            draw.redraw((int)yHover - gap, (int)yHover + gap, (int)xHover - gap, (int)xHover + gap);
            //draw.invalidate();
        }
        return false;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        //hovering pointer
        if(hovering){
            long time = System.currentTimeMillis();
            if(time - lastEventTime > pointerTimeout){
                hovering = false;
                return;
            }
            if((draw.currentInstrument == draw.brush || draw.currentInstrument == draw.line)) {
                size = (Integer) Data.get(Data.brushSizeInt()) / 2f;
                size *= draw.scale.scale_size;
            }
            else if(draw.currentInstrument == draw.erase) {
                size = (Integer) Data.get(Data.eraserSizeInt()) / 2f;
                size *= draw.scale.scale_size;
            }
            else if(draw.currentInstrument == draw.mosaic) {
                size = (Integer) Data.get(Data.mosaicSizeInt()) / 2f;
                size *= draw.scale.scale_size;
            }
            else
                size = Data.store().DPI / 50;

            if(draw.currentInstrument == draw.mosaic){
                canvas.drawRect(xHover-size, yHover-size, xHover+size, yHover+size, paintOuter);
                canvas.drawRect(xHover-size+1, yHover-size+1, xHover+size-1, yHover+size-1, paintInner);
            }
            else {
                canvas.drawCircle(xHover, yHover, size, paintOuter);
                canvas.drawCircle(xHover, yHover, size - 1, paintInner);
            }
        }
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override  public boolean isVisibleToUser() {
        return false;
    }

    @Override
    public View.OnClickListener getOnClickListener() {
        return null;
    }

}
