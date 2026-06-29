package com.example.oknakanban.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: OrderStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): OrderStatus = OrderStatus.valueOf(value)
}
