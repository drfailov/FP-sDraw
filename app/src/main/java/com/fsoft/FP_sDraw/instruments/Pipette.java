package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.HintMenu;
import com.fsoft.FP_sDraw.menu.MainMenu;

/**
 * Created by Dr. Failov on 17.01.2015.
 * Upgraded by Dr. Failov on 11.02.2023.
 */
public class Pipette implements Instrument{
    private final DrawCore draw;
    private final RectF pipette_frameRect = new RectF();
    private final Paint pipette_framePaint = new Paint();
    private int temporaryColor;
    private HintMenu hint = null;

    public Pipette(DrawCore d){
        draw = d;
    }

    @Override
    public String getName() {
        return "pipette";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentPipette);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.menu_pipette;
    }

    @Override
    public void onSelect() {
        temporaryColor = (Integer)Data.get(Data.brushColorInt());
    }

    @Override
    public void onDeselect() {

    }

    @Override
    public boolean onTouch(MotionEvent event) {
        //trigger zoom if over 2 fingers
        if(event.getPointerCount() > 1){// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
            if(Data.tools.isFullVersion()) {
                draw.setInstrument(draw.scale);
                draw.scale.onTouch(event);
                draw.scale.setInstrumentCallback(draw.pipette);
            }
            else
                Data.tools.showBuyFullDialog();
            return true;
        }

        if(hint != null && hint.processTouch(event))
            return true;

        //move canvas if touched by finger but activated  sPen mode
        if(!Data.tools.isAllowedDevice(event)) {
            if(Data.tools.isFullVersion()) {
                draw.setInstrument(draw.scale);
                draw.scale.onTouch(event);
                draw.scale.setInstrumentCallback(draw.pipette);
            }
            return true;
        }

        event = draw.scale.scale_transformMotionEvent(event);

        try{
            if(event.getX() < draw.bitmap.getWidth() && event.getY() < draw.bitmap.getHeight() && event.getX() > 0 && event.getY() > 0)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    int x=(int)event.getX();
                    int y=(int)event.getY();
                    int color = draw.bitmap.getPixel(x, y);
                    temporaryColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    Data.save(temporaryColor, Data.brushColorInt());
                    draw.refresh();
                }
                draw.redraw(0, (int)pipette_frameRect.bottom * 2, 0, (int)pipette_frameRect.right * 2);
            }
        } catch (Exception e){
            Logger.log("Draw.OnDraw.handlerPipette", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Draw.OnDraw.handlerPipette", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        int brushColor = temporaryColor;
        boolean isScaled = draw.scale.scale_size != 1f;

        //draw frame
        float marginTop = Tools.dp(5);
        float marginLeft = Tools.dp(10);
        // Add offset for rounded corners displays
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            WindowInsets insets = draw.view.getRootWindowInsets();
            final RoundedCorner topLeft = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT);
            if (topLeft != null) {
                marginLeft = topLeft.getCenter().x;
                marginLeft *= 0.80f;
            }
        }
        if(isScaled)
            marginTop += Tools.dp(40);
        float frameWidth = Tools.dp(110);
        float frameHeight = Tools.dp(35);
        float frameRoundness = Tools.dp(5);
        pipette_frameRect.set(marginLeft, marginTop, marginLeft+frameWidth, marginTop+frameHeight);
        pipette_framePaint.setAntiAlias(true);
        pipette_framePaint.setColor(MainMenu.transparentBackgroundColor);
        pipette_framePaint.setStyle(Paint.Style.FILL);
        if(Data.isRect())
            canvas.drawRect(pipette_frameRect, pipette_framePaint);
        else
            canvas.drawRoundRect(pipette_frameRect, frameRoundness, frameRoundness, pipette_framePaint);
        pipette_framePaint.setColor(Color.argb(70, 255,255,255));
        pipette_framePaint.setStyle(Paint.Style.STROKE);
        pipette_framePaint.setStrokeWidth(Data.store().DPI * 0.002f);
        if(Data.isRect())
            canvas.drawRect(pipette_frameRect, pipette_framePaint);
        else
            canvas.drawRoundRect(pipette_frameRect, frameRoundness, frameRoundness, pipette_framePaint);


        //DRAW IMAGE
        float iconWidth = Tools.dp(20);
        float iconHeight = Tools.dp(20);
        float iconPadding = (frameHeight-iconHeight)/2;
        pipette_framePaint.setAlpha(255);
        pipette_framePaint.setStyle(Paint.Style.FILL);
        pipette_framePaint.setColor(brushColor);
        if(Data.isSqircle()) //1 of 3
            canvas.drawPath(Tools.getSquircleCenterPath(marginLeft + iconPadding + iconWidth/2f, marginTop + iconPadding + iconWidth/2f, iconWidth/2f), pipette_framePaint);
        else if(Data.isCircle())
            canvas.drawCircle(marginLeft + iconPadding + iconWidth/2f, marginTop + iconPadding + iconWidth/2f, iconWidth/2f, pipette_framePaint);
        else if(Data.isRect())
            canvas.drawPath(Tools.getRectCenterPath(marginLeft + iconPadding + iconWidth/2f, marginTop + iconPadding + iconWidth/2f, iconWidth/2f), pipette_framePaint);
        pipette_framePaint.setStrokeWidth(Data.store().DPI * 0.002f);
        pipette_framePaint.setColor(Color.argb(70, 255,255,255));
        pipette_framePaint.setStyle(Paint.Style.STROKE);
        if(Data.isSqircle())//1 of 3
            canvas.drawPath(Tools.getSquircleCenterPath(marginLeft + iconPadding + iconWidth/2f, marginTop + iconPadding + iconWidth/2f, iconWidth/2f), pipette_framePaint);
        else if(Data.isCircle())
            canvas.drawCircle(marginLeft + iconPadding + iconWidth/2f, marginTop + iconPadding + iconWidth/2f, iconWidth/2f, pipette_framePaint);
        else if(Data.isRect())
            canvas.drawPath(Tools.getRectCenterPath(marginLeft + iconPadding + iconWidth/2f, marginTop + iconPadding + iconWidth/2f, iconWidth/2f), pipette_framePaint);

        //DRAW TEXT
        float scale_text_size = Tools.sp(15);
        String text = getCode(brushColor);
        pipette_framePaint.setTextSize(scale_text_size);
        pipette_framePaint.setAntiAlias(true);
        pipette_framePaint.setColor(Color.WHITE);
        pipette_framePaint.setStyle(Paint.Style.FILL);
        float textWidth = pipette_framePaint.measureText(text);
        float textHeight = pipette_framePaint.getTextSize();
        float textX = pipette_frameRect.right - iconPadding - textWidth;
        float textY = pipette_frameRect.top + textHeight*0.8f + (pipette_frameRect.height()-textHeight)/2;
        canvas.drawText(text, textX, textY, pipette_framePaint);

        if(drawUi) {
            if (hint == null)
                hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_pipette), R.drawable.ic_help, "PIPETTE", HintMenu.SHOW_TIMES);
            hint.draw(canvas);
        }
    }
    public String getCode(int color){
        return String.format("#%06X", (0xFFFFFF & color));
    }


    @Override
    public boolean isActive() {
        if(draw == null)
            return false;
        if(draw.currentInstrument == this)
            return true;
        return draw.currentInstrument == draw.scale && draw.scale != null && draw.scale.instrumentCallback == this;
    }

    @Override  public boolean isVisibleToUser() {
        return true;
    }

    @Override
    public View.OnClickListener getOnClickListener() {
        return view -> draw.setInstrument(Pipette.this);
    }
}
