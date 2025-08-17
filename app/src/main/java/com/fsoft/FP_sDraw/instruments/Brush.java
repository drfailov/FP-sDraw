package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.fsoft.FP_sDraw.common.AreaCalculator;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.HintMenu;

import java.util.HashMap;

/**
 *
 * Created by Dr. Failov on 02.01.2015.
 * Edited by Dr. Failov on 28.10.2015.
 */
public class Brush implements Instrument{
    private final DrawCore draw;
    float brushSize;
    float smoothingSensibility;
    int manageMethod;
    boolean antialiasing;
    int brushColor;
    boolean smoothing;
    HashMap<Integer, Branch> branches;
    AreaCalculator invalidateAreaCalculator =new AreaCalculator();
    private HintMenu hint = null;
    private HintMenu pixelArtHint = null;

    public Brush(DrawCore _draw){
        draw = _draw;
    }
    @Override public String getName() {
        return "brush";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentBrush);
    }
    @Override public int getImageResourceID() {
        return R.drawable.menu_brush;
    }
    @Override public void onSelect() {
        branches = new HashMap<>();
        brushSize = (Integer)Data.get(Data.brushSizeInt());
        manageMethod = (Integer)Data.get(Data.manageMethodInt());
        smoothing = (Boolean)Data.get(Data.smoothingBoolean());
        antialiasing = (Boolean)Data.get(Data.antialiasingBoolean());
        brushColor = Data.getBrushColor();
        smoothingSensibility = (Float)Data.get(Data.smoothingSensibilityFloat());
    }
    @Override public void onDeselect() {
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
    @Override public boolean onTouch(MotionEvent event) {
        try {
            //Logger.log(event.toString());
            //trigger zoom if over 2 fingers
            if(event.getPointerCount() > 1 && ((Boolean)Data.get(Data.twoFingersZoomBoolean()) || (Boolean)Data.get(Data.sPenOnlyBoolean()))){
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    if(Data.tools.isAllowedDevice(event)) //if this pointer already draw something
                        draw.undoProvider.undo();
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.brush);
                }
                else
                    Data.tools.showBuyFullDialog();
                return true;
            }

            //process hint event
            if(draw.scale.isPixelMode()) {
                if (pixelArtHint != null && pixelArtHint.processTouch(event))
                    return true;
            }
            else {
                if (hint != null && hint.processTouch(event))
                    return true;
            }

            //process hint events

            //move canvas if touched by finger but activated  sPen mode
            if(!Data.tools.isAllowedDevice(event)) {
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.brush);
                }
                return true;
            }


            event = draw.scale.scale_transformMotionEvent(event);
            int action = event.getAction() & MotionEvent.ACTION_MASK;

            invalidateAreaCalculator.reset();
            if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN){
                int reasonIndex=(event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
                int reasonID = event.getPointerId(reasonIndex);
                if(action == MotionEvent.ACTION_DOWN)
                    draw.undoAreaCalculator.reset();
                draw.undoAreaCalculator.add(event.getX(reasonIndex), event.getY(reasonIndex), brushSize);
                draw.lastChangeToBitmap = System.currentTimeMillis();
                Branch nextBranch;
                branches.put(reasonID, nextBranch = new Branch());
                int toolType = 1;
                if(Build.VERSION.SDK_INT >= 14)
                    toolType = event.getToolType(reasonIndex);
                nextBranch.down(event.getX(reasonIndex), event.getY(reasonIndex), event.getPressure(reasonIndex), event.getSize(reasonIndex), toolType);
            }
            else if(action == MotionEvent.ACTION_MOVE) {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pointerID = event.getPointerId(i);
                    draw.undoAreaCalculator.add(event.getX(i), event.getY(i), brushSize);
                    draw.lastChangeToBitmap = System.currentTimeMillis();
                    int toolType = 1;
                    if(Build.VERSION.SDK_INT >= 14)
                        toolType = event.getToolType(i);
                    if(branches.containsKey(pointerID))
                        branches.get(pointerID).move(event.getX(i), event.getY(i), event.getPressure(i), event.getSize(i), toolType);
                }
            }
            if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP){
                int reasonIndex=(event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
                int reasonID = event.getPointerId(reasonIndex);
                if(branches.containsKey(reasonID)) {
                    Branch branch = branches.get(reasonID);
                    branches.remove(reasonID);
                    int toolType = 1;
                    if(Build.VERSION.SDK_INT >= 14)
                        toolType = event.getToolType(reasonIndex);
                    if(branch != null)
                        branch.up(event.getX(reasonIndex), event.getY(reasonIndex), event.getPressure(reasonIndex), event.getSize(reasonIndex), toolType);
                }
                if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
                    //сохранить шаг отмены, сбросить все что надо
                    draw.undoAreaCalculator.add((int)event.getX(), (int)event.getY(), (int)brushSize);
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
            if(draw.scale.scale_size == 1.0f && !invalidateAreaCalculator.isEmpty())
                draw.redraw(invalidateAreaCalculator.top, invalidateAreaCalculator.bottom, invalidateAreaCalculator.left, invalidateAreaCalculator.right);
            else
                draw.redraw();
        } catch (Exception e){
            Logger.log("Draw.OnDraw.handlerDraw.ACTION_DOWN", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Draw.OnDraw.handlerDraw.ACTION_DOWN", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return true;
    }
    @Override public boolean onKey(KeyEvent event) {
        return false;
    }
    @Override public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(drawUi){
            if(draw.scale.isPixelMode()){
                if(pixelArtHint == null)
                    pixelArtHint = new HintMenu(draw, Data.tools.getResource(R.string.hint_pixelmode), R.drawable.ic_help, "PIXELMODE", HintMenu.SHOW_TIMES);
                pixelArtHint.draw(canvas);
            }
            else {
                if(hint == null)
                    hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_brush), R.drawable.ic_help, "BRUSH", HintMenu.SHOW_TIMES);
                hint.draw(canvas);
            }
        }
    }
    @Override  public boolean isActive() {
        if(draw == null)
            return false;
        if(draw.currentInstrument == this)
            return true;
        return draw.currentInstrument == draw.scale && draw.scale != null && draw.scale.instrumentCallback == this;
    }
    @Override  public boolean isVisibleToUser() {
        return true;
    }
    @Override public View.OnClickListener getOnClickListener() {
        return view -> draw.setInstrument(getName());
    }

    private class Branch{
        //идея в том что для каждой линии на жкране должен создаваться такой класс. Он сас себя должен рисовать, управлять
        //свсоей толщиной, сглаживанием и т.д.
        private final Paint paint;
        private float lx = -1, ly = -1;
        private float cx = -1, cy = -1;//для сглаживания
        private float vx = 0, vy = 0;
        private final float m = 50;                       //эти два коэффициента получены эмпирическим путем
        private final float rubbingCoefficient = 0.8f;
        private long lastEvent = -1;

        public Branch(){
            paint = new Paint();
            paint.setAntiAlias(antialiasing);
            paint.setColor(brushColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(brushSize);
        }
        public void down(float x, float y, float pressure, float size, int tool){
            setLast(x, y);
            drawCircle(x, y, lastSize = smoothBrushSize(brushSize(x, y, pressure, size, tool)));
        }
        public void move(float x, float y, float pressure, float size, int tool){
            if(Math.sqrt(x*x+y*y) < Data.store().DPI / 50)
                return;
            float lineSize = smoothBrushSize (brushSize(x, y, pressure, size, tool));
            if(smoothing)
                drawSmoothedLine(lx, ly, x, y, lineSize);
            else {
                drawCircle(x, y, lineSize);
                drawLine(lx, ly, x, y, lineSize);
            }
            setLast(x, y);
        }
        public void up(float x, float y, float pressure, float size, int tool){
            float lineSize = lastSize;
            if(smoothing)
                drawSmoothedLine(cx, cy, x, y, lineSize);
            else {
                drawLine(lx, ly, x, y, lineSize);
                drawCircle(x, y, lineSize);
            }
        }

        private float lastSize = getStartingSize();
        private float getStartingSize(){
            if(manageMethod == Data.MANAGE_METHOD_SIZE || manageMethod == Data.MANAGE_METHOD_PRESSURE)
                return 0;
            if(manageMethod == Data.MANAGE_METHOD_SPEED)
                return brushSize*0.6f;
            if(manageMethod == Data.MANAGE_METHOD_SPEED_INVERSE)
                return 0;
            return brushSize;
        }
        private void drawCircle(float x, float y, float size){
            invalidateAreaCalculator.add(x, y, size);
            draw.canvas.drawCircle(x, y, size/2f, paint);
        }
        private void drawLine(float x1, float y1, float x2, float y2, float size){
            invalidateAreaCalculator.add(x1, y1, size);
            invalidateAreaCalculator.add(x2, y2, size);
            float totalParts = ((int)(Math.abs(size - lastSize) / 0.5f/*(((float)Data.store().DPI) / 300f)*/)) + 1;
            for (float i = 0; i < totalParts; i++) {
                float cx = x1 + (i * (x2-x1)/totalParts);
                float ax = x1 + ((i+1) * (x2-x1)/totalParts);
                if(i < totalParts -1)
                    ax += (ax-cx)/2;
                float cy = y1 + (i * (y2-y1)/totalParts);
                float ay = y1 + ((i+1) * (y2-y1)/totalParts);
                if(i < totalParts -1)
                    ay += (ay-cy)/2;
                float curSize = lastSize + (i * ((size - lastSize)/totalParts));
                paint.setStrokeWidth(curSize);
                draw.canvas.drawLine(cx, cy, ax, ay, paint);
            }
            lastSize = size;
        }
        private void drawSmoothedLine(float x1, float y1, float x2, float y2, float size){
            //Идея технологии: Моделируем в точке прикосновения местонахождение виртуального обьекта
            //а при перемещении пальца прикладываем к обьекту силу в направлении нового прикосновения
            float lcx = cx, lcy = cy;
            long now = System.currentTimeMillis();
            long dt = now-lastEvent;
            float iterations = 5;
            if(dt > 5) {
                iterations = dt * smoothingSensibility; // stable: 0.25    min: 0.1    max: 1.0
            }
            if(iterations < 1)
                iterations = 1;
            if(vx == 0 || vy == 0) {
                vx = (x2 - cx) * 0.1f;
                vy = (y2 - cy) * 0.1f;
                iterations = 0;
            }
            //Logger.log("iterations = "+iterations);
            float lastSize = this.lastSize;
            for (float i = 0; i < iterations; i++) {
                float Fx = x2-cx;
                float Fy = y2-cy;
                float ax = Fx / m;
                float ay = Fy / m;
                vx *= rubbingCoefficient;
                vy *= rubbingCoefficient;
                vx += ax;
                vy += ay;
                cx += vx;
                cy += vy;
                if(lcx != cx || lcy != cy){
                    float curSize = lastSize + (size - lastSize)*(i / iterations);
                    drawCircle(cx, cy, curSize);
                    drawLine(lcx, lcy, cx, cy, curSize);
                }
                lcx = cx;
                lcy = cy;
            }
        }
        private void setLast(float x, float y){
            if(cx == -1){
                cx = x;
                cy = y;
            }
            lx = x;
            ly = y;
            lastEvent = System.currentTimeMillis();
        }
        private float brushSize(float x, float y, float pressure, float size, int tool){
            if(manageMethod == Data.MANAGE_METHOD_SIZE){
                float curMin = (Float)Data.get(Data.sizeMinFloat(tool));
                float curMax = (Float)Data.get(Data.sizeMaxFloat(tool));
                if(curMin == -1 || curMax == -1 || curMin == curMax)
                    return brushSize;
                float value = size - curMin;
                float peak = curMax - curMin;
                if(peak == 0)
                    return brushSize;
                return (brushSize * (value/peak));
            }

            if(manageMethod == Data.MANAGE_METHOD_PRESSURE){
                float curMin = (Float)Data.get(Data.pressureMinFloat(tool));
                float curMax = (Float)Data.get(Data.pressureMaxFloat(tool));
                if(curMin == -1 || curMax == -1 || curMin == curMax)
                    return brushSize;
                float value = pressure - curMin;
                float peak = curMax - curMin;
                if(peak == 0)
                    return brushSize;
                return (brushSize * (value/peak));
            }

            else if(manageMethod == Data.MANAGE_METHOD_SPEED){
                long now_ms = System.currentTimeMillis();
                long t_ms = now_ms-lastEvent;
                if(t_ms == 0)
                    t_ms = 10;

                float dx_px = lx - x;
                float dy_px = ly - y;
                float d_px = (float)Math.sqrt(dx_px*dx_px + dy_px*dy_px);
                float dpi_px = Data.store().DPI;
                float d_in = d_px / dpi_px;
                float d_mm = d_in*25.4f;

                float v_mm_ms = d_mm / t_ms;

                float coef = 1;

                coef += brushSize / 10f;   //больше - меньше
                coef /= v_mm_ms * 80.0f; //больше - меньше

                return (brushSize * Math.min(1, coef));
            }

            else if(manageMethod == Data.MANAGE_METHOD_SPEED_INVERSE){
                float dx_px = lx - x;
                float dy_px = ly - y;
                float d_px = (float)Math.sqrt(dx_px*dx_px + dy_px*dy_px);
                float dpi_px = Data.store().DPI;
                float d_in = d_px / dpi_px;

                float result = 1 + brushSize * d_in * 5;
                if(result>brushSize*2)
                    result = brushSize*2;

                return result;
            }

            else {
                return brushSize;
            }
        }
        private float smoothBrushSize(float in){
            //эта функция будет вызываться функцией BrushSize и её задача - не допустить слишком резких скачков толщины
            // Если функция BrushSiz приняла решение о резком скачке, сдвигать значение не более чем на ... пикселей
            return lastSize + (in - lastSize) * 0.1f;
        }
    }
}
