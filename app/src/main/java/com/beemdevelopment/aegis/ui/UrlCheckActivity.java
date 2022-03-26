/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

/* URL lib */
import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
/* 輸入流 */
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
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
    private AlertDialog alert_dialog;
    private Toast dialog_toast;

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
//                System.out.println("URL輸入: "+url_input.getText().toString()); test
                /* 按下send_button就隱藏鍵盤 */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(send_button.getWindowToken(), 0);
                URL_text = url_input.getText().toString();
                /* 執行 URL check */
                UrlCheck();



            }
        });
        /* clear_button監聽事件 */
        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                url_input.setText("");
            }
        });

        /* scan_qrcode_button監聽事件 */
        scan_qrcode_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Intent scan_qrcode_activity = new Intent(getApplicationContext(),UrlCheckActivity_ScanQrcodeActivity.class);
                startActivityForResult(scan_qrcode_activity, Scan_QR_CODE);

            }
        });

        /* 初始化 */
        initialize();


        /* Get File list(test) */
//        String[] files = getApplicationContext().fileList();
//        System.out.println("\nExist file list:");
//        for(String file : files)
//            System.out.println(file); test




    }

    /* 初始化 設定所有參數等等 */
    public void initialize(){
        /* 分析aegis.json檔，並把issuer放入arrayList issuer裡面 */
        Create_issuer_arrayList();

        /* 創立 Domain name的 txt file(目前為空) */
        Create_Domain_name_txt_file();

        /* 設定alert dialog toast的參數 */
        setAlertDialogToast();
        /* 設定alert dialog的參數 */
        setAlertDialog();

    }

    /* 接收activity傳送回來的資料 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        /* resultcode 正常無動作(不掃描QRcode)返回時，是0 */

        switch (resultCode) {
            case Scan_QR_CODE:
                URL_text = data.getStringExtra(pass_name);
                /* 執行 URL check */
                UrlCheck();
                break;
            default: return;  //resultCode為 0 時，return回原本activity
        }
    }

    /* 設定alert_dialog */
    public void setAlertDialog(){

        AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(UrlCheckActivity.this);

        alert_dialog_builder.setTitle("警告");
        alert_dialog_builder.setMessage("這個網址可能不安全，請問要將此網址加入安全名單嗎？");
        alert_dialog_builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog_toast.setText("已把網址加入安全名單");
                dialog_toast.show();

            }
        });

        alert_dialog_builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alert_dialog = alert_dialog_builder.create();
        alert_dialog.dismiss();


    }
    /* 設定alert_dialog_toast */
    public void setAlertDialogToast(){
        dialog_toast = Toast.makeText(this.getApplicationContext(),"",Toast.LENGTH_LONG);
    }

    /* 檢查URL function */
    public void UrlCheck(){

        /* 變數宣告 */
        URL url_obj; /* URL class 提供了解析 URL 地址的基本方法 */
        String host;
        String protocol;
        boolean containsIssuer = false;

            try{
                /* 設定變數 */
                url_obj = new URL(URL_text);
                protocol = url_obj.getProtocol();
                host = url_obj.getHost().toLowerCase();

                if(!protocol.equals("http") && !protocol.equals("https")){
                    alert_dialog.setMessage(URL_text+"\n這個網址可能不安全，請問要將此網址加入安全名單嗎？");
                    alert_dialog.show();
                }
                for(int i=0;i<issuer.size();i++){
                    containsIssuer = false;
                    if(host.contains(issuer.get(i))){
                        containsIssuer = true;
                        break;
                    }
                }
                if(!containsIssuer){
                    alert_dialog.setMessage(URL_text+"\n這個網址可能不安全，請問要將此網址加入安全名單嗎？");
                    alert_dialog.show();
                }
                else{
                    dialog_toast.setText("此為安全網站，可以放心登入");
                    dialog_toast.show();
                }


            }catch (MalformedURLException e){
                dialog_toast.setText("解析失敗：非網址格式，請重新嘗試");
                dialog_toast.show();
                e.printStackTrace();
            }


        /* 每次檢查完都將 URL_text清空 */
        URL_text = null;
    }

    /* 建立Domain name的檔案 */
    public void Create_Domain_name_txt_file(){



        /* Create file */
        File dir = getApplicationContext().getFilesDir();
        Domain_name_txt = new File(dir,"Domain_name_txt.txt");
        try {
            if(Domain_name_txt.createNewFile()){
//                System.out.println("Success Create Domain_name_txt file."); test
            }
            else{
//                System.out.println("Domain_name_txt file is exist."); test
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        for(int i=0;i<issuer.size();i++) System.out.println(issuer.get(i)); test



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
                issuer.add(jsonObject.get("issuer").toString().toLowerCase()); /* issuer轉換成小寫放入arrayList */
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

