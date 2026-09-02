package com.ferforastieri.valkyris.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="events") data class EventEntity(@PrimaryKey val id:String,val cameraId:String,val type:String,val confidence:Double,val occurredAt:String,val snapshotPath:String?,val clipPath:String?,val acknowledgedAt:String?)
@Entity(tableName="rules") data class RuleEntity(@PrimaryKey val id:String,val cameraId:String,val name:String,val detectorTypes:String,val enabled:Boolean)
@Entity(tableName="pending_actions") data class PendingActionEntity(@PrimaryKey val id:String,val kind:String,val payload:String,val createdAt:Long)
@Dao interface ValkyrisDao {
    @Query("SELECT * FROM events ORDER BY occurredAt DESC") fun events():Flow<List<EventEntity>>
    @Query("SELECT * FROM rules ORDER BY name") fun rules():Flow<List<RuleEntity>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun replaceEvents(items:List<EventEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun replaceRules(items:List<RuleEntity>)
    @Query("DELETE FROM events") suspend fun clearEvents()
    @Query("SELECT * FROM pending_actions ORDER BY createdAt") suspend fun pendingActions():List<PendingActionEntity>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun savePending(item:PendingActionEntity)
    @Query("DELETE FROM pending_actions WHERE id=:id") suspend fun deletePending(id:String)
    @Transaction suspend fun syncEvents(items:List<EventEntity>){clearEvents();replaceEvents(items)}
}
@Database(entities=[EventEntity::class,RuleEntity::class,PendingActionEntity::class],version=3,exportSchema=true)
abstract class ValkyrisDatabase:RoomDatabase(){abstract fun dao():ValkyrisDao}
