package com.sbs.data.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sbs.data.AppRepository;
import com.sbs.data.SyncState;
import com.sbs.data.local.SightingEntity;

import java.util.ArrayList;
import java.util.List;

public final class FetchRemoteSightingsWorker extends Worker {

    public FetchRemoteSightingsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<SightingEntity> remote = new ArrayList<>();
            for (QueryDocumentSnapshot doc : Tasks.await(FirebaseFirestore.getInstance().collection("sightings").get())) {
                String rangerId = doc.getString("rangerId");
                if (rangerId == null) {
                    continue;
                }
                long timestamp = doc.getLong("timestamp") == null ? 0L : doc.getLong("timestamp");
                long lastModifiedAt = doc.getLong("lastModifiedAt") == null ? timestamp : doc.getLong("lastModifiedAt");
                remote.add(new SightingEntity(
                        doc.getString("localId") == null ? doc.getId() : doc.getString("localId"),
                        doc.getId(),
                        rangerId,
                        doc.getString("authorName"),
                        doc.getString("title"),
                        doc.getString("notes"),
                        doc.getDouble("lat") == null ? 0.0 : doc.getDouble("lat"),
                        doc.getDouble("lng") == null ? 0.0 : doc.getDouble("lng"),
                        timestamp,
                        doc.getDouble("radius") == null ? 0f : doc.getDouble("radius").floatValue(),
                        doc.getString("audioPath"),
                        doc.getString("imagePath"),
                        doc.getString("videoPath"),
                        SyncState.SYNCED,
                        System.currentTimeMillis(),
                        lastModifiedAt
                ));
            }
            AppRepository.getInstance(getApplicationContext()).mergeRemoteSightings(remote);
            return Result.success();
        } catch (Exception ignored) {
            return Result.retry();
        }
    }
}
