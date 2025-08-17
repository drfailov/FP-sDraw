package com.fsoft.FP_sDraw.instruments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.MainActivity;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.TextInput;
import com.fsoft.FP_sDraw.menu.CircleButtonNextToMenuButton;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.FloatingMenu;
import com.fsoft.FP_sDraw.menu.HintMenu;
import com.fsoft.FP_sDraw.menu.MenuPopup;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Dr. Failov on 17.01.2015.
 * Refactored by Dr. Failov on 26.10.2023.
 */
public class Text implements Instrument{
    private final DrawCore draw;
    public String text = null;
    private final FloatingMenu floatingMenu;
    private final FloatingMenu.FloatingMenuButton buttonAlignment;
    private final FloatingMenu.FloatingMenuSlider opacitySlider;
    private final FloatingMenu.FloatingMenuSlider contourSlider;
    private final FloatingMenu.FloatingMenuSlider intervalSlider;
    private final CircleButtonNextToMenuButton movingButtonDelete;
    private final  CircleButtonNextToMenuButton movingButtonEdit;
    private MenuPopup fontsSelectionMenuPopup;
    private HintMenu hintForAdding = null;
    private HintMenu hintForMoving = null;
    private Paint textPaint = null;
    private Typeface textFont = null;
    private final Point textPosition = new Point(); //point of label center
    private float textSize = -1;  //in px
    private float textInterval = 1.1f;  //0.5 ... 1.5
    private int textOpacity = 255;  //0...255
    private float textContour = 0;  //0...

    @SuppressLint("RtlHardcoded")
    private int textAlignment = Gravity.LEFT;
    private int textFrameColor = Color.TRANSPARENT;//TRANSPARENT is no frame
    private float textRotationRad = 0; //in Radians
    private final Point initialTouch = new Point(0,0);
    private final Point initialPosition = new Point(0, 0);
    private float initialTouchDistance = 0;
    private float initialTouchRotation = 0; //rad
    private float initialTextSize = 0;
    private float initialTextRotation = 0; //rad



    public Text(DrawCore d) {
        draw = d;
        floatingMenu = new FloatingMenu(draw.view);
        buttonAlignment = floatingMenu.addButton(R.drawable.ic_text_align_left, R.string.text_input_alignment_hint, this::onAlignmentPressed, true);
        floatingMenu.addButton(R.drawable.ic_text_frame, R.string.text_input_frame_hint, this::onFramePressed, true);
        floatingMenu.addButton(R.drawable.ic_font, R.string.text_input_font_hint, this::onFontPressed, true);
        floatingMenu.addButton(R.drawable.ic_stamp, R.string.insert_copy, this::onCopyPressed, true);
        floatingMenu.addButton(R.drawable.ic_check, R.string.apply, this::onApplyPressed, true);
        opacitySlider = floatingMenu.addSlider(R.drawable.ic_opacity, 255, 0, 255, this::onOpacityChanged, this::onOpacityChanged, true);
        contourSlider = floatingMenu.addSlider(R.drawable.menu_line, 0, 0, Tools.dp(5), this::onContourChanged, this::onContourChanged, true);
        intervalSlider = floatingMenu.addSlider(R.drawable.ic_height, (int)(textInterval*100), 70, 170, this::onIntervalChanged, this::onIntervalChanged, true);
        movingButtonDelete = new CircleButtonNextToMenuButton(draw, 1.15f, Data.store().DPI / 7f, R.drawable.ic_delete, this::onCancelPressed);
        movingButtonEdit = new CircleButtonNextToMenuButton(draw, 2.15f, Data.store().DPI / 7f, R.drawable.ic_edit, this::onEditPressed);
    }
    @Override
    public String getName() {
        return "text";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentText);
    }
    @Override
    public int getImageResourceID() {
        return R.drawable.menu_text;
    }
    @Override
    public void onSelect() {

    }
    @Override
    public void onDeselect() {

    }
    @Override
    public boolean onTouch(MotionEvent event) {
        try{
            if(text == null || text.length() == 0) { //no text set, open window to create text
                //trigger zoom if over 2 fingers
                if (event.getPointerCount() > 1) {// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
                    if (Data.tools.isFullVersion()) {
                        draw.setInstrument(draw.scale);
                        draw.scale.onTouch(event);
                        draw.scale.setInstrumentCallback(draw.text);
                    } else
                        Data.tools.showBuyFullDialog();
                    return true;
                }

                //process hint events
                if(hintForAdding != null && hintForAdding.processTouch(event))
                    return true;

                //move canvas if touched by finger but activated  sPen mode
                if(!Data.tools.isAllowedDevice(event)) {
                    if(Data.tools.isFullVersion()) {
                        //draw.setInstrument(draw.scale);
                        draw.scale.onTouch(event);
                        //draw.scale.setInstrumentCallback(draw.text);
                    }
                    return true;
                }

                event = draw.scale.scale_transformMotionEvent(event);

                if (event.getX() < draw.bitmap.getWidth() && event.getY() < draw.bitmap.getHeight() && event.getX() > 0 && event.getY() > 0) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        openEditActivity();
                    }
                }
            }
            else{  //if text exist, moving mode
                if(hintForMoving != null && hintForMoving.processTouch(event))
                    return true;
                if(Data.tools.isAllowedDeviceForUi(event) && floatingMenu.processTouch(event))
                    return true;
                if(movingButtonDelete != null && movingButtonDelete.isVisible() && movingButtonDelete.onTouch(event))
                    return true;
                if(movingButtonEdit != null && movingButtonEdit.isVisible() && movingButtonEdit.onTouch(event))
                    return true;

                event = draw.scale.scale_transformMotionEvent(event);
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if(action == MotionEvent.ACTION_DOWN){
                    initialTouch.x = event.getX(0);
                    initialTouch.y = event.getY(0);
                    initialPosition.set(textPosition);
                }
                else if(action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 2){//initiate zoom
                    initialTouchDistance = d(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                    initialTextSize = textSize;
                    initialTouchRotation = k(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                    initialTextRotation = textRotationRad;
                    initialTouch.set(c(event.getX(0), event.getY(0), event.getX(1), event.getY(1)));
                    initialPosition.set(textPosition);
                }
                else if (action == MotionEvent.ACTION_MOVE){
                    if(event.getPointerCount() == 1) { //move
                        float dx = event.getX(0) - initialTouch.x;
                        float dy = event.getY(0) - initialTouch.y;
                        textPosition.x = initialPosition.x + dx;
                        textPosition.y = initialPosition.y + dy;
                        //check for limits
                        float width = draw.getWidth();
                        float height = draw.getHeight();
                        if(textPosition.x < 0)
                            textPosition.x = 0;
                        if(textPosition.x >= width)
                            textPosition.x = width-1;
                        if(textPosition.y < 0)
                            textPosition.y = 0;
                        if(textPosition.y >= height)
                            textPosition.y = height-1;
                        draw.redraw();
                    }
                    else if(event.getPointerCount() == 2){
                        //zoom
                        float currentDistance = d(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        float dd = currentDistance - initialTouchDistance;
                        textSize = initialTextSize + dd*0.2f;
                        if(textSize < 12)
                            textSize = 12;

                        //move
                        PointF c = c(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        float dx = c.x - initialTouch.x;
                        float dy = c.y - initialTouch.y;
                        textPosition.x = initialPosition.x + dx;
                        textPosition.y = initialPosition.y + dy;
                        float width = draw.getWidth();
                        float height = draw.getHeight();
                        if(textPosition.x < 0)
                            textPosition.x = 0;
                        if(textPosition.x >= width)
                            textPosition.x = width-1;
                        if(textPosition.y < 0)
                            textPosition.y = 0;
                        if(textPosition.y >= height)
                            textPosition.y = height-1;

                        //rotate
                        float currentTouchRotation = k(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        textRotationRad = initialTextRotation + (currentTouchRotation - initialTouchRotation);

                        draw.redraw();
                    }
                }
                else if(action == MotionEvent.ACTION_POINTER_UP && event.getPointerCount() == 2){ //user released second finger, back to movement
                    if(event.getActionIndex() == 1) {
                        initialTouch.x = event.getX(0);
                        initialTouch.y = event.getY(0);
                    }
                    if(event.getActionIndex() == 0) {
                        initialTouch.x = event.getX(1);
                        initialTouch.y = event.getY(1);
                    }
                    initialPosition.set(textPosition);
                }

                return true;
            }
        } catch (Exception e){
            Logger.log("Draw.OnDraw.handlerText", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Draw.OnDraw.handlerText", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return true;
    }
    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }
    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(text != null && text.length() != 0){

            //drawTextOnto(canvas);
            drawTextOnto(
                    canvas,
                    textSize*draw.scale.scale_size,
                    textContour*draw.scale.scale_size,
                    draw.scale.ImageToScreenX(textPosition.x),
                    draw.scale.ImageToScreenY(textPosition.y));

            //debug for undoRect
            if((Boolean)Data.get(Data.debugBoolean())) {
                textPaint.setStrokeWidth(2f);
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setColor(Color.WHITE);
                canvas.drawRect( draw.scale.ImageToScreen(undoRect()), textPaint);
                textPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(
                        draw.scale.ImageToScreenX(textPosition.x),
                        draw.scale.ImageToScreenY(textPosition.y),
                        Tools.dp(3),
                        textPaint);
            }

            if(movingButtonEdit != null && movingButtonEdit.isVisible())
                movingButtonEdit.draw(canvas);

            if(movingButtonDelete != null && movingButtonDelete.isVisible())
                movingButtonDelete.draw(canvas);

            if(floatingMenu != null) {
                buttonAlignment.visible = intervalSlider.visible = text.contains("\n");
                float menuWidth = floatingMenu.width();
                Rect undoArea = draw.scale.ImageToScreen(undoRect());
                floatingMenu.setTargetPosition(undoArea.centerX() - (menuWidth/2f), undoArea.bottom + floatingMenu.margin());
                floatingMenu.setBounds(draw.getWidth(), draw.getHeight());
                floatingMenu.clearNoGoZones();
                floatingMenu.addNoGoZone(undoArea);
                if(hintForMoving != null && !hintForMoving.getButtonRect().isEmpty())
                    floatingMenu.addNoGoZone(hintForMoving.getButtonRect());
                if(movingButtonDelete != null)
                    floatingMenu.addNoGoZone(movingButtonDelete.buttonRect);
                if(movingButtonEdit != null)
                    floatingMenu.addNoGoZone(movingButtonEdit.buttonRect);
                floatingMenu.addNoGoZones(draw.getNoGoZones());
                floatingMenu.draw(canvas);
            }

            if(hintForMoving == null)
                hintForMoving = new HintMenu(draw, Data.tools.getResource(R.string.hint_text_moving), R.drawable.ic_help, "TEXT_MOVE", HintMenu.SHOW_TIMES);
            hintForMoving.draw(canvas);
        }
        else {
            if(hintForAdding == null)
                hintForAdding = new HintMenu(draw, Data.tools.getResource(R.string.hint_text_adding), R.drawable.ic_help, "TEXT_ADD", HintMenu.SHOW_TIMES);
            hintForAdding.draw(canvas);
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
        return view -> draw.setInstrument(getName());
    }
    private void openEditActivity(){
        TextInput.initBitmapBackground = draw.createFullBitmap();

        Intent intent = new Intent(draw.context, TextInput.class);
        intent.putExtra("text", text);
        Tools.vibrate(draw.view);
        draw.context.startActivityForResult(intent, MainActivity.REQUEST_ENTER_TEXT_PROGRAM);
    }
    private void onCancelPressed(){
        textPosition.empty();
        textSize = -1;
        this.text = null;
        draw.redraw();
    }
    private void onEditPressed(){
        openEditActivity();
    }
    @SuppressLint("RtlHardcoded")
    private void onAlignmentPressed(){
        if(textAlignment == Gravity.CENTER) {
            textAlignment = Gravity.LEFT;
            buttonAlignment.setImageResource(R.drawable.ic_text_align_left);
        }
        else if(textAlignment == Gravity.RIGHT) {
            textAlignment = Gravity.CENTER;
            buttonAlignment.setImageResource(R.drawable.ic_text_align_center);
        }
        else if(textAlignment == Gravity.LEFT){
            textAlignment = Gravity.RIGHT;
            buttonAlignment.setImageResource(R.drawable.ic_text_align_right);
        }
        draw.redraw();
    }
    private void onFramePressed(){
        if(textFrameColor == Color.TRANSPARENT)
            textFrameColor = Color.BLACK;
        else if(textFrameColor == Color.BLACK)
            textFrameColor = Color.WHITE;
        else if(textFrameColor == Color.WHITE)
            textFrameColor = Color.TRANSPARENT;
        draw.redraw();
    }
    private void onCopyPressed(){
        //apply on canvas bitmap
        //пасхалка
        if(text.equalsIgnoreCase("хуй")){
            Toast.makeText(draw.context, "Извращенец!))))", Toast.LENGTH_SHORT).show();
        }
        drawTextOnto(draw.canvas);

        Rect undoRect = undoRect();
        if (draw.undoAreaCalculator != null) {
            draw.undoAreaCalculator.reset();
            draw.undoAreaCalculator.add(undoRect.left, undoRect.top, 0);
            draw.undoAreaCalculator.add(undoRect.right, undoRect.bottom, 0);
        }
        draw.lastChangeToBitmap = System.currentTimeMillis();
        draw.undoProvider.apply(undoRect);
        draw.undoProvider.prepare();

        textPosition.set(textPosition.plus(Tools.dp(20), Tools.dp(20)));

        draw.redraw();
    }
    private void onApplyPressed(){
        onCopyPressed();
        onCancelPressed();
    }

    private void onOpacityChanged(){
        if(opacitySlider != null) {
            textOpacity = (int)opacitySlider.getValue();
            draw.redraw();
        }
    }

    private void onContourChanged(){
        if(contourSlider != null) {
            textContour = contourSlider.getValue();
            draw.redraw();
        }
    }

    private void onIntervalChanged(){
        if(intervalSlider != null) {
            textInterval = intervalSlider.getValue()/100f;
            draw.redraw();
        }
    }

    private void onFontPressed(){

        try{
            File customFontsFolder = Data.getCustomFontsFolder(draw.context);
            File systemFontFolder = new File("/system/font");
            File systemFontsFolder = new File("/system/fonts");
            File dataFontsFolder = new File("/data/fonts");

            ArrayList<String> customFonts = enumerateFonts(customFontsFolder);
            ArrayList<String> systemFont = enumerateFonts(systemFontFolder);
            ArrayList<String> systemFonts = enumerateFonts(systemFontsFolder);
            ArrayList<String> dataFonts = enumerateFonts(dataFontsFolder);
            int totalFonts = customFonts.size() + systemFont.size() + systemFonts.size() + dataFonts.size();



            fontsSelectionMenuPopup = new MenuPopup(draw.context);
            fontsSelectionMenuPopup.setHeader(Data.tools.getResource(R.string.text_input_font_hint));

            //menuPopup.addButton(Data.tools.getResource(R.string.aboutShowChangelog), v -> { });



            if(totalFonts == 0) {
                TextView textView1 = new TextView(draw.context);
                textView1.setText(R.string.text_input_no_fonts);
                textView1.setTextSize(23);//sp
                textView1.setPadding(Tools.dp(0), Tools.dp(0), Tools.dp(0), Tools.dp(30));
                textView1.setTextColor(Color.WHITE);

                TextView textView2 = new TextView(draw.context);
                textView2.setText(R.string.text_input_no_fonts_hint);
                textView2.setTextSize(15);//sp
                textView2.setTextColor(Color.LTGRAY);

                TextView textView3 = new TextView(draw.context);
                textView3.setText(customFontsFolder.getAbsolutePath());
                textView3.setTextSize(14);//sp
                textView3.setTextColor(Color.GRAY);

                LinearLayout linearLayout = new LinearLayout(draw.context);
                linearLayout.setGravity(Gravity.CENTER);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.setPadding(Tools.dp(20), Tools.dp(20), Tools.dp(20), Tools.dp(20));
                linearLayout.addView(textView1);
                linearLayout.addView(textView2);
                linearLayout.addView(textView3);
                fontsSelectionMenuPopup.addCustomView(linearLayout);
            }

            LinearLayout fontsLinear = new LinearLayout(draw.context);
            fontsLinear.setOrientation(LinearLayout.VERTICAL);

            if(!customFonts.isEmpty())
                fillFontsLinearLayout(fontsLinear, customFontsFolder, customFonts);
            if(!dataFonts.isEmpty())
                fillFontsLinearLayout(fontsLinear, dataFontsFolder, dataFonts);
            if(!systemFont.isEmpty())
                fillFontsLinearLayout(fontsLinear, systemFontFolder, systemFont);
            if(!systemFonts.isEmpty())
                fillFontsLinearLayout(fontsLinear, systemFontsFolder, systemFonts);

            if(customFonts.isEmpty()){ //if custom fonts folder is empty, add hint about custom fonts
                ImageView imageView = new ImageView(draw.context);
                imageView.setImageResource(R.drawable.menu_about);

                TextView textViewHint = new TextView(draw.context);
                textViewHint.setTextColor(Color.WHITE);
                textViewHint.setTextSize(15);
                textViewHint.setText(R.string.text_input_no_fonts_hint);

                TextView textViewAddress = new TextView(draw.context);
                textViewAddress.setTextColor(Color.LTGRAY);
                textViewAddress.setTextSize(9);
                textViewAddress.setText(customFontsFolder.getAbsolutePath());

                LinearLayout ll = new LinearLayout(draw.context);
                ll.setOrientation(LinearLayout.VERTICAL);
                ll.setPadding(Tools.dp(10), 0, Tools.dp(0), 0);
                ll.addView(textViewHint);
                ll.addView(textViewAddress);

                LinearLayout linearLayout = new LinearLayout(draw.context);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.setGravity(Gravity.CENTER_VERTICAL);
                linearLayout.setPadding(0, Tools.dp(5), 0, Tools.dp(5));
                //linearLayout.setBackgroundColor(Color.argb(50, 0, 0, 0));
                linearLayout.addView(imageView, new LinearLayout.LayoutParams(Tools.dp(20), Tools.dp(20)));
                linearLayout.addView(ll);

                fontsLinear.addView(linearLayout);
            }

            fontsSelectionMenuPopup.addCustomView(fontsLinear);
            //menuPopup.addClassicButton(Data.tools.getResource(R.string.aboutPermissionsNote), v -> { }, true);



            fontsSelectionMenuPopup.addSpacer();
            fontsSelectionMenuPopup.show();
        }catch (Exception | OutOfMemoryError e){
            Logger.log("AboutActivity.dispatchKeyEvent " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }

    public void initText(String text){
        getOnClickListener().onClick(draw.view);
        this.text = text;
        //INIT PARAMETERS
        if(textPosition.isEmpty()){
            textPosition.x = draw.scale.ScreenToImageX(draw.getWidth() / 2f);
            textPosition.y = draw.scale.ScreenToImageY(draw.getHeight() / 2f);
        }
        if(textSize == -1) {
            textSize = Tools.sp(30) / draw.scale.scale_size;
        }
    }

    public ArrayList<String> enumerateFonts(File dir) {
        ArrayList<String> fonts = new ArrayList<>();
        try {
            if (!dir.exists())
                return fonts;

            File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".ttf"));
            if (files == null)
                return fonts;
            for (File file : files) {
                fonts.add(file.getAbsolutePath());
            }
        }
        catch (Exception e){
            Logger.log(Tools.getStackTrace(e));
        }
        Logger.log("Loaded " + fonts.size() + " fonts from " + dir);
        return fonts;
    }










    public void drawTextOnto(Canvas canvas){
        drawTextOnto(canvas, textSize, textContour, textPosition.x, textPosition.y);
    }
    @SuppressLint("RtlHardcoded")
    public void drawTextOnto(Canvas canvas, float textSize, float textContour, float textPosition_x, float textPosition_y){
        //draw all the text in canvas
        if(textPaint == null) {
            textPaint = new Paint();
            textPaint.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));
        }
        if(textFont != null)
            textPaint.setTypeface(textFont);
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.FILL);
        //prepare rotation
        canvas.save();
        canvas.rotate((float)Math.toDegrees(magnetRad(textRotationRad)), textPosition_x, textPosition_y);
        //первым этапом (0) нарисовать рамку, а вторым (1) этапом уже рисовать по ней текст
        for(int stage = 0; stage <= 1; stage++){
            String[] lines = text.split("\n");
            float maxLineWidth = 0;
            for (String line : lines) {
                float textWidth = textPaint.measureText(line);
                if (textWidth > maxLineWidth)
                    maxLineWidth = textWidth;
            }
            for(int i=0; i<lines.length; i++){
                float textWidth = textPaint.measureText(lines[i]);
                float textHeight = textSize * textInterval;
                float totalHeight = textHeight * lines.length;
                float drawPositionX = textPosition_x - textWidth / 2f; //center by default
                float drawPositionY = textPosition_y - totalHeight/2f + textHeight*(i+1);
                if(textAlignment == Gravity.LEFT) {
                    drawPositionX = textPosition_x - maxLineWidth / 2f;
                }
                if(textAlignment == Gravity.RIGHT) {
                    drawPositionX = textPosition_x + (maxLineWidth-textWidth) - maxLineWidth / 2f;
                }
                //первым этапом (0) нарисовать рамку, а вторым (1) этапом уже рисовать по ней текст
                if(stage == 0 && textFrameColor != Color.TRANSPARENT){
                    textPaint.setColor(textFrameColor);
                    textPaint.setAlpha(textOpacity);
                    float frameTopPadding = 0.04f * textSize;
                    float frameBottomPadding = 0.41f * textSize;
                    float frameLeftPadding = 0.31f * textSize;
                    float frameRightPadding = 0.31f * textSize;
                    canvas.drawRoundRect(
                            new RectF( drawPositionX - frameLeftPadding,
                                    drawPositionY - textHeight  - frameTopPadding,
                                    drawPositionX + textWidth  + frameRightPadding,
                                    drawPositionY + frameBottomPadding),
                            textSize / 8,
                            textSize / 8,
                            textPaint
                    );
                }
                //первым этапом (0) нарисовать рамку, а вторым (1) этапом уже рисовать по ней текст
                if(stage == 1) {
                    textPaint.setStyle(Paint.Style.FILL);
                    textPaint.setColor(Data.getBrushColor());
                    textPaint.setAlpha(textOpacity);
                    canvas.drawText(lines[i], drawPositionX, drawPositionY, textPaint);
                    if(textContour > 0){
                        textPaint.setColor(Color.BLACK);
                        textPaint.setAlpha(textOpacity);
                        textPaint.setStyle(Paint.Style.STROKE);
                        textPaint.setStrokeWidth(textContour);
                        canvas.drawText(lines[i], drawPositionX, drawPositionY, textPaint);
                    }

                }
            }
        }
        canvas.restore();

    }



    private Rect undoRect(){
        Rect undoRect = new Rect();
        //calculate undo rect
        Paint textPaint = new Paint();
        textPaint.setTextSize(textSize);
        if(textFont != null)
            textPaint.setTypeface(textFont);
        String[] lines = text.split("\n");
        float maxLineWidth = 0;
        for (String line : lines) {
            float textWidth = textPaint.measureText(line);
            if (textWidth > maxLineWidth)
                maxLineWidth = textWidth;
        }
        float frameTopPadding = 0.04f * textSize;
        float frameBottomPadding = 0.41f * textSize;
        float frameLeftPadding = 0.31f * textSize;
        float frameRightPadding = 0.31f * textSize;
        maxLineWidth += frameLeftPadding;
        maxLineWidth += frameRightPadding;

        float textHeight = textSize * 1.1f;
        float totalHeight = textHeight * lines.length + frameTopPadding + frameBottomPadding;
        float yOffset = textSize*0.183f;//Хуй його знає чого получається саме так

        maxLineWidth *= 1.1f;
        totalHeight *= 1.3f;
        float radius = (float)Math.sqrt(totalHeight*totalHeight + maxLineWidth*maxLineWidth);
        radius *= 1.1;
        undoRect.top = (int)(textPosition.y + yOffset - radius/2);
        undoRect.bottom = (int)(textPosition.y + yOffset + radius/2);
        undoRect.left = (int)(textPosition.x - radius/2);
        undoRect.right = (int)(textPosition.x + radius/2);

        return undoRect;
    }


    private float magnetRad(float input){
        double radians = input;
        while(radians > Math.PI*2d)
            radians -= Math.PI*2d;
        while(radians < 0)
            radians += Math.PI*2d;
        double threshold = 0.03f;

        //0 *
        if(radians < 0d + threshold || radians > Math.PI*2d - threshold)
            radians = 0d;

        //90 *
        if(radians < Math.PI*0.5d + threshold && radians > Math.PI*0.5d - threshold)
            radians = Math.PI*0.5d;

        //180 *
        if(radians < Math.PI + threshold && radians > Math.PI - threshold)
            radians = Math.PI;

        //270 *
        if(radians < Math.PI*1.5d + threshold && radians > Math.PI*1.5d - threshold)
            radians = Math.PI*1.5d;

        return (float)radians;
    }
    //distance between points
    private float d(float x1, float y1, float x2, float y2){
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float)Math.sqrt( dx*dx+dy*dy );
    }
    //rotation
    private float k(float x1, float y1, float x2, float y2){
        return (float)Math.atan2((y1 - y2), (x1 - x2));
    }
    //center
    private PointF c(float x1, float y1, float x2, float y2){
        return new PointF((x1 + x2) / 2f, (y1 + y2) / 2f);
    }

    void fillFontsLinearLayout(LinearLayout layoutToFill, File folderToPrintInTitle, ArrayList<String> fontsListToFill){
        ImageView imageView = new ImageView(draw.context);
        imageView.setImageResource(R.drawable.menu_open);

        TextView textViewHint = new TextView(draw.context);
        textViewHint.setTextColor(Color.WHITE);
        textViewHint.setTextSize(15);
        textViewHint.setText(R.string.text_input_fonts_from_folder);

        TextView textViewAddress = new TextView(draw.context);
        textViewAddress.setTextColor(Color.LTGRAY);
        textViewAddress.setTextSize(9);
        textViewAddress.setText(folderToPrintInTitle.getAbsolutePath());

        LinearLayout ll = new LinearLayout(draw.context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(Tools.dp(10), 0, Tools.dp(0), 0);
        ll.addView(textViewHint);
        ll.addView(textViewAddress);

        LinearLayout linearLayout = new LinearLayout(draw.context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setPadding(0, Tools.dp(5), 0, Tools.dp(5));
        //linearLayout.setBackgroundColor(Color.argb(50, 0, 0, 0));
        linearLayout.addView(imageView, new LinearLayout.LayoutParams(Tools.dp(20), Tools.dp(20)));
        linearLayout.addView(ll);

        layoutToFill.addView(linearLayout);

        for(String fontPath:fontsListToFill){
            try {
                FontView.OnSelected onSelected = tf -> {
                    textFont = tf;
                    if(fontsSelectionMenuPopup != null)
                        fontsSelectionMenuPopup.cancel();
                    draw.redraw();
                };
                FontView fontView = new FontView(draw.context, fontPath, text.substring(0, Math.min(20, text.length())), onSelected);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Tools.dp(45));
                layoutToFill.addView(fontView, lp);
            }
            catch (Exception e){
                Logger.log("TextInput.showFontSelectorWindow(), Error: " + e);
                e.printStackTrace();
            }
        }
    }
    private static class FontView extends View {
        private interface OnSelected{
            void onSelected(Typeface tf);
        }
        private final String sampleText;
        private final String fontName;
        private final Paint paintSample = new Paint();
        private final Paint paintName = new Paint();
        private final float textSizeSample = Tools.sp(20);
        private final float textSizeName = Tools.sp(7);
        private final FontView.OnSelected onSelected;
        private Typeface typeface = null;

        FontView(Context in_context, String in_fontPath, String sampleText, FontView.OnSelected _onSelected){
            super(in_context);
            this.sampleText = sampleText;
            fontName = new File(in_fontPath).getName().replace(".ttf", "");
            onSelected = _onSelected;
            //init typeface
            if(!in_fontPath.equals("")){
                typeface = Typeface.createFromFile(in_fontPath);
                paintSample.setTypeface(typeface);
            }
            setClickable(true);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                if(isHovered()){
                    paintSample.setColor(Color.argb(20, 255,255,255));
                    paintSample.setStyle(Paint.Style.FILL);
                    canvas.drawRect(0,0,getWidth(), getHeight(), paintSample);
                }
            }

            paintSample.setTextSize(textSizeSample);
            paintSample.setColor(Color.WHITE);
            paintSample.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));
            paintSample.setStyle(Paint.Style.FILL);

            paintName.setTextSize(textSizeName);
            paintName.setColor(Color.argb(100, 255, 255, 255));
            paintName.setAntiAlias((Boolean)Data.get(Data.antialiasingBoolean()));
            paintName.setStyle(Paint.Style.FILL);

            float height = getHeight();
            float width = getWidth();
            float textWidth = paintSample.measureText(sampleText);
            float textX = width/2f - (textWidth/2f);
            float nameX = Tools.dp(4);
            float textY = height - (height - textSizeSample)/2f - Tools.dp(5);
            float nameY = height - Tools.dp(4);

            canvas.drawText(sampleText, textX, textY, paintSample);
            canvas.drawText(fontName, nameX, nameY, paintName);
            canvas.drawLine(0, height-1, width-1, height-1, paintSample);
        }

        @Override
        public boolean performClick() {
            Logger.log("FontView.performClick Selecting font " + fontName);
            if(onSelected != null)
                onSelected.onSelected(typeface);
            return super.performClick();
        }
    }
}
