package com.beemdevelopment.aegis.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.beemdevelopment.aegis.R;

public class dialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        String[] strings={"如何使用2FA","如何使用網址檢查","兩者都介紹"};

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("新手導覽");//標題
        //builder.setMessage("第七天");//介紹
        builder.setItems(strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        Intent intent = new Intent(dialogActivity.this, Intro2FAActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case 1:
                        Intent intent1 = new Intent(dialogActivity.this, IntroUrlCheckActivity.class);
                        startActivity(intent1);
                        finish();
                        break;
                    case 2:
                        Intent intent2 = new Intent(dialogActivity.this, Intro2FA_UrlCheckActivity.class);
                        startActivity(intent2);
                        finish();
                        break;
                }
                dialog.dismiss();//結束對話框

            }
        });
        builder.setPositiveButton("略過", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                dialog.cancel();//結束對話框
            }
        });
        builder.show();

    }
}
