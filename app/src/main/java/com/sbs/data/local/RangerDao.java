package com.sbs.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface RangerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(RangerEntity ranger);

    @Query("SELECT COUNT(*) FROM rangers WHERE rangerId = :rangerId")
    int countById(String rangerId);
}
