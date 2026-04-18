package com.example.messageinbottle;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsPrivacyActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private LinearLayout placeholderContainer;
    private TextView tvPageTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_privacy);

        initViews();
        setupEdgeToEdge();
        setupBackHandler();
        bindActions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        placeholderContainer = findViewById(R.id.placeholderContainer);
        tvPageTitle = findViewById(R.id.tvPageTitle);
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
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, android.R.anim.fade_out);
        });
    }

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public LinearLayout getPlaceholderContainer() {
        return placeholderContainer;
    }
}
