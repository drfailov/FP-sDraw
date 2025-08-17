package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.FloatingMenu;
import com.fsoft.FP_sDraw.menu.HintMenu;

/**
 * Created by Dr. Failov on 17.01.2023.
 * Updated by Dr. Failov on 17.09.2023.
 */
public class Figures implements Instrument{
    private final DrawCore draw;
    private final int STATE_CREATING = 1;
    private final int STATE_MOVING = 2;
    private int state = STATE_CREATING;
    private final int FIGURE_TRIANGLE = 0;
    private final int FIGURE_CIRCLE = 1;
    private final int FIGURE_RECTANGLE = 2;
    private final int FIGURE_ARROW = 3;
    private final int FIGURE_SQUIRCLE = 4;
    private final int FIGURE_CROSS = 5;
    private final int FIGURE_DICK = 228;
    private int figure = FIGURE_TRIANGLE;
    private final Paint paint = new Paint();

    private final FloatingMenu creatingFloatingMenu;
    private final FloatingMenu.FloatingMenuButton creatingFloatingMenuDick; //for easter egg
    private HintMenu hintCreating = null;
    private long lastOnSelect = 0; //for easter egg
    private long continuousOnSelectCount = 0; //for easter egg

    //moving
    private float movingRotationDegree = 0;
    private float movingCenterX = 0; //screen coordinates
    private float movingCenterY = 0; //screen coordinates
    private float movingWidth = 0; //screen coordinates
    private float movingHeight = 0; //screen coordinates
    private float figureStroke = 0;
    private float movingTouchDownPosition1X = -1;  //screen coordinates
    private float movingTouchDownPosition1Y = -1;  //screen coordinates
    private int movingTouchDownId1 = -1;
    private float movingTouchDownPosition2X = -1;  //screen coordinates
    private float movingTouchDownPosition2Y = -1;  //screen coordinates
    private int movingTouchDownId2 = -1;
    private float movingTouchDownCenterX = 0;  //screen coordinates
    private float movingTouchDownCenterY = 0;  //screen coordinates
    private float movingTouchDownWidth = 0;  //screen coordinates
    private float movingTouchDownHeight = 0;  //screen coordinates
    private float movingTouchDownRotateDegree = 0;


    private final Dot movingBottomRightDot = new Dot(Dot.DOT_BOTTOM_RIGHT);
    private final Dot movingBottomLeftDot = new Dot(Dot.DOT_BOTTOM_LEFT);
    private final Dot movingTopRightDot = new Dot(Dot.DOT_TOP_RIGHT);
    private final Dot movingTopLeftDot = new Dot(Dot.DOT_TOP_LEFT);
    private final FloatingMenu movingFloatingMenu;
    private final FloatingMenu.FloatingMenuSlider movingFloatingMenuOpacitySlider;
    private final FloatingMenu.FloatingMenuSlider movingFloatingMenuStrokeSlider;
    private HintMenu hintMoving = null;


    private class Dot{
        static public final int DOT_TOP_RIGHT = 4;
        static public final int DOT_TOP_LEFT = 3;
        static public final int DOT_BOTTOM_LEFT = 1;
        static public final int DOT_BOTTOM_RIGHT = 2;
        private final float touchRadius = Tools.dp(20);
        public final int type;

        private int caughtTouchId = -1;
        private float touchDownCenterX = 0;  //screen coordinates
        private float touchDownCenterY = 0;  //screen coordinates
        private float touchDownWidth = 0;  //screen coordinates
        private float touchDownHeight = 0;  //screen coordinates
        private float touchDownTouchPositionX = -1;  //screen coordinates
        private float touchDownTouchPositionY = -1;  //screen coordinates

        public Dot(int type) {
            this.type = type;
        }

        public boolean onTouch(MotionEvent event){
            if(event.getActionMasked() == MotionEvent.ACTION_DOWN){
                float dx = event.getX(event.getActionIndex()) - X();
                float dy = event.getY(event.getActionIndex()) - Y();
                float d = (float)Math.sqrt(dx*dx+dy*dy);
                if(d < touchRadius) {
                    touchDownTouchPositionX = event.getX(event.getActionIndex());
                    touchDownTouchPositionY = event.getY(event.getActionIndex());
                    caughtTouchId = event.getPointerId(event.getActionIndex());
                    touchDownCenterX = movingCenterX;
                    touchDownCenterY = movingCenterY;
                    touchDownWidth = movingWidth;
                    touchDownHeight = movingHeight;
                    return true;
                }
            }
            if(event.getActionMasked() == MotionEvent.ACTION_MOVE){
                if(caughtTouchId != -1) {
                    int index = event.findPointerIndex(caughtTouchId);
                    if (index != -1) {
                        //get input
                        float dx = event.getX(index) -  touchDownTouchPositionX;
                        float dy = event.getY(index) -  touchDownTouchPositionY;
                        { //rotate input
                            double rad = Math.toRadians(-movingRotationDegree);
                            double rx = dx * Math.cos(rad) - dy * Math.sin(rad);
                            double ry = dx * Math.sin(rad) + dy * Math.cos(rad);
                            dx = (float) rx;
                            dy = (float) ry;
                        }

                        //calculate
                        float dWidth = 0;
                        float dHeight = 0;
                        float dCenterX = 0;
                        float dCenterY = 0;
                        if(type == DOT_BOTTOM_RIGHT) {
                            dHeight = dy;
                            dWidth = dx;
                            dCenterX = dx / 2;
                            dCenterY = dy / 2;
                        }
                        if(type == DOT_BOTTOM_LEFT) {
                            dHeight = dy;
                            dWidth = -dx;
                            dCenterX = dx / 2;
                            dCenterY = dy / 2;
                        }
                        if(type == DOT_TOP_LEFT) {
                            dHeight = -dy;
                            dWidth = -dx;
                            dCenterX = dx / 2;
                            dCenterY = dy / 2;
                        }
                        if(type == DOT_TOP_RIGHT) {
                            dHeight = -dy;
                            dWidth = dx;
                            dCenterX = dx / 2;
                            dCenterY = dy / 2;
                        }

                        { //rotate output
                            double rad = Math.toRadians(movingRotationDegree);
                            double rx = dCenterX * Math.cos(rad) - dCenterY * Math.sin(rad);
                            double ry = dCenterX * Math.sin(rad) + dCenterY * Math.cos(rad);
                            dCenterX = (float)rx;
                            dCenterY = (float)ry;
                        }
                        //apply
                        movingWidth = touchDownWidth + dWidth;
                        movingHeight = touchDownHeight + dHeight;
                        movingCenterX = touchDownCenterX + dCenterX;
                        movingCenterY = touchDownCenterY + dCenterY;

                        //magnet square
                        float threshold = Tools.dp(10);
                        float nonSquarity = movingWidth - movingHeight;
                        if(Math.abs(nonSquarity) < threshold) {
                            movingWidth -= nonSquarity;
                            if(type == DOT_TOP_LEFT || type == DOT_BOTTOM_LEFT)
                                movingCenterX += nonSquarity/2;
                            if(type == DOT_TOP_RIGHT || type == DOT_BOTTOM_RIGHT)
                                movingCenterX -= nonSquarity/2;
                        }
                    }
                    return true;
                }
            }
            if(event.getActionMasked() == MotionEvent.ACTION_UP){
                if(caughtTouchId != -1) {
                    caughtTouchId = -1;
                    return true;
                }
            }
            return false;
        }

        public void draw(Canvas canvas){
            paint.setColor(Color.argb(100, 0,0,0)); //BLACK
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            float radius = touchRadius / 3f;
            if(caughtTouchId != -1)
                radius *= 1.5f;
            canvas.drawCircle(X(), Y(), radius + Tools.dp(1), paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(X(), Y(),radius, paint);
        }


        private float X(){
            if(type == DOT_BOTTOM_RIGHT)
                return movingGetBottomRightX();
            if(type == DOT_BOTTOM_LEFT)
                return movingGetBottomLeftX();
            if(type == DOT_TOP_RIGHT)
                return movingGetTopRightX();
            if(type == DOT_TOP_LEFT)
                return movingGetTopLeftX();
            return 0;
        }
        private float Y(){
            if(type == DOT_BOTTOM_RIGHT)
                return movingGetBottomRightY();
            if(type == DOT_BOTTOM_LEFT)
                return movingGetBottomLeftY();
            if(type == DOT_TOP_RIGHT)
                return movingGetTopRightY();
            if(type == DOT_TOP_LEFT)
                return movingGetTopLeftY();
            return 0;
        }



        private float movingGetBottomRightX(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterX + (float)((movingWidth / 2f * Math.cos(rad)) - (movingHeight / 2f * Math.sin(rad)));
        }
        private float movingGetBottomRightY(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterY + (float)((movingWidth / 2f * Math.sin(rad)) + (movingHeight / 2f * Math.cos(rad)));
        }

        private float movingGetBottomLeftX(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterX + (float)((-movingWidth / 2f * Math.cos(rad)) - (movingHeight / 2f * Math.sin(rad)));
        }
        private float movingGetBottomLeftY(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterY + (float)((-movingWidth / 2f * Math.sin(rad)) + (movingHeight / 2f * Math.cos(rad)));
        }

        private float movingGetTopLeftX(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterX + (float)((-movingWidth / 2f * Math.cos(rad)) - (-movingHeight / 2f * Math.sin(rad)));
        }
        private float movingGetTopLeftY(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterY + (float)((-movingWidth / 2f * Math.sin(rad)) + (-movingHeight / 2f * Math.cos(rad)));
        }

        private float movingGetTopRightX(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterX + (float)((movingWidth / 2f * Math.cos(rad)) - (-movingHeight / 2f * Math.sin(rad)));
        }
        private float movingGetTopRightY(){ //screen coordinates
            double rad = Math.toRadians(movingRotationDegree);
            return movingCenterY + (float)((movingWidth / 2f * Math.sin(rad)) + (-movingHeight / 2f * Math.cos(rad)));
        }
    }



    public Figures(DrawCore d){
        draw = d;
        creatingFloatingMenu = new FloatingMenu(d.view);
        creatingFloatingMenu.addButton(R.drawable.ic_triangle, Data.tools.getResource(R.string.triangle), this::onTriangleSelected, true);
        creatingFloatingMenu.addButton(R.drawable.ic_rectangle, Data.tools.getResource(R.string.rectangle), this::onRectangleSelected, true);
        creatingFloatingMenu.addButton(R.drawable.ic_squircle, Data.tools.getResource(R.string.squircle), this::onSqiurcleSelected, true);
        creatingFloatingMenu.addButton(R.drawable.ic_ellipse, Data.tools.getResource(R.string.oval), this::onOvalSelected, true);
        creatingFloatingMenu.addButton(R.drawable.ic_arrow, Data.tools.getResource(R.string.arrow), this::onArrowSelected, true);
        creatingFloatingMenu.addButton(R.drawable.ic_cancel, Data.tools.getResource(R.string.cross), this::onCrossSelected, true);
        creatingFloatingMenuDick = creatingFloatingMenu.addButton(R.drawable.ic_mosaic, Data.tools.getResource(R.string.mosaic), this::onDickSelected, false);

        movingFloatingMenu = new FloatingMenu(d.view);
        movingFloatingMenu.addButton(R.drawable.ic_cancel, Data.tools.getResource(R.string.cancel), this::onCancelClick, true);
        movingFloatingMenu.addButton(R.drawable.ic_stamp, Data.tools.getResource(R.string.insert_copy), this::onCopyClick, true);
        movingFloatingMenu.addButton(R.drawable.ic_check, Data.tools.getResource(R.string.apply), this::onApplyClick, true);
        movingFloatingMenuOpacitySlider = movingFloatingMenu.addSlider(R.drawable.ic_opacity, (Integer)Data.get(Data.brushOpacityInt()), 0, 255, this::onOpacityUpdated, this::onOpacityUpdated, true);
        movingFloatingMenuStrokeSlider = movingFloatingMenu.addSlider(R.drawable.menu_line, 0, 0, Tools.dp(50), this::onStrokeUpdated, this::onStrokeUpdated, true);
    }
    private void onApplyClick(){
        onCopyClick();
        onCancelClick();
    }
    private void onCancelClick(){
        state = STATE_CREATING;
        movingRotationDegree = 0;
        movingFloatingMenuOpacitySlider.value = (Integer)Data.get(Data.brushOpacityInt());
        movingCenterX = 0; //screen coordinates
        movingCenterY = 0; //screen coordinates
        movingWidth = 0; //screen coordinates
        movingHeight = 0; //screen coordinates
        draw.redraw();
    }
    private void onOpacityUpdated(){
        draw.redraw();
    }
    private void onStrokeUpdated(){
        figureStroke = (int) movingFloatingMenuStrokeSlider.getValue();
        draw.redraw();
    }


    private void onTriangleSelected(){
        figure = FIGURE_TRIANGLE;
        createFigureInCenterOfScreen();
    }
    private void onRectangleSelected(){
        figure = FIGURE_RECTANGLE;
        createFigureInCenterOfScreen();
    }
    private void onSqiurcleSelected(){
        figure = FIGURE_SQUIRCLE;
        createFigureInCenterOfScreen();
    }
    private void onOvalSelected(){
        figure = FIGURE_CIRCLE;
        createFigureInCenterOfScreen();
    }
    private void onArrowSelected(){
        figure = FIGURE_ARROW;
        createFigureInCenterOfScreen();
    }
    private void onCrossSelected(){
        figure = FIGURE_CROSS;
        createFigureInCenterOfScreen();
    }
    private void onDickSelected(){
        figure = FIGURE_DICK;
        createFigureInCenterOfScreen();
    }
    private void createFigureInCenterOfScreen(){
        movingCenterX = draw.getWidth()/2f;
        movingCenterY = draw.getHeight()/3f;
        movingWidth = Math.min(draw.getWidth(), draw.getHeight()) /3f;
        movingHeight = Math.min(draw.getWidth(), draw.getHeight()) /3f;
        movingRotationDegree = 0;
        state = STATE_MOVING;
    }

    private void onCopyClick(){
        int color = Tools.removeTransparency(Data.getBrushColor());
        color = Color.argb((int)movingFloatingMenuOpacitySlider.getValue(), Color.red(color), Color.green(color), Color.blue(color));
        if(figure == FIGURE_TRIANGLE)
            drawTriangle(draw.canvas, draw.scale.ScreenToImageX(movingCenterX), draw.scale.ScreenToImageY(movingCenterY), movingWidth / draw.scale.scale_size, movingHeight / draw.scale.scale_size, movingRotationDegree, figureStroke, color);
        if(figure == FIGURE_RECTANGLE)
            drawRectangle(draw.canvas, draw.scale.ScreenToImageX(movingCenterX), draw.scale.ScreenToImageY(movingCenterY), movingWidth / draw.scale.scale_size, movingHeight / draw.scale.scale_size, movingRotationDegree, figureStroke, color);
        if(figure == FIGURE_SQUIRCLE)
            drawSquircle(draw.canvas, draw.scale.ScreenToImageX(movingCenterX), draw.scale.ScreenToImageY(movingCenterY), movingWidth / draw.scale.scale_size, movingHeight / draw.scale.scale_size, movingRotationDegree, figureStroke, color);
        if(figure == FIGURE_CIRCLE)
            drawCircle(draw.canvas, draw.scale.ScreenToImageX(movingCenterX), draw.scale.ScreenToImageY(movingCenterY), movingWidth / draw.scale.scale_size, movingHeight / draw.scale.scale_size, movingRotationDegree, figureStroke, color);
        if(figure == FIGURE_ARROW)
            drawArrow(draw.canvas, draw.scale.ScreenToImageX(movingCenterX), draw.scale.ScreenToImageY(movingCenterY), movingWidth / draw.scale.scale_size, movingHeight / draw.scale.scale_size, movingRotationDegree, figureStroke, color);
        if(figure == FIGURE_CROSS)
            drawCross(draw.canvas, draw.scale.ScreenToImageX(movingCenterX), draw.scale.ScreenToImageY(movingCenterY), movingWidth / draw.scale.scale_size, movingHeight / draw.scale.scale_size, movingRotationDegree, figureStroke, color);
        if(figure == FIGURE_DICK)
            drawDick(draw.canvas, draw.scale.ScreenToImageX(movingCenterX), draw.scale.ScreenToImageY(movingCenterY), movingWidth / draw.scale.scale_size, movingHeight / draw.scale.scale_size, movingRotationDegree, figureStroke, color);
        draw.lastChangeToBitmap = System.currentTimeMillis();

        float dx = movingWidth/2;
        float dy = movingHeight/2;
        float radius = (float)Math.sqrt(dx*dx+dy*dy);
        radius += Tools.dp(10);
        if(draw.undoAreaCalculator != null){
            draw.undoProvider.apply(undoAreaImageCoordinates());
        }
        else {
            draw.undoProvider.apply( movingCenterY-radius , movingCenterY + radius, movingCenterX - radius, movingCenterX + radius);
        }
        draw.undoProvider.prepare();

        movingCenterX += Tools.dp(10);
        movingCenterY += Tools.dp(10);
        draw.redraw();
    }
    private Rect undoAreaImageCoordinates(){
        if(draw.undoAreaCalculator != null) {
            draw.undoAreaCalculator.reset();
            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingTopLeftDot.X()), draw.scale.ScreenToImageY(movingTopLeftDot.Y()), figureStroke);
            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingBottomLeftDot.X()), draw.scale.ScreenToImageY(movingBottomLeftDot.Y()), figureStroke);
            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingTopRightDot.X()), draw.scale.ScreenToImageY(movingTopRightDot.Y()), figureStroke);
            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingBottomRightDot.X()), draw.scale.ScreenToImageY(movingBottomRightDot.Y()), figureStroke);

            return new Rect(draw.undoAreaCalculator.left, draw.undoAreaCalculator.top, draw.undoAreaCalculator.right, draw.undoAreaCalculator.bottom);
        }
        return new Rect();
    }
    private Rect undoAreaScreenCoordinates(){
        if(draw.undoAreaCalculator != null) {
            draw.undoAreaCalculator.reset();
            draw.undoAreaCalculator.add((movingTopLeftDot.X()),     (movingTopLeftDot.Y()), figureStroke);
            draw.undoAreaCalculator.add((movingBottomLeftDot.X()),  (movingBottomLeftDot.Y()), figureStroke);
            draw.undoAreaCalculator.add((movingTopRightDot.X()),    (movingTopRightDot.Y()), figureStroke);
            draw.undoAreaCalculator.add((movingBottomRightDot.X()), (movingBottomRightDot.Y()), figureStroke);

            return new Rect(draw.undoAreaCalculator.left, draw.undoAreaCalculator.top, draw.undoAreaCalculator.right, draw.undoAreaCalculator.bottom);
        }
        return new Rect();
    }


    @Override
    public String getName() {
        return "figures";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentFigure);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.menu_figures;
    }

    @Override
    public void onSelect() {
        //for easter egg
        long curTime = System.currentTimeMillis();
        if(curTime - lastOnSelect < 500){
            continuousOnSelectCount ++;
            if(continuousOnSelectCount > 10){
                creatingFloatingMenuDick.visible = true;
            }
        }
        else {
            continuousOnSelectCount = 0;
        }
        lastOnSelect = curTime;

    }

    @Override
    public void onDeselect() {
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        if(state == STATE_CREATING) {
            //trigger zoom if over 2 fingers
            if (event.getPointerCount() > 1) {// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
                if (Data.tools.isFullVersion()) {
                    movingCenterX = 0;
                    movingCenterY = 0;
                    draw.setInstrument(draw.scale);
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.figures);
                } else
                    Data.tools.showBuyFullDialog();
                return true;
            }

            //process hint events
            if(hintCreating != null && hintCreating.processTouch(event))
                return true;
            if(Data.tools.isAllowedDeviceForUi(event) && creatingFloatingMenu != null && creatingFloatingMenu.processTouch(event))
                return true;

            //move canvas if touched by finger but activated  sPen mode
            if(!Data.tools.isAllowedDevice(event)) {
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.figures);
                }
                return true;
            }
        }
        if(state == STATE_MOVING){
            if(hintMoving != null && hintMoving.processTouch(event))
                return true;

            if(Data.tools.isAllowedDeviceForUi(event) && movingFloatingMenu != null && movingFloatingMenu.processTouch(event))
                return true;
            if(movingBottomRightDot.onTouch(event)) {
                draw.redraw();
                return true;
            }
            if(movingBottomLeftDot.onTouch(event)) {
                draw.redraw();
                return true;
            }
            if(movingTopRightDot.onTouch(event)) {
                draw.redraw();
                return true;
            }
            if(movingTopLeftDot.onTouch(event)) {
                draw.redraw();
                return true;
            }


            if(event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP){
                int index1 = 0;
                int index2 = 1;
                if(event.getActionMasked() == MotionEvent.ACTION_POINTER_UP){
                    int actionIndex = event.getActionIndex();
                    if(actionIndex == 0)
                        index1++;
                    if(actionIndex <= 1)
                        index2 ++;
                }
                if(index1 >= event.getPointerCount())
                    index1 = -1;
                if(index2 >= event.getPointerCount())
                    index2 = -1;
                movingTouchDownId1 = event.getPointerId(index1);
                movingTouchDownPosition1X = event.getX(index1);
                movingTouchDownPosition1Y = event.getY(index1);
                if(index2 != -1){
                    movingTouchDownId2 = event.getPointerId(index2);
                    movingTouchDownPosition2X = event.getX(index2);
                    movingTouchDownPosition2Y = event.getY(index2);
                }
                movingTouchDownCenterX = movingCenterX;
                movingTouchDownCenterY = movingCenterY;
                movingTouchDownWidth = movingWidth;
                movingTouchDownHeight = movingHeight;
                movingTouchDownRotateDegree = movingRotationDegree;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE){
                if(event.getPointerCount() == 1) { //moving only
                    int index = event.findPointerIndex(movingTouchDownId1);
                    if(index != -1 && movingTouchDownPosition1X != -1){
                        float dx = event.getX(index) - movingTouchDownPosition1X;
                        float dy = event.getY(index) - movingTouchDownPosition1Y;
                        movingCenterX = movingTouchDownCenterX + dx;
                        movingCenterY = movingTouchDownCenterY + dy;
                    }
                }
                if(event.getPointerCount() == 2) { //moving, rotating, zooming
                    int index1 = event.findPointerIndex(movingTouchDownId1);
                    int index2 = event.findPointerIndex(movingTouchDownId2);
                    if(index1 != -1 && movingTouchDownPosition1X != -1 && index2 != -1 && movingTouchDownPosition2X != -1)
                    {
                        float touchDownTouchCenterX = (movingTouchDownPosition1X + movingTouchDownPosition2X) / 2f;
                        float touchDownTouchCenterY = (movingTouchDownPosition1Y + movingTouchDownPosition2Y) / 2f;
                        float currentTouchCenterX = (event.getX(index1) + event.getX(index2)) / 2f;
                        float currentTouchCenterY = (event.getY(index1) + event.getY(index2)) / 2f;
                        float currentTranslateX = currentTouchCenterX - touchDownTouchCenterX;
                        float currentTranslateY = currentTouchCenterY - touchDownTouchCenterY;
                        float touchDownDistanceDX = Math.abs(movingTouchDownPosition1X - movingTouchDownPosition2X);
                        float touchDownDistanceDY = Math.abs(movingTouchDownPosition1Y - movingTouchDownPosition2Y);
                        float touchDownDistance = (float)Math.sqrt(touchDownDistanceDX*touchDownDistanceDX + touchDownDistanceDY*touchDownDistanceDY);

                        float currentDistanceDX = Math.abs(event.getX(index1) - event.getX(index2));
                        float currentDistanceDY = Math.abs(event.getY(index1) - event.getY(index2));
                        float currentTouchDistance = (float)Math.sqrt(currentDistanceDX*currentDistanceDX + currentDistanceDY*currentDistanceDY);
                        float scale = currentTouchDistance / touchDownDistance;

                        float currentImageCenterPositionX =  movingTouchDownCenterX + currentTranslateX;
                        float currentImageCenterPositionY =  movingTouchDownCenterY + currentTranslateY;
                        float currentDistanceFromTouchToImageX = currentTouchCenterX - currentImageCenterPositionX;
                        float currentDistanceFromTouchToImageY = currentTouchCenterY - currentImageCenterPositionY;
                        float translateCorrectionForZoomingX = currentDistanceFromTouchToImageX * (1-scale);
                        float translateCorrectionForZoomingY = currentDistanceFromTouchToImageY * (1-scale);

                        float touchDownRadian = (float)Math.atan2(movingTouchDownPosition1Y - movingTouchDownPosition2Y, movingTouchDownPosition1X - movingTouchDownPosition2X);
                        float currentTouchRadian = (float)Math.atan2(event.getY(index1) - event.getY(index2), event.getX(index1) - event.getX(index2));
                        float angleDifferenceRad = currentTouchRadian - touchDownRadian;
                        float angleDifferenceDeg = (float)Math.toDegrees(angleDifferenceRad);
                        float translateCorrectionForRotatingX = (float)(currentDistanceFromTouchToImageX * Math.cos(angleDifferenceRad) - currentDistanceFromTouchToImageY * Math.sin(angleDifferenceRad));
                        float translateCorrectionForRotatingY = (float)(currentDistanceFromTouchToImageX * Math.sin(angleDifferenceRad) + currentDistanceFromTouchToImageY * Math.cos(angleDifferenceRad));
                        translateCorrectionForRotatingX = currentDistanceFromTouchToImageX - translateCorrectionForRotatingX;
                        translateCorrectionForRotatingY = currentDistanceFromTouchToImageY - translateCorrectionForRotatingY;

                        movingHeight = movingTouchDownHeight * scale;
                        movingWidth = movingTouchDownWidth * scale;
                        movingRotationDegree = movingTouchDownRotateDegree + angleDifferenceDeg;
                        movingCenterX = movingTouchDownCenterX + currentTranslateX + translateCorrectionForZoomingX + translateCorrectionForRotatingX;
                        movingCenterY = movingTouchDownCenterY + currentTranslateY + translateCorrectionForZoomingY + translateCorrectionForRotatingY;

                        float rotateMagnetDegree = 4;
                        float[] rotateMagnets = {360f+0f, 360f+45f, 360f+90f, 360f+90f+45f, 360f+180f, 360f+180f+45f, 360f+270f, 360f+270f+45f, 360+360f}; //working within range 360 - 720 degree
                        while(movingRotationDegree < 360)
                            movingRotationDegree += 360;
                        while(movingRotationDegree > 720)
                            movingRotationDegree -= 360;
                        for (float magnet : rotateMagnets) {
                            if(movingRotationDegree > magnet - rotateMagnetDegree/2f && movingRotationDegree < magnet + rotateMagnetDegree/2f)
                                movingRotationDegree = magnet;
                        }
                    }
                }
            }
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
                movingTouchDownId1 = -1;
                movingTouchDownId2 = -1;
            }
            draw.redraw();
        }
        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {

        //figure
        if(movingCenterX != 0 || movingCenterY != 0){
            int color = Tools.removeTransparency(Data.getBrushColor());
            color = Color.argb((int)movingFloatingMenuOpacitySlider.getValue(), Color.red(color), Color.green(color), Color.blue(color));
            if(figure == FIGURE_TRIANGLE)
                drawTriangle(canvas, movingCenterX, movingCenterY, movingWidth, movingHeight, movingRotationDegree, figureStroke*draw.scale.scale_size, color);
            if(figure == FIGURE_RECTANGLE)
                drawRectangle(canvas, movingCenterX, movingCenterY, movingWidth, movingHeight, movingRotationDegree, figureStroke*draw.scale.scale_size, color);
            if(figure == FIGURE_SQUIRCLE)
                drawSquircle(canvas, movingCenterX, movingCenterY, movingWidth, movingHeight, movingRotationDegree, figureStroke*draw.scale.scale_size, color);
            if(figure == FIGURE_CIRCLE)
                drawCircle(canvas, movingCenterX, movingCenterY, movingWidth, movingHeight, movingRotationDegree, figureStroke*draw.scale.scale_size, color);
            if(figure == FIGURE_ARROW)
                drawArrow(canvas, movingCenterX, movingCenterY, movingWidth, movingHeight, movingRotationDegree, figureStroke*draw.scale.scale_size, color);
            if(figure == FIGURE_CROSS)
                drawCross(canvas, movingCenterX, movingCenterY, movingWidth, movingHeight, movingRotationDegree, figureStroke*draw.scale.scale_size, color);
            if(figure == FIGURE_DICK)
                drawDick(canvas, movingCenterX, movingCenterY, movingWidth, movingHeight, movingRotationDegree, figureStroke*draw.scale.scale_size, color);
        }

        if(state == STATE_CREATING){
            if(creatingFloatingMenu != null && draw != null) {
                float menuHeight = creatingFloatingMenu.height();
                float menuWidth = creatingFloatingMenu.width();
                float menuY = (draw.getHeight())*(3/4f)   + menuHeight;
                creatingFloatingMenu.setTargetPosition((draw.getWidth() - menuWidth)/2f, menuY);
                if(movingFloatingMenu != null) movingFloatingMenu.setLastPosition((draw.getWidth() - menuWidth)/2f, menuY);
                creatingFloatingMenu.setBounds(draw.getWidth(), draw.getHeight());
                creatingFloatingMenu.clearNoGoZones();
                creatingFloatingMenu.addNoGoZones(draw.getNoGoZones());
                creatingFloatingMenu.draw(canvas);
            }
            if(hintCreating == null)
                hintCreating = new HintMenu(draw, Data.tools.getResource(R.string.hint_figures_creating), R.drawable.ic_help, "FIGURE_CREATING", HintMenu.SHOW_TIMES);
            hintCreating.draw(canvas);
        }
        if(state == STATE_MOVING){
            if(movingTouchDownId1 == -1) {
                //frame
                paint.setStyle(Paint.Style.STROKE);
                paint.setAntiAlias(true);
                paint.setColor(Color.argb(50, 0, 0, 0));
                paint.setStrokeWidth(Data.store().DPI / 60f);
                canvas.save();
                canvas.rotate(movingRotationDegree, movingCenterX, movingCenterY);
                canvas.drawRect(movingCenterX - movingWidth / 2f, movingCenterY - movingHeight / 2f, movingCenterX + movingWidth / 2f, movingCenterY + movingHeight / 2f, paint);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(Data.store().DPI / 200f);
                canvas.drawRect(movingCenterX - movingWidth / 2f, movingCenterY - movingHeight / 2f, movingCenterX + movingWidth / 2f, movingCenterY + movingHeight / 2f, paint);
                canvas.restore();

                //points
                movingBottomLeftDot.draw(canvas);
                movingBottomRightDot.draw(canvas);
                movingTopRightDot.draw(canvas);
                movingTopLeftDot.draw(canvas);
            }


            if(movingFloatingMenu != null && draw != null) {
                float menuWidth = movingFloatingMenu.width();
                Rect undoArea = undoAreaScreenCoordinates();
                movingFloatingMenu.setTargetPosition(movingCenterX - (menuWidth/2f), undoArea.bottom + movingFloatingMenu.margin());
                if(creatingFloatingMenu != null) creatingFloatingMenu.setLastPosition(movingCenterX - (menuWidth/2f), undoArea.bottom + movingFloatingMenu.margin());
                movingFloatingMenu.setBounds(draw.getWidth(), draw.getHeight());
                movingFloatingMenu.clearNoGoZones();
                movingFloatingMenu.addNoGoZone(undoArea);
                movingFloatingMenu.addNoGoZones(draw.getNoGoZones());
                movingFloatingMenu.draw(canvas);
            }
            if(hintMoving == null)
                hintMoving = new HintMenu(draw, Data.tools.getResource(R.string.hint_figures_moving), R.drawable.ic_help, "FIGURE_MOVING", HintMenu.SHOW_TIMES);
            hintMoving.draw(canvas);
        }

        //update area where will be undo
        if(draw != null && draw.undoAreaCalculator != null && (movingCenterX != 0 || movingCenterY != 0)){
            draw.undoAreaCalculator.reset();

            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingTopLeftDot.X()), draw.scale.ScreenToImageY(movingTopLeftDot.Y()), figureStroke);
            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingBottomLeftDot.X()), draw.scale.ScreenToImageY(movingBottomLeftDot.Y()), figureStroke);
            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingTopRightDot.X()), draw.scale.ScreenToImageY(movingTopRightDot.Y()), figureStroke);
            draw.undoAreaCalculator.add(draw.scale.ScreenToImageX(movingBottomRightDot.X()), draw.scale.ScreenToImageY(movingBottomRightDot.Y()), figureStroke);
        }
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
        return view -> draw.setInstrument(Figures.this);
    }

    public void drawTriangle(Canvas canvas, float cx, float cy, float width, float height, float rotationDegree, float stroke, int color){
        if(stroke <= 0) {
            paint.setStyle(Paint.Style.FILL);
        }
        else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
        }
        paint.setColor(color);
        paint.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));



        Path path = new Path();
        path.moveTo(cx, cy - height/2);
        path.lineTo(cx + width/2, cy + height/2);
        path.lineTo(cx - width/2, cy + height/2);
        path.lineTo(cx, cy - height/2);
        path.close();

        canvas.save();
        canvas.rotate(rotationDegree, cx, cy);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void drawRectangle(Canvas canvas, float cx, float cy, float width, float height, float rotationDegree, float stroke, int color){
        if(stroke <= 0) {
            paint.setStyle(Paint.Style.FILL);
        }
        else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
        }
        paint.setColor(color);
        paint.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));



        Path path = new Path();
        path.moveTo(cx + width/2, cy + height/2);
        path.lineTo(cx - width/2, cy + height/2);
        path.lineTo(cx - width/2, cy - height/2);
        path.lineTo(cx + width/2, cy - height/2);
        path.lineTo(cx + width/2, cy + height/2);
        path.close();

        canvas.save();
        canvas.rotate(rotationDegree, cx, cy);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void drawSquircle(Canvas canvas, float cx, float cy, float width, float height, float rotationDegree, float stroke, int color){
        if(stroke <= 0) {
            paint.setStyle(Paint.Style.FILL);
        }
        else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
        }
        paint.setColor(color);
        paint.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));



        Path path = Tools.getSquircleCenterPath(cx, cy, Math.min(Math.abs(width), Math.abs(height))/2f);

        canvas.save();
        canvas.rotate(rotationDegree, cx, cy);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void drawCircle(Canvas canvas, float cx, float cy, float width, float height, float rotationDegree, float stroke, int color){
        if(stroke <= 0) {
            paint.setStyle(Paint.Style.FILL);
        }
        else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
        }
        paint.setColor(color);
        paint.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));

        canvas.save();
        canvas.rotate(rotationDegree, cx, cy);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawOval(cx - width/2f, cy - height / 2f, cx + width / 2f, cy+height/2f, paint);
        }
        else {
            canvas.drawOval(new RectF(cx - width/2f, cy - height / 2f, cx + width / 2f, cy+height/2f), paint);
        }
        canvas.restore();
    }

    public void drawCross(Canvas canvas, float cx, float cy, float width, float height, float rotationDegree, float stroke, int color) {
        if(stroke <= 0){
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((Math.min(width, height))/5);
        }
        else{
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
        }
        paint.setColor(color);
        canvas.save();
        canvas.rotate(rotationDegree, cx, cy);
        float sz=0.4f;
        canvas.drawLine(cx-width*sz, cy-height*sz, cx+width*sz, cy+height*sz, paint);
        canvas.drawLine(cx-width*sz, cy+height*sz, cx+width*sz, cy-height*sz, paint);
        canvas.restore();
    }

    @SuppressWarnings({"DuplicateExpressions", "PointlessArithmeticExpression"})
    public void drawArrow(Canvas canvas, float cx, float cy, float width, float height, float rotationDegree, float stroke, int color){
        if(stroke <= 0) {
            paint.setStyle(Paint.Style.FILL);
        }
        else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
        }
        paint.setColor(color);
        paint.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));

        Path path = new Path();
        path.moveTo(cx - width / 2.0f + width * (0.0f / 9.0f), cy - height / 2.0f + height * (1.2f / 4.0f));
        path.lineTo(cx - width / 2.0f + width * (5.0f / 9.0f), cy - height / 2.0f + height * (1.2f / 4.0f));
        path.lineTo(cx - width / 2.0f + width * (5.0f / 9.0f), cy - height / 2.0f + height * (0.0f / 4.0f));
        path.lineTo(cx - width / 2.0f + width * (9.0f / 9.0f), cy - height / 2.0f + height * (2.0f / 4.0f));
        path.lineTo(cx - width / 2.0f + width * (5.0f / 9.0f), cy - height / 2.0f + height * (4.0f / 4.0f));
        path.lineTo(cx - width / 2.0f + width * (5.0f / 9.0f), cy - height / 2.0f + height * (2.8f / 4.0f));
        path.lineTo(cx - width / 2.0f + width * (0.0f / 9.0f), cy - height / 2.0f + height * (2.8f / 4.0f));
        path.lineTo(cx - width / 2.0f + width * (0.0f / 9.0f), cy - height / 2.0f + height * (1.2f / 4.0f));
        path.close();

        canvas.save();
        canvas.rotate(rotationDegree, cx, cy);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    public void drawDick(Canvas c, float cx, float cy, float width, float height, float rotationDegree, float stroke, int color) {
        RectF area = new RectF(cx - width/2, cy-height/2, cx+width/2, cy+height/2);
        if(stroke <= 0)
            paint.setStyle(Paint.Style.FILL);
        else{
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
        }
        paint.setColor(color);
        c.save();
        c.rotate(rotationDegree, area.centerX(), area.centerY());
        c.drawRect(
                area.left + area.width()*(2f/6f),
                area.top + area.height()*(1f/6f),
                area.left + area.width()*(4f/6f),
                area.top + area.height()*(5f/6f),
                paint
        );
        c.drawCircle(
                area.centerX(),
                area.top + area.height()*(1f/6f),
                area.width()*(1f/6f),
                paint);
        c.drawCircle(
                area.left + area.width()*(2f/6f),
                area.top + area.height()*(5f/6f),
                area.width()*(1f/6f),
                paint);
        c.drawCircle(
                area.left + area.width()*(4f/6f),
                area.top + area.height()*(5f/6f),
                area.width()*(1f/6f),
                paint);
        c.restore();
    }//easter egg
}
