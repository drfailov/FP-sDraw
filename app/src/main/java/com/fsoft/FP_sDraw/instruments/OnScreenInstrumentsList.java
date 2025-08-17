package com.fsoft.FP_sDraw.instruments;

import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterPath;
import static com.fsoft.FP_sDraw.common.Tools.getSquircleCenterSum;

import android.graphics.*;
import android.os.Build;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.menu.ColorPickerDialog;
import com.fsoft.FP_sDraw.menu.MainMenu;
import com.fsoft.FP_sDraw.menu.MenuPopup;
import com.fsoft.FP_sDraw.menu.MyCheckBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 30.12.2015.
 * Refactored by Dr. Failov on 15.02.2023.
 */
public class OnScreenInstrumentsList implements Instrument {
    DrawCore draw;
    ArrayList<ClickableMovableAnimatedCircleWithImageContainer> imageContainers = new ArrayList<>();
    public ClickableMovableAnimatedCircleWithImageContainerForInstruments circleInstruments = null;
    public ClickableMovableAnimatedCircleWithImageContainerForColors colorsCircle = null;

    public OnScreenInstrumentsList(DrawCore drawCore) {
        this.draw = drawCore;
    }
    @Override public String getName() {
        return "instrumentslist";
    }
    @Override public String getVisibleName() {
        return "instrumentslist";
    }
    @Override public int getImageResourceID() {
        return 0;
    }
    @Override public void onSelect() {
        loadInstrumentsList();
        loadPaletteList();

        for(ClickableMovableAnimatedCircleWithImageContainer mainCircle:imageContainers)
            mainCircle.refresh();
    }

    /*Load or refresh list of instruments shown on screen*/
    public void loadInstrumentsList(){
        float radius = Data.store().DPI/8;
        float height = draw.getHeight();

        if(circleInstruments == null) {
            circleInstruments = new ClickableMovableAnimatedCircleWithImageContainerForInstruments(radius * 3.9f, height - radius * 1.3f, radius);
            circleInstruments.setImage(R.drawable.menu_brush);
            circleInstruments.setName("circleInstrument");
            circleInstruments.setMovable(true);
            circleInstruments.addOnLongClickListener(view -> {
                openInstrumentsSelector(view);
                return true;
            });
            imageContainers.add(circleInstruments);
        }
        circleInstruments.opened = false;

        ArrayList<Instrument> instruments = new ArrayList<>();
        //Logger.log("INSTRUMENT LIST REFRESH");
        /*Here is the right place to load and parse list of instruments*/
        ArrayList<String> onScreenInstrumentsList = Data.getOnscreenInstrumentList();
        for (Instrument instrument: draw.instruments) {
            if(onScreenInstrumentsList.contains(instrument.getName())){
                instruments.add(instrument);
            }
        }

        if(circleInstruments.currentInstruments == null || !circleInstruments.currentInstruments.equals(instruments)) {
            circleInstruments.currentInstruments = instruments;
            circleInstruments.clearCircles();
            for (int i = 0; i < instruments.size(); i++) {
                Instrument instrument = instruments.get(i);
                ClickableMovableAnimatedCircleWithImage clickableMovableAnimatedCircleWithImage = new ClickableMovableAnimatedCircleWithImage(0, 0, radius);
                clickableMovableAnimatedCircleWithImage.setImage(instrument.getImageResourceID());
                clickableMovableAnimatedCircleWithImage.addOnClickListener(instrument.getOnClickListener());
                circleInstruments.addCircle(clickableMovableAnimatedCircleWithImage);
            }
        }
    }
    public void loadPaletteList(){
        float height = draw.getHeight();
        float radius = Data.store().DPI/8;
        if(colorsCircle == null) {
            colorsCircle = new ClickableMovableAnimatedCircleWithImageContainerForColors(radius * 1.3f, height - radius * 1.3f, radius);
            colorsCircle.setColor(Data.getBrushColor());
            colorsCircle.setName("circleColor");
            colorsCircle.setImage(R.drawable.settings_palette);
            colorsCircle.setMovable(true);
            colorsCircle.setEnableMount(false);
            colorsCircle.addOnLongClickListener(v -> {
                ColorPickerDialog dialog = new ColorPickerDialog(draw.context, color -> {
                    Data.save(color, Data.brushColorInt());
                    draw.refresh();
                }, (Integer) Data.get(Data.brushColorInt()));
                dialog.show();
                return false;
            });
            imageContainers.add(colorsCircle);
        }
        int[] newPalette = Data.getPaletteBrush();
        int[] oldPalette = colorsCircle.currentPalette;
        if(!Arrays.equals(newPalette, oldPalette)) {
            colorsCircle.currentPalette = newPalette;
            colorsCircle.clearCircles();
            //colorsCircle
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < newPalette.length; i++) {
                final int color = newPalette[i];
                ClickableMovableAnimatedCircleWithImage clickableMovableAnimatedCircleWithImage = new ClickableMovableAnimatedCircleWithImage(0, 0, radius);
                clickableMovableAnimatedCircleWithImage.setColor(color);
                clickableMovableAnimatedCircleWithImage.addOnClickListener(view -> {
                    Data.save(color, Data.brushColorInt());
                    draw.refresh();
                });
//                clickableMovableAnimatedCircleWithImage.addOnLongClickListener(v -> {
//                    Data.save(color, Data.backgroundColorInt());
//                    draw.refresh();
//                    return false;
//                });
                colorsCircle.addCircle(clickableMovableAnimatedCircleWithImage);
            }
        }
    }


    void openInstrumentsSelector(View view){
        try{
            final MenuPopup menuPopup = new MenuPopup(draw.context);
            menuPopup.setHeader(Data.tools.getResource(R.string.instrument_selector_content));

            final ArrayList<MyCheckBox> checkBoxes = new ArrayList<>();
            ArrayList<String> content = Data.getOnscreenInstrumentList();
            for(Instrument instrument: draw.instruments){
                if(instrument.isVisibleToUser()) {
                    MyCheckBox checkBox = menuPopup.addCheckbox(instrument.getVisibleName(), instrument.getImageResourceID(), content.contains(instrument.getName()));
                    checkBox.setTag(instrument);
                    checkBoxes.add(checkBox);
                }
            }


            menuPopup.addLittleText(draw.context.getString(R.string.warning_instruments_can_not_fit));
            //menuPopup.addSpacer();
            menuPopup.addClassicButton(Data.tools.getResource(R.string.apply), v -> {
                ArrayList<String> result = new ArrayList<>();
                for (MyCheckBox checkBox:checkBoxes) {
                    if(checkBox.isChecked()) {
                        if (checkBox.getTag() != null && checkBox.getTag() instanceof Instrument) {
                            result.add(((Instrument) checkBox.getTag()).getName());
                        }
                    }
                }
                if(result.size() < 2){
                    Logger.show(Data.tools.getResource(R.string.onscreen_instrument_list_error_select_minimum_two));
                    return;
                }
                Data.saveOnscreenInstrumentList(result);
                menuPopup.cancel();
                loadInstrumentsList(); //refresh instrument list
            }, false);

            menuPopup.addSpacer();
            menuPopup.show();
        }catch (Exception | OutOfMemoryError e){
            Logger.log("AboutActivity.dispatchKeyEvent " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }

    @Override public void onDeselect() {
        circleInstruments.inpand();
        colorsCircle.inpand();
    }
    @Override public boolean onTouch(MotionEvent event) {
        if(Data.tools == null)
            return false;
        if(!Data.tools.isAllowedDeviceForUi(event))
            return false;
//        if(!Data.tools.isAllowedDevice(event))
//            return false;
        for(ClickableMovableAnimatedCircleWithImageContainer mainCircle:imageContainers)
            if(mainCircle.processTouchEvent(event))
                return true;
        return false;
    }
    @Override public boolean onKey(KeyEvent event) {
        return false;
    }
    @Override public void onCanvasDraw(Canvas canvas, boolean drawUi) {
            for(ClickableMovableAnimatedCircleWithImageContainer mainCircle:imageContainers) {
                //if(!draw.fingersOnScreen() || mainCircle.pressed)   //uncomment this to hide menu while drawing
                mainCircle.draw(canvas);
            }
    }
    @Override public boolean isActive() {
        return false;
    }
    @Override  public boolean isVisibleToUser() {
        return false;
    }
    @Override public View.OnClickListener getOnClickListener() {
        return null;
    }

    public class ClickableMovableAnimatedCircleWithImageContainerForColors extends ClickableMovableAnimatedCircleWithImageContainer{
        boolean enabled = true;
        public int[] currentPalette; // Is used to temporary save current palette to COMPARE it in future by other classes (not used by this class)

        public ClickableMovableAnimatedCircleWithImageContainerForColors(float cx, float cy, float radius) {
            super(cx, cy, radius);
            pointsProvider = new SpiralPointsPrivider();
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override public void addCircle(ClickableMovableAnimatedCircleWithImage circleWithImage) {
            circleWithImage.addOnClickListener(view -> {
                refresh();
                inpand();
            });
            super.addCircle(circleWithImage);
        }
        @Override public void refresh() {
            ClickableMovableAnimatedCircleWithImageContainerForColors.this.setColor(Data.getBrushColor());
            enabled = (Boolean)Data.get(Data.showColorsSelectorBoolean());
        }
        @Override public void draw(Canvas canvas) {
            if(enabled)
                super.draw(canvas);
        }
        @Override public boolean processTouchEvent(MotionEvent motionEvent) {
            //если палитра выключена
            if(!enabled)
                return false;
            //обработать работу палитры
            return super.processTouchEvent(motionEvent);
        }
    }
    public class ClickableMovableAnimatedCircleWithImageContainerForInstruments extends ClickableMovableAnimatedCircleWithImageContainer{
        boolean enabled = true;
        public ArrayList<Instrument> currentInstruments;  // Is used to temporary save current instruments to COMPARE it in future (not used by this class)

        public ClickableMovableAnimatedCircleWithImageContainerForInstruments(float cx, float cy, float radius) {
            super(cx, cy, radius);
            pointsProvider = new CirclePointsProvider();
        }
        @Override public void addCircle(ClickableMovableAnimatedCircleWithImage circleWithImage) {
            circleWithImage.addOnClickListener(view -> {
                refresh();
                inpand();
            });
            super.addCircle(circleWithImage);
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override public void refresh() {
            ClickableMovableAnimatedCircleWithImageContainerForInstruments.this.setImage(draw.currentInstrument.getImageResourceID());
            enabled = (Boolean)Data.get(Data.showInstrumentsSelectorBoolean());
        }
        @Override public void draw(Canvas canvas) {
            if(enabled)
                super.draw(canvas);
        }
        @Override public boolean processTouchEvent(MotionEvent motionEvent) {
            return enabled && super.processTouchEvent(motionEvent);
        }
    }
    class ClickableMovableAnimatedCircleWithImageContainer extends ClickableMovableAnimatedCircleWithImage{
        private final ArrayList<ClickableMovableAnimatedCircleWithImage> circleWithImages = new ArrayList<>();
        protected PointsProvider pointsProvider;
        private boolean needHide = false;
        protected boolean opened = false;
        //перехват действия когда открыта панель
        private boolean interceptedOutsideClick = false;
        private boolean enableMount = false;
        private Paint mountPaint = null;

        public ClickableMovableAnimatedCircleWithImageContainer(float cx, float cy, float radius) {
            super(cx, cy, radius);
            pointsProvider = new CirclePointsProvider();
        }
        public void addCircle(ClickableMovableAnimatedCircleWithImage circleWithImage){
            circleWithImage.setVisible(false);
            circleWithImage.setTouchRectCoef(1f);
            circleWithImages.add(circleWithImage);
        }
        public void clearCircles(){
            circleWithImages.clear();
        }
        public void setEnableMount(boolean enableMount) {
            this.enableMount = enableMount;
        }
        public void refresh(){

        }
        @Override public boolean processTouchEvent(MotionEvent motionEvent) {
            if(!visible)
                return false;
            if(super.processTouchEvent(motionEvent))
                return true;
            if(!opened && !interceptedOutsideClick){
                return false;
            }

            //редирект тач-евента всем подчиненным. но зачем? когда они не открыты.
            for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages) {
                if(circleWithImage.processTouchEvent(motionEvent))
                    return true;
            }
            //перехват нажатия когда меню открыто
            if(opened && motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                interceptedOutsideClick = true;
                draw.view.playSoundEffect(SoundEffectConstants.CLICK);
                inpand();
                return true;
            }
            else if(interceptedOutsideClick && motionEvent.getAction() == MotionEvent.ACTION_UP){
                interceptedOutsideClick = false;
                return true;
            }
            else return interceptedOutsideClick;
        }
        @Override public void draw(Canvas canvas) {
            if(!visible)
                return;


            //DEBUG CIRCLE DRAWING
            if((Boolean)Data.get(Data.debugBoolean()))
                pointsProvider.drawDebug(canvas);

            if(enableMount){
                if(mountPaint == null){
                    mountPaint = new Paint();
                    int mountColor = Color.argb(150, 0, 0, 0);
                    mountPaint.setColor(mountColor);
                    mountPaint.setAntiAlias(true);
                }
                float maxRadius = 0;
                for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages) {
                    float radius = getCenter().d(circleWithImage.getCenter()) + circleWithImage.getRadius()*1.5f;
                    if(radius > maxRadius)
                        maxRadius = radius;
                }
                Point center = getCenter();
                canvas.drawCircle(getCenterX(), getCenterY(), maxRadius, mountPaint);
            }


            for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages) {
                circleWithImage.draw(canvas);
            }
            super.draw(canvas);
        }
        @Override public void setCenter(float cx, float cy, float boundsX, float boundsY) {
            if(circleWithImages != null && circleWithImages.size() > 0 && opened) {
                ArrayList<Point> points = pointsProvider.getPoints();
                for(int i=0; i<points.size(); i++){
                    Point p = points.get(i);
                    if(p != null)
                        circleWithImages.get(i).moveTo(p.x, p.y, boundsX, boundsY);
                }
            }
            super.setCenter(cx, cy, boundsX, boundsY);
        }
        @Override protected void onClick() {
            if(opened)
                inpand();
            else
                expand();
            super.onClick();
        }
        protected void inpand(){
            opened = false;
            Thread inpandThread = new Thread(() -> {
                try {
                    int delay = 100/circleWithImages.size();
                    for (int i = 0; i < circleWithImages.size(); i++) {
                        circleWithImages.get(i).moveTo(getRect().centerX(), getRect().centerY(), draw.getWidth(), draw.getHeight());
                        //noinspection BusyWait
                        Thread.sleep(delay);
                    }
                    needHide = true;
                    Thread.sleep(200);
                    if(needHide) {
                        for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages) {
                            circleWithImage.setVisible(false);
                        }
                        needHide = false;
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            });
            inpandThread.setName("inpandThread for ClickableMovableAnimatedCircleWithImageContainer");
            inpandThread.start();
        }
        protected void expand(){
            for (ClickableMovableAnimatedCircleWithImageContainer circleWithImage:imageContainers){
                try{
                    if(circleWithImage.opened)
                        circleWithImage.inpand();
                }
                catch (Exception e){/*FUCK*/}
            }
            opened = true;
            for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages) {
                circleWithImage.setCenter(getRect().centerX(), getRect().centerY(), draw.getWidth(), draw.getHeight());
            }
            Thread expandThread = new Thread(() -> {
                try {
                    needHide = false;
                    for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages) {
                        circleWithImage.setVisible(true);
                    }
                    ArrayList<Point> points = pointsProvider.getPoints();
                    int delay = 100/points.size();
                    for (int i = 0; i < points.size(); i++) {
                        Point p = points.get(i);
                        circleWithImages.get(i).moveTo(p.x, p.y, draw.getWidth(), draw.getHeight());
                        //noinspection BusyWait
                        Thread.sleep(delay);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            });
            expandThread.setName("expandThread for ClickableMovableAnimatedCircleWithImageContainer");
            expandThread.start();
        }

        abstract class PointsProvider{
            abstract ArrayList<Point> getPoints();
            abstract void drawDebug(Canvas canvas);
        }
        @SuppressWarnings("unused")
        class CirclePointsProviderTest extends PointsProvider{
            private final Point current = new Point();
            private final Point screen = new Point();
            private final Point anchor1 = new Point();
            private final Point anchor2 = new Point();
            private final Paint paint = new Paint();

            public CirclePointsProviderTest() {

            }

            protected ArrayList<Point> getPoints(){
                current.set(getCenter());
                screen.set(draw.getWidth(), draw.getHeight());

                if(current.x < screen.x/2f && current.y >= screen.y/2f){ //BL
                    anchor1.set(/*x*/0, /*y*/0);
                    anchor2.set(/*x*/screen.x, /*y*/screen.y);
                }

                if(current.x >= screen.x/2f && current.y >= screen.y/2f){ //BR
                    anchor1.set(/*x*/0, /*y*/screen.y);
                    anchor2.set(/*x*/screen.x, /*y*/0);
                }

                if(current.x < screen.x/2f && current.y < screen.y/2f){ //TL
                    anchor1.set(/*x*/screen.x, /*y*/0);
                    anchor2.set(/*x*/0, /*y*/screen.y);
                }

                if(current.x >= screen.x/2f && current.y < screen.y/2f){ //TR
                    anchor1.set(/*x*/screen.x, /*y*/screen.y);
                    anchor2.set(/*x*/0, /*y*/0);
                }


                return new ArrayList<>();
            }
            protected void drawDebug(Canvas canvas){
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(Tools.dp(1));
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(Tools.sp(10));
                canvas.drawLine(current.x, current.y, anchor1.x, anchor1.y, paint);
                canvas.drawLine(current.x, current.y, anchor2.x, anchor2.y, paint);
                float angle = getCenter().minus(anchor1).degreeWith(getCenter().minus(anchor2));
                float numberOfInstruments = circleWithImages.size();
                float L = numberOfInstruments * radius * 3f;
                //https://www-formula.ru/index.php/2011-09-24-00-31-25
                float pi = 3.1415f;
                float radius = pi*L*angle / 180;
                canvas.drawText(angle+"*", current.x - Tools.dp(25), current.y  - Tools.dp(50), paint);
                canvas.drawText(radius+"R", current.x - Tools.dp(25), current.y  - Tools.dp(60), paint);


                current.degreeWith(anchor2);
            }
        }
        class CirclePointsProvider extends PointsProvider{
            private final Point lastCalculation = new Point();
            private float radius = 300;

            public CirclePointsProvider() {
                //this.radius = radius*5;
            }

            private ArrayList<Pair<Float, Float>> getSequencies(float radius){
                //ПОИСК КРУГОВЫХ КООРДИНАТ
                ArrayList<Pair<Float, Float>> sequencies = new ArrayList<>();
                float begin = -1;
                float degrees = 0;
                float degress_max = 360;
                float step = 5;
                Boolean lastState = null;
                float cx = getRect().centerX();
                float cy = getRect().centerY();
                double frame = Tools.dp(10);
                while(true){
                    double x = cx + Math.cos(Math.toRadians(degrees))*radius;
                    double y = cy + Math.sin(Math.toRadians(degrees))*radius;
                    boolean state =  x >= getRadius() + frame && x < draw.getWidth()-getRadius() - frame && y >= getRadius() + frame && y < draw.getHeight()-getRadius()-frame;
                    if(lastState == null) //first step
                        lastState = state;
                    if(!lastState && state)//begin
                        begin = degrees;
                    if(!state && lastState){//end
                        if(begin != -1){
                            sequencies.add(new Pair<>(begin, degrees));
                            begin = -1;
                        }
                        if(degrees > degress_max)
                            break;
                    }
                    if(degrees > degress_max && begin == -1){
                        if(sequencies.size() == 0)
                            sequencies.add(new Pair<>(0f, 360f));
                        break;
                    }
                    lastState = state;
                    degrees += step;
                }
                return sequencies;
            }
            private Pair<Float, Float> getMaxSequence(ArrayList<Pair<Float, Float>> sequencies){
                Pair<Float, Float> maxPair = null;
                for(int i=0; i<sequencies.size(); i++) {
                    if(maxPair == null)
                        maxPair = sequencies.get(i);
                    else {
                        float maxdx = maxPair.second - maxPair.first;
                        float dx = sequencies.get(i).second - sequencies.get(i).first;
                        if(dx > maxdx)
                            maxPair = sequencies.get(i);
                    }
                }
                return maxPair;
            }
            protected void drawDebug(Canvas canvas){}
            protected ArrayList<Point> getPoints(){
                if(lastCalculation.isEmpty() || lastCalculation.distanceTo(getCenter()) > Tools.dp(20)){
                    lastCalculation.set(getCenter());
                    getPoints();
                }
/*                if(firstCalculation){
//                    //начальная инициализация позволит эвристическим методом подобрать оптимальный радиус
//                    for (int i=7; i> 0; i--){
//                        Pair<Float, Float> maxPair =  getMaxSequence(getSequencies(radius+getRadius()));
//                        radius = calsulateRadius(maxPair);
//                    }
//                    firstCalculation = false;
//                }*/
                Pair<Float, Float> maxPair =  getMaxSequence(getSequencies(radius));
                radius = calsulateRadius(maxPair);
                lastCalculation.set(getCenter());

                ArrayList<Point> points = new ArrayList<>();
                float cx = getRect().centerX();
                float cy = getRect().centerY();
                float numberOfIcons = circleWithImages.size();
                float dx = maxPair.second - maxPair.first;
                float ddx = dx / (numberOfIcons-(dx > 340?0:1));
                float begin = maxPair.first;
                for(int i=0; i<numberOfIcons; i+= 1){
                    float degrees = ddx*(i) + begin;
                    double x = cx + Math.cos(Math.toRadians(degrees))*radius;
                    double y = cy + Math.sin(Math.toRadians(degrees))*radius;
                    points.add(new Point((float)x,(float)y));
                }
                return points;
            }
            private float calsulateRadius(Pair<Float, Float> maxPair){
                float maxChildRadius = 0;
                float numberOfIcons = circleWithImages.size();
                for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages)
                    if(maxChildRadius < circleWithImage.getRadius())
                        maxChildRadius = circleWithImage.getRadius();

                //got here: http://www-formula.ru/index.php/2011-09-24-00-31-25
                float L = maxChildRadius*2*(numberOfIcons);
                float pi = (float)Math.PI;
                float alpha = maxPair.second - maxPair.first;
                float R = (180f*L)/(pi*alpha);
                return Math.max(R, maxChildRadius*4);
            }
        }
        class SpiralPointsPrivider extends PointsProvider{
            protected void drawDebug(Canvas canvas){}
            protected ArrayList<Point> getPoints(){
                ArrayList<Point> points = new ArrayList<>();
                float cx = getRect().centerX();
                float cy = getRect().centerY();
                float maxChildRadius = 0;
                int numberOfIcons = circleWithImages.size();
                for (ClickableMovableAnimatedCircleWithImage circleWithImage : circleWithImages)
                    if(maxChildRadius < circleWithImage.getRadius())
                        maxChildRadius = circleWithImage.getRadius();
                float radius = getRadius()*4;
                float screenSize = Math.max(draw.getWidth(), draw.getHeight());

                float degrees = 270;
                float degrees_step = (float)Math.toDegrees(Math.atan(2.5f*maxChildRadius / radius));

                while(points.size() < numberOfIcons){
                    double x = cx + Math.cos(Math.toRadians(degrees))*radius;
                    double y = cy + Math.sin(Math.toRadians(degrees))*radius;
                    Point point = new Point((float)x,(float)y);
                    if(isValid(point, maxChildRadius)){
                        if(!checkCollision(point, points, maxChildRadius)){
                            points.add(point);
                        }
                    }
                    else {
                        if(radius > screenSize)
                            points.add(point);
                    }
                    degrees += degrees_step;
                    if(degrees > 630){
                        degrees = 270;
                        radius += maxChildRadius*2.2f;
                        degrees_step = (float)Math.toDegrees(Math.atan(2.3f*maxChildRadius / radius));
                    }
                }
                return points;
            }
            private float minDistance(Point point, ArrayList<Point> points){
                float min = 9999;
                if(points == null)
                    return min;
                for(Point cur:points)
                    min = Math.min(cur.distanceTo(point), min);
                return min;
            }
            private boolean checkCollision(Point point, ArrayList<Point> points, float radius){
                return minDistance(point, points) < radius*2f;
            }
            private boolean isValid(Point point, float radius){
                boolean inScreen = point.x >= radius && point.x < draw.getWidth()-radius && point.y >= radius && point.y < draw.getHeight()-radius;
                boolean intersect = false;
                for (ClickableMovableAnimatedCircleWithImage circleWithImage:imageContainers){
                    if(circleWithImage.getCenter().distanceTo(point) < radius*3)
                        intersect = true;
                }
                return inScreen && !intersect;
            }
        }
    }
    class ClickableMovableAnimatedCircleWithImage extends AnimatedCircleWithImage{
        private final ArrayList<View.OnClickListener> onClickListener = new ArrayList<>();
        private final ArrayList<View.OnLongClickListener> onLongClickListener = new ArrayList<>();
        private boolean moving = false;
        private boolean movable = false;
        private String name = null;
        private Timer longClickTimer = null;

        public ClickableMovableAnimatedCircleWithImage(float cx, float cy, float radius) {
            super(cx, cy, radius);
        }
        public void setMovable(boolean movable) {
            this.movable = movable;
        }
        public boolean processTouchEvent(MotionEvent motionEvent){
            if(!visible)
                return false;
            int action = motionEvent.getAction();
            float x = motionEvent.getX();
            float y = motionEvent.getY();


            if(action == MotionEvent.ACTION_DOWN){
                if(getTouchRect().contains(x,y)){ //pressed on circle
                    pressed = true;

                    //schedule for running long-click action
                    long delay = ViewConfiguration.getLongPressTimeout();
                    if(longClickTimer != null){
                        longClickTimer.cancel();
                        longClickTimer.purge();
                        longClickTimer = null;
                    }
                    longClickTimer = new Timer();
                    longClickTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(longClickTimer != null){
                                longClickTimer.cancel();
                                longClickTimer.purge();
                                longClickTimer = null;
                            }
                            if(pressed && !moving) {
                               draw.uiHandler.post(() -> {
                                   try {
                                       onLongClick();
                                   }
                                   catch (Throwable e) {
                                           Logger.log("Error in OnScreen Instrument Long Click(...): " + Tools.getStackTrace(e));
                                           e.printStackTrace();
                                       }
                               });
                            }
                        }
                    }, delay);
                    return true;
                }
            }
            else if(action == MotionEvent.ACTION_UP){
                if(!moving && pressed) {
                    pressed = false;
                    if(getTouchRect().contains(x,y)){
                        if(longClickTimer != null)
                            onClick();
                    }
                    if(longClickTimer != null){
                        longClickTimer.cancel();
                        longClickTimer.purge();
                        longClickTimer = null;
                    }
                    return true;
                }
                if(moving){
                    moving = false;
                    pressed = false;
                    draw.redraw();
                    return true;
                }
                pressed = false;
                moving = false;
            }
            else if(action == MotionEvent.ACTION_CANCEL){
                if(!moving && pressed) {
                    pressed = false;
                    if(longClickTimer != null){
                        longClickTimer.cancel();
                        longClickTimer.purge();
                        longClickTimer = null;
                    }
                    return true;
                }
                if(moving){
                    moving = false;
                    pressed = false;
                    return true;
                }
                pressed = false;
                moving = false;
            }
            if(action == MotionEvent.ACTION_MOVE){
                if(moving){
                    float width = draw.getWidth();
                    float height = draw.getHeight();
                    //проверка правильности координат и их восстановление
                    if(x >= width)
                        x = width-1;
                    else if(x < 0)
                        x = 0;

                    if(y >= height)
                        y = height-1;
                    else if(y < 0)
                        y = 0;

                    if (name != null) {
                        Data.save(x / width, Data.itemPositionXFloat(name, x / width));
                        Data.save(y / height, Data.itemPositionYFloat(name, y / height));
                    } else
                        Logger.log("ClickableMovableAnimatedCircleWithImage coordinates don't saved because of name is not set.");
                    setCenter(x, y, draw.getWidth(), draw.getHeight());
                    return true;
                }
                if(! getTouchRect().contains(x,y) && pressed){
                    //pressed = false;
                    if(movable)
                        moving = true;
                    return true;
                }
                return pressed;
            }
            return false;
        }
        public ClickableMovableAnimatedCircleWithImage addOnClickListener(View.OnClickListener onClickListener){
            this.onClickListener.add(onClickListener);
            return this;
        }
        public void addOnLongClickListener(View.OnLongClickListener onClickListener){
            this.onLongClickListener.add(onClickListener);
        }
        public ClickableMovableAnimatedCircleWithImage setName(String name){
            this.name = name;
            //load from save
            //float cx = getRect().centerX();
            //float cy = getRect().centerY();
//            float width = draw.getWidth();
//            float height = draw.getHeight();
            float coefX = (Float)Data.get(Data.itemPositionXFloat(name, centerCoefs.x));
            float coefY = (Float)Data.get(Data.itemPositionYFloat(name, centerCoefs.y));
//            Logger.log("itemPositionXFloat="+coefX);
//            Logger.log("itemPositionYFloat="+coefY);
//            cx = width*coefX;
//            cy = height*coefY;
            centerCoefs.set(coefX, coefY);
            return this;
        }
        protected void onClick(){
            Tools.vibrate(draw.view);
            draw.view.playSoundEffect(SoundEffectConstants.CLICK);
            if(onClickListener != null) {
                for (View.OnClickListener listener:onClickListener)
                    if(listener != null)
                        listener.onClick(draw.view);
            }
        }
        protected void onLongClick(){
            try {
                Tools.vibrate(draw.view);
            }catch (Exception e){
                e.printStackTrace();
            }

            if(onLongClickListener != null) {
                for (View.OnLongClickListener listener:onLongClickListener)
                    listener.onLongClick(draw.view);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            if (!visible)
                return;

            if(!onLongClickListener.isEmpty()){
                circlepaint.setTextSize(Tools.sp(10));
                circlepaint.setStyle(Paint.Style.FILL);
                circlepaint.setColor(Color.argb(100, 255, 255, 255));
                float w = circlepaint.measureText("...");
                canvas.drawText("...", getCenterX() - w/2f, getCenterY() + radius*0.82f, circlepaint);
            }
        }
    }
    class AnimatedCircleWithImage extends CircleWithImage{
        protected Point aim = new Point(0,0);
        protected Thread movingThread = null;

        public AnimatedCircleWithImage(float cx, float cy, float radius) {
            super(cx, cy, radius);
        }
        public void moveTo(float x, float y, float boundsX, float boundsY){
            //Logger.log("Move to x="+x+", y="+y+", inside w="+boundsX+", h="+boundsY);
            if(centerCoefs.x == -1 || centerCoefs.y == -1)
                setCenter(x,y,boundsX,boundsY);
            aim.x = x/boundsX;
            aim.y =  y/boundsY;
            //Logger.log("Move to aim.x="+aim.x+", aim.y="+aim.y);
            if (movingThread == null) {
                movingThread = new Thread(this::moveToAimAsync);
                movingThread.setName("movingThread for AnimatedCircleWithImage");
                movingThread.start();
            }
        }
        private void moveToAimAsync(){
            try {
                double initial_d = Math.sqrt(Math.pow(aim.x - centerCoefs.x, 2) + Math.pow(aim.y - centerCoefs.y, 2));
                double threshold = initial_d / 10;
                //Logger.log("initial_d="+initial_d+", threshold="+threshold);

                while (Math.sqrt(Math.pow(aim.x - centerCoefs.x, 2) + Math.pow(aim.y - centerCoefs.y, 2)) > threshold) {
                    float dx = aim.x - centerCoefs.x;
                    float dy = aim.y - centerCoefs.y;
                    centerCoefs.set(centerCoefs.x + dx * 0.15f, centerCoefs.y + dy * 0.15f);
                    draw.redraw();
                    sleep(10);
                }
                centerCoefs.set(aim.x, aim.y);
                movingThread = null;
                draw.redraw(getTouchRect());
            }
            catch (Exception e){
                Logger.log(e);
            }
        }
        @SuppressWarnings("SameParameterValue")
        private void sleep(int ms){
            try {
                Thread.sleep(ms);
            }
            catch (Exception ignored){}
        }
    }
    class CircleWithImage{

        protected Integer image = null;
        protected Bitmap bitmap = null;
        protected Integer color = null;
        protected PointF centerCoefs = new PointF(-1,-1);
        protected float radius;
        private final RectF rect = new RectF(0,0,0,0);
        private final RectF touchRect = new RectF(0,0,0,0);
        protected Thread loadingThread = null;
        protected Paint circlepaint = new Paint();
        protected Paint imagepaint = new Paint();
        protected boolean pressed = false;
        protected boolean visible = true;
        protected float touchRectCoef = 1.5f;
        Path squircleCache = null;
        float squircleCacheSum = 0;
        Path squircle1Cache = null;
        float squircle1CacheSum = 0;

        public CircleWithImage(float cx, float cy, float radius) {
            setCenter(cx, cy, draw.getWidth(), draw.getHeight());
            this.radius = radius;
            circlepaint.setAntiAlias(Build.VERSION.SDK_INT > 14);
        }
        public void setImage(int image){
            if(this.image == null || image != this.image || bitmap == null) {
                this.image = image;
                bitmap = null;
            }
        }
        public void setColor(int color){
            if(this.color == null || this.color != color) {
                this.color = color;
                bitmap = null;
            }
        }
        private boolean isCircle(){
            if(color != null && getClass() == ClickableMovableAnimatedCircleWithImage.class)
                return true;
            return Data.isCircle();
        }
        public void draw(Canvas canvas){
            try {
                if (!visible)
                    return;

                if (color != null) {      //-----color
                    circlepaint.setStyle(Paint.Style.STROKE);
                    circlepaint.setStrokeWidth(radius*0.015f);
                    circlepaint.setColor(MainMenu.transparentBackgroundColor);
                    if(isCircle())
                        canvas.drawCircle(getCenterX(), getCenterY(), radius+1f, circlepaint);
                    else if(Data.isSqircle()) { //2 out of 3
                        if(squircle1Cache == null || squircle1CacheSum != getSquircleCenterSum(getCenterX(), getCenterY(), radius + 1f) ){
                            squircle1Cache = getSquircleCenterPath(getCenterX(), getCenterY(), radius + 1f);
                            squircle1CacheSum = getSquircleCenterSum(getCenterX(), getCenterY(), radius + 1f);
                        }
                        canvas.drawPath(squircle1Cache, circlepaint);
                    }
                    else if(Data.isRect())
                        canvas.drawPath(Tools.getRectCenterPath(getCenterX(), getCenterY(), radius+1f), circlepaint);
                    circlepaint.setColor(color);
                } else {                      //------------background
                    circlepaint.setStyle(Paint.Style.STROKE);
                    circlepaint.setStrokeWidth(radius*0.015f);
                    circlepaint.setColor(Color.argb(100, 255, 255, 255));
                    if(isCircle())
                        canvas.drawCircle(getCenterX(), getCenterY(), radius, circlepaint);
                    else if(Data.isSqircle()) { //2 out of 3
                        if(squircleCache == null || squircleCacheSum != getSquircleCenterSum(getCenterX(), getCenterY(), radius) ){
                            squircleCache = getSquircleCenterPath(getCenterX(), getCenterY(), radius);
                            squircleCacheSum = getSquircleCenterSum(getCenterX(), getCenterY(), radius);
                        }
                        canvas.drawPath(squircleCache, circlepaint);
                    }
                    else if(Data.isRect())
                        canvas.drawPath(Tools.getRectCenterPath(getCenterX(), getCenterY(), radius), circlepaint);
                    if (pressed)
                        circlepaint.setColor(Color.argb(200, 0, 0, 0));
                    else
                        circlepaint.setColor(Color.argb(100, 0, 0, 0));
                }
                circlepaint.setStyle(Paint.Style.FILL);
                if(isCircle())
                    canvas.drawCircle(getCenterX(), getCenterY(), radius, circlepaint);
                else if(Data.isSqircle()) { //2 out of 3
                    if(squircleCache == null || squircleCacheSum != getSquircleCenterSum(getCenterX(), getCenterY(), radius) ){
                        squircleCache = getSquircleCenterPath(getCenterX(), getCenterY(), radius);
                        squircleCacheSum = getSquircleCenterSum(getCenterX(), getCenterY(), radius);
                    }
                    canvas.drawPath(squircleCache, circlepaint);
                }
                else if(Data.isRect())
                    canvas.drawPath(Tools.getRectCenterPath(getCenterX(), getCenterY(), radius), circlepaint);

                if (bitmap == null) {                      //------------bitmap
                    loadImage();
                } else {
                    float imageX = getCenterX() - bitmap.getWidth() / 2f;
                    float imageY = getCenterY() - bitmap.getHeight() / 2f;
                    if(color != null)
                        imagepaint.setAlpha(80);
                    else
                        imagepaint.setAlpha(255);
                    canvas.drawBitmap(bitmap, imageX, imageY, imagepaint);
                }
            }
            catch (Exception e){
                e.printStackTrace();
                Logger.log("OnScreenInstrumentsList.CircleWithImage.draw", "Ошибка обработки отрисовки: " + e.getMessage(), false);
            }
        }
        public void setCenter(float cx, float cy, float boundsX, float boundsY){
            centerCoefs.set(cx/boundsX, cy/boundsY);
            draw.redraw(getTouchRect());
        }
        public RectF getRect() {
            rect.set(getCenterX() - radius, getCenterY() - radius, getCenterX() + radius, getCenterY() + radius);
            return rect;
        }
        public RectF getTouchRect() {
            touchRect.set(getCenterX() - radius*touchRectCoef, getCenterY() - radius*touchRectCoef, getCenterX() + radius*touchRectCoef, getCenterY() + radius*touchRectCoef);
            return touchRect;
        }
        public void setRadius(float radius) {
            this.radius = radius;
        }
        public float getRadius(){
            return radius;
        }
        public void setVisible(boolean visible){
            this.visible = visible;
            draw.redraw();
        }
        public void setTouchRectCoef(float newCoef){
            touchRectCoef = newCoef;
        }
        public float getCenterX(){
            float boundsX = draw.getWidth();
            float result = boundsX * centerCoefs.x;
//            Logger.log("lastBounds.x="+lastBounds.x);
            //Logger.log("centerCoefs.x="+centerCoefs.x);
//            Logger.log("getCenterX="+result);
            if(result < radius*1.3f)
                result = radius*1.3f;
            if(result > boundsX-radius*1.3f)
                result = boundsX-radius*1.3f;
            return result;
        }
        public float getCenterY(){
            float boundsY = draw.getHeight();
            float result = boundsY * centerCoefs.y;
            //Logger.log("centerCoefs.y="+centerCoefs.y);
            //Logger.log("getCenterY="+result);
            if(result < radius*1.3f)
                result = radius*1.3f;
            if(result > boundsY-radius*1.3f)
                result = boundsY-radius*1.3f;
            return result;
        }
        public Point getCenter(){
            return new Point(getCenterX(), getCenterY());
        }

        private void loadImage(){
            if(bitmap == null && loadingThread == null && image != null){
                loadingThread = new Thread(() -> {
                    loadImageAsync();
                    loadingThread = null;
                });
                loadingThread.setName("loadingThread for CircleWithImage");
                loadingThread.start();
            }
        }
        private void loadImageAsync(){
            try {
                float sc = 0.55f;
                float imageSize = 2*(radius*sc);
                //RectF imageRect = new RectF(getCenterX() - radius*sc, getCenterY() - radius*sc, getCenterX() + radius*sc, getCenterY() + radius*sc);
                bitmap = Tools.decodeResource(draw.context.getResources(), image, (int)imageSize, (int)imageSize);
                if(color != null && bitmap != null)
                    bitmap = Tools.getBitmapContour(bitmap, Data.tools.getGridColor(color, 0.7f));
                draw.redraw();
            }catch (Exception e){
                Logger.log("Draw.OnScreenInstrumentList.loadImageAsync", "Exception: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            } catch (OutOfMemoryError e) {
                Logger.log("Draw.OnScreenInstrumentList.loadImageAsync", "OutOfMemoryError: "+ e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            }

        }
    }
}
