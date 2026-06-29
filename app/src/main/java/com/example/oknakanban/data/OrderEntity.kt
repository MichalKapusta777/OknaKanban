package com.example.oknakanban.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientName: String,
    val address: String,
    val realizationDate: String,
    val description: String,
    val status: OrderStatus = OrderStatus.NEW
)
