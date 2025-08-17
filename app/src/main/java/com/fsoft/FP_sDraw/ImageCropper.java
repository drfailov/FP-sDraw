package com.fsoft.FP_sDraw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

/**
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 07.06.13
 * Time: 16:38
 * этот файл занимается выбором фрагмента рисунка для сохранения
 */
public class ImageCropper extends Activity {
    public static Bitmap initBitmap = null;
    public static OnCropListener initListener = null;
    public static Point initFixedSize = null;
    public static boolean initTransparent = false;


    public interface OnCropListener{
        void onCrop(Bitmap croppedImage);
    }

    OnCropListener onCrop = null;
    CropperView cropperView = null;
    //image processing
    Canvas canvas;
    Bitmap image        = null;
    Paint paint         = new Paint();

    final int TAKEN_NOTHING     = 0;
    final int TAKEN_BUTTON      = 1;
    final int TAKEN_TOP         = 2;
    final int TAKEN_BOTTOM      = 3;
    final int TAKEN_LEFT        = 4;
    final int TAKEN_RIGHT       = 5;
    final int TAKEN_MOVE        = 6;
    final int TAKEN_TOP_RIGHT   = 7;
    final int TAKEN_TOP_LEFT    = 8;
    final int TAKEN_BOTTOM_RIGHT= 9;
    final int TAKEN_BOTTOM_LEFT = 10;

    int DPI;
    int touchFund;
    int borderSize;
    int backgroundColor;
    int borderColor;
    int borderDotsColor;
    int borderDotsStrokeColor;
    int borderDotsSize;
    int buttonColor ;
    int buttonColorAct;
    int buttonTextColor;
    int buttonTextSize ;
    int buttonSizeX  ;
    int buttonSizeY   ;

    int height;
    int width;
    float top ;
    float bottom ;
    float left;
    float right;
    float buttonLeft()    {return (left + (right-left)/2)-buttonSizeX/2f;}
    float buttonRight()   {return (left + (right-left)/2)+buttonSizeX/2f;}
    float buttonTop()     {return bottom + touchFund + buttonSizeY < (height*0.95) ? bottom + touchFund : (height*0.95f)-buttonSizeY;}
    float buttonBottom()  {return bottom + touchFund + buttonSizeY < (height*0.95) ? bottom + touchFund + buttonSizeY : (height*0.95f);}
    int taken           = TAKEN_NOTHING;

    float lastTouchX    = 0;
    float lastTouchY    = 0;

    @Override public void onResume() {
            super.onResume();
    }

    public void OK(){
        cropperView.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        Bitmap result = Bitmap.createBitmap(image, (int)left, (int)top, (int)(right - left), (int)(bottom - top));
        if(initFixedSize != null){
            result = Bitmap.createScaledBitmap(result, initFixedSize.x, initFixedSize.y, true);
        }
        onCrop.onCrop(result);
        finish();
    }

    @Override protected void onCreate(Bundle bundle){
        try{
            super.onCreate(bundle);
            //workaround for cutout areas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                getWindow().setAttributes(layoutParams);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            cropperView = new CropperView(this);
            requestWindowFeature(Window.FEATURE_NO_TITLE);  //убрать панель уведомлений
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);   //убрать панель названия
            if(initBitmap != null)
                initActivity();

        }catch (Exception e){
            Logger.log("Где-то в ImageInserter.onCreate произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Где-то в ImageInserter.onCreate Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    void initActivity(){

        if(initListener == null)
            Logger.log("There is no action received! Did you forgot to fill static fields before calling???");
        onCrop = initListener;
        image = initBitmap;
        initListener = null;
        initBitmap = null;
        canvas=new Canvas(image);
        paint.setTextSize(buttonTextSize);
        paint.setAntiAlias(true);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        DPI = dm.densityDpi;

        touchFund       = DPI/6;
        borderSize      = Tools.dp(1);
        backgroundColor = Color.argb(100, 0,0,0);
        borderColor     = Color.WHITE;
        borderDotsColor = Color.argb(150, 0, 0, 0);
        borderDotsStrokeColor = Color.argb(200, 255, 255, 255);
        borderDotsSize  = DPI/23;
        buttonColor     = Color.argb(255,  250, 250, 250);
        buttonColorAct  = Color.argb(255, 150, 150, 150);
        buttonTextColor = Color.BLACK;
        buttonTextSize  = DPI/8;
        buttonSizeX     = DPI/2;
        buttonSizeY     = DPI/4;

        height          = image.getHeight();
        width           = image.getWidth();


        if(initFixedSize != null) {
            if(initFixedSize.x < width && initFixedSize.y < height && initFixedSize.x > Tools.dp(50) && initFixedSize.y > Tools.dp(50)){
                left = width / 2f - initFixedSize.x / 2f;
                right = left + initFixedSize.x;
                top = height / 2f - initFixedSize.y;
                bottom = top + initFixedSize.y;
            }
            else {
                float screenAspect = (float) width / (float) height;
                float fragmentAspect = (float) initFixedSize.x / (float) initFixedSize.y;
                if (screenAspect > fragmentAspect) { // frame is tall to screen
                    top = (int) (((double) height) * 1 / 3);
                    bottom = (int) (((double) height) * 2 / 3);
                    float frameHeight = bottom - top;
                    float frameWidth = frameHeight * fragmentAspect;
                    left = width / 2f - frameWidth / 2f;
                    right = width / 2f + frameWidth / 2f;
                } else {    // frame is wide to screen
                    left = (int) (((double) width) * 1 / 3);
                    right = (int) (((double) width) * 2 / 3);
                    float frameWidth = right - left;
                    float frameHeight = frameWidth / fragmentAspect;
                    top = height / 2f - frameHeight / 2f;
                    bottom = height / 2f + frameHeight / 2f;
                }
            }
        }
        else{
            left          = (int)(((double)width)*1/3);
            right         = (int)(((double)width)*2/3);
            top           = (int)(((double)height)*1/3);
            bottom        = (int)(((double)height)*2/3);
        }




        //Построение экрана...
        cropperView=new CropperView(this);
        //cropperView.setOnTouchListener(touchListener);
        cropperView.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        //cropperView.setBackgroundDrawable(new BitmapDrawable(image));

        //DEBUG
        Logger.log("ImageCropper.onCreate", "Variables:" +
                        "\ntouchFund: " + (touchFund) +
                        "\nborderSize: " + (borderSize) +
                        "\nborderColor: " + (borderColor) +
                        "\nbuttonTextColor: " + (buttonTextColor) +
                        "\nbuttonTextSize: " + (buttonTextSize) +
                        "\nbuttonSizeX: " + (buttonSizeX) +
                        "\nbuttonSizeY: " + (buttonSizeY) +
                        "\nheight: " + (height) +
                        "\nwidth: " + (width) +
                        "\ntop: " + (top) +
                        "\nbottom: " + (bottom) +
                        "\nleft: " + (left) +
                        "\nright: " + (right)
                ,false);
        setContentView(cropperView);
        cropperView.invalidate();
    }

    class CropperView extends View {
        private float touchDownTop = 0;
        private float touchDownBottom = 0;
        private float touchDownRight = 0;
        private float touchDownLeft = 0;

        public CropperView(Context context) {
            super(context);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                float x = event.getX(0);
                float y = event.getY(0);
                float dx = event.getX(0) - lastTouchX;
                float dy = event.getY(0) - lastTouchY;
                if(action == MotionEvent.ACTION_DOWN) {
                    touchDownTop = top;
                    touchDownBottom = bottom;
                    touchDownLeft = left;
                    touchDownRight = right;
                    if(x>buttonLeft() && x<buttonRight() && y>buttonTop() && y<buttonBottom())//button
                        taken = TAKEN_BUTTON;
                    else if(x>(left-touchFund) && x<(left+touchFund)&& y>(bottom-touchFund) && y<(bottom+touchFund))//left bottom
                        taken = TAKEN_BOTTOM_LEFT;
                    else if(x>(left-touchFund) && x<(left+touchFund)&& y>(top-touchFund) && y<(top+touchFund))//left top
                        taken = TAKEN_TOP_LEFT;
                    else if(x>(right-touchFund) && x<(right+touchFund)&& y>(bottom-touchFund) && y<(bottom+touchFund))//right bottom
                        taken = TAKEN_BOTTOM_RIGHT;
                    else if(x>(right-touchFund) && x<(right+touchFund)&& y>(top-touchFund) && y<(top+touchFund))//right top
                        taken = TAKEN_TOP_RIGHT;
                    else if(initFixedSize == null && x>(left-touchFund) && x<(left+touchFund) && y>top && y<bottom)//left
                        taken = TAKEN_LEFT;
                    else if(initFixedSize == null && x>(right-touchFund) && x<(right+touchFund) && y>top && y<bottom)//right
                        taken = TAKEN_RIGHT;
                    else if(initFixedSize == null && x>left && x<right && y>(top-touchFund) && y<(top+touchFund))//top
                        taken = TAKEN_TOP;
                    else if(initFixedSize == null && x>left && x<right && y>(bottom-touchFund) && y<(bottom+touchFund))//bottom
                        taken = TAKEN_BOTTOM;
//                    else if(x>left && x<right && y>top && y<bottom)//move
//                        taken = TAKEN_MOVE;
                    else
                        taken = TAKEN_MOVE;
                }
                else if(action == MotionEvent.ACTION_MOVE) {
                    if(taken == TAKEN_TOP_LEFT){
                        if(initFixedSize == null) {
                            if (left + dx > 0 && left + dx < right && top + dy > 0 && top + dy < bottom) {
                                left += dx;
                                top += dy;
                            }
                        }
                        if(initFixedSize != null && initFixedSize.y != 0 && initFixedSize.x != 0){
                            float touchDownWidth = touchDownRight-touchDownLeft;
                            float touchDownHeight = touchDownBottom-touchDownTop;
                            float touchDownDiagonal = (float)Math.sqrt(touchDownWidth*touchDownWidth+touchDownHeight*touchDownHeight);

                            float currentWidth = touchDownRight - x;
                            float currentHeight = touchDownBottom - y;
                            float currentDiagonal = (float)Math.sqrt(currentWidth*currentWidth+currentHeight*currentHeight);

                            float touchDownCoefficient = touchDownWidth / initFixedSize.x;
                            float changeCoefficient = currentDiagonal / touchDownDiagonal;
                            float currentCoefficient = touchDownCoefficient*changeCoefficient;

                            float maxWidth = right;
                            float maxHeight = bottom;
                            currentCoefficient = Math.min(currentCoefficient, Math.min(maxWidth / initFixedSize.x, maxHeight / initFixedSize.y));
                            float newWidth = initFixedSize.x * currentCoefficient;
                            float newHeight = initFixedSize.y * currentCoefficient;

                            left = right-newWidth;
                            top = bottom-newHeight;
                        }
                    }
                    if(taken == TAKEN_TOP_RIGHT){
                        if(initFixedSize == null) {
                            if (right + dx > left && right + dx < width && top + dy > 0 && top + dy < bottom) {
                                right += dx;
                                top += dy;
                            }
                        }
                        if(initFixedSize != null && initFixedSize.y != 0 && initFixedSize.x != 0){
                            float touchDownWidth = touchDownRight-touchDownLeft;
                            float touchDownHeight = touchDownBottom-touchDownTop;
                            float touchDownDiagonal = (float)Math.sqrt(touchDownWidth*touchDownWidth+touchDownHeight*touchDownHeight);

                            float currentWidth = x - touchDownLeft;
                            float currentHeight = touchDownBottom - y;
                            float currentDiagonal = (float)Math.sqrt(currentWidth*currentWidth+currentHeight*currentHeight);

                            float touchDownCoefficient = touchDownWidth / initFixedSize.x;
                            float changeCoefficient = currentDiagonal / touchDownDiagonal;
                            float currentCoefficient = touchDownCoefficient*changeCoefficient;

                            float maxWidth = width - left;
                            float maxHeight = bottom;
                            currentCoefficient = Math.min(currentCoefficient, Math.min(maxWidth / initFixedSize.x, maxHeight / initFixedSize.y));
                            float newWidth = initFixedSize.x * currentCoefficient;
                            float newHeight = initFixedSize.y * currentCoefficient;

                            right = left+newWidth;
                            top = bottom-newHeight;
                        }
                    }
                    if(taken == TAKEN_BOTTOM_RIGHT){
                        if(initFixedSize == null) {
                            if (right + dx > left && right + dx < width && bottom + dy > top && bottom + dy < height) {
                                right += dx;
                                bottom += dy;
                            }
                        }
                        if(initFixedSize != null && initFixedSize.y != 0 && initFixedSize.x != 0){
                            float touchDownWidth = touchDownRight-touchDownLeft;
                            float touchDownHeight = touchDownBottom-touchDownTop;
                            float touchDownDiagonal = (float)Math.sqrt(touchDownWidth*touchDownWidth+touchDownHeight*touchDownHeight);

                            float currentWidth = x - touchDownLeft;
                            float currentHeight = y - touchDownTop;
                            float currentDiagonal = (float)Math.sqrt(currentWidth*currentWidth+currentHeight*currentHeight);

                            float touchDownCoefficient = touchDownWidth / initFixedSize.x;
                            float changeCoefficient = currentDiagonal / touchDownDiagonal;
                            float currentCoefficient = touchDownCoefficient*changeCoefficient;

                            float maxWidth = width - left;
                            float maxHeight = height - top;
                            currentCoefficient = Math.min(currentCoefficient, Math.min(maxWidth / initFixedSize.x, maxHeight / initFixedSize.y));
                            float newWidth = initFixedSize.x * currentCoefficient;
                            float newHeight = initFixedSize.y * currentCoefficient;

                            right = left+newWidth;
                            bottom = top+newHeight;
                        }
                    }
                    if(taken == TAKEN_BOTTOM_LEFT){
                        if(initFixedSize == null) {
                            if (left + dx > 0 && left + dx < right && bottom + dy > top && bottom + dy < height) {
                                left += dx;
                                bottom += dy;
                            }
                        }
                        if(initFixedSize != null && initFixedSize.y != 0 && initFixedSize.x != 0){
                            float touchDownWidth = touchDownRight-touchDownLeft;
                            float touchDownHeight = touchDownBottom-touchDownTop;
                            float touchDownDiagonal = (float)Math.sqrt(touchDownWidth*touchDownWidth+touchDownHeight*touchDownHeight);

                            float currentWidth = touchDownRight - x;
                            float currentHeight = y - touchDownTop;
                            float currentDiagonal = (float)Math.sqrt(currentWidth*currentWidth+currentHeight*currentHeight);

                            float touchDownCoefficient = touchDownWidth / initFixedSize.x;
                            float changeCoefficient = currentDiagonal / touchDownDiagonal;
                            float currentCoefficient = touchDownCoefficient*changeCoefficient;

                            float maxWidth = right;
                            float maxHeight = height - top;
                            currentCoefficient = Math.min(currentCoefficient, Math.min(maxWidth / initFixedSize.x, maxHeight / initFixedSize.y));
                            float newWidth = initFixedSize.x * currentCoefficient;
                            float newHeight = initFixedSize.y * currentCoefficient;

                            left = right-newWidth;
                            bottom = top+newHeight;
                        }
                    }
                    if(taken == TAKEN_LEFT){
                        if(left+dx>0 && left+dx<right)
                            left += dx;
                    }
                    else if(taken == TAKEN_RIGHT) {
                        if(right+dx>left && right+dx<width)
                            right += dx;
                    }
                    else if(taken == TAKEN_TOP){
                        if(top+dy>0 && top+dy<bottom)
                            top += dy;
                    }
                    else if(taken == TAKEN_BOTTOM){
                        if(bottom+dy>top && bottom+dy<height)
                            bottom += dy;
                    }
                    else if(taken == TAKEN_MOVE){
                        if(left+dx>0 && right+dx<width){
                            left += dx;
                            right += dx;
                        }
                        if(top+dy>0 && bottom+dy<height){
                            bottom += dy;
                            top += dy;
                        }
                    }
                }
                else if(action == MotionEvent.ACTION_UP) {
                    if(taken == TAKEN_BUTTON && x>buttonLeft() && x<buttonRight() && y>buttonTop() && y<buttonBottom()) {
                        OK();
                    }
                    taken = TAKEN_NOTHING;

                    touchDownTop = 0;
                    touchDownBottom = 0;
                    touchDownLeft = 0;
                    touchDownRight = 0;
                }
                lastTouchX = x;
                lastTouchY = y;
                invalidate();
            }catch (Exception e){
                Logger.log("Где-то в ImageInserter.dispatchTouchEvent произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            } catch (OutOfMemoryError e) {
                Logger.log("Где-то в ImageInserter.dispatchTouchEvent Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            }
            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            try{
                //draw background
                if(initTransparent){
                    int background = (Integer)Data.get(Data.backgroundColorInt());
                    int backgroundGridLuminosity = 0;
                    if(Tools.isLightColor(background)){
                        backgroundGridLuminosity = 200;
                    }
                    else{
                        backgroundGridLuminosity = 50;
                    }
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.rgb(backgroundGridLuminosity,backgroundGridLuminosity,backgroundGridLuminosity));
                    canvas.drawRect(0, 0, width, height, paint);

                    float cellSize = DPI / 12f;

                    paint.setColor(Color.rgb(backgroundGridLuminosity+10,backgroundGridLuminosity+10,backgroundGridLuminosity+10));
                    boolean offset = false;
                    for(float y=0; y<height; y += cellSize){
                        for (float x = (offset?0f:cellSize); x<width; x+= cellSize*2){
                            canvas.drawRect(x, y, x+cellSize, y+cellSize, paint);
                        }
                        offset = !offset;
                    }
                }
                //draw image
                canvas.drawBitmap(image, 0,0, paint);
                //draw background with window
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(backgroundColor);
                canvas.drawRect(0, 0, width, top, paint);
                canvas.drawRect(0, top, left, bottom, paint);
                canvas.drawRect(right, top, width, bottom, paint);
                canvas.drawRect(0, bottom, width, height, paint);

                //BRAW BORDER
                //draw border
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(borderSize);
                paint.setColor(borderColor);
                canvas.drawRect(left, top, right, bottom, paint);
                //re-draw black strip
                paint.setStrokeWidth(1);
                paint.setColor(Color.BLACK);
                canvas.drawRect(left, top, right, bottom, paint);
                //DRAW DOTS
                drawSquareDot(canvas, left, top, paint);
                if(initFixedSize == null)
                    drawSquareDot(canvas, (left + right)/2, top, paint);
                drawSquareDot(canvas, right, top, paint);

                if(initFixedSize == null)
                    drawSquareDot(canvas, left, (bottom + top)/2, paint);
                if(initFixedSize == null)
                    drawSquareDot(canvas, right, (bottom + top)/2, paint);

                drawSquareDot(canvas, left, bottom, paint);
                if(initFixedSize == null)
                    drawSquareDot(canvas, (left + right)/2, bottom, paint);
                drawSquareDot(canvas, right, bottom, paint);

                //draw fixed size if any
                if(initFixedSize != null) {
                    String text = initFixedSize.x + "x" + initFixedSize.y;
                    paint.setTextSize(Tools.sp(10));
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.WHITE);
                    float textWidth = paint.measureText(text);
                    float padding = Tools.dp(10);
                    canvas.drawText(text, left+(right-left)/2f-textWidth/2f, top - padding, paint);
                }
                //DRAW BUTTON
                //draw button
                paint.setColor(taken == TAKEN_BUTTON ? buttonColorAct : buttonColor);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(buttonLeft(), buttonTop(), buttonRight(), buttonBottom(), paint);
                //re-drraw button border
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                canvas.drawRect(buttonLeft(), buttonTop(), buttonRight(), buttonBottom(), paint);
                //draw text
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(buttonTextColor);
                paint.setTextSize(Tools.sp(18));
                float textWidth = paint.measureText("OK");
                float textHeight = paint.getTextSize();
                canvas.drawText("OK", (buttonLeft() + (buttonSizeX - textWidth) / 2), (buttonTop() + (buttonSizeY + textHeight*0.7f) / 2), paint);
            }catch (Exception e){
                Logger.log("Где-то в ImageInserter.redraw произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            } catch (OutOfMemoryError e) {
                Logger.log("Где-то в ImageInserter.redraw Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            }
        }
        void drawSquareDot(Canvas canvas, float x, float y, Paint paint){
            paint.setColor(Color.argb(100, 0,0,0)); //BLACK
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            float radius = Tools.dp(20) / 3f;
            canvas.drawCircle(x, y, radius + Tools.dp(1), paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y,radius, paint);
        }




    }
}
