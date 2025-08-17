package com.fsoft.FP_sDraw.instruments;

import android.graphics.*;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.*;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.menu.MainMenu;

/**
 * Инструмент обработки точного рисования кистью
 * Created by Dr. Failov on 02.01.2015.
 */
public class Scale implements Instrument{
    private final DrawCore draw;
    public float scale_size = 1.0f;
    public float scale_offset_x = 0;
    public float scale_offset_y = 0;
    public Matrix scale_matrix = new Matrix();
    public Instrument instrumentCallback = null; //this instrument will be set when touch releases
    public final float pixelModeThreshold = 32f;

    //touch processing
    private boolean scale_move_lock = false;
    private float scale_touch_0_x = -1;
    private float scale_touch_0_y = -1;
    private float scale_touch_1_x = -1;
    private float scale_touch_1_y = -1;
    private float scale_touch_size = -1;
    private float scale_touch_offset_x = scale_offset_x;
    private float scale_touch_offset_y = scale_offset_y;

    //drawHint
    private Bitmap scale_hint_bitmap = null;
    private int scale_hint_bitmap_resource = R.drawable.menu_scale;
    private Thread scale_hint_bitmap_loading_thread = null;
    private final Paint scale_hint_paint = new Paint();
    public final RectF scale_hint_frameRect = new RectF();
    private final Paint scale_text_paint = new Paint();

    //scrollbars
    private Paint scale_scrollbar_paint = null;
    private Paint scale_scrollbarStroke_paint = null;
    private float scale_scrollbar_size;
    private float scale_scrollbar_offset;
    private float scale_scrollbar_round;

    //buttons
    private Paint scale_button_paint = null;
    private Paint scale_buttonStroke_paint = null;
    private RectF scale_ButtonPlus = null;
    private RectF scale_ButtonMinus = null;
    private float scale_buttonRound;
    private Paint scale_buttonText_paint = null;
    private int scale_buttonTextPlus_x;
    private int scale_buttonTextPlus_y;
    private int scale_buttonTextMinus_x;
    private int scale_buttonTextMinus_y;

    private float pixelModeLastScaleValue = scale_size;
    private int pixelModeLastBrushSize;
    private int pixelModeLastEraserSize;
    private boolean pixelModeLastAntialiasingValue;
    private boolean pixelModeLastSmoothingValue;
    private int pixelModeLastOpacity;
    private int pixelModeLastThicknessMode;


    //трансформации координат
    public float ScreenToImageX(float screenX) { /*IMAGE  <-  SCREEN*/
        return ((screenX - scale_offset_x)/scale_size);
    }
    public float ScreenToImageY(float screenY) { /*IMAGE  <-  SCREEN*/
        return ((screenY - scale_offset_y)/scale_size);
    }
    public float ImageToScreenX(float imageX){  /*IMAGE  ->  SCREEN*/
        float out = imageX;
        out *= scale_size;
        out += scale_offset_x;
        return out;
    }
    public float ImageToScreenY(float imageY){  /*IMAGE  ->  SCREEN*/
        float out = imageY;
        out *= scale_size;
        out += scale_offset_y;
        return out;
    }
    public RectF ImageToScreen(RectF in){
        return new RectF(ImageToScreenX(in.left), ImageToScreenY(in.top), ImageToScreenX(in.right), ImageToScreenY(in.bottom));
    }
    public Rect ImageToScreen(Rect in){
        return new Rect((int)ImageToScreenX(in.left), (int)ImageToScreenY(in.top), (int)ImageToScreenX(in.right), (int)ImageToScreenY(in.bottom));
    }
    public Point ImageToScreen(com.fsoft.FP_sDraw.common.Point in){
        if(in == null)
            return null;
        return new Point(ImageToScreenX(in.x), ImageToScreenY(in.y));
    }
    public MotionEvent scale_transformMotionEvent(MotionEvent in_event){
        if(scale_size != 1.0f) {
            try{
                if(Build.VERSION.SDK_INT >= 9) {
                    int pointerCount = in_event.getPointerCount();
                    int[] pointerIDs = new int[pointerCount];
                    MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[pointerCount];
                    for (int i = 0; i < pointerCount; i++) {
                        pointerCoords[i] = new MotionEvent.PointerCoords();
                        pointerIDs[i] = in_event.getPointerId(i);
                        in_event.getPointerCoords(i, pointerCoords[i]);
                        pointerCoords[i].x = ScreenToImageX(pointerCoords[i].x);
                        pointerCoords[i].y = ScreenToImageY(pointerCoords[i].y);
                    }
                    return MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis(), in_event.getAction(), in_event.getPointerCount(), pointerIDs, pointerCoords, in_event.getMetaState(), in_event.getXPrecision(), in_event.getYPrecision(), in_event.getDeviceId(), in_event.getEdgeFlags(), in_event.getSource(), in_event.getFlags());
                }
                else
                    throw new Exception("Not supported. Use CATCH block.");
            }catch (Throwable throwable){
                float x = ((in_event.getX() - scale_offset_x)/scale_size);
                float y = ((in_event.getY() - scale_offset_y)/scale_size);
                return MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis(), in_event.getAction(), x, y, 0);
            }
        }
        else
            return in_event;
    }
    void scale_checkOffset(){
        float imageSizeX = draw.bitmap.getWidth() * scale_size;
        float imageSizeY = draw.bitmap.getHeight() * scale_size;
        float displaySizeY = draw.bitmap.getHeight();
        float displaySizeX = draw.bitmap.getWidth();
        float minimumX = displaySizeX - imageSizeX;
        float maximumX = 0;
        float minimumY = displaySizeY - imageSizeY;
        float maximumY = 0;

        if(scale_offset_x < minimumX)
            scale_offset_x = minimumX;
        if(scale_offset_x > maximumX)
            scale_offset_x = maximumX;

        if(scale_offset_y < minimumY)
            scale_offset_y = minimumY;
        if(scale_offset_y > maximumY)
            scale_offset_y = maximumY;
    }
    void scale_checkSize(){
        if(scale_size < 1.1f)//примагничивание к нормальному размеру
            scale_size = 1;
        float max = 200;//200х зум
        if(scale_size > max)
            scale_size = max;
    }
    boolean isPixelMode(){
        return scale_size > pixelModeThreshold;
    }
    void scale_managePixelMode(){
        try {
            //PixelArt automatic settings
            if (isPixelMode() && pixelModeLastScaleValue <= pixelModeThreshold) { //enter pixel mode
                scale_hint_bitmap_resource = R.drawable.ic_mosaic;
                scale_hint_bitmap = null;
                pixelModeLastBrushSize = (Integer) Data.get(Data.brushSizeInt());
                pixelModeLastEraserSize = (Integer) Data.get(Data.eraserSizeInt());
                pixelModeLastAntialiasingValue = (Boolean) Data.get(Data.antialiasingBoolean());
                pixelModeLastSmoothingValue = (Boolean) Data.get(Data.smoothingBoolean());
                pixelModeLastOpacity = (Integer) Data.get(Data.brushOpacityInt());
                pixelModeLastThicknessMode = (Integer) Data.get(Data.manageMethodInt());
                Data.save(1, Data.brushSizeInt());
                Data.save(1, Data.eraserSizeInt());
                Data.save(255, Data.brushOpacityInt());
                Data.save(Data.MANAGE_METHOD_CONSTANT, Data.manageMethodInt());
                Data.save(false, Data.antialiasingBoolean());
                Data.save(false, Data.smoothingBoolean());
            }

            if (!isPixelMode() && pixelModeLastScaleValue > pixelModeThreshold) { //exit pixel mode
                scale_hint_bitmap_resource = R.drawable.menu_scale;
                scale_hint_bitmap = null;

                Data.save(pixelModeLastBrushSize, Data.brushSizeInt());
                Data.save(pixelModeLastEraserSize, Data.eraserSizeInt());
                Data.save(pixelModeLastOpacity, Data.brushOpacityInt());
                Data.save(pixelModeLastThicknessMode, Data.manageMethodInt());
                Data.save(pixelModeLastAntialiasingValue, Data.antialiasingBoolean());
                Data.save(pixelModeLastSmoothingValue, Data.smoothingBoolean());
            }

            pixelModeLastScaleValue = scale_size;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void immediatelyDisablePixelMode(){
        Logger.log("immediatelyDisablePixelMode!");
        scale_hint_bitmap_resource = R.drawable.menu_scale;
        scale_hint_bitmap = null;

        Data.save(pixelModeLastBrushSize, Data.brushSizeInt(), true);
        Data.save(pixelModeLastEraserSize, Data.eraserSizeInt(), true);
        Data.save(pixelModeLastOpacity, Data.brushOpacityInt(), true);
        Data.save(pixelModeLastThicknessMode, Data.manageMethodInt(), true);
        Data.save(pixelModeLastAntialiasingValue, Data.antialiasingBoolean(), true);
        Data.save(pixelModeLastSmoothingValue, Data.smoothingBoolean(), true);
    }
    int scale_getPointerIndex(MotionEvent event, int ID){
        int pointerCount = event.getPointerCount();
        for(int i=0; i<pointerCount; i++){
            if(event.getPointerId(i) == ID)
                return i;
        }
        return 0;
    }
    public void drawScalePercentage(Canvas canvas){
        //draw background grid
        float scaleBackgroundGridOpacityZero = 25;
        float scaleBackgroundGridOpacityFull = 40;
        scale_hint_paint.setStyle(Paint.Style.STROKE);
        scale_hint_paint.setStrokeWidth(1);
        scale_hint_paint.setColor(Color.WHITE);
        try {
            if (Tools.isLightColor((Integer) Data.get(Data.backgroundColorInt())))
                scale_hint_paint.setColor(Color.BLACK);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        if(scale_size > scaleBackgroundGridOpacityZero){
            float opacity = 40 * (scale_size - scaleBackgroundGridOpacityZero) / (scaleBackgroundGridOpacityFull-scaleBackgroundGridOpacityZero);
            scale_hint_paint.setAlpha((int)opacity);
            float offsetX = scale_size - (ScreenToImageX(0) % 1f) * scale_size;
            for (float x = offsetX; x < draw.getWidth(); x += scale_size)
                canvas.drawLine(x, 0, x, draw.getHeight(), scale_hint_paint);
            float offsetY = scale_size - (ScreenToImageY(0) % 1f) * scale_size;
            for (float y = offsetY; y < draw.getHeight(); y += scale_size)
                canvas.drawLine(0, y, draw.getWidth(), y, scale_hint_paint);
        }
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
        float frameWidth = Tools.dp(90);
        float frameHeight = Tools.dp(35);
        float frameRoundness = Tools.dp(5);
        scale_hint_frameRect.set(marginLeft, marginTop, marginLeft+frameWidth, marginTop+frameHeight);
        scale_hint_paint.setAntiAlias(true);
        scale_hint_paint.setColor(MainMenu.transparentBackgroundColor);
        scale_hint_paint.setStyle(Paint.Style.FILL);
        if(Data.isRect())
            canvas.drawRect(scale_hint_frameRect, scale_hint_paint);
        else
            canvas.drawRoundRect(scale_hint_frameRect, frameRoundness, frameRoundness, scale_hint_paint);
        scale_hint_paint.setColor(Color.argb(70, 255,255,255));
        scale_hint_paint.setStyle(Paint.Style.STROKE);
        scale_hint_paint.setStrokeWidth(Data.store().DPI * 0.002f);
        if(Data.isRect())
            canvas.drawRect(scale_hint_frameRect, scale_hint_paint);
        else
            canvas.drawRoundRect(scale_hint_frameRect, frameRoundness, frameRoundness, scale_hint_paint);


        //DRAW IMAGE
        float iconWidth = Tools.dp(20);
        float iconHeight = Tools.dp(20);
        float iconPadding = (frameHeight-iconHeight)/2;
        if(scale_hint_bitmap == null && scale_hint_bitmap_loading_thread == null){
            scale_hint_bitmap_loading_thread = new Thread(() -> {
                try {
                    scale_hint_bitmap = Tools.decodeResource(draw.context.getResources(), scale_hint_bitmap_resource, iconWidth, iconHeight);
                    draw.redraw();
                    scale_hint_bitmap_loading_thread = null;
                }
                catch (Exception e){
                    Logger.log(e);
                    scale_hint_bitmap_loading_thread = null;
                }
            });
            scale_hint_bitmap_loading_thread.start();
        }
        if(scale_hint_bitmap != null) {
            scale_hint_paint.setAlpha(255);
            scale_hint_paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(scale_hint_bitmap, marginLeft + iconPadding, marginTop + iconPadding, scale_hint_paint);
        }

        //DRAW TEXT
        float scale_text_size = Tools.sp(15);
        String text = (int)(scale_size*100) + "%";
        scale_text_paint.setTextSize(scale_text_size);
        scale_text_paint.setAntiAlias(true);
        scale_text_paint.setColor(Color.WHITE);
        scale_text_paint.setStyle(Paint.Style.FILL);
        float textWidth = scale_text_paint.measureText(text);
        float textHeight = scale_text_paint.getTextSize();
        float textX = scale_hint_frameRect.right - iconPadding - textWidth;
        float textY = scale_hint_frameRect.top + textHeight*0.8f + (scale_hint_frameRect.height()-textHeight)/2;
        canvas.drawText(text, textX, textY, scale_text_paint);
    }
    public void setInstrumentCallback(Instrument instrument){
        instrumentCallback = instrument;
    }

    public Scale(DrawCore d) {
        draw = d;
    }

    @Override
    public String getName() {
        return "scale";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentScale);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.menu_scale;
    }

    @Override
    public void onSelect() {

    }

    @Override
    public void onDeselect() {

    }

    @Override
    public boolean onTouch(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        int pointer_count = event.getPointerCount();
        if(action == MotionEvent.ACTION_POINTER_DOWN && pointer_count == 2){
            scale_touch_size = scale_size;
            scale_touch_offset_x = scale_offset_x;
            scale_touch_offset_y = scale_offset_y;
            scale_touch_0_x = event.getX(scale_getPointerIndex(event, 0));
            scale_touch_0_y = event.getY(scale_getPointerIndex(event, 0));
            scale_touch_1_x = event.getX(scale_getPointerIndex(event, 1));
            scale_touch_1_y = event.getY(scale_getPointerIndex(event, 1));
        }
        else if(action == MotionEvent.ACTION_MOVE && pointer_count == 2){
            //scaling
            float dx = Math.abs(event.getX(scale_getPointerIndex(event, 0)) - event.getX(scale_getPointerIndex(event, 1)));
            float dy = Math.abs(event.getY(scale_getPointerIndex(event, 0)) - event.getY(scale_getPointerIndex(event, 1)));
            if(dx == 0 || dy == 0)
                return true;
            float d_now = (float)Math.sqrt(dx*dx+dy*dy);
            dx = Math.abs(scale_touch_0_x - scale_touch_1_x);
            dy = Math.abs(scale_touch_0_y - scale_touch_1_y);
            if(dx == 0 || dy == 0)
                return true;
            float d_old = (float)Math.sqrt(dx*dx+dy*dy);
            scale_size = scale_touch_size * (d_now/d_old);
            scale_checkSize();
            scale_managePixelMode();

            //moving
            //calculate direct moving
            float cx_old = (scale_touch_0_x + scale_touch_1_x) / 2;
            float cy_old = (scale_touch_0_y + scale_touch_1_y) / 2;
            float cx_now = (event.getX(scale_getPointerIndex(event, 0)) + event.getX(scale_getPointerIndex(event, 1))) / 2;
            float cy_now = (event.getY(scale_getPointerIndex(event, 0)) + event.getY(scale_getPointerIndex(event, 1))) / 2;
            dx = cx_now - cx_old;
            dy = cy_now - cy_old;

            //calculate scaling offset
            float now_onImageOffset_X = ((cx_old - scale_touch_offset_x)/scale_touch_size);//КООРДИНАТЫ НА РИСУНКЕ, БЛЯТЬ!
            float now_onImageOffset_Y = ((cy_old - scale_touch_offset_y)/scale_touch_size);
            float oldSizeX = now_onImageOffset_X * scale_touch_size;
            float oldSizeY = now_onImageOffset_Y * scale_touch_size;
            float newSizeX = now_onImageOffset_X * scale_size;
            float newSizeY = now_onImageOffset_Y * scale_size;
            dx -= (newSizeX - oldSizeX);
            dy -= (newSizeY - oldSizeY);
            //apply offset
            scale_offset_x = scale_touch_offset_x + dx;
            scale_offset_y = scale_touch_offset_y + dy;
            scale_checkOffset();
            draw.redraw();
        }
        else if(action == MotionEvent.ACTION_POINTER_UP && pointer_count == 2){
            scale_move_lock = true;
        }
        else if(action == MotionEvent.ACTION_DOWN && pointer_count == 1){
            if(!Data.tools.isAllowedDeviceForUi(event))
                return false;
            if((Boolean)Data.get(Data.showScaleButtonsBoolean())){
                boolean plus = scale_ButtonPlus.contains(event.getX(), event.getY());
                boolean minus = scale_ButtonMinus.contains(event.getX(), event.getY());
                if(plus || minus){
                    Tools.vibrate(draw.view);
                    float scale_old = scale_size;
                    if(plus)
                        scale_size *= 1.5f;
                    else //minus
                        scale_size /= 1.5f;
                    scale_checkSize();
                    scale_managePixelMode();

                    float cx_old = ((draw.bitmap.getWidth()/2f - scale_offset_x)/scale_old);
                    float cy_old = ((draw.bitmap.getHeight()/2f - scale_offset_y)/scale_old);
                    float oldSizeX = cx_old * scale_old;
                    float oldSizeY = cy_old * scale_old;
                    float newSizeX = cx_old * scale_size;
                    float newSizeY = cy_old * scale_size;
                    scale_offset_x -= newSizeX - oldSizeX;
                    scale_offset_y -= newSizeY - oldSizeY;
                    scale_checkOffset();
                    draw.redraw();
                }
            }
            scale_touch_size = scale_size;
            scale_touch_offset_x = scale_offset_x;
            scale_touch_offset_y = scale_offset_y;
            scale_touch_0_x = event.getX(scale_getPointerIndex(event, 0));
            scale_touch_0_y = event.getY(scale_getPointerIndex(event, 0));
            scale_move_lock = false;
        }
        else if(action == MotionEvent.ACTION_MOVE && pointer_count == 1 && !scale_move_lock){
            if(!Data.tools.isAllowedDeviceForUi(event))
                return false;
            //moving
            float cx_old = scale_touch_0_x;
            float cy_old = scale_touch_0_y;
            float cx_now = event.getX(scale_getPointerIndex(event, 0));
            float cy_now = event.getY(scale_getPointerIndex(event, 0));
            float dx = cx_now-cx_old;
            float dy = cy_now-cy_old;
            scale_offset_x = scale_touch_offset_x + dx;
            scale_offset_y = scale_touch_offset_y + dy;
            scale_checkOffset();
            draw.redraw();
        }
        else if(action == MotionEvent.ACTION_UP){
            if(instrumentCallback != null){
                draw.setInstrument(instrumentCallback);
                instrumentCallback = null;
            }
        }
        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {

    /*
    Отрисовка подсказки жестов здесь не требуется поскольку она отображаетя в Main.java если активен инструмент масштабирование
     */
        //drawScalePersentage(canvas);

        //DRAW SCROLLBARS
        if(scale_size != 1.0f) {
            if (scale_scrollbar_paint == null) {
                scale_scrollbar_paint = new Paint();
                scale_scrollbar_paint.setAntiAlias(true);
                scale_scrollbar_paint.setColor(Color.argb(150, 200, 200, 200));
                scale_scrollbar_paint.setStyle(Paint.Style.FILL);

                scale_scrollbarStroke_paint = new Paint();
                scale_scrollbarStroke_paint.setAntiAlias(true);
                scale_scrollbarStroke_paint.setColor(Color.BLACK);
                scale_scrollbarStroke_paint.setStrokeWidth(0.5f);
                scale_scrollbarStroke_paint.setStyle(Paint.Style.STROKE);

                scale_scrollbar_size = Data.store().DPI / 50;
                scale_scrollbar_offset = Data.store().DPI / 100;
                scale_scrollbar_round = Data.store().DPI / 50;
            }
            float visible_x_min = ((0 - scale_offset_x) / scale_size);
            float visible_x_max = ((draw.bitmap.getWidth() - scale_offset_x) / scale_size);
            float visible_y_min = ((0 - scale_offset_y) / scale_size);
            float visible_y_max = ((draw.bitmap.getHeight() - scale_offset_y) / scale_size);
            //draw scrolls
            RectF verticalRect = new RectF(draw.bitmap.getWidth() - scale_scrollbar_offset - scale_scrollbar_size, visible_y_min, draw.bitmap.getWidth() - scale_scrollbar_offset, visible_y_max);
            canvas.drawRoundRect(verticalRect, scale_scrollbar_round, scale_scrollbar_round, scale_scrollbar_paint);
            canvas.drawRoundRect(verticalRect, scale_scrollbar_round, scale_scrollbar_round, scale_scrollbarStroke_paint);
            //draw horizontal
            RectF horizontalRect = new RectF(visible_x_min, draw.bitmap.getHeight() - scale_scrollbar_offset - scale_scrollbar_size, visible_x_max, draw.bitmap.getHeight() - scale_scrollbar_offset);
            canvas.drawRoundRect(horizontalRect, scale_scrollbar_round, scale_scrollbar_round, scale_scrollbar_paint);
            canvas.drawRoundRect(horizontalRect, scale_scrollbar_round, scale_scrollbar_round, scale_scrollbarStroke_paint);
        }

        //DRAW BUTTONS
        if((Boolean)Data.get(Data.showScaleButtonsBoolean())){
            if(scale_ButtonPlus == null){
                float buttonOffset = Data.store().DPI / 20;
                float buttonSize = Data.store().DPI / 3;
                scale_buttonRound = Data.store().DPI / 20;

                scale_ButtonPlus = new RectF(
                        buttonOffset,
                        (draw.bitmap.getHeight()/2f) - buttonOffset - buttonSize,
                        buttonOffset + buttonSize,
                        (draw.bitmap.getHeight()/2f) - buttonOffset
                );
                scale_ButtonMinus = new RectF(
                        buttonOffset,
                        (draw.bitmap.getHeight()/2f) + buttonOffset,
                        buttonOffset + buttonSize,
                        (draw.bitmap.getHeight()/2f) + buttonOffset + buttonSize
                );
                //paint
                scale_button_paint = new Paint();
                scale_button_paint.setAntiAlias(true);
                scale_button_paint.setColor(Color.argb(150, 200, 200, 200));
                scale_button_paint.setStyle(Paint.Style.FILL);
                //strokepaint
                scale_buttonStroke_paint = new Paint();
                scale_buttonStroke_paint.setAntiAlias(true);
                scale_buttonStroke_paint.setColor(Color.BLACK);
                scale_buttonStroke_paint.setStyle(Paint.Style.STROKE);
                //buttontext
                scale_buttonText_paint = new Paint();
                scale_buttonText_paint.setColor(Color.BLACK);
                scale_buttonText_paint.setAntiAlias(true);
                scale_buttonText_paint.setTextSize(Data.store().DPI / 3);

                scale_buttonTextPlus_x = (int)(scale_ButtonPlus.centerX() - scale_buttonText_paint.measureText("+")/2);
                scale_buttonTextPlus_y = (int)(scale_ButtonPlus.centerY() + scale_buttonText_paint.getTextSize()/3);
                scale_buttonTextMinus_x = (int)(scale_ButtonMinus.centerX() - scale_buttonText_paint.measureText("-")/2);
                scale_buttonTextMinus_y = (int)(scale_ButtonMinus.centerY() + scale_buttonText_paint.getTextSize()/3);
            }
            canvas.drawRoundRect(scale_ButtonPlus, scale_buttonRound, scale_buttonRound, scale_button_paint);
            canvas.drawRoundRect(scale_ButtonPlus, scale_buttonRound, scale_buttonRound, scale_buttonStroke_paint);
            canvas.drawText("+", scale_buttonTextPlus_x, scale_buttonTextPlus_y, scale_buttonText_paint);

            canvas.drawRoundRect(scale_ButtonMinus, scale_buttonRound, scale_buttonRound, scale_button_paint);
            canvas.drawRoundRect(scale_ButtonMinus, scale_buttonRound, scale_buttonRound, scale_buttonStroke_paint);
            canvas.drawText("-", scale_buttonTextMinus_x, scale_buttonTextMinus_y, scale_buttonText_paint);
        }

        //callback UI
        if(instrumentCallback != null)
            instrumentCallback.onCanvasDraw(canvas, drawUi);
    }

    @Override
    public boolean isActive() {
        return draw.currentInstrument == this;
    }

    @Override  public boolean isVisibleToUser() {
        return false;
    }

    @Override
    public View.OnClickListener getOnClickListener() {
        return view -> {
            if(Data.tools.isFullVersion())
                draw.setInstrument(getName());
            else
                Data.tools.showBuyFullDialog();
        };
    }
}
