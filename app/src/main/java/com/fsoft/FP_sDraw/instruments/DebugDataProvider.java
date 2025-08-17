package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.common.UndoProvider;

import java.util.ArrayList;

/**
 * ����� ���������� ���������� ���������� ���������� �� ������ ���������� �� ���������� ��������
 * Created by Dr. Failov on 22.10.2015.
 * Edited by Dr. Failov on 28.10.2023.
 */


public class DebugDataProvider implements Instrument {
    private final DrawCore draw;
    private static final ArrayList<String> log = new ArrayList<>();

    private MotionEvent lastEvent = null;
    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint strokePaint = new Paint();
    private static boolean debugEnabled = false;
    private static Runnable onLog = null;

    public DebugDataProvider(DrawCore draw) {
        this.draw = draw;
    }

    @Override
    public String getName() {
        return "debugdata";
    }

    @Override
    public String getVisibleName() {
        return "Debug Data Provider";
    }

    @Override
    public int getImageResourceID() {
        return 0;
    }

    @Override
    public void onSelect() {
        debugEnabled = (Boolean) Data.get(Data.debugBoolean());
        if(debugEnabled)
            onLog = draw::redraw;

        textPaint.setTextSize(Tools.sp(7));
        strokePaint.setTextSize(Tools.sp(7));

        textPaint.setColor(Color.WHITE);
        strokePaint.setColor(Color.BLACK);
        strokePaint.setAlpha(180);

        textPaint.setAntiAlias(true);
        strokePaint.setAntiAlias(true);

        textPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);

        strokePaint.setStrokeWidth(1);
    }

    @Override
    public void onDeselect() {

    }

    @Override
    public boolean onTouch(MotionEvent event) {
        if(debugEnabled) {
            draw.redraw();
            lastEvent = event;
        }
        return false;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(draw == null)
            return;
        if(!debugEnabled)
            return;
        //draw log
        float logWindowBottom = canvas.getHeight() * 2f/5f;
        float logWindowPadding = Tools.dp(10);
        try{
            paint.setColor(Color.BLACK);
            paint.setAlpha(130);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, canvas.getWidth(), logWindowBottom, paint);

            float logTextSize = Tools.sp(7);
            paint.setTextSize(logTextSize);
            paint.setColor(Color.WHITE);
            paint.setAlpha(170);
            float y = logWindowBottom - logWindowPadding;
            int index = 0;
            while (y > 0 && index < log.size()){
                String text = log.get(index);
                if(text != null)
                    canvas.drawText(text, logWindowPadding, y, paint);
                y -= logTextSize*1.05f;
                index ++;
                if(y <= 0 && index < log.size()){
                    log.remove(index);
                }
            }
        }
        catch (Exception e){
            Logger.log("Error drawing log: " + e.getLocalizedMessage());
            Logger.log(Tools.getStackTrace(e));
        }

        //draw motionEvent data
        if(lastEvent != null) {
            for (int i = 0; i < lastEvent.getPointerCount(); i++) {
                int pointerId = lastEvent.getPointerId(i);
                String data = "";
                data += "action = " + lastEvent.getAction();
                data += ",id = " + pointerId;
                data += ",x = " + lastEvent.getX(i);
                data += ",y = " + lastEvent.getY(i);
                data += ",size = " + lastEvent.getSize(i);
                data += ",pressure = " + lastEvent.getPressure(i);
                if (Build.VERSION.SDK_INT >= 9)
                    if (lastEvent.getDevice() != null)
                        data += ",device = " + lastEvent.getDevice().getName();
                data += "," + lastEvent;

                String[] text = data.split(",");
                float widthMax = 0;
                for (String line : text)
                    widthMax = Math.max(widthMax, textPaint.measureText(line));

                float textX = lastEvent.getX(i) - (widthMax / 2);
                float textY = (lastEvent.getY(i) - (Data.store().DPI / 4) - ((textPaint.getTextSize()*1.05f) * text.length));
                for (int j = 0; j < text.length; j++) {
                    canvas.drawText(text[j], textX, textY + (j * (textPaint.getTextSize()*1.05f)), strokePaint);
                    canvas.drawText(text[j], textX, textY + (j * (textPaint.getTextSize()*1.05f)), textPaint);
                }
            }
        }

        //undo area calculator
        if(draw.undoAreaCalculator != null && draw.scale != null && !draw.undoAreaCalculator.isEmpty()){
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Tools.dp(1));
            paint.setColor(Color.WHITE);
            canvas.drawRect(
                    draw.scale.ImageToScreenX(draw.undoAreaCalculator.left),
                    draw.scale.ImageToScreenY(draw.undoAreaCalculator.top),
                    draw.scale.ImageToScreenX(draw.undoAreaCalculator.right),
                    draw.scale.ImageToScreenY(draw.undoAreaCalculator.bottom),
                    paint);
        }

        //undo memory
        if(draw.undoProvider != null){
            float blockWidth = Tools.dp(45);
            float blockHeight = Tools.dp(20);
            float blockMargin = Tools.dp(3);
            float blockCorner = Tools.dp(4);
            float blockTextSize = Tools.sp(7);
            float opacity = 100;
            float cx = logWindowPadding;
            float cy = logWindowBottom + logWindowPadding;
            int current = draw.undoProvider.getCurrentStepIndex();
            for (int i = 0; i < draw.undoProvider.getStepsCount() && cy < canvas.getHeight() * 3.2f/5f; i++) {
                UndoProvider.UndoStep undoStep = draw.undoProvider.getUndoStep(i);
                paint.setColor(Color.BLACK);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.FILL);
                paint.setAlpha((int)opacity);
                RectF rectF = new RectF(cx, cy, cx+blockWidth, cy+blockHeight);
                canvas.drawRoundRect(rectF, blockCorner, blockCorner, paint);

                if(i == current) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Tools.dp(1));
                    paint.setColor(Color.WHITE);
                    paint.setAlpha((int)(opacity*1.5f));
                    canvas.drawRoundRect(rectF, blockCorner, blockCorner, paint);
                }

                paint.setTextSize(blockTextSize);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setAlpha((int)(opacity*1.5));
                if(undoStep.image == null) {
                    canvas.drawText("NULL", cx + blockCorner, cy + blockCorner + blockTextSize * 0.7f, paint);
                }
                if(undoStep.image != null){
                    int w = undoStep.image.getWidth();
                    int h = undoStep.image.getHeight();
                    int px = w*h;
                    canvas.drawText(w + "x" + h, cx + blockCorner, cy + blockCorner + blockTextSize * 0.7f, paint);
                    canvas.drawText((px/1000)+"K px", cx + blockCorner, cy + blockCorner + blockTextSize * 1.7f, paint);
                }

                cx += blockWidth+blockMargin;
                if(cx+blockWidth > draw.getWidth()) {
                    cx = logWindowPadding;
                    cy += blockHeight + blockMargin;
                    opacity *= 0.8f;
                }
            }
            { //summary for undo provider
                paint.setColor(Color.BLACK);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.FILL);
                paint.setAlpha(100);
                if(cx != logWindowPadding) {
                    cx = logWindowPadding;
                    cy += blockHeight + blockMargin;
                }
                float summaryBlockWidth = Tools.dp(170);
                float summaryBlockHeight = Tools.dp(70);
                RectF rectF = new RectF(cx, cy, cx + summaryBlockWidth, cy + summaryBlockHeight);
                canvas.drawRoundRect(rectF, blockCorner, blockCorner, paint);

                paint.setTextSize(blockTextSize);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setAlpha(150);

                long tenCanvasesBytes = (long)canvas.getHeight() * (long)canvas.getWidth() * 4L * 10L;
                long usedBytes = draw.undoProvider.getMemoryUsageBytes();
                long availableBytes = draw.undoProvider.getMemoryLimitBytes();
                long percent = 100 * usedBytes / tenCanvasesBytes;
                canvas.drawText("Total "+ draw.undoProvider.getStepsCount() + " steps", cx + blockCorner, cy + blockCorner + blockTextSize * 0.7f, paint);
                canvas.drawText("getMemoryUsageBytes = "+ usedBytes/1000000 + "M.", cx + blockCorner, cy + blockCorner + blockTextSize * 1.7f, paint);
                canvas.drawText("getMemoryLimitBytes = "+ availableBytes/1000000 + "M.", cx + blockCorner, cy + blockCorner + blockTextSize * 2.7f, paint);
                canvas.drawText("Used "+percent + "% of ten canvases", cx + blockCorner, cy + blockCorner + blockTextSize * 3.7f, paint);
                Runtime runtime = Runtime.getRuntime();
                if(runtime == null) {
                    canvas.drawText("Total memory limit is unavailable", cx + blockCorner, cy + blockCorner + blockTextSize * 4.7f, paint);
                }
                else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        canvas.drawText("Total memory is " + runtime.totalMemory() / 1000000 + "M/" + runtime.maxMemory() / 1000000 + "M.", cx + blockCorner, cy + blockCorner + blockTextSize * 4.7f, paint);
                    } else {
                        canvas.drawText("Total (non-bitmap) memory is " + runtime.totalMemory() / 1000000 + "M/" + runtime.maxMemory() / 1000000 + "M.", cx + blockCorner, cy + blockCorner + blockTextSize * 4.7f, paint);
                    }
                }

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

    public static void addToLog(String text){
        if(!debugEnabled)
            return;
        log.add(0, text);
        if(onLog != null) {
            try {
                onLog.run();
            }
            catch (Exception e){
                Logger.log("Error running logger listener: " + e.getLocalizedMessage());
                Logger.log(Tools.getStackTrace(e));
            }
        }
    }
}
