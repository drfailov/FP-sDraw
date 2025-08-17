package com.fsoft.FP_sDraw.menu;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 12.01.2015.
 */

public class AnimatedButton extends TextView {
    public interface DynamicColor{int getColor();}
    DynamicColor backgroudColor = () -> Color.rgb(67,78,84);//Color.rgb(67, 86, 92);
    Paint borderPaint = new Paint();
    int circleOpacityMax = 160;
    int circleDelay = 20;
    RectF buttonRect = new RectF();

    int circleX = -1;
    int circleY = -1;
    int circleSize = 0;
    int circleOpacity = circleOpacityMax;
    Timer circleTimer = null;
    Paint circlePaint = new Paint();

    public AnimatedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedButton(Context context) {
        super(context);
        init();
    }

    private void init(){
        setTextColor(Color.WHITE);
        setBackgroundColor(Color.TRANSPARENT);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        setPadding(Tools.dp(15), Tools.dp(8), Tools.dp(15), Tools.dp(10));
        setTextSize(14);
        borderPaint.setAntiAlias(Build.VERSION.SDK_INT > 14);


        // необходимо для корректной работы
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        //borderPaint.setStyle(Paint.Style.STROKE);
        //borderPaint.setStrokeWidth(2);
    }
    @Override
    protected void onDetachedFromWindow() {
        if(circleTimer != null)
            circleTimer.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text.toString().toUpperCase(), type);
    }

    Bitmap mask = null;

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        invalidate();
        return super.onHoverEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        buttonRect.set(0, 0, getWidth() - 1, getHeight() - 1);
        float roundness = Tools.dp(6);

        //draw background
        borderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        borderPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(backgroudColor.getColor());
        canvas.drawRect(buttonRect, borderPaint);
        //draw line at bottom
        int lineColor = Color.argb(20, 255,255,255);
        float lineWidth = Tools.dp(2);
        float y = getHeight() - 1 - lineWidth/2f;
        borderPaint.setColor(lineColor);
        borderPaint.setStrokeWidth(lineWidth);
        canvas.drawLine(0, y, getWidth() - 1, y, borderPaint);

        //draw circle
        if(circleOpacity > 10) {
            circlePaint.setColor(Color.argb(circleOpacity, 255, 255, 255));
            canvas.drawCircle(circleX, circleY, circleSize, circlePaint);
        }
        //draw hover selection
        if(Build.VERSION.SDK_INT >= 14){
            if(isHovered()){
                borderPaint.setColor(Color.argb(150, 255, 255, 255));
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(Tools.dp(3));
                canvas.drawRoundRect(buttonRect, roundness, roundness, borderPaint);
                //canvas.drawRect(buttonRect, borderPaint);
            }
        }
        //crop roundrect frame if needed
        if(!Data.isRect() && (Build.VERSION.SDK_INT > 14)) {
            if (mask == null || mask.getWidth() != buttonRect.width() || mask.getHeight() != buttonRect.height()) {
                borderPaint.setColor(Color.WHITE);
                borderPaint.setStyle(Paint.Style.FILL);
                mask = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(mask);

                c.drawRoundRect(buttonRect, roundness, roundness, borderPaint);
            }
            borderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawBitmap(mask, 0, 0, borderPaint);
        }

        super.onDraw(canvas);
    }

    private void beginHighlight(int x, int y){
        if(circleTimer != null){
            circleTimer.cancel();
            circleTimer.purge();
            circleTimer = null;
        }
        circleX = x;
        circleY = y;
        circleSize = 20;
        circleOpacity = circleOpacityMax;
        circleTimer = new Timer();
        circleTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                circleSize *= 1.5;
                circleOpacity -= 15;
                if(circleOpacity <= 0) {
                    circleOpacity = 0;
                    circleTimer.cancel();
                    circleTimer.purge();
                    circleTimer = null;
                }
                postInvalidate();
            }
        }, circleDelay, circleDelay);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!Data.tools.isAllowedDeviceForUi(event))
            return false;
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        if(action == MotionEvent.ACTION_DOWN){                                         //залить черным
            beginHighlight((int)event.getX(), (int)event.getY());
        }
        return super.onTouchEvent(event);
    }

    @Override public boolean performClick() {
        //click();
        super.performClick();
        return true;
    }
//    public void click(){
//
//    }

    public void setBackgroundDynamicColor(DynamicColor backgroudColor) {
        this.backgroudColor = backgroudColor;
        //backgroudColor = Data.tools.getGridColor(backgroudColor);
        //borderPaint.setColor(backgroudColor);
    }
}
