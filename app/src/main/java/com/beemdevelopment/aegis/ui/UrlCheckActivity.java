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


import android.view.View;


import com.beemdevelopment.aegis.R;


import com.google.android.gms.common.api.GoogleApiClient;
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



import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
/* 輸入流 */
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
/* JSON */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class UrlCheckActivity extends AegisActivity implements View.OnClickListener,Runnable, DialogInterface.OnClickListener {
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
                URL_text = url_input.getText().toString().trim();
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
//        whois_message_builder.setNegativeButton(R.string.no, this);
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
                    //使當前thread進入等待，等待 whois thread完成
                    try {
                        whois_thread.join();

                    }
                    catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    whois_message_dialog.show();
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
                    alert_dialog.setMessage(URL_text+"\n"+getResources().getString(R.string.unsafeURL));
                    alert_dialog.show();
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
                    dialog_toast.setText("不添加此網址到安全名單中"); /* 此網址不會添加到安全名單 */
                    dialog_toast.show();
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


    // 輸入inputstream，串接回傳資訊
    public static String getData(InputStream inputStream) throws IOException {
        String result = null;
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        result = sb.toString();
        return result;
    }

    //IP2WHOIS 使用
    public Map<String, String> getIP2WHOIS(String URL_text) throws IOException {

        Map<String, String> data_map = new LinkedHashMap<String, String>();

        String result = null;
        String key = "TZ6JJY5XVPJH5TOI6R2KQIVD9Y9IB2UX"; //My api key
        Hashtable<String, String> data = new Hashtable<String, String>();
        String domain = InternetDomainName.from(new URL(URL_text).getHost()).topDomainUnderRegistrySuffix().toString(); /* 得到 Domain name */
        data.put("domain", domain);
        data.put("format", "xml");
        String datastr = "";
        for (Map.Entry<String,String> entry : data.entrySet()) {
            datastr += "&" + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
        }
        //建立連線
        URL url = new URL("https://api.ip2whois.com/v2?key=" + key + datastr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }
        result = getData(conn.getInputStream());
        conn.disconnect();

        Document doc = Jsoup.parse(result);
        String[] tag = {"domain","create_date","update_date","expire_date","domain_age", "error_code"};
        for (String s : tag) {
            Element element = doc.getElementsByTag(s).first();
            String element_str = (element == null) ? null : element.text();  //判斷式 ？ 若判斷為真執行區塊 ： 若判斷為假執行區塊
            data_map.put(s, element_str);

        }

        return data_map;
    }


    //VirusTotal返回結果
    public Map<String,Integer> getAalysisResult(String URL_text) throws JSONException, IOException, InterruptedException {
        String analysisID = getAnalysisID(URL_text);
        String x_apikey = "b022681243b4c4217ac2ae51dffbe1f82babf2855816347e1de6e92e66f65714";
        String status = null;
        JSONObject jsonObject = null;
        Map<String, Integer> data_map = new LinkedHashMap<>();

        //建立連線
        HttpURLConnection connection = (HttpURLConnection) new URL("https://www.virustotal.com/api/v3/analyses/"+analysisID).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("x-apikey", x_apikey);
        System.out.println("建立連線");
        connection.connect();
        if(200 == connection.getResponseCode()){
            String result = getData(connection.getInputStream());
            jsonObject = new JSONObject(result).getJSONObject("data").getJSONObject("attributes");
            status = jsonObject.getString("status");
            data_map.put("status",status.equals("completed")? 1 : 0); //若 complete status=1, 其他則 status = 0
            //放入鍵和鍵值
            jsonObject = jsonObject.getJSONObject("stats");
            Iterator<String> iterator = jsonObject.keys();
            while(iterator.hasNext()){
                String key = iterator.next();
                data_map.put(key,jsonObject.getInt(key));
            }
        }else System.out.println(getData(connection.getErrorStream()));

        connection.disconnect();
        return data_map;
    }
    //VirusTotal得到分析ID
    public String getAnalysisID(String URL_text) throws IOException, JSONException {
        String x_apikey = "b022681243b4c4217ac2ae51dffbe1f82babf2855816347e1de6e92e66f65714";
        String analysisID = null;
        HttpURLConnection connection = (HttpURLConnection) new URL("https://www.virustotal.com/api/v3/urls").openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("x-apikey", x_apikey);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setUseCaches(false);
        //需要先寫入流再做connection
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        String parameter = "url="+URLEncoder.encode(URL_text,"UTF-8");
        outputStream.write(parameter.getBytes(StandardCharsets.UTF_8));
        connection.connect();
        if(200 == connection.getResponseCode()){
            String result = getData(connection.getInputStream());
            analysisID = new JSONObject(result).getJSONObject("data").getString("id");
        }else{
            System.out.println(getData(connection.getErrorStream()));

        }
        outputStream.close(); //關閉寫入流
        connection.disconnect(); //關閉連接
        return analysisID;
    }

    //IPQualityScore API
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Map<String,String> getIPQualityScore(String URL_text) throws IOException, JSONException {
        String result = null;
        Map<String, String> data_map = new LinkedHashMap<>(); //存對應的鍵值
        JSONObject jsonObject = null;
        String IPQualityScore = "https://ipqualityscore.com/api/json/url/mMdf76Tro3JGHcC3Cmv9WPGu14C56Rpm/";
        String encodedURL = URLEncoder.encode(URL_text,"UTF-8");
        URL url = new URL(IPQualityScore+encodedURL);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        if (connection.getResponseCode() == 200) {
            result = getData(connection.getInputStream());
            connection.disconnect();
            jsonObject = new JSONObject(result);
            //取出要的資料
            String[] key = {"success","unsafe","domain","parking","spamming","malware","phishing","suspicious","adult","risk_score","category"};
            //若狀態為成功才放入相對應鍵值
            if(jsonObject.getString("success").equals("true")){
                //放入鍵值
                for(int i=0;i<key.length;i++){
                    String value = jsonObject.getString(key[i]);
                    if(value.matches("true|false"))
                        value = value.equals("true")? "yes":"no";
                    data_map.put(key[i],value);
                }

            }
            else data_map.put(key[0],jsonObject.getString(key[0])); //狀態失敗則放success = false


        }else System.out.println(getData(connection.getErrorStream())); //印出失敗資訊

        return data_map;

    }

    /* implements Runnable(subThread會執行裡面內容) */
    /* 執行 whois search */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void run() {


        //IP2WHOIS API Data
        Map<String, String> IP2WHOIS_data = null;
        Map<String, Integer> Virus_data = null;
        Map<String, String> IPQualityScore_data = null;
        String message = null;
        String title = null;
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
            // IP2WHOIS 使用
//            IP2WHOIS_data = getIP2WHOIS(URL_text);
//            System.out.println("data_map內容:");
//            System.out.println(IP2WHOIS_data);

//            VirusTotal 使用
//            System.out.println("要檢查的網址:");
//            System.out.println(URL_text);
//            Map<String, Integer> data_map = getAalysisResult(URL_text);
//            if(data_map.get("status") == 0){
//                System.out.println("請求尚在排隊處理中，請重新嘗試");
//                System.out.println(data_map);
//            }
//            else{
//                System.out.println("請求處理完畢，印出結果:");
//                System.out.println(data_map);
//            }

            //IPQualityScore使用
            //IPQualityScore使用
            IPQualityScore_data = getIPQualityScore(URL_text);
            System.out.println("逐行印出原始訊息:");
            IPQualityScore_data.entrySet().forEach(entry->{
                System.out.println(entry.getKey() + ": " + entry.getValue());
            });
            System.out.println("------------------------------------------");
            //設定輸出訊息
            if(IPQualityScore_data.get("success").equals("false")){
                message = "服務取得失敗，請求檢查網址次數已達上限";
            }
            else{
                String[] key = null;
                //設定 key值
                if(Locale.getDefault().getDisplayLanguage().equals("中文")){
                    key = new String[]{"網站風險分數", "是否為不安全的網站", "域名",
                            "網站是否有域名停留", "網站是否濫發垃圾郵件", "網站是否含惡意軟體",
                            "網站是否為釣魚網站", "網站是否可疑", "網站是否含成人內容", "網站分類"};

                }else{
                    key = new String[]{"Risk Score", "Unsafe Website", "Domain Name",
                            "Website has a domain name suspension", "Website is spamming",
                            "Website contains malware", "Website is a Phishing Website",
                            "Website is Suspicious", "Website contains Adult Content", "Website Category"};

                }
                //拼接 &&設定 Dialog title和 message
                StringBuilder sb = new StringBuilder();
                Iterator<Map.Entry<String, String>> iterator = IPQualityScore_data.entrySet().iterator();
                int index = 1;
                title = key[0]+" "+IPQualityScore_data.get("risk_score");
//                    sb.append(chinese_key[0]+": "+IPQualityScore_data.get("risk_score")+"\n");
                while(iterator.hasNext()){
                    Map.Entry<String, String> entry = iterator.next();
                    String entryKey = entry.getKey();
                    String value = entry.getValue();
                    if(entryKey.matches("success|risk_score")) continue;
                    if(value.matches("yes|no"))
                        value =value.matches("yes")? "是":"否";
                    sb.append(key[index]+": "+value+"\n");
                    index++;
                }
                message = sb.toString();
                System.out.println("後臺測試用:\n"+title+"\n"+message);

                whois_message_dialog.setTitle(title);
                whois_message_dialog.setMessage(message);
                Thread.currentThread().interrupted(); //中斷執行緒

            }







//            System.out.println("嘗試發出SafetyNet證明請求");
//            //初始化SafeNet API
//            Tasks.await(SafetyNet.getClient(this).initSafeBrowsing());
//            //Use SafetyNet
//            System.out.println("想檢查的URL:");
//            System.out.println(URL_text);
//            SafetyNet.getClient(this).lookupUri(URL_text, "AIzaSyAK5QxYVa3JZ4pXc9GbgzJ0bp4VkEZeQtU",
//                    SafeBrowsingThreat.TYPE_POTENTIALLY_HARMFUL_APPLICATION,
//                    SafeBrowsingThreat.TYPE_SOCIAL_ENGINEERING)
//                    .addOnSuccessListener(this,
//                            new OnSuccessListener<SafetyNetApi.SafeBrowsingResponse>() {
//                                @Override
//                                public void onSuccess(SafetyNetApi.SafeBrowsingResponse sbResponse) {
//                                    // Indicates communication with the service was successful.
//                                    // Identify any detected threats.
//                                    System.out.println("SafetyNet響應成功");
//                                    if (sbResponse.getDetectedThreats().isEmpty()) {
//                                        // No threats found.
//                                        System.out.println(sbResponse.getDetectedThreats());
//                                        System.out.println(sbResponse.getState());
//                                        System.out.println("沒有檢查到任何威脅");
//                                    } else {
//                                        // Threats found!
//                                        System.out.println("檢查到威脅!!!");
//                                    }
//                                }
//                            })
//                    .addOnFailureListener(this, new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            System.out.println("取得服務失敗");
//                            // An error occurred while communicating with the service.
//                            if (e instanceof ApiException) {
//                                System.out.println("Google Play Service的API出現error");
//                                // An error with the Google Play Services API contains some
//                                // additional details.
//                                ApiException apiException = (ApiException) e;
//                                System.out.println(CommonStatusCodes
//                                        .getStatusCodeString(apiException.getStatusCode()));
//                                System.out.println(e.getMessage());
//
//                                // Note: If the status code, apiException.getStatusCode(),
//                                // is SafetyNetstatusCode.SAFE_BROWSING_API_NOT_INITIALIZED,
//                                // you need to call initSafeBrowsing(). It means either you
//                                // haven't called initSafeBrowsing() before or that it needs
//                                // to be called again due to an internal error.
//                            } else {
//                                System.out.println("其他 error:");
//                                // A different, unknown type of error occurred.
//                                System.out.println(e.getMessage());
//                            }
//                        }
//                    });





            /* 建立Whois連線 */

//            domain_name = InternetDomainName.from(host).topPrivateDomain().toString(); /* 得到 Domain name */
//            TLD = host.substring(host.lastIndexOf('.')+1);
//            /* 查詢whois_server.xml 得到 Whois_server */
//            whois_server = get_whois_server(TLD);
//            if (whois_server == null) { /* 若返回的伺服器為空(xml檔案中找不到可以配對的伺服器) */
//                msg = "Sorry, no match whois server."; /* 設定 msg不為空 */
//            } else {
//                /* 連接剛剛得到的 whois server找尋資料 */
//                whois.connect(whois_server);
//                msg = whois.query(domain_name);
//                whois.disconnect();
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
          System.out.println(e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {

//            /* 將原本訊息備份 */
////            origin_msg = msg;
//            /* 傳TLD和origin_msg 做欄位切割 */
////            splitting_filed(TLD, msg);  Judy 等等恢復註解
//            /* 處理 message翻譯 */
//            /* https://translate.googleapis.com/translate_a/single?client=gtx&sl={fromCulture}&tl={toCulture}&dt=t&q={text} */
//            /* auto -> 中文 */
////            String translation_url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=zh_tw&dt=t&q="+msg;
////            try {
////                /* 連到 Google Translation的 URL */
////                obj = new URL(translation_url);
////                HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
////                httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
//////
////                /* 讀取得到的資訊 */
////                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
////                String inputLine;
////                StringBuilder stringBuilder = new StringBuilder();
////
////                /* 得到JSON資訊 */
////                while((inputLine = in.readLine())!=null){
////                    stringBuilder.append(inputLine);
////                }
////                in.close(); /* 關閉 Read */
////                msg = stringBuilder.toString();
////
////                /* 傳入原始訊息和翻譯過後的訊息，將JSON資訊做處理，並停止Thread */
////                handle_message(origin_msg,msg);
////                Thread.sleep(9999); /* Thread等待9999毫秒 */
////                Thread.currentThread().interrupt(); /* 發出中斷訊號，通知 currentThread 進行中斷 */
////
////            } catch (IOException | InterruptedException e) {
////                System.out.println(e.getMessage());
////                e.printStackTrace();
////            }

        }







    }





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



}





