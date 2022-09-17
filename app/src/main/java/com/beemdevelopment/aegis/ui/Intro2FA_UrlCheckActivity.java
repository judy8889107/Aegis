package com.beemdevelopment.aegis.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.slides.BlankFragmentClass;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_1;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_2;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_3;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_4;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_4_2;
import com.beemdevelopment.aegis.ui.slides.Intro2FA_5;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_10;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_12;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_13;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_4_2;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_4_3;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_5;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_1;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_4;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_2;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_3;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_6;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_7;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_7_2;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_7_3;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_8;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_9;
import com.lwj.widget.viewpagerindicator.ViewPagerIndicator;

public class Intro2FA_UrlCheckActivity extends AppCompatActivity {

    //1.宣告<ViewPager>標籤為viewPager
    private ViewPager viewPager;

    //2.宣告使用轉換器
    private BlankFragmentClass adapter;
    private ViewPagerIndicator viewPagerIndicator;

    //3.宣告變數為fragments
    private Fragment[] fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);
        //4.指定activity_main.xml內標籤
        viewPager = findViewById(R.id.viewpager);
        viewPagerIndicator = (ViewPagerIndicator) findViewById(R.id.viewPagerIndicator);
        //viewpager是固定页数, 传入viewpager即可
        viewPagerIndicator.setViewPager(viewPager,22);

        //5.初始化三個Fragment分頁
        fragments = new Fragment[22];
        Intro2FA_1 bf1 = new Intro2FA_1();
        Intro2FA_2 bf2 = new Intro2FA_2();
        Intro2FA_3 bf3 = new Intro2FA_3();
        Intro2FA_4 bf4 = new Intro2FA_4();
        Intro2FA_4_2 bf5 = new Intro2FA_4_2();
        Intro2FA_5 bf6 = new Intro2FA_5();
        IntroUrlCheck_1 bf7 = new IntroUrlCheck_1();
        IntroUrlCheck_2 bf8 = new IntroUrlCheck_2();
        IntroUrlCheck_3 bf9 = new IntroUrlCheck_3();
        IntroUrlCheck_4 bf10 = new IntroUrlCheck_4();
        IntroUrlCheck_4_2 bf11 = new IntroUrlCheck_4_2();
        IntroUrlCheck_4_3 bf12 = new IntroUrlCheck_4_3();
        IntroUrlCheck_5 bf13 = new IntroUrlCheck_5();
        IntroUrlCheck_6 bf14 = new IntroUrlCheck_6();
        IntroUrlCheck_7 bf15 = new IntroUrlCheck_7();
        IntroUrlCheck_7_2 bf16 = new IntroUrlCheck_7_2();
        IntroUrlCheck_7_3 bf17 = new IntroUrlCheck_7_3();
        IntroUrlCheck_8 bf18 = new IntroUrlCheck_8();
        IntroUrlCheck_9 bf19 = new IntroUrlCheck_9();
        IntroUrlCheck_10 bf20 = new IntroUrlCheck_10();
        IntroUrlCheck_12 bf21 = new IntroUrlCheck_12();
        IntroUrlCheck_13 bf22 = new IntroUrlCheck_13();

        //6.陣列內容
        fragments[0] = bf1;
        fragments[1] = bf2;
        fragments[2] = bf3;
        fragments[3] = bf4;
        fragments[4] = bf5;
        fragments[5] = bf6;
        fragments[6] = bf7;
        fragments[7] = bf8;
        fragments[8] = bf9;
        fragments[9] = bf10;
        fragments[10] = bf11;
        fragments[11] = bf12;
        fragments[12] = bf13;
        fragments[13] = bf14;
        fragments[14] = bf15;
        fragments[15] = bf16;
        fragments[16] = bf17;
        fragments[17] = bf18;
        fragments[18] = bf19;
        fragments[19] = bf20;
        fragments[20] = bf21;
        fragments[21] = bf22;

        //7.初始化轉換器
        adapter = new BlankFragmentClass(getSupportFragmentManager(), fragments);

        //8.<ViewPager>標籤設定轉換器
        viewPager.setAdapter(adapter);
    }


}

