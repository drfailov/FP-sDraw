package com.fsoft.FP_sDraw.common;

import static com.fsoft.FP_sDraw.DrawCore.OrientationProvider.ORIENTATION_HORIZONTAL;
import static com.fsoft.FP_sDraw.DrawCore.OrientationProvider.ORIENTATION_VERTICAL;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fsoft.FP_sDraw.FullVersionInfoActivity;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.menu.AnimatedButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Используется для хранения общеполезных функций
 * Created by Dr. Failov on 02.01.2015.
 */

public class Tools{
    //Этот класс создан для выполнения общеполезных прикладных задач
    Activity activity=null;
    HashMap<Integer, String> resourceCache;
    private Boolean paid = null;

    Tools(Activity _act){
        activity = _act;
        resourceCache = new HashMap<>();
    }
    static public boolean addToGallery(String filename, Context context){
        try {
            //новый
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.TITLE, "sDraw image");
            v.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            v.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis()/1000);
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            v.put(MediaStore.Images.Media.DATA, filename);
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if(uri != null){
                //сообщаем другим, что добавили
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                Logger.log("GlobalData.addToGallery", "Регистрация прошла успешно: "+filename, false);
                return true;
            }
            else {
                Toast.makeText(context, "Не удалось зарегистрировать файл в галерее. Он появится там позже.", Toast.LENGTH_SHORT).show();
                Logger.log("GlobalData.addToGallery", "В процессе регистрации возникли ошибки", false);
                return false;
            }
        }catch (Exception e){
            Logger.log("Где-то в Tools.addToGallery("+filename+") произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }catch (OutOfMemoryError e) {
            Logger.log("Где-то в Tools.addToGallery("+filename+") Недостаточно памяти ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return false;
    } //регистрация файла в галерее. Вызывается при сохранении файла
    static public String getStackTrace(Throwable aThrowable) {
        try{
            Writer result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            aThrowable.printStackTrace(printWriter);
            return result.toString();
        }catch (Exception e){
            Logger.log("getStackTrace : " + e);
        }
        return "(Error)";
    }
    public boolean isTouchSizeDetectionSupported() {
        //поскольку в хранилище хранятся данные отдельно для каждого устройства,
        // мы пытаемся найти хотя бы одно, которое поддерживает
        int[] tools = {1};

        if (Build.VERSION.SDK_INT >= 14)
            tools = new int[]{MotionEvent.TOOL_TYPE_UNKNOWN,
                    MotionEvent.TOOL_TYPE_ERASER,
                    MotionEvent.TOOL_TYPE_FINGER,
                    MotionEvent.TOOL_TYPE_MOUSE,
                    MotionEvent.TOOL_TYPE_STYLUS};
        for (int tool : tools) {
            float min = (Float) Data.get(Data.sizeMinFloat(tool));
            float max = (Float) Data.get(Data.sizeMaxFloat(tool));
            if (min != max)
                return true;
        }
        return false;
    }
    public boolean isTouchPressureDetectionSupported() {
        //поскольку в хранилище хранятся данные отдельно для каждого устройства,
        // мы пытаемся найти хотя бы одно, которое поддерживает
        int[] tools = {1};

        if (Build.VERSION.SDK_INT >= 14)
            tools = new int[]{MotionEvent.TOOL_TYPE_UNKNOWN,
                    MotionEvent.TOOL_TYPE_ERASER,
                    MotionEvent.TOOL_TYPE_FINGER,
                    MotionEvent.TOOL_TYPE_MOUSE,
                    MotionEvent.TOOL_TYPE_STYLUS};
        for (int tool : tools) {
            float min = (Float) Data.get(Data.pressureMinFloat(tool));
            float max = (Float) Data.get(Data.pressureMaxFloat(tool));
            if (min != max)
                return true;
        }
        return false;
    }
    public boolean isStylusSupported(){
        if (Build.VERSION.SDK_INT < 14){
            return false;
        }

        {
            float min = (Float) Data.get(Data.pressureMinFloat(MotionEvent.TOOL_TYPE_STYLUS));
            float max = (Float) Data.get(Data.pressureMaxFloat(MotionEvent.TOOL_TYPE_STYLUS));
            if (min != max)
                return true;
        }

        {
            float min = (Float) Data.get(Data.sizeMinFloat(MotionEvent.TOOL_TYPE_STYLUS));
            float max = (Float) Data.get(Data.sizeMaxFloat(MotionEvent.TOOL_TYPE_STYLUS));
            return min != max;
        }
    }
    public String readFromFile(int fileResource) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(activity.getResources().openRawResource(fileResource)));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                return sb.toString();
            } finally {
                br.close();
            }
        }catch (Exception e){
            return  e.toString();
        }
    }
    public String readFromAssets(Context context, String filename) {
        BufferedReader reader = null;
        String result = "";
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(filename)));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                result += mLine + "\n";
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    public String readFromAssetsWin1251(Context context, String filename) {
        BufferedReader reader = null;
        String result = "";
        try {
            reader = new BufferedReader( new InputStreamReader( context.getAssets().open(filename), "windows-1251"));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                result += mLine + "\n";
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    String readFromFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
    public String getResource(int id) {
        try{
            if(resourceCache.containsKey(id)){
                return resourceCache.get(id);
            }
            else{
                String result = activity.getResources().getString(id);
                resourceCache.put(id, result);
                return result;
            }
        }catch (Exception e){
            Logger.log("GlobalData.getResource", "Something goes wrong.\n"+ e + "\nStackTrace: \n" + getStackTrace(e), false);
        }
        return " (Error) ";
    }    //будем использовать эту хрень потому что эту лесенку не хочется строить везде
    public int getGridColor(){
        return getGridColor((Integer) Data.get(Data.backgroundColorInt()));
    }
    public int getGridColor(int color){
        int opacity = (Integer)Data.get(Data.gridOpacityInt());
        float gridOpacity = ((float)opacity)/100f;
        return getGridColor(color, gridOpacity);
    }
    public int getGridColor(int color, float difference){
        difference = Math.max(0f, difference);
        difference = Math.min(1f, difference);
        float alpha = Color.alpha(color);
        float red = Color.red(color);
        float green = Color.green(color);
        float blue = Color.blue(color);
        float avg = red*0.3f + green*0.59f + blue*0.11f;
        float max = Math.max(red, Math.max(green, blue));
        //Logger.log("Color="+String.format("#%06X", 0xFFFFFF & color) + " avg= " + avg + " red= " + red + " green= " + green + " blue= " + blue);

        if(avg > 120){//LIGHT
            red -= 300f*difference;
            green -= 300f*difference;
            blue -= 300f*difference;
//            red *= 1f-difference;
//            green *= 1f-difference;
//            blue *= 1f-difference;
        }
        else {  //DERK
            if(max > 200){
                red -= 380f*difference;
                green -= 380f*difference;
                blue -= 380f*difference;
            }
            else {
                red += 255f * difference;
                green += 255f * difference;
                blue += 255f * difference;
            }
        }

        red = Math.min(255, red);
        green = Math.min(255, green);
        blue = Math.min(255, blue);

        red = Math.max(0, red);
        green = Math.max(0, green);
        blue = Math.max(0, blue);
        color = Color.argb((int) alpha, (int) red, (int) green, (int) blue);
        //Logger.log("Color="+String.format("#%06X", 0xFFFFFF & color) + " red= " + red + " green= " + green + " blue= " + blue);
        return color;
    }
    public static boolean isLightColor(int color){
        float red = Color.red(color);
        float green = Color.green(color);
        float blue = Color.blue(color);
        float avg = red*0.3f + green*0.59f + blue*0.11f;
        float max = Math.max(red, Math.max(green, blue));
        if(avg > 120){//LIGHT
            return true;
        }
        else {  //DERK
            return (max > 200);
        }
    }
    public int getGridColor1(int color, float difference){
        try{
            //calculate gridColor
            float [] hsv=new float[3];                        //0 hue      1 saturaton    2 value
            final float threshold = 0.6f;
            float sum =
                    (float)Color.red(color) * 0.3f +
                            (float)Color.green(color) * 0.59f+
                            (float)Color.blue(color)*0.11f;
            Color.colorToHSV(color, hsv);
            //value
            if(sum > 180){//hsv[2] > threshold && hsv[1] < (1-threshold)) {
                //BRIGHT
                hsv[2] -= difference;
                if(hsv[2] < 0)
                    hsv[2] = 0;
            }
            else{
                //DARK
                hsv[2] += difference;
                if(hsv[2] > 1)
                    hsv[2] = 1;
            }
            hsv[1] -= difference;
            if(hsv[1] < 0)
                hsv[1] = 0;
            return Color.HSVToColor(hsv);
        }catch (Exception e){
            Logger.log("Data.tools.getGridColor", "Exception: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }catch (OutOfMemoryError e) {
            Logger.log("Data.tools.getGridColor", "OutOfMemoryError: " + e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        return Color.BLACK;
    }
    //декодирование файла с оптимизацией до определенного размера
    static public Bitmap decodeFile(String path, int required_w, int required_h) {
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
                options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }
            options.inJustDecodeBounds = false;
            // Decode bitmap with inSampleSize set
            Bitmap result=null;
            try {
                result=BitmapFactory.decodeFile(path, options);
            } catch (OutOfMemoryError e){
                Logger.log("GlobalData.decodeFile", getStackTrace(e), false);
            }
            return result;
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.decodeFile произошла ошибка ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }catch (OutOfMemoryError e) {
            Logger.log("Где-то в GlobalData.decodeFile Недостаточно памяти ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }
        return null;
    }
    //декодирование URI с оптимизацией до определенного размера
    static public Bitmap decodeFile(Context context, Uri uri, int required_w, int required_h) {
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            {
                InputStream ims = context.getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(ims, null, options);
                ims.close();
            }
            // Calculate inSampleSize
            final int height = options.outHeight;
            final int width = options.outWidth;
            if (height > required_h || width > required_w) {
                // Calculate ratios of height and width to requested height and width
                final int heightRatio = Math.round((float) height / required_h);
                final int widthRatio = Math.round((float) width /  required_w);
                // Choose the smallest ratio as inSampleSize value, this will guarantee a final image with both dimensions larger than or equal to the requested height and width.
                options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }
            options.inJustDecodeBounds = false;
            // Decode bitmap with inSampleSize set
            Bitmap result=null;
            try {
                InputStream ims = context.getContentResolver().openInputStream(uri);
                result=BitmapFactory.decodeStream(ims, null, options);
                ims.close();
                //result=BitmapFactory.decodeFile(path, options);
            } catch (OutOfMemoryError e){
                Logger.log("GlobalData.decodeFile", getStackTrace(e), false);
            }
            return result;
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.decodeFile произошла ошибка ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }catch (OutOfMemoryError e) {
            Logger.log("Где-то в GlobalData.decodeFile Недостаточно памяти ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }
        return null;
    }
    public static Bitmap decodeResource(Resources resources, int res, float required_w, float required_h) {
        return decodeResource(resources, res, (int) required_w, (int) required_h);
    }
    public static Bitmap decodeResource(Resources resources, int res, int required_w, int required_h) {
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(resources, res, options);
            // Calculate inSampleSize
            final int height = options.outHeight;
            final int width = options.outWidth;
            if (height > required_h || width > required_w) {
                // Calculate ratios of height and width to requested height and width
                final int heightRatio = Math.round((float) height / required_h);
                final int widthRatio = Math.round((float) width /  required_w);
                // Choose the smallest ratio as inSampleSize value, this will guarantee a final image with both dimensions larger than or equal to the requested height and width.
                int iss = heightRatio < widthRatio ? heightRatio : widthRatio;
                if(iss > 16) iss = 16;
                else if(iss > 8) iss = 8;
                else if(iss > 4) iss = 4;
                else if(iss > 2) iss = 2;
                options.inSampleSize = iss;
            }
            options.inJustDecodeBounds = false;
            // Decode bitmap with inSampleSize set
            Bitmap result=null;
            try {
                result=BitmapFactory.decodeResource(resources, res, options);

                result=Bitmap.createScaledBitmap(result,required_w, required_h, true);
            } catch (OutOfMemoryError e){
                Logger.log("GlobalData.decodeFile", getStackTrace(e), false);
            }
            return result;
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.decodeResource произошла ошибка ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }catch (OutOfMemoryError e) {
            Logger.log("Где-то в GlobalData.decodeResource Недостаточно памяти ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }
        return null;
    }   //декодирование файла с оптимизацией до определенного размера
    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        try{
            int width = bm.getWidth();
            int height = bm.getHeight();
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;
            // CREATE A MATRIX FOR THE MANIPULATION
            Matrix matrix = new Matrix();
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight);
            // "RECREATE" THE NEW BITMAP
            return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.getResizedBitmap произошла ошибка ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }catch (OutOfMemoryError e) {
            Logger.log("Где-то в GlobalData.getResizedBitmap Недостаточно памяти ", e + "\nStackTrace: \n" + getStackTrace(e), false);
        }
        return null;
    }
    public static Bitmap addBitmapBackground(Bitmap bm, int newBackground) throws OutOfMemoryError, Exception {
        int width = bm.getWidth();
        int height = bm.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(newBackground);
        canvas.drawBitmap(bm, 0, 0, new Paint());
        bm.recycle();
        return result;
    }
    public static Bitmap cropBitmap(Bitmap bm, RectF croppingCoefficient) throws OutOfMemoryError{
        int width = bm.getWidth();
        int height = bm.getHeight();
        Bitmap.Config config = bm.getConfig();
        Bitmap cropped = Bitmap.createBitmap(width, height, config);
        Canvas croppedCanvas = new Canvas(cropped);
        Paint p = new Paint();
        p.setColor(Color.BLACK);

        //draw mask
        float top = height * croppingCoefficient.top;
        float bottom = height - (height*croppingCoefficient.bottom);
        float left = width * croppingCoefficient.left;
        float right = width - (width * croppingCoefficient.right);
        RectF rectF = new RectF(left, top, right, bottom);
        croppedCanvas.drawRect(rectF, p);

        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        croppedCanvas.drawBitmap(bm, 0,0, p);
        return cropped;
    }
    //Parse name like: /sdcard/Pictures/sDraw/sDraw_2021-04-12_15-43-02.-65536.13.250.png, where -65536 is background int color, 13 is opacity, 250 is grid size
    public static int extractBackgroundColorFromFileName(String path){
        int background = Color.TRANSPARENT;
        String name = new File(path).getName();
        String[] parts = name.split("\\.");
        if(parts.length >= 3) {
            try {
                background = Integer.parseInt(parts[1]);
            }
            catch (Exception ignored){}
        }
        return background;
    }
    public static int extractGridOpacityFromFileName(String path){
        int result = -1;
        String name = new File(path).getName();
        String[] parts = name.split("\\.");
        if(parts.length >= 4) {
            try {
                result = Integer.parseInt(parts[2]);
            }
            catch (Exception ignored){}
        }
        return result;
    }
    public static int extractGridSizeFromFileName(String path){
        int result = -1;
        String name = new File(path).getName();
        String[] parts = name.split("\\.");
        if(parts.length >= 5) {
            try {
                result = Integer.parseInt(parts[3]);
            }
            catch (Exception ignored){}
        }
        return result;
    }
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    static public void sleep(int ms){
        try{
            Thread.sleep(ms);
        }
        catch (Exception e){}
    }
    public boolean isPaid(){
        //Показывает, была ли оплачена программа. При этом она может быть полной версией, но не оплаченной.
        //если программа не оплачена, показывать пользователю напоминания об испытательном сроке
        boolean paid = getResource(R.string.paid).equals("+");
        boolean force = (Boolean)Data.get(Data.forcePaidBoolean());
        paid = paid || force;
        return paid;
    }
    public boolean isFullVersion(){
        //Показывает, доступны ли пользователю полные функции
        try {
            if (paid == null) {
                paid = isPaid();
                if(!paid){
                    int remaining = (Integer) Data.get(Data.paidCounter());
                    if (remaining > 0) {
                        Date dateTick = Data.SIMPLE_DATE_FORMAT.parse(
                                (String)Data.get(Data.paidCounterTickDate())
                        );
                        if(!Tools.isToday(dateTick)) {
                            remaining --;
                            Data.save(remaining, Data.paidCounter());
                        }
                        Data.save(Data.SIMPLE_DATE_FORMAT.format(new Date()), Data.paidCounterTickDate());
                        //Logger.show(Data.activity.getString(R.string.demoModeRemaining).replace("#", String.valueOf(remaining)));
                        paid = true;
                    }
                }
            }
            return paid;
        }
        catch (Exception e){
            return false;
        }
    }
    Dialog buyFullDialog = null;
    public void showBuyFullDialog(){
        if(buyFullDialog != null)
            return;
        Context context = activity;
        int menuBackgroundColor = Color.rgb(39,50,56);

        int textSize = 17;
        Dialog dialog = buyFullDialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        LinearLayout linearLayoutV = new LinearLayout(context);
        linearLayoutV.setPadding((int)Data.store().DPI/15, (int)Data.store().DPI/15, (int)Data.store().DPI/15, (int)Data.store().DPI/15);
        linearLayoutV.setBackgroundColor(menuBackgroundColor);
        linearLayoutV.setOrientation(LinearLayout.VERTICAL);
        dialog.addContentView(linearLayoutV, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                return (Boolean)Data.get(Data.volumeButtonsBoolean()) && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP);
            }
        });

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.ic_lock);
        imageView.setPadding(0,0,(int)Data.store().DPI/12, 0);

        TextView textView = new TextView(context);
        textView.setTextSize(textSize);
        textView.setTextColor(Color.WHITE);
        textView.setText(Data.tools.getResource(R.string.paidFunction));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setVerticalGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(imageView, new LinearLayout.LayoutParams((int)Data.store().DPI/3, (int)Data.store().DPI/5));
        linearLayout.addView(textView);
        linearLayoutV.addView(linearLayout);

        LinearLayout linearLayoutH = new LinearLayout(context);
        linearLayoutV.addView(linearLayoutH);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(Tools.dp(3), Tools.dp(30), Tools.dp(3), Tools.dp(10));

        AnimatedButton buttonY = new AnimatedButton(context);
        linearLayoutH.addView(buttonY);
        buttonY.setText(R.string.buy_pro);
        buttonY.setTextSize(textSize);
        buttonY.setLayoutParams(lp);
        buttonY.setBackgroundDynamicColor(() -> Color.rgb(50, 105, 80));
        buttonY.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(buyFullDialog != null) {
                    buyFullDialog.cancel();
                    buyFullDialog = null;
                }
                openFullVersionMarket();
            }
        });

        AnimatedButton buttonR = new AnimatedButton(context);
        linearLayoutH.addView(buttonR);
        buttonR.setText(R.string.reset_trial);
        buttonR.setTextSize(textSize);
        buttonR.setLayoutParams(lp);
        buttonR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buyFullDialog != null) {
                    buyFullDialog.cancel();
                    buyFullDialog = null;
                }
                context.startActivity(new Intent(context, FullVersionInfoActivity.class));
            }
        });

        TextView buttonN = new AnimatedButton(context);
        linearLayoutH.addView(buttonN);
        buttonN.setText(R.string.cancel);
        buttonN.setTextSize(textSize);
        buttonN.setLayoutParams(lp);
        buttonN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buyFullDialog != null) {
                    buyFullDialog.cancel();
                    buyFullDialog = null;
                }
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                buyFullDialog = null;
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                buyFullDialog = null;
            }
        });

        dialog.show();
    }
    public void openFullVersionMarket(){
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=com.fsoft.FP_sDraw_paid"));
            activity.startActivity(intent);
        }catch (Exception e){
            Logger.log("GlobalData.openFullVersionMarket", "Exception: " + e + "\nStackTrace: \n" + getStackTrace(e), false);
        }catch (OutOfMemoryError e) {
            Logger.log("GlobalData.openFullVersionMarket", "OutOfMemoryError: " + e + "\nStackTrace: \n" + getStackTrace(e), false);
        }
    }
    public static int getScreenOrientation(Context context){
        try{
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                return ORIENTATION_VERTICAL;
            else if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                return ORIENTATION_HORIZONTAL;
        }catch (Exception|OutOfMemoryError e){
            Logger.log(e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
        }
        return -1;
    }
    public boolean isAllowedDevice(MotionEvent event){
        boolean onlySPen = (Boolean)Data.get(Data.sPenOnlyBoolean());
        //Logger.log("onlySPen = " + onlySPen);
        if(!onlySPen)
            return true;
        if(event == null)
            return true;
        if(Build.VERSION.SDK_INT < 14)
            return true;
        else {
            int devID = event.getToolType(0);
            return
                    devID == MotionEvent.TOOL_TYPE_STYLUS/*like sPen*/
                            || devID == MotionEvent.TOOL_TYPE_ERASER /*Like HP's stylus on galaxy*/
                            || devID == MotionEvent.TOOL_TYPE_UNKNOWN; /*generated by AccurateBrush event*/
        }
    }

    public boolean isAllowedDeviceForUi(MotionEvent event){
        boolean onlySPen = (Boolean)Data.get(Data.sPenOnlyUiBoolean());
        //Logger.log("onlySPen = " + onlySPen);
        if(!onlySPen)
            return true;
        if(event == null)
            return true;
        if(Build.VERSION.SDK_INT < 14)
            return true;
        else {
            int devID = event.getToolType(0);
            return
                    devID == MotionEvent.TOOL_TYPE_STYLUS/*like sPen*/
                            || devID == MotionEvent.TOOL_TYPE_ERASER /*Like HP's stylus on galaxy*/
                            || devID == MotionEvent.TOOL_TYPE_UNKNOWN; /*generated by AccurateBrush event*/
        }
    }
    public static Bitmap getBitmapContour(Bitmap in, int neededColor){
        try {
            Bitmap newBitmap = in.copy(Bitmap.Config.ARGB_8888, true);
            for (int x = 0; x < newBitmap.getWidth(); x++) {
                for (int y = 0; y < newBitmap.getHeight(); y++) {
                    int cur = newBitmap.getPixel(x, y);
                    if (Color.alpha(cur) > 2) {
                        int newPixel = Color.argb(
                                (int)(Color.alpha(cur)*(Color.alpha(neededColor)/255f)),
                                Color.red(neededColor),
                                Color.green(neededColor),
                                Color.blue(neededColor)
                        );
                        newBitmap.setPixel(x, y, newPixel);
                    }
                }
            }
            return newBitmap;
        }
        catch (Throwable e){
            Logger.log(e);
            return in;
        }
    }
    public static boolean isSameDay(Date date1, Date date2){
        if(date1 == null && date2 == null)
            return true;
        if(date1 != null && date2 == null)
            return false;
        if(date1 == null && date2 != null)
            return false;
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        return sameDay;
    }
    public static boolean isToday(Date date){
        return isSameDay(date, new Date());
    }
    public static void vibrate(View view){
        //Logger.log("Vibrate: " + ms);
        try {
            if((Boolean)Data.get(Data.enableVibrationBoolean())) {
                view.setHapticFeedbackEnabled(true);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            //((Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
        }
        catch (Throwable e){
            e.printStackTrace();
        }
    }
    public static void vibrateVirtualKeyDown(View view){
        //Logger.log("Vibrate: " + ms);
        try {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            //((Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
        }
        catch (Throwable e){
            e.printStackTrace();
        }
    }
    public static void vibrateVirtualKeyUp(View view){
        //Logger.log("Vibrate: " + ms);
        try {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
            //((Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
        }
        catch (Throwable e){
            e.printStackTrace();
        }
    }
    public static int dp(int dp){
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
    public static int sp(int sp){
        return (int) (sp * Resources.getSystem().getDisplayMetrics().scaledDensity);
    }
    public static float map(float valueCoord1,
                            float startCoord1, float endCoord1,
                            float startCoord2, float endCoord2) {
        double EPSILON = 1e-12;

        if (Math.abs(endCoord1 - startCoord1) < EPSILON) {
            throw new ArithmeticException("/ 0");
        }

        float offset = startCoord2;
        float ratio = (endCoord2 - startCoord2) / (endCoord1 - startCoord1);
        return ratio * (valueCoord1 - startCoord1) + offset;
    }
    public static int removeTransparency(int color){
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.rgb(red, green, blue);
    }
    public static Path getRectCenterPath(float cx, float cy, float radius){
        int left = (int)cx-(int)radius;
        int top = (int)cy-(int)radius;
        int right = (int)cx+(int)radius;
        int bottom = (int)cy+(int)radius;

        Path path = new Path();
        path.moveTo(left, top);
        path.lineTo(right, top);
        path.lineTo(right, bottom);
        path.lineTo(left, bottom);
        path.close();

        return path;
    }
    public static Path getSquircleCenterPath(float cx, float cy, float radius){
        int left = (int)cx-(int)radius;
        int top = (int)cy-(int)radius;

        return getSquirclePath(left, top, (int)radius);
    }
    public static float getSquircleCenterSum(float cx, float cy, float radius){
        return cx+cy+radius;
    }
    public static Path getSquirclePath(int left, int top, int radius){
        //Formula: (|x|)^3 + (|y|)^3 = radius^3
        final double radiusToPow = radius * radius * radius;

        Path path = new Path();
        path.moveTo(-radius, 0);
        for (int x = -radius ; x <= radius ; x+=1)
            path.lineTo(x, ((float) Math.cbrt(radiusToPow - Math.abs(x * x * x))));
        for (int x = radius ; x >= -radius ; x-=1)
            path.lineTo(x, ((float) -Math.cbrt(radiusToPow - Math.abs(x * x * x))));
        path.close();

        Matrix matrix = new Matrix();
        matrix.postTranslate(left + radius, top + radius);
        path.transform(matrix);

        return path;
    }
    public static int countLines(String str) {
        if(str == null || str.length() == 0)
        {
            return 0;
        }
        int lines = 1;
        int pos = 0;
        while ((pos = str.indexOf("\n", pos) + 1) != 0) {
            lines++;
        }
        return lines;
    }
    public static Bitmap flipX(Bitmap src)
    {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        //dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        //return dst;
    }
    public static Bitmap flipY(Bitmap src)
    {
        Matrix m = new Matrix();
        m.preScale(1, -1);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        //dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        //return dst;
    }
    public float getDisplayRefreshRate(){
        try{
            Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            return display.getRefreshRate();
        }
        catch (Exception e){
            Logger.log(e);
            return 30;
        }
    }
    public static void setNavBarForeground(boolean light, Activity activity, View view){
        try {
            if (!light) {//make buttons dark to be more visible
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    WindowInsetsController windowInsetController = activity.getWindow().getInsetsController();
                    if (windowInsetController != null)
                        windowInsetController.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                }
            } else { //make buttons light to be more visible
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    WindowInsetsController windowInsetController = activity.getWindow().getInsetsController();
                    if (windowInsetController != null)
                        windowInsetController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    view.setSystemUiVisibility(0);
                }
            }
        }
        catch (Exception e){
            Logger.log(e);
        }
    }
}
