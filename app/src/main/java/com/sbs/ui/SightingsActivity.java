package com.sbs.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.appbar.MaterialToolbar;
import com.sbs.R;
import com.sbs.data.AppRepository;
import com.sbs.data.AppSettingsManager;
import com.sbs.data.SightingRecord;
import com.sbs.data.SightingSyncManager;

public class SightingsActivity extends BaseActivity implements SightingsAdapter.SightingActionListener {

    private SightingsAdapter adapter;
    private View emptyState;
    private AppSettingsManager appSettingsManager;
    private AppRepository repository;
    private String rangerId;

    private final ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sightings);
        applyWindowInsets(findViewById(R.id.toolbar).getRootView());

        appSettingsManager = new AppSettingsManager(this);
        repository = AppRepository.getInstance(this);
        rangerId = FirebaseAuth.getInstance().getUid();
        if (rangerId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh) {
                refreshFromRemote();
                return true;
            }
            return false;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerSightings);
        emptyState = findViewById(R.id.tvEmptyState);
        adapter = new SightingsAdapter(appSettingsManager, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        repository.observeSightings(rangerId).observe(this, records -> {
            adapter.submitList(records);
            emptyState.setVisibility(records == null || records.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appSettingsManager.isAutoSyncEnabled() && SightingSyncManager.isOnline(this)) {
            SightingSyncManager.syncAllPending(this);
        }
    }

    private void refreshFromRemote() {
        Toast.makeText(this, "Sync queued", Toast.LENGTH_SHORT).show();
        SightingSyncManager.syncAllPending(this);
    }

    @Override
    public void onSyncNow(SightingRecord record) {
        if (SightingSyncManager.isOnline(this)) {
            SightingSyncManager.syncAllPending(this);
            Toast.makeText(this, "Sync queued", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No connection or WiFi required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(SightingRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Sighting")
                .setMessage("Are you sure you want to delete this sighting locally?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteSighting(rangerId, record.localId);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEdit(SightingRecord record) {
        Intent intent = new Intent(this, SightingEditorActivity.class);
        intent.putExtra("sighting_id", record.localId);
        editorLauncher.launch(intent);
    }
}
