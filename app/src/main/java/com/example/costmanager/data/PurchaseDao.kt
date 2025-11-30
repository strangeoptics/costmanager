package com.example.costmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

data class PurchaseWithPositions(
    @Embedded val purchase: Purchase,
    @Relation(
        parentColumn = "id",
        entityColumn = "purchaseId"
    )
    val positions: List<Position>
)

@Dao
interface PurchaseDao {
    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun getAllPurchasesWithPositions(): Flow<List<PurchaseWithPositions>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    fun getPurchaseWithPositions(purchaseId: Long): Flow<PurchaseWithPositions>

    @Transaction
    @Query("SELECT * FROM purchases WHERE id = :purchaseId")
    suspend fun getPurchaseWithPositionsNow(purchaseId: Long): PurchaseWithPositions?

    @Transaction
    @Query("SELECT * FROM purchases WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    suspend fun getPurchasesWithPositionsBetween(startDate: Date, endDate: Date): List<PurchaseWithPositions>


    @Insert
    suspend fun insertPurchase(purchase: Purchase): Long

    @Insert
    suspend fun insertPositions(positions: List<Position>)

    @Update
    suspend fun updatePurchase(purchase: Purchase)

    @Update
    suspend fun updatePosition(position: Position)

    @Query("UPDATE purchases SET purchaseDate = :newDate WHERE id = :purchaseId")
    suspend fun updatePurchaseDate(purchaseId: Long, newDate: Date)

    @Query("UPDATE purchases SET photoUri = :photoUri WHERE id = :purchaseId")
    suspend fun updatePhotoUri(purchaseId: Long, photoUri: String)

    @Delete
    suspend fun deletePurchase(purchase: Purchase)

    @Delete
    suspend fun deletePosition(position: Position)

    @Transaction
    suspend fun insertPurchaseWithPositions(purchase: Purchase, positions: List<Position>): Long {
        val purchaseId = insertPurchase(purchase)
        val positionsWithPurchaseId = positions.map { it.copy(purchaseId = purchaseId) }
        insertPositions(positionsWithPurchaseId)
        return purchaseId
    }
}