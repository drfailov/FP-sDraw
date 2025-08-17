package com.fsoft.FP_sDraw;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;
import com.fsoft.FP_sDraw.common.Tools;
import com.fsoft.FP_sDraw.common.ViewAdapter;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Dr. Failov on 02.01.14.
 * Тестирование новго функционала
 */

public class TestActivity extends Activity implements View.OnClickListener, View.OnLongClickListener {
    Context context = this;
    TextView outputView = null;
    Button scenario1button = null;
    Button scenario2button = null;
    Button scenario3button = null;
    Button scenario4button = null;
    Button scenario5button = null;
    Button scenario6button = null;
    Button scenario7button = null;
    Button scenario8button = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        try{
            outputView  = findViewById(R.id.textOutput);
            outputView.setText("Жду Ваших указаний, мой Господин! \n");

            scenario1button = findViewById(R.id.buttonScenario1);
            scenario2button = findViewById(R.id.buttonScenario2);
            scenario3button = findViewById(R.id.buttonScenario3);
            scenario4button = findViewById(R.id.buttonScenario4);
            scenario5button = findViewById(R.id.buttonScenario5);
            scenario6button = findViewById(R.id.buttonScenario6);
            scenario7button = findViewById(R.id.buttonScenario7);
            scenario8button = findViewById(R.id.buttonScenario8);

            scenario1button.setOnClickListener(this);
            scenario2button.setOnClickListener(this);
            scenario3button.setOnClickListener(this);
            scenario4button.setOnClickListener(this);
            scenario5button.setOnClickListener(this);
            scenario6button.setOnClickListener(this);
            scenario7button.setOnClickListener(this);
            scenario8button.setOnClickListener(this);

            scenario1button.setOnLongClickListener(this);
            scenario2button.setOnLongClickListener(this);
            scenario3button.setOnLongClickListener(this);
            scenario4button.setOnLongClickListener(this);
            scenario5button.setOnLongClickListener(this);
            scenario6button.setOnLongClickListener(this);
            scenario7button.setOnLongClickListener(this);
            scenario8button.setOnLongClickListener(this);

            scenario1button.setText(scenario1Name());
            scenario2button.setText(scenario2Name());
            scenario3button.setText(scenario3Name());
            scenario4button.setText(scenario4Name());
            scenario5button.setText(scenario5Name());
            scenario6button.setText(scenario6Name());
            scenario7button.setText(scenario7Name());
            scenario8button.setText(scenario8Name());
        }catch (Exception e){
            Logger.log("Где-то в TestActivity.onCreate произошла ошибка ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        } catch (OutOfMemoryError e) {
            Logger.log("Где-то в TestActivity.onCreate Недостаточно памяти: ", e + "\nStackTrace: \n" + (Data.tools == null ? e.toString() : Tools.getStackTrace(e)), false);
        }
    }
    @Override public void onResume() {
        super.onResume();
    }
    @Override public void onClick(View view) {
        if(view == scenario1button){
            scenario1();
        }
        else if(view == scenario2button){
            scenario2();
        }
        else if(view == scenario3button){
            scenario3();
        }
        else if(view == scenario4button){
            scenario4();
        }
        else if(view == scenario5button){
            scenario5();
        }
        else if(view == scenario6button){
            scenario6();
        }
        else if(view == scenario7button){
            scenario7();
        }
        else if(view == scenario8button){
            scenario8();
        }
    }
    @Override public boolean onLongClick(View view) {
        if(view == scenario2button){
            scenarioLong2();
            return true;
        }
        if(view == scenario5button){
            Data.save(65536, Data.paidCounter());
            output("Хватит надолго");
            return true;
        }
        return false;
    }
    void output(String text){
        outputView.append(text + "\n");
        Logger.log(text);
    }

     //------------------------------СЦЕНАРИИ
    //СЦЕНАРИЙ 1
    public String scenario1Name(){
        return "Данные управления";
    }
    public void scenario1(){
        output("Device characteristics: ");
        output("API Version = " + Build.VERSION.SDK_INT);
        output("Android Version = " + Build.VERSION.SDK);
        output("displayWidth = " + Data.store().displayWidth);
        output("displayHeight = " + Data.store().displayHeight);
        output("displayRefreshRate = " + Data.tools.getDisplayRefreshRate());
        output("DPI = " + Data.store().DPI);
        output("-------------------------------------------");
        output("store = " + Data.store());
        output("activity = " + Data.store().activity);
        output("draw = " + ((MainActivity) Data.store().activity).draw.toString());
        output("bitmap = " + ((MainActivity)Data.store().activity).draw.bitmap.toString());
        output("Cache entries");
        Iterator iterator = Data.cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry)iterator.next();
            output(pairs.getKey() + " = " + pairs.getValue());
            //iterator.remove(); // avoids a ConcurrentModificationException
        }
        output("-------------------------------------------");
    }

    //СЦЕНАРИЙ 2
    public String scenario2Name(){
        return "Бооольшой список";
    }
    public void scenario2(){
        ListView listView = new ListView(context);
        ViewAdapter viewAdapter = new ViewAdapter(context);
        listView.setAdapter(viewAdapter);
        for(int i=0; i<100; i++){
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
            for(int j=0; j<3; j++){
                ImageView imageBlock = new ImageView(context);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.weight = 1;
                imageBlock.setImageResource(R.mipmap.ic_launcher);
                imageBlock.setLayoutParams(layoutParams);
                linearLayout.addView(imageBlock);
            }
            viewAdapter.addView(linearLayout);
        }
        setContentView(listView);
    }
    public void scenarioLong2(){
        boolean force = (Boolean)Data.get(Data.forcePaidBoolean());
        force = !force;
        Data.save(force, Data.forcePaidBoolean());
        output("Сохранено значение forcePaidBoolean: " + force);
    }

    //СЦЕНАРИЙ 3
    public String scenario3Name(){
        return "Стереть фундамент";
    }
    public void scenario3(){
        Data.fuckIt();
        output("Тут ничего нет");
        output("Скорее всего программа скоро вылетит");
    }

    //СЦЕНАРИЙ 4
    public String scenario4Name(){
        return "Сбросить настройки";
    }
    public void scenario4(){
        output("Сброс настроек...");
        Data.clear();
        output("Сброшено.");
        //((sDraw1)GlobalData.rootContext).finish();
        this.finish();
    }

    //СЦЕНАРИЙ 5
    public String scenario5Name(){
        return "Первое знакомство";
    }
    public void scenario5(){
        output("Щас познакомим...");
        Data.setTutor("tutorial", 0);
        finish();
    }

    //СЦЕНАРИЙ 6
    public String scenario6Name(){
        return "Файл";
    }
    public void scenario6(){
        //Logger.messageBox("Чо смотришь?! Пусто тут! Пока что.");
        try {
            String tmp = Data.tools.readFromAssetsWin1251(getApplicationContext(), "changelog.txt");
            output(tmp);
        }catch(Exception e){
            output(e.toString());
        }
        output("Сценарий пуст");
    }

    //СЦЕНАРИЙ 7
    public String scenario7Name(){
        return "Сбросить демо период";
    }
    public void scenario7(){
        try {
            Data.save(0, Data.paidCounter());
        }catch(Exception e){
            output(e.toString());
        }
        output("Сбросил");
    }

    //СЦЕНАРИЙ 8
    public String scenario8Name(){
        return "Ресурс иконки";
    }
    public void scenario8(){
        output("Показую");
        output("Pro: " + R.mipmap.ic_launcher_pro);
        output("Ordinary: " + R.mipmap.ic_launcher);
    }
}
