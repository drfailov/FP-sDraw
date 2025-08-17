package com.fsoft.FP_sDraw.common;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 *
 * Created by Dr. Failov on 08.11.2015.
 */
public class MyImageView extends View {
    private int resource = 0;
    private Bitmap cache = null;
    private int neededWidth = 0;
    private int neededHeight = 0;
    private Thread loadingThread = null;
    private final Paint paint = new Paint();

    public MyImageView(Context context) {
        super(context);
        paint.setFilterBitmap((Build.VERSION.SDK_INT > 14));
    }
    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setFilterBitmap((Build.VERSION.SDK_INT > 14));
    }
    public MyImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setFilterBitmap((Build.VERSION.SDK_INT > 14));
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MyImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        paint.setFilterBitmap((Build.VERSION.SDK_INT > 14));
    }
    public void setDrawable(int resource) {
        this.resource = resource;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(cache != null) {
            cache.recycle();
            cache = null;
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        neededWidth = getWidth() - getPaddingRight() - getPaddingLeft();
        neededHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        if(cache == null || (cache.getHeight() != neededHeight && cache.getWidth() != neededWidth)) {
            startLoading();
            paint.setColor(Color.GRAY);
            canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), paint);
        }
        else
            canvas.drawBitmap(cache, getPaddingLeft() + (neededWidth - cache.getWidth())/2f, getPaddingTop() + (neededHeight - cache.getHeight())/2f, paint);
    }

    private void startLoading(){
        if(loadingThread == null){
            loadingThread = new Thread(() -> {
                try {
                    float size = Math.min(neededWidth, neededHeight);
                    cache = Tools.decodeResource(getContext().getResources(), resource, size, size);
//                    Bitmap tmp = BitmapFactory.decodeResource(getContext().getResources(), resource);
//                    float coef = Math.min(neededHeight / (float)tmp.getHeight(), neededWidth / (float)tmp.getWidth());
//                    cache = Bitmap.createScaledBitmap(tmp, (int)((float)tmp.getWidth()*coef), (int)((float)tmp.getHeight()*coef), false);
                    postInvalidate();
                }
                catch (Throwable e){
                    e.printStackTrace();
                    Log.d("MyImageView", "Error decoding: " + e);
                }
                finally {
                    loadingThread = null;
                }
            });
            loadingThread.start();
        }
    }
}
