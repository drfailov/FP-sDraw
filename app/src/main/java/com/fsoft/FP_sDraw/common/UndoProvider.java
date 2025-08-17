package com.fsoft.FP_sDraw.common;


import android.app.ActivityManager;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;

import java.util.ArrayList;

/*Created by Dr. Failov 2023-02-01
*
* Memory research:
* Galaxy S21U, RAM12GB, AvailMem=4.5GB, threshold=590MB, NO FAIL, android 13
* Galaxy S8, 1080x2076, RAM4GB, AvailMem=1.8GB, threshold=226MB (1500% norm), NO FAIL, android 9
* Galaxy Tab S4, 1600x2464, AvailMem=1.42GB
 * SGS7, AvailMem=2GB, 1080x1920, runtime.maxMemory=268MB, ,android 8, NO FAIL
* Motorola Droid, 540x888 AvailMem=519MB, threshold=100M, runtime.MaxMemory=100M (100% norm, 200% norm, 440% fail (83MB, 83% FAIL)), android 4.4
* Galaxy Note III, 1080x1920, AvailMem=1.62GB, runtime.maxMemory=201MB 190% FAIL (157MB, 78%), android 5
* SGS4, AvailMem=1.2GB, 1080x1920(8.29MB), runtime.maxMemory=201MB (20x), 200% FAIL (82%), android 7
* Meizu M3s, AvailMem=716MB, 720x1080(3.11MB), runtime.maxMemory=268MB (86x), 670% FAIL (77%), android 5,
*
*
* Итак, вот как это работает
* Начиная с Android 8 (API26) включительно, битмапы не занимают в Heap приложения ничего, они хранятся отдельно и там я лимитов вообще не заметил.
* На версиях ДО Android 8 (API26) битмапы хранятся в Heap приложения и нужно следить за использованием памяти.
* Как?
* Размер битмапа в байтах вычисляется как X*Y*4.
* Размер Heap вычисляется как Runtime.getRuntime().maxMemory().
* Если переполнить этот участок памяти, начинается OOM.
*
*
* //since Android O makes your app also use the native RAM (at least for Bitmaps storage, which is usually the main reason for huge memory usage)
*
* ARGB_8888 = Each pixel is stored on 4 bytes.
* 540x888=479520px=1918080bytes=1.9MB
* 440% fail = 44*1.9MB = 83MB
*
* 1080x2076=2242080px
* 2242080px=8968320bytes=8.9MB
* 8.9MB * 150 = 1.3GB
*
*
* */

public class UndoProvider {
    //prepare() -> apply()
    //undo()
    //redo()
    private final DrawCore draw;
    Bitmap image_tmp;//место, где хранится изображение между вызовами prepare и apply
    private final ArrayList<UndoStep> undoSteps = new ArrayList<>(); //0 is newer steps, last is older steps
    int undoFailCounter=0;
    int current = -1; //на какой точке отмены мы сейчас находимся -1 - текущий как бы
    int cleanUpCounter=0;//счетчик попыток очистки памяти

    public UndoProvider(DrawCore d) {
        draw = d;
    }
    public void prepare() {
        try{
            boolean cont=true;
            while (cont) {
                try {
                    image_tmp=Bitmap.createBitmap(draw.bitmap);
                    cont=false;
                } catch (OutOfMemoryError e){
                    cont=freeUp();
                    if(!cont)
                        Logger.log("UndoProvider.prepare недостаточно памяти: ",e.toString(), false);
                }
            }
        }
        catch (Exception|OutOfMemoryError e){
            Logger.log(e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }
    public void apply(){
        apply(0, draw.getHeight(), 0, draw.getWidth());
    }
    public void apply(float top, float bottom, float left, float right){
        apply((int) top, (int) bottom, (int) left, (int) right);
    }
    public void apply(Rect rect){
        apply(rect.top, rect.bottom, rect.left, rect.right);
    }
    public void apply(int top, int bottom, int left, int right)  {
        //эта функция разнесена на две для того, чтобы была возможность сохранять
        // в памяти только небольшой фрагмент изображения
        try{
            //check bounds
            if(image_tmp!= null && image_tmp.getHeight() > 1 && image_tmp.getWidth() > 1){
                if(bottom >= image_tmp.getHeight())
                    bottom = image_tmp.getHeight()-1;
                if(right >= image_tmp.getWidth())
                    right = image_tmp.getWidth()-1;
                if(left < 0)
                    left = 0;
                if(top < 0)
                    top = 0;
            }
            if(image_tmp!= null && bottom <= image_tmp.getHeight() && right <= image_tmp.getWidth()
                    && top >= 0 && left >= 0) {
                if(current != -1) {//удаление всего что было новее наотменяно
                    if (current >= 0) {
                        undoSteps.subList(0, current + 1).clear();
                    }
                    current=-1;
                    Logger.log("UndoProvider.apply","Возврат в точку отсчета", false);
                }
                //запись
                boolean cont=true;
                while (cont) {
                    try {
                        Bitmap bitmap=Bitmap.createBitmap(image_tmp, left, top, (right-left+1), (bottom-top+1));
                        UndoStep undoStep = new UndoStep(bitmap, left, top);
                        undoSteps.add(0, undoStep);
                        cont=false;
                        image_tmp = null;
                    } catch (OutOfMemoryError e){
                        cont=freeUp();
                        if(!cont)Logger.log("При UndoProvider.apply недостаточно памяти ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                    }
                }
                //Очистка лишнего, чтобы не переполнять память
                while (getMemoryUsageBytes() > getMemoryLimitBytes() && !undoSteps.isEmpty())
                    undoSteps.remove(undoSteps.size()-1);
            }else{
                if(image_tmp == null)
                    Logger.log("Undo apply: ошибка - prepare() не был вызван.");
                else
                    Logger.log("Undo apply: ошибка - область сохранения выходит за рамки буфера.");
            }
        }catch (Exception|OutOfMemoryError e){
            Logger.log(e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
    }
    public void undo() {
        int old_current=current;
        try{
            //Не применять отмену если сейчас работает один из инструментов который вносит изменения в момент проведения по экрану
            if(draw.fingersOnScreen() && (draw.currentInstrument == draw.brush || draw.currentInstrument == draw.erase)){
                Logger.show(draw.context.getResources().getString(R.string.undoSkipped));
                return;
            }
            if(current< undoSteps.size()-1 && undoSteps.get(current+1).image != null) {
                undoFailCounter=0;
                current++;
                Logger.log("UndoProvider.undo", "Отмена до ячейки " + (current), false);
                Bitmap tmp=null;
                boolean cont=true;
                UndoStep undoStep = undoSteps.get(current);
                while (cont) {
                    try {
                        tmp=Bitmap.createBitmap(draw.bitmap, undoStep.x, undoStep.y, undoStep.image.getWidth(), undoStep.image.getHeight());
                        cont=false;
                    } catch (OutOfMemoryError e){
                        cont=freeUp();
                        if(!cont)Logger.log("При UndoProvider.undo недостаточно памяти ",e.toString(), true);
                        if(!cont)current=old_current;
                    }
                }
                if(tmp!=null) {
                    clearArea(undoStep.x, undoStep.y,
                            undoStep.x + undoStep.image.getWidth(), undoStep.y + undoStep.image.getHeight(),
                            draw.canvas);
                    draw.canvas.drawBitmap(undoStep.image,undoStep.x,undoStep.y, new Paint());
                    draw.lastChangeToBitmap = System.currentTimeMillis();
                    undoStep.image = tmp;
                    prepare();
                    draw.redraw();
                }
            }
            else {
                Logger.log("UndoProvider.undo", "Уже некуда отменять", false);
                //ПАСХАЛКА
                undoFailCounter++;
                if(undoFailCounter == 5)   {                               //показать сообщение "достигнуто максимальное количество отмен
                    Logger.messageBox(Data.tools.getResource(R.string.undoEndMaximum));
                }
                else if(undoFailCounter == 10)
                    Logger.show("Уже точно некуда отменять!");
                else if(undoFailCounter == 11)
                    Logger.show("Потому что точек отката больше нет!");
                else if(undoFailCounter == 12)
                    Logger.show("СОВСЕМ НЕТ!");
                else if(undoFailCounter == 13)
                    Logger.show("Ну Вы уже тут всех достали.");
                else if(undoFailCounter == 14)
                    Logger.show("Вы что, что-то потеряли?");
                else if(undoFailCounter == 15){                                       //Ебать ты лох
                    AlertDialog.Builder b = new AlertDialog.Builder(draw.context);
                    ImageView iv=new ImageView(draw.context);
                    iv.setImageResource(R.drawable.loh);
                    b.setView(iv);
                    b.setOnKeyListener((dialogInterface, i, keyEvent) -> keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN);
                    b.setPositiveButton("Okay...", null);
                    b.show();
                }
                else
                    Logger.show(Data.tools.getResource(R.string.undoEnd));
            }
        }catch (Exception e) {
            current=old_current;
            Logger.log("Где-то в UndoProvider.undo произошла ошибка ",e+"\n Oткат изменений..." + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
            Logger.show("Ошибка при попытке отмены");
        }
        catch (OutOfMemoryError e) {
            current=old_current;
            Logger.log("Draw.UndoProvider.undo", "Недостаточно памяти для отмены!" +
                    "\nВозможно, она освободится чуть позже" +
                    "\nOткат изменений...", true);
            freeUp();
        }
    }
    public void redo() {
        int old_current=current;
        try{
            if(current>=0) {
                Logger.log("UndoProvider.redo", "Повтор ячейки " + current, false);
                Bitmap tmp=null;
                boolean cont=true;
                UndoStep undoStep = undoSteps.get(current);
                while (cont) {
                    try {
                        tmp=Bitmap.createBitmap(draw.bitmap, undoStep.x, undoStep.y, undoStep.image.getWidth(), undoStep.image.getHeight());
                        cont=false;
                    } catch (OutOfMemoryError e){
                        cont=freeUp();
                        if(!cont)Logger.log("При UndoProvider.redo недостаточно памяти ",e.toString(), true);
                        if(!cont)current=old_current;
                    }
                }
                if(tmp!=null) {
                    clearArea(undoStep.x, undoStep.y,
                            undoStep.x + undoStep.image.getWidth(), undoStep.y + undoStep.image.getHeight(),
                            draw.canvas);
                    draw.canvas.drawBitmap(undoStep.image,undoStep.x,undoStep.y, new Paint());
                    draw.lastChangeToBitmap = System.currentTimeMillis();
                    undoStep.image = tmp;
                    prepare();
                    draw.redraw();
                    current--;
                }
            }
            else {
                Logger.log("UndoProvider.redo", Data.tools.getResource(R.string.redoEnd), false);
                Logger.show(Data.tools.getResource(R.string.redoEnd));
            }
        }catch (Exception e){
            Logger.log("Где-то в UndoProvider.redo произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), true);
            Logger.show("Ошибка при попытке повтора");
        }catch (OutOfMemoryError e) {
            current=old_current;
            Logger.log("Draw.UndoProvider.redo", "Недостаточно памяти для повтора!" +
                    "\n Возможно, она освободится чуть позже", true);
            freeUp();
        }
    }
    public boolean freeUp() {
        boolean OK=false;
        Logger.log("Draw.undoProvider.freeUp", "Зачистка памяти...", false);
        try {
            cleanUpCounter++;
            if(cleanUpCounter==30) {
                Toast.makeText(draw.context, "Обнаружены проблемы с памятью", Toast.LENGTH_LONG).show();
                Toast.makeText(draw.context, "Вам следует уменьшить количество шагов отмены", Toast.LENGTH_LONG).show();
                Toast.makeText(draw.context, "В дополнительных настройках", Toast.LENGTH_LONG).show();
                cleanUpCounter = 0;
                Logger.log("Draw.undoProvider.freeUp", "Обнаружены проблемы с памятью", false);
            }
            //удалить один самый давний шаг отмены
            if(!undoSteps.isEmpty()){
                undoSteps.remove(undoSteps.size()-1);
                OK=true;
            }
            System.gc();
        }catch (Exception e){
            Logger.log("UndoProvider.freeUp","Exception: " + e, false);
        }catch (OutOfMemoryError e) {
            Logger.log("UndoProvider.freeUp","OutOfMemoryError: " + e, false);
            Toast.makeText(draw.context, "Программа работает нестабильно. Рекомендуется перезагрузить программу.", Toast.LENGTH_LONG).show();
        }
        return OK;
    }

    public int getCurrentStepIndex(){//get system status info for debugging
        return current;
    }
    public int getStepsCount(){  //get system status info for debugging
        return undoSteps.size();
    }
    public long getMemoryUsageBytes(){
        long result = 0;
        for(int i=0; i<undoSteps.size(); i++){
            UndoStep undoStep = undoSteps.get(i);
            if(undoStep.image != null){
                result += undoStep.image.getWidth()*undoStep.image.getHeight()*4L;
            }
        }
        return result;
    }
    public long getMemoryLimitBytes(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ActivityManager.MemoryInfo m =  draw.context.getAvailableMemory();
            if(m == null)
                return draw.getWidth()*draw.getHeight()*4L*10L;
            return m.availMem/2L;
        }
        else {
            Runtime runtime = Runtime.getRuntime();
            if(runtime == null)
                return 15000000L;//15mb, очень мало
            return runtime.maxMemory() / 2L;
        }
    }
    public UndoStep getUndoStep(int i){ //get system status info for debugging
        if(i < getStepsCount())
            return undoSteps.get(i);
        return null;
    }


    private void clearArea(int left, int top, int right, int bottom, Canvas c){
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        c.drawRect(left, top, right, bottom, p);
    }

    public static class UndoStep{
        public Bitmap image;
        public int x;
        public int y;

        public UndoStep(Bitmap image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }
}
