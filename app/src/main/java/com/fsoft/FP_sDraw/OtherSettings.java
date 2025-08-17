package com.fsoft.FP_sDraw;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Dr. Failov
 * Date: 23.03.13
 * Time: 19:34
 */
public class OtherSettings extends Activity {
    Context context;
    int settings_header_text_size = 20;
    LinearLayout.LayoutParams layout_params_header = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    CheckBox debug_checkbox;
    /*
    Конструктор. он тут решает.
     */
    @Override public void onCreate(Bundle bundle){
        try {
            super.onCreate(bundle);
            //Logger.log("OtherSettings.onCreate", "Инициализация...", false);
            context = this;
            layout_params_header.setMargins(0, 5, 0, 0);

            //Logger.log("OtherSettings.onCreate", "Задание параметров формы...", false);
            LinearLayout root=new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            ScrollView scroll=new ScrollView(context);
            LinearLayout.LayoutParams layout_params_common=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
            scroll.setLayoutParams(layout_params_common);
            root.addView(scroll);
            setContentView(root);
            LinearLayout linear=new LinearLayout(context);
            linear.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
            scroll.addView(linear);
            linear.setOrientation(LinearLayout.VERTICAL);
            linear.setFocusable(true);
            linear.setFocusableInTouchMode(true);
    //   //--------------------------------------------------------------------------------------------------------------   developer toolls
            {
                //Logger.log("OtherSettings.onCreate", "Настройка блока для разработчика...", false);
                //TextView developer
                TextView developer_text=new TextView(context);
                developer_text.setText(Data.tools.getResource(R.string.settingsOthersDeveloper));
                developer_text.setTextSize(settings_header_text_size);
                developer_text.setLayoutParams(layout_params_header);
                developer_text.setGravity(Gravity.CENTER);
                developer_text.setTypeface(Typeface.DEFAULT_BOLD);
                linear.addView(developer_text);

                //checkbox debug
                debug_checkbox = new CheckBox(context);
                debug_checkbox.setText(Data.tools.getResource(R.string.settingsOthersDeveloperDebug));
                debug_checkbox.setChecked((Boolean)Data.get(Data.debugBoolean()));
                layout_params_common = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layout_params_common.gravity=Gravity.LEFT;
                layout_params_common.setMargins(10, 10, 10, 0);
                debug_checkbox.setLayoutParams(layout_params_common);
                linear.addView(debug_checkbox);
                //text info
                TextView text3=new TextView(context);
                text3.setText(Data.tools.getResource(R.string.settingsOthersDeveloperDebugTip));
                text3.setTextSize((int) (settings_header_text_size * 0.7));
                text3.setGravity(Gravity.LEFT);
                text3.setTypeface(Typeface.DEFAULT);
                text3.setTextColor(Color.GRAY);
                linear.addView(text3);

                //Button reset pressure coefficient
                Button delete_coef_button = new Button(context);
                delete_coef_button.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        if(Data.cache != null) {
                            Set<Map.Entry<String, Object>> set = Data.cache.entrySet();
                            Iterator<Map.Entry<String, Object>> iterator = set.iterator();
                            while (iterator.hasNext()){
                                Map.Entry<String, Object> entry= iterator.next();
                                String name = entry.getKey();
                                if(name.contains("pressureMax")
                                        || name.contains("pressureMin")
                                        || name.contains("sizeMin")
                                        || name.contains("sizeMax")){
                                    Data.save(-1f, new Object[]{Data.TYPE_FLOAT,-1f,name});
                                }
                            }
                            Data.save(Data.MANAGE_METHOD_CONSTANT, Data.manageMethodInt());
                        }

                        Toast.makeText(context, Data.tools.getResource(R.string.settingsOthersDeveloperResetPressureMessage), Toast.LENGTH_SHORT).show();
                    }});
                delete_coef_button.setText(Data.tools.getResource(R.string.settingsOthersDeveloperResetPressure));
                layout_params_common = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layout_params_common.gravity=Gravity.RIGHT;
                layout_params_common.setMargins(10, 15, 10, 0);
                delete_coef_button.setLayoutParams(layout_params_common);
                linear.addView(delete_coef_button);
                //text info
                TextView text4=new TextView(context);
                text4.setText(Data.tools.getResource(R.string.settingsOthersDeveloperResetPressureTip));
                text4.setTextSize((int) (settings_header_text_size * 0.7));
                text4.setGravity(Gravity.LEFT);
                text4.setTypeface(Typeface.DEFAULT);
                text4.setTextColor(Color.GRAY);
                linear.addView(text4);
            }
        }
        catch (Exception e){
            Logger.log("Где-то в OtherSettings.onCreate произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
        catch (OutOfMemoryError e) {
            Logger.log("Где-то в OtherSettings.onCreate Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }

    /*
    Используется для применения настроек
    */
    public void apply(){
        try {
            Data.save(debug_checkbox.isChecked(), Data.debugBoolean());
        }
        catch (Exception e){
            Logger.log("Где-то в OtherSettings.apply произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Где-то в OtherSettings.apply Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }


    @Override public void onResume() {
        super.onResume();
    }

    /*
     Срабатывает при закрытии окна. Инициирует сохранение данных
     */
    @Override public void onPause() {
        apply();
        super.onPause();
    }
}
