/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;
import android.view.View;


import com.beemdevelopment.aegis.R;
import com.google.common.net.InternetDomainName;



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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
/* 輸入流 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/* JSON */
import org.json.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* google translation */


public class UrlCheckActivity extends AegisActivity implements View.OnClickListener,Runnable, DialogInterface.OnClickListener{
    /* 變數宣告 */
    EditText url_input;
    Button send_button;
    ImageButton clear_button;
    Button scan_qrcode_button;
    private static final int Scan_QR_CODE = 2;
    private static final String pass_name = "URL_text"; /* 傳遞資料的string名，新增變數避免寫死 */
    private ArrayList<String> issuer;
    private AlertDialog alert_dialog; /* 警告diaolog */
    private AlertDialog whois_search_dialog; /* 搜尋whois dialog */
    private AlertDialog whois_message_dialog; /* 搜尋whois dialog */
    private Toast dialog_toast;


    String URL_text = null; /* url_input和qr_code_scan共用的變數，避免判斷時有衝突，判斷完畢後設為null */
    File Domain_name_txt;


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
        /* 建立所有 dialog */
        buildAllDialog();

    }

    /* Layout按鈕監聽器事件，實作 View.OnClickListener */
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

    /* 設定所有dialog */
    public void buildAllDialog(){
        /* alert dialog */
        AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(UrlCheckActivity.this);
        alert_dialog_builder.setTitle(R.string.warning);
        /* 設定按鈕監聽器 */
        alert_dialog_builder.setPositiveButton(R.string.yes, this);
        alert_dialog_builder.setNegativeButton(R.string.no, this);
        alert_dialog = alert_dialog_builder.create();
        alert_dialog.dismiss();

        /* Whois search dialog */
        AlertDialog.Builder whois_dialog_builder = new AlertDialog.Builder(UrlCheckActivity.this);
        whois_dialog_builder.setTitle(R.string.warning);
        /* 設定按鈕監聽器 */
        whois_dialog_builder.setPositiveButton(R.string.yes,this);
        whois_dialog_builder.setNegativeButton(R.string.no, this);
        whois_search_dialog = whois_dialog_builder.create();
        whois_search_dialog.dismiss();

        /* Whois資訊 dialog */
        AlertDialog.Builder whois_message_builder = new AlertDialog.Builder(UrlCheckActivity.this);
        whois_message_builder.setTitle("網站資訊");
        /* 設定按鈕監聽器 */
        whois_message_builder.setPositiveButton(R.string.yes,this);
        whois_message_builder.setNegativeButton(R.string.no, this);
        whois_message_dialog = whois_message_builder.create();
        whois_message_dialog.dismiss();


    }
    /* 設定alert_dialog_toast */
    public void setAlertDialogToast(){
        dialog_toast = Toast.makeText(this.getApplicationContext(),"",Toast.LENGTH_LONG);
    }


    /* 實作dialog按鈕監聽 */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        /* 先判斷哪個dialog */

        /* whois_search_dialog */
        if(dialog.equals(whois_search_dialog)){
            /* 判斷哪個按鈕被按下 */
            switch (which){
                /* 是 */
                case BUTTON_POSITIVE:
                    /* int which = -1 */
                    /* 啟動 whois thread 檢查 */
                    Thread whois_thread = new Thread(this);
                    whois_thread.setName("whois_thread");
                    whois_thread.start();
                    dialog.dismiss();
                    break;
                case BUTTON_NEGATIVE:
                    /* int which = -2 */
                    dialog.dismiss();
                    alert_dialog.setMessage(URL_text+"\n"+R.string.unsafeURL);
                    alert_dialog.show();
                    break;
            }
        }
        /* whois message dialog */
        if(dialog.equals(whois_message_dialog)){
            /* 判斷哪個按鈕被按下 */
            switch (which){
                /* 是 */
                case BUTTON_POSITIVE:
                    /* int which = -1 */
                    dialog.dismiss();
                    break;
                case BUTTON_NEGATIVE:
                    /* int which = -2 */
                    dialog.dismiss();
                    break;
            }
        }
        /* 警告dialog */
        if(dialog.equals(alert_dialog)){
            /* 判斷哪個按鈕被按下 */
            switch (which){
                /* 是 */
                case BUTTON_POSITIVE:
                    /* int which = -1 */
                    dialog_toast.setText(R.string.addURL); /* 此網址已添加到安全名單 */
                    dialog_toast.show();
                    dialog.dismiss();
                    break;
                case BUTTON_NEGATIVE:
                    /* int which = -2 */
                    dialog.dismiss();
                    break;
            }
        }


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
                /* 檢查 host有無包含 issuer */
                for(int i=0;i<issuer.size();i++){
                    if(host.contains(issuer.get(i))){
                        containsIssuer = true;
                        break;
                    }
                }
                if(!containsIssuer){
                    /* 若 host中不包含 otp引入的issuer，則先詢問是否要用 whois查找網站相關資訊 */
                    whois_search_dialog.setMessage(URL_text+"\n"+"這個網址可能不安全，請問要查看網站相關資訊嗎？");
                    whois_search_dialog.show();
                }
                else{
                    /* 檢查網址 host包含 issuer */
                    dialog_toast.setText(R.string.safeURL);
                    dialog_toast.show();
                }




            }catch (MalformedURLException e){
                dialog_toast.setText(R.string.parseFail);
                dialog_toast.show();
                e.printStackTrace();
            }


        /* 每次檢查完都將 URL_text清空 */

    }


    /* implements Runnable(subThread會執行裡面內容) */
    /* 執行 whois search */
    @Override
    public void run() {

        URL obj = null;
        /* 有引用套件，直接使用 WhoisClient */
        WhoisClient whois = new WhoisClient();
        /* 引用 com.google.common.net套件 */
        InternetDomainName internetDomainName = null;
        String whois_server = null;
        String host = null;
        String TLD = null;
        String domain_name = null;
        String msg = null;




            try {
                /* 建立Whois連線 */
                obj = new URL(URL_text);
                host = obj.getHost();
                TLD = host.substring(host.lastIndexOf('.')+1);
                /* 查詢whois_server.xml 得到 Whois_server */
                whois_server = get_whois_server(TLD);
                if (whois_server == null) { /* 若返回的伺服器為空(xml檔案中找不到可以配對的伺服器) */
                    msg = "Sorry, no match whois server."; /* 設定 msg不為空 */
                } else {
                    /* 連接剛剛得到的 whois server找尋資料 */
                    whois.connect(whois_server);
                    internetDomainName = InternetDomainName.from(host).topPrivateDomain();
                    domain_name = internetDomainName.toString(); /* 得到 Domain name */
                    msg = whois.query(domain_name);
                    whois.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                /* 處理 message翻譯 */
                /* https://translate.googleapis.com/translate_a/single?client=gtx&sl={fromCulture}&tl={toCulture}&dt=t&q={text} */
                /* auto -> 中文 */
                String translation_url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=zh_tw&dt=t&q="+msg;
                try {
                    /* 連到 Google Translation的 URL */
                    obj = new URL(translation_url);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
                    httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
//
                    /* 讀取得到的資訊 */
                    BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String inputLine;
                    StringBuilder stringBuilder = new StringBuilder();

                    /* 得到JSON資訊 */
                    while((inputLine = in.readLine())!=null){
                        stringBuilder.append(inputLine);
                    }
                    in.close(); /* 關閉 Read */

                    /* 將JSON資訊做處理，並停止Thread */
                    handle_message(stringBuilder.toString());
                    Thread.sleep(9999); /* Thread等待9999毫秒 */
                    Thread.currentThread().interrupt(); /* 發出中斷訊號，通知 currentThread 進行中斷 */

                 } catch (IOException | InterruptedException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }


            }

    /* 處理翻譯過的 message(包含解析json檔) */
    public void handle_message(String msg){

        System.out.println("翻譯字串: ");
//        System.out.println(msg);
        try{
            JSONArray jsonArray = new JSONArray(msg);
            JSONArray jsonArray2 = (JSONArray) jsonArray.get(0);
            String result ="";
            for(int i =0;i < jsonArray2.length();i ++){
                result += ((JSONArray) jsonArray2.get(i)).get(0).toString();
            }
            System.out.println(result); /* 印出結果字串 */
        }catch (JSONException e){
            e.printStackTrace();
        }


    }
    /* 在 XML 中找 whois server */
    public String get_whois_server(String xdot_text) {

        String whois_server = null;
        /* 利用 resources讀取 res/xml中檔案 */
        XmlResourceParser server_file = getResources().getXml(R.xml.whois_server);
        boolean isFind = false;
        try{
            int event = server_file.getEventType(); /* 得到現在光標的位置 */
            while(event != XmlPullParser.END_DOCUMENT){ /* 當光標還未到文件結尾 */
                if(event == XmlPullParser.TEXT){ /* 若得到的是文字(非 XmlPullParser.START_TAG <XXX></XXX> ) */
                    if(isFind){
                        whois_server = server_file.getText(); /* 找到的server給whois_server, whois_server就不為空了 */
                        break;
                    }
                    if(xdot_text.equalsIgnoreCase(server_file.getText())){
                        isFind = true;
                    }
                }

                if(whois_server != null) break;
                event = server_file.next(); /* 移動光標 */
            }
        }catch (IOException | XmlPullParserException e){
            e.printStackTrace();
        }



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





