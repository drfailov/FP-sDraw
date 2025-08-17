package com.fsoft.FP_sDraw.common;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;

import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.menu.DialogLoading;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BackgroundRemover{

    public interface OnSuccessBackgroundRemoved{
        void success(Bitmap bitmap, int backgroundColor);
    }
    private OnSuccessBackgroundRemoved onSuccess = null;
    private Runnable onError = null;
    private Bitmap bitmap = null;
    private int backgroundColor = 0;
    private Context context = null;
    private Handler handler = null;
    private DialogLoading dialogLoading = null;
    private final RectF CropCoefficient = new RectF(0,0,0,0);
    private float backgroundDetectionStrength = 32;

    public BackgroundRemover(float backgroundDetectionStrength, RectF cropCoefficient) {
        this.backgroundDetectionStrength = backgroundDetectionStrength;
        this.CropCoefficient.set(cropCoefficient);
    }

    public BackgroundRemover(Context context, OnSuccessBackgroundRemoved onSuccess, Runnable onError, Bitmap bitmap) {
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.bitmap = bitmap;
        this.context = context;

    }

    public void setCropCoefficient(RectF cropCoefficient) {
        this.CropCoefficient.set(cropCoefficient);
    }

    public void setBackgroundDetectionStrength(float backgroundDetectionStrength) {
        this.backgroundDetectionStrength = backgroundDetectionStrength;
    }

    public void startOnFrame(){
        if(bitmap == null)
            return;
        handler = new Handler();
        showLoadingDialog(context);

        new Thread(() -> {
            try {
                bitmap = removeBackgroundOnFrame(bitmap);
                success();
            }
            catch (Throwable e){
                e.printStackTrace();
                error();
            }
        }).start();
    }
    public void startOnTransparency(){
        if(bitmap == null)
            return;
        handler = new Handler();
        showLoadingDialog(context);

        new Thread(() -> {
            try {
                bitmap = removeBackgroundOnTransparency(bitmap);
                success();
            }
            catch (Throwable e){
                e.printStackTrace();
                error();
            }
        }).start();
    }
    public void startOnKnownColor(int color){
        if(bitmap == null)
            return;
        handler = new Handler();
        showLoadingDialog(context);

        new Thread(() -> {
            try {
                bitmap = removeKnownBackground(bitmap, color);
                success();
            }
            catch (Throwable e){
                e.printStackTrace();
                error();
            }
        }).start();
    }


    public void  showLoadingDialog(Context context){
        if(dialogLoading == null) {
            dialogLoading = new DialogLoading(context, R.string.analyzingImage);
            dialogLoading.show();
        }
    }
    public void hideLoadingWindow(){
        handler.post(() -> {
            dialogLoading.cancel();
        });
    }
    public Bitmap removeBackgroundOnFrame(Bitmap bitmap){
        Rect frameToAnalyze = new Rect(
                (int)(bitmap.getWidth() * CropCoefficient.left),
                (int)(bitmap.getHeight() * CropCoefficient.top),
                (int)(bitmap.getWidth()-1 - bitmap.getWidth() * CropCoefficient.right),
                (int)(bitmap.getHeight()-1 - bitmap.getHeight() *  CropCoefficient.bottom));
        backgroundColor = getDominantColor(bitmap, frameToAnalyze);
        //create copy
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
            mutableBitmap.setHasAlpha(true);
        //remove it
        clearColor(mutableBitmap, backgroundColor, backgroundDetectionStrength);
        return mutableBitmap;
    }

    public Bitmap removeBackgroundOnTransparency(Bitmap bitmap){
        //create copy
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
            mutableBitmap.setHasAlpha(true);
        //find background color
        backgroundColor = getDominantColorOnEdge(mutableBitmap);
        //remove it
        //return mutableBitmap; //return incomplete for debug
        return clearColor(mutableBitmap, backgroundColor, backgroundDetectionStrength);
    }

    public Bitmap removeKnownBackground(Bitmap bitmap, int backgroundColor){
        this.backgroundColor = backgroundColor;
        //create copy
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
            mutableBitmap.setHasAlpha(true);
        //remove it
        return clearColor(mutableBitmap, backgroundColor, backgroundDetectionStrength);
    }

    private void success(){
        if(handler == null)
            return;
        hideLoadingWindow();
        handler.post(() -> {
            if(onSuccess != null)
                onSuccess.success(bitmap, backgroundColor);
        });
    }
    private void error(){
        if(handler == null)
            return;
        handler.post(() -> {
            Logger.show(context.getString(R.string.errorWhileInserting));
            dialogLoading.cancel();
            if(onError != null)
                onError.run();
        });
    }

    private int getDominantColor(Bitmap bitmap, Rect frameToAnalyze){
        //(color, frequency)
        HashMap<Integer, Integer> stat = new HashMap<>();
        int frame = 3;

        //да, углы в нашем случае считаются по 2 раза.
        // Но это не проблема, т.к. у них и важность выше
        //top
        for(int y=frameToAnalyze.top; y<frameToAnalyze.top + frame; y++)
            analyzeHorizontal(bitmap, /*out*/stat, y, frameToAnalyze.left, frameToAnalyze.right);
        //bottom
        for(int y=frameToAnalyze.bottom; y>frameToAnalyze.bottom - frame; y--)
            analyzeHorizontal(bitmap, /*out*/stat, y, frameToAnalyze.left, frameToAnalyze.right);
        //left
        for(int x=frameToAnalyze.left; x<frameToAnalyze.left + frame; x++)
            analyzeVertical(bitmap, /*out*/stat, x, frameToAnalyze.top, frameToAnalyze.bottom);
        //right
        for(int x=frameToAnalyze.right; x>frameToAnalyze.right - frame; x--)
            analyzeVertical(bitmap, /*out*/stat, x, frameToAnalyze.top, frameToAnalyze.bottom);

        return getMax(stat);
    }
    private void analyzeHorizontal(Bitmap bitmap, HashMap<Integer, Integer> outSstat, int y, int start, int end){
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if(y >= h)
            return;
        if(start < 0)
            start = 0;
        if(end >= w)
            end = w - 1;
        for(int x = start; x < end; x++){
            int color = bitmap.getPixel(x, y);

            Integer count = 0;
            if(outSstat.containsKey(color))
                count = outSstat.get(color);
            if(count == null)
                count = 0;
            outSstat.put(color, count + 1);
        }
    }
    private void analyzeVertical(Bitmap bitmap, HashMap<Integer, Integer> outStat, int x, int start, int end){
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if(x >= w)
            return;
        if(start < 0)
            start = 0;
        if(end >= h)
            end = h - 1;
        for(int y = start; y < end; y++){
            int color = bitmap.getPixel(x, y);

            Integer count = 0;
            if(outStat.containsKey(color))
                count = outStat.get(color);
            if(count == null)
                count = 0;
            outStat.put(color, count + 1);
        }
    }
    private static int getMax(HashMap<Integer, Integer> stat){
        long maxCount = 0;
        int maxColor = 0;

        Iterator<Map.Entry<Integer, Integer>> iterator = stat.entrySet().iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()){
            Map.Entry<Integer, Integer> entry = iterator.next();
            if(entry.getValue() > maxCount){
                maxCount = entry.getValue();
                maxColor = entry.getKey();
            }
        }
        return maxColor;
    }
    private static int getColorDistance(int c1, int c2){
        int dr = Color.red(c1) - Color.red(c2);
        int dg = Color.green(c1) - Color.green(c2);
        int db = Color.blue(c1) - Color.blue(c2);
        int da = Color.alpha(c1) - Color.alpha(c2);
        return Math.abs(dr) + Math.abs(dg) + Math.abs(db) + Math.abs(da);
    }
    public static int getDominantColorOnEdge(Bitmap bitmap){
        //pre-flight checks
        if(bitmap == null) return 0;
        if(bitmap.isRecycled()) return 0;
        //prepare mask
        Bitmap maskBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        //fill mask
        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {//for every pixel
                if(bitmap.getPixel(x, y) == Color.TRANSPARENT) { //if transparent
                    maskBitmap.setPixel(x, y, Color.BLACK); //set mask to black
                }
                else{                                         //if not transparent
                    maskBitmap.setPixel(x, y, Color.BLUE); //set mask to blue
                    for (int xx = x - 1; xx <= x + 1; xx++) { //and find neighbors
                        for (int yy = y - 1; yy <= y + 1; yy++) {
                            if (xx >= 0 && xx < bitmap.getWidth() && yy >= 0 && yy <bitmap.getHeight()
                                    && bitmap.getPixel(xx, yy) == Color.TRANSPARENT) { //which transparent
                                maskBitmap.setPixel(x, y, Color.YELLOW); //if found, set mask to yellow
                            }
                        }
                    }
                }
            }
        }

        //check mask (for debug)
//        for(int x=0; x<bitmap.getWidth(); x++){
//            for(int y=0; y<bitmap.getHeight(); y++){
//                int color = maskBitmap.getPixel(x,y);
//                bitmap.setPixel(x,y,color);
//            }
//        }

        //finding DOMINANT color
        HashMap<Integer, Integer> stat = new HashMap<>();

        for(int x=0; x<bitmap.getWidth(); x++){
            for(int y=0; y<bitmap.getHeight(); y++){
                if(maskBitmap.getPixel(x,y) ==  Color.YELLOW){
                    int color = bitmap.getPixel(x,y);

                    Integer count = 0;
                    if(stat.containsKey(color))
                        count = stat.get(color);
                    if(count == null)
                        count = 0;
                    stat.put(color, count + 1);
                }
            }
        }
        return getMax(stat);
    }
    public static Bitmap clearColor(Bitmap bitmap, int colorKey, float levelTransparent){

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float levelProportional = levelTransparent + 48; //от levelTransparent до levelProportional будет полупрозрачное
        float alphaMax = 255; //Это уровень полной прозрачности

        for(int x=0; x < w; x++){
            for(int y=0; y < h; y++){
                int colorCurrent = bitmap.getPixel(x, y);
                float distance = getColorDistance(colorKey, colorCurrent); //difference between colors
                //0...32 transparent
                //32...80 transparency
                //80...255 not change
                float alpha;
                if(distance < levelTransparent)
                    alpha = 0;
                else if(distance < levelProportional){
                    float proportionalDiapason = levelProportional - levelTransparent;
                    float proportionalValue = distance - levelTransparent;
                    float coefficient = proportionalValue/proportionalDiapason;
                    alpha = alphaMax * coefficient;
                }
                else
                    alpha = alphaMax;

                if(alpha > 255)
                    alpha = 255;

                int colorNew = (alpha == 255?colorCurrent:applyAlpha(colorCurrent, (int)alpha));
                bitmap.setPixel(x, y, colorNew);
            }
        }
        return bitmap;
    }
    private static int applyAlpha(int color, int alpha){
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(alpha, r, g, b);
    }
}
