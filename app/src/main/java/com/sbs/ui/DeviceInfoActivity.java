package com.sbs.ui;

import android.app.ActivityManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.sbs.R;

import java.util.Locale;

public class DeviceInfoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        applyWindowInsets(findViewById(R.id.deviceInfoRoot));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupDeviceOverview();
        setupCpuDiagnostics();
        setupRamDiagnostics();
        setupSensorDiagnostics();
    }

    private void setupDeviceOverview() {
        setRowData(R.id.rowBrand, "Brand", Build.BRAND);
        setRowData(R.id.rowManufacturer, "Manufacturer", Build.MANUFACTURER);
        setRowData(R.id.rowModel, "Model", Build.MODEL);
        setRowData(R.id.rowDevice, "Device", Build.DEVICE);
        setRowData(R.id.rowAndroidVersion, "Android Version", Build.VERSION.RELEASE);
        setRowData(R.id.rowSdk, "SDK Level", String.valueOf(Build.VERSION.SDK_INT));
        setRowData(R.id.rowAppVersion, "App Version", getAppVersion());
    }

    private void setupCpuDiagnostics() {
        setRowData(R.id.rowCpuAbi, "CPU ABI", Build.SUPPORTED_ABIS[0]);
        setRowData(R.id.rowCores, "Processor Cores", String.valueOf(Runtime.getRuntime().availableProcessors()));
        setRowData(R.id.rowBoard, "Board", Build.BOARD);
        setRowData(R.id.rowHardware, "Hardware", Build.HARDWARE);
    }

    private void setupRamDiagnostics() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        double totalRam = mi.totalMem / (1024.0 * 1024.0 * 1024.0);
        double availRam = mi.availMem / (1024.0 * 1024.0 * 1024.0);

        setRowData(R.id.rowTotalRam, "Total RAM", String.format(Locale.US, "%.2f GB", totalRam));
        setRowData(R.id.rowAvailableRam, "Available RAM", String.format(Locale.US, "%.2f GB", availRam));
        setRowData(R.id.rowLowMemory, "Low Memory Status", mi.lowMemory ? "YES" : "NO");
    }

    private void setupSensorDiagnostics() {
        LinearLayout container = findViewById(R.id.sensorContainer);
        container.removeAllViews();

        PackageManager pm = getPackageManager();
        addCapabilityRow(container, "GPS / Location", pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS));
        addCapabilityRow(container, "Camera", pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY));
        addCapabilityRow(container, "Microphone", pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE));
    }

    private void addCapabilityRow(LinearLayout container, String name, boolean available) {
        View row = getLayoutInflater().inflate(R.layout.row_diagnostic, container, false);
        TextView tvLabel = row.findViewById(R.id.tvLabel);
        TextView tvValue = row.findViewById(R.id.tvValue);
        TextView tvSubValue = row.findViewById(R.id.tvSubValue);

        tvLabel.setText(name);
        if (available) {
            tvValue.setText("Available");
            tvValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
            tvSubValue.setVisibility(View.GONE);
        } else {
            tvValue.setText("Not Available");
            tvValue.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
            tvSubValue.setVisibility(View.GONE);
        }
        container.addView(row);
    }

    private void setRowData(int layoutId, String label, String value) {
        View layout = findViewById(layoutId);
        ((TextView) layout.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) layout.findViewById(R.id.tvValue)).setText(value);
    }

    private String getAppVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }
}
