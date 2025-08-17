package com.fsoft.FP_sDraw.menu;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class FloatingMenu{
    private final ArrayList<FloatingMenuButton> buttons = new ArrayList<>();
    private final ArrayList<FloatingMenuSlider> sliders = new ArrayList<>();
    private final PointF targetPosition = new PointF(0,0); //It's not limited by noGo zones and other things. This calculation is done by position() function
    private final PointF bounds = new PointF(1000, 1000); //its limits when menu will stop moving (usually it edge of screen)
    private final Paint paint = new Paint();
    private int backgroundColor = MainMenu.transparentBackgroundColor;
    private final View view; //object to access all its resources if needed
    private boolean touchCaught = false; //true when finger on screen and touchdown happened over menu window
    private FloatingMenuSlider touchCaughtBySlider = null; //When finger on screen it shows which slider is holding its touch
    private Timer hintAppearingTimer = null;
    private int hintAnimationPercent = 0; //0...100
    private String hintText = "help";
    private final PointF hintPosition = new PointF();
    private final ArrayList<RectF> noGoZones = new ArrayList<>();
    private final ArrayList<RectF> possiblePositions = new ArrayList<>(); //used when menu is calculating its position on screen and shown here for debugging
    public boolean enabled = true;

    public FloatingMenu(View view) {
        this.view = view;
    }

    public FloatingMenuButton addButton(int imageResource, int description, Runnable onClickListener, boolean visible){
        return addButton(imageResource, view.getResources().getString(description),onClickListener,visible);
    }
    public FloatingMenuButton addButton(int imageResource, String description, Runnable onClickListener, boolean visible){
        FloatingMenuButton floatingMenuButton = new FloatingMenuButton( imageResource, description, onClickListener, visible);
        buttons.add(floatingMenuButton);
        return floatingMenuButton;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public FloatingMenuSlider addSlider(int imageResource, int initialValue, int minimum, int maximum, Runnable onValueChanged, Runnable onTouchUp, boolean visible){
        FloatingMenuSlider floatingMenuSlider = new FloatingMenuSlider( imageResource, initialValue, minimum, maximum, onValueChanged, onTouchUp, visible);
        sliders.add(floatingMenuSlider);
        return floatingMenuSlider;
    }
    public void setBounds(float X, float Y) {
        this.bounds.x = X;
        this.bounds.y = Y;
    }
    public void setTargetPosition(float X, float Y) {
        this.targetPosition.x = X;
        this.targetPosition.y = Y;
    }
    public void setLastPosition(float X, float Y) { //used to make animation from desired point
        lastPositionX = X;
        lastPositionY = Y;
    }
    public void clearNoGoZones(){
        noGoZones.clear();
    }
    public void addNoGoZone(Rect rectF){
        noGoZones.add(new RectF(rectF));
    }
    public void addNoGoZone(RectF rectF){
        noGoZones.add(rectF);
    }


    public void addNoGoZones(ArrayList<RectF> rectF){
        noGoZones.addAll(rectF);
    }
    public float margin(){//from edge of the screen
        return Tools.dp(10);
    }

    private float lastPositionX = -1;
    private float lastPositionY = -1;
    private Thread smoothPositionThread = null;

    public RectF position() {
        float width = width();
        float height = height();
        RectF desiredPosition = new RectF(targetPosition.x, targetPosition.y, targetPosition.x + width, targetPosition.y + height);
        desiredPosition = correctBounds(desiredPosition);


//        float margin = margin();
//        float resultY = targetPosition.y;
//        float resultX = targetPosition.x;

        //bounds
//        resultX = Math.max(margin, resultX);
//        resultY = Math.max(margin, resultY);
//        resultX = Math.min(resultX, bounds.x - width - margin);
//        resultY = Math.min(resultY, bounds.y - height - margin);

        //noGoZones: find possible positions, stage 1
        possiblePositions.clear();
        for (RectF noGo : noGoZones) {
            if (noGo != null && intersect(desiredPosition, noGo)) {
                possiblePositions.addAll(getResolveVariants(desiredPosition, noGo));
            }
        }
        clearDuplicates(possiblePositions);
        //noGoZones: find possible positions, stage 2
        int numberOfInitialVariants = possiblePositions.size();
        for (int i = 0; i < numberOfInitialVariants; i++) {
            RectF possiblePosition = possiblePositions.get(i);
            for (RectF noGo : noGoZones) {
                if (noGo != null && intersect(possiblePosition, noGo)) {
                    possiblePositions.addAll(getResolveVariants(possiblePosition, noGo));
                }
            }
        }
        clearDuplicates(possiblePositions);
        //noGoZones: find positions with less possible intersections
        ArrayList<RectF> finalPossibleVariants = new ArrayList<>();
        if(!possiblePositions.isEmpty()) {
            int minIntersections = noGoZones.size();
            for (RectF possiblePosition : possiblePositions) {
                int numberOfIntersections = countNoGoIntersections(possiblePosition);
                if (numberOfIntersections < minIntersections) {
                    finalPossibleVariants.clear();
                    minIntersections = numberOfIntersections;
                }
                if((numberOfIntersections == minIntersections)){
                    finalPossibleVariants.add(possiblePosition);
                }
            }
        }
        //noGoZones: peek position closest to last (to not dusturb user by flying over screen menu)
        if(!finalPossibleVariants.isEmpty()){
            RectF minDistancePosition = finalPossibleVariants.get(0);
            float minDistance = calcDistance(minDistancePosition, lastPositionX, lastPositionY);
            for (RectF finalPossibleVariant:finalPossibleVariants){
                float distance = calcDistance(finalPossibleVariant, lastPositionX, lastPositionY);
                if(distance < minDistance){
                    minDistance = distance;
                    minDistancePosition = finalPossibleVariant;
                }
            }
            desiredPosition.set(minDistancePosition);
        }

//                    float noMoveX = resultX;
//                    float noMoveY = resultY;
//                    float moveTopY = noGo.top - height - margin;
//                    float moveBottomY = noGo.bottom + margin;
//                    float moveLeftX = noGo.left - width - margin;
//                    float moveRightX = noGo.right + margin;
//                    float moveTopDY = moveTopY-resultY;
//                    float moveBottomDY = moveBottomY-resultY;
//                    float moveLeftDX = moveLeftX-resultX;
//                    float moveRightDX = moveRightX-resultX;
//                    boolean moveTopSuitable = moveTopY > margin;
//                    boolean moveBottomSuitable = moveBottomY + height + margin < bounds.y;
//                    boolean moveLeftSuitable = moveLeftX > margin;
//                    boolean moveRightSuitable = moveRightX + width + margin < bounds.x;
//                    float moveMinD = moveTopSuitable?Math.abs(moveTopDY):moveBottomSuitable?Math.abs(moveBottomDY):moveLeftSuitable?Math.abs(moveLeftDX):Math.abs(moveRightDX);
//                    if(moveTopSuitable && Math.abs(moveTopDY) <= moveMinD){
//                        resultY = noMoveY + moveTopDY;
//                        resultX = noMoveX;
//                        moveMinD = Math.abs(moveTopDY);
//                    }
//                    if(moveBottomSuitable && Math.abs(moveBottomDY) <= moveMinD){
//                        resultY = noMoveY + moveBottomDY;
//                        resultX = noMoveX;
//                        moveMinD = Math.abs(moveBottomDY);
//                    }
//                    if(moveLeftSuitable && Math.abs(moveLeftDX) <= moveMinD){
//                        resultY = noMoveY;
//                        resultX = noMoveX + moveLeftDX;
//                        moveMinD = Math.abs(moveLeftDX);
//                    }
//                    if(moveRightSuitable && Math.abs(moveRightDX) <= moveMinD){
//                        resultY = noMoveY;
//                        resultX = noMoveX + moveRightDX;
//                    }
//                }
//            }
//        }

            //bounds again
//        resultX = Math.max(margin, resultX);
//        resultY = Math.max(margin, resultY);
//        resultX = Math.min(resultX, bounds.x - width - margin);
//        resultY = Math.min(resultY, bounds.y - height - margin);


            //smooth
        if (lastPositionX == -1) lastPositionX = desiredPosition.left;
        if (lastPositionY == -1) lastPositionY = desiredPosition.top;
        float dx = desiredPosition.left - lastPositionX;
        float dy = desiredPosition.top - lastPositionY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > Tools.dp(5)) {
            if (smoothPositionThread == null) {
                smoothPositionThread = new Thread(() -> {
                    while (smoothPositionThread == Thread.currentThread()) {
                        if (view != null) view.postInvalidate();
                        Tools.sleep(15);
                    }
                });
                smoothPositionThread.start();
            }
        } else {
            if (smoothPositionThread != null) {
                smoothPositionThread = null;
            }
        }
        if (Math.abs(d) > Tools.dp(5)) {
            desiredPosition.offsetTo(lastPositionX + dx * 0.1f, lastPositionY + dy * 0.1f);
        }
        lastPositionX = desiredPosition.left;
        lastPositionY = desiredPosition.top;

        return desiredPosition;
    }
    private float calcDistance(RectF rectF, float positionX, float positionY){
        if(rectF == null)
            return 0;
        float dx = rectF.left-positionX;
        float dy = rectF.top-positionY;
        return (float)Math.sqrt(dx*dx+dy*dy);
    }
    private void clearDuplicates(ArrayList<RectF> rectFS){
        for (int i = 0; i < rectFS.size(); i++){
            for (int j = i + 1; j < rectFS.size(); j++) {
                if (rectFS.get(i).equals(rectFS.get(j))) {
                    rectFS.remove(j);
                    j--;
                }
            }
        }
    }
    private boolean intersect (RectF r1, RectF r2) {
        return r1.left <= r2.right && r1.right >= r2.left &&
                r1.top <= (r2.bottom) && (r1.bottom) >= r2.top;
    }
    public int countNoGoIntersections(RectF rectF){
        int cnt = 0;
        for (RectF noGo : noGoZones) {
            if (noGo != null) {
                if(intersect(noGo, rectF))
                    cnt++;
            }
        }
        return cnt;
    }
    public ArrayList<RectF> getResolveVariants(RectF desiredPosition, RectF noGoZone){
        ArrayList<RectF> result = new ArrayList<>();
        if(!intersect(desiredPosition, noGoZone))
            return result;
        float margin = margin();
        float topToTry;
        float bottomToTry;
        float leftToTry;
        float rightToTry;


        //Right variant
        bottomToTry = desiredPosition.bottom;
        topToTry = desiredPosition.top;
        leftToTry = noGoZone.right + margin;
        rightToTry = leftToTry + desiredPosition.width();
        if(topToTry < bounds.y)
            result.add(correctBounds(new RectF(leftToTry, topToTry, rightToTry, bottomToTry)));

        //Left variant
        bottomToTry = desiredPosition.bottom;
        topToTry = desiredPosition.top;
        rightToTry = noGoZone.left - margin;
        leftToTry = rightToTry - desiredPosition.width();
        if(topToTry < bounds.y)
            result.add(correctBounds(new RectF(leftToTry, topToTry, rightToTry, bottomToTry)));

        //bottom variant
        topToTry = noGoZone.bottom + margin;
        bottomToTry = topToTry + desiredPosition.height();
        leftToTry = desiredPosition.left;
        rightToTry = desiredPosition.right;
        if(topToTry < bounds.y)
            result.add(correctBounds(new RectF(leftToTry, topToTry, rightToTry, bottomToTry)));

        //Top variant
        bottomToTry = noGoZone.top - margin;
        topToTry = bottomToTry - desiredPosition.height();
        leftToTry = desiredPosition.left;
        rightToTry = desiredPosition.right;
        if(bottomToTry > 0)
            result.add(correctBounds(new RectF(leftToTry, topToTry, rightToTry, bottomToTry)));

        return result;
    }
    public RectF correctBounds(RectF rectF){
        float margin = margin();
        if(rectF.left < margin){
            rectF.right = margin + rectF.width();
            rectF.left = margin;
        }
        if(rectF.top < margin){
            rectF.bottom = margin + rectF.height();
            rectF.top = margin;
        }
        if(rectF.right > bounds.x-margin){
            rectF.left = bounds.x-margin-rectF.width();
            rectF.right = bounds.x-margin;
        }
        if(rectF.bottom > bounds.y-margin){
            rectF.top = bounds.y-margin-rectF.height();
            rectF.bottom = bounds.y-margin;
        }
        return rectF;
    }
    public float width(){
        float buttonWidth = buttonWidth();
        float widthOfButtons = 0;
        for (int i = 0; i < buttons.size(); i++) {
            if(buttons.get(i) != null && buttons.get(i).visible)
                widthOfButtons+=buttonWidth;
        }
        int visibleSliders = 0;
        for (FloatingMenuSlider slider: sliders)
            if(slider.visible)
                visibleSliders ++;
        float minWidthOfSliders = visibleSliders==0?0: Tools.dp(200);
        return Math.max(widthOfButtons, minWidthOfSliders);
    }
    public float height(){
        int visibleSliders = 0;
        for (FloatingMenuSlider slider: sliders)
            if(slider.visible)
                visibleSliders ++;
        return buttonHeight() + (sliderHeight()*visibleSliders);
    }
    private float padding(){
        return Tools.dp(12);
    }
    private float roundness(){return Tools.dp(8);}
    private float buttonWidth(){
        float result = Tools.dp(43);
        if(result*buttons.size() > bounds.x-padding()*2)
            result = (bounds.x-padding()*2)/buttons.size();
        return result;
    }
    private float buttonHeight(){
        return Tools.dp(43);
    }
    private float sliderHeight(){
        return Tools.dp(40);
    }

    public void draw(Canvas canvas){
        if(!enabled)
            return;
        RectF pos = position();
        //float width = width();
        //noGoZones and possible positions
        if((Boolean)Data.get(Data.debugBoolean()))
        {
            paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            paint.setStrokeWidth(1);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setAlpha(150);
            for (RectF rectF : noGoZones) {
                if(rectF != null)
                    canvas.drawRect(rectF, paint);
            }

            paint.setColor(Color.GREEN);
            paint.setAlpha(150);
            for (RectF rectF : possiblePositions) {
                if(rectF != null)
                    canvas.drawRect(rectF, paint);
            }

        }
        //frame
        paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(backgroundColor);
        if(Data.isRect())
            canvas.drawRect(pos,paint);
        else
            canvas.drawRoundRect(pos, roundness(), roundness(),paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(MainMenu.menuTextColor);
        paint.setStrokeWidth(0.5f);
        if(Data.isRect())
            canvas.drawRect(pos,paint);
        else
            canvas.drawRoundRect(pos, roundness(), roundness(),paint);

        //buttons
        float buttonWidth = buttonWidth();
        float X = pos.left;
        for(int i = 0; i<buttons.size(); i++){
            FloatingMenuButton floatingMenuButton = buttons.get(i);
            if(floatingMenuButton.visible) {
                if(floatingMenuButton.pressed){
                    paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                    paint.setColor(Color.argb(50, 255,255,255));
                    paint.setStyle(Paint.Style.FILL);
                    if(Data.isRect())
                        canvas.drawRect(X, pos.top, X+buttonWidth, pos.top+buttonHeight(), paint);
                    else {
                        if (Build.VERSION.SDK_INT >= 21)
                            canvas.drawRoundRect(X, pos.top, X + buttonWidth, pos.top + buttonHeight(), roundness(), roundness(), paint);
                        else
                            canvas.drawRoundRect(new RectF(X, pos.top, X + buttonWidth, pos.top + buttonHeight()), roundness(), roundness(), paint);
                    }
                }
                if(floatingMenuButton.highlighted){
                    paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(1);
                    paint.setStyle(Paint.Style.STROKE);
                    if(Data.isRect())
                        canvas.drawRect(X, pos.top, X+buttonWidth, pos.top+buttonHeight(), paint);
                    else {
                        if (Build.VERSION.SDK_INT >= 21)
                            canvas.drawRoundRect(X, pos.top, X + buttonWidth, pos.top + buttonHeight(), roundness(), roundness(), paint);
                        else
                            canvas.drawRoundRect(new RectF(X, pos.top, X + buttonWidth, pos.top + buttonHeight()), roundness(), roundness(), paint);
                    }
                }
                if (floatingMenuButton.imageBitmap == null) {
                    floatingMenuButton.startLoadingImage();
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(X + buttonWidth / 2f, pos.top + buttonHeight() / 2f, (buttonWidth - (padding() * 2f)) / 3f, paint);
                }
                else{
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.FILL);
                    float bitmapLeft = X + (buttonWidth - floatingMenuButton.imageBitmap.getWidth())/2f;
                    float bitmapTop = pos.top + padding();
                    if(floatingMenuButton.rotateIcon) {
                        canvas.save();
                        canvas.rotate(90, bitmapLeft+floatingMenuButton.imageBitmap.getWidth()/2f, bitmapTop + floatingMenuButton.imageBitmap.getHeight()/2f);
                        canvas.drawBitmap(floatingMenuButton.imageBitmap, bitmapLeft, bitmapTop, paint);
                        canvas.restore();
                    }
                    else{
                        canvas.drawBitmap(floatingMenuButton.imageBitmap, bitmapLeft, bitmapTop, paint);
                    }
                }
                X+=buttonWidth;
            }
        }

        //sliders
        float Y = pos.top+buttonHeight();
        for(int i=0; i<sliders.size(); i++){
            FloatingMenuSlider floatingMenuSlider = sliders.get(i);
            if(floatingMenuSlider.visible){
                if (floatingMenuSlider.imageBitmap == null) {
                    floatingMenuSlider.startLoadingImage();
                }
                else{
                    paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(150);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawBitmap(floatingMenuSlider.imageBitmap, pos.left + sliderHeight()/4f, Y +  sliderHeight()/4f, paint);

                    float lineY = Y+sliderHeight()/2f;
                    float lineStart = pos.left+sliderHeight();
                    float lineWidth = pos.width() - sliderHeight()*1.50f;
                    float valueLineWidth = lineWidth * floatingMenuSlider.coefficient();

                    paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                    paint.setColor(MainMenu.menuHintColor);
                    paint.setStrokeWidth(Tools.dp(2));
                    if(floatingMenuSlider.pressed){
                        paint.setColor(MainMenu.menuTextColor);
                        paint.setStrokeWidth(Tools.dp(4));
                    }
                    canvas.drawLine(lineStart, lineY, lineStart + lineWidth, lineY, paint);

                    paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                    paint.setColor(MainMenu.menuAccentColor);
                    paint.setStrokeWidth(Tools.dp(4));
                    if(floatingMenuSlider.pressed){
                        paint.setColor(MainMenu.menuTextColor);
                        paint.setStrokeWidth(Tools.dp(4));
                    }
                    canvas.drawLine(lineStart, lineY, lineStart + valueLineWidth, lineY, paint);

                    paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(MainMenu.menuTextColor);
                    if(floatingMenuSlider.pressed){
                        paint.setColor(MainMenu.menuAccentColor);
                    }
                    canvas.drawCircle(lineStart+valueLineWidth, lineY, Tools.dp(8), paint);

                }
                Y+= sliderHeight();
            }
        }

        //hint
        if(hintAnimationPercent > 0){
            paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            paint.setTextSize(Tools.sp(14));
            float textWidth = paint.measureText(hintText);
            float textHeight = paint.getTextSize();
            float triangleHeight = Tools.dp(10);
            float triangleSides = triangleHeight * 0.9f;
            float animationOffset = (100-hintAnimationPercent)*0.5f;
            int opacity = (int)(hintAnimationPercent * 2.56);
            if(opacity > 255) opacity = 255;
            if(opacity < 0) opacity = 0;
            float frameTop = hintPosition.y - triangleHeight - padding() - textHeight - padding() + animationOffset;
            float frameBottom = hintPosition.y - triangleHeight + animationOffset;
            float frameLeft = hintPosition.x - textWidth/2f - padding();
            float frameRight = hintPosition.x + textWidth/2f + padding();
            if(frameLeft < 0){
                float offset = -frameLeft;
                frameLeft += offset;
                frameRight += offset;
            }

            if(frameRight > bounds.x){
                float offset = -(frameRight-bounds.x);
                frameLeft += offset;
                frameRight += offset;
            }

            if(frameTop > 0){
                //triangle
                Path trianglePath = new Path();
                trianglePath.moveTo(hintPosition.x, hintPosition.y);
                trianglePath.lineTo(hintPosition.x-triangleSides, hintPosition.y - triangleHeight);
                trianglePath.lineTo(hintPosition.x+triangleSides, hintPosition.y - triangleHeight);
                trianglePath.lineTo(hintPosition.x, hintPosition.y);
                trianglePath.close();
                paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(MainMenu.menuBackgroundColor);
                paint.setAlpha(opacity);
                canvas.drawPath(trianglePath, paint);
            }
            if(frameTop < 0){
                float offset = height() + triangleHeight + textHeight + padding()*3;
                frameTop += offset;
                frameBottom += offset;
            }

            //frame
            paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(MainMenu.menuBackgroundColor);
            paint.setAlpha(opacity);
            if(Data.isRect())
                canvas.drawRect(frameLeft, frameTop, frameRight, frameBottom, paint);
            else {
                if (Build.VERSION.SDK_INT >= 21)
                    canvas.drawRoundRect(frameLeft, frameTop, frameRight, frameBottom, roundness(), roundness(), paint);
                else
                    canvas.drawRoundRect(new RectF(frameLeft, frameTop, frameRight, frameBottom), roundness(), roundness(), paint);
            }

            //text
            paint.setAntiAlias(Build.VERSION.SDK_INT > 14);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(MainMenu.menuTextColor);
            paint.setAlpha(opacity);
            canvas.drawText(hintText, frameLeft+padding(), frameTop+padding() + textHeight*0.75f, paint);
        }
    }
    public boolean processTouch(MotionEvent event){
        if(!enabled)
            return false;
        RectF pos = position();
        float buttonWidth = buttonWidth();
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(event.getX() > pos.left && event.getX() < pos.right //pressed inside menu window
                    && event.getY() > pos.top && event.getY() < pos.bottom){
                touchCaught = true;
                //highlight buttons if any
                float X = pos.left;
                for(int i = 0; i<buttons.size(); i++) {
                    FloatingMenuButton floatingMenuButton = buttons.get(i);
                    if (floatingMenuButton.visible) {
                        floatingMenuButton.pressed = event.getX() > X && event.getX() < X + buttonWidth && event.getY() > pos.top && event.getY() < pos.top + buttonHeight();
                        if(floatingMenuButton.pressed){
                            hintText = floatingMenuButton.description;
                            hintPosition.set(event.getX(), pos.top);
                            if(hintAppearingTimer == null) {
                                hintAppearingTimer = new Timer();
                                hintAppearingTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        if (touchCaught) {
                                            if (view != null) Tools.vibrate(view);
                                            for (int i = 1; i <= 100 && hintAppearingTimer != null; i += 11) {
                                                hintAnimationPercent = i;
                                                if (view != null) view.postInvalidate();
                                                Tools.sleep(16);
                                            }
                                            hintAnimationPercent = 100;
                                            if(hintAppearingTimer == null)
                                                hintAnimationPercent = 0;
                                            if (view != null) view.postInvalidate();
                                        }
                                    }
                                }, ViewConfiguration.getLongPressTimeout());
                            }
                        }
                        X+=buttonWidth;
                    }
                }
                //check if touch caught by slider
                float Y = pos.top+buttonHeight();
                for (int i = 0; i < sliders.size(); i++) {
                    FloatingMenuSlider slider = sliders.get(i);
                    if(slider.visible){
                        slider.pressed = event.getX() > pos.left && event.getX() < pos.left + width() && event.getY() > Y && event.getY() < Y + sliderHeight();
                        if(slider.pressed) {
                            touchCaughtBySlider = slider;
                            touchCaughtBySlider.touchDownPositionX = event.getX();
                            touchCaughtBySlider.touchDownValue = touchCaughtBySlider.getValue();
                        }
                        Y+= sliderHeight();
                    }
                }
                if(view != null) view.postInvalidate();
            }
        }
        if(touchCaught && event.getAction() == MotionEvent.ACTION_MOVE){
            //highlight buttons if any
            if(touchCaughtBySlider == null) {
                float X = pos.left;
                for (int i = 0; i < buttons.size(); i++) {
                    FloatingMenuButton floatingMenuButton = buttons.get(i);
                    if (floatingMenuButton.visible) {
                        floatingMenuButton.pressed = event.getX() > X && event.getX() < X + buttonWidth && event.getY() > pos.top && event.getY() < pos.top + buttonHeight();
                        if(floatingMenuButton.pressed){
                            hintText = floatingMenuButton.description;
                            hintPosition.set(event.getX(), pos.top);
                        }
                        X += buttonWidth;
                    }
                }
            }
            //run action if slider moved
            if(touchCaughtBySlider != null){
                float lineSize = width() - sliderHeight()*1.25f;
                float valueSize = touchCaughtBySlider.maximum-touchCaughtBySlider.minimum;
                float dx = event.getX() - touchCaughtBySlider.touchDownPositionX;
                float dValue = dx * valueSize/lineSize;
                touchCaughtBySlider.value = touchCaughtBySlider.touchDownValue + dValue*0.9f;
                if(touchCaughtBySlider.onValueChanged != null) {
                    try {
                        touchCaughtBySlider.onValueChanged.run();
                    } catch (Exception e) {
                        Logger.log(e);
                    }
                }
            }
            if(view != null) view.postInvalidate();
        }
        if(event.getAction() == MotionEvent.ACTION_UP){
            //run action if button pressed
            if(touchCaught && touchCaughtBySlider == null && hintAnimationPercent == 0) {
                float X = pos.left;
                for (int i = 0; i < buttons.size(); i++) {
                    FloatingMenuButton floatingMenuButton = buttons.get(i);
                    if (floatingMenuButton.visible) {
                        if (event.getX() > X && event.getX() < X + buttonWidth && event.getY() > pos.top && event.getY() < pos.top + buttonHeight()) {
                            if (floatingMenuButton.runnable != null) {
                                try {
                                    Tools.vibrate(view);
                                    floatingMenuButton.runnable.run();
                                }
                                catch (Throwable e){
                                    Logger.log(e);
                                }
                            }
                        }
                        X += buttonWidth;
                    }
                }
            }
            //run action if slider unpressed
            if(touchCaught && touchCaughtBySlider != null && touchCaughtBySlider.onTouchUp != null){
                try {
                    touchCaughtBySlider.onTouchUp.run();
                } catch (Exception e) {
                    Logger.log(e);
                }
            }
            //unPress all
            for(FloatingMenuButton button:buttons)
                button.pressed = false;
            for(FloatingMenuSlider slider:sliders)
                slider.pressed = false;
            if(view != null) view.postInvalidate();
            touchCaughtBySlider = null;
            if(hintAppearingTimer != null){
                hintAppearingTimer.cancel();
                hintAppearingTimer = null;
            }
            hintAnimationPercent = 0;
            if(touchCaught) {
                touchCaught = false;
                return true;
            }
        }
        return touchCaught;
    }
    public class FloatingMenuSlider{
        private final int imageResource;
        private Bitmap imageBitmap = null;
        private Thread imageLoadingThread = null;
        private final float minimum;
        private final float maximum;
        private final Runnable onValueChanged;
        private final Runnable onTouchUp;
        public float value;
        public boolean visible;
        private boolean pressed = false;
        private float touchDownValue = 0;
        private float touchDownPositionX = 0;

        public FloatingMenuSlider(int imageResource, int initialValue, int minimum, int maximum, Runnable onValueChanged, Runnable onTouchUp, boolean visible) {
            this.imageResource = imageResource;
            value = initialValue;
            this.minimum = minimum;
            this.maximum = maximum;
            this.onValueChanged = onValueChanged;
            this.onTouchUp = onTouchUp;
            this.visible = visible;
        }
        public float getValue(){
            if(value > maximum)
                return  maximum;
            return Math.max(value, minimum);
        }
        public float coefficient(){
            float normalizedValue = getValue() - minimum;
            float normalizedMaximum = maximum - minimum;
            return normalizedValue / normalizedMaximum;
        }



        public void startLoadingImage() {
            if(imageBitmap == null && imageLoadingThread == null && imageResource != 0){
                imageLoadingThread = new Thread(() -> {
                    try{
                        imageBitmap = Tools.decodeResource(Data.store().activity.getResources(), imageResource, sliderHeight()/2, sliderHeight()/2);
                        if(view != null) view.postInvalidate();
                    }
                    catch (Exception e) {
                        Logger.log("Error loading image for FloatingMenu: " + e.getMessage());
                        e.printStackTrace();
                    }
                    finally {
                        imageLoadingThread = null;
                    }
                });
                imageLoadingThread.start();
            }
        }
    }
    public class FloatingMenuButton{
        private int imageResource;
        private Bitmap imageBitmap = null;
        private Thread imageLoadingThread = null;
        public boolean rotateIcon = false;
        private final String description;
        private final Runnable runnable;
        public boolean visible;
        private boolean pressed = false;
        public boolean highlighted = false;

        public FloatingMenuButton(int imageResource, String description, Runnable runnable, boolean visible) {
            this.imageResource = imageResource;
            this.description = description;
            this.runnable = runnable;
            this.visible = visible;
        }

        public void setImageResource(int image){
            this.imageResource = image;
            imageBitmap = null;
            imageLoadingThread = null;
        }

        public void startLoadingImage() {
            if(imageBitmap == null && imageLoadingThread == null && imageResource != 0){
                imageLoadingThread = new Thread(() -> {
                    try{
                        imageBitmap = Tools.decodeResource(Data.store().activity.getResources(), imageResource, buttonHeight()- padding()*2, buttonHeight()- padding()*2);
                        if(view != null) view.postInvalidate();
                    }
                    catch (Exception e) {
                        Logger.log("Error loading image for FloatingMenu: " + e.getMessage());
                        e.printStackTrace();
                    }
                    finally {
                        imageLoadingThread = null;
                    }
                });
                imageLoadingThread.start();
            }
        }
    }
}
