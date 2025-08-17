package com.fsoft.FP_sDraw.menu;

import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterPath;
import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterSum;
import static com.fsoft.FP_sDraw.common.Tools.getSquirclePath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.ArrayList;

/**
 * Created by Dr. Failov on 17.01.2015.
 */
public class Palette extends HorizontalScrollView {
    public interface ColorChecker{
        boolean isActive(int color);
    }
    public interface ColorApplier {
        void applyColor(int color, View colorView, int colorPositionIndex);
    }
    private final ArrayList<Palette.PaletteElement> elements;
    private final ArrayList<ColorApplier> appliers;


    public Palette(Context context, int[] palette, int size, Palette.ColorApplier applier, Palette.ColorChecker checker, Palette.ColorApplier applierLong) {
        super(context);
        elements = new ArrayList<>();
        appliers = new ArrayList<>();
        appliers.add(applier);
        //palette
        LinearLayout palette_brush_layout = new LinearLayout(context);
        LinearLayout palette_brush_item_layout=null;
        palette_brush_layout.setOrientation(LinearLayout.HORIZONTAL);
        palette_brush_layout.setPadding(0, 0, 0, Tools.dp(5)); //зазор для полосы прокрутки
        Paint palette_brush_item_paint=new Paint();
        palette_brush_item_paint.setAntiAlias(true);

        LinearLayout.LayoutParams layout_params_common;
        layout_params_common = new LinearLayout.LayoutParams(Tools.dp(40), Tools.dp(40));

        for (int i=0; i < palette.length; i++) {
            int color = palette[i];
            PaletteElement paletteElement = new PaletteElement(context, color, i, checker, getBatchApplier(), applierLong);
            paletteElement.setLayoutParams(layout_params_common);
            elements.add(paletteElement);

            //add button to list here
            if (palette_brush_item_layout == null || palette_brush_item_layout.getChildCount() >= size) {
                palette_brush_item_layout = new LinearLayout(context);
                palette_brush_item_layout.setOrientation(LinearLayout.VERTICAL);
                palette_brush_item_layout.addView(paletteElement);
                palette_brush_layout.addView(palette_brush_item_layout);
            } else {
                palette_brush_item_layout.addView(paletteElement);
            }
        }
        if(Build.VERSION.SDK_INT >= 16)
            setScrollBarSize(Tools.dp(3));
        addView(palette_brush_layout);
    }

    public void refresh(){
        for (int i = 0; i < elements.size(); i++) {
            elements.get(i).invalidate();
        }
    }
    private ColorApplier getBatchApplier(){
        return new ColorApplier() {
            @Override
            public void applyColor(int color, View view, int index) {
                for(ColorApplier applier:appliers)
                    applier.applyColor(color, view, index);
            }
        };
    }
    public void addApplier(ColorApplier applier){
        appliers.add(applier);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(!Data.tools.isAllowedDeviceForUi(ev))
            return false;
        return super.onTouchEvent(ev);
    }

    class PaletteElement extends View {
        int DPI = (int)Data.store().DPI;
        MarkPart markPart = new MarkPart();
        HoverMarkPart hoverMarkPart = new HoverMarkPart();
        int color;
        Palette.ColorChecker checker = null;
        Palette.ColorApplier applyer;
        Palette.ColorApplier applyerLong;
        float width = 0;
        float height = 0;
        int strokeSize = 0;
        Paint paint;
        RectF rect;

        Path squircleCache = null;
        float squircleCacheSum = 0;

        public PaletteElement(Context context, int _color, int index, Palette.ColorChecker checker, Palette.ColorApplier _applyer, Palette.ColorApplier _applyerLong) {
            super(context);
            this.color = _color;
            this.checker = checker;
            this.applyer = _applyer;
            this.applyerLong = _applyerLong;

            strokeSize = Tools.dp(3);
            //-----------paint
            paint = new Paint();
            paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            paint.setColor(color);
            paint.setStrokeWidth(strokeSize / 2f);

            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (applyer != null)
                        applyer.applyColor(color, view, index);
                    refresh();
                }
            });

            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(applyerLong != null){
                        applyerLong.applyColor(color, view, index);
                        refresh();
                        return true;
                    }
                    return false;
                }
            });
            setPressedState(false);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

//        @Override
//        public boolean dispatchTouchEvent(MotionEvent event) {
//            try {
////                int action = event.getAction() & MotionEvent.ACTION_MASK;
////                if (action == MotionEvent.ACTION_DOWN) {
////                    setPressedState(true);
////                } else if (action == MotionEvent.ACTION_MOVE) {
////                    if (event.getX() < 0 || event.getY() < 0 || event.getX() > getWidth() || event.getY() > getHeight()) {//если вышли за пределы элемента
////                        setPressedState(false);
////                    }
////                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) { //отпустили палец
////                    setPressedState(false);
////                }
//            }
//            catch (Exception e){
//                e.printStackTrace();
//                Logger.log("PaletteElement.dispatchTouchEvent", "Ошибка обработки прикосновения к палитре: " + e.getMessage(), false);
//            }
//            return super.dispatchTouchEvent(event);
//        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            width = w;
            height = h;
            rect = new RectF(strokeSize, strokeSize, width-strokeSize, height-strokeSize);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            try {
                super.onDraw(canvas);
                {
                    if(Data.isSqircle()) {//1 out of 3
                        if(squircleCache == null || squircleCacheSum != getSquircleCenterSum(width/2f, height/2f, (width / 2f) - strokeSize)) {
                            squircleCache = getSquircleCenterPath(width/2f, height/2f, (width / 2f) - strokeSize);
                            squircleCacheSum = getSquircleCenterSum(width/2f, height/2f, (width / 2f) - strokeSize);
                        }
                        canvas.drawPath(squircleCache, paint);
                    }
                    else if(Data.isCircle())
                        canvas.drawCircle(width / 2, height / 2, (width / 2) - strokeSize, paint);
                    else if(Data.isRect())
                        canvas.drawRect(strokeSize, strokeSize, width-strokeSize, height - strokeSize, paint);
                }
                if(Build.VERSION.SDK_INT >= 14) {
                    hoverMarkPart.draw(canvas);
                }
                markPart.draw(canvas);
            }
            catch (Exception e){
                Logger.log(Tools.getStackTrace(e));
            }
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {

                if(!Data.tools.isAllowedDeviceForUi(event))
                    return false;

                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if (action == MotionEvent.ACTION_DOWN) {
                    setPressedState(true);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (event.getX() < 0 || event.getY() < 0 || event.getX() > getWidth() || event.getY() > getHeight()) {//если вышли за пределы элемента
                        setPressedState(false);
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) { //отпустили палец
                    setPressedState(false);
                }
            }
            catch (Exception e){
                e.printStackTrace();
                Logger.log("PaletteElement.onTouchEvent", "Ошибка обработки прикосновения к палитре: " + e.getMessage(), false);
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean onHoverEvent(MotionEvent event) {
            invalidate();
            return super.onHoverEvent(event);
        }

        void setPressedState(boolean pressed){
            if(pressed) {
                paint.setStyle(Paint.Style.STROKE);
            }else {
                paint.setStyle(Paint.Style.FILL);
            }
            invalidate();
        }

        class MarkPart extends AnimationAdapter{
            boolean lastPressed = false;
            Paint markPaint;
            RectF markBackRect = null;

            public MarkPart() {
                //-----------mark paint
                markPaint = new Paint();
                markPaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                markPaint.setColor(Color.WHITE);
            }
            @Override public void draw(Canvas canvas){
                //Logger.log("percent = " + animPercent);
                boolean pressed = checker != null && checker.isActive(color);
                if(lastPressed != pressed){
                    setPressed(pressed);
                    lastPressed = pressed;
                }
                if(getAnimPercent() < 2)
                    return;
                float markMargin = strokeSize * 3;
                float maxWidth = getWidth() - markMargin*2;
                float maxHeight = getHeight() - markMargin*2;

                //background circle
                {
                    float cx = width/2;
                    float cy = height/2;
                    float coef = Math.min(1, getAnimPercent()/50f);
                    float width = maxWidth * coef;
                    float height = maxHeight * coef;
                    float left = cx-width/2f;
                    float right = left + width;
                    float top = cy-height/2f;
                    float bottom = top + height;
                    int backColor = Color.argb(200, 255, 255, 255);
                    float w = right - left;
                    float h = bottom - top;
                    markPaint.setColor(backColor);

                    if(markBackRect == null)
                        markBackRect = new RectF(left, top, right, bottom);
                    else
                        markBackRect.set(left, top, right, bottom);

                    {
                        if(Data.isSqircle()) //1 out of 3
                            canvas.drawPath(getSquircleCenterPath(left + w / 2, top + h / 2, w / 2), markPaint);
                        else if(Data.isCircle())
                            canvas.drawCircle(left + w / 2, top + h / 2, w / 2, markPaint);
                        else if(Data.isRect())
                            canvas.drawRect(left, top, right, bottom, markPaint);
                    }
                }


                //V - mark
                {
                    int checkColor = Color.argb(200, 0, 0, 0);
                    float coef = Math.min(1, Math.max(0, (getAnimPercent()-50) / 50f));
                    markPaint.setColor(checkColor);
                    markPaint.setStrokeWidth(maxWidth * 0.1f);
                    float[] points = new float[]{
                            markMargin + maxWidth * 0.2f, markMargin + maxHeight * 0.45f, markMargin + maxWidth * 0.4f, markMargin + maxHeight * 0.65f,
                            markMargin + maxWidth * 0.4f, markMargin + maxHeight * 0.7f, markMargin + maxWidth, markMargin + maxHeight * 0.1f
                    }; //bx, by, ex, ey, ...

                    float totalLength = 0;
                    for (int i = 0; i < points.length; i += 4) {
                        float bx = points[i];
                        float by = points[i + 1];
                        float ex = points[i + 2];
                        float ey = points[i + 3];
                        float dx = ex - bx;
                        float dy = ey - by;
                        totalLength += Math.sqrt(dx * dx + dy * dy);
                    }

                    float allowedLength = totalLength * coef;
                    float drawedLength = 0;
                    for (int i = 0; i < points.length; i += 4) {
                        float bx = points[i];
                        float by = points[i + 1];
                        float ex = points[i + 2];
                        float ey = points[i + 3];
                        float dx = ex - bx;
                        float dy = ey - by;
                        float d = (float) Math.sqrt(dx * dx + dy * dy);
                        float remainingLength = allowedLength - drawedLength;
                        float lineCoef = Math.min(1f, Math.max(0, remainingLength / d));
                        ex = bx + lineCoef * dx;
                        ey = by + lineCoef * dy;
                        canvas.drawLine(bx, by, ex, ey, markPaint);
                        float drawed_d = (float) Math.sqrt(dx * dx + dy * dy);
                        drawedLength += drawed_d;
                    }
                }
            }
            @Override protected void redraw(){
                PaletteElement.this.invalidate();
            }
        }
        class HoverMarkPart extends  AnimationAdapter{
            Paint strokePaint;
            boolean lastActive = false;

            public HoverMarkPart() {
                setQuickOn(true);
                setAnimTime(300);
                strokePaint = new Paint();
                strokePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                strokePaint.setStrokeWidth(DPI / 70f);
                strokePaint.setStyle(Paint.Style.STROKE);
            }

            @Override
            protected void redraw() {
                PaletteElement.this.postInvalidate();
            }

            @Override
            public void draw(Canvas canvas) {
                boolean pressed = false;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    pressed = isHovered();
                }
                if(lastActive != pressed){
                    setPressed(pressed);
                    lastActive = pressed;
                }
                if(getAnimPercent() < 2)
                    return;
                strokePaint.setColor(Color.argb((int)(255f*(getAnimPercent()/100f)), 255, 255, 255));

                {
                    if(Data.isSqircle()) //1 out of 3
                        canvas.drawPath(getSquircleCenterPath(width / 2, height / 2, (width / 2) - strokeSize), strokePaint);
                    else if(Data.isCircle())
                        canvas.drawCircle(width / 2, height / 2, (width / 2) - strokeSize, strokePaint);
                    else if(Data.isRect())
                        canvas.drawRect(strokeSize, strokeSize, width-strokeSize, height-strokeSize, strokePaint);
                }
            }
        }
    }
}
