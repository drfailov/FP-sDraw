package com.fsoft.FP_sDraw;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.fsoft.FP_sDraw.common.Data;
import com.fsoft.FP_sDraw.common.Logger;

/**
 * Отображает подробную информацию о том, что такое полная версия, зачем она нужна и предлагает её купить
 *
 * Created by Dr. Failov on 19.10.2017.
 */

public class FullVersionInfoActivity extends Activity {
    private View demoRemainingBlock = null;
    private View demoEndedBlock = null;
    private View buy1button = null;
    private View buy2button = null;
    private View resetDemoButton = null;
    private TextView daysRemainingTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_version_info_layout);
        if(Data.tools == null)
            return;

        daysRemainingTextView = findViewById(R.id.fullVersionInfoTextDaysRemaining);
        demoRemainingBlock = findViewById(R.id.fullVersionInfoBlockTrialRemaining);
        demoEndedBlock = findViewById(R.id.fullVersionInfoBlockTrialEnded);
        buy1button = findViewById(R.id.fullVersionInfoButtonBuy);
        buy2button = findViewById(R.id.fullVersionInfoButtonBuy2);
        resetDemoButton = findViewById(R.id.fullVersionInfoButtonResetDemo);

        if(buy2button == null
                || buy1button == null
                || demoEndedBlock == null
                || demoRemainingBlock == null
                || resetDemoButton == null
                || daysRemainingTextView == null) {
            Logger.log("Some elements on FullVersionActivity wasn't loaded. Exiting.");
            return;
        }

        View.OnClickListener buyClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGP();
            }
        };
        buy1button.setOnClickListener(buyClickListener);
        buy2button.setOnClickListener(buyClickListener);

        if(Data.tools.isPaid()) {
            //когда всё оплачего то не показывать ничего про демо
            demoRemainingBlock.setVisibility(View.GONE);
            demoEndedBlock.setVisibility(View.GONE);
            return;
        }

        int remaining = (Integer)Data.get(Data.paidCounter());
        if(remaining > 0) {
            demoRemainingBlock.setVisibility(View.VISIBLE);
            demoEndedBlock.setVisibility(View.GONE);
            daysRemainingTextView.setText(String.valueOf(remaining));
        }
        else {
            demoRemainingBlock.setVisibility(View.GONE);
            demoEndedBlock.setVisibility(View.VISIBLE);
            resetDemoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showResetDemoDialog();
                }
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#273238"));
            getWindow().setNavigationBarColor(Color.parseColor("#273238"));
        }
    }


    void openGP(){
        Data.tools.openFullVersionMarket();
    }
    void showResetDemoDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.fullVersionInfoActivity1);
        builder.setMessage(R.string.fullVersionInfoActivity2);
        builder.setPositiveButton(R.string.fullVersionInfoActivity3, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetDemo();
            }
        });
        builder.setNegativeButton(R.string.fullVersionInfoActivity4, null);
        builder.show();
    }
    void resetDemo(){
        Data.save(Data.paidCounter()[1], Data.paidCounter());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.fullVersionInfoActivity5);
        builder.setMessage(R.string.fullVersionInfoActivity6);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }
}
