package com.fsoft.FP_sDraw.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.instruments.Filler;
import com.fsoft.FP_sDraw.instruments.Instrument;

/**
 * the view is designed to be shown in main menu and change brush size
 * Created by Dr. Failov on 08.01.2016.
 */
public class BrushSizeView extends View implements Instrument{
    DrawCore draw;
    Paint paint = new Paint();
    boolean pressed = false;
    float initialX = -1;
    float initialSize = -1;
    //fill threshold demo
    Bitmap baseSample = null;
    Bitmap actualSample = null;
    int actualSampleThreshold = -1;
    int actualSampleColor = -1;


    public BrushSizeView(Context context, DrawCore drawCore) {
        super(context);
        draw = drawCore;
        draw.addProvider(this);
    }

    public BrushSizeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!Data.tools.isAllowedDeviceForUi(event))
            return false;

        float x = event.getX();
        int action = event.getAction();
        if(action == MotionEvent.ACTION_DOWN) {
            pressed = true;
            initialX = x;
            if(draw.erase.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.erase))
                initialSize = (Integer) Data.get(Data.eraserSizeInt());
            else if(draw.mosaic.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.mosaic))
                initialSize = (Integer) Data.get(Data.mosaicSizeInt());
            else if(draw.fill.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.fill))
                initialSize = (Integer) Data.get(Data.fillThresholdInt());
            else
                initialSize = (Integer) Data.get(Data.brushSizeInt());
            invalidate();
            return true;
        }
        if(action == MotionEvent.ACTION_MOVE){
            float dx = x - initialX;
            float dSize = dx/10;
            if(initialSize < 10)
                dSize = dx/30;
            int newSize = (int)(initialSize+dSize);
            int max = (int)Data.store().DPI / 4;
            if(draw.erase.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.erase))
                max = (int)Data.store().DPI / 3;
            if(draw.mosaic.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.mosaic))
                max = (int)Data.store().DPI / 3;
            if(draw.fill.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.fill))
                max = 512;
            int min = 1;
            if(draw.mosaic.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.mosaic))
                min = 10;
            if(newSize < min)
                newSize = min;
            if(newSize > max)
                newSize = max;

            if(draw.erase.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.erase))
                Data.save(newSize, Data.eraserSizeInt());
            else if(draw.mosaic.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.mosaic))
                Data.save(newSize, Data.mosaicSizeInt());
            else if(draw.fill.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.fill))
                Data.save(newSize, Data.fillThresholdInt());
            else
                Data.save(newSize, Data.brushSizeInt());

            invalidate();
            return true;
        }
        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            pressed = false;
            invalidate();
            draw.refresh();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (pressed)
                canvas.drawColor(Color.argb(20, 255, 255, 255));
            float width = getWidth();
            float height = getHeight();
            float DPI = Data.store().DPI;
            int showSize;

            if (draw.erase.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.erase)) {  //erase CIRCLE
                showSize = (Integer) Data.get(Data.eraserSizeInt());
                float circleX = width / 10f;
                float circleY = height * 5f / 10f;
                float circleRadius = showSize / 2f;
                int color = Color.WHITE;

                paint.setColor(color);
                paint.setAntiAlias((Build.VERSION.SDK_INT > 14));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);

                canvas.drawCircle(circleX, circleY, circleRadius, paint);
            } else if (draw.mosaic.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.mosaic)) {  //Mosaic SQUARE
                showSize = (Integer) Data.get(Data.mosaicSizeInt());
                float circleX = width / 10f;
                float circleY = height * 5f / 10f;
                //float circleRadius = showSize / 2f;
                int color = Color.WHITE;

                paint.setColor(color);
                paint.setAntiAlias((Build.VERSION.SDK_INT > 14));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);

                canvas.drawRect(circleX - showSize / 2f, circleY - showSize / 2f, circleX + showSize / 2f, circleY + showSize / 2f, paint);
            } else if (draw.fill.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.fill)) {  //Fill THRESHOLD
                showSize = (Integer) Data.get(Data.fillThresholdInt());
                float circleX = width / 10f;
                float circleY = height * 5f / 10f;
                float bitmapOptimalHeight = getHeight() * 0.8f;
                int color = (Integer) Data.get(Data.brushColorInt());
                if (baseSample == null || baseSample.getHeight() != bitmapOptimalHeight) {
                    baseSample = Tools.decodeResource(getResources(), R.drawable.ic_fire, bitmapOptimalHeight * 2f, bitmapOptimalHeight);
                    actualSample = baseSample.copy(Bitmap.Config.ARGB_8888, true);

                    actualSampleThreshold = -1;
                }
                if(showSize != actualSampleThreshold || color != actualSampleColor){
                    actualSample.eraseColor(Color.TRANSPARENT);
                    Canvas c = new Canvas(actualSample);
                    c.drawBitmap(baseSample, 0,0, paint);
                    int cx = (int)(actualSample.getWidth()*0.68f);
                    int cy = (int)(actualSample.getHeight()*0.5f);
                    int antialiasing = 0;
                    if((Boolean) Data.get(Data.antialiasingBoolean()))
                        antialiasing = 1;

                    Filler.fillNative(actualSample, cx, cy, showSize, antialiasing, color, 0);
                    actualSampleThreshold = showSize;
                    actualSampleColor = color;
                }


                canvas.drawBitmap(actualSample, circleX - baseSample.getWidth() / 2f, circleY - baseSample.getHeight() / 2f, paint);
                //canvas.drawRect(circleX-showSize/2f, circleY-showSize/2f, circleX+showSize/2f, circleY+showSize/2f, paint);
            } else {  //BRUSH CIRCLE
                showSize = (Integer) Data.get(Data.brushSizeInt());
                float circleX = width / 10f;
                float circleY = height * 5f / 10f;
                float circleRadius = showSize / 2f;
                //boolean antialiasing = (Boolean) Data.get(Data.antialiasingBoolean());
                int color = (Integer) Data.get(Data.brushColorInt());

                paint.setColor(color);
                paint.setAntiAlias((Build.VERSION.SDK_INT > 14));

                canvas.drawCircle(circleX, circleY, circleRadius, paint);
            }

            //TEXT SIZE
            {
                String text = String.valueOf(showSize);
                float textSize = DPI / 10;
                paint.setTextSize(textSize);
                paint.setStyle(Paint.Style.FILL);
                float textWidth = paint.measureText(text);
                float textCenterX = width * 2.5f / 10f;
                float textCenterY = height * 5f / 10f;
                float textX = textCenterX - textWidth / 2;
                float textY = textCenterY + textSize / 3;
                int textColor = Color.WHITE;

                paint.setColor(textColor);
                paint.setAntiAlias((Build.VERSION.SDK_INT > 14));

                canvas.drawText(text, textX, textY, paint);
            }

            //TEXT BRUSH SIZE
            if (!pressed) {
                String text;

                if (draw.erase.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.erase))
                    text = Data.tools.getResource(R.string.settingsErasersize);
                else if (draw.mosaic.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.mosaic))
                    text = getContext().getString(R.string.mosaic_size);
                else if (draw.fill.isActive() || (draw.accurate.isActive() && draw.accurate.getCurrentInstrument() == draw.fill))
                    text = getContext().getString(R.string.fill_threshold);
                else
                    text = Data.tools.getResource(R.string.settingsBrushsize);

                float textSize = DPI / 9;
                paint.setTextSize(textSize);
                float textWidth = paint.measureText(text);
                float textCenterX = width * 6f / 10f;
                float textCenterY = height * 5f / 10f;
                float textX = textCenterX - textWidth / 2;
                float textY = textCenterY + textSize / 3;
                int textColor = Color.WHITE;

                paint.setColor(textColor);
                paint.setAntiAlias((Build.VERSION.SDK_INT > 14));

                canvas.drawText(text, textX, textY, paint);
            }

            //TEXT +
            if (pressed) {
                String text = "+";
                float textSize = DPI / 5;
                paint.setTextSize(textSize);
                float textWidth = paint.measureText(text);
                float textCenterX = width * 9f / 10f;
                float textCenterY = height * 5f / 10f;
                float textX = textCenterX - textWidth / 2;
                float textY = textCenterY + textSize / 3;
                int textColor = Color.WHITE;

                paint.setColor(textColor);
                paint.setAntiAlias(true);

                canvas.drawText(text, textX, textY, paint);
            }

            //TEXT -
            if (pressed) {
                String text = "-";
                float textSize = DPI / 3;
                paint.setTextSize(textSize);
                float textWidth = paint.measureText(text);
                float textCenterX = width * 4f / 10f;
                float textCenterY = height * 5f / 10f;
                float textX = textCenterX - textWidth / 2;
                float textY = textCenterY + textSize / 3;
                int textColor = Color.WHITE;

                paint.setColor(textColor);
                paint.setAntiAlias(true);

                canvas.drawText(text, textX, textY, paint);
            }
        }
        catch (Throwable e){
            Logger.log(e);
        }
    }


    //====================== instrument functions ===========================
    //нужны они тут для того, чтобы обрабатывать события с программы
    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getVisibleName() {
        return null;
    }

    @Override
    public int getImageResourceID() {
        return 0;
    }

    @Override
    public void onSelect() {
        //изменен инструмент
        invalidate();
    }

    @Override
    public void onDeselect() {

    }

    @Override
    public boolean onTouch(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override  public boolean isVisibleToUser() {
        return false;
    }

    @Override
    public OnClickListener getOnClickListener() {
        return null;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {

    }
}
