/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
/* 使用EditText */
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.amulyakhare.textdrawable.TextDrawable;
import com.avito.android.krop.KropView;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.helpers.DropdownHelper;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.IconViewHelper;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.otp.YandexInfo;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.dialogs.IconPickerDialog;
import com.beemdevelopment.aegis.ui.glide.IconLoader;
import com.beemdevelopment.aegis.ui.tasks.ImportFileTask;
import com.beemdevelopment.aegis.ui.views.IconAdapter;
import com.beemdevelopment.aegis.util.Cloner;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import de.hdodenhof.circleimageview.CircleImageView;



public class UrlCheckActivity extends AegisActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* 孤兒進程導致系統重啟 */
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        /* 設定Content是 layout裡面的 activity_url_check檔案 */
        setContentView(R.layout.activity_url_check);
        setSupportActionBar(findViewById(R.id.toolbar));

        /* 變數宣告 */
        final EditText enter_url;
        final Button send_button;


        /* 設定變數 */
        enter_url = findViewById(R.id.enter_url);
        send_button = findViewById(R.id.send_button);

        /* 監聽器設定 */
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(enter_url.getText().toString());
            }
        });



    }
}

