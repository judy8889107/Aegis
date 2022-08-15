package com.beemdevelopment.aegis.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.BitmapHelper;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.helpers.QrCodeAnalyzer;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.fragments.preferences.BackupsPreferencesFragment;
import com.beemdevelopment.aegis.ui.fragments.preferences.PreferencesFragment;
import com.beemdevelopment.aegis.ui.views.EntryListView;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

public class MainActivity extends AegisActivity implements EntryListView.Listener {
    // activity request codes
    private static final int CODE_SCAN = 0;
    private static final int CODE_ADD_ENTRY = 1;
    private static final int CODE_EDIT_ENTRY = 2;
    private static final int CODE_DO_INTRO = 3;
    private static final int CODE_DECRYPT = 4;
    private static final int CODE_PREFERENCES = 5;
    private static final int CODE_SCAN_IMAGE = 6;
    /* 加入Result Code CODE_URL_CHECK */
    private static final int CODE_URL_CHECK = 7;

    // permission request codes
    private static final int CODE_PERM_CAMERA = 0;
    private static final int CODE_PERM_READ_STORAGE = 1;

    private boolean _loaded;
    private boolean _searchSubmitted;
    private boolean _isRecreated;

    private List<VaultEntry> _selectedEntries;
    private ActionMode _actionMode;

    private Menu _menu;
    private SearchView _searchView;
    private EntryListView _entryListView;
    private LinearLayout _btnBackupError;

    private FabScrollHelper _fabScrollHelper;

    private ActionMode.Callback _actionModeCallbacks = new ActionModeCallbacks();
    private int Preset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        _loaded = false;

        if (savedInstanceState != null) {
            _isRecreated = true;
        }

        _entryListView = (EntryListView) getSupportFragmentManager().findFragmentById(R.id.key_profiles);
        _entryListView.setListener(this);
        _entryListView.setCodeGroupSize(_prefs.getCodeGroupSize());
        _entryListView.setShowAccountName(_prefs.isAccountNameVisible());
        _entryListView.setHighlightEntry(_prefs.isEntryHighlightEnabled());
        _entryListView.setPauseFocused(_prefs.isPauseFocusedEnabled());
        _entryListView.setTapToReveal(_prefs.isTapToRevealEnabled());
        _entryListView.setTapToRevealTime(_prefs.getTapToRevealTime());
        _entryListView.setSortCategory(_prefs.getCurrentSortCategory(), false);
        _entryListView.setViewMode(_prefs.getCurrentViewMode());
        _entryListView.setIsCopyOnTapEnabled(_prefs.isCopyOnTapEnabled());
        _entryListView.setPrefGroupFilter(_prefs.getGroupFilter());

         FloatingActionButton fab = findViewById(R.id.fab);
        /* J:這裡連接dialog_add_entry面板，並setOnClickListener負責監聽 */
         fab.setOnClickListener(v -> {
             View view = getLayoutInflater().inflate(R.layout.dialog_add_entry, null);
             BottomSheetDialog dialog = new BottomSheetDialog(this);
             dialog.setContentView(view);

             /* add_entry的event */
             view.findViewById(R.id.fab_entry).setOnClickListener(v1 -> {
                 dialog.dismiss(); /* 關閉dialog */
                 startEditEntryActivityForManual(CODE_ADD_ENTRY); /* 呼叫對應的function */
             });
             /* scan_image的event */
             view.findViewById(R.id.fab_scan_image).setOnClickListener(v2 -> {
                 dialog.dismiss();
                 startScanImageActivity();
             });
             /* scan_QRcode的event */
             view.findViewById(R.id.fab_scan).setOnClickListener(v3 -> {
                 dialog.dismiss();
                 startScanActivity();
             });
             /* 新增 url_check的event */
             view.findViewById(R.id.fab_url_check).setOnClickListener( v4 -> {
                dialog.dismiss();
                startUrlCheckActivity();
             });

             Dialogs.showSecureDialog(dialog);
         });

        _btnBackupError = findViewById(R.id.btn_backup_error);
        _btnBackupError.setOnClickListener(view -> {
            startPreferencesActivity(BackupsPreferencesFragment.class, "pref_backups");
        });

        _fabScrollHelper = new FabScrollHelper(fab);
        _selectedEntries = new ArrayList<>();
    }

    @Override
    protected void onDestroy() {
        _entryListView.setListener(null);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Map<UUID, Integer> usageMap = _entryListView.getUsageCounts();
        if (usageMap != null) {
            _prefs.setUsageCount(usageMap);
        }

        super.onPause();
    }

    /* J:onActivityResult 按返回鍵回到前一個畫面前執行的事 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case CODE_SCAN:
                onScanResult(data);
                break;
            case CODE_ADD_ENTRY:
                onAddEntryResult(data);
                break;
            case CODE_EDIT_ENTRY:
                onEditEntryResult(data);
                break;
            case CODE_DO_INTRO:
                onIntroResult();
                break;
            case CODE_DECRYPT:
                onDecryptResult();
                break;
            case CODE_PREFERENCES:
                onPreferencesResult(data);
                break;
            case CODE_SCAN_IMAGE:
                onScanImageResult(data);
            /* J:增加 onACtivityResult CODE: CODE_URL_CHECK */
            case CODE_URL_CHECK:
                /* 還未寫function，暫定按下返回鍵時，結束UrlCheckActivity */
                this.finish();
                break;


        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!PermissionHelper.checkResults(grantResults)) {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case CODE_PERM_CAMERA:
                startScanActivity();
                break;
            case CODE_PERM_READ_STORAGE:
                startScanImageActivity();
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onPreferencesResult(Intent data) {
        // refresh the entire entry list if needed
        if (_loaded) {
            if (data.getBooleanExtra("needsRecreate", false)) {
                recreate();
            } else if (data.getBooleanExtra("needsRefresh", false)) {
                boolean showAccountName = _prefs.isAccountNameVisible();
                int codeGroupSize = _prefs.getCodeGroupSize();
                boolean highlightEntry = _prefs.isEntryHighlightEnabled();
                boolean pauseFocused = _prefs.isPauseFocusedEnabled();
                boolean tapToReveal = _prefs.isTapToRevealEnabled();
                int tapToRevealTime = _prefs.getTapToRevealTime();
                ViewMode viewMode = _prefs.getCurrentViewMode();
                boolean copyOnTap = _prefs.isCopyOnTapEnabled();
                _entryListView.setShowAccountName(showAccountName);
                _entryListView.setCodeGroupSize(codeGroupSize);
                _entryListView.setHighlightEntry(highlightEntry);
                _entryListView.setPauseFocused(pauseFocused);
                _entryListView.setTapToReveal(tapToReveal);
                _entryListView.setTapToRevealTime(tapToRevealTime);
                _entryListView.setViewMode(viewMode);
                _entryListView.setIsCopyOnTapEnabled(copyOnTap);
                _entryListView.refresh(true);
            }
        }
    }

    private void startEditEntryActivityForNew(int requestCode, VaultEntry entry) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("newEntry", entry);
        intent.putExtra("isManual", false);
        startActivityForResult(intent, requestCode);
    }
    /* J:Url_check 參考 */
    private void startEditEntryActivityForManual(int requestCode) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        /* intent.putExtra是用來activity之間傳遞參數的(傳遞過去的名稱(string),變數名稱or內容) */
        intent.putExtra("newEntry", VaultEntry.getDefault());
        intent.putExtra("isManual", true); /* 傳遞過去的訊息叫"isManual", 內容是true */
        startActivityForResult(intent, requestCode);
    }

    private void startEditEntryActivity(int requestCode, VaultEntry entry) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("entryUUID", entry.getUUID());
        startActivityForResult(intent, requestCode);
    }

    private void onScanResult(Intent data) {
        List<VaultEntry> entries = (ArrayList<VaultEntry>) data.getSerializableExtra("entries");
        if (entries.size() == 1) {
            startEditEntryActivityForNew(CODE_ADD_ENTRY, entries.get(0));
        } else {
            for (VaultEntry entry : entries) {
                _vaultManager.getVault().addEntry(entry);
                if (_loaded) {
                    _entryListView.addEntry(entry);
                }
            }

            saveAndBackupVault();
        }
    }

    private void onAddEntryResult(Intent data) {
        if (_loaded) {
            UUID entryUUID = (UUID) data.getSerializableExtra("entryUUID");
            VaultEntry entry = _vaultManager.getVault().getEntryByUUID(entryUUID);
            _entryListView.addEntry(entry, true);
        }
    }

    private void onEditEntryResult(Intent data) {
        if (_loaded) {
            UUID entryUUID = (UUID) data.getSerializableExtra("entryUUID");

            if (data.getBooleanExtra("delete", false)) {
                _entryListView.removeEntry(entryUUID);
            } else {
                VaultEntry entry = _vaultManager.getVault().getEntryByUUID(entryUUID);
                _entryListView.replaceEntry(entryUUID, entry);
            }
        }
    }

    private void onScanImageResult(Intent intent) {
        decodeQrCodeImage(intent.getData());
    }

    private void decodeQrCodeImage(Uri inputFile) {
        Bitmap bitmap;

        try {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();

            try (InputStream inputStream = getContentResolver().openInputStream(inputFile)) {
                bitmap = BitmapFactory.decodeStream(inputStream, null, bmOptions);
                bitmap = BitmapHelper.resize(bitmap, QrCodeAnalyzer.RESOLUTION.getWidth(), QrCodeAnalyzer.RESOLUTION.getHeight());
            }

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new QRCodeReader();
            Result result = reader.decode(binaryBitmap);

            GoogleAuthInfo info = GoogleAuthInfo.parseUri(result.getText());
            VaultEntry entry = new VaultEntry(info);

            startEditEntryActivityForNew(CODE_ADD_ENTRY, entry);
        } catch (NotFoundException | IOException | ChecksumException | FormatException | GoogleAuthInfoException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.unable_to_read_qrcode, e);
        }
    }

    private void updateSortCategoryMenu() {
        SortCategory category = _prefs.getCurrentSortCategory();
        _menu.findItem(category.getMenuItem()).setChecked(true);
    }

    private void onIntroResult() {
        loadEntries();
        checkTimeSyncSetting();
    }

    private void checkTimeSyncSetting() {
        boolean autoTime = Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME, 1) == 1;
        if (!autoTime && _prefs.isTimeSyncWarningEnabled()) {
            Dialogs.showTimeSyncWarningDialog(this, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
                startActivity(intent);
            });
        }
    }

    private void onDecryptResult() {
        loadEntries();
        checkTimeSyncSetting();
    }

    private void startScanActivity() {
        if (!PermissionHelper.request(this, CODE_PERM_CAMERA, Manifest.permission.CAMERA)) {
            return;
        }

        Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
        startActivityForResult(scannerActivity, CODE_SCAN);
    }

    /* J:新增url_check function */
    private void startUrlCheckActivity(){
        /* 表示試圖在this這個地方開啟class UrlCheckActivity */
        Intent intent = new Intent(this, UrlCheckActivity.class);
        /* 可以使用StartActivityForResult，並設定一個要求值，回傳時便同時回傳此值
        發出意圖的activity便可以知道是由哪一個要求索回傳的資訊。(CODE_URL_CHECK = 7)
        本來可以用startActivity()，但是若想要取得 url_check 頁面回傳的資料就要用startActivityResult()
        這裡避免需要做備份網址的情況，所以選用startActivityForResult()
        */
        startActivityForResult(intent, CODE_URL_CHECK);

    }

    private void startScanImageActivity() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");

        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_picture));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { fileIntent });
        _vaultManager.startActivityForResult(this, chooserIntent, CODE_SCAN_IMAGE);
    }

    private void startPreferencesActivity() {
        startPreferencesActivity(null, null);
    }

    private void startPreferencesActivity(Class<? extends PreferencesFragment> fragmentType, String preference) {
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.putExtra("fragment", fragmentType);
        intent.putExtra("pref", preference);
        startActivityForResult(intent, CODE_PREFERENCES);
    }

    private void doShortcutActions() {
        Intent intent = getIntent();
        String action = intent.getStringExtra("action");
        if (action == null || !_vaultManager.isVaultLoaded()) {
            return;
        }

        switch (action) {
            case "scan":
                startScanActivity();
                break;
        }

        intent.removeExtra("action");
    }

    private void handleDeeplink() {
        if (!_vaultManager.isVaultLoaded()) {
            return;
        }

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && uri != null) {
            intent.setData(null);
            intent.setAction(null);

            GoogleAuthInfo info = null;
            try {
                info = GoogleAuthInfo.parseUri(uri);
            } catch (GoogleAuthInfoException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.unable_to_read_qrcode, e);
            }

            if (info != null) {
                VaultEntry entry = new VaultEntry(info);
                startEditEntryActivityForNew(CODE_ADD_ENTRY, entry);
            }
        }
    }

    private void handleSharedImage() {
        if (!_vaultManager.isVaultLoaded()) {
            return;
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (Intent.ACTION_SEND.equals(action) && uri != null) {
            intent.setAction(null);
            intent.removeExtra(Intent.EXTRA_STREAM);

            decodeQrCodeImage(uri);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_vaultManager.isVaultInitNeeded()) {
            if (_prefs.isIntroDone()) {
                Toast.makeText(this, getString(R.string.vault_not_found), Toast.LENGTH_SHORT).show();
            }
            Intent intro = new Intent(this, IntroActivity.class);
            startActivityForResult(intro, CODE_DO_INTRO);
            return;
        }

        if (!_vaultManager.isVaultLoaded() && !_vaultManager.isVaultFileLoaded()) {
            Dialogs.showErrorDialog(this, R.string.vault_load_error, _vaultManager.getVaultFileError(), (dialog1, which) -> finish());
            return;
        }

        if (!_vaultManager.isVaultLoaded()) {
            startAuthActivity(false);
        } else if (_loaded) {
            // update the list of groups in the entry list view so that the chip gets updated
            _entryListView.setGroups(_vaultManager.getVault().getGroups());

            // update the usage counts in case they are edited outside of the EntryListView
            _entryListView.setUsageCounts(_prefs.getUsageCounts());

            // refresh all codes to prevent showing old ones
            _entryListView.refresh(false);
        } else {
            loadEntries();
            checkTimeSyncSetting();
        }

        handleDeeplink();
        handleSharedImage();
        updateLockIcon();
        doShortcutActions();
        updateBackupErrorBar();
    }

    @Override
    public void onBackPressed() {
        if (!_searchView.isIconified() || _searchSubmitted) {
            _searchSubmitted = false;
            _entryListView.setSearchFilter(null);

            collapseSearchView();
            setTitle(R.string.app_name);
            return;
        }

        if (_vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_BACK_BUTTON)) {
            _vaultManager.lock(false);
            return;
        }

        super.onBackPressed();
    }

    private void deleteEntries(List<VaultEntry> entries) {
        for (VaultEntry entry: entries) {
            VaultEntry oldEntry = _vaultManager.getVault().removeEntry(entry);
            _entryListView.removeEntry(oldEntry);
        }

        saveAndBackupVault();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        updateLockIcon();
        if (_loaded) {
            _entryListView.setGroups(_vaultManager.getVault().getGroups());
            updateSortCategoryMenu();
        }

        MenuItem searchViewMenuItem = menu.findItem(R.id.mi_search);

        _searchView = (SearchView) searchViewMenuItem.getActionView();

        _searchView.setQueryHint(getString(R.string.search));
        if (_prefs.getFocusSearchEnabled() && !_isRecreated) {
            _searchView.setIconified(false);
            _searchView.setFocusable(true);
            _searchView.requestFocus();
            _searchView.requestFocusFromTouch();
        }
        _searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                setTitle(getString(R.string.search));
                getSupportActionBar().setSubtitle(s);
                _searchSubmitted = true;
                collapseSearchView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!_searchSubmitted) {
                    _entryListView.setSearchFilter(s);
                }
                return false;
            }
        });
        _searchView.setOnSearchClickListener(v -> {
            if (_searchSubmitted) {
                _searchSubmitted = false;
                _entryListView.setSearchFilter(null);
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                startPreferencesActivity();
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_lock:
                _vaultManager.lock(true);
                return true;
            case R.id.action_2FA: {

                String[] strings={"如何使用2FA","如何使用UrlCheck"};

                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setSingleChoiceItems(strings,Preset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Preset = which;//把預設值改成選擇的
                    }
                });
                builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (Preset){
                            case 0:
                                Intent intent = new Intent(MainActivity.this, Intro2FAActivity.class);
                                startActivity(intent);
                                break;
                            case 1:
                                Intent intent1 = new Intent(MainActivity.this, IntroUrlCheckActivity.class);
                                startActivity(intent1);
                                break;
                        }
                        dialog.dismiss();//結束對話框
                    }
                });

                builder.setNeutralButton("略過", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();//結束對話框
                    }
                });
                builder.show();
                return true;
            }
            case R.id.action_intro_Url_Check: {
                Intent intent = new Intent(this, IntroUrlCheckActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                if (item.getGroupId() == R.id.action_sort_category) {
                    item.setChecked(true);

                    SortCategory sortCategory;
                    switch (item.getItemId()) {
                        case R.id.menu_sort_alphabetically:
                            sortCategory = SortCategory.ISSUER;
                            break;
                        case R.id.menu_sort_alphabetically_reverse:
                            sortCategory = SortCategory.ISSUER_REVERSED;
                            break;
                        case R.id.menu_sort_alphabetically_name:
                            sortCategory = SortCategory.ACCOUNT;
                            break;
                        case R.id.menu_sort_alphabetically_name_reverse:
                            sortCategory = SortCategory.ACCOUNT_REVERSED;
                            break;
                        case R.id.menu_sort_usage_count:
                            sortCategory = SortCategory.USAGE_COUNT;
                            break;
                        case R.id.menu_sort_custom:
                        default:
                            sortCategory = SortCategory.CUSTOM;
                            break;
                    }

                    _entryListView.setSortCategory(sortCategory, true);
                    _prefs.setCurrentSortCategory(sortCategory);
                }
                return super.onOptionsItemSelected(item);
        }
    }

    private void collapseSearchView() {
        _searchView.setQuery(null, false);
        _searchView.setIconified(true);
    }

    private void loadEntries() {
        if (!_loaded) {
            _entryListView.setUsageCounts(_prefs.getUsageCounts());
            _entryListView.addEntries(_vaultManager.getVault().getEntries());
            _entryListView.runEntriesAnimation();
            _loaded = true;
        }
    }

    private void startAuthActivity(boolean inhibitBioPrompt) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("inhibitBioPrompt", inhibitBioPrompt);
        startActivityForResult(intent, CODE_DECRYPT);
    }

    private void updateLockIcon() {
        // hide the lock icon if the vault is not unlocked
        if (_menu != null && _vaultManager.isVaultLoaded()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_vaultManager.getVault().isEncryptionEnabled());
        }
    }

    private void updateBackupErrorBar() {
        String error = null;
        if (_prefs.isBackupsEnabled()) {
            error = _prefs.getBackupsError();
        }

        _btnBackupError.setVisibility(error == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEntryClick(VaultEntry entry) {
        if (_actionMode != null) {
            if (_selectedEntries.isEmpty()) {
                _actionMode.finish();
            } else {
                setIsMultipleSelected(_selectedEntries.size() > 1);
            }

            return;
        }
    }

    @Override
    public void onSelect(VaultEntry entry) {
        _selectedEntries.add(entry);
    }

    @Override
    public void onDeselect(VaultEntry entry) {
        _selectedEntries.remove(entry);
    }

    private void setIsMultipleSelected(boolean multipleSelected) {
        _entryListView.setIsLongPressDragEnabled(!multipleSelected);
        _actionMode.getMenu().findItem(R.id.action_edit).setVisible(!multipleSelected);
        _actionMode.getMenu().findItem(R.id.action_copy).setVisible(!multipleSelected);
    }

    @Override
    public void onLongEntryClick(VaultEntry entry) {
        if (!_selectedEntries.isEmpty()) {
            return;
        }

        _selectedEntries.add(entry);
        _entryListView.setActionModeState(true, entry);
        _actionMode = startSupportActionMode(_actionModeCallbacks);
    }

    @Override
    public void onEntryMove(VaultEntry entry1, VaultEntry entry2) {
        _vaultManager.getVault().swapEntries(entry1, entry2);
    }

    @Override
    public void onEntryDrop(VaultEntry entry) {
        saveVault();
    }

    @Override
    public void onEntryChange(VaultEntry entry) {
        saveAndBackupVault();
    }

    public void onEntryCopy(VaultEntry entry) {
        copyEntryCode(entry);
    }

    @Override
    public void onScroll(int dx, int dy) {
        _fabScrollHelper.onScroll(dx, dy);
    }

    @Override
    public void onListChange() { _fabScrollHelper.setVisible(true); }

    @Override
    public void onSaveGroupFilter(List<String> groupFilter) {
        _prefs.setGroupFilter(groupFilter);
    }

    @Override
    public void onLocked(boolean userInitiated) {
        if (_actionMode != null) {
            _actionMode.finish();
        }
        if (_searchView != null && !_searchView.isIconified()) {
            collapseSearchView();
        }

        _entryListView.clearEntries();
        _loaded = false;

        if (userInitiated) {
            startAuthActivity(true);
        } else {
            super.onLocked(false);
        }
    }

    private void copyEntryCode(VaultEntry entry) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text/plain", entry.getInfo().getOtp());
        clipboard.setPrimaryClip(clip);
    }

    private class ActionModeCallbacks implements ActionMode.Callback {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_copy:
                        copyEntryCode(_selectedEntries.get(0));
                        mode.finish();
                        return true;

                    case R.id.action_edit:
                        startEditEntryActivity(CODE_EDIT_ENTRY, _selectedEntries.get(0));
                        mode.finish();
                        return true;

                    case R.id.action_share_qr:
                        Intent intent = new Intent(getBaseContext(), TransferEntriesActivity.class);
                        ArrayList<GoogleAuthInfo> authInfos = new ArrayList<>();
                        for (VaultEntry entry : _selectedEntries) {
                            GoogleAuthInfo authInfo = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
                            authInfos.add(authInfo);
                        }

                        intent.putExtra("authInfos", authInfos);
                        startActivity(intent);

                        mode.finish();
                        return true;

                    case R.id.action_delete:
                        Dialogs.showDeleteEntriesDialog(MainActivity.this, _selectedEntries, (d, which) -> {
                            deleteEntries(_selectedEntries);

                            for (VaultEntry entry : _selectedEntries) {
                                if (entry.getGroup() != null) {
                                    TreeSet<String> groups = _vaultManager.getVault().getGroups();
                                    if (!groups.contains(entry.getGroup())) {
                                        _entryListView.setGroups(groups);
                                        break;
                                    }
                                }
                            }

                            mode.finish();
                        });
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                _entryListView.setActionModeState(false, null);
                _selectedEntries.clear();
                _actionMode = null;
            }
    }
}
