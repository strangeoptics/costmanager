package com.example.costmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseDate: Date,
    val store: String,
    val storeType: String, // Added field
    val totalPrice: Double,
    val photoUri: String? = null
)
