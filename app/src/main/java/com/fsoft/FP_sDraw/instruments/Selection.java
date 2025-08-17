package com.fsoft.FP_sDraw.instruments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.BackgroundRemover;
import com.fsoft.FP_sDraw.menu.CircleButtonNextToMenuButton;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.FloatingMenu;
import com.fsoft.FP_sDraw.menu.HintMenu;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by Dr.Failov, 2023-01-20
 */
public class Selection implements Instrument {
    private final int STATE_SELECTING = 0;
    private final int STATE_SELECTING_PRECISE = 1;
    private final int STATE_CALCULATING_SELECTION = 2;
    private final int STATE_MOVING = 3;

    private final DrawCore draw;
    private final Paint paint = new Paint();
    private int state = STATE_SELECTING;
    private final RectF selectedArea = new RectF(-1,-1,-1,-1); //is also used as synchronization object for bitmap
    public Bitmap selectedImage = null; //это выделенная область
    private Bitmap selectedImageBackup = null; //backup to restore image after modifications
    private HintMenu hintSelecting = null;
    private HintMenu hintPrecision = null;
    private HintMenu hintMoving = null;

    //selection
    private final ArrayList<Point> points = new ArrayList<>();
    private final float minimumPointInterval = Data.store().DPI/30;
    private Thread selectingCalculationThread = null;
    private final CircleButtonNextToMenuButton[] buttonsForSelecting;

    //preciseSelection
    private final Point preciseSelectingCursorPosition = new Point(-1, -1);
    private final Point preciseSelectingTouchDownCursorPosition = new Point(-1, -1);
    private final Point preciseSelectingTouchDownPosition = new Point(-1, -1);
    private int preciseSelectingTouchDownTouchId = -1;
    private final Paint preciseSelectingCursorPaint = new Paint();
    private Bitmap preciseSelectingCursorBitmap = null;
    private final CircleButtonNextToMenuButton preciseSelectingButtonGoRegularSelection;
    private final CircleButtonNextToMenuButton preciseSelectingButtonApply;
    private final CircleButtonNextToMenuButton preciseSelectingButtonCancel;
    private final CircleButtonNextToMenuButton preciseSelectingButtonMinus;
    private final CircleButtonNextToMenuButton[] buttonsForPreciseSelecting;

    //moving
    private float movingTranslateX = 0;
    private float movingTranslateY = 0;
    private float movingScale = 1;
    private float movingRotateDegree = 0;
    private int movingAlpha = 255;
    //moving touch tracking
    private final Point touchDownPosition1 = new Point(-1, -1);
    private int touchDownId1 = -1;
    private final Point touchDownPosition2 = new Point(-1, -1);
    private int touchDownId2 = -1;
    private float touchDownTranslateX = 0;
    private float touchDownTranslateY = 0;
    private float touchDownScale = 0;
    private float touchDownRotateDegree = 0;
    private int removeBackgroundDominantColor = 0;
    private final FloatingMenu floatingMenuForMoving;
    FloatingMenu.FloatingMenuButton mirrorButton;
    FloatingMenu.FloatingMenuButton removeBackgroundButton;
    FloatingMenu.FloatingMenuSlider removeBackgroundSlider;
    FloatingMenu.FloatingMenuSlider opacitySlider;
    CircleButtonNextToMenuButton movingCancelButton;
    CircleButtonNextToMenuButton movingDeleteButton;


    public Selection(DrawCore draw) {
        this.draw = draw;
        buttonsForSelecting = new CircleButtonNextToMenuButton[]{
                new CircleButtonNextToMenuButton(draw, 1.15f, Data.store().DPI / 7f, R.drawable.ic_cursor, getSelectPreciseRunnable()),
                new CircleButtonNextToMenuButton(draw, 2.15f, Data.store().DPI / 7f, R.drawable.ic_select_all, getSelectAllRunnable())
        };
        floatingMenuForMoving = new FloatingMenu(draw.view);
        mirrorButton = floatingMenuForMoving.addButton(R.drawable.ic_mirror, Data.tools.getResource(R.string.mirror), this::onMirrorClick, true);
        removeBackgroundButton = floatingMenuForMoving.addButton(R.drawable.ic_remove_background, Data.tools.getResource(R.string.deleteBackground), this::onRemoveBackgroundClick, true);
        //floatingMenuForMoving.addButton(R.drawable.menu_save, Data.tools.getResource(R.string.saveFragment), this::onSaveClick, true);
        floatingMenuForMoving.addButton(R.drawable.ic_copy, Data.tools.getResource(R.string.copy_selected_to_clipboard), this::onCopyToClipboardClick, true);
        floatingMenuForMoving.addButton(R.drawable.ic_stamp, Data.tools.getResource(R.string.insert_copy), this::onCopyClick, true);
        floatingMenuForMoving.addButton(R.drawable.ic_check, Data.tools.getResource(R.string.apply), this::onApplyClick, true);
        opacitySlider = floatingMenuForMoving.addSlider(R.drawable.ic_opacity, 255, 0, 255, this::onOpacityChanged, this::onOpacityChanged, true);
        removeBackgroundSlider = floatingMenuForMoving.addSlider(R.drawable.ic_remove_background, 50, 10, 200, null, this::onRemoveBackgroundIntensityChanged, false);
        movingCancelButton = new CircleButtonNextToMenuButton(draw, 2.15f, Data.store().DPI / 7f, R.drawable.ic_cancel, this::onCancelClick);
        movingDeleteButton = new CircleButtonNextToMenuButton(draw, 1.15f, Data.store().DPI / 7f, R.drawable.ic_delete, this::onRemoveClick);

        buttonsForPreciseSelecting = new CircleButtonNextToMenuButton[]{
                preciseSelectingButtonApply = new CircleButtonNextToMenuButton(draw, 2.75f, Data.store().DPI / 6f, R.drawable.ic_check, getApplyPrecisePointsRunnable()),
                preciseSelectingButtonCancel = new CircleButtonNextToMenuButton(draw, 1.15f, Data.store().DPI / 7f, R.drawable.ic_cancel, getClearPrecisePointsRunnable()),
                //new CircleButtonNextToMenuButton(draw, 2.1f, Data.store().DPI / 6f, R.drawable.ic_plus, getAddPrecisePointRunnable()),
                preciseSelectingButtonMinus = new CircleButtonNextToMenuButton(draw, 2.15f, Data.store().DPI / 7f, R.drawable.ic_minus, getRemovePrecisePointRunnable()),
                preciseSelectingButtonGoRegularSelection = new CircleButtonNextToMenuButton(draw, 1.15f, Data.store().DPI / 7f, R.drawable.icon_tap, getSelectRegularRunnable())
        };

    }

    @Override
    public String getName() {
        return "selection";
    }

    @Override
    public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentSelectAndMove);
    }

    @Override
    public int getImageResourceID() {
        return R.drawable.menu_selection;
    }

    @Override
    public void onSelect() {
        if(state == STATE_SELECTING)
            points.clear();
    }

    @Override
    public void onDeselect() {

    }

    @Override
    public boolean onTouch(MotionEvent event) {
        float movingTranslateMagnetThreshold = Tools.dp(8)/draw.scale.scale_size;
        float movingScaleMagnetThreshold = 0.03f;
        float movingRotateDegreeMagnetThreshold = 5;


        if(state == STATE_SELECTING){
            //trigger zoom if over 2 fingers
            if(event.getPointerCount() > 1){// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.selectandmove);
                }
                else
                    Data.tools.showBuyFullDialog();
                return true;
            }

            for (CircleButtonNextToMenuButton button:buttonsForSelecting){
                if(button.isVisible() && button.onTouch(event)) {
                    draw.redraw();
                    return true;
                }
            }

            //process hint events
            if(hintSelecting != null && hintSelecting.processTouch(event))
                return true;

            //move canvas if touched by finger but activated  sPen mode
            if(!Data.tools.isAllowedDevice(event)) {
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.selectandmove);
                }
                return true;
            }

            event = draw.scale.scale_transformMotionEvent(event);
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                points.clear();
                Point object = new Point(event);
                points.add(object);
                selectedArea.top = selectedArea.bottom = (int)object.y;
                selectedArea.right = selectedArea.left = (int)object.x;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE){
                if(points.isEmpty() || points.get(points.size()-1).distanceTo(event) > minimumPointInterval/draw.scale.scale_size) {
                    if(points.size() < 2000) {
                        Point object = new Point(event);
                        object.limitMin(0, 0);
                        object.limitMax(draw.getWidth(), draw.getHeight());
                        points.add(object);
                        if (object.y < selectedArea.top || selectedArea.top == -1)
                            selectedArea.top = (int) object.y;
                        if (object.y > selectedArea.bottom || selectedArea.bottom == -1)
                            selectedArea.bottom = (int) object.y;
                        if (object.x > selectedArea.right || selectedArea.right == -1)
                            selectedArea.right = (int) object.x;
                        if (object.x < selectedArea.left || selectedArea.left == -1)
                            selectedArea.left = (int) object.x;
                    }
                }
            }
            else if (event.getAction() == MotionEvent.ACTION_CANCEL){
                selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
                points.clear();
            }
            else if (event.getAction() == MotionEvent.ACTION_UP){
                if(points.size() > 3) {
                    state = STATE_CALCULATING_SELECTION;
                    selectingCalculationThread = new Thread(() -> {
                        try {
                            selectingCalculationAsync();
                        } catch (Throwable e) {
                            Logger.show("Error selection");
                            e.printStackTrace();
                            state = STATE_SELECTING;
                            selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
                            points.clear();
                        }
                    });
                    selectingCalculationThread.start();
                }
                else {
                    state = STATE_SELECTING;
                    selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
                    points.clear();
                }
            }
            draw.redraw();
        }
        else if(state == STATE_SELECTING_PRECISE) { //------------------ ============= PRECISE SELECTION
            //trigger zoom if over 2 fingers
            if(event.getPointerCount() > 1){// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
                if(Data.tools.isFullVersion()) {
                    draw.setInstrument(draw.scale);
                    draw.scale.onTouch(event);
                    draw.scale.setInstrumentCallback(draw.selectandmove);
                }
                else
                    Data.tools.showBuyFullDialog();
                return true;
            }

            if(hintPrecision != null && hintPrecision.processTouch(event))
                return true;

            if(!Data.tools.isAllowedDevice(event))
                return true;
            for (CircleButtonNextToMenuButton button : buttonsForPreciseSelecting) {
                if (button.isVisible() && button.onTouch(event)) {
                    draw.redraw();
                    return true;
                }
            }

            event = draw.scale.scale_transformMotionEvent(event);

            if(event.getAction() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                preciseSelectingTouchDownTouchId = event.getPointerId(event.getActionIndex());
                preciseSelectingTouchDownPosition.set(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()));
                preciseSelectingTouchDownCursorPosition.set(preciseSelectingCursorPosition);
                return true;
            }
            else if(event.getAction() == MotionEvent.ACTION_MOVE) {
                int indexOfMyId = -1;
                for(int i=0; i<event.getPointerCount(); i++)
                    if(event.getPointerId(i) == preciseSelectingTouchDownTouchId)
                        indexOfMyId = i;
                if(indexOfMyId != -1) {
                    float dx = event.getX(indexOfMyId) - preciseSelectingTouchDownPosition.x;
                    float dy = event.getY(indexOfMyId) - preciseSelectingTouchDownPosition.y;
                    float preciseSelectingCursorSpeed = 0.5f;
                    float nx = preciseSelectingTouchDownCursorPosition.x + dx * preciseSelectingCursorSpeed;
                    float ny = preciseSelectingTouchDownCursorPosition.y + dy * preciseSelectingCursorSpeed;
                    if (nx < 0)
                        nx = 0;
                    if (ny < 0)
                        ny = 0;
                    if (nx >= draw.getWidth())
                        nx = draw.getWidth() - 1;
                    if (ny >= draw.getHeight())
                        ny = draw.getHeight() - 1;
                    preciseSelectingCursorPosition.set(nx, ny);
                }
                draw.redraw();
                return true;
            }
            else if(event.getAction() == MotionEvent.ACTION_UP){
                int indexOfMyId = event.findPointerIndex(preciseSelectingTouchDownTouchId);
                if(indexOfMyId != -1) {
                    float dx = event.getX(indexOfMyId) - preciseSelectingTouchDownPosition.x;
                    float dy = event.getY(indexOfMyId) - preciseSelectingTouchDownPosition.y;
                    if(draw.scale.scale_size * Math.abs(dx) < Data.store().DPI / 20
                            && draw.scale.scale_size * Math.abs(dy) < Data.store().DPI / 20
                            && (event.getEventTime() - event.getDownTime()) < 200/*ms*/){
                        Tools.vibrate(draw.view);
                        getAddPrecisePointRunnable().run();
                    }
                }
                return true;
            }
        }
        else if(state == STATE_MOVING){ //------------------ ============= MOVING
            if(Data.tools.isAllowedDeviceForUi(event) && floatingMenuForMoving != null && floatingMenuForMoving.processTouch(event))
                return true;

            if(movingCancelButton != null && movingCancelButton.isVisible() && movingCancelButton.onTouch(event))
                return true;

            if(movingDeleteButton != null && movingDeleteButton.isVisible() && movingDeleteButton.onTouch(event))
                return true;


            if(hintMoving != null && hintMoving.processTouch(event))
                return true;

            event = draw.scale.scale_transformMotionEvent(event);

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
                touchDownId1 = event.getPointerId(index1);
                touchDownPosition1.set(event.getX(index1), event.getY(index1));
                if(index2 != -1){
                    touchDownId2 = event.getPointerId(index2);
                    touchDownPosition2.set(event.getX(index2), event.getY(index2));
                }
                touchDownTranslateX = movingTranslateX;
                touchDownTranslateY = movingTranslateY;
                touchDownScale = movingScale;
                touchDownRotateDegree = movingRotateDegree;
            }
            else if (event.getAction() == MotionEvent.ACTION_MOVE){
                if(event.getPointerCount() == 1) { //moving only
                    int index = event.findPointerIndex(touchDownId1);
                    if(index != -1 && touchDownPosition1.x != -1){
                        float dx = event.getX(index) - touchDownPosition1.x;
                        float dy = event.getY(index) - touchDownPosition1.y;
                        movingTranslateX = touchDownTranslateX + dx;
                        movingTranslateY = touchDownTranslateY + dy;
                        //magnet to original position
                        if(Math.abs(movingTranslateX) < movingTranslateMagnetThreshold && Math.abs(movingTranslateY) < movingTranslateMagnetThreshold){
                            movingTranslateX = 0;
                            movingTranslateY = 0;
                        }
                    }
                }
                if(event.getPointerCount() == 2) { //moving, rotating, zooming
                    int index1 = event.findPointerIndex(touchDownId1);
                    int index2 = event.findPointerIndex(touchDownId2);
                    if(index1 != -1 && touchDownPosition1.x != -1 && index2 != -1 && touchDownPosition2.x != -1){
                        Point currentTouchPosition1 = new Point(event.getX(index1), event.getY(index1));
                        Point currentTouchPosition2 = new Point(event.getX(index2), event.getY(index2));
                        Point touchDownCenter = touchDownPosition1.centerWith(touchDownPosition2);
                        Point currentTouchCenter = currentTouchPosition1.centerWith(currentTouchPosition2);
                        Point translate = currentTouchCenter.minus(touchDownCenter);
                        float touchDownDistance = touchDownPosition1.distanceTo(touchDownPosition2);
                        float currentTouchDistance = currentTouchPosition1.distanceTo(currentTouchPosition2);
                        float scale = currentTouchDistance / touchDownDistance;
                        Point currentImageCenterPosition =  translate.plus(selectedArea.centerX() + touchDownTranslateX, selectedArea.centerY() + touchDownTranslateY);
                        Point currentDistanceFromTouchToImage = currentTouchCenter.minus(currentImageCenterPosition);
                        Point translateCorrectionForZooming = currentDistanceFromTouchToImage.multiply((1-scale));

                        float touchDownRadian = touchDownPosition1.k(touchDownPosition2);
                        float currentTouchRadian = currentTouchPosition1.k(currentTouchPosition2);
                        float angleDifferenceRad = currentTouchRadian - touchDownRadian;
                        float angleDifferenceDeg = (float)Math.toDegrees(angleDifferenceRad);
                        Point translateCorrectionForRotating = currentDistanceFromTouchToImage.rotate(angleDifferenceRad);
                        translateCorrectionForRotating = currentDistanceFromTouchToImage.minus(translateCorrectionForRotating);


                        movingScale = touchDownScale * scale;
                        //magnet to its original scale
                        if(movingScale > 1-movingScaleMagnetThreshold && movingScale < 1+movingScaleMagnetThreshold)
                            movingScale = 1;

                        movingRotateDegree = touchDownRotateDegree + angleDifferenceDeg;
                        //magnet to its original rotation
                        float[] rotateMagnets = {360f+0f, 360f+90f, 360f+180f, 360f+270f, 360+360f}; //working within range 360 - 720 degree
                        while(movingRotateDegree < 360)
                            movingRotateDegree += 360;
                        while(movingRotateDegree > 720)
                            movingRotateDegree -= 360;
                        for (float magnet : rotateMagnets) {
                            if(movingRotateDegree > magnet - movingRotateDegreeMagnetThreshold/2f && movingRotateDegree < magnet + movingRotateDegreeMagnetThreshold/2f)
                                movingRotateDegree = magnet;
                        }

                        movingTranslateX = touchDownTranslateX + translate.x + translateCorrectionForZooming.x + translateCorrectionForRotating.x;
                        movingTranslateY = touchDownTranslateY + translate.y + translateCorrectionForZooming.y + translateCorrectionForRotating.y;
                        //magnet to original position
                        if(Math.abs(movingTranslateX) < movingTranslateMagnetThreshold && Math.abs(movingTranslateY) < movingTranslateMagnetThreshold){
                            movingTranslateX = 0;
                            movingTranslateY = 0;
                        }

                    }
                }
            }
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
                touchDownId1 = -1;
                touchDownId2 = -1;
            }

            //Нахуй защиты, они ломают многие процессы в программе
            draw.redraw();
        }

        return true;
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    /*Draw black-white punkteer line
    * */
    static private class PunkteerTracker{
        public int currentColor = Color.WHITE;
        public float currentPhase = 0;
        public float punkteerInterval = Data.store().DPI / 13;
        float lx = -1;
        float ly = -1;
    }
    private void drawPunkteerLine(Canvas canvas, Point p1, Point p2, PunkteerTracker punkteerTracker, Paint paint){
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        float d = p1.distanceTo(p2);
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;
        float k =  punkteerTracker.punkteerInterval / d; //какая доля от текущего участка является пунктиром
        float n =  d / punkteerTracker.punkteerInterval; //какое количество пунктиров влазит в текущий участок
        float p = punkteerTracker.currentPhase; //остаток с прошлого участка
        float x0 = p1.x;
        float y0 = p1.y;
        paint.setColor(punkteerTracker.currentColor);
        float i=p;

        if(punkteerTracker.lx != -1)
            canvas.drawLine(punkteerTracker.lx, punkteerTracker.ly, p1.x, p1.y, paint);
        canvas.drawCircle(p1.x, p1.y, paint.getStrokeWidth()/2,paint);
        punkteerTracker.lx = p1.x;
        punkteerTracker.ly = p1.y;
        if(punkteerTracker.lx == -1) {
            punkteerTracker.lx = x0;
            punkteerTracker.ly = y0;
        }
        while(i<n){
            float xp = x0+(dx*k)*(i);
            float yp = y0+(dy*k)*(i);
            canvas.drawLine(punkteerTracker.lx, punkteerTracker.ly, xp, yp, paint);
            canvas.drawCircle(xp, yp, paint.getStrokeWidth()/2,paint);
            punkteerTracker.lx = xp;
            punkteerTracker.ly = yp;
            punkteerTracker.currentColor = (punkteerTracker.currentColor == Color.BLACK ? Color.WHITE : Color.BLACK);
            paint.setColor(punkteerTracker.currentColor);
            i++;
        }
        punkteerTracker.currentPhase = i - n;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(state == STATE_SELECTING){
            try {
                if (points.size() > 1) {
                    Point last = null;
                    Point first = null;
                    PunkteerTracker punkteerTracker = new PunkteerTracker();
                    punkteerTracker.currentPhase = (points.size() / 20f) % 1;
                    paint.setStrokeWidth(Data.store().DPI / 70);
                    if ((points.size() / 20f) % 2 >= 1)
                        punkteerTracker.currentColor = Color.BLACK;
                    for (int i = 0; i < points.size(); i++) {
                        Point cur = points.get(i);
                        cur = draw.scale.ImageToScreen(cur);
                        if (first == null)
                            first = cur;
                        if (last != null)
                            drawPunkteerLine(canvas, last, cur, punkteerTracker, paint);
                        last = cur;
                    }
                    if (first != null && last != null) {
                        drawPunkteerLine(canvas, last, first, punkteerTracker, paint);
                        canvas.drawLine(punkteerTracker.lx, punkteerTracker.ly, first.x, first.y, paint);
                        canvas.drawCircle(first.x, first.y, Data.store().DPI / 60, paint);
                    }
                }
                //buttons
                for (CircleButtonNextToMenuButton button : buttonsForSelecting)
                    if (button.isVisible())
                        button.draw(canvas);

                if(hintSelecting == null)
                    hintSelecting = new HintMenu(draw, Data.tools.getResource(R.string.hint_selection_selecting), R.drawable.ic_help, "SELECTION_SELECTING", HintMenu.SHOW_TIMES);
                hintSelecting.draw(canvas);
            }
            catch (Throwable e){
                Logger.log("Error in Selection.OnDraw.STATE_SELECTING : " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if(state == STATE_SELECTING_PRECISE){
            try {
                //selectionLine
                paint.setColor(Color.argb(100, 0, 0, 0)); //BLACK
                paint.setStyle(Paint.Style.FILL);
                for (int i = 1; i < points.size(); i++) {
                    Point prev = points.get(i - 1);
                    prev = draw.scale.ImageToScreen(prev);
                    Point cur = points.get(i);
                    cur = draw.scale.ImageToScreen(cur);

                    paint.setStrokeWidth(Data.store().DPI * 0.02f);
                    canvas.drawLine(prev.x, prev.y, cur.x, cur.y, paint);
                    canvas.drawCircle(prev.x, prev.y, Data.store().DPI * 0.03f, paint);
                }
                if (points.size() > 0) {
                    Point last = points.get(points.size() - 1);
                    last = draw.scale.ImageToScreen(last);
                    canvas.drawCircle(last.x, last.y, Data.store().DPI * 0.03f, paint);
                }

                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                for (int i = 1; i < points.size(); i++) {
                    Point prev = points.get(i - 1);
                    prev = draw.scale.ImageToScreen(prev);
                    Point cur = points.get(i);
                    cur = draw.scale.ImageToScreen(cur);

                    paint.setStrokeWidth(Data.store().DPI * 0.01f);
                    canvas.drawLine(prev.x, prev.y, cur.x, cur.y, paint);
                    canvas.drawCircle(prev.x, prev.y, Data.store().DPI * 0.023f, paint);
                }
                if (points.size() > 0) {
                    Point last = points.get(points.size() - 1);
                    last = draw.scale.ImageToScreen(last);
                    canvas.drawCircle(last.x, last.y, Data.store().DPI * 0.023f, paint);
                    if (points.size() >= 3) {
                        Point first = points.get(0);
                        first = draw.scale.ImageToScreen(first);
                        paint.setStrokeWidth(Data.store().DPI * 0.02f);
                        paint.setColor(Color.argb(100, 255, 255, 255)); //WHITE
                        canvas.drawLine(last.x, last.y, first.x, first.y, paint);
                    }
                }

                //cursor
                if (preciseSelectingCursorBitmap == null) {
                    preciseSelectingCursorBitmap = Data.tools.getResizedBitmap(BitmapFactory.decodeResource(draw.context.getResources(), R.drawable.cursor), ((int) Data.store().DPI / 7), ((int) Data.store().DPI / 7));
                }
                if (preciseSelectingCursorPosition.x == -1) { //it's backup initialization. because point initialized when PreciseSelection mode is set
                    preciseSelectingCursorPosition.set(draw.getWidth() / 2f, draw.getHeight() / 2f);
                }
                canvas.drawBitmap(preciseSelectingCursorBitmap, draw.scale.ImageToScreenX(preciseSelectingCursorPosition.x), draw.scale.ImageToScreenY(preciseSelectingCursorPosition.y), preciseSelectingCursorPaint);

                //buttons
                preciseSelectingButtonCancel.setVisible(points.size() >= 1);
                preciseSelectingButtonApply.setVisible(points.size() >= 3);
                preciseSelectingButtonMinus.setVisible(points.size() > 1);
                preciseSelectingButtonGoRegularSelection.setVisible(points.size() == 0);


                for (CircleButtonNextToMenuButton button : buttonsForPreciseSelecting)
                    if (button.isVisible())
                        button.draw(canvas);

                if(hintPrecision == null)
                    hintPrecision = new HintMenu(draw, Data.tools.getResource(R.string.hint_selection_precision), R.drawable.ic_help, "SELECTION_PRECISION", HintMenu.SHOW_TIMES);
                hintPrecision.draw(canvas);
            }
            catch (Throwable e){
                Logger.log("Error in Selection.OnDraw.STATE_SELECTING_PRECISE : " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if(state == STATE_CALCULATING_SELECTION){
            try {
                //background
                paint.setColor(Color.argb(150, 0, 0, 0));
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(0, 0, draw.getWidth(), draw.getHeight(), paint);
                //frame
                paint.setColor(Color.argb(10, 255, 255, 255));
                paint.setStrokeWidth(Data.store().DPI / 100);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(draw.scale.ImageToScreen(selectedArea), paint);

                //line of selection
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(Data.store().DPI / 30);
                if (points.size() > 3) {
                    synchronized (points) {
                        Point last = points.get(points.size() - 1);
                        if (last != null) {
                            last = draw.scale.ImageToScreen(last);
                            for (int i = 0; i < points.size(); i++) {
                                Point cur = points.get(i);
                                if (cur != null) {
                                    cur = draw.scale.ImageToScreen(cur);
                                    canvas.drawLine(last.x, last.y, cur.x, cur.y, paint);
                                    canvas.drawCircle(cur.x, cur.y, paint.getStrokeWidth() / 2, paint);
                                    last = cur;
                                }
                            }
                        }
                    }
                }
                //text and frame
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                float textSize = Data.store().DPI / 9;
                paint.setTextSize(textSize);
                String text = Data.tools.getResource(R.string.creatingSecetion) + "...";
                float textWidth = paint.measureText(text);
                float textY = draw.getHeight() * 0.3f;
                float textX = (draw.getWidth() - textWidth) / 2;
                float margin = Data.store().DPI / 7;
                float round = Data.store().DPI / 14;
                RectF frameRect = new RectF(textX - margin, textY - textSize * 0.7f - margin, textX + textWidth + margin, textY + margin);
                paint.setColor(Color.argb(200, 0, 0, 0));
                canvas.drawRoundRect(frameRect, round, round, paint);
                paint.setColor(Color.WHITE);
                canvas.drawText(text, textX, textY, paint);
            }
            catch (Throwable e){
                Logger.log("Error in Selection.OnDraw.STATE_CALCULATING_SELECTION : " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if(state == STATE_MOVING){
            try {
                //debug
                //canvas.drawText(String.valueOf(movingRotateDegree), 50, 100, paint);

                //frame
                RectF frameRect = new RectF(
                        /*left*/selectedArea.centerX() - movingScale * selectedArea.width() / 2f + movingTranslateX,
                        /*top*/ selectedArea.centerY() - movingScale * selectedArea.height() / 2f + +movingTranslateY,
                        /*right*/ selectedArea.centerX() + movingScale * selectedArea.width() / 2f + movingTranslateX,
                        /*bottom*/selectedArea.centerY() + movingScale * selectedArea.height() / 2f + +movingTranslateY
                );
                frameRect = draw.scale.ImageToScreen(frameRect);
                paint.setStyle(Paint.Style.STROKE);
                paint.setAntiAlias(true);
                paint.setColor(Color.argb(50, 0, 0, 0));
                paint.setStrokeWidth(Data.store().DPI / 60f);
                canvas.save();
                canvas.rotate(movingRotateDegree, frameRect.centerX(), frameRect.centerY());
                canvas.drawRect(frameRect, paint);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(Data.store().DPI / 200f);
                canvas.drawRect(frameRect, paint);
                canvas.restore();

                //image
                paint.setColor(Color.WHITE);
                paint.setAlpha(movingAlpha);
                paint.setAntiAlias(false);
                paint.setFilterBitmap(false);
                Matrix matrix = new Matrix();
                matrix.preTranslate(frameRect.left, frameRect.top);
                matrix.preScale(movingScale * draw.scale.scale_size, movingScale * draw.scale.scale_size);
                canvas.save();
                canvas.rotate(movingRotateDegree, frameRect.centerX(), frameRect.centerY());
                synchronized (selectedArea) {
                    if (selectedImage != null && !selectedImage.isRecycled())
                        canvas.drawBitmap(selectedImage, matrix, paint);
                }
                canvas.restore();

                //status text
                canvas.save();
                canvas.rotate(movingRotateDegree, frameRect.centerX(), frameRect.centerY());
                float topOffset = Tools.dp(4);
                String text = "";
                text += (int)(movingRotateDegree%360)+"°; ";
                text += (int)(movingScale*100)+"%; ";
                text += "X: " + (int)movingTranslateX + "px; ";
                text += "Y: " + (int)movingTranslateY + "px; ";
                paint.setColor(Color.WHITE);
                paint.setTextSize(Tools.sp(9));
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(text, frameRect.left, frameRect.top-topOffset, paint);
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(0.5f);
                canvas.drawText(text, frameRect.left, frameRect.top-topOffset, paint);
                canvas.restore();

                //buttons
                if(movingCancelButton != null && movingCancelButton.isVisible())
                    movingCancelButton.draw(canvas);

                if(movingDeleteButton != null && movingDeleteButton.isVisible())
                    movingDeleteButton.draw(canvas);

                if(floatingMenuForMoving != null) {
                    double diagonal = Math.sqrt(frameRect.width()*frameRect.width()+frameRect.height()*frameRect.height());

                    floatingMenuForMoving.setBounds(draw.getWidth(), draw.getHeight());
                    floatingMenuForMoving.clearNoGoZones();
                    floatingMenuForMoving.addNoGoZone(frameRect);
                    floatingMenuForMoving.addNoGoZones(draw.getNoGoZones());
                    if(movingDeleteButton != null)
                        floatingMenuForMoving.addNoGoZone(movingDeleteButton.buttonRect);
                    if(movingCancelButton != null)
                        floatingMenuForMoving.addNoGoZone(movingCancelButton.buttonRect);
                    floatingMenuForMoving.setTargetPosition(frameRect.centerX() - floatingMenuForMoving.width()/2f, (float)(frameRect.centerY() + diagonal/2f));
                    floatingMenuForMoving.draw(canvas);
                }

                if(drawUi && hintMoving == null)
                    hintMoving = new HintMenu(draw, Data.tools.getResource(R.string.hint_selection_moving), R.drawable.ic_help, "SELECTION_MOVING", HintMenu.SHOW_TIMES);
                if(drawUi)
                    hintMoving.draw(canvas);
            }
            catch (Throwable e){
                Logger.log("Error in Selection.OnDraw.STATE_MOVING : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    /*start calculating area from points list (used on PreciseSelection screen)*/
    private Runnable getApplyPrecisePointsRunnable(){
        return () -> {
            state = STATE_CALCULATING_SELECTION;
            selectingCalculationThread = new Thread(() -> {
                try {
                    selectingCalculationAsync();
                } catch (Throwable e) {
                    Logger.show("Error selection");
                    e.printStackTrace();
                    state = STATE_SELECTING_PRECISE;
                    selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
                    points.clear();
                }
            });
            selectingCalculationThread.start();
        };
    }
    /*remove all points from list (used on PreciseSelection screen)*/
    private Runnable getClearPrecisePointsRunnable(){
        return () -> {
            points.clear();
            selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
            draw.redraw();
        };
    }
    /*Add point to list (used on PreciseSelection screen)*/
    private Runnable getAddPrecisePointRunnable(){
        return () -> {
            Point object = preciseSelectingCursorPosition.copy();
            object.limitMin(0,0);
            object.limitMax(draw.getWidth(), draw.getHeight());
            points.add(object);
            if(object.y < selectedArea.top || selectedArea.top == -1)
                selectedArea.top = (int)object.y;
            if(object.y > selectedArea.bottom || selectedArea.bottom == -1)
                selectedArea.bottom = (int)object.y;
            if(object.x > selectedArea.right || selectedArea.right == -1)
                selectedArea.right = (int)object.x;
            if(object.x < selectedArea.left || selectedArea.left == -1)
                selectedArea.left = (int)object.x;
            draw.redraw();
        };
    }
    /*remove last point from list (used on PreciseSelection screen)*/
    private Runnable getRemovePrecisePointRunnable(){
        return () -> {
            if(points.size() > 1){
                points.remove(points.size()-1);
            }
            draw.redraw();
        };
    }
    /*Set mode to Selection (used on PreciseSelection screen)*/
    private Runnable getSelectRegularRunnable(){
        return () -> {
            points.clear();
            selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
            state = STATE_SELECTING;
            draw.redraw();
        };
    }
    /*Set mode to PreciseSelection (used on Selection screen)*/
    private Runnable getSelectPreciseRunnable(){
        return () -> {
            points.clear();
            selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
            preciseSelectingCursorPosition.set(draw.scale.ScreenToImageX(draw.getWidth()/2f), draw.scale.ScreenToImageY(draw.getHeight()/2f));
            state = STATE_SELECTING_PRECISE;
            draw.redraw();
        };
    }
    private Runnable getSelectAllRunnable(){
        return () -> {
            selectedArea.top = 0;
            selectedArea.left = 0;
            selectedArea.right = draw.bitmap.getWidth();
            selectedArea.bottom = draw.bitmap.getHeight();

            selectedImage = draw.bitmap.copy(Bitmap.Config.ARGB_8888, true);
            selectedImageBackup = selectedImage;
            draw.bitmap.eraseColor(Color.TRANSPARENT);
            draw.lastChangeToBitmap = System.currentTimeMillis();

            draw.undoProvider.apply(selectedArea.top, selectedArea.bottom, selectedArea.left, selectedArea.right);
            draw.undoProvider.prepare();

            onResetClick();
            state = STATE_MOVING;
            draw.redraw();
        };
    }

    /*Translate bitmap from "selection" to main bitmap with parameters from "moving" variables.*/
    private void apply(){
        try {
            //frame
            RectF frameRect = new RectF(
                    /*left*/selectedArea.centerX() - movingScale * selectedArea.width() / 2f + movingTranslateX,
                    /*top*/ selectedArea.centerY() - movingScale * selectedArea.height() / 2f + +movingTranslateY,
                    /*right*/ selectedArea.centerX() + movingScale * selectedArea.width() / 2f + movingTranslateX,
                    /*bottom*/selectedArea.centerY() + movingScale * selectedArea.height() / 2f + +movingTranslateY
            );

            //image
            paint.setAlpha(movingAlpha);
            paint.setFilterBitmap(false);
            Bitmap selectedCopy = Bitmap.createBitmap(selectedImage.getWidth(), selectedImage.getHeight(), selectedImage.getConfig());
            Canvas canvas = new Canvas(selectedCopy);
            canvas.drawBitmap(selectedImage, 0, 0, paint);
            paint.setAlpha(255);
            Matrix matrix = new Matrix();
            matrix.preTranslate(Math.round(frameRect.left), Math.round(frameRect.top));
            matrix.preScale(movingScale, movingScale);
            draw.canvas.save();
            draw.canvas.rotate(movingRotateDegree, frameRect.centerX(), frameRect.centerY());
            //noinspection SynchronizeOnNonFinalField
            synchronized (draw.bitmap) {
                draw.canvas.drawBitmap(selectedCopy, matrix, paint);
            }
            draw.canvas.restore();
            draw.lastChangeToBitmap = System.currentTimeMillis();

            float dx = frameRect.width() / 2;
            float dy = frameRect.height() / 2;
            float radius = (float) Math.sqrt(dx * dx + dy * dy);

            //update area where will be undo
            if (draw.undoAreaCalculator != null) {
                draw.undoAreaCalculator.reset();
                draw.undoAreaCalculator.add(frameRect.centerX() - radius, frameRect.centerY() - radius, Tools.dp(10));
                draw.undoAreaCalculator.add(frameRect.centerX() + radius, frameRect.centerY() + radius, Tools.dp(10));

                draw.undoProvider.apply(
                        draw.undoAreaCalculator.top,
                        draw.undoAreaCalculator.bottom,
                        draw.undoAreaCalculator.left,
                        draw.undoAreaCalculator.right);
            } else {
                draw.undoProvider.apply(frameRect.centerY() - radius, frameRect.centerY() + radius, frameRect.centerX() - radius, frameRect.centerX() + radius);
            }
            draw.undoProvider.prepare();
            draw.redraw();
        }
        catch (Throwable e){
            Logger.log(e);
        }
    }

    private void onApplyClick(){
        try{
            apply();
            synchronized (selectedArea) {
                selectedImage.recycle();
                selectedImage = null;
                selectedImageBackup = null;
            }
            state = STATE_SELECTING;
        }
        catch (Throwable e){
            Logger.log("onApplyClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onCopyClick(){
        try{
            apply();
            movingTranslateX += (Data.store().DPI / 6) / draw.scale.scale_size;
            movingTranslateY += (Data.store().DPI / 6) / draw.scale.scale_size;
        }
        catch (Throwable e){
            Logger.log("onCopyClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    void onCopyToClipboardClick(){
        try{
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                Bitmap drawableicon = selectedImage.copy(selectedImage.getConfig(), true);
                ClipboardManager mClipboard = (ClipboardManager) draw.context.getSystemService(Context.CLIPBOARD_SERVICE);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                drawableicon.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                String path = MediaStore.Images.Media.insertImage(draw.context.getContentResolver(), drawableicon, "selected.png", null);
                Uri imageUri = Uri.parse(path);

                ClipData theClip = ClipData.newUri(draw.context.getContentResolver(), "Image", imageUri);
                mClipboard.setPrimaryClip(theClip);
            }
        }
        catch (Throwable e){
            Logger.log("onCopyToClipboardClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onCancelClick(){
        try{
            onResetClick();
            apply();
            synchronized (selectedArea) {
                selectedImage.recycle();
                selectedImage = null;
                selectedImageBackup = null;
            }
            state = STATE_SELECTING;
        }
        catch (Throwable e){
            Logger.log("onCancelClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onResetClick(){
        try{
            movingTranslateX = 0;
            movingTranslateY = 0;
            movingAlpha = 255;
            movingScale = 1;
            movingRotateDegree = 0;
            if(mirrorButton != null)mirrorButton.visible = true;
            if(mirrorButton != null)mirrorButton.highlighted = false;
            if(mirrorButton != null)mirrorButton.rotateIcon = false;
            if(removeBackgroundButton != null)removeBackgroundButton.visible = true;
            if(removeBackgroundButton != null)removeBackgroundButton.highlighted = false;
            if(removeBackgroundSlider != null)removeBackgroundSlider.visible = false;
            if(opacitySlider != null)opacitySlider.value = 255;
            selectedImage = selectedImageBackup;
        }
        catch (Throwable e){
            Logger.log("onResetClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onRemoveClick(){
        try{
            movingTranslateX = 0;
            movingTranslateY = 0;
            movingAlpha = 255;
            movingScale = 1;
            movingRotateDegree = 0;
            synchronized (selectedArea) {
                selectedImage.recycle();
                selectedImage = null;
                selectedImageBackup = null;
            }
            state = STATE_SELECTING;
        }
        catch (Throwable e){
            Logger.log("onRemoveClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void onMirrorClick(){
        try{
            if(mirrorButton != null){
                if(!mirrorButton.highlighted)
                    mirrorButton.highlighted = true;
                else{
                    if(!mirrorButton.rotateIcon){
                        mirrorButton.rotateIcon = true;
                    }
                    else {
                        mirrorButton.highlighted = false;
                        mirrorButton.rotateIcon = false;
                    }
                }
                if(removeBackgroundButton != null)removeBackgroundButton.visible = !mirrorButton.highlighted;
                if(removeBackgroundButton != null)removeBackgroundButton.highlighted = false;
                if(removeBackgroundSlider != null)removeBackgroundSlider.visible = false;
                if(mirrorButton.highlighted){
                    if(mirrorButton.rotateIcon)
                        selectedImage = Tools.flipY(selectedImageBackup);
                    else
                        selectedImage = Tools.flipX(selectedImageBackup);
                }
                else{
                    selectedImage = selectedImageBackup;
                }
                draw.redraw();
            }
        }
        catch (Throwable e){
            Logger.log("onMirrorClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onSaveClick(){
        try{
            if(selectedImage == null)
                return;
            synchronized (selectedArea) {
                try {
                    draw.saver.saveImageToGallery(selectedImage, Saver.addDateTimeExtension("sDraw"));
                }
                catch (Throwable e){
                    Logger.log("MainMenu.getSaveLongListener.OnCropListener.OnCrop", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                }
            }

        }
        catch (Throwable e){
            Logger.log("onRemoveClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    void onRemoveBackgroundClick(){
        if(removeBackgroundSlider == null)
            return;
        if(removeBackgroundButton == null)
            return;
        try{
            removeBackgroundButton.highlighted = !removeBackgroundButton.highlighted;
            removeBackgroundSlider.visible = removeBackgroundButton.highlighted;
            if(mirrorButton != null)mirrorButton.visible = !removeBackgroundButton.highlighted;
            if(mirrorButton != null)mirrorButton.highlighted = false;
            if(removeBackgroundButton.highlighted){
                BackgroundRemover backgroundRemover = new BackgroundRemover(draw.context, new BackgroundRemover.OnSuccessBackgroundRemoved() {
                    @Override
                    public void success(Bitmap bitmap, int backgroundColor) {
                        removeBackgroundDominantColor = backgroundColor;
                        selectedImage = bitmap;
                        draw.redraw();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        removeBackgroundButton.highlighted = false;
                        if(mirrorButton != null)mirrorButton.visible = true;
                        removeBackgroundSlider.visible = false;
                    }
                }, selectedImageBackup);
                backgroundRemover.setBackgroundDetectionStrength(removeBackgroundSlider.getValue());
                backgroundRemover.startOnTransparency();
            }
            else {
                selectedImage = selectedImageBackup;
            }
            draw.redraw();
        }
        catch (Throwable e){
            Logger.log("onRemoveBackgroundClick error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void onRemoveBackgroundIntensityChanged(){
        if(removeBackgroundSlider == null)
            return;
        BackgroundRemover backgroundRemover = new BackgroundRemover(draw.context, new BackgroundRemover.OnSuccessBackgroundRemoved() {
            @Override
            public void success(Bitmap bitmap, int backgroundColor) {
                selectedImage = bitmap;
                draw.redraw();
            }
        }, new Runnable() {
            @Override
            public void run() {
                removeBackgroundButton.highlighted = false;
                removeBackgroundSlider.visible = false;
            }
        }, selectedImageBackup);
        backgroundRemover.setBackgroundDetectionStrength(removeBackgroundSlider.getValue());
        backgroundRemover.startOnKnownColor(removeBackgroundDominantColor);
    }

    private void onOpacityChanged(){
        if(opacitySlider != null) {
            movingAlpha = (int)opacitySlider.getValue();
            draw.redraw();
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

    @Override
    public boolean isVisibleToUser() {
        return true;
    }

    @Override
    public View.OnClickListener getOnClickListener() {
        return view -> draw.setInstrument(getName());
    }

    /*This function is working in background and working
     on converting set of points into bitmap was cut from canvas*/
    void selectingCalculationAsync(){
        draw.redraw();
        //optimize: remove points with small angle
        int changed = -1;
        while(changed != 0) {
            changed = 0;
            synchronized (points) {
                for (int i = 1; i < points.size() - 1; i += 1) {
                    Point p0 = points.get(i - 1);
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    //https://ru.onlinemschool.com/math/library/vector/angl/
                    float v1x = p1.x - p0.x; //вектор 1
                    float v1y = p1.y - p0.y;
                    float v2x = p2.x - p1.x; //вектор 2
                    float v2y = p2.y - p1.y;
                    double sm = v1x * v2x + v1y * v2y; //скалярное произведение векторов
                    double v1m = Math.sqrt(v1x * v1x + v1y * v1y); //модуль (длина) вектора 1
                    double v2m = Math.sqrt(v2x * v2x + v2y * v2y); //модуль (длина) вектора 2
                    double cos = sm / (v1m * v2m); //косинуус угла между векторами
                    double rad = Math.acos(cos);
                    double degree = Math.toDegrees(rad);
                    //Logger.log("Angle " + i + " = " + degree + " degree");
                    if (degree < 5) {
                        //noinspection SuspiciousListRemoveInLoop
                        points.remove(i);
                        changed++;
                        //screen redraw for debug purposes
                        //draw.redraw(); Thread.sleep(10);]
                    }
                }
            }
        }
        draw.redraw();
        //Thread.sleep(2000);

        //create mask
        boolean[][] mask = new boolean[draw.bitmap.getWidth()][draw.bitmap.getHeight()];
        inverseFacet(mask, points.get(0), points.get(points.size() - 1));
        for (int i = 1; i < points.size(); i++)
            inverseFacet(mask, points.get(i-1), points.get(i));

        //cut image
        boolean empty = true;
        synchronized (selectedArea) {
            selectedImage = Bitmap.createBitmap((int) selectedArea.width(), (int) selectedArea.height(), draw.bitmap.getConfig());
            //noinspection SynchronizeOnNonFinalField
            synchronized (draw.bitmap) {
                for (int y = 0; y < selectedImage.getHeight(); y++) {
                    for (int x = 0; x < selectedImage.getWidth(); x++) {
                        int gx = (int) selectedArea.left + x;
                        int gy = (int) selectedArea.top + y;
                        if (mask[gx][gy]) {
                            int pixel = draw.bitmap.getPixel(gx, gy);
                            if (pixel != Color.TRANSPARENT) {
                                empty = false;
                                selectedImage.setPixel(x, y, pixel);
                                draw.bitmap.setPixel(gx, gy, Color.TRANSPARENT);
                            }
                        }
                    }
                }
            }
        }
        draw.lastChangeToBitmap = System.currentTimeMillis();
        selectedImageBackup = selectedImage;
        if(draw.undoAreaCalculator != null){
            draw.undoAreaCalculator.reset();
            draw.undoAreaCalculator.add(selectedArea.left, selectedArea.top, 0);
            draw.undoAreaCalculator.add(selectedArea.right, selectedArea.bottom, 0);
        }
        draw.undoProvider.apply(selectedArea.top, selectedArea.bottom, selectedArea.left, selectedArea.right);
        draw.undoProvider.prepare();
        draw.redraw();
        if(empty){
            selectedImage = null;
            selectedImageBackup = null;
            Logger.show(draw.context.getString(R.string.selectedAreaEmpty));
            state = STATE_SELECTING;
            selectedArea.right = selectedArea.left = selectedArea.top = selectedArea.bottom = -1;
        }
        else {
            state = STATE_MOVING;
            onResetClick();

        }

        //finalize
        selectingCalculationThread = null;
        points.clear();
        draw.redraw();
    }
    private void inverseFacet(boolean[][] mask, Point p1, Point p2){
        if(mask != null && p1 != null && p2 != null
                && p1.x>=0 && p1.y >= 0 && p1.x < mask.length && p1.y < mask[(int)p1.x].length
                && p2.x>=0 && p2.y >= 0 && p2.x < mask.length && p2.y < mask[(int)p2.x].length){
            for(int y = (int)Math.min(p1.y, p2.y); y <= Math.max(p1.y, p2.y)-1; y++){
                float c = ((float)y-p1.y)/(p2.y-p1.y);
                c=Math.max(Math.min(1, c), 0);
                int bx = (int)(p1.x + (p2.x - p1.x)*c);
                //Logger.log("p1="+p1 + " p2="+p2 + " c="+c+" bx="+bx);
                for(int x=bx; x< selectedArea.right; x++)
                    mask[x][y] = !mask[x][y];
            }
        }
    }
}
