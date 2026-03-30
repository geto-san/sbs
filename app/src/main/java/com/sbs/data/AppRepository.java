package com.sbs.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sbs.data.local.AppDatabase;
import com.sbs.data.local.PatrolLogEntity;
import com.sbs.data.local.RangerEntity;
import com.sbs.data.local.SightingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppRepository {

    public interface RecordCallback<T> {
        void onLoaded(T value);
    }

    private static volatile AppRepository instance;

    private final AppDatabase database;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppRepository(Context context) {
        database = AppDatabase.getInstance(context);
        io.execute(() -> LegacyDataImporter.importIfNeeded(context.getApplicationContext(), database));
    }

    public static AppRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    instance = new AppRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<SightingRecord>> observeSightings(String rangerId) {
        MediatorLiveData<List<SightingRecord>> liveData = new MediatorLiveData<>();
        liveData.addSource(database.sightingDao().observeByRangerId(rangerId), entities -> liveData.setValue(mapSightings(entities)));
        return liveData;
    }

    public LiveData<List<PatrolLogRecord>> observePatrolLogs(String rangerId) {
        MediatorLiveData<List<PatrolLogRecord>> liveData = new MediatorLiveData<>();
        liveData.addSource(database.patrolLogDao().observeByRangerId(rangerId), entities -> liveData.setValue(mapPatrolLogs(entities)));
        return liveData;
    }

    public void loadSighting(String rangerId, String localId, RecordCallback<SightingRecord> callback) {
        io.execute(() -> {
            SightingEntity entity = database.sightingDao().getById(rangerId, localId);
            post(callback, entity == null ? null : toRecord(entity));
        });
    }

    public void loadPatrolLog(String rangerId, String localId, RecordCallback<PatrolLogRecord> callback) {
        io.execute(() -> {
            PatrolLogEntity entity = database.patrolLogDao().getById(rangerId, localId);
            post(callback, entity == null ? null : toRecord(entity));
        });
    }

    public void saveSighting(
            String rangerId,
            String localId,
            String title,
            String notes,
            double lat,
            double lng,
            long timestamp,
            float radius,
            String audioPath,
            String imagePath,
            String videoPath,
            RecordCallback<SightingRecord> callback
    ) {
        io.execute(() -> {
            long now = System.currentTimeMillis();
            String id = TextUtils.isEmpty(localId) ? UUID.randomUUID().toString() : localId;
            SightingEntity current = database.sightingDao().getById(rangerId, id);
            upsertRanger(resolveCurrentUser());
            SightingEntity entity = new SightingEntity(
                    id,
                    current != null ? current.remoteId : null,
                    rangerId,
                    resolveAuthorName(),
                    title,
                    notes,
                    lat,
                    lng,
                    timestamp,
                    radius,
                    coalesce(audioPath, current != null ? current.audioPath : null),
                    coalesce(imagePath, current != null ? current.imagePath : null),
                    coalesce(videoPath, current != null ? current.videoPath : null),
                    SyncState.PENDING,
                    0L,
                    now
            );
            database.sightingDao().upsert(entity);
            post(callback, toRecord(entity));
        });
    }

    public void savePatrolLog(
            String rangerId,
            String localId,
            String title,
            String notes,
            long timestamp,
            String audioPath,
            String videoPath,
            RecordCallback<PatrolLogRecord> callback
    ) {
        io.execute(() -> {
            long now = System.currentTimeMillis();
            String id = TextUtils.isEmpty(localId) ? UUID.randomUUID().toString() : localId;
            PatrolLogEntity current = database.patrolLogDao().getById(rangerId, id);
            upsertRanger(resolveCurrentUser());
            PatrolLogEntity entity = new PatrolLogEntity(
                    id,
                    current != null ? current.remoteId : null,
                    rangerId,
                    resolveAuthorName(),
                    title,
                    notes,
                    timestamp,
                    coalesce(audioPath, current != null ? current.audioPath : null),
                    coalesce(videoPath, current != null ? current.videoPath : null),
                    SyncState.PENDING,
                    0L,
                    now
            );
            database.patrolLogDao().upsert(entity);
            post(callback, toRecord(entity));
        });
    }

    public void deleteSighting(String rangerId, String localId) {
        io.execute(() -> database.sightingDao().delete(rangerId, localId));
    }

    public void deletePatrolLog(String rangerId, String localId) {
        io.execute(() -> database.patrolLogDao().delete(rangerId, localId));
    }

    public void upsertRanger(FirebaseUser user) {
        if (user == null) {
            return;
        }
        long now = System.currentTimeMillis();
        database.rangerDao().upsert(new RangerEntity(
                user.getUid(),
                resolveDisplayName(user),
                user.getEmail(),
                now,
                now
        ));
    }

    public void upsertCurrentRanger() {
        io.execute(() -> upsertRanger(resolveCurrentUser()));
    }

    public List<SightingEntity> getPendingSightings(int limit) {
        return database.sightingDao().getPending(new String[]{SyncState.PENDING, SyncState.FAILED}, limit);
    }

    public List<PatrolLogEntity> getPendingPatrolLogs(int limit) {
        return database.patrolLogDao().getPending(new String[]{SyncState.PENDING, SyncState.FAILED}, limit);
    }

    public void markSightingSynced(SightingEntity entity, String remoteId) {
        entity.remoteId = remoteId;
        entity.syncStatus = SyncState.SYNCED;
        entity.lastSyncAttempt = System.currentTimeMillis();
        database.sightingDao().upsert(entity);
    }

    public void markSightingFailed(SightingEntity entity) {
        entity.syncStatus = SyncState.FAILED;
        entity.lastSyncAttempt = System.currentTimeMillis();
        database.sightingDao().upsert(entity);
    }

    public void markPatrolLogSynced(PatrolLogEntity entity, String remoteId) {
        entity.remoteId = remoteId;
        entity.syncStatus = SyncState.SYNCED;
        entity.lastSyncAttempt = System.currentTimeMillis();
        database.patrolLogDao().upsert(entity);
    }

    public void markPatrolLogFailed(PatrolLogEntity entity) {
        entity.syncStatus = SyncState.FAILED;
        entity.lastSyncAttempt = System.currentTimeMillis();
        database.patrolLogDao().upsert(entity);
    }

    public void mergeRemoteSightings(List<SightingEntity> remoteSightings) {
        for (SightingEntity remote : remoteSightings) {
            SightingEntity local = database.sightingDao().getById(remote.rangerId, remote.localId);
            if (local == null || SyncState.SYNCED.equals(local.syncStatus) || local.lastModifiedAt <= remote.lastModifiedAt) {
                database.sightingDao().upsert(remote);
            }
        }
    }

    public void runOnIo(Runnable action) {
        io.execute(action);
    }

    private <T> void post(RecordCallback<T> callback, T value) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onLoaded(value));
    }

    private List<SightingRecord> mapSightings(List<SightingEntity> entities) {
        List<SightingRecord> records = new ArrayList<>(entities.size());
        for (SightingEntity entity : entities) {
            records.add(toRecord(entity));
        }
        return records;
    }

    private List<PatrolLogRecord> mapPatrolLogs(List<PatrolLogEntity> entities) {
        List<PatrolLogRecord> records = new ArrayList<>(entities.size());
        for (PatrolLogEntity entity : entities) {
            records.add(toRecord(entity));
        }
        return records;
    }

    private static SightingRecord toRecord(SightingEntity entity) {
        return new SightingRecord(
                entity.localId,
                entity.remoteId,
                entity.title,
                entity.notes,
                entity.latitude,
                entity.longitude,
                entity.timestamp,
                entity.rangerId,
                entity.authorName,
                entity.syncStatus,
                entity.lastSyncAttempt,
                entity.audioPath,
                entity.imagePath,
                entity.videoPath,
                entity.radius
        );
    }

    private static PatrolLogRecord toRecord(PatrolLogEntity entity) {
        return new PatrolLogRecord(
                entity.localId,
                entity.remoteId,
                entity.title,
                entity.notes,
                entity.timestamp,
                entity.rangerId,
                entity.authorName,
                entity.syncStatus,
                entity.audioPath,
                entity.videoPath
        );
    }

    private static FirebaseUser resolveCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    private static String resolveAuthorName() {
        FirebaseUser user = resolveCurrentUser();
        return user == null ? null : resolveDisplayName(user);
    }

    private static String resolveDisplayName(FirebaseUser user) {
        if (!TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName();
        }
        if (TextUtils.isEmpty(user.getEmail())) {
            return null;
        }
        int atIndex = user.getEmail().indexOf('@');
        return atIndex > 0 ? user.getEmail().substring(0, atIndex) : user.getEmail();
    }

    private static String coalesce(String preferred, String fallback) {
        return !TextUtils.isEmpty(preferred) ? preferred : fallback;
    }
}
