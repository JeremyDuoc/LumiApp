package com.jeremy.lumi.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jeremy.lumi.data.local.database.entity.BondLinkEntity
import com.jeremy.lumi.data.local.database.entity.BondsGestureEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para persistencia local de vínculos y gestos de Bonds.
 */
@Dao
interface BondsDao {
    @Query("SELECT * FROM bonds_links ORDER BY updatedAtMs DESC")
    fun getAllBonds(): Flow<List<BondLinkEntity>>

    @Query("SELECT * FROM bonds_links WHERE linkId = :linkId LIMIT 1")
    suspend fun getBondById(linkId: String): BondLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBond(bond: BondLinkEntity)

    @Query("DELETE FROM bonds_links WHERE linkId = :linkId")
    suspend fun deleteBond(linkId: String)

    @Query("SELECT * FROM bonds_gestures WHERE linkId = :linkId ORDER BY timestampMs DESC")
    fun getGesturesForBond(linkId: String): Flow<List<BondsGestureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGesture(gesture: BondsGestureEntity)
}
