package com.sbs.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sbs.R;
import com.sbs.data.AppSettingsManager;
import com.sbs.data.SightingSyncManager;

public class SettingsActivity extends BaseActivity {

    private AutoCompleteTextView actThemeMode;
    private MaterialButtonToggleGroup toggleSyncMode;
    private SwitchMaterial switchWifiOnlySync;
    private SwitchMaterial switchAutoCenterMap;
    private SwitchMaterial switchShowSampleMarkers;
    private Button btnSyncNowGlobal;
    private AppSettingsManager appSettingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appSettingsManager = new AppSettingsManager(this);
        appSettingsManager.applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        applyWindowInsets(findViewById(R.id.toolbar).getRootView());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        actThemeMode = findViewById(R.id.actThemeMode);
        toggleSyncMode = findViewById(R.id.toggleSyncMode);
        switchWifiOnlySync = findViewById(R.id.switchWifiOnlySync);
        switchAutoCenterMap = findViewById(R.id.switchAutoCenterMap);
        switchShowSampleMarkers = findViewById(R.id.switchShowSampleMarkers);
        btnSyncNowGlobal = findViewById(R.id.btnSyncNowGlobal);

        setupThemeDropdown();
        bindSavedValues();
        bindSettingListeners();
        
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountConfirmation());
        btnSyncNowGlobal.setOnClickListener(v -> {
            if (SightingSyncManager.isOnline(this)) {
                SightingSyncManager.syncAllPending(this);
                Toast.makeText(this, "Syncing all records...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupThemeDropdown() {
        String[] themeOptions = {"System default", "Light", "Dark"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, themeOptions);
        actThemeMode.setAdapter(adapter);
    }

    private void bindSavedValues() {
        if (appSettingsManager.isAutoSyncEnabled()) {
            toggleSyncMode.check(R.id.btnSyncAuto);
        } else {
            toggleSyncMode.check(R.id.btnSyncManual);
        }
        switchWifiOnlySync.setChecked(appSettingsManager.isWifiOnlySyncEnabled());
        switchAutoCenterMap.setChecked(appSettingsManager.isAutoCenterMapEnabled());
        switchShowSampleMarkers.setChecked(appSettingsManager.isShowSampleMarkersEnabled());

        String themeMode = appSettingsManager.getThemeMode();
        switch (themeMode) {
            case AppSettingsManager.THEME_LIGHT:
                actThemeMode.setText(getString(R.string.light), false);
                break;
            case AppSettingsManager.THEME_DARK:
                actThemeMode.setText(getString(R.string.dark), false);
                break;
            default:
                actThemeMode.setText(getString(R.string.system_default), false);
                break;
        }
        updateSyncUI();
    }

    private void bindSettingListeners() {
        actThemeMode.setOnItemClickListener((parent, view, position, id) -> {
            String selected = actThemeMode.getText().toString();
            if ("Light".equals(selected)) appSettingsManager.setThemeMode(AppSettingsManager.THEME_LIGHT);
            else if ("Dark".equals(selected)) appSettingsManager.setThemeMode(AppSettingsManager.THEME_DARK);
            else appSettingsManager.setThemeMode(AppSettingsManager.THEME_SYSTEM);
            appSettingsManager.applyTheme();
        });

        toggleSyncMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean auto = checkedId == R.id.btnSyncAuto;
            appSettingsManager.setAutoSyncEnabled(auto);
            updateSyncUI();
        });

        switchWifiOnlySync.setOnCheckedChangeListener((buttonView, isChecked) ->
                appSettingsManager.setWifiOnlySyncEnabled(isChecked));

        switchAutoCenterMap.setOnCheckedChangeListener((buttonView, isChecked) ->
                appSettingsManager.setAutoCenterMapEnabled(isChecked));

        switchShowSampleMarkers.setOnCheckedChangeListener((buttonView, isChecked) ->
                appSettingsManager.setShowSampleMarkersEnabled(isChecked));
    }

    private void updateSyncUI() {
        boolean autoSync = appSettingsManager.isAutoSyncEnabled();
        btnSyncNowGlobal.setVisibility(autoSync ? View.GONE : View.VISIBLE);
        switchWifiOnlySync.setEnabled(autoSync);
        switchWifiOnlySync.setAlpha(autoSync ? 1f : 0.5f);
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.confirm_delete_account)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAccount())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete account. Please re-authenticate.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
