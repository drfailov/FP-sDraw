package com.fsoft.FP_sDraw.menu;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.ArrayList;

public class HintMenu {
    public static int SHOW_TIMES = 1;
    private final DrawCore draw;
    private final String text;
    private final int imageResource;
    private boolean allowHide = true;
    private boolean touchCaught = false;
    private Bitmap image = null;
    private Thread imageLoadingThread = null;

    private ArrayList<String> textLinesCache = null;
    private float textLinesCacheWidth = 0;
    private float textLinesCacheSize = 0;

    private final float STATE_HIDDEN = -1;
    private final float STATE_SHOWN = 1;
    private float state = 0; //changed by animationThread
    private float targetState = STATE_SHOWN;
    private Thread animationThread = null;


    private final float buttonPagging = Tools.dp(10);
    private final float buttonSize = Tools.dp(43);
    private final float buttonPadding = Tools.dp(12);


    private final int backgroundColor = Color.argb(210,0,0,0);
    private final int messageTopMargin = Tools.dp(45);
    private final int messageSideMargins = Tools.dp(15);
    private final int messagePaddingTopBottom = Tools.dp(17);
    private final int messagePaddingSides = Tools.dp(10);
    private final int messageImageSize = Tools.dp(25);
    private float textSize = Tools.sp(13);
    private final float textHeightCoefficient = 1.2f;
    private final int roundness = Tools.sp(8);
    private final Paint paint = new Paint();
    private final RectF rect = new RectF();//reusable
    private final RectF buttonRect = new RectF();

    public HintMenu(DrawCore draw, String text, int imageResource, String messageDescription, int showTimes) {
        this.draw = draw;
        this.text = text;
        this.imageResource = imageResource;
        if(!Data.isTutor(messageDescription, showTimes))
            targetState = STATE_HIDDEN;
    }
    public boolean processTouch(MotionEvent event){
        if(!Data.tools.isAllowedDeviceForUi(event))
            return false;
        if(!allowHide)
            return false;
        if(!enabled())
            return false;

        if (targetState == STATE_SHOWN){
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                hide();
                return false;
            }
            return touchCaught;
        }

        if (targetState == STATE_HIDDEN) {
            if(event.getAction() == MotionEvent.ACTION_DOWN && rect.contains(event.getX(), event.getY())) {
                touchCaught = true;
                if(draw != null)
                    draw.redraw();
                return true;
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                if(touchCaught && rect.contains(event.getX(), event.getY())) {
                    touchCaught = false;
                    show();
                    return true;
                }
                touchCaught = false;
            }
            return touchCaught;

        }
        return false;
    }
    public void show(){
        targetState = STATE_SHOWN;
        Tools.vibrate(draw.view);
        draw.redraw();
    }
    public void hide(){
        targetState = STATE_HIDDEN;
        Tools.vibrate(draw.view);
        draw.redraw();
    }
    public void draw(Canvas canvas){
        if(!enabled())
            return;

        if(state != targetState && animationThread == null){
            animationThread = new Thread(this::runAnimationAsync);
            animationThread.start();
        }

        if(state > 0) { //draw message itself
            float animationXOffset = Tools.map(state, 1, 0, 0, Tools.dp(150));//0 for state=1; -width for state=0;
            float x = messageSideMargins + animationXOffset;
            float y = messageTopMargin;
            float width = draw.getWidth() - messageSideMargins * 2;
            //width = Tools.dp(230);
            float textWidth = width - messagePaddingSides * 3 - messageImageSize;
            float textX = x + messagePaddingSides * 2 + messageImageSize;
            float textY = y + messagePaddingTopBottom + textSize * 0.7f;
            boolean showImage = width > Tools.dp(310);
            if(!showImage) {
                textWidth = width - messagePaddingSides * 2;
                textX = x + messagePaddingSides;
            }
            float height, imageX, imageY;
            do {
                height = messagePaddingTopBottom * 2 + Math.max(messageImageSize, getTextHeight(textWidth));
                imageX = x + messagePaddingSides;
                imageY = y + height * 0.45f - messageImageSize / 2f;
                rect.set(x, y, x + width, y + height);
                if(rect.bottom > draw.getHeight()*0.7f)
                    textSize--;
            }
            while (rect.bottom > draw.getHeight()*0.7f);

            int contentOpacity = (int)Tools.map(state, 1, 0, 255, 0);
            int backgroundOpacity = (int)Tools.map(state, 1, 0, Color.alpha(backgroundColor), 0);
            int frameOpacity = (int)Tools.map(state, 1, 0, 70, 0);
            if(touchCaught)
                backgroundOpacity = 255;

            //frame
            paint.setColor(backgroundColor);
            paint.setAlpha(backgroundOpacity);
            paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            paint.setStyle(Paint.Style.FILL);
            if (Data.isRect())
                canvas.drawRect(rect, paint);
            else
                canvas.drawRoundRect(rect, roundness, roundness, paint);
            paint.setColor(Color.argb(70, 255, 255, 255));
            paint.setAlpha(frameOpacity);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Data.store().DPI * 0.002f);
            if (Data.isRect())
                canvas.drawRect(rect, paint);
            else
                canvas.drawRoundRect(rect, roundness, roundness, paint);

            //image
            if(showImage) {
                if (image == null) {
                    if (imageLoadingThread == null) {
                        imageLoadingThread = new Thread(this::loadBitmapAsync);
                        imageLoadingThread.start();
                    }
                    paint.setStyle(Paint.Style.FILL);
                    paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                    paint.setColor(Color.argb(contentOpacity / 2, 255, 255, 255));
                    canvas.drawRect(imageX, imageY, imageX + messageImageSize, imageY + messageImageSize, paint);
                } else {
                    paint.setColor(Color.argb(contentOpacity, 255, 255, 255));
                    canvas.drawBitmap(image, imageX, imageY, paint);
                }
            }

            //text
            if (textLinesCache != null) {
                paint.setTextSize(textSize);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(contentOpacity, 255, 255, 255));
                paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                for (int i = 0; i < textLinesCache.size(); i++) {
                    canvas.drawText(textLinesCache.get(i), textX, textY + i * textSize * textHeightCoefficient, paint);
                }
            }
        }
        if(state < 0){//draw show button
            float animationYOffset = Tools.map(state, -1, 0, 0, Tools.dp(50));//0 for state=1; -width for state=0;
            float x = draw.getWidth()-buttonSize-buttonPadding;
            float menuYOffset = 0;
            if(draw.onScreenInputDeviceSelectorMenu != null && draw.onScreenInputDeviceSelectorMenu.floatingMenuForSelectingInputDevice.enabled)
                menuYOffset = draw.onScreenInputDeviceSelectorMenu.floatingMenuForSelectingInputDevice.position().bottom;
            float y = buttonPadding + animationYOffset + menuYOffset;
            float imageX = x + buttonPadding;
            float imageY = y + buttonPagging;
            int contentOpacity = (int) Tools.map(state, -1, 0, 255, 0);
            int backgroundOpacity = (int) Tools.map(state, -1, 0, Color.alpha(MainMenu.transparentBackgroundColor), 0);
            int frameOpacity = (int) Tools.map(state, -1, 0, 70, 0);
            if(touchCaught)
                backgroundOpacity = 255;
            rect.set(x, y, x + buttonSize, y + buttonSize);
            buttonRect.set(rect);

            //frame
            paint.setColor(backgroundColor);
            paint.setAlpha(backgroundOpacity);
            paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            paint.setStyle(Paint.Style.FILL);
            if (Data.isRect())
                canvas.drawRect(rect, paint);
            else
                canvas.drawRoundRect(rect, roundness, roundness, paint);
            paint.setColor(Color.argb(70, 255, 255, 255));
            paint.setAlpha(frameOpacity);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Data.store().DPI * 0.002f);
            if (Data.isRect())
                canvas.drawRect(rect, paint);
            else
                canvas.drawRoundRect(rect, roundness, roundness, paint);

            //image
            if (image == null) {
                if (imageLoadingThread == null) {
                    imageLoadingThread = new Thread(this::loadBitmapAsync);
                    imageLoadingThread.start();
                }
                paint.setStyle(Paint.Style.FILL);
                paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                paint.setColor(Color.argb(contentOpacity / 2, 255, 255, 255));
                canvas.drawRect(imageX, imageY, imageX + messageImageSize, imageY + messageImageSize, paint);
            } else {
                paint.setColor(Color.argb(contentOpacity, 255, 255, 255));
                Rect buttonBitmapSourceRect = new Rect(0,0, image.getWidth(), image.getHeight());
                RectF buttonBitmapDestRect = new RectF(x+buttonPadding,y+buttonPadding, x+buttonSize-buttonPadding, y+buttonSize-buttonPadding);
                canvas.drawBitmap(image, buttonBitmapSourceRect, buttonBitmapDestRect, paint);
            }
        }
    }

    private void loadBitmapAsync(){
        try{
            image = Tools.decodeResource(draw.context.getResources(), imageResource, messageImageSize, messageImageSize);
        }
        catch (Throwable e){
            Logger.log(e);
        }
        finally {
            imageLoadingThread = null;
        }

    }

    private void runAnimationAsync(){
        try{
            float threshold = 0.01f;
            float refreshRating = Data.tools.getDisplayRefreshRate();

            while (Math.abs(state-targetState) > threshold){
                state += (targetState-state)*0.08f*(120f/refreshRating);
                if(rect.height() > 10) draw.redraw(0, (int)rect.bottom, 0, draw.getWidth());
                else draw.redraw();
                //noinspection BusyWait
                Thread.sleep((int)(1000f/refreshRating));
                //Logger.log("state="+state);
            }
            state = targetState;
        }
        catch (Throwable e){
            Logger.log(e);
        }
        finally {
            animationThread = null;
        }
    }

    private float getTextHeight(float fieldWidth){
        if(text != null && (textLinesCache == null || textLinesCacheWidth != fieldWidth || textLinesCacheSize != textSize)){
            String[] words = text.trim().replace("\n", " \n").replaceAll(" +", " ").split(" ");
            textLinesCache = new ArrayList<>();
            paint.setTextSize(textSize);
            paint.setStyle(Paint.Style.FILL);
            String lastLine = null;
            String currentLine = "";
            for (String word : words) {
                if (lastLine == null)
                    lastLine = word;
                currentLine += " " + word;
                if (paint.measureText(currentLine) > fieldWidth || word.contains("\n")) {
                    textLinesCache.add(lastLine.trim());
                    lastLine = currentLine = word;
                } else {
                    lastLine = currentLine;
                }
            }
            textLinesCache.add(currentLine.trim());
            textLinesCacheWidth = fieldWidth;
            textLinesCacheSize = textSize;
        }

        if(textLinesCache != null)
            return textLinesCache.size() * textSize*textHeightCoefficient;
        else
            return 0;
    }

    public boolean enabled() {
        return (Boolean)Data.get(Data.showHelpMessagesBoolean());
    }

    public void setAllowHide(boolean allowHide) {
        this.allowHide = allowHide;
        if(!allowHide)
            targetState = state = STATE_SHOWN;
    }

    public RectF getButtonRect(){
        return buttonRect;
    }
}
