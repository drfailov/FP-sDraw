package com.fsoft.FP_sDraw.menu;

/*


  Created by Dr. Failov on 13.11.2015.
 */

import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterPath;
import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterSum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.MyImageView;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.ArrayList;


public class MyCheckBox extends LinearLayout {
    private LinearLayout linearLayout;
    private TextView textView;
    private Check check = null;
    private MyImageView imageView;



    public MyCheckBox(Context context, boolean locked) {
        super(context);
        setWillNotDraw(false);
        init(locked);
    }

    public MyCheckBox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        init(false);
    }

    @Override protected void dispatchSetPressed(boolean pressed) {
        if(!isEnabled())
            return;
        if(check != null) {
            setBackgroundColor(Color.argb(pressed ? 50 : 0, 255, 255, 255));
            check.setPressed(pressed);
            invalidate();
        }
        super.dispatchSetPressed(pressed);
    }
    @Override public void setHovered(boolean hovered) {
        if(!isEnabled())
            return;
        if(check != null) {
            setBackgroundColor(Color.argb(hovered ? 10 : 0, 255, 255, 255));
            invalidate();
        }
        super.setHovered(hovered);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!Data.tools.isAllowedDeviceForUi(event))
            return false;
        return super.onTouchEvent(event);
    }

    public void setText(String text){
        if(textView != null)
            textView.setText(text);
    }
    public void addHintView(View v){
        v.setPadding(0, 0, 0, 0);
        if(linearLayout != null)
            linearLayout.addView(v);
    }
    public void setChecked(boolean checked){
        if(check != null) {
            check.setChecked(checked);
            invalidate();
        }
    }
    public boolean getChecked(){
        if(check == null)
            return false;
        return check.getChecked();
    }
    public void setImage(int res){
        if(imageView != null)
            imageView.setDrawable(res);
    }
    public boolean isChecked(){
        return getChecked();
    }
    @Override public boolean performClick() {
        setChecked(!getChecked());
        return super.performClick();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setPadding(Tools.dp(10), Tools.dp(5), Tools.dp(10), Tools.dp(5));
    }

    @SuppressLint("SetTextI18n")
    protected void init(boolean locked){
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(0, Tools.dp(0), Tools.dp(10), Tools.dp(0));
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        imageView = new MyImageView(getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(52), Tools.dp(40), 0));
        imageView.setPadding(Tools.dp(5), Tools.dp(5), Tools.dp(10), Tools.dp(5));

        textView = new TextView(getContext());
        textView.setText("CheckBox");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setPadding(0, 0, 0, Tools.dp(2));
        linearLayout.addView(textView);

        addView(imageView);
        addView(linearLayout);


        if(locked){
            ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(R.drawable.ic_lock);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(35), Tools.dp(35), 0));
            imageView.setPadding(Tools.dp(5), Tools.dp(5), Tools.dp(5), Tools.dp(5));
            addView(imageView);
        }
        else {
            check = new Check(getContext());
            check.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(22), Tools.dp(22), 0));
            check.setPadding(Tools.dp(5), Tools.dp(5), Tools.dp(5), Tools.dp(5));
            addView(check);
        }



    }

    private static class Check extends View{
        private final MarkerPart markerPart = new MarkerPart();
        private final BackroundPart backroundPart = new BackroundPart();

        public Check(Context context) {
            super(context);
        }
        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(getMeasuredHeight() + getPaddingBottom() + getPaddingTop(), getMeasuredHeight() + getPaddingLeft() + getPaddingRight());
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            backroundPart.draw(canvas);
            markerPart.draw(canvas);
        }
        public void setPressed(boolean pressed){
            backroundPart.setPressed(pressed);
        }
        public void setChecked(boolean checked){
            markerPart.setChecked(checked);
        }
        public boolean getChecked(){
            return markerPart.pressed;
        }

        class BackroundPart{
            boolean pressed = false;
            ArrayList<Thread> animationThreads = new ArrayList<>();
            float minPercent = 0;
            float maxPercent = 90;
            float animPercent = minPercent;
            float animTime = 100;
            float frameRate;
            int circleColor = Color.argb(255, 255, 255, 255);
            Paint paint = new Paint();
            RectF rect = new RectF();

            Path squircleCache = null;
            float squircleCacheSum = 0;

            public BackroundPart() {
                paint.setColor(circleColor);
                paint.setAntiAlias((Build.VERSION.SDK_INT > 14));
                frameRate = Data.tools.getDisplayRefreshRate();
            }
            void draw(Canvas canvas){
                //Logger.log("percent = " + animPercent);
                float maxWidth = getWidth() - getPaddingRight() - getPaddingLeft();
                float maxHeight = getHeight() - getPaddingBottom() - getPaddingTop();
                float maxStroke = maxWidth / 6f;
                float stroke = maxStroke*(animPercent/100f);
                float height = maxHeight - stroke*2;
                float width = maxWidth - stroke*2;
                float x = getPaddingLeft()+stroke;
                float y = getPaddingTop()+stroke;

                rect.set(x, y, x+width, y+height);
                if(Data.isSqircle()) { //1 out of 3
                    if(squircleCache == null || squircleCacheSum != getSquircleCenterSum(rect.centerX(), rect.centerY(), rect.width() * 0.53f)) {
                        squircleCache = getSquircleCenterPath(rect.centerX(), rect.centerY(), rect.width() * 0.53f);
                        squircleCacheSum = getSquircleCenterSum(rect.centerX(), rect.centerY(), rect.width() * 0.53f);
                    }
                    canvas.drawPath(squircleCache, paint);
                }
                else if(Data.isCircle())
                    canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width()*0.53f, paint);
                else if(Data.isRect())
                    canvas.drawRect(rect, paint);

            }
            void setPressed(boolean p){
                if(pressed != p){
                    pressed = p;
                    startPressing(!p);
                }
            }
            void startPressing( final  boolean unpress){
                for(Thread t:animationThreads)
                    t.interrupt();
                Thread animationThread = new Thread(() -> {
                    float frameTime = 1000 / frameRate;
                    float frames = animTime / frameTime;
                    float percentStep = maxPercent/frames;
                    if(unpress)
                        percentStep = -percentStep;
                    try {
                        for (; (unpress ? animPercent > minPercent : animPercent < maxPercent) && !Thread.currentThread().isInterrupted(); animPercent += percentStep) {
                            //noinspection BusyWait
                            Thread.sleep((int) frameTime);
                            if(unpress)
                                //noinspection BusyWait
                                Thread.sleep((int) frameTime* 2L);
                            Check.this.postInvalidate();
                        }

                        if (!Thread.interrupted())
                            animationThreads.remove(Thread.currentThread());
                    }
                    catch (Exception e){
                        //Logger.log("thread stopped");
                    }
                    finally {
                        if(animPercent < minPercent) animPercent = minPercent;
                        if(animPercent > maxPercent) animPercent = maxPercent;
                    }
                });
                animationThreads.add(animationThread);
                animationThread.start();
            }
        }
        class MarkerPart{
            boolean pressed = false;
            ArrayList<Thread> animationThreads = new ArrayList<>();
            float minPercent = 0;
            float maxPercent = 100;
            float animPercent = minPercent;
            float animTime = 200;
            float frameRate = 60;
            int lineColor = Color.parseColor("#16A085");
            Paint paint = new Paint();

            public MarkerPart() {
                paint.setColor(lineColor);
                paint.setAntiAlias((Build.VERSION.SDK_INT > 14));
            }
            void draw(Canvas canvas){
                //Logger.log("percent = " + animPercent);
                if(animPercent < 2)
                    return;
                float maxWidth = getWidth() - getPaddingRight() - getPaddingLeft();
                float maxHeight = getHeight() - getPaddingBottom() - getPaddingTop();
                paint.setStrokeWidth(maxWidth*0.21f);
                float[] points = new float[]{
                        getPaddingLeft() + maxWidth*0.2f, getPaddingTop() + maxHeight*0.35f,         getPaddingLeft() + maxWidth*0.4f, getPaddingTop() + maxHeight*0.55f,
                        getPaddingLeft() + maxWidth*0.4f, getPaddingTop() + maxHeight*0.7f,         getPaddingLeft() + maxWidth, getPaddingTop() + maxHeight*0.1f
                }; //bx, by, ex, ey, ...

                float totalLength = 0;
                for(int i=0; i< points.length; i+=4) {
                    float bx = points[i];
                    float by = points[i+1];
                    float ex = points[i+2];
                    float ey = points[i+3];
                    float dx = ex-bx;
                    float dy = ey-by;
                    totalLength += Math.sqrt(dx*dx+dy*dy);
                }

                float allowedLength = totalLength*(animPercent/100f);
                float drawedLength = 0;
                for(int i=0; i< points.length; i+=4) {
                    float bx = points[i];
                    float by = points[i+1];
                    float ex = points[i+2];
                    float ey = points[i+3];
                    float dx = ex-bx;
                    float dy = ey-by;
                    float d = (float)Math.sqrt(dx*dx+dy*dy);
                    float remainingLength = allowedLength - drawedLength;
                    float lineCoef = Math.min(1f, Math.max(0, remainingLength / d));
                    ex = bx + lineCoef*dx;
                    ey = by + lineCoef*dy;
                    canvas.drawLine(bx, by, ex, ey, paint);
                    float drawed_d = (float)Math.sqrt(dx*dx+dy*dy);
                    drawedLength += drawed_d;
                }
            }
            void setChecked(boolean p){
                if(pressed != p){
                    pressed = p;
                    startPressing(!p);
                }
            }
            void startPressing( final  boolean unpress){
                for(Thread t:animationThreads)
                    t.interrupt();
                Thread animationThread = new Thread(() -> {
                    float frameTime = 1000 / frameRate;
                    float frames = animTime / frameTime;
                    float percentStep = maxPercent/frames;
                    if(unpress)
                        percentStep = -percentStep;
                    try {
                        for (; (unpress ? animPercent > minPercent : animPercent < maxPercent) && !Thread.currentThread().isInterrupted(); animPercent += percentStep) {
                            //noinspection BusyWait
                            Thread.sleep((int) frameTime);
                            Check.this.postInvalidate();
                        }

                        if (!Thread.interrupted())
                            animationThreads.remove(Thread.currentThread());
                    }
                    catch (Exception e){
                        //Logger.log("thread stopped");
                    }
                    finally {
                        if(animPercent < minPercent) animPercent = minPercent;
                        if(animPercent > maxPercent) animPercent = maxPercent;
                    }
                });
                animationThreads.add(animationThread);
                animationThread.start();
            }
        }
    }
}

