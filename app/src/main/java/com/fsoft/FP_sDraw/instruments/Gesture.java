package com.fsoft.FP_sDraw.instruments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.SettingsScreen;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.HintMenu;

/**
 * новая хрень смециально для обработки жестов
 * Created by Dr. Failov on 29.12.2015.
 */
public class Gesture implements Instrument {
    private final DrawCore draw;
    private final Point gesture_start = new Point(-1,-1);
    private final Point gesture_end = new Point(-1, -1);
    private final Paint paint = new Paint();
    private final float threshold = Data.store().DPI/5;
    private final Icon top = new Icon(R.drawable.menu_settings, iconSize());
    private final Icon left = new Icon(R.drawable.menu_undo, iconSize());
    private final Icon right = new Icon(R.drawable.menu_redo, iconSize());
    private final Icon bottom = new Icon(R.drawable.menu_clear, iconSize());
    private final Icon center = new Icon(R.drawable.menu_text, iconSize());
    private final HintMenu hint;


    public Gesture(DrawCore draw) {
        this.draw = draw;
        hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_gesture), R.drawable.ic_help, "GESTURE", HintMenu.SHOW_TIMES);
        hint.setAllowHide(false);
    }
    @Override public String getName() {
        return "gesture";
    }
    @Override public String getVisibleName() {
        return "gesture";
    }
    @Override public int getImageResourceID() {
        return R.drawable.icon_tap;
    }
    @Override public void onSelect() {
        gesture_start.set(-1, -1);
        gesture_end.set(-1, -1);
        resetIcons();
    }
    @Override public void onDeselect() {

    }
    @Override public boolean onTouch(MotionEvent event) {
        //Logger.log("GESTURE RECEIVED EVENT " + event);
        int action = event.getAction();
        if(event.getPointerCount() > 1){
            //stopMoveTimer();
            if(Data.tools.isFullVersion()) {
                draw.setInstrument(draw.scale);
                draw.scale.onTouch(event);
            }
            else
                Data.tools.showBuyFullDialog();
            return true;
        }
        if(action == MotionEvent.ACTION_DOWN){
            gesture_start.set(event.getX(), event.getY());
            gesture_end.set(event.getX(), event.getY());
            draw.redraw();
            return true;
        }
        if(gesture_start.equals(-1, -1))
            return false;
        if(action == MotionEvent.ACTION_MOVE){
            gesture_end.set(event.getX(), event.getY());
            updateIcons();
            draw.redraw();
            return true;
        }
        if(action == MotionEvent.ACTION_UP){
            gesture_end.set(event.getX(), event.getY());
            processGesture(event);
            resetIcons();
            gesture_start.set(-1, -1);
            gesture_end.set(-1, -1);
            draw.redraw();
            return true;
        }
        if(action == MotionEvent.ACTION_CANCEL){
            gesture_start.set(-1, -1);
            gesture_end.set(-1, -1);
            draw.redraw();
            return true;
        }
        return false;
    }
    @Override public boolean onKey(KeyEvent event) {
        return false;
    }
    @Override public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        float gap = top.getSize()/3;
        RectF outerRect = new RectF();
        outerRect.bottom = bottom.getY()+bottom.getSize()+gap;
        outerRect.left = left.getX() - gap;
        outerRect.right = right.getX() + right.getSize() + gap;
        outerRect.top = top.getY() - gap;
        int rectColor = Color.argb(100, 0, 0, 0);
        paint.setColor(rectColor);
        paint.setAntiAlias(false);
        if(Data.isSqircle()) //1 of 3
            canvas.drawPath(Tools.getSquircleCenterPath(outerRect.centerX(), outerRect.centerY(), Math.max(outerRect.width(), outerRect.height())/2f), paint);
        else if(Data.isRect())
            canvas.drawRect(outerRect, paint);
        else
            canvas.drawRoundRect(outerRect, gap, gap, paint);

        drawGestureName(canvas);

        top.draw(canvas);
        bottom.draw(canvas);
        left.draw(canvas);
        right.draw(canvas);
        center.draw(canvas);
        hint.draw(canvas);
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
    private float gestureNameTextSize = -1;
    private void drawGestureName(Canvas canvas){
        //draw text
        String _text = getGestureName();
        Paint _paint = new Paint();
        float _size = gestureNameTextSize;
        if(_size == -1) {
            _size = Data.store().DPI / 7f;   //подобрать размер
            _paint.setTextSize(_size);
            while (_paint.measureText(_text) > draw.bitmap.getWidth()) {
                _size -= 1;
                _paint.setTextSize(_size);
            }
            _size -= 1;
            gestureNameTextSize = _size;
        }
        _paint.setTextSize(_size);
        _paint.setAntiAlias(true);
        float _x = (draw.bitmap.getWidth() / 2f) - (_paint.measureText(_text) / 2);
        float _y = draw.bitmap.getHeight() * (13f / 16f);
        //fill
        _paint.setStyle(Paint.Style.FILL);
        _paint.setColor(Color.WHITE);
        canvas.drawText(_text, _x, _y, _paint);
        //stroke
        _paint.setStyle(Paint.Style.STROKE);
        _paint.setStrokeWidth(1);
        _paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawText(_text, _x, _y, _paint);
    }
    private float iconSize(){
        return Data.store().DPI/5f;
    }
    private void resetIcons(){
        float coef = 1.5f;//distance
        float cx = draw.getWidth()/2f;
        float cy = draw.getHeight()/2f;
        float size = iconSize();

        float centerX = cx - size/2;
        float centerY = cy - size/2;
        center.setPosition(centerX, centerY);

        float leftX = centerX - size*coef;
        left.setPosition(leftX, centerY);

        float rightX = centerX + size*coef;
        right.setPosition(rightX, centerY);

        float topY = centerY - size*coef;
        top.setPosition(centerX, topY);

        float bottomY = centerY + size*coef;
        bottom.setPosition(centerX, bottomY);
    }
    private void updateIcons(){
        float coef = 1.5f;//distance
        float cx = draw.getWidth()/2f;
        float cy = draw.getHeight()/2f;
        float size = iconSize();
        float dx = gesture_end.x - gesture_start.x;
        dx = Math.signum(dx)*(float)Math.sqrt(Math.abs(dx))*5;
        float dy = gesture_end.y - gesture_start.y;
        dy = Math.signum(dy)*(float)Math.sqrt(Math.abs(dy))*5;

        float centerX = cx - size/2;
        float centerY = cy - size/2;

        if(isLeftGesture())
            centerX += dx;
        else if(isRightGesture())
            centerX += dx;
        else if(isTopGesture())
            centerY += dy;
        else if(isBottomGesture())
            centerY += dy;


        float leftX = centerX - size*coef;
        float rightX = centerX + size*coef;
        float topY = centerY - size*coef;
        float bottomY = centerY + size*coef;

        if(isLeftGesture())
            leftX += dx;
        else if(isRightGesture())
            rightX += dx;
        else if(isTopGesture())
            topY += dy;
        else if(isBottomGesture())
            bottomY += dy;

        center.setPosition(centerX, centerY);
        left.setPosition(leftX, centerY);
        right.setPosition(rightX, centerY);
        top.setPosition(centerX, topY);
        bottom.setPosition(centerX, bottomY);
    }
    private boolean isCenterGesture(){
        float dx = gesture_end.x - gesture_start.x;
        float dy = gesture_end.y - gesture_start.y;
        double length = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        return length < threshold;
    }
    private boolean isRightGesture(){
        float dx = gesture_end.x - gesture_start.x;
        float dy = gesture_end.y - gesture_start.y;
        return dx > threshold && Math.abs(dx) > Math.abs(dy);
    }
    private boolean isLeftGesture(){
        float dx = gesture_end.x - gesture_start.x;
        float dy = gesture_end.y - gesture_start.y;
        return dx < -threshold && Math.abs(dx) > Math.abs(dy);
    }
    private boolean isBottomGesture(){
        float dx = gesture_end.x - gesture_start.x;
        float dy = gesture_end.y - gesture_start.y;
        return dy > threshold && Math.abs(dx) < Math.abs(dy);
    }
    private boolean isTopGesture(){
        float dx = gesture_end.x - gesture_start.x;
        float dy = gesture_end.y - gesture_start.y;
        return dy < -threshold && Math.abs(dx) < Math.abs(dy);
    }
    private int getGestureNameId(){
        if(gesture_start.equals(-1,-1))
            return 0;
        if(isTopGesture())
            return R.string.menuSettings;
        if(isBottomGesture())
            return R.string.menuClear;
        if(isRightGesture())
            return R.string.menuRedo;
        if(isLeftGesture())
            return R.string.menuUndo;
        if(isCenterGesture())
            return R.string.TextInputMenuHeader;
        return 0;
    }
    private String getGestureName(){
        int id = getGestureNameId();
        if(id == 0)
            return "";
        return Data.tools.getResource(id);
    }
    private void processGesture(MotionEvent event){
        Tools.vibrate(draw.view);
        if(isLeftGesture())
            draw.undoProvider.undo();
        else if(isRightGesture())
            draw.undoProvider.redo();
        else if(isBottomGesture())
            draw.clear();
        else if(isTopGesture())
            draw.context.startActivity(new Intent(draw.context, SettingsScreen.class));
        else if(isCenterGesture()){
            draw.text.onSelect();
            draw.text.onTouch(event);
        }
    }

    class Icon {
        private final int resource ;
        private float x = -1;
        private float y = -1;
        private float aimX = 0;
        private float aimY = 0;
        private float width ;
        private float height ;
        private Thread movingThread = null;
        private Thread loadingThread = null;
        private Bitmap bitmap = null;
        private final Paint paint = new Paint();

        public Icon(int resource, float size) {
            this.resource = resource;
            width = size;
            height = size;
        }
        public void setSize(float newSize){
            width = newSize;
            height = newSize;
        }
        public void setPosition(float x, float y){
            if(this.x == -1)
                this.x = x;
            if(this.y == -1)
                this.y = y;
            aimX = x;
            aimY = y;
            if (movingThread == null) {
                movingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        moveToAimAsync();
                    }
                });
                movingThread.start();
            }
        }
        public void draw(Canvas canvas){
            if(bitmap == null && loadingThread == null){
                startLoading();
            }
            if(bitmap != null){
                canvas.drawBitmap(bitmap, x, y, paint);
            }
        }
        public float getX() {
            return x;
        }
        public float getY() {
            return y;
        }
        public float getSize(){
            return width;
        }

        private void startLoading(){
            if(loadingThread == null){
                loadingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        loadIconAsync();
                    }
                });
                loadingThread.start();
            }
        }
        private void loadIconAsync(){
            try{
                bitmap = decodeResource(resource, (int)width, (int)height);
                draw.redraw();
            }
            catch (Exception e){
                Logger.log("Error loading icon " + resource + " : " + e);
                e.printStackTrace();
            }
        }
        private Bitmap decodeResource(int resId, int required_w, int required_h) {
            try{
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                //options.inScreenDensity = Data.store().DPI*2;
                BitmapFactory.decodeResource(draw.context.getResources(), resId, options);
                // Calculate inSampleSize
                final int height = options.outHeight;
                final int width = options.outWidth;
                if (height > required_h && width > required_w) {
                    // Calculate ratios of height and width to requested height and width
                    int heightRatio = Math.max(1, Math.round((float) height / required_h)-1);
                    int widthRatio = Math.max(1, Math.round((float) width / required_w)-1);
                    // Choose the smallest ratio as inSampleSize value, this will guarantee a final image with both dimensions larger than or equal to the requested height and width.
                    int iss = Math.min(heightRatio, widthRatio);
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
                    result = BitmapFactory.decodeResource(draw.context.getResources(), resId, options);
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
        private double distanceToAim(){
            return distance(aimX, x, aimY, y);
        }
        private double distance(float x1, float x2, float y1, float y2){
            float dx = x1-x2;
            float dy = y1-y2;
            return Math.sqrt(dx*dx+dy*dy);
        }
        private void moveToAimAsync(){
            int threshold = 10;
            while(distanceToAim() > threshold){
                float dx = aimX-x;
                float dy = aimY-y;
                x += dx*0.2f;
                y += dy*0.2f;
                draw.redraw();
                sleep(10);
            }
            movingThread = null;
            x = aimX;
            y = aimY;
            draw.redraw();
        }
        private void sleep(int ms){
            try {
                Thread.sleep(ms);
            }
            catch (Exception e){
                //do nothing
            }
        }
    }
}
