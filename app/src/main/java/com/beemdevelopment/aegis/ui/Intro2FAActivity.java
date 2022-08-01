package com.beemdevelopment.aegis.ui;

import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_BIOMETRIC;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_INVALID;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_NONE;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_PASS;

import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ThemeMap;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.intro.IntroBaseActivity;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;
import com.beemdevelopment.aegis.ui.slides.DoneSlide;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_first;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_five;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_four;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_fourth;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_one;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_second;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_third;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_three;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_two;
import com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide;
import com.beemdevelopment.aegis.ui.slides.SecuritySetupSlide;
import com.beemdevelopment.aegis.ui.slides.WelcomeSlide;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;

public class Intro2FAActivity extends IntroBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(Intro2FA_first.class);
        addSlide(Intro2FA_second.class);
        addSlide(Intro2FA_third.class);
        addSlide(Intro2FA_one.class);
        addSlide(Intro2FA_two.class);
        addSlide(Intro2FA_three.class);
        addSlide(Intro2FA_four.class);
        addSlide(Intro2FA_five.class);
        addSlide(Intro2FA_fourth.class);
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

        if (oldSlide == Intro2FA_second.class
                && newSlide == Intro2FA_third.class
                && getState().getInt("cryptType", CRYPT_TYPE_INVALID) == CRYPT_TYPE_NONE) {
            skipToSlide(Intro2FA_fourth.class);
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