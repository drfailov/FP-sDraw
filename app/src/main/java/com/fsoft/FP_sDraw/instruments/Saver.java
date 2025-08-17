package com.fsoft.FP_sDraw.instruments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Point;
import com.fsoft.FP_sDraw.common.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Dr. Failov on 17.01.2015.
 */
public class Saver {
    DrawCore draw;
    Context context;
    public boolean savingInProgress = false;
    public boolean fileCompressorInProgress = false;
    private Thread autoSaveThread = null;
    private Timer autoSaveTimer = null;
    private long lastAutoSave = 0;

    public Saver(DrawCore d){
        draw = d;
        context = d.context;
        //schedule autosaver
        autoSaveTimer = new Timer();
        autoSaveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //Logger.log("Я живий!");
                if(draw.lastChangeToBitmap > lastAutoSave
                        && System.currentTimeMillis() - lastAutoSave > 10000
                        && System.currentTimeMillis() - draw.lastChangeToBitmap > 3000){
                    autoSave();
                }
            }
        }, 10000, 10000);
    }
    public void autoSave(){
        if(autoSaveThread == null){
            autoSaveThread = new Thread(() -> {
                try {
                    lastAutoSave = System.currentTimeMillis();
                    draw.saver.autoSaveAsync();
                }
                catch (Exception e){
                    Logger.log(e);
                }
                finally {
                    autoSaveThread = null;
                }
            } );
            autoSaveThread.start();
        }

    }
    public void autoSaveAsync(){
        if(draw == null || draw.context == null || draw.bitmap == null)
            return;
        try{
            savingInProgress = true;
            //INIT
            String filename = addDateTimeExtension("autosave");          //имя файла
            File path = Data.getAutosaveFolder(draw.context);     //путь для сохранения без слеша конце
            Logger.log("autosave_file", "Автосохранение рисунка в " + path + "/" + filename + "...", false);
            //CHECK
            boolean empty = draw.isEmpty();
            if(empty){
                Logger.log("autosave", "Рисунок пустой.", false);
                return;
            }
            boolean ready = checkFolder(path);
            if(!ready){
                Logger.log("autosave", "Ошибка создания папки", true);
                return;
            }
            //CREATE NOMEDIA FILE
            checkNomedia(path);
            //SAVE
            File file = new File(path, filename);
            //Bitmap.createBitmap(draw.bitmap.getWidth(), draw.bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Bitmap tmp = null;
            try {
                //Canvas canvas = new Canvas(tmp);
                //noinspection SynchronizeOnNonFinalField
                synchronized (draw.bitmap) {
                    //canvas.drawBitmap(draw.bitmap, 0, 0, new Paint());
                    tmp = draw.bitmap.copy(Bitmap.Config.ARGB_8888, false);
                }
            }catch (Exception | OutOfMemoryError e){
                Logger.show("Ошибка обращения к рисунку");
                Logger.log("Error while copying bitmap");
            }
            Saver.FileCompressor fileCompressor = new Saver.FileCompressor(file, tmp, false, context, draw.uiHandler);
            if(tmp != null)
                fileCompressor.start();
            cleanAutosaves();
        }
        catch (Exception | OutOfMemoryError e){
            Logger.log("Draw.save_file", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        finally {
            savingInProgress = false;
        }
    }
    public void save(boolean withoutGrid){ //this function is called from MENU
        new Thread(() -> {
            Integer gridOpacityBeforeSave = null;
            try {
                if(withoutGrid) {
                    gridOpacityBeforeSave = (Integer) Data.get(Data.gridOpacityInt());
                    Data.save(0, Data.gridOpacityInt());
                }
                //PREPARE BITMAPS
                Bitmap transparentBitmap = null;
                Bitmap fullBitmap;
                String nameFull;
                String nameTransparent;

                try {
                    Logger.log("Copying transparent bitmap...");
                    //noinspection SynchronizeOnNonFinalField
                    synchronized (draw.bitmap) {
                        transparentBitmap = Bitmap.createBitmap(draw.bitmap);
                    }
                } catch (Exception | OutOfMemoryError e) {
                    Logger.show("Ошибка получения прозрачного изображения");
                }

                Logger.log("Copying full bitmap...");
                fullBitmap = draw.createFullBitmap(); //can return null if error

                nameFull = addDateTimeExtension("sDraw");
                Logger.log("Filename nameFull: " + nameFull);

                nameTransparent = addDateTime("sDraw");
                nameTransparent += "." + Data.get(Data.backgroundColorInt());
                nameTransparent += "." + Data.get(Data.gridOpacityInt());
                nameTransparent += "." + Data.get(Data.gridSizeInt());
                nameTransparent += ".png";
                Logger.log("Filename nameTransparent: " + nameTransparent);

                saveImageToGallery(fullBitmap, nameFull);
                saveImageToTransparentFolder(transparentBitmap, nameTransparent);
            }catch (Exception e){
                Logger.log("Ошибка сохранения рисунка " + e);
                Logger.log(Tools.getStackTrace(e));
                Logger.show(Data.tools.getResource(R.string.error) + ": " + e.getLocalizedMessage());
            }catch (OutOfMemoryError e){
                if(draw.undoProvider.freeUp()){
                    Logger.log("Недостаточно памяти для сохранения рисунка. Попробуем еще разок...");
                    Logger.show("Подождите...");
                    Tools.sleep(1000);
                    save(withoutGrid);
                }
                else {
                    Logger.log("Недостаточно памяти для сохранения рисунка " + e);
                    Logger.show(Data.tools.getResource(R.string.error) + ": недостаточно памяти для сохранения рисунка. Попробуйте позже.");
                }
            }
            finally {
                if(gridOpacityBeforeSave != null){
                    Data.save(gridOpacityBeforeSave, Data.gridOpacityInt());
                }
            }
        }).start();
    }
    public void saveImageToGallery(Bitmap bitmap, @NonNull String name) throws IOException {
        boolean saved;
        OutputStream fos;
        String pathToAddToGallery = null;
        String IMAGES_FOLDER_NAME = "sDraw";
        if(!name.endsWith(".png"))
            name = name + ".png";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = draw.context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/" + IMAGES_FOLDER_NAME);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        }
        else  {
            //String imagesDir = Data.getPicturessDrawFolder();
            File file = Data.getPicturessDrawFolder();
            if (!file.exists())
                if(!file.mkdirs())
                    throw new IOException("Can't create folder to save image. Do it manually: " + file.getPath());
            File image = new File(file, name);
            pathToAddToGallery = image.getAbsolutePath();
            fos = new FileOutputStream(image);
        }

        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
        if(saved){
            if(pathToAddToGallery != null)
                Tools.addToGallery(pathToAddToGallery, context);
            Logger.show(context.getString(R.string.drawingSavedAs) + " "+Environment.DIRECTORY_PICTURES);
        }
    }
    public void saveImageToTransparentFolder(Bitmap bitmap, String filename){//сохраняет рисунок во внутреннюю папку
        try{
            File path = Data.getTransparentFolder(context);
            Logger.log("Draw.save_file", "Сохранение рисунка в " + path + "...", false);
            boolean ready = checkFolder(path);
            if(!ready){
                Logger.log("save", "Ошибка создания папки", false);
                Logger.show(context.getString(R.string.save_error_folder));
                return;
            }
            //SAVE
            File file = new File(path, filename);

            Saver.FileCompressor fileCompressor = new Saver.FileCompressor(file, bitmap, false, context, draw.uiHandler);
            fileCompressor.start();
        }catch (Exception | OutOfMemoryError e){
            Logger.log("save", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    public void open(final String path){
    /*
    Отрисовка файла на холсте
     */
        new Thread(() -> {
            try{
                Logger.log("Draw.open_file", "Открыть файл " + path, false);
                if(new File(path).isFile()){
                    //apply background
                    int newBackground = Tools.extractBackgroundColorFromFileName(path);
                    if(newBackground != Color.TRANSPARENT)
                        Data.save(newBackground, Data.backgroundColorInt());
                    //apply grid Opacity
                    int newOpacity = Tools.extractGridOpacityFromFileName(path);
                    if(newOpacity != -1)
                        Data.save(newOpacity, Data.gridOpacityInt());
                    //apply grid size
                    int newSize = Tools.extractGridSizeFromFileName(path);
                    if(newSize != -1)
                        Data.save(newSize, Data.gridSizeInt());

                    //load image
                    Bitmap fileBitmap=null;
                    boolean cont=true;
                    while (cont) {
                        try {
                            fileBitmap=BitmapFactory.decodeFile(path);
                            cont=false;
                        } catch (OutOfMemoryError e){
                            cont=draw.undoProvider.freeUp();
                            if(!cont){
                                Logger.log("Saver.Open()", "Неисправимая ошибка памяти: "+e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                                throw e;
                            }
                        }
                    }
                    if(fileBitmap == null){
                        Logger.show("С файла прочитан нулевой битмап: \n" + path);
                        return;
                    }
                    final int fileHeight = fileBitmap.getHeight();
                    final int fileWidth = fileBitmap.getWidth();
                    float differenceNormal = Math.abs(fileHeight-draw.bitmap.getHeight()) + Math.abs(fileWidth-draw.bitmap.getWidth());
                    float differenceRotated = Math.abs(fileWidth-draw.bitmap.getHeight()) + Math.abs(fileHeight-draw.bitmap.getWidth());
                    Logger.log("Draw.open_file", "differenceNormal: " + differenceNormal, false);
                    Logger.log("Draw.open_file", "differenceRotated: " + differenceRotated, false);
                    draw.clear();
                    //apply with no rotation
                    if(differenceNormal < differenceRotated){
                        Logger.log("Draw.open_file", "Opening file without rotation...", false);
                        draw.canvas.drawBitmap(fileBitmap, 0,0, new Paint());
                        Logger.log("Draw.open_file", "Файл открыт без поворота ", false);
                    }
                    else{ //apply with rotation
                        draw.canvas.save();
                        if(draw.canvas.getWidth() < draw.canvas.getHeight()) {  //if current orientation is portrait
                            Logger.log("Draw.open_file", "Opening file with 90* rotation...", false);
                            draw.canvas.rotate(90);
                            draw.canvas.drawBitmap(fileBitmap, 0,-fileHeight, new Paint());
                        }
                        else {//if current orientation is landscape
                            Logger.log("Draw.open_file", "Opening file with -90* rotation...", false);
                            draw.canvas.rotate(270);
                            draw.canvas.drawBitmap(fileBitmap, -fileWidth, 0, new Paint());
                        }
                        draw.canvas.restore();
                    }
                    draw.lastChangeToBitmap = System.currentTimeMillis();
                    draw.undoProvider.apply(0, draw.bitmap.getHeight(), 0, draw.bitmap.getWidth());
                    draw.undoProvider.prepare();
                    draw.redraw();
                }
                else {
                    Logger.log("Draw.open_file", "Файл не существует: " + path, true);
                }
            }catch (Exception e){
                Logger.log("Draw.open_file", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                Logger.show("Ошибка отрытия файла \n" + path);
            } catch (OutOfMemoryError e) {
                Logger.log("Draw.open_file", "OutOfMemoryError: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                Logger.show("Недостаточно памяти для открытия файла");
                draw.undoProvider.freeUp();
            }
        }).start();
    }

    void cleanAutosaves(){
        Saver.AutosavesCleaner autosavesCleaner = new Saver.AutosavesCleaner();
        autosavesCleaner.run();
    }
    private Bitmap decodeFile(String path, int required_w, int required_h) {
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            // Calculate inSampleSize
            final int height = options.outHeight;
            final int width = options.outWidth;
            if (height > required_h || width > required_w) {
                // Calculate ratios of height and width to requested height and width
                final int heightRatio = Math.round((float) height / required_h);
                final int widthRatio = Math.round((float) width /  required_w);
                // Choose the smallest ratio as inSampleSize value, this will guarantee a final image with both dimensions larger than or equal to the requested height and width.
                options.inSampleSize = Math.min(heightRatio, widthRatio);
            }
            options.inJustDecodeBounds = false;
            // Decode bitmap with inSampleSize set
            Bitmap result=null;
            boolean cont=true;
            while (cont) {
                try {
                    result=BitmapFactory.decodeFile(path, options);
                    cont=false;
                } catch (OutOfMemoryError e){
                    cont=draw.undoProvider.freeUp();
                    if(!cont){
                        Logger.log("GlobalData.decodeFile", "Неисправимая ошибка памяти: "+e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
                        throw e;
                    }
                }
            }
            return result;
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.decodeFile произошла ошибка ",e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }catch (OutOfMemoryError e) {
            Logger.log("Где-то в GlobalData.decodeFile Недостаточно памяти ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return null;
    }   //декодирование файла с оптимизацией до определенного размера
    private void checkNomedia(File path){
        File nomedia = new File(path, ".nomedia");
        try{
            if(nomedia.createNewFile())
                Logger.log("Draw.checkNomedia", "Файл папке "+path+" файл .nomedia создан!", false);
        }catch (Exception e) {
            Logger.log("Draw.checkNomedia", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    private boolean checkFolder(File path){
        //если надо - создать папку
        boolean exist = path.isDirectory();
        if(!exist) {
            Logger.log("Draw.checkFolder", "Создание папки " + path + "...", false);
            exist = path.mkdirs();
            //если папку созать не удалось
            if(!exist){
                Logger.log("Draw.checkFolder", "Создать папку не удалось: \n " + path, false);
            }
        }
        return exist;
    }
    //добавляет нижнее подчеркивание, дату, время и разрешение .png
    static public String addDateTimeExtension(String filename){
        return addDateTime(filename) + ".png";
    }
    static public String addDateTime(String filename){
        //дополнить имя файла
        Calendar date = Calendar.getInstance();
        filename += "_" + (date.get(Calendar.YEAR)) + "-";
        if(date.get(Calendar.MONTH)+1 < 10) filename += "0";
        filename += (date.get(Calendar.MONTH)+1) + "-";
        if(date.get(Calendar.DAY_OF_MONTH) < 10) filename += "0";
        filename += (date.get(Calendar.DAY_OF_MONTH)) + "_";
        if(date.get(Calendar.HOUR_OF_DAY) < 10) filename += "0";
        filename += (date.get(Calendar.HOUR_OF_DAY)) + "-";
        if(date.get(Calendar.MINUTE) < 10) filename += "0";
        filename += (date.get(Calendar.MINUTE)) + "-";
        if(date.get(Calendar.SECOND) < 10) filename += "0";
        filename += String.valueOf(date.get(Calendar.SECOND));
        return filename;
    }

    private class FileCompressor extends Thread {
        File file;
        Bitmap image;
        boolean ok = true;
        boolean messages;
        Context context;
        Handler uiHandler;

        public FileCompressor(File file, Bitmap toSave, boolean messages, Context _context, Handler uiHandler) {
            this.file = file;
            image = toSave;
            this.messages = messages;
            context = _context;
            this.uiHandler = uiHandler;
        }

        @Override
        public synchronized void start() {
            Saver.this.fileCompressorInProgress = true;
            super.start();
        }

        @Override
        public void run() {
            super.run();
            try {
                doInBackground();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            finally {
                Saver.this.fileCompressorInProgress = false;
            }
        }

        protected void doInBackground() {
            FileOutputStream fileOutputStream;
            if(file == null || image == null)
                publishProgress(context.getString(R.string.error_saving_saving));
            try {
                fileOutputStream = new FileOutputStream(file);
            }catch(Exception e){
                publishProgress(context.getString(R.string.error_saving_writing_file));
                Logger.log("Error while opening file");
                ok = false;
                return;
            }

            try {
                image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            }catch(Exception | OutOfMemoryError e){
                publishProgress(context.getString(R.string.error_saving_compressing));
                Logger.log("Error while compressing file");
                ok = false;
            }

            try {
                fileOutputStream.close();
            }catch (Exception e){
                Logger.log("Error while closing file");
                ok = false;
            }

            if(ok)
                publishProgress(context.getString(R.string.drawingSavedAs) + " "+file);

            if(uiHandler != null) {
                uiHandler.post(() -> {
                    try {
                        if (ok && messages)
                            Tools.addToGallery(file.toString(), context);
                    } catch (Throwable e) {
                        Logger.log("Error in AddToGallery(...): " + Tools.getStackTrace(e));
                        e.printStackTrace();
                    }
                });
            }
        }

        void publishProgress(final String text){
            if(uiHandler != null)
                uiHandler.post(() -> {
                    if(text != null) {
                        if(messages)
                            Logger.show(text);
                        else
                            Logger.log(text);
                    }
                });
        }
    }
    private class AutosavesCleaner{
        //limit number of autosaved images to 100
        int autosave_limit = 100;
        File autosave_path;      //путь для сохранения без слеша конце


        void run(){
            new Thread(() -> {
                try {
                    runAsync();
                }
                catch (Exception e){
                    Logger.log(e);
                }
            }).start();
        }
        void runAsync() {
            autosave_path = Data.getAutosaveFolder(draw.context);

            if(autosave_limit!=0)
            {
                Logger.log("clearAutosaves", "Чистка автосохранений...", false);
                //AutoClearCache
                String[] files;
                if(autosave_path.isDirectory()){
                    files=autosave_path.list((file, s) -> s.endsWith(".jpg") || s.endsWith(".png"));
                }
                else  {
                    Logger.log("clearAutosaves", "Папки нет", false);
                    files=null;
                }
                if(files != null && files.length> autosave_limit) {
                    int howDelete=files.length- autosave_limit;
                    Logger.log("clearAutosaves", "Удаление " + (howDelete) + " файлов...", false);
                    java.util.Arrays.sort(files, 0, files.length);
                    for(int i=0; i<howDelete;i++)
                        if(!new File(autosave_path+File.separator+files[i]).delete())
                            Logger.log("sDraw.clearAutosaves", "Удалить не получилось:" + files[i], false);
                }
            }
        }
    }
}
