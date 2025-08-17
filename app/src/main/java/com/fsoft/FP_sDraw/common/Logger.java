package com.fsoft.FP_sDraw.common;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.fsoft.FP_sDraw.instruments.DebugDataProvider;

/**
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 02.05.13
 * Time: 23:14
 */
public class Logger {
    static String TAG="sDraw";
    static Toast currentToast;
    static Handler handler = new Handler();


    public static void show(final String text){
        Log.d(TAG, "Toast: " + text);
        handler.post(() -> {
            try {
                if (currentToast != null)
                    currentToast.cancel();
                currentToast = Toast.makeText(Data.store().activity, text, Toast.LENGTH_SHORT);
                currentToast.show();
            }catch (Exception e){
                Log.d(TAG, "Error: " + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)));
            }
        });
    }
    public static void show(final int textId){
        Log.d(TAG, "Toast text ID: " + textId);
        handler.post(() -> {
            try {
                if (currentToast != null)
                    currentToast.cancel();
                currentToast = Toast.makeText(Data.store().activity, textId, Toast.LENGTH_SHORT);
                currentToast.show();
            }catch (Exception e){
                Log.d(TAG, "Error: " + Tools.getStackTrace(e));
            }
        });
    }
    public static void log(Throwable e){
        log(e.getMessage());
        log(Tools.getStackTrace(e));
    }
    public static void log(String text){
        try {
            Log.d(TAG, text);
            if((Boolean)Data.get(Data.debugBoolean())){
                if(text.contains("\n")) {
                    for (String line : text.split("\n"))
                        sendToService(line);
                }
                else {
                    sendToService(text);
                }
            }
        }
        catch (Throwable e){
            Log.d(Logger.TAG, "Error (log): " + e);
            Log.d(Logger.TAG, Data.tools == null ? e.toString() : Tools.getStackTrace(e));
        }
    }
    public static void log(String from, int textId, boolean display) {
        log(from, Data.store().activity.getString(textId), display);
    }
    public static void log(String from, String text, boolean display) {
        try{
            Log.d(TAG, from + " : " + text);
            if(display)
                show(text);
            if((Boolean)Data.get(Data.debugBoolean())){
                sendToService(from + ": " + text);
            }
        }catch (Exception e){
            Log.e(TAG, "Error while LOG!!!");
            e.printStackTrace();
        }catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
    }
    static void sendToService(String text){
        DebugDataProvider.addToLog(text);
//        if(Data.store() != null && Data.store().activity != null) {
//            Data.store().activity.startService(new Intent(Data.store().activity, LoggerOverlayService.class).putExtra("text", text));
//        }
    }
    public static void messageBox(String text){
        messageBox(text, null);
    }
    static void messageBox(final String text, final DialogInterface.OnClickListener listener){
        handler.post(() -> {

            try{
                AlertDialog.Builder builder = new AlertDialog.Builder(Data.store().activity);
                //builder.setTitle(Data.tools.getResource(R.string.AlertHeader));
                builder.setMessage(text);
                builder.setPositiveButton("OK", listener);
                //предотвратить срабатывание клавиш громкости
                builder.setOnKeyListener((dialogInterface, i, keyEvent) -> keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP);
                builder.setCancelable(false);
                builder.show();
            }catch (Exception e){
                Log.e(TAG, "Error while messageBox!!!");
                e.printStackTrace();
            }catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        });
    }
    public static void fuckIt(){
        if(currentToast != null)
            currentToast.cancel();
        currentToast = null;
    }
}
