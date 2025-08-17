package com.fsoft.FP_sDraw.instruments;

import android.graphics.*;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

/**
 * Created by Dr. Failov on 17.01.2015.
 */
public class Main implements Instrument {
    DrawCore draw;
    Paint paint;
    boolean grid = false;
    boolean watermark = false;
    boolean volumeKeys = true;
    boolean volumeDownPressed = false;
    boolean lastPenButton = false;
    Instrument lastInstrument = null;
    Instrument tmpLastInstrument = null;//used for backing up to previous instrument when volume buttons doing something


    float curSizeMax = 0;
    float curSizeMin = 0;
    float curPressureMax = 0;
    float curPressureMin = 0;
    boolean statsModified = false;

    int backgroundColor = 0;



    public Main(DrawCore _draw){
        draw = _draw;
        paint = new Paint();
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);
        paint.setAlpha(0xFF);
    }
    @Override public String getName() {
        return "system";
    }
    @Override public String getVisibleName() {
        return "system";
    }
    @Override public int getImageResourceID() {
        return R.mipmap.ic_launcher;
    }
    @Override public void onSelect() {
        grid = (Integer) Data.get(Data.gridSizeInt()) > 1;
        watermark = (Boolean)Data.get(Data.watermarkBoolean());
        volumeKeys = (Boolean)Data.get(Data.volumeButtonsBoolean());
        backgroundColor = (Integer) Data.get(Data.backgroundColorInt());
        watermarkColour = Data.tools.getGridColor((Integer)Data.get(Data.backgroundColorInt()), .05f);
    }
    @Override public void onDeselect() {
        draw.saver.autoSave();
    }
    @Override public boolean onTouch(MotionEvent event) {
        //Logger.log(event.toString());
        if(Build.VERSION.SDK_INT >= 14) {
            if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY   //кнопка  пера на Galaxy Note 3
                    || event.getButtonState() == MotionEvent.BUTTON_STYLUS_PRIMARY    //Кнопка пера на Galaxy Tab S4
                    || event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {   //обратная сторона пера на MediaPad M5 Pro
                if (!lastPenButton) {
                    Logger.log("Gesture.onTouch", "Set eraser on...", false);
                    lastInstrument = draw.currentInstrument;
                    draw.setInstrument(draw.erase);
                    lastPenButton = true;
                }
            }
            else {
                if (lastPenButton) {
                    Logger.log("Gesture.onTouch", "Set eraser off...", false);
                    draw.setInstrument(lastInstrument);
                    lastPenButton = false;
                }
            }
        }

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            int toolType = 1;
            if(Build.VERSION.SDK_INT >= 14)
                toolType = event.getToolType(0);

            curSizeMax = (Float)Data.get(Data.sizeMaxFloat(toolType));
            curSizeMin = (Float)Data.get(Data.sizeMinFloat(toolType));
            curPressureMax = (Float)Data.get(Data.pressureMaxFloat(toolType));
            curPressureMin = (Float)Data.get(Data.pressureMinFloat(toolType));
            statsModified = false;
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE) {
            float size = event.getSize();
            float pressure = event.getPressure();

            if(curPressureMax == -1) {
                curSizeMax = curSizeMin = size;
                curPressureMax = curPressureMin = pressure;
            }
            else{
                if(size > curSizeMax){
                    curSizeMax = size;
                    statsModified = true;
                }
                if(size < curSizeMin){
                    curSizeMin = size;
                    statsModified = true;
                }
                if(pressure > curPressureMax) {
                    curPressureMax = pressure;
                    statsModified = true;
                }
                if(pressure < curPressureMin) {
                    curPressureMin = pressure;
                    statsModified = true;
                }
            }
        }
        else if(statsModified && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)){
            int toolType = 1;
            if(Build.VERSION.SDK_INT >= 14)
                toolType = event.getToolType(0);

            Data.save(curPressureMax, Data.pressureMaxFloat(toolType));
            Data.save(curPressureMin, Data.pressureMinFloat(toolType));
            Data.save(curSizeMax, Data.sizeMaxFloat(toolType));
            Data.save(curSizeMin, Data.sizeMinFloat(toolType));
        }
        return false;
    }
    @Override public boolean onKey(KeyEvent event) {
        //Logger.log("event = " + event.toString());
        //Обработка клавиш громкости
        if(volumeKeys) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || (event.getScanCode() == 106 && Build.BRAND.equalsIgnoreCase("sony"))) { //FUCKING SONY!
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(draw.currentInstrument != draw.erase && draw.currentInstrument != draw.scale) {
                        if (tmpLastInstrument == null)
                            tmpLastInstrument = draw.currentInstrument;
                        draw.setInstrument(draw.erase);
                    }
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (tmpLastInstrument != null) {
                        draw.setInstrument(tmpLastInstrument);
                        tmpLastInstrument = null;
                    }
                }
                return true;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || (event.getScanCode() == 105 && Build.BRAND.equalsIgnoreCase("sony"))) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && !volumeDownPressed) {
                    volumeDownPressed = true;
                    if (tmpLastInstrument == null)
                        tmpLastInstrument = draw.currentInstrument;
                    draw.setInstrument(draw.gesture);
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    volumeDownPressed = false;
                    if (tmpLastInstrument != null) {
                        draw.setInstrument(tmpLastInstrument);
                        tmpLastInstrument = null;
                    }
                }
                return true;
            }
        }
        return false;
    }
    public void drawTransparent(Canvas canvas) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (draw.bitmap)
        {
            canvas.drawBitmap(draw.bitmap, 0,0, paint);
        }
    }
    //Draws content of IMAGE. INCLUDING WHEN SAVING!
    @Override public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        canvas.drawColor(backgroundColor);
        if(watermark)
            addWatermark(canvas, "sDraw");
        if(grid)
            drawGrid(canvas);
        draw.scale.scale_matrix.setScale(draw.scale.scale_size, draw.scale.scale_size);
        draw.scale.scale_matrix.postTranslate(draw.scale.scale_offset_x, draw.scale.scale_offset_y);
        //noinspection SynchronizeOnNonFinalField
        synchronized (draw.bitmap)
        {
            canvas.drawBitmap(draw.bitmap, draw.scale.scale_matrix, paint);
        }
        if(draw.scale.scale_size != 1.0f || draw.currentInstrument == draw.scale)
            draw.scale.drawScalePercentage(canvas);

    }
    @Override public boolean isActive() {
        return false;
    }
    @Override  public boolean isVisibleToUser() {
        return false;
    }
    @Override public View.OnClickListener getOnClickListener() {
        return null;
    }


    Paint watermarkPaint = null;
    int watermarkColour = 0;
    public void addWatermark(Canvas canvas, String text){
        if(watermarkPaint == null) {
            watermarkPaint = new Paint();
            watermarkPaint.setTextSize(Data.store().DPI / 8);
            watermarkPaint.setAntiAlias(false);
            watermarkPaint.setStyle(Paint.Style.FILL);
        }
        watermarkPaint.setColor(watermarkColour);
        float _x = draw.bitmap.getWidth() - watermarkPaint.measureText(text) - Tools.dp(5);
        float _y = draw.bitmap.getHeight() - Tools.dp(5);
        canvas.drawText(text, _x, _y, watermarkPaint);
    }
    public void drawGrid(Canvas canvas){
        float gridSize = (Integer)Data.get(Data.gridSizeInt());
        boolean gridVertical = (Boolean)Data.get(Data.gridVerticalBoolean());
        int gridColor = Data.tools.getGridColor();
        //draw grid
        if(gridSize>1) {
            paint.setColor(gridColor);
            paint.setStrokeWidth(1);
            gridSize *= draw.scale.scale_size;
            float offsetX = gridSize;
            offsetX += draw.scale.scale_offset_x%gridSize;
            float offsetY = gridSize;
            offsetY += draw.scale.scale_offset_y%gridSize;
            for(float c=offsetY; c<draw.bitmap.getHeight(); c+=gridSize)
                canvas.drawLine(0, c, draw.bitmap.getWidth(), c, paint);
            if(gridVertical)
                for(float c=offsetX; c<draw.bitmap.getWidth(); c+=gridSize)
                    canvas.drawLine(c, 0, c, draw.bitmap.getHeight(), paint);
        }
    }


}
