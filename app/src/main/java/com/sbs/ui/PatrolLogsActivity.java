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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sbs.R;
import com.sbs.data.AppRepository;
import com.sbs.data.AppSettingsManager;
import com.sbs.data.PatrolLogRecord;
import com.sbs.data.SightingSyncManager;

public class PatrolLogsActivity extends BaseActivity implements PatrolLogsAdapter.LogActionListener {

    private PatrolLogsAdapter adapter;
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
        setContentView(R.layout.activity_patrol_logs);
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

        RecyclerView recyclerView = findViewById(R.id.recyclerPatrolLogs);
        emptyState = findViewById(R.id.tvEmptyState);
        adapter = new PatrolLogsAdapter(appSettingsManager, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fabAddLog);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, PatrolLogEditorActivity.class);
            editorLauncher.launch(intent);
        });

        repository.observePatrolLogs(rangerId).observe(this, records -> {
            adapter.submitList(records);
            emptyState.setVisibility(records == null || records.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onSyncNow(PatrolLogRecord record) {
        if (SightingSyncManager.isOnline(this)) {
            SightingSyncManager.syncAllPending(this);
            Toast.makeText(this, "Sync queued", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No connection or WiFi required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(PatrolLogRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Patrol Log")
                .setMessage("Are you sure you want to delete this log locally?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deletePatrolLog(rangerId, record.localId);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEdit(PatrolLogRecord record) {
        Intent intent = new Intent(this, PatrolLogEditorActivity.class);
        intent.putExtra("log_id", record.localId);
        editorLauncher.launch(intent);
    }
}
