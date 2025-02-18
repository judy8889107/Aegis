/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;


import android.Manifest;
import android.app.Dialog;

import android.app.SearchManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import android.content.Intent;


import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;


import android.os.Vibrator;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.Html;

import android.text.TextWatcher;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Pair;

import android.view.KeyEvent;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;


import com.beemdevelopment.aegis.R;


import com.beemdevelopment.aegis.ui.dialogs.Dialogs;


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
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;


import java.io.*;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
/* 輸入流 */
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/* JSON */

import org.apache.commons.validator.routines.UrlValidator;
import org.bouncycastle.jcajce.provider.symmetric.ChaCha;
import org.json.JSONException;
import org.json.JSONObject;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import kotlin.Triple;

class Struct {
    public static class urlObject {
        public String tagName, uuid, text;
        int format = 0;
        int safe_score = 0;
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
    Dialog del_dialog;
    Dialog EXP_IMP_dialog;
    View dialog_online_check_add_entry_view;
    View dialog_progress_view;
    View dialog_online_check_result;
    View dialog_local_check_result;
    View dialog_delete_dialog;
    View dialog_export_file;
    View dialog_import_file;
    private static final int Scan_QR_CODE = 2;
    private static final String pass_name = "URL_text"; /* 傳遞資料的string名，新增變數避免寫死 */
    private Toast dialog_toast;
    private Snackbar snackbar;
    private String api_key = null; /* SafetyNet與 Google Play建立連線用的 API KEY */
    String URL_text = null; /* local_url_input和qr_code_scan共用的變數，避免判斷時有衝突，判斷完畢後設為null */
    File url_database;
    private ArrayList<ArrayList<Struct.urlObject>> url_database_list;
    private ArrayList<Struct.urlObject> PD_urlObjects;
    private ExpandableListView expandableListView;
    private MyBaseExpandableListAdapter myAdapter;
    private ClipboardManager cmb;
    private Vibrator myVibrator;
    private long firstPressedTime;

    /* Code代碼 */
    final int CODE_SCAN = 0;
    final int unmatched = 1;
    final int basedomain = 2;
    final int host = 3;
    final int startwith = 4;
    final int exact = 5;
    private boolean isPermissionPassed = false;
    final int ACTION_CREATE_DOCUMENT = 11;
    final int ACTION_GET_CONTENT = 12;
    final String IPQS_API_Key = "https://ipqualityscore.com/api/json/url/mMdf76Tro3JGHcC3Cmv9WPGu14C56Rpm/";


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

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }


    }


    // 獲取新intent值
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.v("share", "新intent");
        setIntent(intent);
    }

    // intent字串取得貼上Input框
    public void shareAction() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                local_url_input.setText(sharedText);
            }
        }
    }

    //應用後臺執行 返回繼續
    @Override
    protected void onResume() {
        super.onResume();
        shareAction();
        refreshUI(); //修復bug(App後臺執行回到前台刷新list)
    }


    //捕捉返回鍵, 寫入到外部記憶體後離開
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (myListener.isLongClick) { //長按返回事件
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
                    for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                        Collections.sort(urlObjects.subList(1, urlObjects.size()), Collections.reverseOrder(myListener.sort_sub_old_to_new));
                    Collections.sort(url_database_list, Collections.reverseOrder(myListener.sort_main_old_to_new));
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
        //摺疊清單監聽
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
        dialog_delete_dialog = getLayoutInflater().inflate(R.layout.mydialog_delete_dialog, null);
        dialog_export_file = getLayoutInflater().inflate(R.layout.mydialog_export_file, null);
        dialog_import_file = getLayoutInflater().inflate(R.layout.mydialog_import_file, null);
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
        // 設定 delete dialog
        View view3 = dialog_delete_dialog;
        view3.findViewById(R.id.delete_no_btn).setOnClickListener(myListener);
        view3.findViewById(R.id.delete_yes_btn).setOnClickListener(myListener);
        //TODO:設定dialog export file
        View view4 = dialog_export_file;
        view4.findViewById(R.id.export_eye_btn).setOnClickListener(myListener);
        view4.findViewById(R.id.export_confirm_btn).setOnClickListener(myListener);
        //TODO:設定dialog export file
        View view5 = dialog_import_file;
        view5.findViewById(R.id.import_eye_btn).setOnClickListener(myListener);
        view5.findViewById(R.id.import_confirm_btn).setOnClickListener(myListener);

    }


    public void setButtomDialog(View view, boolean isTouchCanceled, String... addition) {
        BottomSheetBehavior buttomDialogbehavior;
        System.out.println((int) view.getId());
        System.out.println(view.getId() == (int) R.layout.mydialog_local_check);
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
    public void refreshUI(String... params) {
        String search_str = null;
        if (params.length != 0)
            search_str = params[0];
        myAdapter = new MyBaseExpandableListAdapter(url_database_list, this, myListener, search_str);
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /* resultcode 正常無動作(不掃描QRcode)返回時，是0 */
        Log.v("mydebug", "requestCode: " + requestCode);
        Log.v("mydebug", "resultCode: " + resultCode);
        Log.v("mydebug", "resultCodeOK: " + RESULT_OK);
        if (resultCode == Scan_QR_CODE) {
            URL_text = data.getStringExtra(pass_name);
            /* 將網址輸入input text改為QRcode掃出的內容 */
            if (buttomDialog.isShowing()) {
                online_url_input.setText(URL_text);
            } else {
                local_url_input.setText(URL_text);
            }
        }
        if (requestCode == ACTION_CREATE_DOCUMENT) {
            try {
                Uri fileUri = data.getData();
                String passwd = ((EditText) dialog_export_file.findViewById(R.id.export_passwd)).getText().toString();
                exportFile(fileUri, passwd);
                setSnackbar("輸出資料庫成功!", "SUCCESS", Snackbar.LENGTH_SHORT);
                snackbar.show();
            } catch (Exception e) {
                Log.v("mydebug", "" + e);
            }
        }
        if (requestCode == ACTION_GET_CONTENT) {
            Uri fileUri = data.getData();
            myListener.pass_params(fileUri); /* pass變數 */
            EXP_IMP_dialog.setContentView(dialog_import_file);
            EXP_IMP_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            EXP_IMP_dialog.show(); /*顯示輸入密碼 dialog*/

        }

        return;
    }

    /*AES加密(MD5 key)*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String Encrypt(String content, String password) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] MD5 = messageDigest.digest(password.getBytes());

        SecretKeySpec secretKeySpec = new SecretKeySpec(MD5, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        byte[] byteContent = content.getBytes("utf-8");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] result = cipher.doFinal(byteContent);
        String result_str = Base64.getEncoder().encodeToString(result);
        return result_str;
    }

    /*AES解密(MD5 key)*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String Decrypt(String content, String password) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] MD5 = messageDigest.digest(password.getBytes());

        byte[] decodedContent = Base64.getDecoder().decode(content);
        SecretKeySpec secretKeySpec = new SecretKeySpec(MD5, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        try {
            byte[] result = cipher.doFinal(decodedContent);
            return new String(result);
        } catch (Exception e) {
            Log.v("mydebug", "例外" + e);
            Log.v("mydebug", "匯入資料庫失敗");
            return null;
        }
    }

    //TODO:輸出 url_database
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void exportFile(Uri fileUri, String password) throws Exception {
        if (!isPermissionPassed) {
            setSnackbar("尚未取得存取權限", "失敗", Snackbar.LENGTH_LONG);
            getPermission();
        } else {
            OutputStream os = getContentResolver().openOutputStream(fileUri); /*檔案輸出流*/
            InputStream is = new FileInputStream(url_database);
            String str = getData(is, true);
            Log.v("mydebug", "匯出的資料庫內容:" + str);
            String encrypted_str = Encrypt(str, password); /*加密檔案*/
            byte[] strTobyte = encrypted_str.getBytes();
            os.write(strTobyte); /*寫入檔案*/
            os.close();
            is.close();

        }
    }

    //TODO:輸出 url_database
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void importFile(Uri fileUri, String password) throws Exception {
        if (!isPermissionPassed) {
            setSnackbar("尚未取得存取權限", "失敗", Snackbar.LENGTH_LONG);
            getPermission();
        } else {
            InputStream is = getContentResolver().openInputStream(fileUri); /*檔案輸入*/
            String str = getData(is, false);
            String descrypted_str = Decrypt(str, password); /*解密檔案*/
            if (descrypted_str != null) { /*返回不為空值*/
                OutputStream os = new FileOutputStream(url_database); /*檔案輸出目標(覆蓋原有檔案)*/
                byte[] strTobyte = descrypted_str.getBytes();
                os.write(strTobyte); /*寫入檔案*/
                os.close();
                setSnackbar("成功匯入資料庫", "SUCCESS", Snackbar.LENGTH_LONG);
            } else {
                setSnackbar("匯入資料庫失敗", "錯誤", Snackbar.LENGTH_LONG);
            }
            is.close();
            loadDatabase();
            refreshUI();
            snackbar.show();
        }

    }

    /* 設定所有dialog */
    public void buildAllDialog() {
        //del_dialog 宣告
        del_dialog = new Dialog(this);
        del_dialog.setContentView(dialog_delete_dialog);
        del_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //EXP_IMP_dialog宣告
        EXP_IMP_dialog = new Dialog(this);


        //toast宣告
        dialog_toast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        //Snakerbar宣告
        snackbar = Snackbar.make(this.findViewById(R.id.activity_url_check), "", Snackbar.LENGTH_SHORT)
                .setAction("已複製", myListener)
                .setActionTextColor(Color.parseColor("#FF60AF"));
        //底部dialog
        buttomDialog = new BottomSheetDialog(this);
        buttomDialog.setContentView(dialog_online_check_add_entry_view);
        buttomDialog.setCanceledOnTouchOutside(true);

    }

    //設定snackbar
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

        url_database_list = new ArrayList<>();

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
                if (node.getNodeName().equals("mainURL"))
                    urlObject.safe_score = Integer.valueOf(node.getAttributes().getNamedItem("safe_score").getNodeValue());
                if (node.getNodeName().equals("subURL"))
                    urlObject.format = Integer.valueOf(node.getAttributes().getNamedItem("format").getNodeValue());

                groupList.add(urlObject);
            }

            url_database_list.add(Integer.valueOf(groupID), groupList);

        }
        System.out.println("讀取資料庫...完畢");

        myAdapter = new MyBaseExpandableListAdapter(url_database_list, this, myListener);
        expandableListView.setAdapter(myAdapter);
        System.out.println("創建UI清單...完畢");
    }


    /* 設定安全網址 - mainURL加入網址到資料庫中 */
    public void addMainURL(String url, int safe_score) throws Exception {
        Boolean isExist = false;
        System.out.println("要加入mainURL的資料: " + url);
        // 先檢查有無重複網址

        for (int i = 0; i < url_database_list.size(); i++) {
            ArrayList<Struct.urlObject> urlObjects = url_database_list.get(i);
            String mainURL = urlObjects.get(0).text;
            if (mainURL.equals(url)) {
                isExist = true;
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
            urlObject.safe_score = safe_score; //安全分數
            ArrayList<Struct.urlObject> urlObjects = new ArrayList<>();
            urlObjects.add(urlObject);

            //往最前面插入元素
            url_database_list.add(0, urlObjects);

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
                        mainURL.setAttribute("safe_score", String.valueOf(urlObject.safe_score));
                        mainURL.setAttribute("uuid", urlObject.uuid);
                        mainURL.setIdAttribute("uuid", true);
                        //增加
                        token.appendChild(mainURL);
                        break;
                    case "subURL":
                        org.w3c.dom.Element subURL = doc.createElement("subURL");
                        subURL.setTextContent(urlObject.text);
                        subURL.setAttribute("format", String.valueOf(urlObject.format));
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


    //解析並比對資料庫 - 檢查網址
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void matchDatabase(String url) throws Exception {
        // 外部變數紀錄
        int format = unmatched;
        int max_main_relative = unmatched;
        int max_sub_relative = unmatched;
        int groupID = url_database_list.size();
        //配對第一層如果exact就返回,否則要記錄其他groupPosition和format, 以便進行第二層匹配
        ArrayList<Pair<Integer, Integer>> record = new ArrayList<>();
        for (int i = 0; i < url_database_list.size(); i++) {
            Struct.urlObject urlObject = url_database_list.get(i).get(0);
            String mainURL = urlObject.text;
            format = getURLMatchFormat(url, mainURL);
            Log.v("mydebug [matchDatabase]", String.format("%s比對結果為...%d", mainURL, format));
            if (format == 5) break;
            if (1 < format && format < 5) {
                record.add(new Pair<>(i, format)); //紀錄相關的mainURL位置和format
            }
        }
        if (format != 5) {// 不為exact, 則進行第二層級比對
            Log.v("mydebug [matchDatabase]", "record: " + record.toString() + "...進入二級比對");
            for (int i = 0; i < record.size(); i++) {
                int r_groupID = record.get(i).first;
                int r_format = record.get(i).second;
                String mainURL = url_database_list.get(r_groupID).get(0).text;
                //Triple:groupID, mainRelative, subRelative
                Triple<Integer, Integer, Integer> matchTriple = matchSubURL(r_groupID, url, r_format);
                if (matchTriple.getThird() == exact) {
                    format = 5;
                    break;
                }
                Log.v("mydebug [matchDatabase]", String.format("%s的主關聯性%d...群組%d:子關聯性%d", mainURL, matchTriple.getSecond(), matchTriple.getFirst(), matchTriple.getThird()));
                if (matchTriple.getSecond() >= max_main_relative) {
                    max_main_relative = matchTriple.getSecond();
                    format = max_main_relative;
                    if (matchTriple.getThird() > max_sub_relative) {
                        max_sub_relative = matchTriple.getThird();
                        groupID = matchTriple.getFirst();
                    }
                }
            }
        }
        Log.v("mydebug [matchDatabase]", String.format("最終配對結果,群組%d:格式%d:主相關性%d:子相關性%d", groupID, format, max_main_relative, max_sub_relative));
        // mainURL全無匹配
        if (format == unmatched) {
            setButtomDialog(dialog_local_check_result, false, "1");
            Dialogs.showSecureDialog(buttomDialog);
        } else {  /* 有匹配到 mainURL */
            myListener.pass_params(groupID, url, format);
            /* 比對 subURL */
            if (format == exact) { /*若 mainURL為 exact或 subURL配對成功*/
                setButtomDialog(dialog_local_check_result, true, "5");
                Dialogs.showSecureDialog(buttomDialog);
            } else { /*配對失敗*/
                // 比對級數配對
                switch (format) {
                    case startwith:
                        setButtomDialog(dialog_local_check_result, false, "4");
                        break;
                    case host:
                        setButtomDialog(dialog_local_check_result, false, "3");
                        break;
                    case basedomain:
                        setButtomDialog(dialog_local_check_result, false, "2");
                        break;
                }
                Dialogs.showSecureDialog(buttomDialog);
            }

        }


    }

    // 四種比對模式
    public int getURLMatchFormat(String url, String mainURL) throws Exception {
        Log.v("getURLMatchFormat", String.format("主URL: %s", url));
        Log.v("getURLMatchFormat", String.format("Other-URL: %s", mainURL));
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
        Log.v("getURLMatchFormat", String.format("Domain:[%s] [%s]", maj_basedomain, tmp_basedomain));
        Log.v("getURLMatchFormat", String.format("Host:[%s] [%s]", maj_host, tmp_host));
        Log.v("getURLMatchFormat", String.format("Port:[%s] [%s]", maj_port, tmp_port));
        Log.v("getURLMatchFormat", String.format("Path:[%s] [%s]", maj_path, tmp_path));
        if (url.equals(mainURL)) return exact;
        if (maj_startwith.contains(tmp_startwith)) return startwith;
        if (maj_hoststr.contains(tmp_hoststr)) return host;
        if (maj_basedomain.contains(tmp_basedomain)) return basedomain;
        return unmatched;
    }

    // 紀錄 subURL 到 database
    public void addsubURL(final int groupID, String url, final int format) throws Exception {
        System.out.println(String.format("準備將 %s[格式%s]的網址加入群組%d中...", url, format, groupID));
        Struct.urlObject urlObject = new Struct.urlObject();
        urlObject.text = url;
        urlObject.format = format;
        urlObject.tagName = "subURL";
        urlObject.uuid = Long.toHexString(System.currentTimeMillis());
        ArrayList<Struct.urlObject> urlObjects = url_database_list.get(groupID); //加入清單中
        urlObjects.add(1, urlObject); //加到mainURL後面
        url_database_list.set(groupID, urlObjects); //Arraylist更新元素
        System.out.println(String.format("%s 已成功加入資料庫中!", url));
//        write_url_database();
        refreshUI();//刷新 UI
    }

    // 比對 subURL有無 exact
    public Triple<Integer, Integer, Integer> matchSubURL(int groupID, String url, int format) throws Exception {
        Log.v("mydebug [matchSubURL]", String.format("準備在群組%d中搜尋...", groupID));
        int _format = format;
        ArrayList<Struct.urlObject> urlObjects = url_database_list.get(groupID);
        for (int i = 0; i < urlObjects.size(); i++) {
            Struct.urlObject urlObject = urlObjects.get(i);
            if (urlObject.tagName.equals("mainURL")) continue; //跳過mainURL
            String subURL = urlObject.text;
            Log.v("mydebug [matchSubURL]", String.format("群組%d,第%d個..subURL為%s", groupID, i, subURL));
            Log.v("mydebug [matchSubURL]", String.format("得到subURL比對層級...%d", getURLMatchFormat(url, subURL)));
            _format = Math.max(_format, getURLMatchFormat(url, subURL));
            if (urlObject.format == format) {
                if (subURL.equals(url)) {
                    return new Triple<>(groupID, format, exact);
                }
            }
        }
        return new Triple<>(groupID, format, _format);
    }

//    TODO:URL convert to URI to valid
    public static boolean urlValidator(String url){
        try {
            URL urlObj = new URL(url);
            URI uriObj = new URI(urlObj.getProtocol(), urlObj.getHost(), urlObj.getPath(), urlObj.getQuery(), null);
            return true;
        }
        catch (URISyntaxException exception) {
            return false;
        }
        catch (MalformedURLException exception) {
            return false;
        }

    }
    /* 檢查URL function */
    public void IPQSCheck() {

        /* 檢查網址是否 valid */
//        TODO:判斷是否valid有問題
//        UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
//        System.out.println("UrlValidator是否valid?"+defaultValidator.isValid(URL_text));
        final String URL_REGEX =
                "^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))" +
                        "(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)" +
                        "([).!';/?:,][[:blank:]])?$";
        Pattern urlPattern = Pattern.compile(URL_REGEX);
        Matcher matcher = urlPattern.matcher(URL_text);

//        TODO:URL 沒有嚴格按照 RFC 2396 並且不能轉換為 URI
        System.out.println("是否可轉為URI: "+urlValidator(URL_text));

        if (matcher.matches()) {
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
    public static String getData(InputStream inputStream, boolean newline) throws IOException {
        String result = null;
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = br.readLine()) != null) {
            if (newline) sb.append(line).append("\n");
            else {
                sb.append(line);
            }

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
        try {
            String encodedURL = URLEncoder.encode(URL_text, "UTF-8");
            URL url = new URL(IPQS_API_Key + encodedURL);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() == 200) {
                result = getData(connection.getInputStream(), false);
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
                System.out.println(getData(connection.getErrorStream(), false)); //印出失敗資訊
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
            myListener.safe_score = 100 - Integer.valueOf(risk_score); //安全分數
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

    // TODO:取得權限(備用)
    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { /*SDK版本*/
            String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            String readPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
            boolean checkWritePermisson = ActivityCompat.checkSelfPermission(this, writePermission)
                    == PackageManager.PERMISSION_GRANTED;
            boolean checkReadPermisson = ActivityCompat.checkSelfPermission(this, readPermission)
                    == PackageManager.PERMISSION_GRANTED;
            Log.v("mydebug", "寫入權限: " + checkWritePermisson);
            Log.v("mydebug", "讀取權限: " + checkReadPermisson);
            if (checkWritePermisson && checkReadPermisson) {
                isPermissionPassed = true;
            } else {
                ActivityCompat.requestPermissions(
                        this, new String[]{writePermission, readPermission}, 100);
            }

        }

    }

    //TODO:取得權限結果(備用)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                /*如果用戶同意*/
                isPermissionPassed = true;
            } else {
                /*如果用戶不同意*/
                if (ActivityCompat.shouldShowRequestPermissionRationale(this
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    setSnackbar("取得權限失敗", "錯誤", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(this
                        , Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    setSnackbar("取得權限失敗", "錯誤", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        }
    }

    //實作各種監聽器
    class MyListener implements View.OnClickListener, TextWatcher, ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener, Toolbar.OnMenuItemClickListener, SearchView.OnQueryTextListener, View.OnTouchListener, AdapterView.OnItemLongClickListener {

        private int groupID;
        private String url;
        private int format;
        private int safe_score;
        private boolean isExist; //addMainURL使用
        private boolean isLongClick = false;
        private Uri FileUri;
        //Comparator
        private Comparator sort_sub_old_to_new;
        private Comparator sort_main_old_to_new;
        private Comparator sort_sub_a_to_z;
        private Comparator sort_main_a_to_z;
        private Comparator sort_sub_unsafe_to_safe;
        private Comparator sort_main_unsafe_to_safe;


        //Constructor
        public MyListener() {
            initializeCmp();
        }

        //pass變數用
        public void pass_params(Object... objects) {
            if (objects.length == 1) {
                isExist = objects[0].getClass() == Boolean.class ? (boolean) objects[0] : false;
                FileUri = objects[0].getClass() == Uri.EMPTY.getClass() ? (Uri) objects[0] : null;

            }
            if (objects.length == 3) {
                groupID = objects[0].getClass() == Integer.class ? (int) objects[0] : null;
                url = objects[1].getClass() == String.class ? (String) objects[1] : null;
                format = objects[2].getClass() == Integer.class ? (int) objects[2] : null;
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
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onClick(View v) {
            Intent scan_qrcode_activity = new Intent(getApplicationContext(), UrlCheckActivity_ScanQrcodeActivity.class);
            String iconTag;
            switch (v.getId()) {
                case R.id.import_confirm_btn: /* 輸入確定按鈕 */
                    String password = ((EditText) dialog_import_file.findViewById(R.id.import_passwd)).getText().toString();
                    try {
                        importFile(FileUri, password);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    EXP_IMP_dialog.dismiss();
                    break;
                case R.id.export_confirm_btn: /* 輸出確定按鈕 */
                    String _password = ((EditText) dialog_export_file.findViewById(R.id.export_passwd)).getText().toString();
                    if (_password.length() < 8) { /* 密碼長度至少 8位數，至多到 15位數 */
                       dialog_toast.setText("密碼長度過短!");
                       dialog_toast.show();
                    }else {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/xml");
                        intent.putExtra(Intent.EXTRA_TITLE, "myOTP_Database.xml"); /* 預設名稱 */
                        startActivityForResult(intent, ACTION_CREATE_DOCUMENT);
                        EXP_IMP_dialog.dismiss();
                    }
                    break;
                case R.id.export_eye_btn: /* 密碼顯示眼睛 */
                case R.id.import_eye_btn:
                    ImageButton eye_btn = (ImageButton) v;
                    eye_btn.setSelected(!eye_btn.isSelected());
                    EditText editText;
                    if (v.getId() == R.id.export_eye_btn)
                        editText = dialog_export_file.findViewById(R.id.export_passwd);
                    else editText = dialog_import_file.findViewById(R.id.import_passwd);
                    if (eye_btn.isPressed()) { //顯示密碼
                        editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    } else { //隱藏密碼
                        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    }
                    break;
                case R.id.parent_item_icon: /*展開收合清單*/
                    int groupPosition = (int) v.getTag();
                    //還未長按時可以收合清單
                    //長按後不可收合清單
                    if (!isLongClick) {
                        if (!expandableListView.isGroupExpanded(groupPosition)) {
                            expandableListView.expandGroup(groupPosition);

                        } else {
                            //收合清單前先收合textView(單行設置)
                            int view_position = expandableListView.getPositionForView(v);
                            for (int i = 0; i < myAdapter.getChildrenCount(groupPosition) + 1; i++, view_position++) {
                                Object obj_tag = expandableListView.getChildAt(view_position).getTag();
                                TextView textView = null;
                                if (obj_tag.getClass() == MyBaseExpandableListAdapter.ViewHolderGroup.class)
                                    textView = ((MyBaseExpandableListAdapter.ViewHolderGroup) obj_tag).tv_parent_item;
                                else
                                    textView = ((MyBaseExpandableListAdapter.ViewHolderItem) obj_tag).tv_child_item;
                                textView.setSingleLine(true); //收合某群組時,設置單行
                            }
                            expandableListView.collapseGroup(groupPosition);
                        }
                    } else { //長按後不可收合清單
                        ImageButton imgBtn = (ImageButton) v;
                        imgBtn.setImageDrawable(getDrawable(R.drawable.down_arrow)); //改變圖標方向
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
                            local_url_input.setText("");
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
                        addMainURL(URL_text, safe_score);
                        local_url_input.setText("");
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
                    local_url_input.setText("");
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
                    URL_text = online_url_input.getText().toString().trim();
                    imm.hideSoftInputFromWindow(buttomDialog.getWindow().getDecorView().getWindowToken(), 0);
                    IPQSCheck();
                    break;
                case R.id.delete_no_btn:
                    del_dialog.dismiss();
                    this.isLongClick = false;
                    setSnackbar("已取消操作", "成功", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    PD_urlObjects.clear();
                    online_check_add_btn.setImageDrawable(getDrawable(R.drawable.ic_add_black_24dp));
                    online_check_add_btn.setTag(R.drawable.ic_add_black_24dp);
                    refreshUI();
                    break;
                case R.id.delete_yes_btn:
                    del_dialog.dismiss();
                    //移除要刪除的 item
                    for (int i = 0; i < PD_urlObjects.size(); i++) {
                        Struct.urlObject urlObject = PD_urlObjects.get(i);
                        Log.v("mydelete", "待刪除清單" + urlObject.text);
                        //每刪除一個元素就遍歷
                        for (int j = 0; j < url_database_list.size(); j++) {
                            ArrayList<Struct.urlObject> urlObjects = url_database_list.get(j);
                            if (urlObjects.contains(urlObject)) {
                                if (urlObject.tagName.equals("mainURL")) {
                                    url_database_list.remove(urlObjects);
                                } else {
                                    urlObjects.remove(urlObject);
                                    url_database_list.set(j, urlObjects);
                                }
                            }
                        }
                    }
                    this.isLongClick = false;
                    setSnackbar("已刪除選中項", "成功", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    PD_urlObjects.clear();
                    online_check_add_btn.setImageDrawable(getDrawable(R.drawable.ic_add_black_24dp));
                    online_check_add_btn.setTag(R.drawable.ic_add_black_24dp);
                    refreshUI(); //更新 UI
                    //確認刪除事件
                    break;
                case R.id.online_check_add_btn:
                    int iconID = (int) online_check_add_btn.getTag();
                    switch (iconID) {
                        case R.drawable.ic_add_black_24dp:
                            setButtomDialog(dialog_online_check_add_entry_view, true);
                            Dialogs.showSecureDialog(buttomDialog);
                            online_url_input.setText("");
                            break;
                        case R.drawable.ic_delete_black_24dp:
                            //刪除操作
                            if (PD_urlObjects.size() == 0) {
                                setSnackbar("待刪除清單沒有任何東西", "已選擇0", Snackbar.LENGTH_INDEFINITE);
                                myVibrator.vibrate(300);
                            } else {
                                del_dialog.show();

                            }
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
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            System.out.println(item.getItemId());
            int presort = R.id.sort_old_to_new;
            switch (item.getItemId()) {
                case R.id.action_intro_Url_Check:
                    Intent intent1 = new Intent(UrlCheckActivity.this, IntroUrlCheckActivity.class);
                    startActivity(intent1);
                    break;
                case R.id.export_btn:
                    //TODO:import按鈕監聽
                    try {
                        write_url_database(); /* 將目前資料寫入File中 */
                        getPermission();/*取得讀寫權限*/
                        if (isPermissionPassed) {
                            EditText editText = dialog_export_file.findViewById(R.id.export_passwd);
                            editText.setText(""); /*清空輸入*/
                            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            ((ImageButton)dialog_export_file.findViewById(R.id.export_eye_btn)).setSelected(false);
                            EXP_IMP_dialog.setContentView(dialog_export_file);
                            EXP_IMP_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            EXP_IMP_dialog.show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.import_btn:
                    //TODO:export按鈕監聽
                    getPermission();

                    if (isPermissionPassed) {
                        EditText editText1 = dialog_import_file.findViewById(R.id.import_passwd);
                        editText1.setText("");
                        editText1.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        ((ImageButton)dialog_import_file.findViewById(R.id.import_eye_btn)).setSelected(false);

                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("text/xml");
                        startActivityForResult(intent, ACTION_GET_CONTENT);
                    }
                    break;
                //排序
                case R.id.sort_old_to_new:
                    item.setChecked(true);
                    if (presort == R.id.sort_new_to_old) { //反轉
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.reverse(urlObjects.subList(1, urlObjects.size()));
                        Collections.reverse(url_database_list);
                    } else { //重新排序
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.sort(urlObjects.subList(1, urlObjects.size()), sort_sub_old_to_new);
                        Collections.sort(url_database_list, sort_main_old_to_new);
                    }
                    presort = R.id.sort_old_to_new;
                    refreshUI();
                    break;
                case R.id.sort_new_to_old:
                    item.setChecked(true);
                    if (presort == R.id.sort_old_to_new) { //反轉
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.reverse(urlObjects.subList(1, urlObjects.size()));
                        Collections.reverse(url_database_list);
                    } else { //重新排序
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.sort(urlObjects.subList(1, urlObjects.size()), Collections.reverseOrder(sort_sub_old_to_new));
                        Collections.sort(url_database_list, Collections.reverseOrder(sort_main_old_to_new));
                    }
                    presort = R.id.sort_new_to_old;
                    refreshUI();
                    break;
                case R.id.sort_a_to_z:
                    item.setChecked(true);
                    if (presort == R.id.sort_z_to_a) { //反轉
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.reverse(urlObjects.subList(1, urlObjects.size()));
                        Collections.reverse(url_database_list);
                    } else { //重新排列
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.sort(urlObjects.subList(1, urlObjects.size()), sort_sub_a_to_z);
                        Collections.sort(url_database_list, sort_main_a_to_z);
                    }
                    presort = R.id.sort_a_to_z;
                    refreshUI();
                    break;
                case R.id.sort_z_to_a:
                    item.setChecked(true);
                    if (presort == R.id.sort_a_to_z) { //反轉
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.reverse(urlObjects.subList(1, urlObjects.size()));
                        Collections.reverse(url_database_list);
                    } else { //重新排列
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.sort(urlObjects.subList(1, urlObjects.size()), Collections.reverseOrder(sort_sub_a_to_z));
                        Collections.sort(url_database_list, Collections.reverseOrder(sort_main_a_to_z));
                    }
                    presort = R.id.sort_z_to_a;
                    refreshUI();
                    break;
                case R.id.sort_unsafe_to_safe:
                    item.setChecked(true);
                    if (presort == R.id.sort_safe_to_unsafe) { //反轉
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.reverse(urlObjects.subList(1, urlObjects.size()));
                        Collections.reverse(url_database_list);
                    } else {
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.sort(urlObjects.subList(1, urlObjects.size()), sort_sub_unsafe_to_safe);
                        Collections.sort(url_database_list, sort_main_unsafe_to_safe);
                    }
                    presort = R.id.sort_unsafe_to_safe;
                    refreshUI();
                    break;
                case R.id.sort_safe_to_unsafe:
                    item.setChecked(true);
                    if (presort == R.id.sort_unsafe_to_safe) { //反轉
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.reverse(urlObjects.subList(1, urlObjects.size()));
                        Collections.reverse(url_database_list);
                    } else { //重新排序
                        for (ArrayList<Struct.urlObject> urlObjects : url_database_list)
                            Collections.sort(urlObjects.subList(1, urlObjects.size()), Collections.reverseOrder(sort_sub_unsafe_to_safe));
                        Collections.sort(url_database_list, Collections.reverseOrder(sort_main_unsafe_to_safe));
                    }
                    presort = R.id.sort_safe_to_unsafe;
                    refreshUI();
                    break;
            }
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            //按下搜尋後string
            return true;
        }

        //只要有文字變動就會有的string
        @Override
        public boolean onQueryTextChange(String newText) {
            refreshUI(newText);
            for (int i = 0; i < myAdapter.getGroupCount(); i++)
                expandableListView.expandGroup(i);
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
                Log.v("mytmp", "position+1: " + child_position);
                Log.v("mytmp", "群組子數目: " + myAdapter.getChildrenCount(groupPosition));
                for (int i = 0; i < myAdapter.getChildrenCount(groupPosition); i++) {
                    View view = expandableListView.getChildAt(child_position);
                    Log.v("mytmp", "子class: " + view.getClass());
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
                    if (parent_item.isSelected()) {
                        if (!PD_urlObjects.contains(item)) //不重複添加
                            PD_urlObjects.add(item);
                    } else {
                        PD_urlObjects.remove(item);  //若parent item沒選中
                    }
                }
                Log.v("mydebug", PD_urlObjects.toString());
                setSnackbar("請選擇要刪除的項目", "已選擇" + PD_urlObjects.size(), Snackbar.LENGTH_INDEFINITE);

            } else {
                //輕觸展開並複製
                parent_item.setSingleLine(!parent_item.isSingleLine()); //網址展開過長切換
                String text = parent_item.getText().toString();
                String pre_text = cmb.getPrimaryClip().getItemAt(0).getText().toString();
                setSnackbar(text, "已複製", Snackbar.LENGTH_SHORT);
                if (!text.equals(pre_text)) { //不重新複製
                    cmb.setPrimaryClip(ClipData.newPlainText(null, text));
                    snackbar.show();
                } else { //若已複製過
                    if (!snackbar.isShown()) snackbar.show();
                }

            }


            return true;
        }

        //輕點即可展開複製
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            TextView child_item = v.findViewById(R.id.tv_group_child);
            if (isLongClick) { //長按事件
                child_item.setSingleLine(false);
                child_item.setSelected(!child_item.isSelected());//state_select轉換
                toggleStrike(child_item);
                //添加移除toggle
                if (child_item.isSelected()) {
                    if (!PD_urlObjects.contains(child_item.getTag())) //不重複添加
                        PD_urlObjects.add((Struct.urlObject) child_item.getTag());
                } else {
                    PD_urlObjects.remove(child_item.getTag());
                }
                Log.v("mydebug", PD_urlObjects.toString());
                setSnackbar("請選擇要刪除的項目", "已選擇" + PD_urlObjects.size(), Snackbar.LENGTH_INDEFINITE);
            } else { //輕觸複製事件
                //輕觸展開並複製
                child_item.setSingleLine(!child_item.isSingleLine()); //網址過長展開切換
                String text = child_item.getText().toString();
                String pre_text = cmb.getPrimaryClip().getItemAt(0).getText().toString();
                setSnackbar(text, "已複製", Snackbar.LENGTH_SHORT);
                if (!text.equals(pre_text)) { //不重新複製
                    cmb.setPrimaryClip(ClipData.newPlainText(null, text));
                    snackbar.show();
                } else { //若已複製過
                    if (!snackbar.isShown()) snackbar.show();
                }

            }
            return true;
        }

        //長按事件
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

        public void initializeCmp() {
            // 時間(舊 - 新)
            sort_sub_old_to_new = new Comparator<Struct.urlObject>() {
                @Override
                public int compare(Struct.urlObject o1, Struct.urlObject o2) {
                    return o1.uuid.compareTo(o2.uuid);
                }
            };
            sort_main_old_to_new = new Comparator<ArrayList<Struct.urlObject>>() {
                @Override
                public int compare(ArrayList<Struct.urlObject> o1, ArrayList<Struct.urlObject> o2) {
                    return o1.get(0).uuid.compareTo(o2.get(0).uuid);
                }
            };
            // 字母 a - z
            sort_sub_a_to_z = new Comparator<Struct.urlObject>() {
                @Override
                public int compare(Struct.urlObject o1, Struct.urlObject o2) {
                    return o1.text.compareTo(o2.text);
                }
            };
            sort_main_a_to_z = new Comparator<ArrayList<Struct.urlObject>>() {
                @Override
                public int compare(ArrayList<Struct.urlObject> o1, ArrayList<Struct.urlObject> o2) {
                    Log.v("sort", o1.get(0).text + " " + o2.get(0).text);
                    Log.v("sort", String.valueOf(o1.get(0).text.compareTo(o2.get(0).text)));
                    //FIXME 感覺排序有點怪
                    return o1.get(0).text.compareTo(o2.get(0).text);
                }
            };
            // 安全(低 - 高)
            sort_sub_unsafe_to_safe = new Comparator<Struct.urlObject>() {
                @Override
                public int compare(Struct.urlObject o1, Struct.urlObject o2) {
                    if (o1.format == o2.format) {
                        return o1.uuid.compareTo(o2.uuid);
                    }
                    return o2.format - o1.format;
                }
            };
            sort_main_unsafe_to_safe = new Comparator<ArrayList<Struct.urlObject>>() {
                @Override
                public int compare(ArrayList<Struct.urlObject> o1, ArrayList<Struct.urlObject> o2) {
                    if (o1.get(0).safe_score == o2.get(0).safe_score) {
                        float avg_1 = 0;
                        float avg_2 = 0;
                        //avg_1計算
                        if (o1.subList(1, o1.size()).size() == 0) avg_1 = 5;
                        else {
                            for (Struct.urlObject urlObject : o1)
                                avg_1 += urlObject.format;
                            avg_1 = avg_1 / o1.subList(1, o1.size()).size();
                        }
                        // avg_2計算
                        if (o2.subList(1, o2.size()).size() == 0) avg_2 = 5;
                        else {
                            for (Struct.urlObject urlObject : o2)
                                avg_2 += urlObject.format;
                            avg_2 = avg_2 / o2.subList(1, o2.size()).size();
                        }
                        if (Float.compare(avg_1, avg_2) == 0) { //若星星平均數相同
                            if (o1.size() == o2.size()) //若size也相同,則最舊添加進來的較不安全
                                return o1.get(0).uuid.compareTo(o2.get(0).uuid);

                            return o2.size() - o1.size();
                        }
                        return Float.compare(avg_1, avg_2);

                    }
                    return o2.get(0).safe_score - o1.get(0).safe_score;
                }
            };
        }


    }


}





