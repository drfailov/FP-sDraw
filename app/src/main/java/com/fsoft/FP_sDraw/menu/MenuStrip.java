package com.fsoft.FP_sDraw.menu;

import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterPath;
import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterSum;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.ArrayList;

/**
 *
 * Created by Dr. Failov on 17.01.2015.
 */
public class MenuStrip extends HorizontalScrollView {
    public interface ActiveChecker{
        boolean isActive();
    }

    ArrayList<MenuStrip.MenuStripElement> elements;
    public LinearLayout linearLayout;
    Context context;
    int colorBackground;
    int colorIdle;
    int colorActive;
    int colorPressed;

    public MenuStrip(Context c, int backgroundColor) {
        super(c);
        context = c;
        elements = new ArrayList<>();
        if(Build.VERSION.SDK_INT >= 16)
            setScrollBarSize(Tools.dp(3));
        int DPI = (int)Data.store().DPI;
        linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        linearLayout.setPadding(0, 0, 0, DPI / 50);
        setBackgroundColor(backgroundColor);
        setFillViewport(true);
        addView(linearLayout);
    }
    @Override  public void setBackgroundColor(int color) {
        colorBackground = color;
        colorIdle = Data.tools.getGridColor(colorBackground, 0.2f);
        colorActive = Data.tools.getGridColor(colorBackground, 0.3f);
        colorPressed = Data.tools.getGridColor(colorBackground, 0.5f);
        super.setBackgroundColor(color);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(!Data.tools.isAllowedDeviceForUi(ev))
            return false;
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    public void refresh() {

        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            linearLayout.getChildAt(i).invalidate();
        }
    }
    public void clear(){
        linearLayout.removeAllViews();
    }
    public MenuStripElement addButton(int image, OnClickListener listener, MenuStrip.ActiveChecker checker, int size, String displayName){
        MenuStrip.MenuStripElement mse = new MenuStrip.MenuStripElement(context, image, listener, checker, size, displayName);
        LinearLayout.LayoutParams layout_params = new LinearLayout.LayoutParams(size, size);
        layout_params.setMargins(size/20, size/10, size/20, size/10);
        mse.setLayoutParams(layout_params);
        elements.add(mse);
        linearLayout.addView(mse);
        return mse;
    }
    public void setColors(int idle, int active, int pressed){
        colorIdle = idle;
        colorPressed = pressed;
        colorActive = active;
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        if(!Data.tools.isAllowedDevice(ev))
//            return true;
//        return super.onTouchEvent(ev);
//    }

    public class MenuStripElement extends View {
        Context context;
        HoverMarkPart hoverMarkPart = new HoverMarkPart();
        ActiveMarkPart activeMarkPart = new ActiveMarkPart();
        PressedMarkPart pressedMarkPart = new PressedMarkPart();
        boolean pressed = false;
        int resource;
        //int size;
        int cachedColor = 0;
        //int padding = 3;
        String displayName;
        OnClickListener onClickListener;
        MenuStrip.ActiveChecker activeChecker;
        Paint paint;
        RectF area;
        Bitmap icon = null;
        Thread loadingThread = null;

        public MenuStripElement(Context _context, int _resource, OnClickListener _onClick, MenuStrip.ActiveChecker _activeChecker, int _size, String displayName) {
            super(_context);
            context = _context;
            resource = _resource;
            activeChecker = _activeChecker;
            onClickListener = _onClick;
            this.displayName = displayName;
            //size = _size;
            //padding = size/6;
            paint = new Paint();
            paint.setAntiAlias((Build.VERSION.SDK_INT > 14));
            paint.setFilterBitmap((Build.VERSION.SDK_INT > 14));
            paint.setColor(Color.WHITE);
            paint.setFilterBitmap(false);

            if(onClickListener != null)
                setOnClickListener(onClickListener);
            if(displayName != null)
                setOnLongClickListener(getOnLongClinkListener());
            //startLoading();
            //setScaleType(ScaleType.FIT_XY);
        }
        @Override protected void onDraw(Canvas canvas) {
            if(area == null)
                area = new RectF(0,0, getWidth()-1, getHeight()-1);

            hoverMarkPart.draw(canvas);
            activeMarkPart.draw(canvas);
            pressedMarkPart.draw(canvas);

            if(cachedColor != colorBackground)
                startLoading();
            if(icon != null) {
                //Log.d("TAG", "Icon: w="+icon.getWidth() + " h=" + icon.getHeight());
                canvas.drawBitmap(icon, getPaddingLeft(), getPaddingTop(), paint);
            }
            else{
                float r = getWidth()/4;
                float cx = getWidth()/2;
                float cy = getHeight() / 2;
                canvas.drawCircle(cx, cy, r, paint);
            }
//            if(activeChecker != null && activeChecker.isActive()) {
//                paint.setColor(colorActive);
//                paint.setStyle(Paint.Style.STROKE);
//                paint.setStrokeWidth(2f);
//                canvas.drawRoundRect(area, getWidth() / 10, getHeight() / 10, paint);
//            }
            //super.onCanvasDraw(canvas);
        }
        private boolean hovered(){
            if(Build.VERSION.SDK_INT >= 14){
                return isHovered();
            }
            return false;
        }
        @Override public boolean performClick() {
            boolean result = super.performClick();
            Tools.vibrate(this);
            refresh();
            return result;
        }
        @Override public boolean onHoverEvent(MotionEvent event) {
            invalidate();
            return super.onHoverEvent(event);
        }
        @Override  public boolean onTouchEvent(MotionEvent event) {
            if(!Data.tools.isAllowedDeviceForUi(event))
                return false;
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(action == MotionEvent.ACTION_DOWN){                                         //залить черным
                pressed = true;
                invalidate();
            }
            else if(action == MotionEvent.ACTION_MOVE){
                if(event.getX() < 0  || event.getY() < 0 || event.getX() > getWidth() || event.getY() > getHeight()) {//если вышли за пределы элемента
                    pressed = false;
                    invalidate();
                }
            }
            else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){ //отпустили палец
                pressed = false;
                invalidate();
            }
            return super.onTouchEvent(event);
        }
        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            setPadding(getWidth() / 5, getHeight() / 5, getWidth() / 5, getHeight() / 5);
            startLoading();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            try {
                if (icon != null) {
                    icon.recycle();
                    icon = null;
                    cachedColor = Color.TRANSPARENT;
                }
            }
            catch (Exception e){
                //do nothing
            }
        }

        boolean isActive(){
            if(activeChecker == null)
                return false;
            return activeChecker.isActive();
        }

        private void startLoading(){
            if(loadingThread != null)
                loadingThread.interrupt();
            loadingThread = new Thread(() -> {
                Bitmap bitmap = null;
                try {
                    //Thread.sleep(300);
                    if (!Thread.interrupted())
                        bitmap = transformResource(resource, getWidth() - (getPaddingLeft() + getPaddingRight()));//
                    if (!Thread.interrupted() && bitmap != null)
                        setImageBitmap(bitmap);
                    if (!Thread.interrupted()) {
                        postInvalidate();
                        loadingThread = null;
                    }
                } catch (Throwable e) {
                    loadingThread = null;
                    //Logger.log(e);
                }
            });
            loadingThread.start();
        }
        public void setImageBitmap(Bitmap bitmap){
            icon = bitmap;
        }
        private OnLongClickListener getOnLongClinkListener(){
            return new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Tools.vibrate(view);
                    if(displayName != null)
                        Logger.show(displayName);
                    return true;
                }
            };
        }

        private Bitmap transformResource(int resource, int size){
            try {
                Bitmap bitmap = decodeResource(resource, size, size);
                cachedColor = colorBackground;
                if(Tools.isLightColor(cachedColor))
                    bitmap = Tools.getBitmapContour(bitmap, Color.argb(150, 0,0,0));//Data.tools.getGridColor(cachedColor, 0.4f));
//                else
//                    bitmap = Tools.getBitmapContour(bitmap, Color.WHITE);
                return bitmap;
            }
            catch (Throwable e){
                Logger.log(Tools.getStackTrace(e));
                return Bitmap.createBitmap(10,10, Bitmap.Config.ARGB_8888);
            }
        }
        Bitmap decodeResource(int resId, int required_w, int required_h) {
            try{
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                //options.inScreenDensity = Data.store().DPI*2;
                BitmapFactory.decodeResource(context.getResources(), resId, options);
                // Calculate inSampleSize
                final int height = options.outHeight;
                final int width = options.outWidth;
                if (height > required_h && width > required_w) {
                    // Calculate ratios of height and width to requested height and width
                    int heightRatio = Math.max(1, Math.round((float) height / required_h)-1);
                    int widthRatio = Math.max(1, Math.round((float) width / required_w)-1);
                    // Choose the smallest ratio as inSampleSize value, this will guarantee a final image with both dimensions larger than or equal to the requested height and width.
                    int iss = heightRatio < widthRatio ? heightRatio : widthRatio;
                    if(iss > 16) iss = 16;
                    else if(iss > 8) iss = 8;
                    else if(iss > 4) iss = 4;
                    else if(iss > 2) iss = 2;
                    options.inSampleSize = iss;
                }
                options.inJustDecodeBounds = false;
                // Decode bitmap with inSampleSize set

//                Log.d("TAG", "----------------------------------------------------------");
//                Log.d("TAG", "decodeResource: original_height = "+height + " original_width=" + width);
//                Log.d("TAG", "decodeResource: required_w = "+required_w + " required_h=" + required_h);
//                Log.d("TAG", "decodeResource: size = "+getWidth());
//                Log.d("TAG", "decodeResource: options.inSampleSize="+options.inSampleSize);

                Bitmap result=null;
                try {
                    result = BitmapFactory.decodeResource(context.getResources(), resId, options);
                    //Log.d("TAG", "decodeResource: loaded_w="+ result.getWidth() + " loaded_h=" + result.getHeight());
                    result=Bitmap.createScaledBitmap(result,required_w, required_h, true);
                    //Log.d("TAG", "decodeResource: scaled_w="+ result.getWidth() + " scaled_h=" + result.getHeight());
                } catch (OutOfMemoryError e){
                    Logger.log("GlobalData.decodeFile", Tools.getStackTrace(e), false);
                }
                return result;
            }catch (Exception e){
                Logger.log("Где-то в GlobalData.decodeFile произошла ошибка ", e + "\nStackTrace: \n" + Tools.getStackTrace(e), false);
            }catch (OutOfMemoryError e) {
                Logger.log("Где-то в GlobalData.decodeFile Недостаточно памяти ", e + "\nStackTrace: \n" + Tools.getStackTrace(e), false);
            }
            return null;
        }   //декодирование файла с оптимизацией до определенного размера

        class HoverMarkPart extends  AnimationAdapter{
            Paint strokePaint;
            boolean lastActive = false;

            public HoverMarkPart() {
                setQuickOn(true);
                setAnimTime(300);
                strokePaint = new Paint();
                strokePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            }

            @Override
            protected void redraw() {
                MenuStripElement.this.invalidate();
            }

            @Override
            public void draw(Canvas canvas) {
                boolean pressed = hovered();
                if(lastActive != pressed){
                    setPressed(pressed);
                    lastActive = pressed;
                }
                if(getAnimPercent() < 2)
                    return;
                int color = colorIdle;
                strokePaint.setColor(Color.argb((int) (255f * (getAnimPercent() / 100f)), Color.red(color), Color.green(color), Color.blue(color)));
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(Tools.dp(2));
                //strokePaint.setColor(color);
                float size = area.height()/2 - strokePaint.getStrokeWidth()/2f;
                //size *= getAnimPercent()/100f;

                if(Data.isSqircle()) //1 out of 3
                    canvas.drawPath(getSquircleCenterPath(area.centerX(), area.centerY(), size), strokePaint);
                else if(Data.isCircle())
                    canvas.drawCircle(area.centerX(), area.centerY(), size, strokePaint);
                else if(Data.isRect())
                    canvas.drawRect(area.centerX()-size, area.centerY()-size, area.centerX()+size, area.centerY()+size, strokePaint);
            }
        }
        class ActiveMarkPart extends  AnimationAdapter{
            Paint strokePaint;
            boolean lastActive = false;
            Path squircleCache = null;
            float squircleCacheSum = 0;

            public ActiveMarkPart() {
                setQuickOn(true);
                setAnimTime(300);
                strokePaint = new Paint();
                strokePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            }

            @Override
            protected void redraw() {
                MenuStripElement.this.invalidate();
            }

            @Override
            public void draw(Canvas canvas) {
                boolean pressed = activeChecker != null && activeChecker.isActive();
                if(lastActive != pressed){
                    setPressed(pressed);
                    lastActive = pressed;
                }
                if(getAnimPercent() < 2)
                    return;
                int color = colorActive;
                float size = area.height()/2;
                size *= getAnimPercent()/100f;
                strokePaint.setColor(Color.argb((int) (255f * (getAnimPercent() / 100f)), Color.red(color), Color.green(color), Color.blue(color)));
                //strokePaint.setColor(color);


                if(Data.isSqircle()) { //1 out of 3
                    if(squircleCache == null || squircleCacheSum != getSquircleCenterSum(area.centerX(), area.centerY(), size)) {
                        squircleCache = getSquircleCenterPath(area.centerX(), area.centerY(), size);
                        squircleCacheSum = getSquircleCenterSum(area.centerX(), area.centerY(), size);
                    }
                    canvas.drawPath(squircleCache, strokePaint);
                }
                else if(Data.isCircle())
                    canvas.drawCircle(area.centerX(), area.centerY(), size, strokePaint);
                else if(Data.isRect())
                    canvas.drawRect(area.centerX()-size, area.centerY()-size, area.centerX()+size, area.centerY()+size, strokePaint);
            }
        }
        class PressedMarkPart extends  AnimationAdapter{
            Paint strokePaint;
            boolean lastActive = false;

            public PressedMarkPart() {
                setQuickOn(true);
                setAnimTime(300);
                strokePaint = new Paint();
                strokePaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            }

            @Override
            protected void redraw() {
                MenuStripElement.this.invalidate();
            }

            @Override
            public void draw(Canvas canvas) {
                //boolean pressed = pressed;
                if(lastActive != pressed){
                    setPressed(pressed);
                    lastActive = pressed;
                }
                if(getAnimPercent() < 2)
                    return;
                int color = colorPressed;
                float size = area.height()/2;
                size *= getAnimPercent()/100f;
                strokePaint.setColor(Color.argb((int) (255f * (getAnimPercent() / 100f)), Color.red(color), Color.green(color), Color.blue(color)));
                //strokePaint.setColor(color);


                if(Data.isSqircle()) //1 out of 3
                    canvas.drawPath(getSquircleCenterPath(area.centerX(), area.centerY(), size), strokePaint);
                else if(Data.isCircle())
                    canvas.drawCircle(area.centerX(), area.centerY(), size, strokePaint);
                else if(Data.isRect())
                    canvas.drawRect(area.centerX()-size, area.centerY()-size, area.centerX()+size, area.centerY()+size, strokePaint);
            }
        }
    }
}
