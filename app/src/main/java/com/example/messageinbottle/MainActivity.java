package com.example.messageinbottle;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.messageinbottle.data.local.SessionManager;
import com.example.messageinbottle.data.model.AcceptedTask;
import com.example.messageinbottle.data.model.HomeTask;
import com.example.messageinbottle.data.model.MessageEnvelope;
import com.example.messageinbottle.data.model.MessageItem;
import com.example.messageinbottle.data.model.MineTaskRecord;
import com.example.messageinbottle.data.model.Wallet;
import com.example.messageinbottle.data.repository.AuthRepository;
import com.example.messageinbottle.data.repository.MessageRepository;
import com.example.messageinbottle.data.repository.MineRepository;
import com.example.messageinbottle.data.repository.StageTwoRepository;
import com.example.messageinbottle.data.remote.NetworkClient;
import com.example.messageinbottle.databinding.ActivityMainBinding;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {

    private static final String QUERY_PUBLISHED = "已发布";
    private static final String MINE_STATUS_ALL = "全部";
    private static final String MINE_STATUS_ACCEPTED = "已接取";
    private static final String MINE_STATUS_CANCELLED = "已取消";
    private static final String MINE_STATUS_REJECTED = "驳回";
    private static final String MINE_STATUS_COMPLETED = "已完成";
    private static final String QUERY_ACCEPTED_IN_PROGRESS = "进行中";
    private static final String QUERY_ACCEPTED_PENDING = "待审核";
    private static final String QUERY_ACCEPTED_COMPLETED = "已完成";
    private static final String QUERY_ACCEPTED_FAILED = "完成失败";
    private static final String QUERY_NOTIFICATION_ALL = "全部";
    private static final String QUERY_NOTIFICATION_READ = "已读";
    private static final String QUERY_NOTIFICATION_UNREAD = "未读";
    private static final long ACCEPTED_TASK_REFRESH_INTERVAL_MS = 5000L;

    private static final int DETAIL_MODE_HOME = 1;
    private static final int DETAIL_MODE_MINE = 2;
    private static final int DETAIL_MODE_ACCEPTED = 3;
    private static final String NOTIFICATION_PREF_NAME = "message_notification_state";
    private static final String NOTIFICATION_READ_PREFIX = "read_ids_";
    private static final String NOTIFICATION_DELETED_PREFIX = "deleted_ids_";
    private static final String SYSTEM_NOTIFICATION_CHANNEL_ID = "message_updates";
    private static final String EXTRA_OPEN_NOTIFICATION_PANEL = "extra_open_notification_panel";

    private ActivityMainBinding binding;
    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private StageTwoRepository stageTwoRepository;
    private MineRepository mineRepository;
    private MessageRepository messageRepository;
    private SharedPreferences notificationPreferences;
    private boolean isLoginMode = true;
    private String selectedCategory = "全部";
    private String currentMineQuery = QUERY_PUBLISHED;
    private String currentMineStatusFilter = MINE_STATUS_ALL;
    private String currentAcceptedQuery = QUERY_ACCEPTED_IN_PROGRESS;
    private String currentNotificationQuery = QUERY_NOTIFICATION_ALL;
    private HomeTask currentDetailTask;
    private MineTaskRecord currentMineDetailTask;
    private AcceptedTask currentAcceptedDetailTask;
    private int currentDetailMode = DETAIL_MODE_HOME;
    private File pendingProofImageFile;
    private File pendingAvatarImageFile;
    private boolean isSubmittingProof;
    private boolean isUploadingAvatar;
    private WebSocket messageWebSocket;
    private final List<AppNotification> notifications = new ArrayList<>();
    private final Set<Long> notificationIds = new HashSet<>();
    private final Set<Long> readNotificationIds = new HashSet<>();
    private final Set<Long> deletedNotificationIds = new HashSet<>();
    private final Set<Long> selectedNotificationIds = new HashSet<>();
    private boolean isNotificationSelectionMode;
    private final List<String> lastAcceptedNotificationKeys = new ArrayList<>();
    private final List<String> lastMineNotificationKeys = new ArrayList<>();
    private boolean hasRequestedNotificationPermission;

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> hasRequestedNotificationPermission = true
    );

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null || currentAcceptedDetailTask == null) {
                    return;
                }
                File tempFile = createTempImageFile(uri, "task-proof");
                if (tempFile == null) {
                    showMessage(getString(R.string.avatar_picker_failed));
                    return;
                }
                pendingProofImageFile = tempFile;
                submitAcceptedTaskWithProof();
            }
    );

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
                pendingAvatarImageFile = tempFile;
                uploadAvatar();
            }
    );

    private final Handler acceptedTaskRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable acceptedTaskRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (sessionManager != null && sessionManager.isLoggedIn()) {
                refreshAcceptedTasksIfVisible();
                acceptedTaskRefreshHandler.postDelayed(this, ACCEPTED_TASK_REFRESH_INTERVAL_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authRepository = new AuthRepository(this);
        sessionManager = new SessionManager(this);
        stageTwoRepository = new StageTwoRepository(this);
        mineRepository = new MineRepository(this);
        messageRepository = new MessageRepository(this);
        notificationPreferences = getSharedPreferences(NOTIFICATION_PREF_NAME, MODE_PRIVATE);

        initViews();
        bindActions();
        createNotificationChannelIfNeeded();
        updateAuthState();
        handleNotificationPanelIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationPanelIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAcceptedTaskAutoRefresh();
        connectMessageWebSocketIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAcceptedTaskAutoRefresh();
        disconnectMessageWebSocket();
    }

    private void initViews() {
        switchMode(true);
        selectDashboardPage(binding.navHome);
        renderCategoryChips();
        setMineFiltersExpanded(false, false);
        updateAcceptedQueryButtons();
        updateMineQueryButtons();
        updateNotificationQueryButtons();
        renderNotifications();
        hideSideMenu();
        hideNotificationPanel();
    }

    private void bindActions() {
        binding.tabLogin.setOnClickListener(v -> switchMode(true));
        binding.tabRegister.setOnClickListener(v -> switchMode(false));
        binding.btnSubmit.setOnClickListener(v -> submitAuth());
        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnMineLogout.setOnClickListener(v -> logout());
        binding.navHome.setOnClickListener(v -> selectDashboardPage(binding.navHome));
        binding.navAccepted.setOnClickListener(v -> {
            selectDashboardPage(binding.navAccepted);
            refreshAcceptedTasksIfVisible();
        });
        binding.navMine.setOnClickListener(v -> {
            selectDashboardPage(binding.navMine);
            renderWalletInfo();
            renderMineTaskList();
        });
        binding.ivUserAvatar.setOnClickListener(v -> toggleSideMenu());
        binding.btnNotification.setOnClickListener(v -> toggleNotificationPanel());
        binding.sideMenuOverlay.setOnClickListener(v -> hideSideMenu());
        binding.sideMenuPanel.setOnClickListener(v -> { });
        binding.notificationOverlay.setOnClickListener(v -> hideNotificationPanel());
        binding.notificationPanel.setOnClickListener(v -> { });
        binding.btnUploadAvatar.setOnClickListener(v -> pickAvatarImage());
        binding.menuProfile.setOnClickListener(v -> showMessage(getString(R.string.avatar_feature_reserved_profile)));
        binding.menuSettingsPrivacy.setOnClickListener(v -> showMessage(getString(R.string.avatar_feature_reserved_privacy)));
        binding.btnQueryNotificationAll.setOnClickListener(v -> {
            currentNotificationQuery = QUERY_NOTIFICATION_ALL;
            updateNotificationQueryButtons();
            renderNotifications();
        });
        binding.btnQueryNotificationRead.setOnClickListener(v -> {
            currentNotificationQuery = QUERY_NOTIFICATION_READ;
            updateNotificationQueryButtons();
            renderNotifications();
        });
        binding.btnQueryNotificationUnread.setOnClickListener(v -> {
            currentNotificationQuery = QUERY_NOTIFICATION_UNREAD;
            updateNotificationQueryButtons();
            renderNotifications();
        });
        binding.btnMarkAllNotificationsRead.setOnClickListener(v -> markAllNotificationsRead());
        binding.btnDeleteSelectedNotifications.setOnClickListener(v -> deleteSelectedNotifications());
        binding.btnCancelNotificationSelection.setOnClickListener(v -> exitNotificationSelectionMode());
        binding.btnCloseDetail.setOnClickListener(v -> hideDetail());
        binding.btnAcceptTask.setOnClickListener(v -> onDetailPrimaryAction());
        binding.btnSecondaryAction.setOnClickListener(v -> onDetailSecondaryAction());
        binding.detailOverlay.setOnClickListener(v -> hideDetail());
        binding.btnOpenPublishPanel.setOnClickListener(v -> showPublishPanel());
        binding.btnConfirmPublish.setOnClickListener(v -> publishTask());
        binding.btnClosePublishPanel.setOnClickListener(v -> hidePublishPanel());
        binding.publishOverlay.setOnClickListener(v -> hidePublishPanel());
        binding.inputPublishDeadline.setOnClickListener(v -> openDeadlinePicker());
        binding.inputPublishDeadline.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                openDeadlinePicker();
            }
        });
        binding.inputPublishDeadline.setKeyListener(null);
        binding.btnQueryAcceptedInProgress.setOnClickListener(v -> {
            currentAcceptedQuery = QUERY_ACCEPTED_IN_PROGRESS;
            updateAcceptedQueryButtons();
            renderAcceptedTaskList();
        });
        binding.btnQueryAcceptedCompleted.setOnClickListener(v -> {
            currentAcceptedQuery = QUERY_ACCEPTED_PENDING;
            updateAcceptedQueryButtons();
            renderAcceptedTaskList();
        });
        binding.btnQueryAcceptedPending.setOnClickListener(v -> {
            currentAcceptedQuery = QUERY_ACCEPTED_COMPLETED;
            updateAcceptedQueryButtons();
            renderAcceptedTaskList();
        });
        binding.btnQueryAcceptedFailed.setOnClickListener(v -> {
            currentAcceptedQuery = QUERY_ACCEPTED_FAILED;
            updateAcceptedQueryButtons();
            renderAcceptedTaskList();
        });
        binding.btnQueryPublished.setOnClickListener(v -> {
            currentMineQuery = QUERY_PUBLISHED;
            currentMineStatusFilter = MINE_STATUS_ALL;
            setMineFiltersExpanded(false, false);
            updateMineQueryButtons();
            renderMineTaskList();
        });
        binding.mineFilterAllToggle.setOnClickListener(v -> {
            if (!QUERY_PUBLISHED.equals(currentMineQuery)) {
                return;
            }
            setMineFiltersExpanded(binding.mineExtraFilters.getVisibility() != View.VISIBLE, true);
            currentMineStatusFilter = MINE_STATUS_ALL;
            updateMineQueryButtons();
            renderMineTaskList();
        });
        binding.btnMineFilterAccepted.setOnClickListener(v -> {
            if (!QUERY_PUBLISHED.equals(currentMineQuery)) {
                return;
            }
            currentMineStatusFilter = MINE_STATUS_ACCEPTED;
            updateMineQueryButtons();
            renderMineTaskList();
        });
        binding.btnMineFilterCancelled.setOnClickListener(v -> {
            if (!QUERY_PUBLISHED.equals(currentMineQuery)) {
                return;
            }
            currentMineStatusFilter = MINE_STATUS_CANCELLED;
            updateMineQueryButtons();
            renderMineTaskList();
        });
        binding.btnMineFilterRejected.setOnClickListener(v -> {
            if (!QUERY_PUBLISHED.equals(currentMineQuery)) {
                return;
            }
            currentMineStatusFilter = MINE_STATUS_REJECTED;
            updateMineQueryButtons();
            renderMineTaskList();
        });
        binding.btnMineFilterCompleted.setOnClickListener(v -> {
            if (!QUERY_PUBLISHED.equals(currentMineQuery)) {
                return;
            }
            currentMineStatusFilter = MINE_STATUS_COMPLETED;
            updateMineQueryButtons();
            renderMineTaskList();
        });

        binding.inputTaskSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterHomeTasks();
            }
        });
    }

    private void logout() {
        authRepository.logout();
        updateAuthState();
        showMessage(getString(R.string.logout_success));
    }

    private void switchMode(boolean loginMode) {
        isLoginMode = loginMode;
        binding.layoutNickname.setVisibility(loginMode ? View.GONE : View.VISIBLE);
        binding.layoutConfirmPassword.setVisibility(loginMode ? View.GONE : View.VISIBLE);

        binding.tabLogin.setSelected(loginMode);
        binding.tabRegister.setSelected(!loginMode);

        setAuthTabState(binding.tabLogin, loginMode);
        setAuthTabState(binding.tabRegister, !loginMode);

        binding.tvTitle.setText(loginMode ? R.string.login_title : R.string.register_title);
        binding.tvSubtitle.setText(loginMode ? R.string.login_subtitle : R.string.register_subtitle);
        binding.btnSubmit.setText(loginMode ? R.string.action_login : R.string.action_register);
        binding.inputUsername.setText(null);
        binding.inputNickname.setText(null);
        binding.inputPassword.setText(null);
        binding.inputConfirmPassword.setText(null);
        binding.tvHint.setText(loginMode ? R.string.login_hint : R.string.register_hint);
    }

    private void setAuthTabState(TextView tabView, boolean selected) {
        tabView.setBackgroundResource(selected ? R.drawable.bg_auth_tab_selected : R.drawable.bg_auth_tab_unselected);
        tabView.setTextColor(getColor(selected ? R.color.auth_tab_selected_text : R.color.auth_tab_unselected_text));
    }

    private void setNavState(LinearLayout tabView, boolean selected) {
        tabView.setBackgroundResource(selected ? R.drawable.bg_nav_selected : R.drawable.bg_nav_unselected);
        TextView label = (TextView) tabView.getChildAt(0);
        label.setTextColor(getColor(selected ? R.color.dashboard_title : R.color.dashboard_subtitle));
    }

    private void submitAuth() {
        String username = getInput(binding.inputUsername.getText() == null ? null : binding.inputUsername.getText().toString());
        String nickname = getInput(binding.inputNickname.getText() == null ? null : binding.inputNickname.getText().toString());
        String password = getInput(binding.inputPassword.getText() == null ? null : binding.inputPassword.getText().toString());
        String confirmPassword = getInput(binding.inputConfirmPassword.getText() == null ? null : binding.inputConfirmPassword.getText().toString());

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showMessage(getString(R.string.error_empty_login));
            return;
        }

        if (!isLoginMode) {
            if (TextUtils.isEmpty(nickname)) {
                showMessage(getString(R.string.error_empty_nickname));
                return;
            }
            if (!password.equals(confirmPassword)) {
                showMessage(getString(R.string.error_password_not_match));
                return;
            }
        }

        setLoading(true);
        if (isLoginMode) {
            authRepository.login(username, password, new AuthRepository.AuthResultCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        updateAuthState();
                        showMessage(message);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showMessage(message);
                    });
                }
            });
        } else {
            authRepository.register(username, nickname, password, new AuthRepository.AuthResultCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showMessage(message);
                        switchMode(true);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showMessage(message);
                    });
                }
            });
        }
    }

    private void updateAuthState() {
        boolean loggedIn = sessionManager.isLoggedIn();
        binding.authContainer.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        binding.dashboardContainer.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        binding.bottomNavBar.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        hideDetail();
        hidePublishPanel();
        hideSideMenu();
        hideNotificationPanel();

        if (loggedIn) {
            loadPersistedNotificationState();
            String displayName = sessionManager.getDisplayName();
            long userId = sessionManager.getUserId();
            mineRepository.ensureWalletSeed(userId);
            binding.cardSession.setVisibility(View.VISIBLE);
            binding.tvSessionUser.setText(getString(R.string.current_user_format, displayName));
            binding.tvDashboardGreeting.setText(getString(R.string.dashboard_greeting_format, displayName));
            binding.tvDrawerUserName.setText(displayName);
            notifications.clear();
            notificationIds.clear();
            lastAcceptedNotificationKeys.clear();
            lastMineNotificationKeys.clear();
            renderNotifications();
            renderUserAvatar();
            renderDashboardLists();
            renderWalletInfo();
            renderMineTaskList();
            selectDashboardPage(binding.navHome);
            fetchMessages();
        } else {
            binding.cardSession.setVisibility(View.GONE);
            notifications.clear();
            notificationIds.clear();
            readNotificationIds.clear();
            deletedNotificationIds.clear();
            selectedNotificationIds.clear();
            isNotificationSelectionMode = false;
            lastAcceptedNotificationKeys.clear();
            lastMineNotificationKeys.clear();
            disconnectMessageWebSocket();
            renderNotifications();
            clearDashboardLists();
        }
    }

    private void renderUserAvatar() {
        String avatarUrl = sessionManager.getAvatarUrl();
        if (TextUtils.isEmpty(avatarUrl)) {
            binding.ivUserAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            binding.ivDrawerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.ivUserAvatar);
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.ivDrawerAvatar);
    }

    private void renderDashboardLists() {
        clearDashboardLists();
        filterHomeTasks();
        renderAcceptedTaskList();
        renderMineTaskList();
    }

    private void renderWalletInfo() {
        Wallet wallet = mineRepository.getWallet(sessionManager.getUserId());
        if (wallet != null) {
            binding.tvWalletBalance.setText(getString(R.string.wallet_balance_format, wallet.getBalance()));
        }
    }

    private void renderMineTaskList() {
        binding.publishedTaskList.removeAllViews();
        List<MineTaskRecord> records = mineRepository.getPublishedTasks(sessionManager.getUserId());

        syncMineTaskNotifications(records);
        int visibleCount = 0;
        for (MineTaskRecord record : records) {
            if (!matchesMineStatusFilter(record)) {
                continue;
            }
            binding.publishedTaskList.addView(createMineRecordCard(record));
            visibleCount++;
        }
        if (visibleCount == 0) {
            TextView emptyView = new TextView(this);
            emptyView.setText(getMineEmptyText());
            emptyView.setTextColor(getColor(R.color.dashboard_muted));
            emptyView.setTextSize(14);
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            emptyParams.topMargin = dp(18);
            emptyView.setLayoutParams(emptyParams);
            binding.publishedTaskList.addView(emptyView);
        }
    }

    private void showPublishPanel() {
        binding.inputPublishTitle.setText(null);
        binding.inputPublishDescription.setText(null);
        binding.inputPublishAmount.setText(null);
        binding.inputPublishDeadline.setText(getDefaultDeadline());
        binding.publishOverlay.setVisibility(View.VISIBLE);
    }

    private void hidePublishPanel() {
        binding.publishOverlay.setVisibility(View.GONE);
    }

    private void showSideMenu() {
        hideNotificationPanel();
        binding.sideMenuOverlay.setVisibility(View.VISIBLE);
    }

    private void hideSideMenu() {
        binding.sideMenuOverlay.setVisibility(View.GONE);
    }

    private void toggleSideMenu() {
        if (binding.sideMenuOverlay.getVisibility() == View.VISIBLE) {
            hideSideMenu();
        } else {
            showSideMenu();
        }
    }

    private void showNotificationPanel() {
        hideSideMenu();
        if (sessionManager.isLoggedIn()) {
            fetchMessages();
        }
        binding.notificationOverlay.setVisibility(View.VISIBLE);
    }

    private void hideNotificationPanel() {
        exitNotificationSelectionMode();
        binding.notificationOverlay.setVisibility(View.GONE);
    }

    private void toggleNotificationPanel() {
        if (binding.notificationOverlay.getVisibility() == View.VISIBLE) {
            hideNotificationPanel();
        } else {
            showNotificationPanel();
        }
    }

    private void renderNotifications() {
        binding.notificationList.removeAllViews();
        List<AppNotification> visibleNotifications = getFilteredNotifications();
        if (visibleNotifications.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(getNotificationEmptyText());
            emptyView.setTextColor(getColor(R.color.notification_body));
            emptyView.setTextSize(14);
            emptyView.setPadding(0, dp(12), 0, 0);
            binding.notificationList.addView(emptyView);
        } else {
            for (AppNotification notification : visibleNotifications) {
                binding.notificationList.addView(createNotificationCard(notification));
            }
        }
        binding.btnMarkAllNotificationsRead.setEnabled(getUnreadNotificationCount() > 0 && !isNotificationSelectionMode);
        binding.notificationSelectionBar.setVisibility(isNotificationSelectionMode ? View.VISIBLE : View.GONE);
        updateNotificationBadge();
    }

    private void enterNotificationSelectionMode(long notificationId) {
        isNotificationSelectionMode = true;
        selectedNotificationIds.clear();
        selectedNotificationIds.add(notificationId);
        renderNotifications();
    }

    private void exitNotificationSelectionMode() {
        if (!isNotificationSelectionMode && selectedNotificationIds.isEmpty()) {
            return;
        }
        isNotificationSelectionMode = false;
        selectedNotificationIds.clear();
        renderNotifications();
    }

    private void toggleNotificationSelection(AppNotification notification) {
        if (selectedNotificationIds.contains(notification.id)) {
            selectedNotificationIds.remove(notification.id);
        } else {
            selectedNotificationIds.add(notification.id);
        }
        if (selectedNotificationIds.isEmpty()) {
            isNotificationSelectionMode = false;
        }
        renderNotifications();
    }

    private void deleteSelectedNotifications() {
        if (selectedNotificationIds.isEmpty()) {
            return;
        }
        notifications.removeIf(notification -> selectedNotificationIds.contains(notification.id));
        notificationIds.removeAll(selectedNotificationIds);
        readNotificationIds.removeAll(selectedNotificationIds);
        persistReadNotificationIds();
        persistDeletedNotificationIds(selectedNotificationIds);
        selectedNotificationIds.clear();
        isNotificationSelectionMode = false;
        renderNotifications();
    }

    private void markAllNotificationsRead() {
        boolean changed = false;
        for (AppNotification notification : notifications) {
            if (!notification.isRead) {
                notification.isRead = true;
                readNotificationIds.add(notification.id);
                changed = true;
            }
        }
        if (changed) {
            persistReadNotificationIds();
            renderNotifications();
        }
    }

    private void fetchMessages() {
        if (!sessionManager.isLoggedIn()) {
            return;
        }
        new Thread(() -> {
            List<MessageItem> items = messageRepository.getMessages(sessionManager.getUserId());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                int previousUnreadCount = getUnreadNotificationCount();
                prunePersistedNotificationState(items);
                notifications.clear();
                notificationIds.clear();
                for (MessageItem item : items) {
                    addNotificationFromMessage(item, false);
                }
                requestNotificationPermissionIfNeeded(previousUnreadCount);
                renderNotifications();
                connectMessageWebSocketIfNeeded();
            });
        }).start();
    }

    private void requestNotificationPermissionIfNeeded(int previousUnreadCount) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (hasRequestedNotificationPermission) {
            return;
        }
        if (getUnreadNotificationCount() <= previousUnreadCount) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            hasRequestedNotificationPermission = true;
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                SYSTEM_NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.notification_channel_description));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void showSystemNotification(AppNotification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_OPEN_NOTIFICATION_PANEL, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) (notification.id % Integer.MAX_VALUE),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SYSTEM_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat.from(this).notify((int) notification.id, builder.build());
    }

    private void handleNotificationPanelIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_NOTIFICATION_PANEL, false)) {
            return;
        }
        if (!sessionManager.isLoggedIn()) {
            return;
        }
        intent.removeExtra(EXTRA_OPEN_NOTIFICATION_PANEL);
        selectDashboardPage(binding.navHome);
        showNotificationPanel();
    }

    private void connectMessageWebSocketIfNeeded() {
        if (!sessionManager.isLoggedIn() || messageWebSocket != null) {
            return;
        }
        String baseUrl = com.example.messageinbottle.BuildConfig.API_BASE_URL;
        String wsUrl = baseUrl.replaceFirst("^http://", "ws://").replaceFirst("^https://", "wss://")
                + "/ws/messages?userId=" + sessionManager.getUserId();
        Request request = new Request.Builder().url(wsUrl).build();
        messageWebSocket = NetworkClient.getInstance().webSocketClient().newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                MessageEnvelope envelope = NetworkClient.getInstance().gson().fromJson(text, MessageEnvelope.class);
                if (envelope == null || envelope.getMessage() == null) {
                    return;
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    int previousUnreadCount = getUnreadNotificationCount();
                    addNotificationFromMessage(envelope.getMessage(), true);
                    requestNotificationPermissionIfNeeded(previousUnreadCount);
                    renderNotifications();
                });
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                if (messageWebSocket == webSocket) {
                    messageWebSocket = null;
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                if (messageWebSocket == webSocket) {
                    messageWebSocket = null;
                }
            }
        });
    }

    private void disconnectMessageWebSocket() {
        if (messageWebSocket != null) {
            messageWebSocket.close(1000, "pause");
            messageWebSocket = null;
        }
    }

    private void loadPersistedNotificationState() {
        readNotificationIds.clear();
        deletedNotificationIds.clear();
        readNotificationIds.addAll(getPersistedIdSet(NOTIFICATION_READ_PREFIX));
        deletedNotificationIds.addAll(getPersistedIdSet(NOTIFICATION_DELETED_PREFIX));
    }

    private void persistReadNotificationIds() {
        putPersistedIdSet(NOTIFICATION_READ_PREFIX, readNotificationIds);
    }

    private void persistDeletedNotificationIds(Set<Long> idsToDelete) {
        deletedNotificationIds.addAll(idsToDelete);
        putPersistedIdSet(NOTIFICATION_DELETED_PREFIX, deletedNotificationIds);
    }

    private Set<Long> getPersistedIdSet(String keyPrefix) {
        String rawValue = notificationPreferences.getString(keyPrefix + sessionManager.getUserId(), "");
        Set<Long> ids = new HashSet<>();
        if (TextUtils.isEmpty(rawValue)) {
            return ids;
        }
        String[] values = rawValue.split(",");
        for (String value : values) {
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private void putPersistedIdSet(String keyPrefix, Set<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(id);
        }
        notificationPreferences.edit()
                .putString(keyPrefix + sessionManager.getUserId(), builder.toString())
                .apply();
    }

    private void prunePersistedNotificationState(List<MessageItem> items) {
        Set<Long> validIds = new HashSet<>();
        for (MessageItem item : items) {
            if (item != null) {
                validIds.add(item.getId());
            }
        }

        if (readNotificationIds.retainAll(validIds)) {
            persistReadNotificationIds();
        }
        if (deletedNotificationIds.retainAll(validIds)) {
            putPersistedIdSet(NOTIFICATION_DELETED_PREFIX, deletedNotificationIds);
        }
    }

    private void addNotificationFromMessage(MessageItem item, boolean shouldPushSystemNotification) {
        if (item == null || notificationIds.contains(item.getId()) || isNotificationDeleted(item.getId())) {
            return;
        }
        notificationIds.add(item.getId());
        String title = TextUtils.isEmpty(item.getTitle()) ? getNotificationTitle(item.getType()) : item.getTitle();
        AppNotification notification = new AppNotification(
                item.getId(),
                title,
                item.getContent(),
                formatNotificationTime(item.getCreatedAt()),
                readNotificationIds.contains(item.getId())
        );
        notifications.add(notification);
        notifications.sort((left, right) -> Long.compare(right.id, left.id));
        if (shouldPushSystemNotification && !notification.isRead) {
            showSystemNotification(notification);
        }
    }

    private boolean isNotificationDeleted(long notificationId) {
        return deletedNotificationIds.contains(notificationId);
    }

    private View createNotificationCard(AppNotification notification) {
        boolean isSelected = selectedNotificationIds.contains(notification.id);

        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        container.setLayoutParams(params);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        container.setBackgroundResource(!notification.isRead ? R.drawable.bg_nav_selected : R.drawable.bg_task_pill);
        container.setOnClickListener(v -> {
            if (isNotificationSelectionMode) {
                toggleNotificationSelection(notification);
            } else {
                openNotificationDetail(notification);
            }
        });
        container.setOnLongClickListener(v -> {
            if (!isNotificationSelectionMode) {
                enterNotificationSelectionMode(notification.id);
            } else {
                toggleNotificationSelection(notification);
            }
            return true;
        });

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleView.setLayoutParams(titleParams);
        titleView.setText(notification.title);
        titleView.setTextColor(getColor(R.color.notification_title));
        titleView.setTextSize(15);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);

        titleRow.addView(titleView);
        if (isNotificationSelectionMode) {
            titleRow.addView(createSelectionIndicator(isSelected));
        } else if (!notification.isRead) {
            titleRow.addView(createUnreadIndicator());
        }

        TextView timeView = new TextView(this);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.topMargin = dp(6);
        timeView.setLayoutParams(timeParams);
        timeView.setText(notification.timeText);
        timeView.setTextColor(getColor(R.color.dashboard_muted));
        timeView.setTextSize(12);

        TextView bodyView = new TextView(this);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bodyParams.topMargin = dp(8);
        bodyView.setLayoutParams(bodyParams);
        bodyView.setText(notification.message);
        bodyView.setTextColor(getColor(R.color.notification_body));
        bodyView.setTextSize(13);
        bodyView.setMaxLines(isNotificationSelectionMode ? 2 : 3);

        TextView stateView = new TextView(this);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        stateParams.topMargin = dp(10);
        stateView.setLayoutParams(stateParams);
        stateView.setText(notification.isRead ? getString(R.string.notification_filter_read) : getString(R.string.notification_filter_unread));
        stateView.setTextColor(getColor(notification.isRead ? R.color.dashboard_subtitle : R.color.dashboard_title));
        stateView.setTextSize(12);
        stateView.setTypeface(stateView.getTypeface(), Typeface.BOLD);

        container.addView(titleRow);
        container.addView(timeView);
        container.addView(bodyView);
        container.addView(stateView);
        return container;
    }

    private View createSelectionIndicator(boolean selected) {
        LinearLayout indicatorLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                dp(22),
                dp(22)
        );
        layoutParams.leftMargin = dp(10);
        indicatorLayout.setLayoutParams(layoutParams);
        indicatorLayout.setGravity(Gravity.CENTER);

        GradientDrawable boxDrawable = new GradientDrawable();
        boxDrawable.setShape(GradientDrawable.RECTANGLE);
        boxDrawable.setCornerRadius(dp(6));
        boxDrawable.setStroke(dp(2), getColor(selected ? R.color.auth_accent : R.color.dashboard_muted));
        boxDrawable.setColor(getColor(selected ? R.color.nav_selected : android.R.color.transparent));
        indicatorLayout.setBackground(boxDrawable);

        if (selected) {
            TextView checkView = new TextView(this);
            checkView.setText("✓");
            checkView.setTextSize(14);
            checkView.setTypeface(checkView.getTypeface(), Typeface.BOLD);
            checkView.setTextColor(getColor(R.color.dashboard_title));
            indicatorLayout.addView(checkView);
        }
        return indicatorLayout;
    }

    private View createUnreadIndicator() {
        View dotView = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(12), dp(12));
        dotParams.leftMargin = dp(10);
        dotView.setLayoutParams(dotParams);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(getColor(android.R.color.holo_red_light));
        drawable.setStroke(dp(2), getColor(android.R.color.white));
        dotView.setBackground(drawable);
        return dotView;
    }

    private void updateNotificationBadge() {
        int count = getUnreadNotificationCount();
        binding.tvNotificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        binding.tvNotificationBadge.setText(String.valueOf(Math.min(count, 99)));
    }

    private List<AppNotification> getFilteredNotifications() {
        List<AppNotification> filtered = new ArrayList<>();
        for (AppNotification notification : notifications) {
            if (QUERY_NOTIFICATION_READ.equals(currentNotificationQuery) && !notification.isRead) {
                continue;
            }
            if (QUERY_NOTIFICATION_UNREAD.equals(currentNotificationQuery) && notification.isRead) {
                continue;
            }
            filtered.add(notification);
        }
        return filtered;
    }

    private String getNotificationEmptyText() {
        if (QUERY_NOTIFICATION_READ.equals(currentNotificationQuery)) {
            return getString(R.string.notification_empty_read);
        }
        if (QUERY_NOTIFICATION_UNREAD.equals(currentNotificationQuery)) {
            return getString(R.string.notification_empty_unread);
        }
        return getString(R.string.notification_empty);
    }

    private int getUnreadNotificationCount() {
        int count = 0;
        for (AppNotification notification : notifications) {
            if (!notification.isRead) {
                count++;
            }
        }
        return count;
    }

    private void updateNotificationQueryButtons() {
        updateMineQueryButton(binding.btnQueryNotificationAll, QUERY_NOTIFICATION_ALL.equals(currentNotificationQuery));
        updateMineQueryButton(binding.btnQueryNotificationRead, QUERY_NOTIFICATION_READ.equals(currentNotificationQuery));
        updateMineQueryButton(binding.btnQueryNotificationUnread, QUERY_NOTIFICATION_UNREAD.equals(currentNotificationQuery));
    }

    private void openNotificationDetail(AppNotification notification) {
        if (!notification.isRead) {
            notification.isRead = true;
            readNotificationIds.add(notification.id);
            persistReadNotificationIds();
            renderNotifications();
        }

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundResource(R.drawable.bg_detail_overlay);

        TextView tagView = new TextView(this);
        tagView.setText(getString(R.string.notification_detail_title));
        tagView.setTextSize(12);
        tagView.setTypeface(tagView.getTypeface(), Typeface.BOLD);
        tagView.setTextColor(getColor(R.color.dashboard_title));
        tagView.setBackgroundResource(R.drawable.bg_task_pill);
        tagView.setPadding(dp(12), dp(6), dp(12), dp(6));

        TextView titleView = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(16);
        titleView.setLayoutParams(titleParams);
        titleView.setText(TextUtils.isEmpty(notification.title) ? getString(R.string.notification_detail_title) : notification.title);
        titleView.setTextSize(22);
        titleView.setTextColor(getColor(android.R.color.white));
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);

        TextView timeView = new TextView(this);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.topMargin = dp(10);
        timeView.setLayoutParams(timeParams);
        timeView.setText(getString(R.string.notification_detail_time_format, notification.timeText));
        timeView.setTextSize(13);
        timeView.setTextColor(0xD8FFFFFF);

        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        dividerParams.topMargin = dp(16);
        dividerParams.bottomMargin = dp(16);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(0x33FFFFFF);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollView.setLayoutParams(scrollParams);
        scrollView.setFillViewport(true);

        TextView messageView = new TextView(this);
        messageView.setText(notification.message);
        messageView.setTextSize(15);
        messageView.setLineSpacing(dp(4), 1f);
        messageView.setTextColor(0xFFEDEDED);
        scrollView.addView(messageView);

        MaterialButton closeButton = new MaterialButton(this);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        closeParams.topMargin = dp(22);
        closeButton.setLayoutParams(closeParams);
        closeButton.setText("我知道了");
        closeButton.setCornerRadius(dp(16));
        closeButton.setBackgroundTintList(getColorStateList(R.color.auth_accent));
        closeButton.setOnClickListener(v -> dialog.dismiss());

        root.addView(tagView);
        root.addView(titleView);
        root.addView(timeView);
        root.addView(divider);
        root.addView(scrollView);
        root.addView(closeButton);

        dialog.setView(root);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private String formatNotificationTime(long createdAt) {
        if (createdAt <= 0L) {
            return "--";
        }
        long timestamp = createdAt < 1000000000000L ? createdAt * 1000L : createdAt;
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return formatter.format(new java.util.Date(timestamp));
    }

    private String getNotificationTitle(String type) {
        if ("publish_success".equals(type)) {
            return "发布提醒";
        }
        if ("task_accepted".equals(type)) {
            return "任务接取提醒";
        }
        if ("task_pending_review".equals(type)) {
            return "任务待审核提醒";
        }
        if ("task_completed".equals(type)) {
            return "任务完成提醒";
        }
        if ("task_rejected_publisher".equals(type)) {
            return "审核驳回";
        }
        if ("task_rejected_accepter".equals(type)) {
            return "审核驳回";
        }
        if ("task_timeout_publisher".equals(type)) {
            return "任务超时关闭";
        }
        if ("task_timeout_accepter".equals(type)) {
            return "任务已失败";
        }
        if ("task_accept_cancelled_publisher".equals(type)) {
            return "任务已取消";
        }
        if ("task_accept_cancelled_accepter".equals(type)) {
            return "取消接取";
        }
        if ("task_cancel_failed".equals(type)) {
            return "任务完成失败";
        }
        if ("accept_success".equals(type)) {
            return "接取成功";
        }
        if ("reward_granted".equals(type)) {
            return "审核通过";
        }
        return "消息通知";
    }

    private void syncAcceptedTaskNotifications(List<AcceptedTask> acceptedTasks) {
        List<String> latestKeys = new ArrayList<>();
        for (AcceptedTask task : acceptedTasks) {
            if (task.isCompleted() && "审核通过".equals(task.getReviewStatus())) {
                String key = task.getId() + "-" + task.getReviewStatus() + "-reward";
                latestKeys.add(key);
            }
        }
        lastAcceptedNotificationKeys.clear();
        lastAcceptedNotificationKeys.addAll(latestKeys);
    }

    private void syncMineTaskNotifications(List<MineTaskRecord> records) {
        List<String> latestKeys = new ArrayList<>();
        for (MineTaskRecord record : records) {
            if ("已接取".equals(record.getType())) {
                latestKeys.add(record.getId() + "-accepted");
            } else if ("已完成".equals(record.getType()) || "审核通过".equals(record.getType())) {
                latestKeys.add(record.getId() + "-completed");
            }
        }
        lastMineNotificationKeys.clear();
        lastMineNotificationKeys.addAll(latestKeys);
    }

    private void pickAvatarImage() {
        if (isUploadingAvatar) {
            return;
        }
        avatarPickerLauncher.launch("image/*");
    }

    private void uploadAvatar() {
        if (pendingAvatarImageFile == null || isUploadingAvatar) {
            return;
        }
        isUploadingAvatar = true;
        binding.btnUploadAvatar.setEnabled(false);
        binding.btnUploadAvatar.setText(R.string.avatar_uploading);

        authRepository.uploadAvatar(sessionManager.getUserId(), pendingAvatarImageFile, new AuthRepository.AvatarUploadCallback() {
            @Override
            public void onSuccess(String avatarUrl, String message) {
                runOnUiThread(() -> {
                    isUploadingAvatar = false;
                    pendingAvatarImageFile = null;
                    binding.btnUploadAvatar.setEnabled(true);
                    binding.btnUploadAvatar.setText("上传头像");
                    renderUserAvatar();
                    showMessage(TextUtils.isEmpty(message) ? getString(R.string.avatar_upload_success) : message);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isUploadingAvatar = false;
                    pendingAvatarImageFile = null;
                    binding.btnUploadAvatar.setEnabled(true);
                    binding.btnUploadAvatar.setText("上传头像");
                    showMessage(TextUtils.isEmpty(message) ? getString(R.string.avatar_upload_failed) : message);
                });
            }
        });
    }

    private void publishTask() {
        String title = getInput(binding.inputPublishTitle.getText() == null ? null : binding.inputPublishTitle.getText().toString());
        String description = getInput(binding.inputPublishDescription.getText() == null ? null : binding.inputPublishDescription.getText().toString());
        String amountText = getInput(binding.inputPublishAmount.getText() == null ? null : binding.inputPublishAmount.getText().toString());
        String deadline = getInput(binding.inputPublishDeadline.getText() == null ? null : binding.inputPublishDeadline.getText().toString());

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(amountText) || TextUtils.isEmpty(deadline)) {
            showMessage(getString(R.string.error_empty_publish));
            return;
        }

        double amount = Double.parseDouble(amountText);
        MineRepository.ActionResult result = mineRepository.publishTask(sessionManager.getUserId(), title, description, amount, deadline);
        if (!result.isSuccess()) {
            showMessage(TextUtils.isEmpty(result.getMessage()) ? getString(R.string.error_network) : result.getMessage());
            return;
        }
        binding.inputPublishTitle.setText(null);
        binding.inputPublishDescription.setText(null);
        binding.inputPublishAmount.setText(null);
        binding.inputPublishDeadline.setText(null);
        hidePublishPanel();
        currentMineQuery = QUERY_PUBLISHED;
        updateMineQueryButtons();
        renderMineTaskList();
        renderWalletInfo();
        filterHomeTasks();
        showMessage(TextUtils.isEmpty(result.getMessage()) ? getString(R.string.publish_success) : result.getMessage());
    }

    private String getDefaultDeadline() {
        return formatDeadline(LocalDateTime.now().plusDays(1).withHour(23).withMinute(59));
    }

    private void openDeadlinePicker() {
        LocalDateTime initial = parseDeadlineOrDefault(getInput(binding.inputPublishDeadline.getText() == null ? null : binding.inputPublishDeadline.getText().toString()));
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> openTimePicker(year, month, dayOfMonth, initial.getHour(), initial.getMinute()),
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth()
        );
        datePickerDialog.show();
    }

    private void openTimePicker(int year, int month, int dayOfMonth, int initialHour, int initialMinute) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    LocalDateTime selected = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute);
                    binding.inputPublishDeadline.setText(formatDeadline(selected));
                },
                initialHour,
                initialMinute,
                true
        );
        timePickerDialog.show();
    }

    private LocalDateTime parseDeadlineOrDefault(String input) {
        if (TextUtils.isEmpty(input)) {
            return LocalDateTime.now().plusDays(1).withHour(23).withMinute(59);
        }
        String value = input.trim();
        try {
            if (value.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}")) {
                String[] parts = value.split("\\s+");
                String[] date = parts[0].split("-");
                String[] time = parts[1].split(":");
                return LocalDateTime.of(
                        Integer.parseInt(date[0]),
                        Integer.parseInt(date[1]),
                        Integer.parseInt(date[2]),
                        Integer.parseInt(time[0]),
                        Integer.parseInt(time[1])
                );
            }
        } catch (Exception ignored) {
        }
        return LocalDateTime.now().plusDays(1).withHour(23).withMinute(59);
    }

    private String formatDeadline(LocalDateTime dateTime) {
        return String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d %02d:%02d",
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute()
        );
    }

    private void renderAcceptedTaskList() {
        binding.acceptedTaskList.removeAllViews();
        List<AcceptedTask> acceptedTasks = stageTwoRepository.getAcceptedTasks(sessionManager.getUserId());
        syncAcceptedTaskNotifications(acceptedTasks);
        int visibleCount = 0;
        for (AcceptedTask task : acceptedTasks) {
            if (!matchesAcceptedQuery(task, currentAcceptedQuery)) {
                continue;
            }
            binding.acceptedTaskList.addView(createAcceptedTaskCard(task));
            visibleCount++;
        }
        if (visibleCount == 0) {
            TextView emptyView = new TextView(this);
            emptyView.setText(getAcceptedEmptyText());
            emptyView.setTextColor(getColor(R.color.dashboard_muted));
            emptyView.setTextSize(14);
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            emptyParams.topMargin = dp(18);
            emptyView.setLayoutParams(emptyParams);
            binding.acceptedTaskList.addView(emptyView);
        }
    }

    private boolean matchesAcceptedQuery(AcceptedTask task, String query) {
        if (QUERY_ACCEPTED_PENDING.equals(query)) {
            return "待审核".equals(task.getReviewStatus());
        }
        if (QUERY_ACCEPTED_COMPLETED.equals(query)) {
            return "审核通过".equals(task.getReviewStatus());
        }
        if (QUERY_ACCEPTED_FAILED.equals(query)) {
            return isAcceptedTaskFailed(task);
        }
        return "进行中".equals(task.getReviewStatus());
    }

    private boolean isAcceptedTaskFailed(AcceptedTask task) {
        String status = task.getReviewStatus();
        return "驳回".equals(status)
                || "已超时关闭".equals(status)
                || "已取消接取".equals(status);
    }

    private String getAcceptedEmptyText() {
        if (QUERY_ACCEPTED_PENDING.equals(currentAcceptedQuery)) {
            return "暂无待审核任务";
        }
        if (QUERY_ACCEPTED_COMPLETED.equals(currentAcceptedQuery)) {
            return "暂无已完成任务";
        }
        if (QUERY_ACCEPTED_FAILED.equals(currentAcceptedQuery)) {
            return "暂无完成失败任务";
        }
        return "暂无进行中任务";
    }

    private void startAcceptedTaskAutoRefresh() {
        acceptedTaskRefreshHandler.removeCallbacks(acceptedTaskRefreshRunnable);
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            acceptedTaskRefreshHandler.post(acceptedTaskRefreshRunnable);
        }
    }

    private void stopAcceptedTaskAutoRefresh() {
        acceptedTaskRefreshHandler.removeCallbacks(acceptedTaskRefreshRunnable);
    }

    private void refreshAcceptedTasksIfVisible() {
        if (binding.pageAccepted.getVisibility() == View.VISIBLE) {
            renderAcceptedTaskList();
        }
    }

    private void filterHomeTasks() {
        if (binding.homeTaskList == null) {
            return;
        }

        binding.homeTaskList.removeAllViews();
        String keyword = getInput(binding.inputTaskSearch.getText() == null ? null : binding.inputTaskSearch.getText().toString());
        List<HomeTask> tasks = stageTwoRepository.searchHomeTasks(keyword, selectedCategory);
        long currentUserId = sessionManager.getUserId();

        if (tasks.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(R.string.task_result_empty);
            emptyView.setTextColor(getColor(R.color.dashboard_muted));
            emptyView.setTextSize(14);
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            emptyParams.topMargin = dp(18);
            emptyView.setLayoutParams(emptyParams);
            binding.homeTaskList.addView(emptyView);
        } else {
            int visibleCount = 0;
            for (HomeTask task : tasks) {
                if (task.getPublisherId() == currentUserId) {
                    continue;
                }
                binding.homeTaskList.addView(createHomeTaskCard(task));
                visibleCount++;
            }
            if (visibleCount == 0) {
                TextView emptyView = new TextView(this);
                emptyView.setText(R.string.task_result_empty);
                emptyView.setTextColor(getColor(R.color.dashboard_muted));
                emptyView.setTextSize(14);
                LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                emptyParams.topMargin = dp(18);
                emptyView.setLayoutParams(emptyParams);
                binding.homeTaskList.addView(emptyView);
            }
        }

        int displayCount = 0;
        for (HomeTask task : tasks) {
            if (task.getPublisherId() != currentUserId) {
                displayCount++;
            }
        }
        binding.tvTaskResultHint.setText(getString(R.string.task_result_count_format, displayCount));
    }

    private void renderCategoryChips() {
        binding.categoryChipGroup.removeAllViews();
        List<String> categories = stageTwoRepository.getHomeCategories();
        for (String category : categories) {
            TextView chip = new TextView(this);
            chip.setText(category);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), dp(10), dp(16), dp(10));
            chip.setTextSize(14);
            chip.setTypeface(chip.getTypeface(), Typeface.BOLD);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.rightMargin = dp(10);
            chip.setLayoutParams(params);

            updateCategoryChipStyle(chip, category.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                refreshCategoryChipStyles();
                filterHomeTasks();
            });
            binding.categoryChipGroup.addView(chip);
        }
    }

    private void refreshCategoryChipStyles() {
        for (int i = 0; i < binding.categoryChipGroup.getChildCount(); i++) {
            View child = binding.categoryChipGroup.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                updateCategoryChipStyle(chip, chip.getText().toString().equals(selectedCategory));
            }
        }
    }

    private void updateAcceptedQueryButtons() {
        updateMineQueryButton(binding.btnQueryAcceptedInProgress, QUERY_ACCEPTED_IN_PROGRESS.equals(currentAcceptedQuery));
        updateMineQueryButton(binding.btnQueryAcceptedCompleted, QUERY_ACCEPTED_PENDING.equals(currentAcceptedQuery));
        updateMineQueryButton(binding.btnQueryAcceptedPending, QUERY_ACCEPTED_COMPLETED.equals(currentAcceptedQuery));
        updateMineQueryButton(binding.btnQueryAcceptedFailed, QUERY_ACCEPTED_FAILED.equals(currentAcceptedQuery));
    }

    private void updateMineQueryButtons() {
        boolean publishedSelected = QUERY_PUBLISHED.equals(currentMineQuery);
        boolean extraFiltersVisible = binding != null
                && binding.mineExtraFilters != null
                && binding.mineExtraFilters.getVisibility() == View.VISIBLE;
        updateMineQueryButton(binding.btnQueryPublished, publishedSelected);
        updateMineFilterAllToggleStyle(publishedSelected);
        updateMineQueryButton(binding.btnMineFilterAccepted, publishedSelected && extraFiltersVisible && MINE_STATUS_ACCEPTED.equals(currentMineStatusFilter));
        updateMineQueryButton(binding.btnMineFilterCancelled, publishedSelected && extraFiltersVisible && MINE_STATUS_CANCELLED.equals(currentMineStatusFilter));
        updateMineQueryButton(binding.btnMineFilterRejected, publishedSelected && extraFiltersVisible && MINE_STATUS_REJECTED.equals(currentMineStatusFilter));
        updateMineQueryButton(binding.btnMineFilterCompleted, publishedSelected && extraFiltersVisible && MINE_STATUS_COMPLETED.equals(currentMineStatusFilter));
    }

    private void updateMineFilterAllToggleStyle(boolean selected) {
        binding.mineFilterAllToggle.setBackgroundResource(selected ? R.drawable.bg_nav_selected : R.drawable.bg_task_pill);
        binding.btnMineFilterAll.setTextColor(getColor(selected ? R.color.dashboard_title : R.color.dashboard_subtitle));
        binding.ivMineFilterArrow.setColorFilter(getColor(selected ? R.color.dashboard_title : R.color.dashboard_subtitle));
    }

    private void setMineFiltersExpanded(boolean expanded, boolean animate) {
        binding.ivMineFilterArrow.animate()
                .rotation(expanded ? 180f : 0f)
                .setDuration(180)
                .start();

        if (!animate) {
            binding.mineExtraFilters.animate().cancel();
            binding.mineExtraFilters.setVisibility(expanded ? View.VISIBLE : View.GONE);
            binding.mineExtraFilters.setAlpha(expanded ? 1f : 0f);
            binding.mineExtraFilters.setTranslationX(expanded ? 0f : -dp(8));
            return;
        }

        binding.mineExtraFilters.animate().cancel();
        if (expanded) {
            binding.mineExtraFilters.setVisibility(View.VISIBLE);
            binding.mineExtraFilters.setAlpha(0f);
            binding.mineExtraFilters.setTranslationX(-dp(8));
            binding.mineExtraFilters.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(180)
                    .start();
        } else {
            binding.mineExtraFilters.animate()
                    .alpha(0f)
                    .translationX(-dp(8))
                    .setDuration(180)
                    .withEndAction(() -> binding.mineExtraFilters.setVisibility(View.GONE))
                    .start();
        }
    }

    private boolean matchesMineStatusFilter(MineTaskRecord record) {
        if (!QUERY_PUBLISHED.equals(currentMineQuery)) {
            return false;
        }
        if (MINE_STATUS_ALL.equals(currentMineStatusFilter)) {
            return true;
        }
        if (MINE_STATUS_ACCEPTED.equals(currentMineStatusFilter)) {
            return "已接取".equals(record.getType());
        }
        if (MINE_STATUS_CANCELLED.equals(currentMineStatusFilter)) {
            return "已取消".equals(record.getType()) || "已取消发布".equals(record.getType());
        }
        if (MINE_STATUS_REJECTED.equals(currentMineStatusFilter)) {
            return "驳回".equals(record.getType()) || "审核驳回".equals(record.getType());
        }
        if (MINE_STATUS_COMPLETED.equals(currentMineStatusFilter)) {
            return "已完成".equals(record.getType()) || "审核通过".equals(record.getType());
        }
        return true;
    }

    private String getMineEmptyText() {
        if (MINE_STATUS_ACCEPTED.equals(currentMineStatusFilter)) {
            return "暂无已接取任务";
        }
        if (MINE_STATUS_CANCELLED.equals(currentMineStatusFilter)) {
            return "暂无已取消任务";
        }
        if (MINE_STATUS_REJECTED.equals(currentMineStatusFilter)) {
            return "暂无驳回任务";
        }
        if (MINE_STATUS_COMPLETED.equals(currentMineStatusFilter)) {
            return "暂无已完成任务";
        }
        return "暂无已发布任务";
    }

    private void updateMineQueryButton(TextView view, boolean selected) {
        view.setBackgroundResource(selected ? R.drawable.bg_nav_selected : R.drawable.bg_task_pill);
        view.setTextColor(getColor(selected ? R.color.dashboard_title : R.color.dashboard_subtitle));
    }

    private void updateCategoryChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_nav_selected : R.drawable.bg_task_pill);
        chip.setTextColor(getColor(selected ? R.color.dashboard_title : R.color.dashboard_subtitle));
    }

    private View createHomeTaskCard(HomeTask task) {
        LinearLayout container = createCardContainer();

        TextView categoryView = new TextView(this);
        categoryView.setText(task.getCategory());
        categoryView.setTextSize(12);
        categoryView.setTypeface(categoryView.getTypeface(), Typeface.BOLD);
        categoryView.setTextColor(getColor(R.color.dashboard_title));
        categoryView.setBackgroundResource(R.drawable.bg_task_pill);
        categoryView.setPadding(dp(10), dp(6), dp(10), dp(6));

        TextView titleView = createTitleView(task.getTitle(), 12);
        TextView amountView = createAccentView(getString(R.string.task_amount_format, task.getAmount()), 8);
        TextView metaView = createSubtitleView(getString(R.string.task_card_meta_format, task.getCategory(), task.getPublishTime()), 8);
        TextView publisherView = createMutedView(getString(R.string.task_card_publisher_format, task.getPublisher()), 6);
        TextView descriptionView = createSubtitleView(task.getDescription(), 10);
        descriptionView.setMaxLines(2);
        TextView actionView = createActionText("点击查看任务详情", 12);

        container.addView(categoryView);
        container.addView(titleView);
        container.addView(amountView);
        container.addView(metaView);
        container.addView(publisherView);
        container.addView(descriptionView);
        container.addView(actionView);
        container.setOnClickListener(v -> showDetail(task));
        return container;
    }

    private View createAcceptedTaskCard(AcceptedTask task) {
        LinearLayout container = createCardContainer();

        TextView titleView = createTitleView(task.getTitle(), 0);
        String amount = String.format(Locale.getDefault(), "¥ %.2f", task.getAmount());
        TextView metaView = createSubtitleView(getString(R.string.accepted_meta_format, amount, task.getDeadline()), 10);
        TextView statusView = createMutedView(getString(R.string.review_status_format, task.getReviewStatus()), 6);
        TextView completeView = createMutedView(
                getString(R.string.accepted_completed_format, getAcceptedCompletionStatusText(task)),
                6
        );

        MaterialButton completeButton = new MaterialButton(this);
        LinearLayout.LayoutParams completeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        completeParams.topMargin = dp(14);
        completeButton.setLayoutParams(completeParams);
        completeButton.setText(getAcceptedPrimaryActionText(task));
        completeButton.setCornerRadius(dp(14));
        completeButton.setBackgroundTintList(getColorStateList(R.color.auth_accent));
        completeButton.setEnabled(true);
        completeButton.setOnClickListener(v -> showAcceptedDetail(task));

        container.addView(titleView);
        container.addView(metaView);
        container.addView(statusView);
        container.addView(completeView);
        container.addView(completeButton);
        container.setOnClickListener(v -> showAcceptedDetail(task));
        return container;
    }

    private View createMineRecordCard(MineTaskRecord record) {
        LinearLayout container = createCardContainer();
        container.addView(createTitleView(record.getTitle(), 0));
        container.addView(createSubtitleView(record.getDescription(), 10));
        container.addView(createMutedView(getString(R.string.mine_record_meta_format, record.getAmount(), record.getDeadline()), 8));
        container.addView(createMutedView(getString(R.string.mine_record_type_format, record.getType()), 6));
        if (MINE_STATUS_COMPLETED.equals(currentMineStatusFilter)) {
            container.addView(createAccentView("可查看完成详情与图片", 10));
        }
        container.addView(createActionText("点击查看任务详情", 12));
        container.setOnClickListener(v -> showMineDetail(record));
        return container;
    }

    private LinearLayout createCardContainer() {
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.topMargin = dp(12);
        container.setLayoutParams(containerParams);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(18), dp(18), dp(18));
        container.setBackgroundResource(R.drawable.bg_dashboard_card);
        return container;
    }

    private TextView createTitleView(String text, int topMarginDp) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        view.setLayoutParams(params);
        view.setText(text);
        view.setTextSize(18);
        view.setTextColor(getColor(R.color.dashboard_title));
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        return view;
    }

    private TextView createSubtitleView(String text, int topMarginDp) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        view.setLayoutParams(params);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(getColor(R.color.dashboard_subtitle));
        return view;
    }

    private TextView createMutedView(String text, int topMarginDp) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        view.setLayoutParams(params);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(getColor(R.color.dashboard_muted));
        return view;
    }

    private TextView createAccentView(String text, int topMarginDp) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        view.setLayoutParams(params);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(getColor(R.color.task_amount));
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        return view;
    }

    private TextView createActionText(String text, int topMarginDp) {
        TextView view = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        view.setLayoutParams(params);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(getColor(R.color.auth_label));
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        return view;
    }

    private void showDetail(HomeTask task) {
        currentDetailMode = DETAIL_MODE_HOME;
        currentAcceptedDetailTask = null;
        currentMineDetailTask = null;
        currentDetailTask = task;
        binding.tvDetailCategory.setText(task.getCategory());
        binding.tvDetailTitle.setText(task.getTitle());
        binding.tvDetailAmount.setText(getString(R.string.task_amount_format, task.getAmount()));
        binding.tvDetailMeta.setText(getString(R.string.task_detail_meta_format, task.getPublishTime(), task.getPublisher()));
        binding.tvDetailDeadline.setText(getString(R.string.task_detail_deadline_format, task.getDeadline()));
        binding.tvDetailDescription.setText(task.getDescription());
        binding.ivDetailProof.setVisibility(View.GONE);
        binding.btnAcceptTask.setText("接取任务");
        binding.btnAcceptTask.setEnabled(true);
        binding.btnSecondaryAction.setVisibility(View.GONE);
        binding.detailOverlay.setVisibility(View.VISIBLE);
    }

    private void showMineDetail(MineTaskRecord record) {
        boolean isCompletedRecord = MINE_STATUS_COMPLETED.equals(currentMineStatusFilter)
                || "已完成".equals(record.getType())
                || "审核通过".equals(record.getType());
        if (isCompletedRecord) {
            openCompletedTaskDetailPage(record);
            return;
        }

        currentDetailMode = DETAIL_MODE_MINE;
        currentAcceptedDetailTask = null;
        currentDetailTask = null;
        currentMineDetailTask = record;
        binding.tvDetailCategory.setText(record.getType());
        binding.tvDetailTitle.setText(record.getTitle());
        binding.tvDetailAmount.setText(getString(R.string.task_amount_format, record.getAmount()));
        binding.tvDetailMeta.setText(getString(R.string.mine_record_type_format, record.getType()));
        binding.tvDetailDeadline.setText(getString(R.string.task_detail_deadline_format, record.getDeadline()));
        binding.tvDetailDescription.setText(record.getDescription());
        binding.ivDetailProof.setVisibility(View.GONE);
        binding.ivDetailProof.setImageDrawable(null);

        boolean canCancel = QUERY_PUBLISHED.equals(currentMineQuery) && MINE_STATUS_ALL.equals(currentMineStatusFilter) && "已发布".equals(record.getType());
        binding.btnAcceptTask.setText(canCancel ? "取消发布" : "关闭操作");
        binding.btnAcceptTask.setEnabled(canCancel);
        binding.btnSecondaryAction.setVisibility(View.GONE);
        binding.detailOverlay.setVisibility(View.VISIBLE);
    }

    private void openCompletedTaskDetailPage(MineTaskRecord record) {
        Intent intent = new Intent(this, CompletedTaskDetailActivity.class);
        intent.putExtra(CompletedTaskDetailActivity.EXTRA_TASK_TITLE, record.getTitle());
        intent.putExtra(CompletedTaskDetailActivity.EXTRA_TASK_STATUS, record.getType());
        intent.putExtra(CompletedTaskDetailActivity.EXTRA_TASK_DESCRIPTION, record.getDescription());
        intent.putExtra(CompletedTaskDetailActivity.EXTRA_TASK_DEADLINE, record.getDeadline());
        intent.putExtra(CompletedTaskDetailActivity.EXTRA_TASK_AMOUNT, record.getAmount());
        intent.putExtra(CompletedTaskDetailActivity.EXTRA_TASK_PROOF_URL, record.getCompletionProofUrl());
        startActivity(intent);
    }

    private void showAcceptedDetail(AcceptedTask task) {
        currentDetailMode = DETAIL_MODE_ACCEPTED;
        currentDetailTask = null;
        currentMineDetailTask = null;
        currentAcceptedDetailTask = task;
        binding.tvDetailCategory.setText(task.getReviewStatus());
        binding.tvDetailTitle.setText(task.getTitle());
        binding.tvDetailAmount.setText(getString(R.string.task_amount_format, task.getAmount()));
        binding.tvDetailMeta.setText(getString(R.string.review_status_format, task.getReviewStatus()));
        binding.tvDetailDeadline.setText(getString(R.string.task_detail_deadline_format, task.getDeadline()));
        String proofText = TextUtils.isEmpty(task.getCompletionProofUrl())
                ? getString(R.string.accepted_proof_missing)
                : getString(R.string.accepted_proof_uploaded_format, task.getCompletionProofUrl());
        binding.tvDetailDescription.setText(proofText);
        if (TextUtils.isEmpty(task.getCompletionProofUrl())) {
            binding.ivDetailProof.setVisibility(View.GONE);
            binding.ivDetailProof.setImageDrawable(null);
        } else {
            binding.ivDetailProof.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(task.getCompletionProofUrl())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(binding.ivDetailProof);
        }
        binding.btnAcceptTask.setText(getAcceptedPrimaryActionText(task));
        boolean canComplete = canCompleteAcceptedTask(task);
        binding.btnAcceptTask.setEnabled(canComplete && !isSubmittingProof);
        if (canCancelAcceptedTask(task)) {
            binding.btnSecondaryAction.setVisibility(View.VISIBLE);
            binding.btnSecondaryAction.setEnabled(!isSubmittingProof);
            binding.btnSecondaryAction.setText(getString(R.string.accepted_action_cancel));
        } else {
            binding.btnSecondaryAction.setVisibility(View.GONE);
        }
        binding.detailOverlay.setVisibility(View.VISIBLE);
    }

    private void onDetailPrimaryAction() {
        if (currentDetailMode == DETAIL_MODE_ACCEPTED) {
            if (currentAcceptedDetailTask == null || isSubmittingProof || !canCompleteAcceptedTask(currentAcceptedDetailTask)) {
                return;
            }
            pickProofImage();
            return;
        }
        if (currentMineDetailTask != null) {
            cancelCurrentMineTask();
            return;
        }
        acceptCurrentTask();
    }

    private void onDetailSecondaryAction() {
        if (currentDetailMode != DETAIL_MODE_ACCEPTED || currentAcceptedDetailTask == null || isSubmittingProof) {
            return;
        }
        cancelCurrentAcceptedTask();
    }

    private void acceptCurrentTask() {
        if (currentDetailTask == null) {
            return;
        }
        AcceptedTask acceptedTask = stageTwoRepository.acceptTask(currentDetailTask.getId(), sessionManager.getUserId());
        if (acceptedTask == null) {
            showMessage(getString(R.string.task_action_invalid));
            return;
        }
        hideDetail();
        filterHomeTasks();
        renderAcceptedTaskList();
        renderMineTaskList();
        showMessage("接取成功");
    }

    private void cancelCurrentMineTask() {
        if (currentMineDetailTask == null) {
            return;
        }
        MineRepository.ActionResult result = mineRepository.cancelTask(currentMineDetailTask.getId(), sessionManager.getUserId());
        if (!result.isSuccess()) {
            showMessage(TextUtils.isEmpty(result.getMessage()) ? getString(R.string.task_action_invalid) : result.getMessage());
            return;
        }
        hideDetail();
        renderWalletInfo();
        renderMineTaskList();
        filterHomeTasks();
        showMessage(TextUtils.isEmpty(result.getMessage()) ? "取消成功" : result.getMessage());
    }

    private void pickProofImage() {
        if (currentAcceptedDetailTask == null || isSubmittingProof || !canCompleteAcceptedTask(currentAcceptedDetailTask)) {
            return;
        }
        imagePickerLauncher.launch("image/*");
    }

    private void submitAcceptedTaskWithProof() {
        if (currentAcceptedDetailTask == null || pendingProofImageFile == null || isSubmittingProof) {
            return;
        }
        isSubmittingProof = true;
        binding.btnAcceptTask.setEnabled(false);
        binding.btnAcceptTask.setText("正在上传...");

        long taskId = currentAcceptedDetailTask.getId();
        long userId = sessionManager.getUserId();
        File proofFile = pendingProofImageFile;

        NetworkClient.getInstance().executor().execute(() -> {
            StageTwoRepository.CompleteTaskResult result = stageTwoRepository.completeTask(taskId, userId, proofFile);
            runOnUiThread(() -> {
                isSubmittingProof = false;
                pendingProofImageFile = null;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (result == null || !result.isSuccess() || result.getTask() == null) {
                    binding.btnAcceptTask.setEnabled(true);
                    binding.btnAcceptTask.setText(getString(R.string.accepted_action_complete));
                    String message = result == null ? null : result.getMessage();
                    showMessage(TextUtils.isEmpty(message) ? getString(R.string.task_action_invalid) : message);
                    return;
                }
                currentAcceptedDetailTask = result.getTask();
                showAcceptedDetail(currentAcceptedDetailTask);
                renderAcceptedTaskList();
                renderMineTaskList();
                showMessage(TextUtils.isEmpty(result.getMessage()) ? getString(R.string.task_complete_success) : result.getMessage());
            });
        });
    }

    private void cancelCurrentAcceptedTask() {
        if (currentAcceptedDetailTask == null || isSubmittingProof) {
            return;
        }
        isSubmittingProof = true;
        binding.btnAcceptTask.setEnabled(false);
        binding.btnSecondaryAction.setEnabled(false);
        binding.btnSecondaryAction.setText(getString(R.string.accepted_action_cancelled));

        long taskId = currentAcceptedDetailTask.getId();
        long userId = sessionManager.getUserId();
        NetworkClient.getInstance().executor().execute(() -> {
            StageTwoRepository.CompleteTaskResult result = stageTwoRepository.cancelAcceptedTask(taskId, userId);
            runOnUiThread(() -> {
                isSubmittingProof = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (result == null || !result.isSuccess() || result.getTask() == null) {
                    showAcceptedDetail(currentAcceptedDetailTask);
                    String message = result == null ? null : result.getMessage();
                    showMessage(TextUtils.isEmpty(message) ? getString(R.string.task_action_invalid) : message);
                    return;
                }
                currentAcceptedDetailTask = result.getTask();
                renderAcceptedTaskList();
                renderMineTaskList();
                renderWalletInfo();
                filterHomeTasks();
                hideDetail();
                showMessage(TextUtils.isEmpty(result.getMessage()) ? getString(R.string.task_cancel_accept_success) : result.getMessage());
            });
        });
    }

    private String getAcceptedPrimaryActionText(AcceptedTask task) {
        if (isAcceptedTaskFailed(task)) {
            return getString(R.string.accepted_action_failed);
        }
        if ("待审核".equals(task.getReviewStatus())) {
            return getString(R.string.accepted_action_waiting);
        }
        return task.isCompleted() ? getString(R.string.accepted_action_done) : getString(R.string.accepted_action_complete);
    }

    private String getAcceptedCompletionStatusText(AcceptedTask task) {
        if (isAcceptedTaskFailed(task)) {
            return getString(R.string.accepted_completed_failed);
        }
        if ("待审核".equals(task.getReviewStatus())) {
            return getString(R.string.accepted_completed_pending);
        }
        return getString(task.isCompleted() ? R.string.accepted_completed_yes : R.string.accepted_completed_no);
    }

    private boolean canCompleteAcceptedTask(AcceptedTask task) {
        return !task.isCompleted()
                && "进行中".equals(task.getReviewStatus())
                && !isAcceptedTaskFailed(task);
    }

    private boolean canCancelAcceptedTask(AcceptedTask task) {
        return !task.isCompleted() && "进行中".equals(task.getReviewStatus()) && !isAcceptedTaskFailed(task);
    }

    private File createTempImageFile(Uri uri, String prefix) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return null;
            }
            File outputFile = new File(getCacheDir(), prefix + "-" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
            }
            return outputFile;
        } catch (IOException exception) {
            return null;
        }
    }

    private void hideDetail() {
        currentDetailTask = null;
        currentMineDetailTask = null;
        currentAcceptedDetailTask = null;
        currentDetailMode = DETAIL_MODE_HOME;
        pendingProofImageFile = null;
        isSubmittingProof = false;
        binding.btnSecondaryAction.setVisibility(View.GONE);
        binding.detailOverlay.setVisibility(View.GONE);
    }

    private void clearDashboardLists() {
        binding.homeTaskList.removeAllViews();
        binding.acceptedTaskList.removeAllViews();
        binding.publishedTaskList.removeAllViews();
    }

    private void selectDashboardPage(View selectedView) {
        setNavState(binding.navHome, selectedView == binding.navHome);
        setNavState(binding.navAccepted, selectedView == binding.navAccepted);
        setNavState(binding.navMine, selectedView == binding.navMine);

        binding.pageHome.setVisibility(selectedView == binding.navHome ? View.VISIBLE : View.GONE);
        binding.pageAccepted.setVisibility(selectedView == binding.navAccepted ? View.VISIBLE : View.GONE);
        binding.pageMine.setVisibility(selectedView == binding.navMine ? View.VISIBLE : View.GONE);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
        binding.tabLogin.setEnabled(!loading);
        binding.tabRegister.setEnabled(!loading);
    }

    private String getInput(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static class AppNotification {
        private final long id;
        private final String title;
        private final String message;
        private final String timeText;
        private boolean isRead;

        private AppNotification(long id, String title, String message, String timeText, boolean isRead) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.timeText = timeText;
            this.isRead = isRead;
        }
    }
}
