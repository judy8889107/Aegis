package com.beemdevelopment.aegis.ui;

import static com.mikepenz.iconics.Iconics.TAG;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.beemdevelopment.aegis.R;


public class ShareActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        intent.setClass(ShareActivity.this,UrlCheckActivity.class);
        String receivedAction = intent.getAction();
        String receivedType = intent.getType();
        if (receivedAction.equals(Intent.ACTION_SEND)) {

            // check mime type
            if (receivedType.startsWith("text/")) {

                String receivedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (receivedText != null) {
                    //do your stuff
                    Intent intent1 = new Intent();
                    intent1.setClass(ShareActivity.this,UrlCheckActivity.class);
                    startActivity(intent1);
                }
            }

            else if (receivedType.startsWith("image/")) {

                Uri receiveUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

                if (receiveUri != null) {
                    //do your stuff

                }
            }

        } else if (receivedAction.equals(Intent.ACTION_MAIN)) {

            Log.e(TAG, "onSharedIntent: nothing shared" );
        }
    }
}