/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;


import android.text.TextUtils;
import android.view.LayoutInflater;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/* URL lib */
import androidx.annotation.RequiresApi;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
/* 輸入流 */
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
/* JSON */

import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;


import org.jsoup.nodes.Element;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class UrlCheckActivity extends AegisActivity implements View.OnClickListener, Runnable, DialogInterface.OnClickListener {
    /* 變數宣告 */
    EditText url_input;
    Button set_safe_url;
    Button url_check;
    ImageButton clear_button;
    Button scan_qrcode_button;
    private static final int Scan_QR_CODE = 2;
    private static final String pass_name = "URL_text"; /* 傳遞資料的string名，新增變數避免寫死 */
    private ArrayList<String> issuer;
    private AlertDialog alert_dialog; /* 警告diaolog */
    private AlertDialog IPQS_search_dialog; /* 搜尋 IPQS dialog */
    private ProgressDialog progressDialog; /* 加載 dialog */
    private AlertDialog IPQS_message_dialog; /* 顯示網站資訊 IPQS dialog */
    private AlertDialog message_dialog; /* 顯示提示訊息 dialog */

    private Toast dialog_toast;
    private String api_key = null; /* SafetyNet與 Google Play建立連線用的 API KEY */
    String URL_text = null; /* url_input和qr_code_scan共用的變數，避免判斷時有衝突，判斷完畢後設為null */
    File url_database;


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



        /*測試function*/
        try {
            addMainURL("https://www.google.com/");
            addMainURL("https://github.com/judy8889107?tab=repositories");
            addMainURL("https://www.youtube.com/?gl=TW&hl=zh-TW");
//            addsubURL("https://accounts2.google.com","0","basedomain");
//            addsubURL("https://accounts3.google.com","0","basedomain");
//            addsubURL("https://accounts.google.com","0","basedomain");
//            matchDatabase("https://accounts.google.com");
//            matchDatabase("http://google.com");
//            matchDatabase("http://yahoo.com");
//            deleteMainURL("0");
//            matchDatabase("");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        try {
            displayDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /* 初始化 設定所有參數等等 */
    public void initialize() {
        /* 設定參數 */
        url_input = findViewById(R.id.url_input);
        set_safe_url = findViewById(R.id.set_safe_url);
        url_check = findViewById(R.id.url_check);
        clear_button = findViewById(R.id.clear_button);
        scan_qrcode_button = findViewById(R.id.scan_qrcode_button);
        /* 監聽器設定 */
        set_safe_url.setOnClickListener(this);
        clear_button.setOnClickListener(this);
        scan_qrcode_button.setOnClickListener(this);
        url_check.setOnClickListener(this);


        /* 創立 url database(目前為空) */
        Create_url_database_file();


        /* 建立所有 dialog 和 toast */
        buildAllDialog();


    }

    /* Layout按鈕監聽器事件，實作 View.OnClickListener */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.set_safe_url:
                /* 按下set_safe_url就隱藏鍵盤 */
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(set_safe_url.getWindowToken(), 0);
                URL_text = url_input.getText().toString().trim();
                /* 執行 IPQS 檢查 */
                IPQSCheck();
                break;
            case R.id.clear_button:
                url_input.setText("");
                break;
            case R.id.scan_qrcode_button:
                Intent scan_qrcode_activity = new Intent(getApplicationContext(), UrlCheckActivity_ScanQrcodeActivity.class);
                startActivityForResult(scan_qrcode_activity, Scan_QR_CODE);
                break;
            case R.id.url_check:
                System.out.println("按下url_check，進行網址資料庫比對");
                /* 按下url_check就隱藏鍵盤 */
                imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(url_check.getWindowToken(), 0);
                URL_text = url_input.getText().toString().trim();
                try {
                    matchDatabase(URL_text);
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (TransformerException e) {
                    e.printStackTrace();
                }
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
                /* 將網址輸入input text改為QRcode掃出的內容 */
                url_input.setText(URL_text);
                break;
            default:
                return;  //resultCode為 0 時，return回原本activity
        }
    }

    //設定 message dialog顯示圖片&&文字(重建以更新UI)
    public void setMessageDialog(int id, String msg, boolean enable_no_button) {
        AlertDialog.Builder message_dialog_builder = new AlertDialog.Builder(UrlCheckActivity.this);
        message_dialog_builder.setPositiveButton(R.string.yes, this);
        if (enable_no_button) //開啟取消 button
            message_dialog_builder.setNegativeButton(R.string.no, this);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.dialog_picture, null);
        ImageView imageView = view.findViewById(R.id.safe_scale);
        TextView textView = view.findViewById(R.id.dialog_message_box);
        imageView.setImageResource(id); //設定icon來源
        textView.setText(msg);
        message_dialog_builder.setView(view);
        message_dialog = message_dialog_builder.create();
        message_dialog.dismiss();
    }

    /* 設定所有dialog */
    public void buildAllDialog() {
        /* alert dialog */
        AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(UrlCheckActivity.this);

        alert_dialog_builder.setTitle(R.string.warning);
        /* 設定按鈕監聽器 */
        alert_dialog_builder.setPositiveButton(R.string.yes, this);
        alert_dialog_builder.setNegativeButton(R.string.no, this);
        alert_dialog = alert_dialog_builder.create();
        alert_dialog.dismiss();



        /* IPQS search dialog */
        AlertDialog.Builder IPQS_dialog_builder = new AlertDialog.Builder(UrlCheckActivity.this);
        IPQS_dialog_builder.setTitle(R.string.warning);
        /* 設定按鈕監聽器 */
        IPQS_dialog_builder.setPositiveButton(R.string.yes, this);
        IPQS_dialog_builder.setNegativeButton(R.string.no, this);
        IPQS_search_dialog = IPQS_dialog_builder.create();
        IPQS_search_dialog.dismiss();


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        dialog_toast = Toast.makeText(this.getApplicationContext(), "", Toast.LENGTH_LONG);

    }

    //顯示資料庫
    public void displayDatabase() throws Exception {
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //解析 url_database檔案
        org.w3c.dom.Document doc = db.parse(url_database);
        NodeList nodeList = doc.getElementsByTagName("token");
        LinearLayout scroll_block = this.findViewById(R.id.scroll_block);

        for(int i=0;i<nodeList.getLength();i++){
            Node tokenNode = nodeList.item(i);
            NodeList itemNodes = tokenNode.getChildNodes();
            for(int j=0;j<itemNodes.getLength();j++){
                Node itemNode = itemNodes.item(j);
                String URLstr = itemNode.getTextContent();
                //TextView設定
                TextView textView = new TextView(this);
                textView.setText(URLstr);
                textView.setTextColor(Color.parseColor("#000000"));
                textView.setOnClickListener(this);
                textView.setSingleLine();//設定單行顯示
                textView.setEllipsize(TextUtils.TruncateAt.END); //設定省略符號在尾端
                textView.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);

            }
        }



    }
    /* 實作dialog按鈕監聽 */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(DialogInterface dialog, int which) {
        /* 先判斷哪個dialog */

        /* IPQS_search_dialog */
        if (dialog.equals(IPQS_search_dialog)) {
            /* 判斷哪個按鈕被按下 */
            switch (which) {
                /* 是 */
                case BUTTON_POSITIVE:
                    /* int which = -1 */
                    /* 啟動 whois thread 檢查 */
                    IPQS_search_dialog.dismiss(); //隱藏 dialog再啟動 Thread
                    Thread IPQS_thread = new Thread(this);
                    IPQS_thread.setName("IPQS_thread");
                    progressDialog.setMessage("網址正在IPQS進行檢查中，請稍後...");
                    progressDialog.show();
                    IPQS_thread.start();
                    break;
                case BUTTON_NEGATIVE:
                    /* int which = -2 */
                    alert_dialog.setMessage(URL_text + "\n" + R.string.unsafeURL);
                    alert_dialog.show();
                    break;
            }
        }
        /* IPQS message dialog */
        if (dialog.equals(IPQS_message_dialog)) {
            /* 判斷哪個按鈕被按下 */
            switch (which) {
                /* 是 */
                case BUTTON_POSITIVE:
                    /* int which = -1 */
                    dialog.dismiss();
                    alert_dialog.setMessage(URL_text + "\n" + "請問是否要將此網址加入安全網址資料庫中?");
                    alert_dialog.show();
                    break;
            }
        }
        /* 警告dialog */
        if (dialog.equals(alert_dialog)) {
            /* 判斷哪個按鈕被按下 */
            switch (which) {
                /* 是 */
                case BUTTON_POSITIVE:
                    /* int which = -1 */
                    try {
                        addMainURL(URL_text);
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (TransformerException e) {
                        e.printStackTrace();
                    }
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


    // 設定安全網址 - mainURL加入網址到資料庫中
    public void addMainURL(String url) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        Boolean isExist = false;
        System.out.println("要加入mainURL節點的資料:" + url);
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //解析 url_database檔案
        org.w3c.dom.Document doc = db.parse(url_database);
        //先檢查有無重複網址
        NodeList nodeList = doc.getElementsByTagName("mainURL");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getTextContent().equals(url)) {
                isExist = true;
                break;
            }
        }
        //若此網址從未添加過才寫入xml檔
        if (!isExist) {
            //得到根節點
            org.w3c.dom.Element root = doc.getDocumentElement();
            // 創建新節點
            org.w3c.dom.Element token = doc.createElement("token");
            org.w3c.dom.Element mainURL = doc.createElement("mainURL");
            mainURL.setTextContent(url);
            // 設定 mainURL id
            if (doc.getElementsByTagName("token").getLength() >= 0) {
                int index = doc.getElementsByTagName("token").getLength();
                token.setAttribute("id", String.valueOf(index));
                token.setIdAttribute("id", true);
                mainURL.setAttribute("groupID", String.valueOf(index));
            }
            //設定唯一id
            String uuid = Long.toHexString(System.currentTimeMillis());
            mainURL.setAttribute("uuid",uuid);
            mainURL.setIdAttribute("uuid",true);
            // 新增新節點
            token.appendChild(mainURL);
            root.appendChild(token);
            //寫入xml檔案
            writeXml(doc);
        } else {
            dialog_toast.setText("此網址已存在於資料庫中");
            dialog_toast.show();
        }

    }

    //寫入xml檔案
    public void writeXml(org.w3c.dom.Document doc) throws IOException, TransformerException {
        //開始把 Document對映到檔案
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transFormer = transFactory.newTransformer();
        //設定輸出結果並且生成XML檔案
        DOMSource domSource = new DOMSource(doc);
        FileOutputStream out = new FileOutputStream(url_database);
        StreamResult xmlResult = new StreamResult(out); //設定輸入源
        transFormer.setOutputProperty(OutputKeys.INDENT, "yes"); //元素換行設定
        transFormer.transform(domSource, xmlResult); //輸出xml檔案
        out.close();
    }

    // 刪除 mainURL網址
    public void deleteMainURL(String id) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //解析 url_database檔案
        org.w3c.dom.Document doc = db.parse(url_database);
        // 得到父節點並移除mainURL
        org.w3c.dom.Element root = doc.getDocumentElement();
        org.w3c.dom.Element tokenNode = doc.getElementById(id);
        root.removeChild(tokenNode);
        //mainURL id編號重新命名
        NodeList nodeList = doc.getElementsByTagName("token");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            node.getAttributes().getNamedItem("id").setNodeValue(String.valueOf(i));
        }
        writeXml(doc);
    }

    // 解析並比對資料庫 - 檢查網址
    public void matchDatabase(String url) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        System.out.println("進入matchDatabase");
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //解析 url_database檔案
        org.w3c.dom.Document doc = db.parse(url_database);
        // 創建 mainURLNode元素
        org.w3c.dom.Element mainURLNode = null;
        String format = null;
        String mainURL = null;
        Node node = null;
        String tokenID = null;
        //得到所有節點標籤名為 mainURL的 nodes
        NodeList nodeList = doc.getElementsByTagName("mainURL");
        // 逐一比對
        System.out.println("列出mainURL和id");
        for (int i = 0; i < nodeList.getLength(); i++) {
            node = nodeList.item(i);
            mainURL = node.getTextContent();
            tokenID = node.getAttributes().getNamedItem("groupID").getNodeValue();
            System.out.println(mainURL + " groupID: " + tokenID);

            format = getURLMatchFormat(url, mainURL);
            if (format != null) break;
        }

        // mainURL全無匹配
        if (format == null) {
            System.out.println("mainURL全無匹配");
            setMessageDialog(R.drawable.safe_scale_1, "此網址在資料庫中無任何匹配\n是否進一步檢查此網址？", true);
            message_dialog.show();
            message_dialog.getButton(BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    message_dialog.dismiss();
                    dialog_toast.setText("取消進一步檢查此網址");
                    dialog_toast.show();
                }
            });
            message_dialog.getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    message_dialog.dismiss();
                    IPQSCheck();
                }
            });
        } else {  /* 有匹配到 mainURL */
            /* 比對 subURL */
            if (format.equals("exact") || matchSubURL(tokenID, url, format)) { /*若 mainURL為 exact或 subURL配對成功*/
                setMessageDialog(R.drawable.safe_scale_5, "此為安全網址，可以放心登入", false);
                message_dialog.show();

            } else { /*配對失敗*/
                System.out.println("subURL配對失敗");
                System.out.println(url + "/格式:" + format);
                String str = "此網址安全層級為%s級\n%s此網址不在資料庫中，請問是否要加入資料庫？";
                String hint = "(低於三級網址建議登入後小心使用)\n";
                // 比對級數配對
                switch (format) {
                    case "startwith":
                        str = String.format(str, "四", "");
                        setMessageDialog(R.drawable.safe_scale_4, str, true);
                        break;
                    case "host":
                        str = String.format(str, "三", hint);
                        setMessageDialog(R.drawable.safe_scale_3, str, true);
                        break;
                    case "basedomain":
                        str = String.format(str, "二", hint);
                        setMessageDialog(R.drawable.safe_scale_2, str, true);
                        break;
                }
                message_dialog.show();
                /* 詢問是否加入subURL(監聽器製作) */
                final String final_tokenID = tokenID;
                final String final_format = format;
                message_dialog.getButton(BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        message_dialog.dismiss();
                        dialog_toast.setText("取消添加此網址到資料庫中");
                        dialog_toast.show();

                    }
                });
                message_dialog.getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            addsubURL(url, final_tokenID, final_format);
                            message_dialog.dismiss();
                            dialog_toast.setText("此網址已加入資料庫");
                            dialog_toast.show();
                        } catch (TransformerException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SAXException e) {
                            e.printStackTrace();
                        } catch (ParserConfigurationException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

        }


    }

    //四種比對模式
    public String getURLMatchFormat(String url, String mainURL) throws MalformedURLException {
        System.out.println();
        System.out.println("主URL:" + url);
        System.out.println("mainURL:" + mainURL);
        String format = null;
        //變數
        String maj_basedomain = null;
        String tmp_basedomain = null;
        String maj_host = null;
        String tmp_host = null;
        int maj_port = 0;
        int tmp_port = 0;
        String maj_path = null;
        String tmp_path = null;
        //賦值
        maj_host = new URL(url).getHost();
        tmp_host = new URL(mainURL).getHost();
        maj_basedomain = InternetDomainName.from(maj_host).topPrivateDomain().toString(); //要比對的網址的 domain name
        tmp_basedomain = InternetDomainName.from(tmp_host).topPrivateDomain().toString(); //mainURL網址的 domain name
        maj_port = new URL(url).getPort() < 0 ? new URL(url).getDefaultPort() : new URL(url).getPort();
        tmp_port = new URL(mainURL).getPort() < 0 ? new URL(mainURL).getDefaultPort() : new URL(mainURL).getPort();
        maj_path = new URL(url).getPath();
        tmp_path = new URL(mainURL).getPath();
        //比對
        String maj_startwith = maj_host + ":" + maj_port + maj_path;
        String tmp_startwith = tmp_host + ":" + tmp_port + tmp_path;
        String maj_hoststr = maj_host + ":" + maj_port;
        String tmp_hoststr = tmp_host + ":" + tmp_port;
        System.out.println(maj_basedomain + " " + tmp_basedomain);
        System.out.println(maj_host + " " + tmp_host);
        System.out.println(maj_port + " " + tmp_port);
        System.out.println(maj_path + " " + tmp_path);
        if (url.equals(mainURL)) return "exact";
        if (maj_startwith.contains(tmp_startwith)) return "startwith";
        if (maj_hoststr.contains(tmp_hoststr)) return "host";
        if (maj_basedomain.contains(tmp_basedomain)) return "basedomain";
        return format;
    }

    // 紀錄 subURL 到 database
    public void addsubURL(String url, String tokenID, String format) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        System.out.println("要加入sub節點的資料:" + url);
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //解析 url_database檔案
        org.w3c.dom.Document doc = db.parse(url_database);
        org.w3c.dom.Element tokenNode = doc.getElementById(tokenID);
        //檢查有無添加過,若有則先把舊的那筆刪除
        NodeList nodeList = tokenNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            //判定是否為subURL node
            if (node.hasAttributes()) {
                if (node.getTextContent().equals(url)) {
                    tokenNode.removeChild(node);
                }
            }

        }
        // 創建新節點(subURL)
        org.w3c.dom.Element subURL = doc.createElement("subURL");
        subURL.setTextContent(url);
        subURL.setAttribute("format", format);
        //加入uuid 和 groupID
        String uuid = Long.toHexString(System.currentTimeMillis());
        subURL.setAttribute("groupID", tokenID);
        subURL.setAttribute("uuid",uuid);
        subURL.setIdAttribute("uuid", true);
        // 新增 subURL節點
        tokenNode.appendChild(subURL);
        writeXml(doc);
    }

    //比對 subURL有無exact
    public boolean matchSubURL(String tokenID, String url, String format) throws ParserConfigurationException, IOException, SAXException {
        System.out.println("\n\nfunction matchSubURL");
        Boolean isMatch = false;
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //解析 url_database檔案
        org.w3c.dom.Document doc = db.parse(url_database);
        org.w3c.dom.Element tokenNode = doc.getElementById(tokenID);
        // 創建 變數
        String subURL = null;
        Node node = null;
        String node_format;
        //得到 toeknNode底下的 子 nodes
        NodeList nodeList = tokenNode.getChildNodes();
        System.out.println(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            System.out.println("進入迴圈" + i);
            node = nodeList.item(i);
            //若為subURL才進行判斷
            if (node.getNodeName().equals("subURL")) {
                node_format = node.getAttributes().getNamedItem("format").getNodeValue();
                System.out.println(node_format);
                if (node_format.equals(format)) {
                    subURL = node.getTextContent();
                    if (subURL.equals(url)) return true; //若找到相符的 subURL, 直接返回
                }
            }


        }
        return isMatch;

    }


    /* 檢查URL function */
    public void IPQSCheck() {

        /* 檢查網址是否 valid */
        UrlValidator defaultValidator = new UrlValidator();
        if (defaultValidator.isValid(URL_text)) {
            /* 啟動IPQS thread送出請求 */
            Thread IPQS_thread = new Thread(this);
            IPQS_thread.setName("IPQS_thread");
            progressDialog.setMessage("網址正在IPQS進行檢查中，請稍後...");
            progressDialog.show();
            IPQS_thread.start();
        } else {
            dialog_toast.setText(R.string.parseFail);
            dialog_toast.show();
        }
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

    //IPQualityScore API
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Map<String, String> getIPQualityScore(String URL_text) {
        String result = null;
        Map<String, String> data_map = new LinkedHashMap<>(); //存對應的鍵值
        JSONObject jsonObject = null;
        String IPQualityScore = "https://ipqualityscore.com/api/json/url/mMdf76Tro3JGHcC3Cmv9WPGu14C56Rpm/";

        try {
            String encodedURL = URLEncoder.encode(URL_text, "UTF-8");
            URL url = new URL(IPQualityScore + encodedURL);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() == 200) {
                result = getData(connection.getInputStream());
                connection.disconnect();
                jsonObject = new JSONObject(result);
                //取出要的資料
                String[] key = {"success", "domain", "parking", "spamming", "malware", "phishing", "suspicious", "adult", "risk_score", "category"};
                //若狀態為成功才放入相對應鍵值
                if (jsonObject.getString("success").equals("true")) {
                    //放入鍵值
                    for (int i = 0; i < key.length; i++) {
                        String value = jsonObject.getString(key[i]);
                        if (value.matches("true|false"))
                            value = value.equals("true") ? "yes" : "no";
                        data_map.put(key[i], value);
                    }

                } else {
                    data_map.put(key[0], jsonObject.getString(key[0])); //若連結成功,但為其他狀態則放入其他狀態
                }
            } else {
                /* 若連結失敗則success = false */
                System.out.println(getData(connection.getErrorStream())); //印出失敗資訊
                data_map.put("success", "false");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            data_map.put("success", "false"); /*若有任何例外,成功狀態都設為false值 */
        } finally { /*必定執行 返回 data*/
            return data_map;
        }

    }

    // 處理IPQS輸出資訊
    public String[] getIPQualityMessage(Map<String, String> IPQualityData) {
        String[] result = new String[3];
        if (IPQualityData.get("success").equals("false")) {
            result[0] = "錯誤";
            result[1] = "服務取得失敗，請重新嘗試";
        } else { /*其他狀態 */
            String[] key = null;
            //設定 key值 (中文和其他為英文)
            if (Locale.getDefault().getDisplayLanguage().equals("中文")) {
                key = new String[]{"網站風險分數", "域名",
                        "網站是否有域名停留", "網站是否濫發垃圾郵件", "網站是否含惡意軟體",
                        "網站是否為釣魚網站", "網站是否可疑", "網站是否含成人內容", "網站分類"};

            } else {
                key = new String[]{"Risk Score", "Domain Name",
                        "Website has a domain name suspension", "Website is spamming",
                        "Website contains malware", "Website is a Phishing Website",
                        "Website is Suspicious", "Website contains Adult Content", "Website Category"};

            }
            //拼接 &&設定 Dialog title和 message
            StringBuilder sb = new StringBuilder();
            Iterator<Map.Entry<String, String>> iterator = IPQualityData.entrySet().iterator();
            int index = 1;
            result[0] = IPQualityData.get("risk_score"); //取得分數
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String entryKey = entry.getKey();
                String value = entry.getValue();
                if (entryKey.matches("success|risk_score")) continue;
                if (value.matches("yes|no"))
                    value = value.matches("yes") ? "是" : "否";
                sb.append(key[index] + ": " + value + "\n");
                index++;
            }
            result[1] = sb.toString();
            System.out.println("後臺測試用:\n" + result[0] + "\n" + result[1]);
        }
        return result;
    }

    public void setIPQSDialog(String risk_score, String msg) {
        int score;
        /* IPQS資訊 dialog */
        AlertDialog.Builder IPQS_message_builder = new AlertDialog.Builder(UrlCheckActivity.this);
        /* 設定按鈕監聽器 */
        IPQS_message_builder.setPositiveButton(R.string.yes, this);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.ipqs_msg_display, null);
        TextView ipqs_score = view.findViewById(R.id.ipqs_score);
        TextView ipqs_msg = view.findViewById(R.id.ipqs_msg);
        ipqs_score.setText(risk_score);
        ipqs_msg.setText(msg);
        /* 評判風險分數並換顏色 */
        score = Integer.valueOf(risk_score);
        if (0 <= score && score <= 20)
            ipqs_score.setTextColor(Color.parseColor("#457c0d"));
        else if (21 <= score && score <= 40)
            ipqs_score.setTextColor(Color.parseColor("#78c430"));
        else if (41 <= score && score <= 60)
            ipqs_score.setTextColor(Color.parseColor("#fec721"));
        else if (61 <= score && score <= 80)
            ipqs_score.setTextColor(Color.parseColor("#f65922"));
        else
            ipqs_score.setTextColor(Color.parseColor("#d63839"));
        IPQS_message_builder.setView(view);
        IPQS_message_dialog = IPQS_message_builder.create();
        IPQS_message_dialog.dismiss();
    }

    /* implements Runnable(subThread會執行裡面內容) */
    /* 執行 IPQS search */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void run() {
        Map<String, String> IPQualityScore_data = null;
        String message;
        String risk_score;
        try {
            //IPQualityScore使用
            IPQualityScore_data = getIPQualityScore(URL_text);
            System.out.println("逐行印出原始訊息:");
            IPQualityScore_data.entrySet().forEach(entry -> {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            });
            System.out.println("------------------------------------------");
            message = getIPQualityMessage(IPQualityScore_data)[1];
            risk_score = getIPQualityMessage(IPQualityScore_data)[0];
//            執行 Thread UI更新
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                    setIPQSDialog(risk_score, message);
                    IPQS_message_dialog.show();
                }
            });
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());

        }


    }

    /* 建立 url database的檔案 */
    public void Create_url_database_file() {
        /* Create file */
        File dir = getApplicationContext().getFilesDir();
        url_database = new File(dir, "url_database.xml");
        url_database.setWritable(true);  // 設為可讀寫
        url_database.setReadable(true);
        try {
            if (url_database.createNewFile()) {
                //建立一個 Document類
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = factory.newDocumentBuilder();
                //建立一個根節點，並且將根節點新增到Document物件中去
                org.w3c.dom.Document doc = db.newDocument();
                org.w3c.dom.Element root = doc.createElement("root");
                doc.appendChild(root);

                //開始把Document對映到檔案
                TransformerFactory transFactory = TransformerFactory.newInstance();
                Transformer transFormer = transFactory.newTransformer();
                //設定輸出結果並且生成XML檔案
                DOMSource domSource = new DOMSource(doc);
                File file = url_database;
                FileOutputStream out = new FileOutputStream(file);
                StreamResult xmlResult = new StreamResult(out); //設定輸入源
                transFormer.transform(domSource, xmlResult); //輸出xml檔案
                System.out.println("成功創建url database 檔案");

            } else {
                System.out.println("url database檔案已存在");
            }
        } catch (IOException | ParserConfigurationException | TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }


    }

    /* 沒用到的程式碼 */
    /* ================================================================================================================== */
    /* ================================================================================================================== */
    /* ================================================================================================================== */
    /* ================================================================================================================== */
    /* ================================================================================================================== */
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
        for (Map.Entry<String, String> entry : data.entrySet()) {
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
        String[] tag = {"domain", "create_date", "update_date", "expire_date", "domain_age", "error_code"};
        for (String s : tag) {
            Element element = doc.getElementsByTag(s).first();
            String element_str = (element == null) ? null : element.text();  //判斷式 ？ 若判斷為真執行區塊 ： 若判斷為假執行區塊
            data_map.put(s, element_str);

        }

        return data_map;
    }


    //VirusTotal返回結果
    public Map<String, Integer> getAalysisResult(String URL_text) throws JSONException, IOException, InterruptedException {
        String analysisID = getAnalysisID(URL_text);
        String x_apikey = "b022681243b4c4217ac2ae51dffbe1f82babf2855816347e1de6e92e66f65714";
        String status = null;
        JSONObject jsonObject = null;
        Map<String, Integer> data_map = new LinkedHashMap<>();

        //建立連線
        HttpURLConnection connection = (HttpURLConnection) new URL("https://www.virustotal.com/api/v3/analyses/" + analysisID).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("x-apikey", x_apikey);
        System.out.println("建立連線");
        connection.connect();
        if (200 == connection.getResponseCode()) {
            String result = getData(connection.getInputStream());
            jsonObject = new JSONObject(result).getJSONObject("data").getJSONObject("attributes");
            status = jsonObject.getString("status");
            data_map.put("status", status.equals("completed") ? 1 : 0); //若 complete status=1, 其他則 status = 0
            //放入鍵和鍵值
            jsonObject = jsonObject.getJSONObject("stats");
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                data_map.put(key, jsonObject.getInt(key));
            }
        } else System.out.println(getData(connection.getErrorStream()));

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
        String parameter = "url=" + URLEncoder.encode(URL_text, "UTF-8");
        outputStream.write(parameter.getBytes(StandardCharsets.UTF_8));
        connection.connect();
        if (200 == connection.getResponseCode()) {
            String result = getData(connection.getInputStream());
            analysisID = new JSONObject(result).getJSONObject("data").getString("id");
        } else {
            System.out.println(getData(connection.getErrorStream()));

        }
        outputStream.close(); //關閉寫入流
        connection.disconnect(); //關閉連接
        return analysisID;
    }

    /* 在 XML 中找 whois server */
    public String get_whois_server(String xdot_text) {

        String whois_server = null;
        /* 利用 resources讀取 res/xml中檔案 */
        XmlResourceParser server_file = getResources().getXml(R.xml.whois_server);
        boolean isFind = false;
        try {
            int event = server_file.getEventType(); /* 得到現在光標的位置 */
            while (event != XmlPullParser.END_DOCUMENT) { /* 當光標還未到文件結尾 */
                if (event == XmlPullParser.TEXT) { /* 若得到的是文字(非 XmlPullParser.START_TAG <XXX></XXX> ) */
                    if (isFind) {
                        whois_server = server_file.getText(); /* 找到的server給whois_server, whois_server就不為空了 */
                        break;
                    }
                    if (xdot_text.equalsIgnoreCase(server_file.getText())) {
                        isFind = true;
                    }
                }

                if (whois_server != null) break;
                event = server_file.next(); /* 移動光標 */
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }


        return whois_server;

    }

    /* information欄位切割 (註：若非英文的information 將不欄位處理，但會進行翻譯)*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void splitting_filed(String TLD, String msg) {

        System.out.println("############################################\n原始訊息:\n" + msg);


        /* 先將可以處理的大致處理 */
        msg = msg.replaceAll("_", " ");
        msg = msg.replaceAll("\\.{2,}", ""); //匹配一長串..... .no TLD
        msg = msg.replaceAll("\r", ""); //把 \r置換(\r是將光標移動到行首)
//        System.out.println("\n\nReplace過後:\n"+msg+"\n----------------------------");

        /* 分割字串 */
//        System.out.println("\n\n=================================================================================\n");

        String[] token = msg.split("\n");
        //TLD 為.pl的另外處理，但.pl的REGISTRAR 還要再處理其他前面有空白的欄位。所以會在下面 if判斷.ua錯誤，執行 else再把其他欄位處理好
        if (TLD.equals("pl")) {
            //處理 REGISTRAR field
            for (int i = 0; i < token.length; i++) {
                //找後面沒有跟任何字串的Tag
                if (token[i].matches(".*[:]$")) {
                    int j = i + 1;
                    if (j >= token.length) break;
                    if (!token[j].equals("")) token[i] += "\n";
                    else continue;
                    while (!token[j].equals("")) {
                        token[i] += token[j] + "\n";
                        token[j] = "";
                        if (j == token.length - 1) break;
                        else j++;
                    }
                }
            }
        }
        //TLD為 .ua的另外處理
        if (TLD.matches("ua")) {
            for (int i = 0; i < token.length; i++) {
                //移除 % 開頭且非 :結尾的字串及只有%開頭的字串
                if (token[i].matches("^%.*[^:]$|%")) {
                    token[i] = "";
                }

            }
            //移除空字串
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(token));
            arrayList.removeIf(item -> item.equals(""));
            token = arrayList.toArray(new String[0]);
            //開始處理field
            for (int i = 0; i < token.length; i++) {
                //判斷標籤，若符合 % 開頭且 : 結尾的字串就是Tag
                if (token[i].matches("^%.*[:]$")) {
                    //把標籤前的 %和空白移除
                    token[i] = token[i].replaceAll("%\\s*", "");
                    int j = i + 1;
                    if (j >= token.length) break; //若超過index就break
//                    //若字串符合開頭非 % 開頭的字串
                    if (token[j].matches("^[^%].*")) token[i] += "\n";
                    else continue;
                    while (token[j].matches("^[^%].*")) {
                        token[i] += token[j] + "\n";
                        token[j] = "";
                        if (j == token.length - 1) break; //不超過index(因為.ua底下不會再有其他說明等等，所以避免陷入無限迴圈)
                        else j++;
                    }
                }
            }


        }
        //針對.gov, .net, .cc做特殊處理(不需要做串接)
        else if (TLD.matches("gov|net|cc|tv|com")) {
            /* 只把 % 開頭的字串移除 */
            for (int i = 0; i < token.length; i++) {
                token[i] = token[i].trim(); //去除字串頭尾空白
                if (token[i].matches("^%.*")) {
                    token[i] = "";
                }
            }
        }
        //對 .br 做特殊串接處理
        else if (TLD.matches("br")) {
            //先清除前面有 %的字串
            for (int i = 0; i < token.length; i++) {
                if (token[i].matches("^%.*")) {
                    token[i] = "";
                }
            }
            //清除空字串
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(token));
            arrayList.removeIf(item -> item.equals(""));
            token = arrayList.toArray(new String[0]);
            //字串串接
            for (int i = 0; i < token.length; i++) {
                if (token[i].matches("nic-hdl-br.*")) {
                    int j = i + 1;
                    if (j >= token.length) break;
                    if (!token[j].matches("nic-hdl-br.*")) token[i] += "\n";
                    while (!token[j].matches("nic-hdl-br.*")) {
                        token[i] += token[j] + "\n";
                        token[j] = "";
                        if (j == token.length - 1) break;
                        else j++;
                    }

                }
            }
        }
        //.pl 處理完後會跳至這個區塊接續處理
        //處理非例外TLD的字串
        else {
            for (int i = 0; i < token.length; i++) {
                /* 把前面有 #和 % 的字串清空，或單一開頭為 %、# (多餘字串)*/
                if (token[i].matches("^%.*") || token[i].matches("^#.*")) {
                    token[i] = "";
                }
            }
            //移除空字串
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(token));
            arrayList.removeIf(item -> item.equals(""));
            token = arrayList.toArray(new String[0]);
            //串接
            for (int i = 0; i < token.length; i++) {
                /* 先找標籤，並判斷標籤後的字串是否前面有無數空格 */
                if (token[i].matches(".+:.+|.+:")) { //Tag後面有無東西(.pl TLD Tag後面有東西)
                    int j = i + 1; // next index
                    if (j >= token.length) break; // 判斷有無超過index
                    //判斷是否需要串接 前面有空格且結尾不為:的字串
                    if (token[j].matches("\\s{2,}.+[^:)]$")) token[i] += "\n";
                    else continue;
                    while (token[j].matches("\\s{2,}.+[^:)]$")) { // 匹配前面多個空格(\\s{2,}兩個以上空格，多個可視字元.+)
                        token[i] += token[j] + "\n";
                        token[j] = "";
                        if (j == token.length - 1) break; //不超過index
                        else j++;
                    }
                }
            }
        }

        //將空白標籤刪除，並將頭尾空白去除
        for (int i = 0; i < token.length; i++) {
            token[i] = token[i].trim();
            //移除空白標籤和不含標籤資訊
            if (token[i].matches(".*[:]$") || !token[i].contains(":")) {
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
        arrayList.forEach(a -> System.out.println("-----------------------\n" + a));

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

        for (int i = 0; i < token.length; i++) {
            String tag = token[i].substring(0, token[i].indexOf(':'));
            tag = tag.replaceAll("\\s+", ""); //把所有空白置換掉
            tag = tag.replaceAll("-", ""); //把-置換掉
            if (tag.matches("(?i)domain(name)*")) {
                System.out.println(token[i]);
            }
            //Registrar
            if (tag.matches("(?i)" + TLD_id + "registrar" + registrar_parameter) || tag.matches("(?i)Owner")) {
                //對.ua的Registrar做特別處理
                if (TLD.matches("ua")) {
                    String[] temp = token[i].split("\n");
                    System.out.println(temp[0]); //印出Title(主Tag)
                    for (int j = 1; j < temp.length; j++) {
                        String temp_tag = temp[j].substring(0, temp[j].indexOf(':'));
                        if (temp_tag.matches("(?i)" + ua_parameter)) {
                            System.out.println("\t\t" + temp[j]);
                        }
                    }
                } else System.out.println(token[i]);
            }
            //Registrant
            if (tag.matches("(?i)registrant" + registrant_parameter)) {
                //對.ua的Registrant做特別處理
                if (TLD.matches("ua")) {
                    String[] temp = token[i].split("\n");
                    System.out.println(temp[0]); //印出Title(主Tag)
                    for (int j = 1; j < temp.length; j++) {
                        String temp_tag = temp[j].substring(0, temp[j].indexOf(':'));
                        if (temp_tag.matches("(?i)" + ua_parameter)) {
                            System.out.println("\t\t" + temp[j]);
                        }
                    }
                } else System.out.println(token[i]);
            }
            //Updated Date
            if (tag.matches("(?i).*Updated.*") || tag.matches("(?i).*Modifi(ed|cation).*") ||
                    tag.matches("(?i).*Changed.*") || tag.matches("(?i)RelevantDates")) {
                System.out.println(token[i]);
            }
            //Creation Date
            if (tag.matches("(?i).*Creation.*") || tag.matches("(?i).*Created.*") ||
                    tag.matches("(?i)Registration(Date|Time)")) {
                System.out.println(token[i]);
            }
            //Expiry Date
            if (tag.matches("(?i).*Expir(y|es|ation).*") || tag.matches("(?i).*DateRegistered.*") ||
                    tag.matches("(?i)PaidTill")) {
                System.out.println(token[i]);
            }
        }

    }

    /* 分析aegis.json檔，並把issuer放入arrayList issuer裡面 */
    public void Create_issuer_arrayList() {
        /* 打開 aegis.json 轉換 && 解析JSON檔，並創建 issuer arraylist */
        File f = new File(getApplicationContext().getFilesDir(), "aegis.json");
        BufferedReader br;
        String aegis_json_string = ""; /* File JSON檔轉為String */
        JSONObject jsonObject; /* String再建立成jsonObject */
        JSONArray jsonArray;   /* 用來解析jsonObject */
        issuer = new ArrayList<>(); /* 利用ArrayList儲存issuer */
        try {
            /* 讀取 file轉換成String，因為JDK版本關係要用 BufferedReader轉(用BufferedReader是因為讀取效率高) */
            br = new BufferedReader(new FileReader(f));
            while (br.ready()) {
                aegis_json_string += br.readLine();
            }
            br.close();
            /* 創立並解析 JSON物件 */
            jsonObject = new JSONObject(aegis_json_string);
            jsonObject = jsonObject.getJSONObject("db");
            jsonArray = jsonObject.getJSONArray("entries");
            for (int i = 0; i < jsonArray.length(); i++) {
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





