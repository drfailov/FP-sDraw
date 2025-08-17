package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * Базовый класс для инструментов
 * Created by Dr. Failov on 02.01.2015.
 * добавлять новые инструменты в
 * DrawCore -> переменные
 * DrawCore -> sizeChanged()
 */
public interface Instrument {
    String getName();//имя интстремнта. Также может быть использовано для отображения пользоватлю.
    String getVisibleName();//видимое имя интстремнта. ДОЛЖЖНО быть использовано для отображения пользоватлю.
    int getImageResourceID();//ИД пиктограммы инмтрумента для отображения пользователю
    void onSelect();//загрузка и настройка инструмента. Может быть вызвано несколько раз
    void onDeselect();//можно освобождать память, отключать метки и т.д.
    boolean onTouch(MotionEvent event);//при прикосновеннии
    boolean onKey(KeyEvent event);//при нажатии на кнопку
    void onCanvasDraw(Canvas canvas, boolean drawUi);//позволяет рисовать на экране что либо. drawUi=false если не нужно рисовать интерфейс пользователя (например вызвано из другого инструмента)
    boolean isActive();//проверка на предмет того, выбран ли этот инструмент
    boolean isVisibleToUser(); //Нужно ли показывать этот инструмент пользователю в менюшках.
    View.OnClickListener getOnClickListener();//нажатие на иконку инструмента. Должно елать так, чтобы инструмент становился активным
}
