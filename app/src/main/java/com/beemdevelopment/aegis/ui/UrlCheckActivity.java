/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;


import android.util.Log;
import android.view.View;


import com.beemdevelopment.aegis.R;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.safetynet.SafetyNetClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.apache.commons.net.whois.WhoisClient;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
/* 輸入流 */
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* JSON */
import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;




public class UrlCheckActivity extends AegisActivity implements View.OnClickListener,Runnable, DialogInterface.OnClickListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
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
    private GoogleApiClient mGoogleApiClient; /* ( GoogleApiClient已經棄用了) */
    private String api_key = null; /* SafetyNet與 Google Play建立連線用的 API KEY */
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



        /* 初始化 */
        initialize();


        /* Get File list(test) */
//        String[] files = getApplicationContext().fileList();
//        System.out.println("\nExist file list:");
//        for(String file : files)
//            System.out.println(file); test






    }

    //生成 nonce
    private static byte[] generateNonce(){
        //生成隨機長度 範圍: 16~1024 (max - min) + min
        int nonce_length = new SecureRandom().nextInt(1024 - 16) + 16;
        byte[] nonce = new byte[nonce_length];
        //SecureRandom默認用 SHA1PRNG生成隨機數，占用較少資源 (內置兩種隨機數字算法: NativePRNG 和 SHA1PRNG)
        new SecureRandom().nextBytes(nonce);

        // test -- 印出 nonce
        StringBuilder result = new StringBuilder();
        for (byte temp : nonce) {
            result.append(String.format("%02x", temp));
        }
        System.out.println("印出nonce長度:"+nonce_length);
        System.out.println("印出nonce: "+result.toString());
        // test
        return nonce;

    }


    /* 初始化 設定所有參數等等 */
    public void initialize(){
        /* 設定參數 */
        url_input = findViewById(R.id.url_input);
        send_button = findViewById(R.id.send_button);
        clear_button = findViewById(R.id.clear_button);
        scan_qrcode_button = findViewById(R.id.scan_qrcode_button);
        api_key = getString(R.string.safety_net_api_key);
        /* 監聽器設定 */
        send_button.setOnClickListener(this);
        clear_button.setOnClickListener(this);
        scan_qrcode_button.setOnClickListener(this);
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
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void run() {



        /* 釣魚網站網址檢查並解析結果 */
        String ScamAdviser = "https://www.scamadviser.com/check-website/";


        /* Whois 參數 */
        URL url_obj = null;
        /* 有引用套件，直接使用 WhoisClient */
        WhoisClient whois = new WhoisClient();
        /* 引用 com.google.common.net套件 */
        InternetDomainName internetDomainName = null;
        String whois_server = null;
        String host = null;
        String TLD = null;
        String domain_name = null;
        String origin_msg = null;
        String msg = null;
        //判斷 thread name 執行對應動作

        try {

            url_obj = new URL(URL_text);
            host = url_obj.getHost(); //取得 host
            ScamAdviser += URL_text.replaceAll("http(s?)://(www\\.)?|(/)$","").toLowerCase();
            System.out.println("ScamAdviser網站="+ScamAdviser);
            url_obj = new URL(ScamAdviser);


            //Judy
            //Google Safe Browsing 确保用户设备上安装了正确的 Google Play 服务版本
//            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this.getApplicationContext())
//                    == ConnectionResult.SUCCESS) {
//                System.out.println("确保用户设备上安装了正确的 Google Play 服务版本：OK");
//                // The SafetyNet Attestation API is available.
//            } else {
//                System.out.println("确保用户设备上安装了正确的 Google Play 服务版本：No");
//                // Prompt user to update Google Play services.
//            }
            System.out.println("API KEY為: "+api_key);
            System.out.println("嘗試發出SafetyNet證明請求");

            //Use SafetyNet
            SafetyNet.getClient(this).attest(generateNonce(), api_key)
                    .addOnSuccessListener(this,
                            new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                                @Override
                                public void onSuccess(SafetyNetApi.AttestationResponse response) {
                                    // Indicates communication with the service was successful.
                                    System.out.println("成功取得服務，印出回應: ");
                                    // Use response.getJwsResult() to get the result data.
                                    System.out.println(response.getJwsResult());
                                }
                            })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            System.out.println("進入FailureListener");
                            // An error occurred while communicating with the service.
                            if (e instanceof ApiException) {
                                // An error with the Google Play services API contains some
                                // additional details.
                                ApiException apiException = (ApiException) e;
                                System.out.println("失敗，印出回應代碼: ");
                                System.out.println(apiException.getStatus());
                                // You can retrieve the status code using the
                                // apiException.getStatusCode() method.
                            } else {
                                // A different, unknown type of error occurred.
                                System.out.println("其他失敗: ");
                                System.out.println(e.getMessage());
                            }
                        }
                    });






            /* 建立Whois連線 */

            domain_name = InternetDomainName.from(host).topPrivateDomain().toString(); /* 得到 Domain name */
            TLD = host.substring(host.lastIndexOf('.')+1);
            /* 查詢whois_server.xml 得到 Whois_server */
            whois_server = get_whois_server(TLD);
            if (whois_server == null) { /* 若返回的伺服器為空(xml檔案中找不到可以配對的伺服器) */
                msg = "Sorry, no match whois server."; /* 設定 msg不為空 */
            } else {
                /* 連接剛剛得到的 whois server找尋資料 */
                whois.connect(whois_server);
                msg = whois.query(domain_name);
                whois.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
          System.out.println(e.getMessage());
        } finally {

            /* 將原本訊息備份 */
            origin_msg = msg;

            /* 傳TLD和origin_msg 做欄位切割 */
//            splitting_filed(TLD, msg);  Judy 等等恢復註解


            /* 處理 message翻譯 */
            /* https://translate.googleapis.com/translate_a/single?client=gtx&sl={fromCulture}&tl={toCulture}&dt=t&q={text} */
            /* auto -> 中文 */
//            String translation_url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=zh_tw&dt=t&q="+msg;
//            try {
//                /* 連到 Google Translation的 URL */
//                obj = new URL(translation_url);
//                HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
//                httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
////
//                /* 讀取得到的資訊 */
//                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
//                String inputLine;
//                StringBuilder stringBuilder = new StringBuilder();
//
//                /* 得到JSON資訊 */
//                while((inputLine = in.readLine())!=null){
//                    stringBuilder.append(inputLine);
//                }
//                in.close(); /* 關閉 Read */
//                msg = stringBuilder.toString();
//
//                /* 傳入原始訊息和翻譯過後的訊息，將JSON資訊做處理，並停止Thread */
//                handle_message(origin_msg,msg);
//                Thread.sleep(9999); /* Thread等待9999毫秒 */
//                Thread.currentThread().interrupt(); /* 發出中斷訊號，通知 currentThread 進行中斷 */
//
//            } catch (IOException | InterruptedException e) {
//                System.out.println(e.getMessage());
//                e.printStackTrace();
//            }

        }







    }
    //
    /* information欄位切割 (註：若非英文的information 將不欄位處理，但會進行翻譯)*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void splitting_filed(String TLD, String msg){

        System.out.println("############################################\n原始訊息:\n"+msg);


        /* 先將可以處理的大致處理 */
        msg = msg.replaceAll("_"," ");
        msg = msg.replaceAll("\\.{2,}",""); //匹配一長串..... .no TLD
        msg = msg.replaceAll("\r",""); //把 \r置換(\r是將光標移動到行首)
//        System.out.println("\n\nReplace過後:\n"+msg+"\n----------------------------");

        /* 分割字串 */
//        System.out.println("\n\n=================================================================================\n");

        String[] token = msg.split("\n");
        //TLD 為.pl的另外處理，但.pl的REGISTRAR 還要再處理其他前面有空白的欄位。所以會在下面 if判斷.ua錯誤，執行 else再把其他欄位處理好
        if(TLD.equals("pl")){
            //處理 REGISTRAR field
            for(int i=0;i<token.length;i++){
                //找後面沒有跟任何字串的Tag
                if(token[i].matches(".*[:]$")){
                    int j = i+1;
                    if(j>= token.length) break;
                    if(!token[j].equals("")) token[i] += "\n";
                    else continue;
                    while(!token[j].equals("")){
                        token[i] += token[j]+"\n";
                        token[j] = "";
                        if(j == token.length -1) break;
                        else j++;
                    }
                }
            }
        }
        //TLD為 .ua的另外處理
        if(TLD.matches("ua")) {
            for(int i=0; i<token.length;i++){
                //移除 % 開頭且非 :結尾的字串及只有%開頭的字串
                if(token[i].matches("^%.*[^:]$|%")){
                    token[i] = "";
                }

            }
            //移除空字串
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(token));
            arrayList.removeIf(item -> item.equals(""));
            token = arrayList.toArray(new String[0]);
            //開始處理field
            for(int i=0;i<token.length;i++){
                //判斷標籤，若符合 % 開頭且 : 結尾的字串就是Tag
                if(token[i].matches("^%.*[:]$")){
                    //把標籤前的 %和空白移除
                    token[i] = token[i].replaceAll("%\\s*","");
                    int j = i+1;
                    if(j >= token.length) break; //若超過index就break
//                    //若字串符合開頭非 % 開頭的字串
                    if(token[j].matches("^[^%].*")) token[i] += "\n";
                    else continue;
                    while(token[j].matches("^[^%].*")){
                        token[i] += token[j]+"\n";
                        token[j] = "";
                        if(j == token.length-1 )break; //不超過index(因為.ua底下不會再有其他說明等等，所以避免陷入無限迴圈)
                        else j++;
                    }
                }
            }


        }
        //針對.gov, .net, .cc做特殊處理(不需要做串接)
        else if(TLD.matches("gov|net|cc|tv|com")){
            /* 只把 % 開頭的字串移除 */
            for(int i=0;i<token.length;i++){
                token[i] = token[i].trim(); //去除字串頭尾空白
                if(token[i].matches("^%.*")){
                    token[i] = "";
                }
            }
        }
        //對 .br 做特殊串接處理
        else if(TLD.matches("br")){
            //先清除前面有 %的字串
            for(int i=0;i< token.length;i++){
                if(token[i].matches("^%.*")){
                    token[i] = "";
                }
            }
            //清除空字串
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(token));
            arrayList.removeIf(item -> item.equals(""));
            token = arrayList.toArray(new String[0]);
            //字串串接
            for(int i=0;i<token.length;i++){
                if(token[i].matches("nic-hdl-br.*")){
                    int j = i+1;
                    if(j >= token.length) break;
                    if(!token[j].matches("nic-hdl-br.*")) token[i] += "\n";
                    while(!token[j].matches("nic-hdl-br.*")){
                        token[i] += token[j] + "\n";
                        token[j] = "";
                        if(j == token.length-1) break;
                        else j++;
                    }

                }
            }
        }
        //.pl 處理完後會跳至這個區塊接續處理
        //處理非例外TLD的字串
        else{
            for(int i=0;i<token.length;i++){
                /* 把前面有 #和 % 的字串清空，或單一開頭為 %、# (多餘字串)*/
                if(token[i].matches("^%.*") || token[i].matches("^#.*")){
                    token[i] = "";
                }
            }
            //移除空字串
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(token));
            arrayList.removeIf(item -> item.equals(""));
            token = arrayList.toArray(new String[0]);
            //串接
            for(int i=0;i< token.length;i++){
                /* 先找標籤，並判斷標籤後的字串是否前面有無數空格 */
                if(token[i].matches(".+:.+|.+:")){ //Tag後面有無東西(.pl TLD Tag後面有東西)
                    int j = i+1; // next index
                    if(j >= token.length) break; // 判斷有無超過index
                    //判斷是否需要串接 前面有空格且結尾不為:的字串
                    if(token[j].matches("\\s{2,}.+[^:)]$")) token[i]+="\n";
                    else continue;
                    while(token[j].matches("\\s{2,}.+[^:)]$")){ // 匹配前面多個空格(\\s{2,}兩個以上空格，多個可視字元.+)
                        token[i] += token[j]+"\n";
                        token[j] = "";
                        if(j == token.length-1 )break; //不超過index
                        else j++;
                    }
                }
            }
        }

        //將空白標籤刪除，並將頭尾空白去除
        for(int i=0;i<token.length;i++){
            token[i] = token[i].trim();
            //移除空白標籤和不含標籤資訊
            if(token[i].matches(".*[:]$") || !token[i].contains(":")){
                token[i] = "";
            }
        }
        //移除所有空字串
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(token));
        arrayList.removeIf(item -> item.equals(""));
        token = arrayList.toArray(new String[0]);

        //印出所有field
        System.out.println("\n########################################################");
        System.out.println("Field切割狀態:");
        arrayList.forEach(a -> System.out.println("-----------------------\n"+a));

//        //測試，列印標籤用以對照用
//        System.out.println("列出去除空白的所有標籤：");
//        for(int i=0;i<token.length;i++){
//            String tag = token[i].substring(0,token[i].indexOf(':'));
//            tag = tag.replaceAll("\\s+",""); //把所有空白置換掉
//            tag = tag.replaceAll("-",""); //把-置換掉
//            System.out.println(tag);
//        }
        System.out.println("\n########################################\n回傳結果：\n");

        //抓取關鍵字並印出
        String TLD_id = "(sponsoring)*"; //TLD為.id的特殊開頭
        String registrar_parameter = "(URL|Organization|City|(State//)*Province|Phone|Email|Country|Handle)*";
        String registrant_parameter = "(Name|Street|City|(State//)*Province|Country|Phone)*";
        String ua_parameter = "(Registrar|URL|Organization|City|(State//)*Province|Phone|Email|Country|Handle|Address|Phone)";

        for(int i=0;i<token.length;i++){
            String tag = token[i].substring(0,token[i].indexOf(':'));
            tag = tag.replaceAll("\\s+",""); //把所有空白置換掉
            tag = tag.replaceAll("-",""); //把-置換掉
            if(tag.matches("(?i)domain(name)*")){
                System.out.println(token[i]);
            }
            //Registrar
            if(tag.matches("(?i)"+TLD_id+"registrar" + registrar_parameter)|| tag.matches("(?i)Owner")){
                //對.ua的Registrar做特別處理
                if(TLD.matches("ua")){
                    String[] temp = token[i].split("\n");
                    System.out.println(temp[0]); //印出Title(主Tag)
                    for(int j=1;j<temp.length;j++){
                        String temp_tag = temp[j].substring(0,temp[j].indexOf(':'));
                        if(temp_tag.matches("(?i)"+ua_parameter)){
                            System.out.println("\t\t"+temp[j]);
                        }
                    }
                }
                else System.out.println(token[i]);
            }
            //Registrant
            if(tag.matches("(?i)registrant" + registrant_parameter)){
                //對.ua的Registrant做特別處理
                if(TLD.matches("ua")){
                    String[] temp = token[i].split("\n");
                    System.out.println(temp[0]); //印出Title(主Tag)
                    for(int j=1;j<temp.length;j++){
                        String temp_tag = temp[j].substring(0,temp[j].indexOf(':'));
                        if(temp_tag.matches("(?i)"+ua_parameter)){
                            System.out.println("\t\t"+temp[j]);
                        }
                    }
                }
                else System.out.println(token[i]);
            }
            //Updated Date
            if(tag.matches("(?i).*Updated.*") || tag.matches("(?i).*Modifi(ed|cation).*")||
               tag.matches("(?i).*Changed.*")|| tag.matches("(?i)RelevantDates")){
                System.out.println(token[i]);
            }
            //Creation Date
            if(tag.matches("(?i).*Creation.*") || tag.matches("(?i).*Created.*")||
               tag.matches("(?i)Registration(Date|Time)")){
                System.out.println(token[i]);
            }
            //Expiry Date
            if(tag.matches("(?i).*Expir(y|es|ation).*") || tag.matches("(?i).*DateRegistered.*") ||
               tag.matches("(?i)PaidTill")){
                System.out.println(token[i]);
            }




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

    // 實作 GoogleApiClient.ConnectionCallbacks,
    // GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}





