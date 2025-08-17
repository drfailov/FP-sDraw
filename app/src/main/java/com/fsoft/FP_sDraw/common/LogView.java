package com.fsoft.FP_sDraw.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 *
 * Created by Dr. Failov on 22.10.2015.
 */
public class LogView extends View {
    private final Paint mLoadPaint;
    private final Paint mLoadPaintStr;
    private String text = "START";

    public LogView(Context context) {
        super(context);
        mLoadPaint = new Paint();
        mLoadPaintStr = new Paint();
        mLoadPaint.setAntiAlias(false);
        mLoadPaintStr.setAntiAlias(false);
        mLoadPaint.setTextSize((Integer)Data.get(Data.debugTextSizeInt()));
        mLoadPaintStr.setTextSize((Integer)Data.get(Data.debugTextSizeInt()));
        mLoadPaint.setARGB(255, 255, 255, 255);
        mLoadPaintStr.setARGB(255, 0, 0, 0);
        mLoadPaint.setStyle(Paint.Style.FILL);
        mLoadPaintStr.setStyle(Paint.Style.STROKE);
        mLoadPaintStr.setStrokeWidth(5);
    }

    public void text(String text){
        this.text = text + "\n" + this.text;
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int y = 0;
        for(String line:text.split("\n")) {
            canvas.drawText(line, 10, y += mLoadPaint.getTextSize()+3, mLoadPaintStr);
            canvas.drawText(line, 10, y, mLoadPaint);
            if(y > getHeight()/2)
                break;
        }
    }
}
