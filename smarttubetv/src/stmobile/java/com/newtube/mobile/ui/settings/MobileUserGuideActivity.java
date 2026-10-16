package com.newtube.mobile.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.liskovsoft.smartyoutubetv2.tv.R;

public class MobileUserGuideActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_user_guide);

        findViewById(R.id.btn_guide_back).setOnClickListener(v -> finish());
    }
}
