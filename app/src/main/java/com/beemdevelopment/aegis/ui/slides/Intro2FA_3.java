package com.beemdevelopment.aegis.ui.slides;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;

public class Intro2FA_3 extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)  {
        return inflater.inflate(R.layout.fragment_main_third, container, false);
    }
}