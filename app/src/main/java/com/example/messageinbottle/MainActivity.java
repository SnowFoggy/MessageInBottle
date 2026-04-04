package com.example.messageinbottle;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messageinbottle.data.local.SessionManager;
import com.example.messageinbottle.data.model.AcceptedTask;
import com.example.messageinbottle.data.model.HomeTask;
import com.example.messageinbottle.data.model.MineTaskRecord;
import com.example.messageinbottle.data.model.Wallet;
import com.example.messageinbottle.data.repository.AuthRepository;
import com.example.messageinbottle.data.repository.MineRepository;
import com.example.messageinbottle.data.repository.StageTwoRepository;
import com.example.messageinbottle.databinding.ActivityMainBinding;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String QUERY_PUBLISHED = "已发布";
    private static final String QUERY_COMPLETED = "已完成";

    private ActivityMainBinding binding;
    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private StageTwoRepository stageTwoRepository;
    private MineRepository mineRepository;
    private boolean isLoginMode = true;
    private String selectedCategory = "全部";
    private String currentMineQuery = QUERY_PUBLISHED;
    private HomeTask currentDetailTask;

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

        initViews();
        bindActions();
        updateAuthState();
    }

    private void initViews() {
        switchMode(true);
        selectDashboardPage(binding.navHome);
        renderCategoryChips();
        updateMineQueryButtons();
    }

    private void bindActions() {
        binding.tabLogin.setOnClickListener(v -> switchMode(true));
        binding.tabRegister.setOnClickListener(v -> switchMode(false));
        binding.btnSubmit.setOnClickListener(v -> submitAuth());
        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnMineLogout.setOnClickListener(v -> logout());
        binding.navHome.setOnClickListener(v -> selectDashboardPage(binding.navHome));
        binding.navAccepted.setOnClickListener(v -> selectDashboardPage(binding.navAccepted));
        binding.navMine.setOnClickListener(v -> selectDashboardPage(binding.navMine));
        binding.btnCloseDetail.setOnClickListener(v -> hideDetail());
        binding.btnAcceptTask.setOnClickListener(v -> acceptCurrentTask());
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
        binding.btnQueryPublished.setOnClickListener(v -> {
            currentMineQuery = QUERY_PUBLISHED;
            updateMineQueryButtons();
            renderMineTaskList();
        });
        binding.btnQueryCompleted.setOnClickListener(v -> {
            currentMineQuery = QUERY_COMPLETED;
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
        hideDetail();
        hidePublishPanel();

        if (loggedIn) {
            String displayName = sessionManager.getDisplayName();
            long userId = sessionManager.getUserId();
            mineRepository.ensureWalletSeed(userId);
            binding.cardSession.setVisibility(View.VISIBLE);
            binding.tvSessionUser.setText(getString(R.string.current_user_format, displayName));
            binding.tvDashboardGreeting.setText(getString(R.string.dashboard_greeting_format, displayName));
            binding.tvSessionBadge.setText(getString(R.string.dashboard_session_format, displayName));
            binding.tvMineSummary.setText(getString(R.string.mine_summary_format, displayName));
            renderDashboardLists();
            renderWalletInfo();
            renderMineTaskList();
            selectDashboardPage(binding.navHome);
        } else {
            binding.cardSession.setVisibility(View.GONE);
            clearDashboardLists();
        }
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
        List<MineTaskRecord> records = QUERY_PUBLISHED.equals(currentMineQuery)
                ? mineRepository.getPublishedTasks(sessionManager.getUserId())
                : mineRepository.getCompletedTasks(sessionManager.getUserId());

        for (MineTaskRecord record : records) {
            binding.publishedTaskList.addView(createMineRecordCard(record));
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
        boolean success = mineRepository.publishTask(sessionManager.getUserId(), title, description, amount, deadline);
        if (!success) {
            showMessage(getString(R.string.error_network));
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
        showMessage(getString(R.string.publish_success));
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
        for (AcceptedTask task : acceptedTasks) {
            binding.acceptedTaskList.addView(createAcceptedTaskCard(task));
        }
    }

    private void filterHomeTasks() {
        if (binding.homeTaskList == null) {
            return;
        }

        binding.homeTaskList.removeAllViews();
        String keyword = getInput(binding.inputTaskSearch.getText() == null ? null : binding.inputTaskSearch.getText().toString());
        List<HomeTask> tasks = stageTwoRepository.searchHomeTasks(keyword, selectedCategory);

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
            for (HomeTask task : tasks) {
                binding.homeTaskList.addView(createHomeTaskCard(task));
            }
        }

        binding.tvTaskResultHint.setText(getString(R.string.task_result_count_format, tasks.size()));
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

    private void updateMineQueryButtons() {
        updateMineQueryButton(binding.btnQueryPublished, QUERY_PUBLISHED.equals(currentMineQuery));
        updateMineQueryButton(binding.btnQueryCompleted, QUERY_COMPLETED.equals(currentMineQuery));
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
                getString(
                        R.string.accepted_completed_format,
                        getString(task.isCompleted() ? R.string.accepted_completed_yes : R.string.accepted_completed_no)
                ),
                6
        );

        LinearLayout actionRow = new LinearLayout(this);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionParams.topMargin = dp(14);
        actionRow.setLayoutParams(actionParams);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        MaterialButton completeButton = new MaterialButton(this);
        LinearLayout.LayoutParams completeParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        completeButton.setLayoutParams(completeParams);
        completeButton.setText(task.isCompleted() ? R.string.accepted_action_done : R.string.accepted_action_complete);
        completeButton.setCornerRadius(dp(14));
        completeButton.setBackgroundTintList(getColorStateList(R.color.auth_accent));
        completeButton.setEnabled(!task.isCompleted());
        completeButton.setOnClickListener(v -> {
            AcceptedTask updated = stageTwoRepository.completeTask(task.getId(), sessionManager.getUserId());
            if (updated == null) {
                showMessage(getString(R.string.task_action_invalid));
            } else {
                renderAcceptedTaskList();
                renderMineTaskList();
                showMessage(getString(R.string.task_complete_success));
            }
        });

        MaterialButton reviewButton = new MaterialButton(this);
        LinearLayout.LayoutParams reviewParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        reviewParams.leftMargin = dp(10);
        reviewButton.setLayoutParams(reviewParams);
        reviewButton.setText(task.isCompleted() ? R.string.accepted_action_waiting : R.string.accepted_action_complete);
        reviewButton.setCornerRadius(dp(14));
        reviewButton.setBackgroundTintList(getColorStateList(R.color.auth_logout));
        reviewButton.setEnabled(false);

        actionRow.addView(completeButton);
        actionRow.addView(reviewButton);

        container.addView(titleView);
        container.addView(metaView);
        container.addView(statusView);
        container.addView(completeView);
        container.addView(actionRow);
        return container;
    }

    private View createMineRecordCard(MineTaskRecord record) {
        LinearLayout container = createCardContainer();
        container.addView(createTitleView(record.getTitle(), 0));
        container.addView(createSubtitleView(record.getDescription(), 10));
        container.addView(createMutedView(getString(R.string.mine_record_meta_format, record.getAmount(), record.getDeadline()), 8));
        container.addView(createMutedView(getString(R.string.mine_record_type_format, record.getType()), 6));
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
        currentDetailTask = task;
        binding.tvDetailCategory.setText(task.getCategory());
        binding.tvDetailTitle.setText(task.getTitle());
        binding.tvDetailAmount.setText(getString(R.string.task_amount_format, task.getAmount()));
        binding.tvDetailMeta.setText(getString(R.string.task_detail_meta_format, task.getPublishTime(), task.getPublisher()));
        binding.tvDetailDeadline.setText(getString(R.string.task_detail_deadline_format, task.getDeadline()));
        binding.tvDetailDescription.setText(task.getDescription());
        binding.detailOverlay.setVisibility(View.VISIBLE);
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

    private void hideDetail() {
        currentDetailTask = null;
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
}
