package com.example.messageinbottle;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.messageinbottle.databinding.ActivityCompletedTaskDetailBinding;

import java.util.Locale;

public class CompletedTaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_TITLE = "extra_task_title";
    public static final String EXTRA_TASK_STATUS = "extra_task_status";
    public static final String EXTRA_TASK_DESCRIPTION = "extra_task_description";
    public static final String EXTRA_TASK_DEADLINE = "extra_task_deadline";
    public static final String EXTRA_TASK_AMOUNT = "extra_task_amount";
    public static final String EXTRA_TASK_PROOF_URL = "extra_task_proof_url";

    private ActivityCompletedTaskDetailBinding binding;
    private String currentProofUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCompletedTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        renderContent();
        bindActions();
    }

    private void renderContent() {
        String title = getIntent().getStringExtra(EXTRA_TASK_TITLE);
        String status = getIntent().getStringExtra(EXTRA_TASK_STATUS);
        String description = getIntent().getStringExtra(EXTRA_TASK_DESCRIPTION);
        String deadline = getIntent().getStringExtra(EXTRA_TASK_DEADLINE);
        double amount = getIntent().getDoubleExtra(EXTRA_TASK_AMOUNT, 0D);
        String proofUrl = getIntent().getStringExtra(EXTRA_TASK_PROOF_URL);
        currentProofUrl = proofUrl;

        binding.tvDetailTitle.setText(TextUtils.isEmpty(title) ? getString(R.string.completed_detail_default_title) : title);
        binding.tvDetailAmount.setText(String.format(Locale.getDefault(), "¥ %.2f", amount));
        binding.tvDetailDeadline.setText(TextUtils.isEmpty(deadline)
                ? getString(R.string.completed_detail_time_empty)
                : getString(R.string.completed_detail_time_format, deadline));
        binding.tvCompletedContent.setText(TextUtils.isEmpty(description)
                ? getString(R.string.completed_detail_content_empty)
                : description);

        if (TextUtils.isEmpty(proofUrl)) {
            binding.ivDetailProof.setVisibility(View.GONE);
            binding.tvProofEmpty.setVisibility(View.VISIBLE);
            binding.tvProofEmpty.setText(getString(R.string.completed_detail_image_empty));
        } else {
            binding.ivDetailProof.setVisibility(View.VISIBLE);
            binding.tvProofEmpty.setVisibility(View.GONE);
            Glide.with(this)
                    .load(proofUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .into(binding.ivDetailProof);
        }
    }

    private void bindActions() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.ivDetailProof.setOnClickListener(v -> showImagePreview());
        binding.btnReward.setOnClickListener(v -> {
        });
        binding.btnFeedback.setOnClickListener(v -> {
        });
    }

    private void showImagePreview() {
        if (TextUtils.isEmpty(currentProofUrl)) {
            return;
        }
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setBackgroundColor(Color.BLACK);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        Glide.with(this)
                .load(currentProofUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(imageView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
        dialog.show();
    }
}

