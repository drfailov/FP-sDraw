package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.HintMenu;

/**
 * Created by Dr. Failov on 17.01.2015.
 */
public class Line implements Instrument{
    private final DrawCore draw;
    Point line_start = new Point(0,0);
    Point line_end = new Point(0,0);
    boolean line_drawing=false;
    int brushSize = 0;
    int brushColor = 0;
    boolean antialiasing = false;
    Paint paint = new Paint();
    HintMenu hint = null;

    public Line(DrawCore d){
        draw = d;
    }

    @Override
    public String getName() {
        return "line";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentLine);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.menu_line;
    }

    @Override
    public void onSelect() {
        brushSize = (Integer)Data.get(Data.brushSizeInt());
        brushColor = Data.getBrushColor();//(Integer)Data.get(Data.brushColorInt());
        antialiasing = (Boolean)Data.get(Data.antialiasingBoolean());
        line_drawing=false;
        paint.setStrokeWidth(brushSize);
        paint.setAntiAlias(antialiasing);
        paint.setColor(brushColor);
    }

    @Override
    public void onDeselect() {
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        //trigger zoom if over 2 fingers
        if(event.getPointerCount() > 1){// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
            if(Data.tools.isFullVersion()) {
                line_drawing=false;
                draw.setInstrument(draw.scale);
                draw.scale.onTouch(event);
                draw.scale.setInstrumentCallback(draw.line);
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
                draw.scale.setInstrumentCallback(draw.line);
            }
            return true;
        }

        event = draw.scale.scale_transformMotionEvent(event);

        try {
            int action = event.getAction() & MotionEvent.ACTION_MASK;

            if(action==MotionEvent.ACTION_DOWN){
                line_start.set(event.getX(), event.getY());
                line_end.set(event.getX(), event.getY());
                line_drawing=true;
            }
            else if(action==MotionEvent.ACTION_MOVE){
                line_end.set(event.getX(), event.getY());

                draw.undoAreaCalculator.reset();
                draw.undoAreaCalculator.add((int) line_start.x, (int) line_start.y, brushSize);
                draw.undoAreaCalculator.add((int) line_end.x, (int) line_end.y, brushSize);
            }
            else if(action==MotionEvent.ACTION_UP) {
                if(line_drawing){
                    line_drawing=false;
                    line_end.set(event.getX(), event.getY());
                    //draw it
                    draw.canvas.drawCircle(line_start.x, line_start.y, (float) brushSize / 2, paint);
                    draw.canvas.drawLine(line_start.x, line_start.y, line_end.x, line_end.y, paint);
                    draw.canvas.drawCircle(line_end.x, line_end.y, (float)brushSize/2, paint);
                    draw.lastChangeToBitmap = System.currentTimeMillis();

                    //make undo backup
                    draw.undoAreaCalculator.reset();
                    draw.undoAreaCalculator.add((int) line_start.x, (int) line_start.y, brushSize);
                    draw.undoAreaCalculator.add((int) line_end.x, (int) line_end.y, brushSize);
                    draw.undoAreaCalculator.check(draw.bitmap.getWidth(), draw.bitmap.getHeight());
                    draw.undoProvider.apply(draw.undoAreaCalculator.top, draw.undoAreaCalculator.bottom, draw.undoAreaCalculator.left, draw.undoAreaCalculator.right);
                    draw.undoProvider.prepare();
                    draw.undoAreaCalculator.reset();
                }
            }
            draw.redraw();
        } catch (Exception e){
            Logger.log("Draw.OnDraw.handlerLine", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Draw.OnDraw.handlerLine", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(line_drawing) {
            canvas.drawCircle(draw.scale.ImageToScreenX(line_start.x), draw.scale.ImageToScreenY(line_start.y), (float) (brushSize / 2) * draw.scale.scale_size, paint);
            float oldStrokeSize = paint.getStrokeWidth();
            paint.setStrokeWidth(oldStrokeSize * draw.scale.scale_size);
            canvas.drawLine(draw.scale.ImageToScreenX(line_start.x), draw.scale.ImageToScreenY(line_start.y), draw.scale.ImageToScreenX(line_end.x), draw.scale.ImageToScreenY(line_end.y), paint);
            paint.setStrokeWidth(oldStrokeSize);
            canvas.drawCircle(draw.scale.ImageToScreenX(line_end.x), draw.scale.ImageToScreenY(line_end.y), (float) (brushSize / 2) * draw.scale.scale_size, paint);
        }
        if(drawUi) {
            if (hint == null)
                hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_line), R.drawable.ic_help, "LINE_DRAWING", HintMenu.SHOW_TIMES);
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
        return view -> draw.setInstrument(getName());
    }
}
