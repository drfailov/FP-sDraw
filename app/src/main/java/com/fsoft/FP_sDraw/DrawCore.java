package com.fsoft.FP_sDraw;

import static com.fsoft.FP_sDraw.common.Tools.setNavBarForeground;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.*;

import com.fsoft.FP_sDraw.common.*;
import com.fsoft.FP_sDraw.instruments.*;

import java.util.*;

/**
 * This is main canvas
 * Created by Dr. Failov on 08.06.2014.
 */
public class DrawCore{
    //buffers for save data between rotating
    private static Bitmap rotatingImageBuffer = null;
    private static Integer rotatingPositionBuffer = null;
    private static String rotatingInstrumentBuffer = null;
    //interfaces
    public interface OnRedrawListener{
        void redraw();
        void redraw(int top, int bottom, int left, int right);
    }
    private final ArrayList<OnRedrawListener> redrawListeners = new ArrayList<>();
    //constants
    static public final int MULTITOUCH_MAX = 10;
    public static final Object drawSync = new Object();
    //variables
    public Bitmap bitmap;
    public OrientationProvider orientationProvider;
    public MainActivity context;
    public View view;
    public Canvas canvas;
    //instruments
    public Main main;
    public HoverPointer hoverPointer;
    public Instrument brush;
    public Instrument fill;
    public Instrument erase;
    public Instrument mosaic;
    public Accurate accurate;
    public Instrument gesture;
    public Text text;
    public Instrument pipette;
    public Instrument figures;
    public Selection selectandmove;
    public OnScreenInstrumentsList onScreenInstrumentsList;
    public OnScreenInputDeviceSelectorMenu onScreenInputDeviceSelectorMenu;
    public OnScreenMenuButton onScreenMenuButton;
    public Scale scale;
    public Instrument line;
    public Instrument currentInstrument = null;
    public final ArrayList<Instrument> instruments = new ArrayList<>(); //selectable modules, can handle events only when selected
    private final ArrayList<Instrument> providers = new ArrayList<>(); //modules can handle all available events all the time
    private final ArrayList<Instrument> controls = new ArrayList<>(); //fuck... The same shit)))
    private boolean fingersOnScreen = false;
    public long lastChangeToBitmap = 0;
    public boolean fingersOnScreen(){
        return fingersOnScreen;
    }
    //providers
    public AreaCalculator undoAreaCalculator = new AreaCalculator();
    public UndoProvider undoProvider;
    public Saver saver;


    public Handler uiHandler = new Handler();
    public DrawCore(MainActivity activity, View view) {
        this.view = view;
        this.context = activity;
        undoProvider=new UndoProvider(this);
        orientationProvider = new OrientationProvider(context);
        saver = new Saver(this);
    }
    public boolean processEvent(MotionEvent event){
        //Logger.log("CORE RECEIVED EVENT " + event);
        //учёт количества пальцев на экране для предотвращения проблем с отменой
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        if(action == MotionEvent.ACTION_DOWN)
            fingersOnScreen = true;
        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
            fingersOnScreen = false;

        for (int i = 0; i < controls.size(); i++)
            if (controls.get(i).onTouch(event))
                return true;
        for (int i = 0; i < providers.size(); i++)
            if (providers.get(i).onTouch(event))
                return true;
        if (currentInstrument != null)
            currentInstrument.onTouch(event);
        //bitmap.prepareToDraw();
        return true;
    }
    public void sizeChanged(int w, int h){
        try {
            if(bitmap != null && bitmap.getHeight() == h && bitmap.getWidth() == w) {
                redraw();
                return;
            }
            Logger.log("Size Changed:   " + w + "    " + h);
            Bitmap oldBitmap = bitmap;
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(/*Bitmap.createBitmap*/(bitmap));
            if(oldBitmap != null && orientationProvider.isEmpty()) {
                Logger.log("Восстановление рисунка после изменения размера...");
                canvas.drawBitmap(oldBitmap, 0, 0, new Paint());
            }

            if(oldBitmap == null) {
                //DEFINE INSTRUMENTS AND PROVIDERS
                instruments.clear();
                instruments.add(brush = new Brush(this));
                instruments.add(erase = new Eraser(this));
                instruments.add(fill = new Filler(this));
                instruments.add(mosaic = new Mosaic(this));
                instruments.add(text = new Text(this));
                instruments.add(selectandmove = new Selection(this));
                instruments.add(figures = new Figures(this));
                instruments.add(line = new Line(this));
                instruments.add(scale = new Scale(this));
                instruments.add(pipette = new Pipette(this));
                instruments.add(accurate = new Accurate(this));
                instruments.add(gesture = new Gesture(this));
                providers.clear();
                providers.add(main = new Main(this));
                providers.add(new DebugDataProvider(this));
                providers.add(hoverPointer =new HoverPointer(this));
                controls.clear();
                controls.add(onScreenInstrumentsList = new OnScreenInstrumentsList(this));
                controls.add(onScreenMenuButton = new OnScreenMenuButton(this));
                controls.add(onScreenInputDeviceSelectorMenu = new OnScreenInputDeviceSelectorMenu(this));
                uiHandler.post(() -> context.refreshBottomToolbar());
            }

            orientationProvider.afterRotate();

            if (currentInstrument == null)
                setInstrument(brush);

            undoProvider.prepare();
            lastChangeToBitmap = System.currentTimeMillis();

            refresh();
            TimeProvider.finish("start");

            redraw();
        }
        catch (Exception e){
            Logger.log("Error in OnSizeChanged: " + Tools.getStackTrace(e));
        }
        catch (OutOfMemoryError e){
            Logger.show("Недостаточно памяти. Попробуйте перезапустить программу.");
        }
    }
    public boolean processKeyEvent(KeyEvent event) {
        boolean processed = false;
        for(int i=0; i<providers.size(); i++)
            processed = processed || providers.get(i).onKey(event);
        if(currentInstrument != null)
            processed = processed || currentInstrument.onKey(event);
        return processed;
    }
    public void addRedrawListener(OnRedrawListener onRedrawListener){
        redrawListeners.add(onRedrawListener);
    }
    public void addProvider(Instrument instrument){
        providers.add(instrument);
    }
    /*Invalidate view. Can be called from thread, because command forwarded to handler*/
    public void redraw(){
        uiHandler.post(() -> {
            for(OnRedrawListener redrawListener:redrawListeners)
                redrawListener.redraw();
        });
    }
    public void redraw(RectF rectF){
        redraw((int)rectF.top, (int)rectF.bottom, (int)rectF.left, (int)rectF.right);
    }
    public void redraw(Rect rectF){
        redraw(rectF.top, rectF.bottom, rectF.left, rectF.right);
    }
    public void redraw(final int _top, final int _bottom, final int _left, final int _right){
        uiHandler.post(() -> {
            try {
                for(OnRedrawListener redrawListener:redrawListeners) {
                    int top = _top;
                    int bottom = _bottom;
                    int left = _left;
                    int right = _right;
                    if(bitmap != null) {
                        if (top < 0)
                            top = 0;
                        if (bottom < top)
                            bottom = top;
                        if (bottom >= bitmap.getHeight())
                            bottom = bitmap.getHeight() -1;

                        if (left < 0)
                            left = 0;
                        if (right < left)
                            right = left;
                        if (right >= bitmap.getWidth())
                            right = bitmap.getWidth() -1;
                    }
                    redrawListener.redraw(top, bottom, left, right);
                }
            }catch (Exception e){
                Logger.log("Error in redraw(...): " + Tools.getStackTrace(e));
            }
        });
    }
    public void drawContent(Canvas canvas){
        //noinspection CommentedOutCode
        synchronized (drawSync) {
            if(canvas == null)
                return;
            for (int i = 0; i < providers.size(); i++)
                providers.get(i).onCanvasDraw(canvas, true);
            if (currentInstrument != null)
                currentInstrument.onCanvasDraw(canvas, true);
            for (int i = controls.size()-1; i >= 0; i--)
                controls.get(i).onCanvasDraw(canvas, true);
            //debug of undo functions
//            if(undoProvider.image[0] != null)
//                canvas.drawBitmap(undoProvider.image[0], 0, 0, new Paint());
        }
    }
    public ArrayList<RectF> getNoGoZones(){
        ArrayList<RectF> arrayList = new ArrayList<>();
        if(onScreenMenuButton != null && onScreenMenuButton.enabled)
            arrayList.add(onScreenMenuButton.menuButtonRectTouch());
        if(onScreenInstrumentsList != null && onScreenInstrumentsList.circleInstruments != null && onScreenInstrumentsList.circleInstruments.isEnabled())
            arrayList.add(onScreenInstrumentsList.circleInstruments.getTouchRect());
        if(onScreenInstrumentsList != null && onScreenInstrumentsList.colorsCircle != null && onScreenInstrumentsList.colorsCircle.isEnabled())
            arrayList.add(onScreenInstrumentsList.colorsCircle.getTouchRect());
        if(onScreenInputDeviceSelectorMenu != null && onScreenInputDeviceSelectorMenu.floatingMenuForSelectingInputDevice.enabled)
            arrayList.add(onScreenInputDeviceSelectorMenu.floatingMenuForSelectingInputDevice.position());
        if(scale != null && scale.scale_size != 1)
            arrayList.add(scale.scale_hint_frameRect);
        return arrayList;
    }
    public void refresh(){
        try {
            MainActivity mainActivity = context;
            if(mainActivity.bottomPalette != null) {
                mainActivity.bottomPalette.setBackgroundColor((Integer) Data.get(Data.backgroundColorInt()));
                mainActivity.bottomPalette.refresh();
            }
            if(mainActivity.bottomToolbar != null) {
                mainActivity.bottomToolbar.setBackgroundColor((Integer) Data.get(Data.backgroundColorInt()));
                mainActivity.bottomToolbar.refresh();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.getWindow().setStatusBarColor((Integer) Data.get(Data.backgroundColorInt()));
                if(mainActivity.mainMenu != null && mainActivity.mainMenu.isShowing())
                    context.getWindow().setNavigationBarColor(Color.rgb(39, 50, 56));
                else {
                    int backgroundColor = (Integer) Data.get(Data.backgroundColorInt());
                    context.getWindow().setNavigationBarColor(backgroundColor);
                    setNavBarForeground(!Tools.isLightColor(backgroundColor), context, view);
                }
            }
        }
        catch (Exception e){
            Logger.log(e);
        }
        for(int i=0; i<providers.size(); i++)
            providers.get(i).onSelect();
        for(int i=0; i<controls.size(); i++)
            controls.get(i).onSelect();
        if(currentInstrument != null)
            currentInstrument.onSelect();
        redraw();
        if(bitmap != null)
            doScheduled();
    }
    public void pause(){
        if(scale != null && scale.scale_size > scale.pixelModeThreshold){
            scale.immediatelyDisablePixelMode();
        }

        for(int i=0; i<providers.size(); i++)
            providers.get(i).onDeselect();
        for(int i=0; i<controls.size(); i++)
            controls.get(i).onDeselect();
        if(currentInstrument != null)
            currentInstrument.onDeselect();
    }
    public void clear(){
        try {
            saver.autoSave();
            if ((Boolean) Data.get(Data.einkClean())) {
                Logger.log("Draw.clear", "Очистка рисунка на E-ink экране...", false);
                bitmap.eraseColor(Color.TRANSPARENT);
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        uiHandler.post(() -> {
                            try {
                                bitmap.eraseColor(Color.BLACK);
                                redraw();
                            } catch (Exception e) {
                                Logger.log("Error in clearEink(...): " + Tools.getStackTrace(e));
                            }
                        });
                        Thread.sleep(500);
                        uiHandler.post(() -> {
                            try {
                                bitmap.eraseColor(Color.WHITE);
                                redraw();
                            } catch (Exception e) {
                                Logger.log("Error in clearEink(...): " + Tools.getStackTrace(e));
                            }
                        });
                        Thread.sleep(500);
                        uiHandler.post(() -> {
                            try {
                                bitmap.eraseColor(Color.BLACK);
                                redraw();
                            } catch (Exception e) {
                                Logger.log("Error in clearEink(...): " + Tools.getStackTrace(e));
                            }
                        });
                        Thread.sleep(500);
                        uiHandler.post(() -> {
                            try {
                                bitmap.eraseColor(Color.WHITE);
                                redraw();
                            } catch (Exception e) {
                                Logger.log("Error in clearEink(...): " + Tools.getStackTrace(e));
                            }
                        });
                        Thread.sleep(500);
                        uiHandler.post(() -> {
                            try {
                                bitmap.eraseColor(Color.TRANSPARENT);
                                Logger.log("Draw.clear", "Рисунок на e-ink очищен", false);
                                undoProvider.apply(0, bitmap.getHeight(), 0, bitmap.getWidth());
                                undoProvider.prepare();
                                lastChangeToBitmap = System.currentTimeMillis();
                                redraw();
                            } catch (Exception e) {
                                Logger.log("Error in clearEink(...): " + Tools.getStackTrace(e));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                try {
                    bitmap.eraseColor(Color.TRANSPARENT);
                    Logger.log("Draw.clear", "Рисунок очищен", false);
                    undoProvider.apply(0, bitmap.getHeight(), 0, bitmap.getWidth());
                    undoProvider.prepare();
                    lastChangeToBitmap = System.currentTimeMillis();
                    redraw();
                } catch (Exception e) {
                    Logger.log("Draw.clear", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                } catch (OutOfMemoryError e) {
                    Logger.log("Draw.clear", "OutOfMemoryError: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                    undoProvider.freeUp();
                }
            }
        }
        catch (Exception e){
            Logger.log(e);
        }
    }
    public Bitmap createFullBitmap(){
        int tries = 0;
        while(tries < 5) {
            try {
                Bitmap tmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                tries++;

                Canvas canvas = new Canvas(tmp);
                float oldScale = scale.scale_size;
                float oldOffsetX = scale.scale_offset_x;
                float oldOffsetY = scale.scale_offset_y;
                scale.scale_size = 1.0f;
                scale.scale_offset_x = 0;
                scale.scale_offset_y = 0;

                main.onCanvasDraw(canvas, true);

                scale.scale_size = oldScale;
                scale.scale_offset_x = oldOffsetX;
                scale.scale_offset_y = oldOffsetY;

                return tmp;
            } catch (Exception | OutOfMemoryError e) {
                Logger.show("Ошибка обращения к рисунку");
                Logger.log("Error while copying bitmap");
                if(!undoProvider.freeUp())
                    break;
            }
        }
        Logger.show(context.getString(R.string.errorWhileInserting));
        return null;
    }
    public Bitmap createFullTransparentBitmap(){
        int tries = 0;
        while(tries < 5) {
            try {
                tries++;
                Bitmap tmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(tmp);
                main.drawTransparent(canvas);

                return tmp;
            } catch (Exception | OutOfMemoryError e) {
                Logger.show("Ошибка обращения к рисунку");
                Logger.log("Error while copying bitmap");
                if(!undoProvider.freeUp())
                    break;
            }
        }
        Logger.show(context.getString(R.string.errorWhileInserting));
        return null;
    }
    public Instrument getInstrument(String name){
        Instrument result = null;
        for(int i=0; i<instruments.size(); i++) {
            if(instruments.get(i).getName().equals(name)) {
                result = instruments.get(i);
                break;
            }
        }
        return result;
    }
    public void setInstrument(String name){
        if(currentInstrument != null && currentInstrument.getName().equals(name))
            return;
        Instrument next = getInstrument(name);
        if(next != null) {
            if(currentInstrument != null)
                currentInstrument.onDeselect();
            currentInstrument = next;
            refresh();
            //currentInstrument.onSelect();
            Logger.log("Set instrument " + name + ": ok.");
        }else{
            Logger.log("Set instrument " + name + ": fail.");
        }
        //invalidate();
        redraw();
    }
    public void setInstrument(Instrument next){
        if(currentInstrument == next)
            return;
        if(next != null) {
            if(currentInstrument != null)
                currentInstrument.onDeselect();
            currentInstrument = next;
            refresh();
            //currentInstrument.onSelect();
            Logger.log("Set instrument " + next.getName() + ": ok.");
        }else{
            Logger.log("Set instrument NULL fail.");
        }
        redraw();
        //invalidate();
    }
    public int getWidth(){
        return bitmap.getWidth();
    }
    public int getHeight(){
        return bitmap.getHeight();
    }
    public void playSoundEffect(int i){
        view.playSoundEffect(i);
    }
    public void post(Runnable runnable){
        uiHandler.post(runnable);
    }
    public boolean isEmpty(){
    /*
    проверяет пустой ли холст
    (сравнивается с 0 пикселем)
     */
        try{
            boolean result_empty=true;
            if(bitmap != null) {
                float step = Data.store().DPI / 20;
                int zero = bitmap.getPixel(0, 0);
                for(int i=0; i<bitmap.getWidth(); i += step)//x
                    for(int j=0; j<bitmap.getHeight(); j += step)//y
                        if(bitmap.getPixel(i, j) != zero){
                            result_empty=false;
                            break;
                        }
                Logger.log("Draw.isEmpty", "Проверка: является ли рисунок пустым? - " + (result_empty ? "да" : "нет"), false);
            }
            else {
                Logger.log("Draw.isEmpty", "Проверка: является ли рисунок пустым? - рисунка нет...", false);
            }
            return result_empty;
        }catch (Exception e){
            Logger.log("Draw.isEmpty", "Exception: "+e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }catch (OutOfMemoryError e){
            Logger.log("Draw.isEmpty", "OutOfMemoryError: "+e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return  false;
    }
    public void fuckIt(){
        if(saver != null) {
            long start = new Date().getTime();
            if(saver.savingInProgress || saver.fileCompressorInProgress)
                Logger.log("Waiting while saving complete...");
            while (saver.savingInProgress || saver.fileCompressorInProgress){
                Tools.sleep(1);
            }
            long end = new Date().getTime();
            if(start != end)
                Logger.log("Waiting took " + (end-start) + "ms");
        }
        //variables
        orientationProvider=null;
        context=null;
            bitmap=null;
        canvas=null;
        //instruments
        main=null;
        brush=null;
        fill=null;
        erase=null;
        accurate=null;
        gesture=null;
        text=null;
        pipette=null;
        figures=null;
        scale=null;
        line=null;
        currentInstrument = null;
        instruments.clear();
        providers.clear();
        controls.clear();
        redrawListeners.clear();
        //providers
        undoAreaCalculator=null;
        undoProvider=null;
        saver=null;
    }
    //Scheduling tasks
    // Этот механизм предусмотрен для того, чтобы пожно было создавать события,
    // которые должны быть выполнены на этапе когда программа полностью загружена.
    private final ArrayList<Runnable> scheduledTasks= new ArrayList<>();
    private void doScheduled(){
        while(scheduledTasks.size() > 0){
            Runnable task = scheduledTasks.get(scheduledTasks.size()-1);
            scheduledTasks.remove(task);
            try {
                task.run();
            }
            catch (Exception e){
                Logger.log("Error executing scheduled: " + Tools.getStackTrace(e));
            }
        }
    }
    public void schedule(Runnable r){
        scheduledTasks.add(r);
    }
    //Providers
    public class OrientationProvider{
        static public final int ORIENTATION_HORIZONTAL = 1;
        static public final int ORIENTATION_VERTICAL = 2;
        static public final int ORIENTATION_AUTO = 3;
        Activity context;

        OrientationProvider(Activity c){
            context = c;
            if(rotatingPositionBuffer == null)
                rotatingPositionBuffer = getScreenRotation();
        }
        @SuppressWarnings("deprecation")
        @SuppressLint("ObsoleteSdkInt")
        public int getScreenRotation() /*0 90 180 270*/ {
            if(Build.VERSION.SDK_INT<8)
                return 0;
            try{
                int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
                DisplayMetrics dm = new DisplayMetrics();
                context.getWindowManager().getDefaultDisplay().getMetrics(dm);
                int width = dm.widthPixels;
                int height = dm.heightPixels;
                int orientation;
                // if the device's natural orientation is portrait:
                if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width ||
                        (rotation == Surface.ROTATION_90  || rotation == Surface.ROTATION_270) && width > height) {
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            orientation = 0;
                            break;
                        case Surface.ROTATION_90:
                            orientation = 90;
                            break;
                        case Surface.ROTATION_180:
                            orientation = 180;
                            break;
                        case Surface.ROTATION_270:
                            orientation = 270;
                            break;
                        default:
                            Logger.log("", "Unknown screen orientation. Defaulting to " +
                                    "portrait.", false);
                            orientation = 0;
                            break;
                    }
                }
                // if the device's natural orientation is landscape or if the device
                // is square:
                else {
                    switch(rotation) {
                        case Surface.ROTATION_0:
                            orientation = 0;//ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                            break;
                        case Surface.ROTATION_90:
                            orientation = 90;//ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                            break;
                        case Surface.ROTATION_180:
                            orientation = 180;// ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                            break;
                        case Surface.ROTATION_270:
                            orientation = 270;//ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                            break;
                        default:
                            Logger.log("", "Unknown screen orientation. Defaulting to landscape.", false);
                            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                            break;
                    }
                }
                Logger.log("Orientation = " + orientation);
                return orientation;
            }catch (Exception|OutOfMemoryError e){
                Logger.log(e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
            return 0;
        }
        public int getScreenOrientation(){
            return Tools.getScreenOrientation(context);
        }
        public boolean isEmpty(){
            return rotatingImageBuffer == null;
        }
        void beforeRotate(){
            rotatingImageBuffer = bitmap;
            if(currentInstrument != null)
                rotatingInstrumentBuffer = currentInstrument.getName();
        }
        void afterRotate(){
            if(rotatingImageBuffer != null) {
                //show message
                Logger.log("Last = " + rotatingPositionBuffer + "; now = " + getScreenRotation());
                int oldMul=rotatingImageBuffer.getWidth() * rotatingImageBuffer.getHeight();
                int newMul=bitmap.getWidth() * bitmap.getHeight();
//                if(newMul != oldMul && Data.isTutor("rotate", 8)){
//                    Toast.makeText(context, Data.tools.getResource(R.string.RotateHelp1), Toast.LENGTH_SHORT).show();
//                    Toast.makeText(context, Data.tools.getResource(R.string.RotateHelp2), Toast.LENGTH_LONG).show();
//                }
                //get angle
                int angle;
                int orientation = getScreenRotation();
                if(rotatingPositionBuffer == 270 && orientation == 0)
                    angle = -90;
                else if(rotatingPositionBuffer == 0 && orientation == 270)
                    angle = 90;
                else
                    angle = rotatingPositionBuffer - orientation;
                //create matrix
                Matrix matrix = new Matrix();
                if(angle == -90){
                    matrix.preRotate(angle);
                    //matrix.postTranslate(0, bitmap.getHeight());
                    matrix.postTranslate(0, rotatingImageBuffer.getWidth());
                    //matrix.postScale(bitmap.getWidth()/(float)rotatingImageBuffer.getHeight(), bitmap.getHeight()/(float)rotatingImageBuffer.getWidth());
                }
                else if(angle == 90){
                    matrix.preRotate(angle);
                    matrix.postTranslate(bitmap.getWidth(), 0);
                    //matrix.postTranslate(rotatingImageBuffer.getHeight(), 0);
                    //matrix.postScale(bitmap.getWidth()/(float)rotatingImageBuffer.getHeight(), bitmap.getHeight()/(float)rotatingImageBuffer.getWidth());
                }
                //draw
                canvas.drawBitmap(rotatingImageBuffer, matrix, new Paint());
                rotatingImageBuffer=null;
                rotatingPositionBuffer = getScreenRotation();
            }
            if(rotatingInstrumentBuffer != null){
                setInstrument(rotatingInstrumentBuffer);
            }
        }
    }
}

