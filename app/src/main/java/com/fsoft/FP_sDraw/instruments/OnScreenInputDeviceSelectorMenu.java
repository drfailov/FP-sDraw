package com.fsoft.FP_sDraw.instruments;

import android.graphics.Canvas;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.fsoft.FP_sDraw.DrawCore;
import com.fsoft.FP_sDraw.R;
import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.menu.FloatingMenu;

public class OnScreenInputDeviceSelectorMenu implements Instrument{
    DrawCore draw;


    //floatingMenuForSelecting input device
    public final FloatingMenu floatingMenuForSelectingInputDevice;
    private final FloatingMenu.FloatingMenuButton floatingMenuButtonFinger;
    private final FloatingMenu.FloatingMenuButton floatingMenuButtonSPen;
    private final FloatingMenu.FloatingMenuButton floatingMenuButtonSPenUi;
    boolean sPenDetectedOnThisSession = false;

    public OnScreenInputDeviceSelectorMenu(DrawCore draw) {
        this.draw = draw;
        floatingMenuForSelectingInputDevice = new FloatingMenu(draw.view);
        floatingMenuButtonFinger = floatingMenuForSelectingInputDevice.addButton(R.drawable.settings_finger_hovering, Data.tools.getResource(R.string.any_input_device), this::onAnyInputDeviceClick, true);
        floatingMenuButtonSPen = floatingMenuForSelectingInputDevice.addButton(R.drawable.settings_spen_only, Data.tools.getResource(R.string.useOnlySPen), this::onOnlySPenInputDeviceClick, true);
        floatingMenuButtonSPenUi = floatingMenuForSelectingInputDevice.addButton(R.drawable.settings_spen_only_ui, Data.tools.getResource(R.string.useOnlySPenInUi), this::onOnlySPenUiInputDeviceClick, true);
    }

    @Override
    public String getName() {
        return "OnScreenInputDeviceSelectorMenu";
    }

    @Override
    public String getVisibleName() {
        return "OnScreenInputDeviceSelectorMenu";
    }

    @Override
    public int getImageResourceID() {
        return 0;
    }

    @Override
    public void onSelect() {

    }

    @Override
    public void onDeselect() {

    }

    @Override
    public boolean onTouch(MotionEvent event) {
        if(Build.VERSION.SDK_INT < 14)
            return false;
        {
            int devID = event.getToolType(0);
            if(devID == MotionEvent.TOOL_TYPE_STYLUS || devID == MotionEvent.TOOL_TYPE_ERASER)
                sPenDetectedOnThisSession = true;
        }

        return (floatingMenuForSelectingInputDevice != null && floatingMenuForSelectingInputDevice.processTouch(event));
    }

    @Override
    public boolean onKey(KeyEvent event) {
        return false;
    }

    @Override
    public void onCanvasDraw(Canvas canvas, boolean drawUi) {
        if(floatingMenuForSelectingInputDevice != null){
            boolean sPenOnly = (Boolean)Data.get(Data.sPenOnlyBoolean());
            boolean sPenUiOnly = (Boolean)Data.get(Data.sPenOnlyUiBoolean());
            floatingMenuForSelectingInputDevice.enabled = (sPenOnly || sPenDetectedOnThisSession);
            floatingMenuButtonFinger.highlighted = !sPenOnly;
            floatingMenuButtonSPen.highlighted = sPenOnly && !sPenUiOnly;
            floatingMenuButtonSPenUi.highlighted = sPenOnly && sPenUiOnly;
            float width = floatingMenuForSelectingInputDevice.width();
            floatingMenuForSelectingInputDevice.setBounds(draw.getWidth(), draw.getHeight());
            floatingMenuForSelectingInputDevice.setTargetPosition(draw.getWidth()-width, floatingMenuForSelectingInputDevice.margin());
            floatingMenuForSelectingInputDevice.draw(canvas);
        }
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isVisibleToUser() {
        return false;
    }

    @Override
    public View.OnClickListener getOnClickListener() {
        return null;
    }



    private void onAnyInputDeviceClick(){
        Data.save(false, Data.sPenOnlyBoolean());
        Data.save(false, Data.sPenOnlyUiBoolean());
    }
    private void onOnlySPenInputDeviceClick(){
        Data.save(true, Data.sPenOnlyBoolean());
        Data.save(false, Data.sPenOnlyUiBoolean());
    }
    private void onOnlySPenUiInputDeviceClick(){
        Data.save(true, Data.sPenOnlyBoolean());
        Data.save(true, Data.sPenOnlyUiBoolean());
    }
}
