/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;


import android.os.Vibrator;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;


import com.beemdevelopment.aegis.R;


import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.google.android.gms.vision.text.Text;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.net.InternetDomainName;

import java.io.IOException;

import android.view.inputmethod.InputMethodManager;

/* 使用EditText */
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
/* 控制鍵盤 */
/* ImageButton的import */
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/* URL lib */
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
/* 輸入流 */
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
/* JSON */

import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONException;
import org.json.JSONObject;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

class Struct {
    public static class urlObject {
        public String tagName, uuid, text, format = "none";
    }
}

public class UrlCheckActivity extends AegisActivity implements Runnable {
    /* 變數宣告 */
    public MyListener myListener;
    EditText local_url_input;
    EditText online_url_input;
    FloatingActionButton online_check_add_btn;
    ImageButton local_check_send_btn;
    ImageButton local_input_right_btn;
    ImageButton online_input_right_btn;
    androidx.appcompat.widget.Toolbar toolbar;
    InputMethodManager imm;
    BottomSheetDialog buttomDialog;
    View dialog_online_check_add_entry_view;
    View dialog_progress_view;
    View dialog_online_check_result;
    View dialog_local_check_result;
    private static final int Scan_QR_CODE = 2;
    private static final String pass_name = "URL_text"; /* 傳遞資料的string名，新增變數避免寫死 */
    private Toast dialog_toast;
    private Snackbar snackbar;
    private String api_key = null; /* SafetyNet與 Google Play建立連線用的 API KEY */
    String URL_text = null; /* local_url_input和qr_code_scan共用的變數，避免判斷時有衝突，判斷完畢後設為null */
    File url_database;
    private HashMap<Integer, ArrayList<Struct.urlObject>> url_database_list;
    private ArrayList<Struct.urlObject> PD_urlObjects;
    private ExpandableListView expandableListView;
    private MyBaseExpandableListAdapter myAdapter;
    private ClipboardManager cmb;
    private Vibrator myVibrator;
    private long firstPressedTime;

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
        try {
            initialize();
//            addMainURL("http://google.com");
//            addMainURL("https://github.com/");
//            addMainURL("http://www.eyny.com/");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }


    }

    //捕捉返回鍵, 寫入到外部記憶體後離開
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //TODO:長按返回事件
            if (myListener.isLongClick) {
                if (PD_urlObjects.size() == 0 || (System.currentTimeMillis() - firstPressedTime < 2000)) {
                    myListener.isLongClick = false;
                    online_check_add_btn.setImageDrawable(getDrawable(R.drawable.ic_add_black_24dp));
                    online_check_add_btn.setTag(R.drawable.ic_add_black_24dp);
                    refreshUI();
                    setSnackbar("已退出操作", "", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    PD_urlObjects.clear();
                    return false;
                } else { //當選擇數目不為 0 時
                    myVibrator.vibrate(300);
                    setSnackbar("請重新按下返回鍵以退出", "", Snackbar.LENGTH_INDEFINITE);
                    firstPressedTime = System.currentTimeMillis();
                    return false;
                }
            } else {
                try {
                    write_url_database();
                    this.finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    //点击空白区域 自动隐藏软键盘
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (this.getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        if (!buttomDialog.isShowing())
            imm.hideSoftInputFromWindow(buttomDialog.getWindow().getDecorView().getWindowToken(), 0);
        return super.dispatchTouchEvent(event);
    }

    /* 初始化 設定所有參數等等 */
    public void initialize() throws Exception {
        /* 設定參數 */
        myListener = new MyListener();
        local_url_input = findViewById(R.id.url_input);
        online_check_add_btn = findViewById(R.id.online_check_add_btn);
        local_check_send_btn = findViewById(R.id.local_check_send_btn);
        local_input_right_btn = findViewById(R.id.local_input_right_btn);
        toolbar = findViewById(R.id.toolbar);
        expandableListView = (ExpandableListView) findViewById(R.id.expand_listview);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        myVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        PD_urlObjects = new ArrayList<>();
        /* 其他變數設定 */
        setSupportActionBar(toolbar);
        online_check_add_btn.setTag(R.drawable.ic_add_black_24dp);
        /* 監聽器設定 */
        toolbar.setOnMenuItemClickListener(myListener);
        local_input_right_btn.setOnClickListener(myListener);
        local_url_input.addTextChangedListener(myListener);
        online_check_add_btn.setOnClickListener(myListener);
        local_check_send_btn.setOnClickListener(myListener);
        //TODO:摺疊清單監聽
        expandableListView.setOnGroupClickListener(myListener);
        expandableListView.setOnChildClickListener(myListener);
        expandableListView.setOnItemLongClickListener(myListener);
        /* 設定Dialog view */
        setAllView();
        /* 創立 url database(目前為空) */
        Create_url_database_file();
        /* 建立所有 dialog 和 toast */
        buildAllDialog();
        /* 讀資料庫 */
        loadDatabase();
    }

    public void setAllView() {
        dialog_online_check_add_entry_view = getLayoutInflater().inflate(R.layout.mydialog_online_check_add_entry, null);
        dialog_progress_view = getLayoutInflater().inflate(R.layout.mydialog_progress_view, null);
        dialog_online_check_result = getLayoutInflater().inflate(R.layout.mydialog_online_check_result, null);
        dialog_local_check_result = getLayoutInflater().inflate(R.layout.mydialog_local_check, null);
        //設定 view id
        dialog_online_check_add_entry_view.setId((int) R.layout.mydialog_online_check_add_entry);
        dialog_progress_view.setId((int) R.layout.mydialog_progress_view);
        dialog_online_check_result.setId((int) R.layout.mydialog_online_check_result);
        dialog_local_check_result.setId((int) R.layout.mydialog_local_check);
        //設定 dialog_online_check_add_entry_view 元件
        View view = dialog_online_check_add_entry_view;
        online_url_input = view.findViewById(R.id.online_check_input);
        online_input_right_btn = view.findViewById(R.id.online_check_input_right_btn);
        online_url_input.addTextChangedListener(myListener);
        view.findViewById(R.id.online_check_input_right_btn).setOnClickListener(myListener);
        view.findViewById(R.id.online_check_send_btn).setOnClickListener(myListener);
        view.findViewById(R.id.online_check_close_btn).setOnClickListener(myListener);
        view.setOnTouchListener(myListener);
        //設定dialog_progress_view 元件
        ImageView imageView = dialog_progress_view.findViewById(R.id.progress_dot_img);
        AnimationDrawable ani = (AnimationDrawable) imageView.getDrawable();
        ani.start();
        //設定 dialog_online_check_result元件
        View view1 = dialog_online_check_result;
        view1.findViewById(R.id.ipqs_yes_btn).setOnClickListener(myListener);
        view1.findViewById(R.id.ipqs_no_btn).setOnClickListener(myListener);
        View view2 = dialog_local_check_result;
        view2.findViewById(R.id.local_check_yes_btn).setOnClickListener(myListener);
        view2.findViewById(R.id.local_check_no_btn).setOnClickListener(myListener);

    }


    public void setButtomDialog(View view, boolean isTouchCanceled, String... addition) {
        BottomSheetBehavior buttomDialogbehavior;
        switch (view.getId()) {
            case R.layout.mydialog_online_check_add_entry:
                buttomDialog.setContentView(view);
                buttomDialog.setCanceledOnTouchOutside(isTouchCanceled);
                break;
            case R.layout.mydialog_progress_view:
                buttomDialog.setContentView(view);
                buttomDialogbehavior = BottomSheetBehavior.from((View) dialog_progress_view.getParent());
                buttomDialogbehavior.setHideable(false);
                buttomDialog.setCanceledOnTouchOutside(isTouchCanceled);
                break;
            case R.layout.mydialog_online_check_result:
                TextView ipqs_score = view.findViewById(R.id.ipqs_score);
                TextView ipqs_msg = view.findViewById(R.id.ipqs_msg);
                TextView ipqs_url = view.findViewById(R.id.ipqs_url);
                String score = addition[0];
                String msg = addition[1].trim();
                ipqs_score.setText(score);
                ipqs_msg.setText(msg);
                ipqs_url.setText(URL_text);
                int _score = Integer.valueOf(score);
                //設定文字顏色
                if (0 <= _score && _score <= 20)
                    ipqs_score.setTextColor(Color.parseColor("#457c0d"));
                else if (21 <= _score && _score <= 40)
                    ipqs_score.setTextColor(Color.parseColor("#78c430"));
                else if (41 <= _score && _score <= 60)
                    ipqs_score.setTextColor(Color.parseColor("#fec721"));
                else if (61 <= _score && _score <= 80)
                    ipqs_score.setTextColor(Color.parseColor("#f65922"));
                else ipqs_score.setTextColor(Color.parseColor("#d63839"));
                // bottomDialog完全展開
                buttomDialog.setContentView(dialog_online_check_result);
                buttomDialogbehavior = BottomSheetBehavior.from((View) dialog_online_check_result.getParent());
                buttomDialogbehavior.setState(BottomSheetBehavior.STATE_EXPANDED); //完全展開
                buttomDialogbehavior.setDraggable(false); //不能拖曳dialog
                buttomDialogbehavior.setHideable(false);  //無法隱藏
                buttomDialog.setCanceledOnTouchOutside(isTouchCanceled);
                break;
            case R.layout.mydialog_local_check:
                int safe_scale = Integer.valueOf(addition[0]);
                ImageView safe_scale_img = view.findViewById(R.id.safe_scale);
                TextView result_title = view.findViewById(R.id.local_check_title);
                TextView result_msg = view.findViewById(R.id.dialog_message_box);
                Button yes_btn = view.findViewById(R.id.local_check_yes_btn);
                Button no_btn = view.findViewById(R.id.local_check_no_btn);
                //設定輸出訊息 && 按鈕 tag
                String title = "此網址安全層級為<font color='%s'><u>%s星</u></font>";
                String _msg = "%s此網址不在資料庫中，請問是否要加入資料庫？";
                String hint = "低於三星網址建議登入後小心使用<br>";
                int[] drawableID = {0, R.drawable.mydrawble_star_scale1, R.drawable.mydrawble_star_scale2, R.drawable.mydrawble_star_scale3, R.drawable.mydrawble_star_scale4, R.drawable.mydrawble_star_scale5};
                String[] numText = {"", "一", "二", "三", "四", "五"};
                String[] textColor = {"", "#d63839", "#f65922", "#fec721", "#78c430", "#457c0d"};
                title = String.format(title, textColor[safe_scale], numText[safe_scale]);
                if (safe_scale == 1)
                    _msg = "此網址在資料庫中<font color='#d63839'><u>無任何匹配</u></font><br>是否進一步檢查此網址？";
                if (safe_scale == 2 || safe_scale == 3) _msg = String.format(_msg, hint);
                if (safe_scale == 4) _msg = String.format(_msg, "");
                if (safe_scale == 5) _msg = "此為<font color='#457c0d'><u>安全網址</u></font>，可以放心登入";
                //設定按鈕
                yes_btn.setTag(safe_scale == 1 ? "ipqs_search" : "add_url"); //若 scale==1則設定 tag為ipqs_search
                no_btn.setTag(safe_scale == 1 ? "ipqs_search" : "add_url");
                yes_btn.setVisibility(safe_scale == 5 ? View.GONE : View.VISIBLE);
                no_btn.setVisibility(safe_scale == 5 ? View.GONE : View.VISIBLE);
                //輸出到widget
                safe_scale_img.setImageDrawable(getDrawable(drawableID[safe_scale]));
                result_title.setText(Html.fromHtml(title));
                result_msg.setText(Html.fromHtml(_msg));
                buttomDialog.setContentView(dialog_local_check_result);
                buttomDialog.setCanceledOnTouchOutside(isTouchCanceled);
                break;

        }
    }


    //刷新database UI
    public void refreshUI(boolean... addition) {
        boolean isDelClick = false;
        if (addition.length != 0) isDelClick = addition[0]; //isDelClick:true
        myAdapter = new MyBaseExpandableListAdapter(url_database_list, this, myListener, isDelClick);
        expandableListView.setAdapter(myAdapter);
    }

    // 設定 toolbar&& 搜尋功能
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_url_check, menu);
        MenuItem menuSearchItem = menu.findItem(R.id.search_btn);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menuSearchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(myListener);
        return true;
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
                if (buttomDialog.isShowing()) {
                    online_url_input.setText(URL_text);
                } else {
                    local_url_input.setText(URL_text);
                }

                break;
            default:
                return;  //resultCode為 0 時，return回原本activity
        }
    }

    /* 設定所有dialog */
    public void buildAllDialog() {
        //toast宣告
        dialog_toast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        //TODO:Snakerbar宣告
        snackbar = Snackbar.make(this.findViewById(R.id.activity_url_check), "", Snackbar.LENGTH_SHORT)
                .setAction("已複製", myListener)
                .setActionTextColor(Color.parseColor("#FF60AF"));
        //底部dialog
        buttomDialog = new BottomSheetDialog(this);
        buttomDialog.setContentView(dialog_online_check_add_entry_view);
        buttomDialog.setCanceledOnTouchOutside(true);

    }

    //TODO:設定snackbar
    public void setSnackbar(String text, String action, int duration) {
        snackbar.setText(text);
        snackbar.setAction(action, myListener);
        snackbar.setDuration(duration);

    }

    //讀取資料庫進入 url_database_list
    public void loadDatabase() throws Exception {
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //解析 url_database檔案
        org.w3c.dom.Document doc = db.parse(url_database);
        NodeList nodeList = doc.getElementsByTagName("token");

        url_database_list = new HashMap<>();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node tokenNode = nodeList.item(i);
            String groupID = tokenNode.getAttributes().getNamedItem("id").getNodeValue();
            NodeList childNodes = tokenNode.getChildNodes();
            ArrayList<Struct.urlObject> groupList = new ArrayList<>();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node node = childNodes.item(j);
                if (!node.getNodeName().matches("mainURL|subURL")) continue;
                Struct.urlObject urlObject = new Struct.urlObject();
                urlObject.tagName = node.getNodeName();
                urlObject.uuid = node.getAttributes().getNamedItem("uuid").getNodeValue();
                urlObject.text = node.getTextContent();
                if (node.getNodeName().equals("subURL"))
                    urlObject.format = node.getAttributes().getNamedItem("format").getNodeValue();


                groupList.add(urlObject);
            }

            url_database_list.put(Integer.valueOf(groupID), groupList);

        }
        System.out.println("讀取資料庫...完畢");
        myAdapter = new MyBaseExpandableListAdapter(url_database_list, this, myListener, false);
        expandableListView.setAdapter(myAdapter);
        System.out.println("創建UI清單...完畢");
    }


    // 設定安全網址 - mainURL加入網址到資料庫中
    public void addMainURL(String url) throws Exception {
        Boolean isExist = false;
        int groupID = 0;
        System.out.println("要加入mainURL的資料: " + url);
        //先檢查有無重複網址
        for (int i = 0; i < url_database_list.size(); i++) {
            ArrayList<Struct.urlObject> urlObjects = url_database_list.get(i);
            String mainURL = urlObjects.get(0).text;
            if (mainURL.equals(url)) {
                isExist = true;
                System.out.println(url + " 已存在於資料庫中");
                break;
            }
        }
        myListener.pass_params(isExist);
        //若此網址從未添加過才寫入xml檔
        if (!isExist) {
            //創建 url Object加入到 list中
            Struct.urlObject urlObject = new Struct.urlObject();
            //設定 urlObject
            urlObject.text = url;
            urlObject.tagName = "mainURL";
            urlObject.uuid = Long.toHexString(System.currentTimeMillis());
            ArrayList<Struct.urlObject> urlObjects = new ArrayList<>();
            urlObjects.add(urlObject);
            //檢查 groupID有無沒被用到的,有的話就先放, 沒有則放入hashmap最後
            for (int i = 0; i < url_database_list.size() + 1; i++) {
                if (!url_database_list.containsKey(i)) {
                    url_database_list.put(i, urlObjects);
                    groupID = i;
                    break;
                }
            }
            System.out.println(url + " 成功新增mainURL");
//            write_url_database(); //寫入xml檔案
        } else {
            dialog_toast.setText("此網址已存在於資料庫中");
            dialog_toast.show();
        }
        refreshUI(); //刷新 UI


    }

    //寫入xml檔案
    public void writeXml(org.w3c.dom.Document doc) throws Exception {
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

    //清空doc重新寫入, 因可能有刪除id會改變的情況
    public void write_url_database() throws Exception {
        //建立一個 Document類
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(url_database); //解析 url_database檔案
        org.w3c.dom.Element root = doc.getDocumentElement(); //得到根節點
        NodeList childNodes = root.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
            root.removeChild(childNodes.item(i)); //移除所有子node

        for (int i = 0; i < url_database_list.size(); i++) {
            ArrayList<Struct.urlObject> urlObjects = url_database_list.get(i);
            String groupID = String.valueOf(i);
            //每一個 entry都一定有一個 token
            org.w3c.dom.Element token = doc.createElement("token");
            for (int j = 0; j < urlObjects.size(); j++) {
                Struct.urlObject urlObject = urlObjects.get(j);
                switch (urlObject.tagName) {
                    case "mainURL":
                        org.w3c.dom.Element mainURL = doc.createElement("mainURL");
                        // 設定 attribute
                        mainURL.setTextContent(urlObject.text);
                        token.setAttribute("id", groupID);
                        token.setIdAttribute("id", true);
                        mainURL.setAttribute("groupID", groupID);
                        mainURL.setAttribute("uuid", urlObject.uuid);
                        mainURL.setIdAttribute("uuid", true);
                        //增加
                        token.appendChild(mainURL);
                        break;
                    case "subURL":
                        org.w3c.dom.Element subURL = doc.createElement("subURL");
                        subURL.setTextContent(urlObject.text);
                        subURL.setAttribute("format", urlObject.format);
                        subURL.setAttribute("groupID", groupID);
                        subURL.setAttribute("uuid", urlObject.uuid);
                        subURL.setIdAttribute("uuid", true);
                        // 新增 subURL節點
                        token.appendChild(subURL);

                        break;
                }
            }
            root.appendChild(token);
        }
        writeXml(doc);
        System.out.println("資料庫寫入完畢!");
    }

    // 刪除 mainURL網址
    public void deleteMainURL(String id) throws Exception {

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
    public void matchDatabase(String url) throws Exception {
        System.out.println(url + " 進入網址資料庫進行比對...");
        // 外部變數紀錄
        String format = null;
        int groupID = 0;
        for (int i = 0; i < url_database_list.size(); i++) {
            Struct.urlObject urlObject = url_database_list.get(i).get(0);
            String mainURL = urlObject.text;
            groupID = i;
            format = getURLMatchFormat(url, mainURL);
            if (format != null) break;
        }
        System.out.println("mainURL配對完畢...比對結果為: " + format);
        // mainURL全無匹配
        if (format == null) {
            setButtomDialog(dialog_local_check_result, false, "1");
            Dialogs.showSecureDialog(buttomDialog);
        } else {  /* 有匹配到 mainURL */
            myListener.pass_params(groupID, url, format);
            /* 比對 subURL */
            if (format.equals("exact") || matchSubURL(groupID, url, format)) { /*若 mainURL為 exact或 subURL配對成功*/
                setButtomDialog(dialog_local_check_result, true, "5");
                Dialogs.showSecureDialog(buttomDialog);
            } else { /*配對失敗*/
                // 比對級數配對
                switch (format) {
                    case "startwith":
                        setButtomDialog(dialog_local_check_result, false, "4");
                        break;
                    case "host":
                        setButtomDialog(dialog_local_check_result, false, "3");
                        break;
                    case "basedomain":
                        setButtomDialog(dialog_local_check_result, false, "2");
                        break;
                }
                Dialogs.showSecureDialog(buttomDialog);
            }

        }


    }

    //四種比對模式
    public String getURLMatchFormat(String url, String mainURL) throws Exception {
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
    public void addsubURL(final int groupID, String url, final String format) throws Exception {
        System.out.println(String.format("準備將 %s[格式%s]的網址加入群組%d中...", url, format, groupID));
        Struct.urlObject urlObject = new Struct.urlObject();
        urlObject.text = url;
        urlObject.format = format;
        urlObject.tagName = "subURL";
        urlObject.uuid = Long.toHexString(System.currentTimeMillis());
        ArrayList<Struct.urlObject> urlObjects = url_database_list.get(groupID); //加入清單中
        urlObjects.add(urlObject);
        url_database_list.put(groupID, urlObjects); //hashMap鍵更新鍵值
        System.out.println(String.format("%s 已成功加入資料庫中!", url));
//        write_url_database();
        refreshUI();//刷新 UI
    }

    //比對 subURL有無 exact
    public boolean matchSubURL(int groupID, String url, String format) {
        System.out.println("準備在群組" + groupID + "中搜尋...");
        Boolean isMatch = false;
        ArrayList<Struct.urlObject> urlObjects = url_database_list.get(groupID);
        for (int i = 0; i < urlObjects.size(); i++) {
            Struct.urlObject urlObject = urlObjects.get(i);
            if (urlObject.format.equals(format)) {
                String subURL = urlObject.text;
                if (subURL.equals(url)) {
                    isMatch = true;
                    System.out.println("\n檢查subURL有無存在網址...檢查完畢");
                    System.out.println("結果為..." + isMatch);
                    return isMatch;
                }
            }
        }
        System.out.println("\n檢查subURL有無存在網址...檢查完畢");
        System.out.println("結果為..." + isMatch);
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
            setButtomDialog(dialog_progress_view, false); //顯示處理 dialog
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
                    buttomDialog.dismiss();
                    setButtomDialog(dialog_online_check_result, false, risk_score, message);
                    buttomDialog.show();
                }
            });
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());

        }


    }

    /* 建立 url database的檔案 */
    public void Create_url_database_file() throws Exception {
        /* Create file */
        File dir = getApplicationContext().getFilesDir();
        url_database = new File(dir, "url_database.xml");
        url_database.setWritable(true);  // 設為可讀寫
        url_database.setReadable(true);
        if (url_database.createNewFile()) {
            //建立一個 Document類
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            //建立一個根節點，並且將根節點新增到Document物件中去
            org.w3c.dom.Document doc = db.newDocument();
            org.w3c.dom.Element root = doc.createElement("root");
            doc.appendChild(root);

            writeXml(doc);
            System.out.println("成功創建url_database 檔案");
        } else {
            System.out.println("url_database檔案已存在");
        }


    }

    //實作各種監聽器
    class MyListener implements View.OnClickListener, TextWatcher, ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener, Toolbar.OnMenuItemClickListener, SearchView.OnQueryTextListener, View.OnTouchListener, AdapterView.OnItemLongClickListener {

        private int groupID;
        private String url, format;
        private boolean isExist; //addMainURL使用
        private boolean isLongClick = false;

        //pass變數用
        public void pass_params(Object... objects) {
            if (objects.length == 1) {
                isExist = objects[0].getClass() == Boolean.class ? (boolean) objects[0] : null;
            }
            if (objects.length == 3) {
                groupID = objects[0].getClass() == Integer.class ? (int) objects[0] : null;
                url = objects[1].getClass() == String.class ? (String) objects[1] : null;
                format = objects[2].getClass() == String.class ? (String) objects[2] : null;
            }
        }

        // input框監聽:打字事件
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (buttomDialog.isShowing()) {
                ImageButton close_btn = dialog_online_check_add_entry_view.findViewById(R.id.online_check_close_btn);
                if (count == 0) {
                    buttomDialog.setCanceledOnTouchOutside(true);
                    close_btn.setVisibility(View.GONE);
                    online_input_right_btn.setImageDrawable(getDrawable(R.drawable.ic_qrcode_scan));
                    online_input_right_btn.setTag("ic_qrcode_scan");
                } else {
                    buttomDialog.setCanceledOnTouchOutside(false);
                    close_btn.setVisibility(View.VISIBLE);
                    online_input_right_btn.setImageDrawable(getDrawable(R.drawable.ic_clear_button));
                    online_input_right_btn.setTag("ic_clear_button");
                }
            } else {
                if (count == 0) {
                    local_input_right_btn.setImageDrawable(getDrawable(R.drawable.ic_qrcode_scan));
                    local_input_right_btn.setTag("ic_qrcode_scan");
                } else {
                    local_input_right_btn.setImageDrawable(getDrawable(R.drawable.ic_clear_button));
                    local_input_right_btn.setTag("ic_clear_button");
                }
            }

        }

        @Override
        public void afterTextChanged(Editable s) {

        }

        // Button事件監聽
        @Override
        public void onClick(View v) {
            Intent scan_qrcode_activity = new Intent(getApplicationContext(), UrlCheckActivity_ScanQrcodeActivity.class);
            String iconTag;
            switch (v.getId()) {
                case R.id.parent_item_icon: /*展開收合清單*/
                    int groupPosition = (int) v.getTag();
                    //還未長按時可以收合清單
                    //長按後不可收合清單
                    if (!isLongClick) {
                        if (!expandableListView.isGroupExpanded(groupPosition)) {
                            expandableListView.expandGroup(groupPosition);
                        } else {
                            //TODO:->groupPosition而非全部!!收合清單時，將textView也收合(設為singleLine)
                            for (int i = 0; i < expandableListView.getChildCount(); i++) {
                                View view = expandableListView.getChildAt(i);
                                TextView textView = view.findViewById(R.id.tv_group_child);
                                if (textView == null)
                                    textView = view.findViewById(R.id.tv_group_parent);
                                if (textView != null) textView.setSingleLine(true);
                            }
                            expandableListView.collapseGroup(groupPosition);
                        }
                    } else { //長按後不可收合清單
                        ImageButton imgBtn = (ImageButton) v;
                        imgBtn.setImageDrawable(getDrawable(R.drawable.down_arrow));
                    }
                    break;
                case R.id.online_check_close_btn: /*關閉buttomDialog(X)*/
                    buttomDialog.dismiss();
                    break;
                case R.id.local_check_yes_btn:
                    if (v.getTag().equals("add_url")) {
                        try {
                            buttomDialog.dismiss();
                            addsubURL(groupID, url, format);
                            dialog_toast.setText("已添加此網址至資料庫中");
                            dialog_toast.show();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    } else { //ipqs_search
                        IPQSCheck();
                    }
                    break;
                case R.id.local_check_no_btn:
                    if (v.getTag().equals("add_url")) {
                        dialog_toast.setText("已取消添加此網址至資料庫中");
                        dialog_toast.show();
                    } else { //ipqs_search
                        dialog_toast.setText("取消進一步檢查此網址");
                        dialog_toast.show();
                    }
                    buttomDialog.dismiss();
                    break;
                case R.id.ipqs_yes_btn:
                    try {
                        buttomDialog.dismiss();
                        addMainURL(URL_text);
                        if (!isExist) {
                            dialog_toast.setText("已添加此網址至資料庫中");
                            dialog_toast.show();
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    break;
                case R.id.ipqs_no_btn:
                    buttomDialog.dismiss();
                    dialog_toast.setText("取消添加此網址到資料庫中");
                    dialog_toast.show();
                    break;
                case R.id.online_check_input_right_btn:
                    iconTag = (String) online_input_right_btn.getTag();
                    if (iconTag.equals("ic_clear_button")) {
                        online_url_input.setText("");
                    } else {
                        startActivityForResult(scan_qrcode_activity, Scan_QR_CODE);
                    }
                    break;
                case R.id.online_check_send_btn:
                    System.out.println("按下送出按鈕");
                    URL_text = online_url_input.getText().toString().trim();
                    imm.hideSoftInputFromWindow(buttomDialog.getWindow().getDecorView().getWindowToken(), 0);
                    IPQSCheck();
                    break;
                case R.id.online_check_add_btn:
                    int iconID = (int) online_check_add_btn.getTag();
                    switch (iconID) {
                        case R.drawable.ic_add_black_24dp:
                            setButtomDialog(dialog_online_check_add_entry_view, true);
                            Dialogs.showSecureDialog(buttomDialog);
                            online_url_input.setText("");
                            break;
                        case R.drawable.mydrawble_arrow_back:
                            //TODO:返回按紐[待刪除]
                            break;
                        case R.drawable.ic_delete_black_24dp:
                            //TODO:刪除按鈕
                            break;
                    }
                    break;
                case R.id.local_input_right_btn:
                    iconTag = (String) local_input_right_btn.getTag();
                    if (iconTag.equals("ic_clear_button")) { // clear icon 動作
                        local_url_input.setText("");
                    } else { // qrcode_scan_icon 動作
                        startActivityForResult(scan_qrcode_activity, Scan_QR_CODE);
                    }
                    break;
                case R.id.local_check_send_btn:
                    System.out.println("按下url_check，進行網址資料庫比對");
                    /* 按下url_check就隱藏鍵盤 */
                    imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(local_check_send_btn.getWindowToken(), 0);
                    URL_text = local_url_input.getText().toString().trim();
                    try {
                        matchDatabase(URL_text);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }


        //TODO:Menu按鈕監聽
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            System.out.println(item.getItemId());
            switch (item.getItemId()) {
                case R.id.action_intro_Url_Check:
                    break;
                case R.id.action_delete:
                    //TODO:delete鍵按下
                    refreshUI(true); //isLongClick:true
                    break;
                case R.id.import_export_btn:
                    break;
            }
            return true;
        }

        //TODO:搜尋資料庫事件監聽
        @Override
        public boolean onQueryTextSubmit(String query) {
            //按下搜尋後string
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            //只要有文字變動就會有的string
            return true;
        }


        // 鍵盤點擊 dialog空白處收起事件
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (online_url_input.isFocused())
                imm.hideSoftInputFromWindow(buttomDialog.getWindow().getDecorView().getWindowToken(), 0);
            return true;
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
            TextView parent_item = v.findViewById(R.id.tv_group_parent);
            if (this.isLongClick) {
                //parent item設定
                parent_item.setSingleLine(false);
                parent_item.setSelected(!parent_item.isSelected()); //state_select轉換
                toggleStrike(parent_item);
                //child item設定
                int child_position = expandableListView.getPositionForView(v) + 1;
                for (int i = 0; i < myAdapter.getChildrenCount(groupPosition); i++) {
                    View view = expandableListView.getChildAt(child_position);
                    TextView child_item = view.findViewById(R.id.tv_group_child);
                    child_item.setSingleLine(false);
                    child_item.setSelected(parent_item.isSelected());
                    toggleStrike(child_item);
                    child_position++;
                }
                //加入待刪除清單
                ArrayList<Struct.urlObject> urlObjects = (ArrayList<Struct.urlObject>) parent_item.getTag();
                for (Struct.urlObject item : urlObjects) {
                    //若parent item選中
                    if (parent_item.isSelected() && !PD_urlObjects.contains(item))
                        PD_urlObjects.add(item);
                    else PD_urlObjects.remove(item);  //若parent item沒選中
                }
                setSnackbar("請選擇要刪除的項目", "已選擇" + PD_urlObjects.size(), Snackbar.LENGTH_INDEFINITE);

            } else {
                //輕觸展開並複製
                parent_item.setSingleLine(false);
                String text = parent_item.getText().toString();
                String pre_text = cmb.getPrimaryClip().getItemAt(0).getText().toString();
                //不重新複製
                if (!text.equals(pre_text))
                    cmb.setPrimaryClip(ClipData.newPlainText(null, text)); //刪除線轉換
                setSnackbar(text, "已複製", Snackbar.LENGTH_SHORT);
                snackbar.show();
            }

            return true;
        }

        //TODO:輕點即可展開複製
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            TextView child_item = v.findViewById(R.id.tv_group_child);
            if (isLongClick) { //長按事件
                child_item.setSingleLine(false);
                child_item.setSelected(!child_item.isSelected());//state_select轉換
                toggleStrike(child_item);
                //添加移除toggle
                if (child_item.isSelected() && !PD_urlObjects.contains(child_item))
                    PD_urlObjects.add((Struct.urlObject) child_item.getTag());
                else PD_urlObjects.remove(child_item.getTag());
                setSnackbar("請選擇要刪除的項目", "已選擇" + PD_urlObjects.size(), Snackbar.LENGTH_INDEFINITE);
            } else { //輕觸複製事件
                //輕觸展開並複製
                child_item.setSingleLine(false);
                String text = child_item.getText().toString();
                String pre_text = cmb.getPrimaryClip().getItemAt(0).getText().toString();
                //不重新複製
                if (!text.equals(pre_text)) cmb.setPrimaryClip(ClipData.newPlainText(null, text));
                setSnackbar(text, "已複製", Snackbar.LENGTH_SHORT);
                snackbar.show();

            }
            return true;
        }

        //TODO:長按事件
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
            //長按事件觸發一次
            if (!this.isLongClick) {
                this.isLongClick = true;
                myVibrator.vibrate(500);
                //長按展開所有清單
                for (int i = 0; i < myAdapter.getGroupCount(); i++) {
                    expandableListView.expandGroup(i);
                }
                //floating button 設定
                online_check_add_btn.setImageDrawable(getDrawable(R.drawable.ic_delete_black_24dp));
                online_check_add_btn.setTag(R.drawable.ic_delete_black_24dp);
                //snackbar 設定
                setSnackbar("請選擇要刪除的項目", "已選擇" + PD_urlObjects.size(), Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
            }

            return true;
        }

        public void toggleStrike(TextView item) {
            int strike_line_show = (item.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            int strike_line_hide = (item.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            if (item.isSelected())
                item.setPaintFlags(strike_line_show);
            else item.setPaintFlags(strike_line_hide);
        }


    }

}





