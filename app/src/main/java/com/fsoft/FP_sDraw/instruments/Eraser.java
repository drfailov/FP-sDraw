package com.fsoft.FP_sDraw.instruments;

import android.graphics.*;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.AreaCalculator;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.HistoryProvider;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.HintMenu;

/**
 * Это тупо ластик
 * Created by Dr. Failov on 02.01.2015.
 */
public class Eraser implements Instrument {
    private final DrawCore draw;
    private final Paint paint = new Paint();
    private final Paint circlePaint = new Paint();
    private final AreaCalculator invalidateAreaCalculator =new AreaCalculator();
    HistoryProvider historyProvider;
    int eraserSize = 0;
    MotionEvent lastEvent = null;
    private HintMenu hint;

    public Eraser(DrawCore d){
        draw = d;
    }

    @Override
    public String getName() {
        return "eraser";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentEraser);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.menu_eraser;
    }

    @Override
    public void onSelect() {
        eraserSize = (Integer)Data.get(Data.eraserSizeInt());
        historyProvider = new HistoryProvider(2, DrawCore.MULTITOUCH_MAX);
        //load
        paint.setAntiAlias(false);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(eraserSize);
    }

    @Override
    public void onDeselect() {
        if(!draw.undoAreaCalculator.isEmpty()) {
            draw.undoAreaCalculator.check(draw.getWidth(), draw.getHeight());
            draw.undoProvider.apply(
                    draw.undoAreaCalculator.top,
                    draw.undoAreaCalculator.bottom,
                    draw.undoAreaCalculator.left,
                    draw.undoAreaCalculator.right
            );
            draw.undoProvider.prepare();
            draw.undoAreaCalculator.reset();
        }
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        try{
            //trigger zoom if over 2 fingers
            if(event.getPointerCount() > 1 && ((Boolean)Data.get(Data.twoFingersZoomBoolean()) || (Boolean)Data.get(Data.sPenOnlyBoolean()))){
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    lastEvent = null;
                    if(Data.tools.isAllowedDevice(event)) //if this pointer already draw something
                        draw.undoProvider.undo();
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.erase);
                }
                else
                    Data.tools.showBuyFullDialog();
                return true;
            }

            //process hint events
            if(hint != null && hint.processTouch(event)) {
                return true;
            }

            //move canvas if touched by finger but activated  sPen mode
            if(!Data.tools.isAllowedDevice(event)) {
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.erase);
                }
                return true;
            }

            lastEvent = event;
            event = draw.scale.scale_transformMotionEvent(event);
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(action == MotionEvent.ACTION_DOWN                                //-----------------------------------------ACTION_DOWN
                    || action == MotionEvent.ACTION_POINTER_DOWN
                    || action == 211){ //stylus DOWN with pressed button on Galaxy Tab S4
                invalidateAreaCalculator.reset();
                if(action == MotionEvent.ACTION_DOWN)
                    draw.undoAreaCalculator.reset();
                //erase
                for(int pointer=0;pointer<event.getPointerCount();pointer++){ //multitouch
                    draw.canvas.drawCircle(event.getX(pointer), event.getY(pointer), (float) eraserSize / 2, paint);
                    invalidateAreaCalculator.add((int) event.getX(pointer), (int) event.getY(pointer), eraserSize);
                    draw.undoAreaCalculator.add((int)event.getX(pointer), (int)event.getY(pointer), eraserSize);
                    draw.lastChangeToBitmap = System.currentTimeMillis();
                }
                historyProvider.write(event);
                //invalidate
                if(draw.scale.scale_size == 1.0f)
                    draw.redraw(invalidateAreaCalculator.top, invalidateAreaCalculator.bottom, invalidateAreaCalculator.left, invalidateAreaCalculator.right);
                else
                    draw.redraw();


            } else if(action == MotionEvent.ACTION_MOVE                     //-------------------------------------------------------------------------------ACTION_MOVE
                    || action == 213){//stylus MOVE with pressed button on Galaxy Tab S4
                invalidateAreaCalculator.reset();
                //erase
                for(int pointer=0; pointer<event.getPointerCount(); pointer++){ //multitouch
                    if(historyProvider.getX(event.getPointerId(pointer), 0) == -1) {
                        Logger.log("Внимание! Что-то пошло не так в Eraser. Операция ACTION_DOWN не была вызвана перед вызовом ACTION_MOVE и мы попробуем это исправить...");
                        for(int i=0; i<historyProvider.historySize; i++)
                            historyProvider.write(event);
                    }
                    draw.canvas.drawLine(historyProvider.getX(event.getPointerId(pointer),0), historyProvider.getY(event.getPointerId(pointer), 0), event.getX(pointer), event.getY(pointer), paint);
                    draw.canvas.drawCircle(event.getX(pointer), event.getY(pointer), (float) eraserSize / 2, paint);
                    invalidateAreaCalculator.add((int)event.getX(pointer), (int)event.getY(pointer), eraserSize);
                    invalidateAreaCalculator.add((int) historyProvider.getX(event.getPointerId(pointer), 0), (int) historyProvider.getY(event.getPointerId(pointer), 0), eraserSize);
                    draw.undoAreaCalculator.add((int)event.getX(pointer), (int)event.getY(pointer), eraserSize);
                    draw.lastChangeToBitmap = System.currentTimeMillis();
                }
                historyProvider.write(event);
                //invalidate
                if(draw.scale.scale_size == 1.0f)
                    draw.redraw(invalidateAreaCalculator.top, invalidateAreaCalculator.bottom, invalidateAreaCalculator.left, invalidateAreaCalculator.right);
                else
                    draw.redraw();

            }
            else if(event.getAction() == MotionEvent.ACTION_UP     //-------------------------------------------------------------------------ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL
                    || action == MotionEvent.ACTION_POINTER_UP
                    || action == 212){   //stylus UP with pressed button on Galaxy Tab S4
                if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    draw.undoAreaCalculator.add((int) event.getX(), (int) event.getY(), eraserSize);
                    draw.undoAreaCalculator.check(draw.bitmap.getWidth(), draw.bitmap.getHeight());
                    draw.undoProvider.apply(draw.undoAreaCalculator.top, draw.undoAreaCalculator.bottom, draw.undoAreaCalculator.left, draw.undoAreaCalculator.right);
                    draw.undoProvider.prepare();
                    draw.lastChangeToBitmap = System.currentTimeMillis();
                    draw.redraw();
                }
            }
            if(action == MotionEvent.ACTION_CANCEL){
                draw.undoProvider.undo();
            }
        } catch (Exception e){
            Logger.log("Draw.OnDraw.handlerErase", "Exception: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Draw.OnDraw.handlerErase", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;

    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        //213 is action code received on Galaxy Tab S4 with pressed stylus button
        if(lastEvent != null && (lastEvent.getAction() == MotionEvent.ACTION_MOVE || lastEvent.getAction() == 213)){
            circlePaint.setStyle(Paint.Style.STROKE);
            float size = (float)((Integer)Data.get(Data.eraserSizeInt())) * draw.scale.scale_size;
            for (int i = 0; i < lastEvent.getPointerCount(); i++) {
                circlePaint.setColor(Color.BLACK);
                circlePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                circlePaint.setStrokeWidth(3);
                canvas.drawCircle(lastEvent.getX(i), lastEvent.getY(i), size/2, circlePaint);
                circlePaint.setColor(Color.WHITE);
                circlePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                circlePaint.setStrokeWidth(1);
                canvas.drawCircle(lastEvent.getX(i), lastEvent.getY(i), size/2, circlePaint);
            }
        }
        if(drawUi){
            if(hint == null)
                hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_eraser), R.drawable.ic_help, "ERASER", HintMenu.SHOW_TIMES);
            hint.draw(canvas);
        }
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
        return view -> draw.setInstrument(this);
    }
}
