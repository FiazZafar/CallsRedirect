package com.fiver.clientapp.helpers

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "call_history")
data class CallHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerId: String,
    val timestamp: Long,
    val duration: Long,
    val status: String
)

@Dao
interface CallHistoryDao {
    @Insert
    suspend fun insert(callHistory: CallHistory)

    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getAll(): LiveData<List<CallHistory>>
}