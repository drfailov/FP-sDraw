package com.fsoft.FP_sDraw.instruments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.view.*;
import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.SettingsScreen;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 03.06.2015.
 */
public class OnScreenMenuButton implements Instrument {
    private final DrawCore draw;
    public boolean enabled = true;
    private boolean pressed = false;
    private boolean hovered = false;
    private boolean touched = false;
    private boolean moving = false;
    private final Point centerCoefficient = new Point(-1,-1);
    private final RectF menuButtonRectTmp = new RectF(-1,-1,-1,-1);
    private Path squircleCache = null; //squircle path calculation is complex operation, so its result is stored in cache
    public RectF menuButtonRect(){
        float buttonSize = Tools.dp(40);

        if(centerCoefficient.isEmpty()){
            float coefX;
            float coefY;
            if(draw.getWidth() < draw.getHeight()){ //portrait
                coefX = (Float)Data.get(Data.itemPositionXFloat(getName(), 0.9f));
                coefY = (Float)Data.get(Data.itemPositionYFloat(getName(), 0.95f));
            }
            else { //landscape
                coefX = (Float)Data.get(Data.itemPositionXFloat(getName(), 0.95f));
                coefY = (Float)Data.get(Data.itemPositionYFloat(getName(), 0.9f));
            }
            centerCoefficient.set(coefX, coefY);
        }

        float cx = draw.getWidth()*centerCoefficient.x;
        float cy = draw.getHeight()*centerCoefficient.y;
        float marginFromCenter = buttonSize/2f * 1.3f;

        if(cx > draw.getWidth()-marginFromCenter)
            cx=draw.getWidth()-marginFromCenter;
        if(cy > draw.getHeight()-marginFromCenter)
            cy=draw.getHeight()-marginFromCenter;
        if(cx < marginFromCenter)
            cx = marginFromCenter;
        if(cy < marginFromCenter)
            cy = marginFromCenter;

        float buttonBottom = cy + buttonSize/2f;
        float buttonTop = buttonBottom - buttonSize;
        float buttonRight = cx + buttonSize/2f;
        float buttonLeft = buttonRight - buttonSize;

        menuButtonRectTmp.set(buttonLeft, buttonTop, buttonRight, buttonBottom);

        return menuButtonRectTmp;
    }
    protected final RectF menuButtonRectTouchTmp = new RectF(-1,-1,-1,-1);

    public RectF menuButtonRectTouch(){
        RectF menuButtonRect = menuButtonRect();
        menuButtonRectTouchTmp.set(menuButtonRect);
        menuButtonRectTouchTmp.inset (-menuButtonRect.width()/3, -menuButtonRect.height()/3);
        return menuButtonRectTouchTmp;
    }
    Paint circlePaint = null;
    Paint dotsPaint = null;

    public OnScreenMenuButton(DrawCore draw) {
        this.draw = draw;
    }
    @Override    public String getName() {
        return "menuButton";
    }
    @Override    public String getVisibleName() {
        return "menuButton";
    }
    @Override    public int getImageResourceID() {
        return 0;
    }
    @Override    public void onSelect() {
        enabled = (Boolean) Data.get(Data.showScreenMenuButtonBoolean());
    }
    @Override    public void onDeselect() {

    }
    @Override    public boolean onTouch(MotionEvent event) {
        if(Data.tools == null)
            return false;
        if(!Data.tools.isAllowedDeviceForUi(event))
            return false;
        if(!enabled)
            return false;

        //Logger.log(event.toString());
        //menuButtonRectTouch.contains(event.getX(), event.getY())
        if(event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
            if (menuButtonRectTouch().contains(event.getX(), event.getY())) {
                hovered = true;
                draw.redraw(menuButtonRectTouch());
            }
        }
        else if(event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
            hovered = menuButtonRectTouch().contains(event.getX(), event.getY());
            draw.redraw(menuButtonRectTouch());
        }
        else if(event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
            hovered = false;
            draw.redraw(menuButtonRectTouch());
        }
        else if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if (menuButtonRectTouch().contains(event.getX(), event.getY())) {
                pressed = true;
                touched = true;
                draw.redraw(menuButtonRectTouch());
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        draw.uiHandler.post(() -> {
                            if(pressed){
                                try {
                                    Context context = draw.context;
                                    Tools.vibrate(draw.view);
                                    context.startActivity(new Intent(context, SettingsScreen.class));
                                }
                                catch (Throwable e) {
                                    Logger.log("Error in OnScreenMenuButton.down(...): " + Tools.getStackTrace(e));
                                    e.printStackTrace();
                                }
//                                finally {
//                                    //pressed = false;
//                                }
                            }
                        });
                    }
                }, ViewConfiguration.getLongPressTimeout());
                return true;
            }
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE) {
            if(pressed) {
                if (!menuButtonRectTouch().contains(event.getX(), event.getY())) {
                    pressed = false;
                    moving = true;
                }
                draw.redraw(menuButtonRectTouch());
                return true;
            }
            if(moving){
                float imageWidth = draw.getWidth();
                float imageHeight = draw.getHeight();

                float x = event.getX();
                float y = event.getY();

                //проверка правильности координат и их восстановление
                if(x >= imageWidth)
                    x = imageWidth-1;
                else if(x < 0)
                    x = 0;
                if(y >= imageHeight)
                    y = imageHeight-1;
                else if(y < 0)
                    y = 0;

                float coefX = x/imageWidth;
                float coefY = y/imageHeight;
                Data.save(coefX, Data.itemPositionXFloat(getName(), coefX));
                Data.save(coefY, Data.itemPositionYFloat(getName(), coefY));
                centerCoefficient.set(coefX, coefY);
                squircleCache = null;
                draw.redraw();
                return true;
            }
            return touched;
        }
        else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            touched = false;
            if(moving) {
                moving = false;
                return true;
            }
            if(pressed){
                pressed = false;
                if (menuButtonRectTouch().contains(event.getX(), event.getY())){
                    showMenu();
                }
                draw.redraw(menuButtonRectTouch());
                return true;
            }
        }
        return false;
    }
    @Override    public boolean onKey(KeyEvent event) {
        return false;
    }
    @Override    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        //if(showOnScreenMenuKey && (!draw.fingersOnScreen() || moving || pressed))    //uncomment this to hide menu while drawing
        if(enabled)
            drawMenuButton(canvas);
    }
    @Override    public boolean isActive() {
        return false;
    }
    @Override  public boolean isVisibleToUser() {
        return false;
    }
    @Override    public View.OnClickListener getOnClickListener() {
        return null;
    }

    void drawMenuButton(Canvas canvas){
        if(circlePaint == null){
            circlePaint = new Paint();
            circlePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setStrokeWidth(Data.store().DPI/200);
        }

        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor(Color.argb(60, 0, 0, 0));
        if(Data.isSqircle()) {  //1 out of 3
            if(squircleCache == null)
                squircleCache = Tools.getSquircleCenterPath(menuButtonRect().centerX(), menuButtonRect().centerY(), menuButtonRect().width() / 2f);
            canvas.drawPath(squircleCache, circlePaint);
        }
        else if (Data.isCircle())
            canvas.drawOval(menuButtonRect(), circlePaint);
        else if(Data.isRect())
            canvas.drawRect(menuButtonRect(), circlePaint);


        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(Color.argb((pressed?255: hovered?120:60), 255, 255, 255));
        if(Data.isSqircle())  //1 out of 3
            canvas.drawPath(squircleCache, circlePaint);
        else if (Data.isCircle())
            canvas.drawOval(menuButtonRect(), circlePaint);
        else if(Data.isRect())
            canvas.drawRect(menuButtonRect(), circlePaint);

        float totalRadius = (menuButtonRect().width())/2f;
        float dotsDistance = totalRadius * 0.4f;
        float dotsSize = totalRadius * (pressed?0.150f:0.125f);
        float cx1 = menuButtonRect().centerX();
        float cx2 = cx1 + dotsDistance;
        float cx3 = cx1 - dotsDistance;
        float cy = menuButtonRect().centerY();

        if(dotsPaint == null){
            dotsPaint = new Paint();
            dotsPaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            dotsPaint.setStyle(Paint.Style.FILL);
        }
        dotsPaint.setColor(Color.argb((pressed ? 255 : hovered ? 150 : 120), 0, 0, 0));
        canvas.drawCircle(cx1, cy, dotsSize, dotsPaint);
        canvas.drawCircle(cx2, cy, dotsSize, dotsPaint);
        canvas.drawCircle(cx3, cy, dotsSize, dotsPaint);
    }
    public boolean isEnabled(){
        return enabled;
    }
    void showMenu(){
        //�������� ����
        draw.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Tools.vibrate(draw.view);
                    draw.playSoundEffect(SoundEffectConstants.CLICK);
                    draw.context.openMainMenu();
                } catch (Exception e) {
                    Logger.log("Где-то в OnScreenMenuButton.showMenu произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                }
            }
        });
    }


}
