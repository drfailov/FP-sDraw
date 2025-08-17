package com.fsoft.FP_sDraw.instruments;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.fsoft.FP_sDraw.*;
import com.fsoft.FP_sDraw.common.*;
import com.fsoft.FP_sDraw.menu.DialogLoading;
import com.fsoft.FP_sDraw.menu.HintMenu;

import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 17.01.2015.
 */
public class Filler implements Instrument{
    static boolean libraryLoaded = false;
    static { // load the native library "nativeFill"
        try {
            System.loadLibrary("nativeFill");
            libraryLoaded = true;
        }
        catch (Error e){
            libraryLoaded = false;
        }
    }

    boolean fillingInProgress = false; //для предотвращения запуска заливки в нескольких потоках
    final DrawCore draw;
    int color = 0;
    private HintMenu hint = null;

    public Filler(DrawCore d){
        draw = d;
    }
    @Override  public String getName() {
        return "fill";
    }
    @Override public String getVisibleName() {
        return Data.tools.getResource(R.string.instrumentFilling);
    }
    @Override public int getImageResourceID() {
        return R.drawable.menu_fill;
    }
    @Override public void onSelect() {
        color = Data.getBrushColor();//(Integer)Data.get(Data.brushColorInt());
    }
    @Override public void onDeselect() {

    }
    @Override public boolean onTouch(MotionEvent event) {
        //trigger zoom if over 2 fingers
        if(event.getPointerCount() > 1){// && (Boolean)Data.get(Data.twoFingersZoomBoolean())){
            if(Data.tools.isFullVersion()) {
                draw.setInstrument(draw.scale);
                draw.scale.onTouch(event);
                draw.scale.setInstrumentCallback(draw.fill);
            }
            else
                Data.tools.showBuyFullDialog();
            return true;
        }

        //process hint events
        if(hint != null && hint.processTouch(event))
            return true;

        //move canvas if touched by finger but activated  sPen mode
        if(!Data.tools.isAllowedDevice(event)) {
            if(Data.tools.isFullVersion()) {
                draw.setInstrument(draw.scale);
                draw.scale.onTouch(event);
                draw.scale.setInstrumentCallback(draw.fill);
            }
            return true;
        }

        event = draw.scale.scale_transformMotionEvent(event);
        if(!fillingInProgress
                && event.getX() < draw.bitmap.getWidth()
                && event.getY() < draw.bitmap.getHeight()
                &&  event.getX() > 0
                && event.getY() > 0) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                int x=(int)event.getX(0);
                int y=(int)event.getY(0);
                int oldColor = draw.bitmap.getPixel(x, y);
                Logger.log("OldColor:    " + Integer.toHexString(oldColor));
                Logger.log("colorToFill: " + Integer.toHexString(color));
                if((color) != oldColor){
                    if(event.getPointerCount() == 1){
                        if(libraryLoaded && Build.VERSION.SDK_INT > 8) {
                            Filler.NativeFillerStackPixels filler = new Filler.NativeFillerStackPixels(draw.bitmap, x, y,
                                    //Tools.removeTransparency(color),
                                    (color),
                                    draw.context,
                                    Data.tools.getResource(R.string.saveMenuCancel),
                                    Data.tools.getResource(R.string.instrumentFilling));
                            filler.execute();
                        }
                        else {
                            Logger.log("As native LIBRARY WASN'T LOADED, using java code.");
                            Filler.FillerRecursivePixels filler=new Filler.FillerRecursivePixels(draw.bitmap, x, y,
                                    Tools.removeTransparency(color), draw.context,
                                    Data.tools.getResource(R.string.saveMenuCancel),
                                    Data.tools.getResource(R.string.instrumentFilling));
                            filler.execute();
                        }
                        fillingInProgress = true;
                    }
                    if(event.getPointerCount() == 2){
                        Filler.FillerRecursivePixels filler=new Filler.FillerRecursivePixels(draw.bitmap, x, y,
                                Tools.removeTransparency(color), draw.context,
                                Data.tools.getResource(R.string.saveMenuCancel),
                                Data.tools.getResource(R.string.instrumentFilling));
                        filler.execute();
                        fillingInProgress = true;
                    }
                    if(event.getPointerCount() == 3){
                        Filler.FillerRecursiveStackPixels filler=new Filler.FillerRecursiveStackPixels(draw.bitmap, x, y,
                                Tools.removeTransparency(color), draw.context,
                                Data.tools.getResource(R.string.saveMenuCancel),
                                Data.tools.getResource(R.string.instrumentFilling));
                        filler.execute();
                        fillingInProgress = true;
                    }
                    if(event.getPointerCount() == 4){
                        Filler.FillerMathWave filler=new Filler.FillerMathWave(draw.bitmap, x, y,
                                Tools.removeTransparency(color), draw.context,
                                Data.tools.getResource(R.string.saveMenuCancel),
                                Data.tools.getResource(R.string.instrumentFilling));
                        filler.execute();
                        fillingInProgress = true;
                    }
                    if(event.getPointerCount() == 5){
                        Filler.FillerRecursiveLines filler=new Filler.FillerRecursiveLines(draw.bitmap, x, y,
                                Tools.removeTransparency(color), draw.context,
                                Data.tools.getResource(R.string.saveMenuCancel),
                                Data.tools.getResource(R.string.instrumentFilling));
                        filler.execute(new Point(x,y));
                        fillingInProgress = true;
                    }
                }
            }
        }
        return true;
    }
    @Override public boolean onKey(KeyEvent event) {
        return false;

    }
    @Override public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(drawUi) {
            if (hint == null)
                hint = new HintMenu(draw, Data.tools.getResource(R.string.hint_filler), R.drawable.ic_help, "FILL", HintMenu.SHOW_TIMES);
            hint.draw(canvas);
        }
    }
    @Override public boolean isActive() {
        if(draw == null)
            return false;
        if(draw.currentInstrument == this)
            return true;
        return draw.currentInstrument == draw.scale && draw.scale != null && draw.scale.instrumentCallback == this;
    }
    @Override  public boolean isVisibleToUser() {
        return true;
    }
    @Override public View.OnClickListener getOnClickListener() {
        return view -> draw.setInstrument(getName());
    }

    class FillerMathWave {
        DialogLoading dialogLoading = null;
        final Object visualizationSync = new Object();
        Handler handler = new Handler();
        Timer refreshTimer = null;
        boolean cont=true;
        int[][] mask;
        int[] buf;
        int height;
        int width;
        int startX;
        int startY;
        Bitmap bitmap;
        int color;
        Context dialogContext;
        String dialogCancelButtonText;
        String dialogText;
        LinkedList<IntPoint> queue;
        boolean[][] queued;
        int counter=0;

        FillerMathWave (Bitmap _bitmap, int _startX, int _startY, int _color, Context _dialogContext, String _dialogCancelButtonText, String _dialogText){
            bitmap = _bitmap;
            startX = _startX;
            startY = _startY;
            color = _color;
            dialogContext = _dialogContext;
            dialogCancelButtonText = _dialogCancelButtonText;
            dialogText = _dialogText;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            queue = new LinkedList<IntPoint>();
            queued = new boolean[width][height];
        }
        DialogInterface.OnClickListener getCancelListener(){
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cont = false;
                }
            };
        }
        public void execute(){
            showMessage();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doFill();
                }
            }).start();
        }
        protected void showMessage() {
            TimeProvider.start("FillerRecursivePixels");
            if(dialogContext != null && dialogText != null && dialogCancelButtonText != null) {
                dialogLoading = new DialogLoading(dialogContext, dialogCancelButtonText, getCancelListener(), dialogText + "...");
                dialogLoading.show();
            }

        }
        private void showMessage(String text){
            Logger.show(text);
        }
        protected void doFill() {
            AreaCalculator fillerArea=new AreaCalculator();
            try{
                int x=startX;
                int y=startY;
                Logger.log("Draw.handlerFill", "Filling from (" + x + " ; " + y + ")", false);
                synchronized (visualizationSync) {
                    buf=new int[width*height] ;
                    mask = new int[width][height];
                    bitmap.getPixels(buf, 0, width, 0, 0, width, height);
                }
                beginVisualization();
                //calculate mask
                fillerArea.reset();
                fillerArea.add(x, y, 0);
                fillerArea.check(width, height);
                mask[x][y]=1;
                int start_color;
                start_color = bitmap.getPixel(x,y);
                bitmap.setPixel(x, y, color);
                int added=0;
                //some iterations
                while(cont){
                    //ride across the image
                    boolean condition=counter%2==0;
                    for(    int cy = condition ? fillerArea.top : fillerArea.bottom-1;
                            (condition ? cy<fillerArea.bottom : cy>fillerArea.top) && cont;
                            cy+= condition ? 1 : -1) {
                        for(    int cx = condition ? fillerArea.left : fillerArea.right-1;
                                (condition ? cx<fillerArea.right : cx>fillerArea.left) && cont;
                                cx+= condition ? 1 : -1) {
                            //work with each pixel
                            if(mask[cx][cy]==1) { //detected
                                fillerArea.add(cx, cy, 0);
                                fillerArea.check(width, height);
                                if(cx-1 >= 0){  //valid pixel
                                    if(mask[cx-1][cy]==0){ //empty space
                                        synchronized (visualizationSync){
                                            if(buf[cx-1 + cy*width] == start_color){ //valid color
                                                mask[cx-1][cy]=1;
                                                buf[cx-1 + cy*width] =  color;
                                                added++;
                                            }
                                        }
                                    }
                                }
                                if(cx+1 < width) { //valid pixel
                                    if(mask[cx+1][cy]==0) {//empty space
                                        synchronized (visualizationSync){
                                            if(buf[cx+1 + cy*width] == start_color){ //valid color
                                                mask[cx+1][cy]=1;
                                                buf[cx+1 + cy*width] = color;
                                                added++;
                                            }
                                        }
                                    }
                                }
                                if(cy-1 >= 0){
                                    if(mask[cx][cy-1]==0) {
                                        synchronized (visualizationSync){
                                            if(buf[cx + width*(cy-1)] == start_color)  {
                                                mask[cx][cy-1]=1;
                                                buf[cx + width*(cy-1)] = color;
                                                added++;
                                            }
                                        }
                                    }
                                }
                                if(cy+1 <height) {
                                    if(mask[cx][cy+1]==0) {
                                        synchronized (visualizationSync){
                                            if(buf[cx + (cy+1)*width] == start_color)  {
                                                mask[cx][cy+1]=1;
                                                buf[cx + (cy+1)*width] = color;
                                                added++;
                                            }
                                        }
                                    }
                                }
                                mask[cx][cy]=2;
                            }
                        }
                    }
                    counter++;
                    //stop if needed
                    if(added == 0)
                        cont=false;
                    added=0;
                }
                draw.undoProvider.apply(fillerArea.top, fillerArea.bottom, fillerArea.left, fillerArea.right);
                draw.undoProvider.prepare();
                bitmap.setPixels(buf, 0, width, 0, 0, width, height);
                draw.lastChangeToBitmap = System.currentTimeMillis();
                handler.post(this::hideMessage);
            }catch (Exception | OutOfMemoryError e){
                showMessage("ошибка " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
        private void beginVisualization(){
            refreshTimer = new Timer();
            refreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (visualizationSync) {
                        bitmap.setPixels(buf, 0, width, 0, 0, width, height);
                        draw.redraw();
                        handler.post(() -> dialogLoading.setText(Data.tools.getResource(R.string.Filling) + " (" + queue.size() + ") ..."));
                    }
                }
            }, 100, 100);
        }
        protected void hideMessage() {
            try{
                draw.redraw();
                fillingInProgress = false;
                refreshTimer.cancel();
                if(dialogLoading != null)
                    dialogLoading.cancel();
                TimeProvider.finish("FillerRecursivePixels");
                draw.redraw();
            }catch (Exception | OutOfMemoryError e){
                Logger.log("ошибка "+(Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
    }
    class FillerRecursivePixels {
        DialogLoading dialogLoading = null;
        final Object visualizationSync = new Object();
        Handler handler = new Handler();
        Timer refreshTimer = null;
        AreaCalculator undoAreaCalculator = null;
        boolean cont=true;
        int[] mask;
        int height;
        int width;
        int startX;
        int startY;
        Bitmap bitmap;
        int color;
        Context dialogContext;
        String dialogCancelButtonText;
        String dialogText;
        LinkedList<IntPoint> queue;
        boolean[][] queued;
        int start_color;

        FillerRecursivePixels (Bitmap _bitmap, int _startX, int _startY, int _color, Context _dialogContext, String _dialogCancelButtonText, String _dialogText){
            bitmap = _bitmap;
            startX = _startX;
            startY = _startY;
            color = _color;
            dialogContext = _dialogContext;
            dialogCancelButtonText = _dialogCancelButtonText;
            dialogText = _dialogText;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            queue = new LinkedList<IntPoint>();
            queued = new boolean[width][height];
        }
        DialogInterface.OnClickListener getCancelListener(){
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cont = false;
                }
            };
        }
        public void execute(){
            showMessage();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doFill();
                }
            }).start();
        }
        private void beginVisualization(){
            refreshTimer = new Timer();
            refreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (visualizationSync) {
                        bitmap.setPixels(mask, 0, width, 0, 0, width, height);
                        draw.redraw();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                dialogLoading.setText(Data.tools.getResource(R.string.Filling) + " (" + queue.size() + ") ...");
                            }
                        });
                    }
                }
            }, 100, 100);
        }
        protected void showMessage() {
            TimeProvider.start("FillerRecursivePixels");
            if(dialogContext != null && dialogText != null && dialogCancelButtonText != null) {
                dialogLoading = new DialogLoading(dialogContext, dialogCancelButtonText, getCancelListener(), dialogText + "...");
                dialogLoading.show();
            }

        }
        private void showMessage(String text){
            Logger.show(text);
        }
        protected void doFill() {
            try{
                undoAreaCalculator = new AreaCalculator();
                int x=startX, y=startY;
                Logger.log("Draw.handlerFill", "Filling from ("+x+" ; "+y+")", false);
                synchronized (visualizationSync) {
                    mask=new int[width*height] ;
                    bitmap.getPixels(mask, 0, width, 0, 0, width, height);
                }
                beginVisualization();
                start_color = mask[width * y + x];
                int cx, cy;          //current position
                //some itratins
                queue.add(new IntPoint(x, y));
                while(!queue.isEmpty() && cont){
                    IntPoint cur = queue.poll();
                    undoAreaCalculator.add(cur.x, cur.y, 5);


                    int oc = mask[width * cur.y + cur.x];   //old
                    int oa = Color.alpha(oc);
                    int or = Color.red(oc);
                    int og = Color.green(oc);
                    int ob = Color.blue(oc);

                    int ac = color;                     //aim
                    int aa = Color.alpha(ac);
                    int ar = Color.red(ac);
                    int ag = Color.green(ac);
                    int ab = Color.blue(ac);

                    int na = Math.min(255, oa + aa);        //new
                    //Logger.log("oa="+oa+" aa="+aa+" na="+na);
                    int nr = or + (ar-or)*aa/255;
                    int ng = og + (ag-og)*aa/255;
                    int nb = ob + (ab-ob)*aa/255;
                    mask[width * cur.y + cur.x] = Color.argb(na, nr, ng, nb);


                    cx = cur.x+1;
                    cy = cur.y;
                    if(cur.x < width-1 && !queued[cx][cy] && start_color == mask[width*cy+cx]){//bitmap.getPixel(cx, cy)){
                        queue.add(new IntPoint(cx, cy));
                        queued[cx][cy] = true;
                    }

                    cx = cur.x;
                    cy = cur.y+1;
                    if(cur.y < height-1 && !queued[cx][cy] && start_color == mask[width*cy+cx]){
                        queue.add(new IntPoint(cx, cy));
                        queued[cx][cy]=true;
                    }

                    cx = cur.x-1;
                    cy = cur.y;
                    if(cur.x > 0 && !queued[cx][cy] && start_color == mask[width*cy+cx]){
                        queue.add(new IntPoint(cx, cy));
                        queued[cx][cy]=true;
                    }

                    cx = cur.x;
                    cy = cur.y-1;
                    if(cur.y > 0 && !queued[cx][cy] && start_color == mask[width*cy+cx]){
                        queue.add(new IntPoint(cx, cy));
                        queued[cx][cy]=true;
                    }
                }
                bitmap.setPixels(mask, 0, width, 0, 0, width, height);
                undoAreaCalculator.check(bitmap.getWidth(), bitmap.getHeight());
                draw.undoProvider.apply(undoAreaCalculator.top, undoAreaCalculator.bottom, undoAreaCalculator.left, undoAreaCalculator.right);
                draw.undoProvider.prepare();
                draw.lastChangeToBitmap = System.currentTimeMillis();
            }catch (Exception | OutOfMemoryError e){
                showMessage("Ошибка: " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    hideMessage();
                }
            });
        }

        protected void hideMessage() {
            try{
                draw.redraw();
                fillingInProgress = false;
                if(refreshTimer != null)
                    refreshTimer.cancel();
                if(dialogLoading != null)
                    dialogLoading.cancel();
                TimeProvider.finish("FillerRecursivePixels");
                draw.redraw();
            }catch (Exception | OutOfMemoryError e){
                Logger.log("ошибка "+(Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
    }
    class FillerRecursiveStackPixels {
        DialogLoading dialogLoading = null;
        final Object visualizationSync = new Object();
        Handler handler = new Handler();
        Timer refreshTimer = null;
        AreaCalculator undoAreaCalculator = null;
        boolean cont=true;
        int[] mask;
        int height;
        int width;
        int startX;
        int startY;
        Bitmap bitmap;
        int color;
        Context dialogContext;
        String dialogCancelButtonText;
        String dialogText;
        LinkedList<IntPoint> queue;
        boolean[][] queued;
        int start_color;

        FillerRecursiveStackPixels (Bitmap _bitmap, int _startX, int _startY, int _color, Context _dialogContext, String _dialogCancelButtonText, String _dialogText){
            bitmap = _bitmap;
            startX = _startX;
            startY = _startY;
            color = _color;
            dialogContext = _dialogContext;
            dialogCancelButtonText = _dialogCancelButtonText;
            dialogText = _dialogText;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            queue = new LinkedList<IntPoint>();
            queued = new boolean[width][height];
        }
        DialogInterface.OnClickListener getCancelListener(){
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cont = false;
                }
            };
        }
        public void execute(){
            showMessage();
            new Thread(() -> doFill()).start();
        }
        private void beginVisualization(){
            refreshTimer = new Timer();
            refreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (visualizationSync) {
                        bitmap.setPixels(mask, 0, width, 0, 0, width, height);
                        draw.redraw();
                        handler.post(() -> dialogLoading.setText(Data.tools.getResource(R.string.Filling) + " (" + queue.size() + ") ..."));
                    }
                }
            }, 100, 100);
        }
        protected void showMessage() {
            TimeProvider.start("FillerRecursivePixels");
            if(dialogContext != null && dialogText != null && dialogCancelButtonText != null) {
                dialogLoading = new DialogLoading(dialogContext, dialogCancelButtonText, getCancelListener(), dialogText + "...");
                dialogLoading.show();
            }

        }
        private void showMessage(String text){
            Logger.show(text);
        }
        protected void doFill() {
            try{
                undoAreaCalculator = new AreaCalculator();
                int x=startX, y=startY;
                Logger.log("Draw.handlerFill", "Filling from ("+x+" ; "+y+")", false);
                synchronized (visualizationSync) {
                    mask=new int[width*height] ;
                    bitmap.getPixels(mask, 0, width, 0, 0, width, height);
                }
                beginVisualization();
                start_color = mask[width * y + x];
                int cx, cy;          //current position
                //some itratins
                queue.add(new IntPoint(x, y));
                while(!queue.isEmpty() && cont){
                    IntPoint cur = queue.poll();
                    undoAreaCalculator.add(cur.x, cur.y, 5);
                    mask[width * cur.y + cur.x] = color;

                    cx = cur.x+1;
                    cy = cur.y;
                    if(cur.x < width-1 && !queued[cx][cy] && start_color == mask[width*cy+cx]){//bitmap.getPixel(cx, cy)){
                        queue.addFirst(new IntPoint(cx, cy));
                        queued[cx][cy] = true;
                    }

                    cx = cur.x;
                    cy = cur.y+1;
                    if(cur.y < height-1 && !queued[cx][cy] && start_color == mask[width*cy+cx]){
                        queue.addFirst(new IntPoint(cx, cy));
                        queued[cx][cy]=true;
                    }

                    cx = cur.x-1;
                    cy = cur.y;
                    if(cur.x > 0 && !queued[cx][cy] && start_color == mask[width*cy+cx]){
                        queue.addFirst(new IntPoint(cx, cy));
                        queued[cx][cy]=true;
                    }

                    cx = cur.x;
                    cy = cur.y-1;
                    if(cur.y > 0 && !queued[cx][cy] && start_color == mask[width*cy+cx]){
                        queue.addFirst(new IntPoint(cx, cy));
                        queued[cx][cy]=true;
                    }
                }
                bitmap.setPixels(mask, 0, width, 0, 0, width, height);
                undoAreaCalculator.check(bitmap.getWidth(), bitmap.getHeight());
                draw.undoProvider.apply(undoAreaCalculator.top, undoAreaCalculator.bottom, undoAreaCalculator.left, undoAreaCalculator.right);
                draw.undoProvider.prepare();
                draw.lastChangeToBitmap = System.currentTimeMillis();
            }catch (Exception | OutOfMemoryError e){
                showMessage("Ошибка: " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
            handler.post(() -> hideMessage());
        }

        protected void hideMessage() {
            try{
                draw.redraw();
                fillingInProgress = false;
                if(refreshTimer != null)
                    refreshTimer.cancel();
                if(dialogLoading != null)
                    dialogLoading.cancel();
                TimeProvider.finish("FillerRecursivePixels");
                draw.redraw();
            }catch (Exception | OutOfMemoryError e){
                Logger.log("ошибка "+(Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
    }
    class FillerRecursiveLines extends AsyncTask<Object, String, Void> {
        DialogLoading dialogLoading = null;
        boolean cont=true;
        boolean[][] mask;
        int height;
        int width;
        int start_color;
        int startX;
        int startY;
        Bitmap bitmap;
        int color;
        Context dialogContext;
        String dialogCancelButtonText;
        String dialogText;
        boolean[] queued;
        LinkedList<Integer> queue;

        FillerRecursiveLines (Bitmap _bitmap, int _startX, int _startY, int _color,
                              Context _dialogContext, String _dialogCancelButtonText, String _dialogText){
            bitmap = _bitmap;
            startX = _startX;
            startY = _startY;
            color = _color;
            dialogContext = _dialogContext;
            dialogCancelButtonText = _dialogCancelButtonText;
            dialogText = _dialogText;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            mask=new boolean[width][height] ;
        }
        DialogInterface.OnClickListener getCancelListener(){
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cont = false;
                }
            };
        }
        @Override protected void onPreExecute() {
            super.onPreExecute();
            TimeProvider.start("FillerRecursiveLines");
            if(dialogContext != null && dialogText != null && dialogCancelButtonText != null) {
                dialogLoading = new DialogLoading(dialogContext, dialogCancelButtonText, getCancelListener(), dialogText + "...");
                dialogLoading.show();
            }

            queue = new LinkedList<Integer>();
            queued = new boolean[height];
        }
        boolean valid(int x, int y){
            synchronized (bitmap){
                return check(start_color, bitmap.getPixel(x, y));
            }
        }
        boolean check(int color1, int color2){
            return color1 == color2;
        }
        void step(int y){
            for(int x = width-1; x > 0; x--){
                if(mask[x][y]){
                    if(valid(x - 1, y)){
                        set(x - 1, y);
                    }
                }
            }
            boolean validUp = false;
            boolean validDown = false;
            for(int x=0; x < width-1; x++){
                if(mask[x][y]){
                    if(valid(x + 1, y)){
                        set(x+1, y);
                    }

                    if(y < height-1 && valid(x, y + 1)){
                        set(x, y+1);
                        if(!validDown){
                            queue.add(y+1);
                            validDown = true;
                        }
                    }

                    if(y > 1 && valid(x, y - 1)){
                        set(x, y - 1);
                        if(!validUp){
                            queue.add(y - 1);
                            validUp = true;
                        }
                    }
                }
            }
        }
        void set(int x, int y){
            synchronized (bitmap){
                bitmap.setPixel(x, y, color);
            }
            mask[x][y] = true;
        }
        @Override protected Void doInBackground(Object... params) {
            try{
                publishProgress("Message", "doInBackground...", "0");
                int x=(int)((Point)params[0]).x;
                int y=(int)((Point)params[0]).y;
                Logger.log("Draw.handlerFill", "Filling from (" + x + " ; " + y + ")", false);
                start_color = bitmap.getPixel(x,y);
                //some itrations
                set(x, y);
                queue.add(y);
                int counter = 0;
                while(!queue.isEmpty() && cont){
                    int cur = queue.poll();
                    step(cur);

                    if(counter == 20){
                        publishProgress("Progress", String.valueOf(queue.size()), "0");
                        Thread.sleep(0, 1);
                        counter = 0;
                    }
                    counter ++;
                }
                draw.undoProvider.apply(0, bitmap.getHeight(), 0, bitmap.getWidth());
                draw.undoProvider.prepare();
                draw.lastChangeToBitmap = System.currentTimeMillis();
            }catch (Exception e){
                publishProgress("Message", "Exception: "+ e, "0");
                e.printStackTrace();
            }catch (OutOfMemoryError e) {
                publishProgress("Message", "OutOfMemoryError: "+ e, "0");
                e.printStackTrace();
            }
            return null;
        }
        @Override protected void onProgressUpdate(String... message) {
            try {
                draw.redraw();
                if(message[0].contains("Message"))
                    Logger.log("doInBackground", message[1], message[2].contains("1"));
                else if(message[0].contains("Progress")) {
                    dialogLoading.setText(Data.tools.getResource(R.string.Filling)+" ("+message[1]+")");
                }
            }catch (Exception | OutOfMemoryError e){
                Logger.log("ошибка " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
        @Override protected void onPostExecute(Void result) {
            try{
                super.onPostExecute(result);
                draw.redraw();
                fillingInProgress = false;
                if(dialogLoading != null)
                    dialogLoading.cancel();
                TimeProvider.finish("FillerRecursiveLines");
                draw.redraw();
            }catch (Exception | OutOfMemoryError e){
                Logger.log("ошибка "+(Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
    }
    class NativeFillerStackPixels {
        DialogLoading dialogLoading = null;
        final Object visualizationSync = new Object();
        Handler handler = new Handler();
        Timer refreshTimer = null;
        int startX;
        int startY;
        Bitmap bitmap;
        int color;
        Context dialogContext;
        String dialogCancelButtonText;
        String dialogText;

        NativeFillerStackPixels (Bitmap _bitmap, int _startX, int _startY, int _color, Context _dialogContext, String _dialogCancelButtonText, String _dialogText){
            bitmap = _bitmap;
            startX = _startX;
            startY = _startY;
            color = _color;
            dialogContext = _dialogContext;
            dialogCancelButtonText = _dialogCancelButtonText;
            dialogText = _dialogText;
        }
        public void execute(){
            showMessage();
            new Thread(this::doFill).start();
        }
        private void beginVisualization(){
            refreshTimer = new Timer();
            refreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (visualizationSync) {
                        draw.redraw();
                    }
                }
            }, 200, 200);
        }
        protected void showMessage() {
            if(dialogContext != null && dialogText != null && dialogCancelButtonText != null) {
                dialogLoading = new DialogLoading(dialogContext, dialogCancelButtonText, null, dialogText + "...");
                dialogLoading.show();
            }
        }
        private void showMessage(String text){
            Logger.show(text);
        }
        protected void doFill() {
            try{
                Logger.log("Draw.handlerFill", "Filling from (" + startX + " ; " + startY + ")", false);
                Logger.log("Color to fill: " + color);
                //beginVisualization();

                TimeProvider.start("nativeFill");
                int threshold = ((Integer) Data.get(Data.fillThresholdInt()));
                int overfill = ((Boolean)Data.get(Data.antialiasingBoolean())?1:0); //0,1
                String result = fillNative(bitmap, startX, startY, threshold, overfill, color, 1);
                draw.lastChangeToBitmap = System.currentTimeMillis();
                Logger.log("result: " + result);
                TimeProvider.finish("nativeFill");
                if(result.contains("Error:"))
                    Logger.show(result);
                else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (jsonObject.getString("result").equals("SUCCESS")) {
                            int top = jsonObject.getInt("top");
                            int bottom = jsonObject.getInt("bottom") + 1;
                            int left = jsonObject.getInt("left");
                            int right = jsonObject.getInt("right") + 1;
                            Logger.log("Backing up: " + " top=" + top + " bottom=" + bottom + " right=" + right + " left=" + left + " ...");

                            if(draw.undoAreaCalculator != null) { //useful for debug
                                draw.undoAreaCalculator.reset();
                                draw.undoAreaCalculator.add(left, top, 1);
                                draw.undoAreaCalculator.add(right, bottom, 1);
                            }
                            draw.undoProvider.apply(top, bottom, left, right);
                            draw.undoProvider.prepare();
                        } else {
                            draw.undoProvider.apply();
                            draw.undoProvider.prepare();
                        }
                    }
                    catch (Exception e){
                        draw.undoProvider.apply();
                        draw.undoProvider.prepare();
                    }
                }
            }catch (Exception | Error e){
                Logger.log("Ошибка в NativeFillerStackPixels.doFill(): " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
                Logger.messageBox("Ошибка в процедуре заливки. Попробуйте использовать другой метод, вызвав заливку двумя пальцами.");
            }
            handler.post(() -> hideMessage());
        }

        protected void hideMessage() {
            try{
                fillingInProgress = false;
                if(refreshTimer != null)
                    refreshTimer.cancel();
                if(dialogLoading != null)
                    dialogLoading.cancel();
                if(draw != null)
                    draw.redraw();
            }catch (Exception | OutOfMemoryError e){
                Logger.log("ошибка "+(Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        }
    }

    public static native String fillNative(Bitmap bitmap, int cx, int cy, int threshold, int overfill, int color, int writeLog);
}
