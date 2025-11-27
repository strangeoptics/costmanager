package com.example.costmanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "positions",
    foreignKeys = [ForeignKey(
        entity = Purchase::class,
        parentColumns = ["id"],
        childColumns = ["purchaseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["purchaseId"])]
)
data class Position(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long,
    val itemName: String,
    val itemType: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val price: Double
)
