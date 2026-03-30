package com.sbs.data.work;

import android.content.Context;
import android.os.BatteryManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sbs.data.AppRepository;
import com.sbs.data.local.PatrolLogEntity;
import com.sbs.data.local.SightingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UploadPendingDataWorker extends Worker {

    private static final int BATCH_LIMIT = 200;

    public UploadPendingDataWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (getBatteryPercent() < 20) {
            return Result.retry();
        }

        AppRepository repository = AppRepository.getInstance(getApplicationContext());
        try {
            syncSightings(repository.getPendingSightings(BATCH_LIMIT), repository);
            syncPatrolLogs(repository.getPendingPatrolLogs(BATCH_LIMIT), repository);
            return Result.success();
        } catch (Exception ignored) {
            return Result.retry();
        }
    }

    private void syncSightings(List<SightingEntity> entities, AppRepository repository) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        for (SightingEntity entity : entities) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("localId", entity.localId);
            payload.put("rangerId", entity.rangerId);
            payload.put("authorName", entity.authorName);
            payload.put("title", entity.title);
            payload.put("notes", entity.notes);
            payload.put("lat", entity.latitude);
            payload.put("lng", entity.longitude);
            payload.put("timestamp", entity.timestamp);
            payload.put("radius", entity.radius);
            payload.put("audioPath", entity.audioPath);
            payload.put("imagePath", entity.imagePath);
            payload.put("videoPath", entity.videoPath);
            payload.put("lastModifiedAt", entity.lastModifiedAt);
            payload.put("updatedAt", FieldValue.serverTimestamp());
            String remoteId = TextUtils.isEmpty(entity.remoteId) ? entity.localId : entity.remoteId;
            try {
                Tasks.await(firestore.collection("sightings").document(remoteId).set(payload));
                repository.markSightingSynced(entity, remoteId);
            } catch (Exception e) {
                repository.markSightingFailed(entity);
                throw e;
            }
        }
    }

    private void syncPatrolLogs(List<PatrolLogEntity> entities, AppRepository repository) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        for (PatrolLogEntity entity : entities) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("localId", entity.localId);
            payload.put("rangerId", entity.rangerId);
            payload.put("authorName", entity.authorName);
            payload.put("title", entity.title);
            payload.put("notes", entity.notes);
            payload.put("timestamp", entity.timestamp);
            payload.put("audioPath", entity.audioPath);
            payload.put("videoPath", entity.videoPath);
            payload.put("lastModifiedAt", entity.lastModifiedAt);
            payload.put("updatedAt", FieldValue.serverTimestamp());
            String remoteId = TextUtils.isEmpty(entity.remoteId) ? entity.localId : entity.remoteId;
            try {
                Tasks.await(firestore.collection("patrol_logs").document(remoteId).set(payload));
                repository.markPatrolLogSynced(entity, remoteId);
            } catch (Exception e) {
                repository.markPatrolLogFailed(entity);
                throw e;
            }
        }
    }

    private int getBatteryPercent() {
        BatteryManager batteryManager = getApplicationContext().getSystemService(BatteryManager.class);
        if (batteryManager == null) {
            return 100;
        }
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }
}
