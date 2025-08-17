package com.fsoft.FP_sDraw.menu;

import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterPath;
import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterSum;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.MainMenu;

import java.util.Timer;
import java.util.TimerTask;

/*Button used on selection move mode
* 2023-01-11
* */
/*Usage:

        buttonsForSelecting = new CircleButtonNextToMenuButton[]{
                new CircleButtonNextToMenuButton(draw, 1.2f, Data.store().DPI / 7f, R.drawable.ic_cursor, getSelectPreciseRunnable())
        };
...

        for (CircleButtonNextToMenuButton button:buttonsForSelecting){
            if(button.isVisible() && button.onTouch(event)) {
                draw.redraw();
                return true;
            }
        }

 ...

        for (CircleButtonNextToMenuButton button:buttonsForSelecting)
            if(button.isVisible())
                button.draw(canvas);
* */
public class CircleButtonNextToMenuButton {
    private final DrawCore draw;
    private final float buttonNumber;
    private final float touchRadius;
    private Integer image;
    private final Runnable action;
    private Timer longClickActionTimer = null;
    private Runnable longClickAction = null;
    public final RectF buttonRect = new RectF();

    private final Paint circlePaint = new Paint();
    private final Paint imagePaint = new Paint();
    private Thread loadingThread = null;
    private Bitmap bitmap = null;
    private boolean visible = true;
    private boolean pressed = false;
    private int touchId = -1;
    private boolean hovered = false;
    Path squircleCache = null;
    float squircleCacheSum = 0;

    public CircleButtonNextToMenuButton(DrawCore draw, float buttonNumber, float touchRadius, Integer image, Runnable action) {
        this.draw = draw;
        this.buttonNumber = buttonNumber;
        this.touchRadius = touchRadius;
        this.image = image;
        this.action = action;
    }
    public float getButtonRadius(){
        return touchRadius*0.87f;
    }

    private Point getButtonPosition(float buttonNumber, float buttonInterval){
        Point result = new Point(draw.getWidth(), draw.getHeight() - touchRadius * 1.2f);
        if(draw.onScreenMenuButton != null && draw.onScreenMenuButton.isEnabled()) {
            result.x = draw.onScreenMenuButton.menuButtonRect().centerX();
            result.y = draw.onScreenMenuButton.menuButtonRect().centerY();
        }
        result.x -= buttonInterval*buttonNumber;
        return result;
    }

    private void loadImage() {
        if (bitmap == null && loadingThread == null && image != null && image != 0) {
            loadingThread = new Thread(() -> {
                loadImageAsync();
                loadingThread = null;
            });
            loadingThread.setName("loadingThread for CircleWithImage");
            loadingThread.start();
        }
    }

    private void loadImageAsync() {
        float sc = 0.9f;
        bitmap = Tools.decodeResource(draw.context.getResources(), image, (int) (touchRadius * sc), (int) (touchRadius * sc));
        draw.redraw();
    }

    public void draw(Canvas canvas) {
        try {
            Point center = getButtonPosition(buttonNumber, touchRadius*2);
            buttonRect.set(center.x-getButtonRadius(), center.y-getButtonRadius(), center.x+getButtonRadius(), center.y+getButtonRadius());
            circlePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(touchRadius*0.015f);
            circlePaint.setColor(Color.argb(100, 255, 255, 255));
            if(Data.isSqircle()) { //1 out of 3
                if(squircleCache == null || squircleCacheSum != getSquircleCenterSum(center.x, center.y, getButtonRadius())) {
                    squircleCache = getSquircleCenterPath(center.x, center.y, getButtonRadius());
                    squircleCacheSum = getSquircleCenterSum(center.x, center.y, getButtonRadius());
                }
                canvas.drawPath(squircleCache, circlePaint);
            }
            else if(Data.isCircle())
                canvas.drawCircle(center.x, center.y, getButtonRadius(), circlePaint); //DPI/8
            else if(Data.isRect())
                canvas.drawPath(Tools.getRectCenterPath(center.x, center.y, getButtonRadius()), circlePaint);

            if (pressed)
                circlePaint.setColor(Color.argb(250, 0, 0, 0));
            else if (hovered)
                circlePaint.setColor(Color.argb(170, 0, 0, 0));
            else
                circlePaint.setColor(MainMenu.transparentBackgroundColor);
            circlePaint.setStyle(Paint.Style.FILL);
            if(Data.isSqircle()) //1 out of 3
                canvas.drawPath(squircleCache, circlePaint);
            else if(Data.isCircle())
                canvas.drawCircle(center.x, center.y, getButtonRadius(), circlePaint); //DPI/8
            else if(Data.isRect())
                canvas.drawPath(Tools.getRectCenterPath(center.x, center.y, getButtonRadius()), circlePaint);


            //canvas.drawCircle(center.x, center.y, touchRadius*0.87f, circlePaint); //DPI/8

            if (bitmap == null) {                      //------------bitmap
                loadImage();
            } else {
                float imageX = center.x - bitmap.getWidth() / 2f;
                float imageY = center.y - bitmap.getHeight() / 2f;
                canvas.drawBitmap(bitmap, imageX, imageY, imagePaint);
            }

            if(longClickAction != null){
                circlePaint.setTextSize(Tools.sp(10));
                circlePaint.setStyle(Paint.Style.FILL);
                circlePaint.setColor(Color.argb(100, 255, 255, 255));
                float w = circlePaint.measureText("...");
                canvas.drawText("...", center.x - w/2f, center.y + getButtonRadius()*0.82f, circlePaint);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Logger.log("CircleButton.draw", "Ошибка обработки отрисовки: " + e.getMessage(), false);
        }
    }

    public boolean onTouch(MotionEvent event){
        try {

            if(!Data.tools.isAllowedDeviceForUi(event))
                return false;
            Point center = getButtonPosition(buttonNumber, touchRadius * 2);
            if (event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                hovered = (center.distanceTo(event) < touchRadius);
            }
            if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                hovered = false;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                Point point = new Point(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
                if (center.distanceTo(point) < touchRadius) {
                    touchId = event.getPointerId(event.getActionIndex());
                    pressed = true;
                    if (longClickAction != null) {
                        longClickActionTimer = new Timer();
                        longClickActionTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (longClickAction != null) {
                                    try {
                                        pressed = false;
                                        Tools.vibrate(draw.view);
                                        longClickAction.run();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }, ViewConfiguration.getLongPressTimeout());
                    }
                    return true;
                }
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if ((longClickActionTimer != null || pressed) && event.getPointerCount() == 1) {
                    return true;
                }
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                if (longClickActionTimer != null) {
                    longClickActionTimer.cancel();
                    longClickActionTimer = null;
                }
                if (event.getPointerId(event.getActionIndex()) == touchId) {
                    Point point = new Point(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
                    touchId = -1;
                    if (pressed) {
                        pressed = false;
                        if (center.distanceTo(point) < touchRadius) {
                            Tools.vibrate(draw.view);
                            if (action != null) {
                                action.run();
                            }
                        }
                    }
                    return true;
                }
            }
        }
        catch (Exception e){
            Logger.log(e);
        }
        return false;
    }

    //pass null ti disable
    public void setLongClickAction(Runnable longClickAction) {
        this.longClickAction = longClickAction;
    }

    public void changeImage(int image){
        if(image != this.image) {
            this.image = image;
            bitmap = null;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
