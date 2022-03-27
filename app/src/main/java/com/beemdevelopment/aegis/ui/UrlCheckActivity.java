/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;

import android.view.View;

import com.beemdevelopment.aegis.R;
import com.bumptech.glide.load.engine.Resource;


import java.io.IOException;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
/* 使用EditText */
import android.widget.EditText;
/* 控制鍵盤 */
/* ImageButton的import */
import android.widget.ImageButton;
import android.widget.Toast;

/* URL lib */
import androidx.annotation.RequiresApi;

import org.apache.commons.net.whois.WhoisClient;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
/* 輸入流 */
import java.util.ArrayList;
/* JSON */
import org.json.*;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class UrlCheckActivity extends AegisActivity implements View.OnClickListener,Runnable{
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
        send_button.setOnClickListener(this);
        clear_button.setOnClickListener(this);
        scan_qrcode_button.setOnClickListener(this);

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

    /* 監聽器事件，實作 View.OnClickListener */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.send_button:
                /* 按下send_button就隱藏鍵盤 */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(send_button.getWindowToken(), 0);
                URL_text = url_input.getText().toString();
                /* 執行 URL check */
                UrlCheck();
                break;
            case R.id.clear_button:
                url_input.setText("");
                break;
            case R.id.scan_qrcode_button:
                Intent scan_qrcode_activity = new Intent(getApplicationContext(),UrlCheckActivity_ScanQrcodeActivity.class);
                startActivityForResult(scan_qrcode_activity, Scan_QR_CODE);
                break;

        }
    }

    /* RequestCode/ResultCode 在兩個intent間接收/傳遞資料 */
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
        alert_dialog_builder.setTitle(R.string.warning);
        alert_dialog_builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog_toast.setText(R.string.addURL);
                dialog_toast.show();

            }
        });

        alert_dialog_builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
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

                /* check protocol */
                if(!protocol.equals("http") && !protocol.equals("https")){
                    alert_dialog.setMessage(URL_text+"\n"+ getResources().getString(R.string.unsafeURL));
                    alert_dialog.show();
                }
                /* check issuer */
                for(int i=0;i<issuer.size();i++){
                    if(host.contains(issuer.get(i))){
                        containsIssuer = true;
                        break;
                    }
                }
                if(!containsIssuer){
                    alert_dialog.setMessage(URL_text+"\n"+ getResources().getString(R.string.unsafeURL));
                    alert_dialog.show();
                }
                else{
                    dialog_toast.setText(R.string.safeURL);
                    dialog_toast.show();
                }
                /* WHOIS */
                Thread subThread = new Thread(this);
                subThread.start();



            }catch (MalformedURLException e){
                dialog_toast.setText(R.string.parseFail);
                dialog_toast.show();
                e.printStackTrace();
            }


        /* 每次檢查完都將 URL_text清空 */

    }


    /* implements Runnable(subThread會執行裡面內容) */
    @Override
    public void run() {

        URL obj = null;
        int start;
        String domain_name;
        /* 有引用套件，直接使用 WhoisClient */
        WhoisClient whois = new WhoisClient();
        try {
            /* 處理 Domain name */
            obj = new URL(URL_text);
                /* 第一個點 */
            start = obj.getHost().toString().lastIndexOf('.');
                /* 若有第二個點，則返回第二個點位置 +1 */
            if(obj.getHost().toString().lastIndexOf('.', start-1) != -1){
                start = obj.getHost().toString().lastIndexOf('.', start-1);
            }
            else start = 0;
            domain_name = obj.getHost().substring(start+1);
            /* 處理 Whois_server */
            String tmp = get_whois_server(domain_name);
            whois.connect(WhoisClient.DEFAULT_HOST);
            System.out.println(whois.query("strato.de"));
            whois.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    /* 在 XML 中找 whois server */
    public String get_whois_server(String domain_name){
        String whois_server = null;

        /* 利用 resources讀取 res/xml中檔案 */
        XmlResourceParser server_file = getResources().getXml(R.xml.whois_server);


        return whois_server;



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

