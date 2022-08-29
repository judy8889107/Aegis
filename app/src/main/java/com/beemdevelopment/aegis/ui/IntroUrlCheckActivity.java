package com.beemdevelopment.aegis.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.ui.slides.BlankFragmentClass;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_fifth;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_first;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_fourth;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_second;
import com.beemdevelopment.aegis.ui.slides.IntroUrlCheck_third;
import com.lwj.widget.viewpagerindicator.ViewPagerIndicator;

public class IntroUrlCheckActivity extends AppCompatActivity {

    //1.宣告<ViewPager>標籤為viewPager
    private ViewPager viewPager;
    private ViewPagerIndicator viewPagerIndicator;

    //2.宣告使用轉換器
    private BlankFragmentClass adapter;

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
        viewPagerIndicator.setViewPager(viewPager,5);


        //5.初始化三個Fragment分頁
        fragments = new Fragment[5];
        IntroUrlCheck_first bf1 = new IntroUrlCheck_first();
        IntroUrlCheck_second bf2 = new IntroUrlCheck_second();
        IntroUrlCheck_third bf3= new IntroUrlCheck_third();
        IntroUrlCheck_fourth bf4 = new IntroUrlCheck_fourth();
        IntroUrlCheck_fifth bf5 = new IntroUrlCheck_fifth();
        //6.陣列內容
        fragments[0] = bf1;
        fragments[1] = bf2;
        fragments[2] = bf3;
        fragments[3] = bf4;
        fragments[4] = bf5;

        //7.初始化轉換器
        adapter = new BlankFragmentClass(getSupportFragmentManager(), fragments);

        //8.<ViewPager>標籤設定轉換器
        viewPager.setAdapter(adapter);
    }


}
