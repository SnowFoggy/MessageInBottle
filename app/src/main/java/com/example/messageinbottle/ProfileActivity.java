package com.example.messageinbottle;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.messageinbottle.data.local.SessionManager;
//import com.example.messageinbottle.data.model.AuthResponse;
import com.example.messageinbottle.data.remote.NetworkClient;
import com.example.messageinbottle.data.repository.AuthRepository;
import com.google.android.gms.games.gamessignin.AuthResponse;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    // SharedPreferences key for avatar upload state
    private static final String PREF_NAME = "profile_avatar_state";

    private SessionManager sessionManager;
    private AuthRepository authRepository;
    private SharedPreferences uploadStatePrefs;

    private ImageView ivUserAvatar;
    private TextView tvNickname;
    private TextView tvUsername;
    private TextView tvUserId;
    private FrameLayout avatarUploadOverlay;
    private MaterialButton btnEditProfile;
    private View avatarRing;
    private ImageView ivAvatarEditHint;

    private File pendingAvatarFile;
    private boolean isUploadingAvatar = false;

    private final ActivityResultLauncher<String> avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) {
                    return;
                }
                File tempFile = createTempImageFile(uri, "user-avatar");
                if (tempFile == null) {
                    showMessage(getString(R.string.avatar_picker_failed));
                    return;
                }
                pendingAvatarFile = tempFile;
                uploadAvatar();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository(this);
        uploadStatePrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initViews();
        setupEdgeToEdge();
        setupBackHandler();
        bindActions();
        loadUserData();
    }

    private void initViews() {
        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvUsername = findViewById(R.id.tvUsername);
        tvUserId = findViewById(R.id.tvUserId);
        avatarUploadOverlay = findViewById(R.id.avatarUploadOverlay);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        avatarRing = findViewById(R.id.avatarRing);
        ivAvatarEditHint = findViewById(R.id.ivAvatarEditHint);
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getSystemWindowInsets();
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(0, android.R.anim.fade_out);
            }
        });
    }

    private void bindActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, android.R.anim.fade_out);
        });

        // 点击头像区域 → 打开图片选择器
        avatarRing.setOnClickListener(v -> openAvatarPicker());
        ivUserAvatar.setOnClickListener(v -> openAvatarPicker());
        ivAvatarEditHint.setOnClickListener(v -> openAvatarPicker());

        // 点击编辑资料按钮（预留功能入口）
        btnEditProfile.setOnClickListener(v -> {
            // TODO: 后续扩展编辑昵称等功能
            showMessage(getString(R.string.profile_edit_hint));
        });
    }

    private void openAvatarPicker() {
        if (isUploadingAvatar) {
            return;
        }
        avatarPickerLauncher.launch("image/*");
    }

    /**
     * 从本地会话管理器加载用户数据并渲染界面
     */
    private void loadUserData() {
        String nickname = sessionManager.getDisplayName();
        String username = sessionManager.getUsername();
        long userId = sessionManager.getUserId();
        String avatarUrl = sessionManager.getAvatarUrl();

        tvNickname.setText(nickname);
        tvUsername.setText(username);
        tvUserId.setText("ID: " + userId);

        renderAvatar(avatarUrl);
    }

    /**
     * 使用 Glide 加载并显示用户头像
     *
     * @param avatarUrl 头像 URL，为空时显示默认占位图
     */
    private void renderAvatar(String avatarUrl) {
        if (TextUtils.isEmpty(avatarUrl)) {
            ivUserAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        } else {
            Glide.with(this)
                    .load(avatarUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(ivUserAvatar);
        }
    }

    /**
     * 将选中的图片 URI 复制到临时文件
     *
     * @param uri     图片 URI
     * @param prefix  临时文件名前缀
     * @return 临时文件，失败返回 null
     */
    private File createTempImageFile(Uri uri, String prefix) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            File outputDir = new File(getCacheDir(), "avatar_temp");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File outputFile = new File(outputDir, prefix + "_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            return outputFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 调用后端接口上传用户头像
     */
    private void uploadAvatar() {
        if (pendingAvatarFile == null || isUploadingAvatar) {
            return;
        }
        if (!sessionManager.isLoggedIn()) {
            showMessage(getString(R.string.error_empty_login));
            return;
        }

        isUploadingAvatar = true;
        showUploadOverlay(true);
        findViewById(R.id.btnBack).setEnabled(false);
        btnEditProfile.setEnabled(false);

        long userId = sessionManager.getUserId();

        NetworkClient.getInstance().executor().execute(() -> {
            try {
                Map<String, String> formData = new HashMap<>();
                formData.put("userId", String.valueOf(userId));
                String json = NetworkClient.getInstance().postMultipart(
                        "/api/auth/avatar",
                        formData,
                        "avatar",
                        pendingAvatarFile,
                        "image/jpeg"
                );

                new Handler(Looper.getMainLooper()).post(() -> {
                    isUploadingAvatar = false;
                    showUploadOverlay(false);
                    findViewById(R.id.btnBack).setEnabled(true);
                    btnEditProfile.setEnabled(true);

                    if (json != null && !json.isEmpty()) {
                        try {
                            com.google.gson.JsonObject result =
                                    new com.google.gson.JsonParser().parse(json).getAsJsonObject();
                            String avatarUrl = "";
                            String message = "";
                            if (result.has("data")) {
                                avatarUrl = result.getAsJsonObject("data")
                                        .get("avatarUrl").getAsString();
                            }
                            if (result.has("message")) {
                                message = result.get("message").getAsString();
                            }
                            sessionManager.updateAvatarUrl(avatarUrl);
                            renderAvatar(avatarUrl);
                            showMessage(TextUtils.isEmpty(message)
                                    ? getString(R.string.avatar_upload_success) : message);
                        } catch (Exception e) {
                            showMessage(getString(R.string.avatar_upload_success));
                        }
                    } else {
                        showMessage(getString(R.string.avatar_upload_failed));
                    }
                });

                // 清理临时文件
                pendingAvatarFile.delete();
                pendingAvatarFile = null;

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    isUploadingAvatar = false;
                    showUploadOverlay(false);
                    findViewById(R.id.btnBack).setEnabled(true);
                    btnEditProfile.setEnabled(true);
                    showMessage(getString(R.string.avatar_upload_failed));
                });

                if (pendingAvatarFile != null) {
                    pendingAvatarFile.delete();
                    pendingAvatarFile = null;
                }
            }
        });
    }

    /**
     * 显示或隐藏头像上传中的遮罩层
     */
    private void showUploadOverlay(boolean show) {
        avatarUploadOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
