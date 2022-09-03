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
import com.beemdevelopment.aegis.ui.slides.Intro2FA_5;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_5;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_1;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_4;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_2;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_3;
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
        viewPagerIndicator.setViewPager(viewPager,10);

        //5.初始化三個Fragment分頁
        fragments = new Fragment[10];
        Intro2FA_1 bf1 = new Intro2FA_1();
        Intro2FA_2 bf2 = new Intro2FA_2();
        Intro2FA_3 bf3 = new Intro2FA_3();
        Intro2FA_4 bf4 = new Intro2FA_4();
        Intro2FA_5 bf5 = new Intro2FA_5();
        IntroUrlCheck_1 bf6 = new IntroUrlCheck_1();
        IntroUrlCheck_2 bf7 = new IntroUrlCheck_2();
        IntroUrlCheck_3 bf8= new IntroUrlCheck_3();
        IntroUrlCheck_4 bf9 = new IntroUrlCheck_4();
        IntroUrlCheck_5 bf0 = new IntroUrlCheck_5();
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
        fragments[9] = bf0;

        //7.初始化轉換器
        adapter = new BlankFragmentClass(getSupportFragmentManager(), fragments);

        //8.<ViewPager>標籤設定轉換器
        viewPager.setAdapter(adapter);
    }


}

