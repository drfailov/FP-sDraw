package com.fsoft.FP_sDraw.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.fsoft.FP_sDraw.DrawCore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * class for shared preferences
 * Created by Dr. Failov on 08.06.2014.
 */

public class Data {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final int MANAGE_METHOD_PRESSURE =1;
    public static final int MANAGE_METHOD_SIZE =5;
    public static final int MANAGE_METHOD_SPEED=2;
    public static final int MANAGE_METHOD_SPEED_INVERSE=4;
    public static final int MANAGE_METHOD_CONSTANT=3;

    public static final int TYPE_INT = 0;
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_STRING = 2;
    public static final int TYPE_FLOAT = 3;

    /*
    * В этих функциях применена ленивая инициализация поскольку некоторые из них вызываются
    * многократно, в циклах а также перерисовке.
    * Поскольку без ленивой инициализации они представляют собой операции выделения памяти,
    * есть смысл их инициализировать и не выделять повторно
    * */
    static private Object[] backgroundColorInt = null;
    static public Object[] backgroundColorInt(){
        if (backgroundColorInt == null) {
            backgroundColorInt = new Object[]{TYPE_INT,-16765405,"background_color"};
        }
        return backgroundColorInt;
    }
    static public int getBackgroundColor(){
        return (Integer)Data.get(Data.backgroundColorInt());
    }
    static private Object[] brushColorInt = null;
    static public Object[] brushColorInt(){
        if (brushColorInt == null) {
            brushColorInt = new Object[]{TYPE_INT,-1,"brush_color"};
        }
        return brushColorInt;
    }
    static private Object[] brushSizeInt = null;
    static public Object[] brushSizeInt(){
        if (brushSizeInt == null) {
            brushSizeInt = new Object[]{TYPE_INT, Tools.dp(5), "brush_size"};
        }
        return brushSizeInt;
    }
    static public Object[] brushOpacityInt = null;
    static public Object[] brushOpacityInt(){
        if (brushOpacityInt == null) {
            brushOpacityInt = new Object[]{TYPE_INT, 255, "brush_opacity"};
        }
        return brushOpacityInt;
    }
    static public Object[] eraserSizeInt(){return new Object[]{TYPE_INT, (int)store.DPI/5, "eraser_size"};}
    static public Object[] gridSizeInt(){return new Object[]{TYPE_INT, (int)store.DPI/8, "grid_size"};}
    static public Object[] gridOpacityInt(){return new Object[]{TYPE_INT, 6, "grid_opacity"}; /*1...99*/}
    static public int getManageMethod(){
        return (Integer)Data.get(Data.manageMethodInt());
    }
    static public Object[] manageMethodInt(){return new Object[]{TYPE_INT, MANAGE_METHOD_SPEED, "manage_method"};}
    static public Object[] gridVerticalBoolean(){return new Object[]{TYPE_BOOLEAN, true, "grid_vertical"};}
    static public Object[] antialiasingBoolean(){return new Object[]{TYPE_BOOLEAN, true, "antialiasing"};}
    static public Object[] fingerHoverBoolean() {return new Object[]{TYPE_BOOLEAN, false, "fingerHover"};}
    static public Object[] smoothingBoolean(){return new Object[]{TYPE_BOOLEAN, true, "smoothing"};}
    static public int getOrientationCanvas(){
        return (Integer)Data.get(Data.orientationCanvasInt());
    }
    static public Object[] orientationCanvasInt(){return new Object[]{TYPE_INT, DrawCore.OrientationProvider.ORIENTATION_AUTO, "orientation_canvas"};}
    static public Object[] showScaleButtonsBoolean(){return new Object[]{TYPE_BOOLEAN, false, "showScaleButtons"};}
    static public Object[] volumeButtonsBoolean(){return new Object[]{TYPE_BOOLEAN, true, "volumeKeys"};} 
    static public Object[] menuStripBoolean(){return new Object[]{TYPE_BOOLEAN, true, "menuStrip"};}
    static public Object[] statusBarBoolean(){return new Object[]{TYPE_BOOLEAN, false, "statusBar"};} 
    static public Object[] paletteStripBoolean(){return new Object[]{TYPE_BOOLEAN, false, "paletteStrip", "{\"nonPaid\":false}"};}
    static public Object[] watermarkBoolean(){return new Object[]{TYPE_BOOLEAN, true, "addWatermark", "{\"nonPaid\":true}"};}
    static public Object[] backKeyUndoBoolean(){return new Object[]{TYPE_BOOLEAN, true, "backKeyUndo"};} 
    static public Object[] keepScreenOnBoolean(){return new Object[]{TYPE_BOOLEAN, false, "keepScreenOn"};}
    static public Object[] maximumBrightnessBoolean(){return new Object[]{TYPE_BOOLEAN, false, "maximumBrightness"};}
    static public Object[] einkClean(){return new Object[]{TYPE_BOOLEAN, false, "einkClean"};}
    static public Object[] enableHardwareAcceleration(){return new Object[]{TYPE_BOOLEAN, true, "enableHardwareAcceleration"};}
    static public Object[] paidCounter(){return new Object[]{TYPE_INT, 20, "paid_counter"};}
    static public Object[] paidCounterTickDate(){return new Object[]{TYPE_STRING, SIMPLE_DATE_FORMAT.format(new Date()), "paid_counter_tick_date"};}
    static public Object[] twoFingersZoomBoolean(){return new Object[]{TYPE_BOOLEAN, true, "twoFingersZoom"};}
    static public Object[] showHelpMessagesBoolean(){return new Object[]{TYPE_BOOLEAN, true, "showHelpMessages"};}
    static public Object[] mosaicSizeInt(){return new Object[]{TYPE_INT, (int)store.DPI/13, "mosaic_size"};}
    static public Object[] fillThresholdInt(){return new Object[]{TYPE_INT, 5, "fill_threshold"};}
    static public Object[] saveFixedSizeLastWidthInt(){return new Object[]{TYPE_INT, 400, "save_fixed_size_last_width"};}
    static public Object[] saveFixedSizeLastHeightInt(){return new Object[]{TYPE_INT, 300, "save_fixed_size_last_height"};}
    static public Object[] interfaceFormInt(){return new Object[]{TYPE_INT, 0, "interface_form"};}
    static public void setSquircle(){
        save(0, interfaceFormInt());
    }
    static public void setCircle(){
        save(1, interfaceFormInt());
    }
    static public void setRect(){
        save(2, interfaceFormInt());
    }
    static public boolean isSqircle(){
        return (Integer) get(interfaceFormInt()) == 0;
    }
    static public boolean isCircle(){
        return (Integer) get(interfaceFormInt()) == 1;
    }
    static public boolean isRect(){
        return (Integer) get(interfaceFormInt()) == 2;
    }

    static private Object[] pressureMaxFloat = null;
    static public Object[] pressureMaxFloat(int tool){
        if (pressureMaxFloat == null) {
            pressureMaxFloat = new Object[]{TYPE_FLOAT, -1f, "pressureMax"+tool};
        }
        else
            pressureMaxFloat[2] = "pressureMax"+tool;
        return pressureMaxFloat;
    }
    static private Object[] pressureMinFloat = null;
    static public Object[] pressureMinFloat(int tool){
        if (pressureMinFloat == null) {
            pressureMinFloat = new Object[]{TYPE_FLOAT, -1f, "pressureMin"+tool};
        }
        else
            pressureMinFloat[2] = "pressureMin"+tool;
        return pressureMinFloat;
    }

    static public Object[] sizeMaxFloat(int tool){return new Object[]{TYPE_FLOAT, -1f, "sizeMax"+tool};}
    static public Object[] sizeMinFloat(int tool){return new Object[]{TYPE_FLOAT, -1f, "sizeMin"+tool};}
    static public Object[] debugBoolean(){return new Object[]{TYPE_BOOLEAN, false, "debug"};}
    static public Object[] showScreenMenuButtonBoolean(){return new Object[]{TYPE_BOOLEAN, false, "showScreenMenuKey"};}
    static public Object[] forcePaidBoolean(){return new Object[]{TYPE_BOOLEAN, false, "fp"};}
    static public Object[] sPenOnlyBoolean(){return new Object[]{TYPE_BOOLEAN, false, "sPenOnly"};}
    static public Object[] sPenOnlyUiBoolean(){return new Object[]{TYPE_BOOLEAN, false, "sPenOnlyUi"};} //реагировать только на перо  кнопками в інтерфейсі
    static public Object[] debugTextSizeInt(){return new Object[]{TYPE_INT, Math.max(10, (int)store.DPI/20), "dedugTextSize"};}
    static public Object[] useCanvasDrawingBoolean(){return new Object[]{TYPE_BOOLEAN, false, "canvasDrawing"};}
    static public Object[] itemPositionXFloat(String name, float def){return new Object[]{TYPE_FLOAT, def, "item_"+name+"_PositionXc"};}
    static public Object[] itemPositionYFloat(String name, float def){return new Object[]{TYPE_FLOAT, def, "item_"+name+"_PositionYc"};}
    static public Object[] showInstrumentsSelectorBoolean(){return new Object[]{TYPE_BOOLEAN, false, "showInstrumentsSelector"};}
    static public Object[] showColorsSelectorBoolean(){return new Object[]{TYPE_BOOLEAN, true, "showColorsSelector", "{\"nonPaid\":false}"};}
    static public Object[] smoothingSensibilityFloat(){ return new Object[]{TYPE_FLOAT, 0.30f, "smoothingSensibility"}; }
    static public Object[] enableVibrationBoolean(){return new Object[]{TYPE_BOOLEAN, true, "enableVibration"};}
    static public void savePaletteBrush(int[] newPalette){
        if(prefs == null) {
            Logger.log("Data.savePaletteBrush(): prefs is NULL, can't save");
            return;
        }
        if(newPalette == null) {
            Logger.log("Data.savePaletteBrush(): newPalette is NULL, can't save");
            return;
        }
        if(newPalette.length == 0) {
            Logger.log("Data.savePaletteBrush(): newPalette is empty, can't save");
            return;
        }
        try{
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < newPalette.length; i++) {
                int intColor = newPalette[i];
                String hexColor = String.format("#%06X", (0xFFFFFF & intColor));
                jsonArray.put(hexColor);
            }
            String result = jsonArray.toString();
            Logger.log("Data.savePaletteBrush(): Result: " + result);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("PaletteBrush", result);
            edit.commit();
        }
        catch (Exception e){
            Logger.log("Data.savePaletteBrush(): Error: " + Tools.getStackTrace(e));
        }
    }
    static public int[] getPaletteBrush(){
        try{
            if(prefs == null) {
                Logger.log("Data.getPaletteBrush(): prefs is NULL, return default");
                return getDefaultPaletteBrush();
            }
            if(!prefs.contains("PaletteBrush")) {
                //Logger.log("Data.getPaletteBrush(): prefs don't have PaletteBrush, return default");
                return getDefaultPaletteBrush();
            }
            String paletteBrushString = prefs.getString("PaletteBrush", "[]");
            JSONArray jsonArray = new JSONArray(paletteBrushString);
            if(jsonArray.length() == 0){
                Logger.log("Data.getPaletteBrush(): PaletteBrush array is empty, return default");
                return getDefaultPaletteBrush();
            }
            ArrayList<Integer> resultList = new ArrayList<>();
            for(int i=0; i<jsonArray.length(); i++){
                String colorCode = jsonArray.getString(i);
                try {
                    int color = Color.parseColor(colorCode);
                    resultList.add(color);
                }
                catch (Exception e){
                    Logger.log("Data.getPaletteBrush(): Color code " + colorCode + " can't be parsed");
                }
            }
            //copy list to array
            int[] result = new int[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                result[i] = resultList.get(i);
            }
            return result;
        }
        catch (Exception e){
            return getDefaultPaletteBrush();
        }
    }
    static public int[] getDefaultPaletteBrush(){
        return new int[]{
                Color.parseColor("#ffffff"),
                Color.parseColor("#b5b6b5"),
                Color.parseColor("#494949"),
                Color.parseColor("#000000"),
                Color.parseColor("#3a1f04"),
                Color.parseColor("#54190b"),
                Color.parseColor("#ee4056"),
                Color.parseColor("#ff0000"),
                Color.parseColor("#f88900"),
                Color.parseColor("#ffac82"),
                Color.parseColor("#f8f378"),
                Color.parseColor("#ffff00"),
                Color.parseColor("#c6ff00"),
                Color.parseColor("#00ff00"),
                Color.parseColor("#00822c"),
                Color.parseColor("#003e2f"),
                Color.parseColor("#2c736b"),
                Color.parseColor("#00ff99"),
                Color.parseColor("#00ffff"),
                Color.parseColor("#21a8ef"),
                Color.parseColor("#0078bf"),
                Color.parseColor("#4e75a0"),
                Color.parseColor("#0255a5"),
                Color.parseColor("#0000ff"),
                Color.parseColor("#23365e"),
                Color.parseColor("#19138f"),
                Color.parseColor("#54007e"),
                Color.parseColor("#4d3d6f"),
                Color.parseColor("#ab1742"),
                Color.parseColor("#ff00ff"),
                Color.parseColor("#ff6ebc"),
                Color.parseColor("#fc9c9f")
        };
    }

    static public void savePaletteBackground(int[] newPalette){
        if(prefs == null) {
            Logger.log("Data.savePaletteBackground(): prefs is NULL, can't save");
            return;
        }
        if(newPalette == null) {
            Logger.log("Data.savePaletteBackground(): newPalette is NULL, can't save");
            return;
        }
        if(newPalette.length == 0) {
            Logger.log("Data.savePaletteBackground(): newPalette is empty, can't save");
            return;
        }
        try{
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < newPalette.length; i++) {
                int intColor = newPalette[i];
                String hexColor = String.format("#%06X", (0xFFFFFF & intColor));
                jsonArray.put(hexColor);
            }
            String result = jsonArray.toString();
            Logger.log("Data.savePaletteBackground(): Result: " + result);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("PaletteBackground", result);
            edit.commit();
        }
        catch (Exception e){
            Logger.log("Data.savePaletteBackground(): Error: " + Tools.getStackTrace(e));
        }
    }
    static public int[] getPaletteBackground(){
        try{
            if(prefs == null) {
                Logger.log("Data.getPaletteBackground(): prefs is NULL, return default");
                return getDefaultPaletteBackground();
            }
            if(!prefs.contains("PaletteBackground")) {
                Logger.log("Data.getPaletteBackground(): prefs don't have PaletteBackground, return default");
                return getDefaultPaletteBackground();
            }
            String paletteBackgroundString = prefs.getString("PaletteBackground", "[]");
            JSONArray jsonArray = new JSONArray(paletteBackgroundString);
            if(jsonArray.length() == 0){
                Logger.log("Data.getPaletteBackground(): PaletteBackground array is empty, return default");
                return getDefaultPaletteBackground();
            }
            ArrayList<Integer> resultList = new ArrayList<>();
            for(int i=0; i<jsonArray.length(); i++){
                String colorCode = jsonArray.getString(i);
                try {
                    int color = Color.parseColor(colorCode);
                    resultList.add(color);
                }
                catch (Exception e){
                    Logger.log("Data.getPaletteBackground(): Color code " + colorCode + " can't be parsed");
                }
            }
            //copy list to array
            int[] result = new int[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                result[i] = resultList.get(i);
            }
            return result;
        }
        catch (Exception e){
            return getDefaultPaletteBackground();
        }
    }
    public static int[] getDefaultPaletteBackground(){
        return new int[]{
                Color.parseColor("#ffb5b5"), //red
                Color.parseColor("#2b0000"), //red
                Color.parseColor("#ffb5eb"), //violet
                Color.parseColor("#2b0026"), //violet
                Color.parseColor("#b7b5ff"), //blue
                Color.parseColor("#00002b"), //blue
                Color.parseColor("#b5fcff"), //cyan
                Color.parseColor("#00292b"), //cyan
                Color.parseColor("#bcffb5"), //green
                Color.parseColor("#001a00"), //green
                Color.parseColor("#feffb5"), //yellow
                Color.parseColor("#2b2700"), //yellow
                Color.parseColor("#ffdab5"), //brown
                Color.parseColor("#241300"), //brown
                Color.parseColor("#ffffff"), //white
                Color.parseColor("#343434"), //white
                Color.parseColor("#b6b6b6"), //grey
                Color.parseColor("#000000") //black
        };
    }


    static public void saveOnscreenInstrumentList(ArrayList<String> newOnScreenInstrumentList){
        if(prefs == null) {
            Logger.log("Data.saveOnscreenInstrumentList(): prefs is NULL, can't save");
            return;
        }
        if(newOnScreenInstrumentList == null) {
            Logger.log("Data.saveOnscreenInstrumentList(): newOnScreenInstrumentList is NULL, can't save");
            return;
        }
        if(newOnScreenInstrumentList.size() == 0) {
            Logger.log("Data.saveOnscreenInstrumentList(): newOnScreenInstrumentList is empty, can't save");
            return;
        }
        try{
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < newOnScreenInstrumentList.size(); i++) {
                jsonArray.put(newOnScreenInstrumentList.get(i));
            }
            String result = jsonArray.toString();
            Logger.log("Data.saveOnscreenInstrumentList(): Result: " + result);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("OnscreenInstrumentList", result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                edit.apply();
            else
                edit.commit();
        }
        catch (Exception e){
            Logger.log("Data.saveOnscreenInstrumentList(): Error: " + Tools.getStackTrace(e));
        }
    }
    static public ArrayList<String> getOnscreenInstrumentList(){
        try{
            if(prefs == null) {
                Logger.log("Data.getOnscreenInstrumentList(): prefs is NULL, return default");
                return getDefaultOnscreenInstrumentList();
            }
            if(!prefs.contains("OnscreenInstrumentList")) {
                Logger.log("Data.getOnscreenInstrumentList(): prefs don't have OnscreenInstrumentList, return default");
                return getDefaultOnscreenInstrumentList();
            }
            String OnscreenInstrumentListString = prefs.getString("OnscreenInstrumentList", "[]");
            JSONArray jsonArray = new JSONArray(OnscreenInstrumentListString);
            if(jsonArray.length() == 0){
                Logger.log("Data.getOnscreenInstrumentList(): OnscreenInstrumentList array is empty, return default");
                return getDefaultOnscreenInstrumentList();
            }
            ArrayList<String> resultList = new ArrayList<>();
            for(int i=0; i<jsonArray.length(); i++){
                String instrumentCode = jsonArray.getString(i);
                resultList.add(instrumentCode);
            }
            if(resultList.isEmpty())
                return getDefaultOnscreenInstrumentList();
            return resultList;
        }
        catch (Exception e){
            return getDefaultOnscreenInstrumentList();
        }
    }
    public static ArrayList<String> getDefaultOnscreenInstrumentList(){
        ArrayList<String> result = new ArrayList<>();
        result.add("brush");
        result.add("eraser");
        result.add("fill");
        result.add("text");
        result.add("selectandmove");
        result.add("line");
        return result;
    }




    static public void saveMenuStripList(ArrayList<String> newMenuStripListList){
        if(prefs == null) {
            Logger.log("Data.saveMenuStripList(): prefs is NULL, can't save");
            return;
        }
        if(newMenuStripListList == null) {
            Logger.log("Data.saveMenuStripList(): newMenuStripListList is NULL, can't save");
            return;
        }
        if(newMenuStripListList.size() == 0) {
            Logger.log("Data.saveMenuStripList(): newMenuStripListList is empty, can't save");
            return;
        }
        try{
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < newMenuStripListList.size(); i++) {
                jsonArray.put(newMenuStripListList.get(i));
            }
            String result = jsonArray.toString();
            Logger.log("Data.saveMenuStripList(): Result: " + result);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("MenuStripList", result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                edit.apply();
            else
                edit.commit();
        }
        catch (Exception e){
            Logger.log("Data.saveMenuStripList(): Error: " + Tools.getStackTrace(e));
        }
    }
    static public ArrayList<String> getMenuStripList(){
        try{
            if(prefs == null) {
                Logger.log("Data.getMenuStripList(): prefs is NULL, return default");
                return getDefaultMenuStripList();
            }
            if(!prefs.contains("MenuStripList")) {
                Logger.log("Data.getMenuStripList(): prefs don't have MenuStripList, return default");
                return getDefaultMenuStripList();
            }
            String MenuStripListString = prefs.getString("MenuStripList", "[]");
            JSONArray jsonArray = new JSONArray(MenuStripListString);
            if(jsonArray.length() == 0){
                Logger.log("Data.getMenuStripList(): MenuStripList array is empty, return default");
                return getDefaultMenuStripList();
            }
            ArrayList<String> resultList = new ArrayList<>();
            for(int i=0; i<jsonArray.length(); i++){
                String instrumentCode = jsonArray.getString(i);
                resultList.add(instrumentCode);
            }
            if(resultList.isEmpty())
                return getDefaultMenuStripList();
            return resultList;
        }
        catch (Exception e){
            return getDefaultMenuStripList();
        }
    }
    public static ArrayList<String> getDefaultMenuStripList(){
        ArrayList<String> result = new ArrayList<>();
        //"menu","undo","redo",
        result.add("menu");
        result.add("undo");
        result.add("redo");
        // "brush","eraser","fill","mosaic","text",
        result.add("brush");
        result.add("eraser");
        result.add("fill");
        result.add("mosaic");
        result.add("text");
        // "selection","figures","line","pipette","accurate"]
        result.add("selection");
        result.add("figures");
        result.add("line");
        result.add("pipette");
        result.add("accurate");
        return result;
    }

    static Activity activity;
    public static SharedPreferences prefs;
    private static Store store;
    static public Tools tools;
    public static HashMap<String, Object> cache;
    static HashMap<String, Timer> timers;

    public static void init(Activity _act){
        activity = _act;
        prefs = activity.getPreferences(Context.MODE_PRIVATE);
        tools = new Tools(activity);
        cache = new HashMap<>();
        timers = new HashMap<>();
    }
    //example: Data.save(brush, Data.backgroundColorInt());
    public static boolean save(Object data, Object[] tag){
        return save(data, tag, false);
    }
    public static boolean save(Object data, Object[] tag, boolean saveImmediately){
        boolean ok = true;
        try{
            int type = (Integer)tag[0];
            Object def = tag[1];
            String name = (String)tag[2];
            //put into cache
            cache.put(name, data);
            if(!saveImmediately) {
                //schedule timer
                if (timers.containsKey(name))
                    timers.get(name).cancel();
                Timer timer = new Timer();
                timers.put(name, timer);
                timer.schedule(new MyTimerTask(data, name, def, type), 2000);
            }
            if(saveImmediately){
                new MyTimerTask(data, name, def, type).run();
            }
        }catch (Exception e){
            e.printStackTrace();
            ok = false;
        }
        return ok;
    }
    public static Store store(){
        if(store == null)
            store = new Store(activity);
        return store;
    }

    static class MyTimerTask extends TimerTask{
        Object data;
        int type;
        Object def;
        String name;

        protected MyTimerTask(Object _data, String _name, Object _def, int _type) {
            super();
            data = _data;
            type = _type;
            def = _def;
            name = _name;
        }

        @Override
        public void run() {
            try {
                Logger.log("Write " + name + ": " + data);
                SharedPreferences.Editor edit = prefs.edit();
                if (type == TYPE_INT)
                    edit.putInt(name, (Integer) data);
                else if (type == TYPE_FLOAT)
                    edit.putFloat(name, (Float) data);
                else if (type == TYPE_BOOLEAN)
                    edit.putBoolean(name, (Boolean) data);
                else if (type == TYPE_STRING)
                    edit.putString(name, (String) data);
                edit.commit();
            }
            catch (Exception | OutOfMemoryError e){
                Log.d(Logger.TAG, "Error while (delayed) WRITE: " + e);
                Tools.getStackTrace(e);
            }
        }
    }
    static public Object get(Object[] tag){
        try {
            int type = (Integer)tag[0];
            Object def = tag[1];
            String name = (String)tag[2];
            JSONObject extra = null;
            if(tag.length >= 4) {
                try {
                    String extraString = (String)tag[3];
                    extra = new JSONObject(extraString);
                }
                catch (Exception e){
                    extra = null;
                }
            }

            try{
                Object result = cache.get(name);
                if(result == null)
                    throw new NullPointerException();
                return result;
            }
            catch (Exception e) {
                try {
                    //NULL: если полученное из массива значение null, выполнить инициализацию
                    // параметра стандартным значением либо из SharedPrefs

                    //NON-PAID: В массиве tag четвёртый аргумент non-paid показывает,
                    // какое значение параметра принудительно присваивается если программа
                    // не куплена и пробная версия закончилась
                    if (type == TYPE_INT) {
                        int result = prefs.getInt(name, (Integer) def);
                        if (extra != null && extra.has("nonPaid") && !tools.isFullVersion())
                            result = extra.getInt("nonPaid");
                        cache.put(name, result);
                        return result;
                    } else if (type == TYPE_FLOAT) {
                        float result = prefs.getFloat(name, (Float) def);
                        if (extra != null && extra.has("nonPaid") && !tools.isFullVersion())
                            result = (float) extra.getDouble("nonPaid");
                        cache.put(name, result);
                        return result;
                    } else if (type == TYPE_BOOLEAN) {
                        boolean result = prefs.getBoolean(name, (Boolean) def);
                        if (extra != null && extra.has("nonPaid") && !tools.isFullVersion())
                            result = extra.getBoolean("nonPaid");
                        cache.put(name, result);
                        return result;
                    } else if (type == TYPE_STRING) {
                        String result = prefs.getString(name, (String) def);
                        if (extra != null && extra.has("nonPaid") && !tools.isFullVersion())
                            result = extra.getString("nonPaid");
                        cache.put(name, result);
                        return result;
                    } else
                        return def;
                }
                catch (Exception ex){
                    return def;
                }
            }
        }catch (Exception | OutOfMemoryError e){
            Log.d(Logger.TAG, "Error while GET: " + Tools.getStackTrace(e));
            return new Object();
        }
    }
    public static boolean clear(){
        boolean ok = true;
        try {
            SharedPreferences.Editor edit = prefs.edit();
            edit.clear();
            edit.commit();
        }catch (Exception e){
            Logger.log("Clear settings: " + e);
            ok = false;
        }
        return ok;
    }
    static public boolean isTutor(String subject, int limit){
        try {
            return isTutorInt(subject, limit) < limit;
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.isTutor("+subject+") произошла ошибка ", e + "\nStackTrace: \n" + Tools.getStackTrace(e), false);
        }
        return false;
    }
    static public int isTutorInt(String subject, int limit){
        try {
            String tag = "Tutor_" + subject;
            int current_times = prefs.getInt(tag, 0);
            if(current_times < limit) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putInt(tag, current_times+1);
                edit.commit();
            }
            return current_times;
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.isTutorInt("+subject+") произошла ошибка ", e + "\nStackTrace: \n" + Tools.getStackTrace(e), false);
        }
        return 0;
    }
    static public void setTutor(String subject, int value){
        //используется для сброса значения показов
        try {
            String tag = "Tutor_" + subject;
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt(tag, value);
            edit.commit();
        }catch (Exception e){
            Logger.log("Где-то в GlobalData.isTutor("+subject+") произошла ошибка ", e + "\nStackTrace: \n" + Tools.getStackTrace(e), false);
        }
    }
    static public int getBrushColor(){
        int brushColor = (Integer)Data.get(Data.brushColorInt());
        int brushOpacity = (Integer)Data.get(Data.brushOpacityInt());
        return Color.argb(brushOpacity,
                Color.red(brushColor),
                Color.green(brushColor),
                Color.blue(brushColor));
    }
    public static File getAutosaveFolder(Context context){
        //папка во внутреннем хранилище для хранения автоматически сохраненных рисунков
        if(context == null)
            return null;
        File result = new File(context.getApplicationContext().getCacheDir(), "Autosave");
        if(!result.isDirectory()){
            try {
                if(!result.mkdir())
                    Logger.log("Can't create folder: " + result);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
    }
    public static File getTransparentFolder(Context context){
        //папка во внутреннем хранилище для хранения дубликатов пользовательских рисунков, только с прозрачным фоном
        if(context == null)
            return null;
        File result = new File(context.getApplicationContext().getCacheDir(), "Transparent");
        if(!result.isDirectory()){
            try {
                if(!result.mkdir())
                    Logger.log("Can't create folder: " + result);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
    }
    public static File getCustomFontsFolder(Context context){
        //папка во внутреннем хранилище для хранения дубликатов пользовательских рисунков, только с прозрачным фоном
        if(context == null)
            return null;
        File result = new File(context.getExternalFilesDir(null), "Fonts");
        if(!result.isDirectory()){
            try {
                if(!result.mkdir())
                    Logger.log("Can't create folder: " + result);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
    }
    public static File getPicturessDrawFolder(){
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), "sDraw");
    }
    public static void fuckIt(){
        activity = null;
        prefs = null;
        store = null;
        tools = null;
    }
}

