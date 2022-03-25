/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;

import com.beemdevelopment.aegis.R;


import java.io.IOException;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
/* 使用EditText */
import android.widget.EditText;
/* 控制鍵盤 */
import android.view.inputmethod.InputMethodManager;
/* ImageButton的import */
import android.widget.ImageButton;

/* URL lib */
import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
/* 輸入流 */
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
/* JSON */
import org.json.*;



public class UrlCheckActivity extends AegisActivity{
    /* 變數宣告 */
    EditText url_input;
    Button send_button;
    ImageButton clear_button;
    Button scan_qrcode_button;
    private static final int Scan_QR_CODE = 2;
    private static final String pass_name = "URL_text"; /* 傳遞資料的string名，新增變數避免寫死 */
    private ArrayList<String> issuer;

    String URL_text = null; /* url_input和qr_code_scan共用的變數，避免判斷時有衝突，判斷完畢後設為null */
    File Domain_name_txt;
    boolean SuccessCreate;

    /* Code代碼 */
    final int CODE_SCAN = 0;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* 孤兒進程導致系統重啟 */
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        /* 設定Content是 layout裡面的 activity_url_check檔案
        * 原本要寫為 final View variablename = setContentView(R.layout.activityName);
        * 這裡應該是因為 extends Aegis，用this即可
        *  */
        this.setContentView(R.layout.activity_url_check);
        this.setSupportActionBar(findViewById(R.id.toolbar));




        /* 設定變數 */
        url_input = findViewById(R.id.url_input);
        send_button = findViewById(R.id.send_button);
        clear_button = findViewById(R.id.clear_button);
        scan_qrcode_button = findViewById(R.id.scan_qrcode_button);





        /* 監聽器設定 */
        /* sent_button */
        send_button.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                System.out.println("URL輸入: "+url_input.getText().toString());
                /* 按下send_button就隱藏鍵盤 */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(send_button.getWindowToken(), 0);
                URL_text = url_input.getText().toString();



            }
        });
        /* clear_button */
        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                url_input.setText("");
            }
        });

        /* scan_qrcode_button */
        scan_qrcode_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent scan_qrcode_activity = new Intent(getApplicationContext(),UrlCheckActivity_ScanQrcodeActivity.class);
                startActivityForResult(scan_qrcode_activity, Scan_QR_CODE);

            }
        });

//

        Create_Domain_name_txt_file();
        /* Get File list(test) */
        String[] files = getApplicationContext().fileList();
        System.out.println("\nExist file list:");
        for(String file : files)
            System.out.println(file);



    }

    /* 接收activity傳送回來的資料 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        /* 加入resultCode(該activity回傳的回傳碼，RESULT_OK預設為-1) 在 Scan_QR_code甚麼都不做按返回鍵的話，resultCode = 0
        * 所以要判斷resultCode = 0 的話返回原activity*/
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case Scan_QR_CODE:
                URL_text = data.getStringExtra(pass_name);
                System.out.println("test");
                break;
        }
    }


    /* 檢查URL function */
    public void UrlCheck(){

        /* 變數宣告 */
        URL url_obj; /* URL class 提供了解析 URL 地址的基本方法 */
        URLConnection url_connection;
        try{
            /* 設定變數 */
            url_obj = new URL(URL_text);
            url_connection = url_obj.openConnection();



            /* print URL參數 */
            System.out.println("URL參數：");
            System.out.println("URL：" + url_obj.toString());
            System.out.println("協議：" + url_obj.getProtocol());
            System.out.println("驗證信息：" + url_obj.getAuthority());
            System.out.println("文件名及請求參數：" + url_obj.getFile());
            System.out.println("主機名：" + url_obj.getHost());
            System.out.println("路徑：" + url_obj.getPath());
            System.out.println("端口：" + url_obj.getPort());
            System.out.println("默認端口：" + url_obj.getDefaultPort());
            System.out.println("請求參數：" + url_obj.getQuery());
            System.out.println("定位位置：" + url_obj.getRef());
            System.out.println("使用者資訊：" + url_obj.getUserInfo());

            System.out.println();
            /* URL Connection參數 */
//            System.out.println(url_connection.getContentType());


        }catch (IOException e){
            e.printStackTrace();
        }
        URL_text = null;
    }

    /* 建立Domain name的檔案 */
    public void Create_Domain_name_txt_file(){

        /* 分析aegis.json檔，並把issuer放入arrayList issuer裡面 */
        Create_issuer_arrayList();

        /* Create file */
        File dir = getApplicationContext().getFilesDir();
        Domain_name_txt = new File(dir,"Domain_name_txt.txt");
        try {
            if(Domain_name_txt.createNewFile()){
//                System.out.println("Success Create Domain_name_txt file.");
            }
            else{
//                System.out.println("Domain_name_txt file is exist.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int index = 0;
        while(index<issuer.size()){
//            System.out.println(issuer.get(index));
            index++;
        }



    }

    /* 分析aegis.json檔，並把issuer放入arrayList issuer裡面 */
    public void Create_issuer_arrayList(){
        /* 打開 aegis.json 轉換 && 解析JSON檔，並創建 issuer arraylist */
        File f = new File(getApplicationContext().getFilesDir(), "aegis.json");
        BufferedReader br;
        String aegis_json_string =""; /* File JSON檔轉為String */
        JSONObject jsonObject; /* String再建立成jsonObject */
        JSONArray jsonArray;   /* 用來解析jsonObject */
        issuer = new ArrayList<>(); /* 利用ArrayList儲存issuer */
        try {
            /* 讀取 file轉換成String，因為JDK版本關係要用 BufferedReader轉(用BufferedReader是因為讀取效率高) */
            br = new BufferedReader(new FileReader(f));
            while(br.ready()){
                aegis_json_string += br.readLine();
            }
            br.close();
            /* 創立並解析 JSON物件 */
            jsonObject = new JSONObject(aegis_json_string);
            jsonObject = jsonObject.getJSONObject("db");
            jsonArray = jsonObject.getJSONArray("entries");
            for(int i=0;i<jsonArray.length();i++){
                jsonObject = jsonArray.getJSONObject(i);
                issuer.add(jsonObject.get("issuer").toString());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

}

