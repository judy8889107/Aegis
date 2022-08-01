package com.beemdevelopment.aegis.ui;

import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_INVALID;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_NONE;

import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import com.beemdevelopment.aegis.ThemeMap;
import com.beemdevelopment.aegis.ui.intro.IntroBaseActivity;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_first;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_second;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_third;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_fourth;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_fifth;



public class IntroUrlCheckActivity extends IntroBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(IntroUrlCheck_first.class);
        addSlide(IntroUrlCheck_second.class);
        addSlide(IntroUrlCheck_third.class);
        addSlide(IntroUrlCheck_fourth.class);
        addSlide(IntroUrlCheck_fifth.class);

    }
//呼叫按鈕activity_intro
    @Override
    protected void onSetTheme() {
        setTheme(ThemeMap.NO_ACTION_BAR);
    }
    //
    @Override
    protected boolean onBeforeSlideChanged(Class<? extends SlideFragment> oldSlide, Class<? extends SlideFragment> newSlide) {
        // hide the keyboard before every slide change
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);

        if (oldSlide == IntroUrlCheck_second.class
                && newSlide == IntroUrlCheck_third.class
                && getState().getInt("cryptType", CRYPT_TYPE_INVALID) == CRYPT_TYPE_NONE) {
            skipToSlide(IntroUrlCheck_fourth.class);
            return true;
        }

        return false;
    }

    @Override
    protected void onDonePressed() {

       //結束介面
        // skip the intro from now on
        _prefs.setIntroDone(true);

        setResult(RESULT_OK);
        finish();
    }
}