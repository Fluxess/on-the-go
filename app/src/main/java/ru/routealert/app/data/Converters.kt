package ru.routealert.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAlertType(value: AlertType): String = value.name

    @TypeConverter
    fun toAlertType(value: String): AlertType =
        runCatching { AlertType.valueOf(value) }.getOrDefault(AlertType.BOTH)
}
