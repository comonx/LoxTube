package com.newtube.mobile.ui.settings;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.browse.CategoryChip;
import com.newtube.mobile.ui.browse.CategoryHabitManager;

import java.util.List;

public class MobileCategoryManagerActivity extends AppCompatActivity {
    private LinearLayout mContainerActiveCategories;
    private LinearLayout mContainerSuggestions;
    private View mViewEmpty;
    private TextView mTextCategoryCount;

    private static final String[][] SUGGESTIONS = {
            {"📰 সংবাদ", "News bangla"},
            {"🎬 সিনেমা", "Bangla movie"},
            {"🎵 গান", "Bangla song"},
            {"🎭 নাটক", "Bangla natok"},
            {"😂 ফানি", "Funny comedy bangla"},
            {"🎮 গেমিং", "Gaming gameplay"},
            {"📱 প্রযুক্তি", "Tech unboxing bangla"},
            {"⚽ খেলা", "Cricket highlights"},
            {"🕌 ইসলামিক", "Islamic lecture bangla"},
            {"🍳 রান্না", "Cooking recipe bangla"},
            {"✨ অ্যানিমেশন", "Animation cartoon"},
            {"📚 শিক্ষা", "Educational lecture"}
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_category_manager);

        findViewById(R.id.btn_category_manager_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_category_dialog).setOnClickListener(v -> showAddEditDialog(null));

        mContainerActiveCategories = findViewById(R.id.container_active_categories);
        mContainerSuggestions = findViewById(R.id.container_category_suggestions);
        mViewEmpty = findViewById(R.id.view_category_empty);
        mTextCategoryCount = findViewById(R.id.text_category_count);

        setupSuggestions();
        refreshCategoryList();
    }

    private void setupSuggestions() {
        mContainerSuggestions.removeAllViews();
        for (String[] item : SUGGESTIONS) {
            final String title = item[0];
            final String query = item[1];

            TextView chip = new TextView(this);
            chip.setText(title);
            chip.setTextColor(Color.parseColor("#334155"));
            chip.setTextSize(12.5f);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setBackgroundResource(R.drawable.bg_setup_chip_colorful_unselected);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                // Check if already added
                List<CategoryChip> current = CategoryHabitManager.instance(this).getCustomTabs();
                for (CategoryChip c : current) {
                    if (c.getTitle().equalsIgnoreCase(title)) {
                        Toast.makeText(this, title + " ইতিমধ্যে যুক্ত আছে!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                CategoryHabitManager.instance(this).addCustomTab(title, query);
                Toast.makeText(this, title + " হোমস্ক্রিনে যুক্ত করা হয়েছে ✓", Toast.LENGTH_SHORT).show();
                refreshCategoryList();
            });

            mContainerSuggestions.addView(chip);
        }
    }

    private void refreshCategoryList() {
        mContainerActiveCategories.removeAllViews();
        List<CategoryChip> customTabs = CategoryHabitManager.instance(this).getCustomTabs();

        mTextCategoryCount.setText(customTabs.size() + " টি");

        if (customTabs.isEmpty()) {
            mViewEmpty.setVisibility(View.VISIBLE);
            return;
        }

        mViewEmpty.setVisibility(View.GONE);

        for (int i = 0; i < customTabs.size(); i++) {
            final CategoryChip chip = customTabs.get(i);
            View row = LayoutInflater.from(this).inflate(R.layout.item_manage_category_tab, mContainerActiveCategories, false);

            TextView textBadge = row.findViewById(R.id.text_category_badge);
            TextView textTitle = row.findViewById(R.id.text_category_title);
            TextView textQuery = row.findViewById(R.id.text_category_query);
            View btnEdit = row.findViewById(R.id.btn_edit_category);
            View btnDelete = row.findViewById(R.id.btn_delete_category);

            textBadge.setText(String.valueOf(i + 1));
            textTitle.setText(chip.getTitle());
            textQuery.setText("সার্চ: " + (chip.getSearchQuery() != null ? chip.getSearchQuery() : chip.getTitle()));

            btnEdit.setOnClickListener(v -> showAddEditDialog(chip));

            btnDelete.setOnClickListener(v -> {
                CategoryHabitManager.instance(this).deleteCustomTab(chip.getId());
                Toast.makeText(this, chip.getTitle() + " ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show();
                refreshCategoryList();
            });

            mContainerActiveCategories.addView(row);
        }
    }

    private void showAddEditDialog(@Nullable CategoryChip existing) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_category, null);
        dialog.setContentView(content);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            window.setLayout(w, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        TextView titleView = content.findViewById(R.id.dialog_category_title);
        EditText editName = content.findViewById(R.id.edit_category_name);
        EditText editQuery = content.findViewById(R.id.edit_category_query);
        TextView btnCancel = content.findViewById(R.id.btn_dialog_cancel);
        TextView btnSave = content.findViewById(R.id.btn_dialog_save);

        if (existing != null) {
            titleView.setText("ক্যাটাগরি এডিট করুন");
            editName.setText(existing.getTitle());
            editQuery.setText(existing.getSearchQuery() != null ? existing.getSearchQuery() : existing.getTitle());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String query = editQuery.getText().toString().trim();

            if (name.isEmpty()) {
                editName.setError("অনুগ্রহ করে একটি নাম লিখুন");
                return;
            }

            if (existing != null) {
                CategoryHabitManager.instance(this).updateCustomTab(existing.getId(), name, query);
                Toast.makeText(this, "ক্যাটাগরি আপডেট হয়েছে ✓", Toast.LENGTH_SHORT).show();
            } else {
                CategoryHabitManager.instance(this).addCustomTab(name, query);
                Toast.makeText(this, "নতুন ক্যাটাগরি যুক্ত হয়েছে ✓", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
            refreshCategoryList();
        });

        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
