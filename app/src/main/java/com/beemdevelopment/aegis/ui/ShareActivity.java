package com.beemdevelopment.aegis.ui;

import static com.mikepenz.iconics.Iconics.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;


public class ShareActivity extends AegisActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_check);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
            handleSendImage (intent);
        }
    }

    private void handleSendImage(Intent intent) {
        EditText pathTextView = findViewById(R.id.url_input);
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            pathTextView.setText(sharedText);
        }

    }

}