package com.example.ebookmanager.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun fileSize(bytes: Long): String {
        if (bytes <= 0) return "大小未知"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return if (unit == 0) {
            "$bytes B"
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
        }
    }

    fun dateTime(millis: Long): String {
        if (millis <= 0) return "未知"
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
    }
}
