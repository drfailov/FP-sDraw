package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.AreaCalculator;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.HintMenu;

/**
 * Created by Dr. Failov on 15.01.2023.
 */
public class Mosaic implements Instrument{
    private final DrawCore draw;
    private AreaCalculator undoAreaCalculator;
    private HintMenu hint = null;

    public Mosaic(DrawCore d){
        draw = d;
        if(d != null)
            undoAreaCalculator = d.undoAreaCalculator;
    }

    @Override
    public String getName() {
        return "mosaic";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.mosaic);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.ic_mosaic;
    }

    @Override
    public void onSelect() {
    }

    @Override
    public void onDeselect() {
        if(!draw.undoAreaCalculator.isEmpty()){
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
        //trigger zoom if over 2 fingers
        if(event.getPointerCount() > 1){// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
            if(Data.tools.isFullVersion()) {
                boolean isEmpty = undoAreaCalculator.isEmpty();
                draw.setInstrument(draw.scale);
                if(!isEmpty) //if this pointer already draw something
                    draw.undoProvider.undo();
                draw.scale.onTouch(event);
                draw.scale.setInstrumentCallback(draw.mosaic);
            }
            else
                Data.tools.showBuyFullDialog();
            return true;
        }

        //process hint events
        if(hint != null && hint.processTouch(event))
            return true;

        //move canvas if touched by finger but activated  sPen mode
        if(!Data.tools.isAllowedDevice(event)) {
            if(Data.tools.isFullVersion()) {
                draw.setInstrument(draw.scale);
                draw.scale.onTouch(event);
                draw.scale.setInstrumentCallback(draw.mosaic);
            }
            return true;
        }

        event = draw.scale.scale_transformMotionEvent(event);

        try{
            if(event.getX() < draw.bitmap.getWidth() && event.getY() < draw.bitmap.getHeight() && event.getX() > 0 && event.getY() > 0){
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    undoAreaCalculator.reset();
                }
                if(event.getAction() == MotionEvent.ACTION_MOVE) {
                    float blockSize = (Integer)Data.get(Data.mosaicSizeInt());
                    float x = event.getX();
                    float y = event.getY();
                    float ix = x/blockSize;
                    float iy = y/blockSize;
                    float bx = ((int)ix*blockSize);
                    float by = ((int)iy*blockSize);
                    int basePixel = draw.bitmap.getPixel((int)(bx+blockSize/2), (int)(by+blockSize/2));
//                    boolean empty = true;
//                    for(int cx = (int)bx; cx<bx+blockSize && empty; cx++){
//                        for(int cy = (int)by; cy<by+blockSize && empty; cy++){
//                            if(cx < draw.bitmap.getWidth() && cy < draw.bitmap.getHeight()) {
//                                if (draw.bitmap.getPixel(cx, cy) != basePixel) {
//                                    empty = false;
//                                }
//                            }
//                        }
//                    }
//                    if(!empty)
                    {
                        undoAreaCalculator.add((int)(bx+blockSize/2), (int)(by+blockSize/2), blockSize);
                        for(int cx = (int)bx; cx<bx+blockSize; cx++){
                            for(int cy = (int)by; cy<by+blockSize; cy++){
                                if(cx < draw.bitmap.getWidth() && cy < draw.bitmap.getHeight()) {
                                    draw.bitmap.setPixel(cx, cy, basePixel);
                                }
                            }
                        }
                        draw.lastChangeToBitmap = System.currentTimeMillis();
                    }
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    undoAreaCalculator.check(draw.bitmap.getWidth(), draw.bitmap.getHeight());
                    draw.undoProvider.apply(undoAreaCalculator.top, undoAreaCalculator.bottom, undoAreaCalculator.left, undoAreaCalculator.right);
                    draw.undoProvider.prepare();
                    undoAreaCalculator.reset();
                }
                draw.redraw();
                //draw.redraw(0, circleRadius * 2, 0, circleRadius * 2);
            }
        } catch (Exception e){
            Logger.log("Draw.touch.Mosaic", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Draw.touch.Mosaic", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(drawUi) {
            if (hint == null)
                hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_mosaic), R.drawable.ic_help, "MOSAIC", HintMenu.SHOW_TIMES);
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
        return view -> draw.setInstrument(Mosaic.this);
    }
}
