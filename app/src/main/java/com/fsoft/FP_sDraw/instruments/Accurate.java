package com.fsoft.FP_sDraw.instruments;

import android.graphics.*;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;

import com.fsoft.FP_sDraw.*;
import com.fsoft.FP_sDraw.menu.CircleButtonNextToMenuButton;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.HintMenu;
import com.fsoft.FP_sDraw.menu.MainMenu;

/**
 * Инструмент обработки точного режима кисти
 * Created by Dr. Failov on 02.01.2015.
 * Upgraded by Dr. Failov on 11.02.2023.
 */
public class Accurate implements Instrument{
    private final DrawCore draw;
    private Instrument currentInstrument;

    float cursor_x;   //положение курсора в режиме кисти повыщенной точности в координатах экрана
    float cursor_y;
    float[] cursor_finger_last_x = new float[DrawCore.MULTITOUCH_MAX];
    float[] cursor_finger_last_y = new float[DrawCore.MULTITOUCH_MAX];
    int applyerID=-1; //ID пальца которым нажато на кнопку действия
    boolean cursor_draw = false;
    int cursor_button_size = Tools.dp(80);
    Bitmap cursor_bitmap = null;
    private final Paint cursor_paint = new Paint();

    private Bitmap button_bitmap = null;
    private final int button_bitmap_resource = R.drawable.settings_finger_hovering;
    private Thread button_bitmap_loading_thread = null;
    private final RectF button_frameRect = new RectF();
    private final Paint button_framePaint = new Paint();

    private final CircleButtonNextToMenuButton instrumentButton;
    private HintMenu hint = null;

    public Accurate(DrawCore draw) {
        this.draw = draw;
        currentInstrument = draw.brush;
        instrumentButton = new CircleButtonNextToMenuButton(draw, 1.2f, Data.store().DPI/7f, currentInstrument.getImageResourceID(), this::changeInstrument);

        cursor_x = Data.store().DPI;   //положение курсора в режиме кисти повыщенной точности
        cursor_y = Data.store().DPI;
        for(int i=0 ; i<DrawCore.MULTITOUCH_MAX ; i++){
            cursor_finger_last_x[i]=-1;
            cursor_finger_last_y[i]=-1;
        }
    }

    void changeInstrument(){
        currentInstrument.onDeselect();
        if(currentInstrument == draw.brush)
            currentInstrument = draw.erase;
        else if(currentInstrument == draw.erase)
            currentInstrument = draw.fill;
        else if(currentInstrument == draw.fill)
            currentInstrument = draw.line;
        else
            currentInstrument = draw.brush;
        instrumentButton.changeImage(currentInstrument.getImageResourceID());
        currentInstrument.onSelect();
    }

    public Instrument getCurrentInstrument() {
        return currentInstrument;
    }

    @Override
    public String getName() {
        return "accurate";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentAccurate);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.ic_cursor;
    }

    @Override
    public void onSelect() {
        if(currentInstrument != null){
            currentInstrument.onSelect();
        }
    }

    @Override
    public void onDeselect() {
        if(currentInstrument != null){
            currentInstrument.onDeselect();
        }
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        try{
            if(instrumentButton.onTouch(event)) {
                draw.redraw();
                return true;
            }
            if(hint != null && hint.processTouch(event))
                return true;
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_DOWN) {
                int reasonIndex=(event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
                int reasonID = event.getPointerId(reasonIndex);

                //if it button apply
                if(button_frameRect.contains(event.getX(reasonIndex), event.getY(reasonIndex))){
                    cursor_draw = true;
                    applyerID = reasonID;

                    MotionEvent e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, cursor_x, cursor_y, event.getPressure(0), event.getSize(0), 0,0,0,0,0);
                    if(currentInstrument != null)
                        currentInstrument.onTouch(e);
                }
                cursor_finger_last_x[reasonID] = event.getX(reasonIndex);
                cursor_finger_last_y[reasonID] = event.getY(reasonIndex);
            }
            if(!cursor_draw){
                //trigger zoom if over 2 fingers
                if(event.getPointerCount() > 1){
                    if(Data.tools.isFullVersion()) {
                        draw.setInstrument(draw.scale);
                        draw.scale.onTouch(event);
                        draw.scale.setInstrumentCallback(draw.accurate);
                    }
                    else
                        Data.tools.showBuyFullDialog();
                    return true;
                }
            }
            if(action == MotionEvent.ACTION_MOVE) {
                //move cursor
                for(int i=0 ; i<event.getPointerCount() ; i++){
                    int IDi=event.getPointerId(i);
                    if(cursor_finger_last_x[IDi] != -1 && IDi != applyerID){
                        cursor_x += (event.getX(i) - cursor_finger_last_x[IDi])*0.6;
                        cursor_y += (event.getY(i) - cursor_finger_last_y[IDi])*0.6;

                        cursor_finger_last_x[IDi] = event.getX(i);
                        cursor_finger_last_y[IDi] = event.getY(i);
                    }
                }
                //check
                correctCursorPosition();
                //draw
                if(cursor_draw){
                    MotionEvent e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, cursor_x, cursor_y, event.getPressure(0), event.getSize(0), 0,0,0,0,0);
                    if(currentInstrument != null)
                        currentInstrument.onTouch(e);
                }
            }
            else if(action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP){
                int reasonIndex=(event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
                int reasonID = event.getPointerId(reasonIndex);

                if(reasonID == applyerID){//убрали палец, которым рисовали
                    cursor_draw=false;
                    applyerID=-1;
                    MotionEvent e = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, cursor_x, cursor_y, event.getPressure(0), event.getSize(0), 0,0,0,0,0);
                    if(currentInstrument != null)
                        currentInstrument.onTouch(e);
                }
                else{
                    cursor_finger_last_x[reasonID]=-1;
                }
            }
            draw.redraw();
        } catch (Exception e){
            Logger.log("Draw.OnDraw.handlerAccurate", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Draw.OnDraw.handlerAccurate", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        //DRAW CURSOR
        if(cursor_bitmap == null)
            cursor_bitmap = Data.tools.getResizedBitmap(BitmapFactory.decodeResource(draw.context.getResources(), R.drawable.cursor), ((int)Data.store().DPI / 7), ((int)Data.store().DPI / 7));
        //canvas.drawBitmap(cursor_bitmap, draw.scale.ImageToScreenX(cursor_x), draw.scale.ImageToScreenY(cursor_y), cursor_paint);
        canvas.drawBitmap(cursor_bitmap, (cursor_x), (cursor_y), cursor_paint);

        //DRAW APPLY BUTTON
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
        float frameWidth = cursor_button_size;
        float frameHeight = cursor_button_size;
        float frameRoundness = Tools.dp(5);
        button_frameRect.set(marginLeft, marginTop, marginLeft+frameWidth, marginTop+frameHeight);
        button_framePaint.setAntiAlias(true);
        button_framePaint.setColor(MainMenu.transparentBackgroundColor);
        if(cursor_draw)
            button_framePaint.setAlpha(120);
        button_framePaint.setStyle(Paint.Style.FILL);
        if(Data.isRect())
            canvas.drawRect(button_frameRect, button_framePaint);
        else
            canvas.drawRoundRect(button_frameRect, frameRoundness, frameRoundness, button_framePaint);
        button_framePaint.setColor(Color.argb(70, 255,255,255));
        button_framePaint.setStyle(Paint.Style.STROKE);
        button_framePaint.setStrokeWidth(Data.store().DPI * 0.002f);
        if(Data.isRect())
            canvas.drawRect(button_frameRect, button_framePaint);
        else
            canvas.drawRoundRect(button_frameRect, frameRoundness, frameRoundness, button_framePaint);


        //DRAW IMAGE
        float iconWidth = cursor_button_size * 0.6f;
        float iconHeight = cursor_button_size * 0.6f;
        float iconPadding = (frameHeight-iconHeight)/2;
        if(button_bitmap == null && button_bitmap_loading_thread == null){
            button_bitmap_loading_thread = new Thread(() -> {
                try {
                    button_bitmap = Tools.decodeResource(draw.context.getResources(), button_bitmap_resource, iconWidth, iconHeight);
                    draw.redraw();
                    button_bitmap_loading_thread = null;
                }
                catch (Exception e){
                    button_bitmap_loading_thread = null;
                    Logger.log(e);
                }
            });
            button_bitmap_loading_thread.start();
        }
        if(button_bitmap != null) {
            button_framePaint.setAlpha(255);
            button_framePaint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(button_bitmap, marginLeft + iconPadding, marginTop + iconPadding, button_framePaint);
        }


        if(currentInstrument != null) {
            currentInstrument.onCanvasDraw(canvas, false);
        }

        //draw change instrument button
        if(drawUi)
            instrumentButton.draw(canvas);

        if(hint == null && drawUi)
            hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_accurate), R.drawable.ic_help, "ACCURATE", HintMenu.SHOW_TIMES);
        if(drawUi)
            hint.draw(canvas);
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
        return view -> {
            if(Data.tools.isFullVersion())
                draw.setInstrument(this);
            else
                Data.tools.showBuyFullDialog();
        };
    }

    void correctCursorPosition(){
        float visible_x_min = 0;
        float visible_x_max = draw.bitmap.getWidth();
        float visible_y_min = 0;
        float visible_y_max = draw.bitmap.getHeight();

        if(cursor_x < visible_x_min)
            cursor_x = visible_x_min;
        if(cursor_x > visible_x_max)
            cursor_x = visible_x_max;
        if(cursor_y < visible_y_min)
            cursor_y = visible_y_min;
        if(cursor_y > visible_y_max)
            cursor_y = visible_y_max;
    }
}
