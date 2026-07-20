package com.volaris.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return format.format(amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getDaysRemaining(dueDate: Long): Long {
    val dueCal = Calendar.getInstance().apply {
        timeInMillis = dueDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diff = dueCal.timeInMillis - todayCal.timeInMillis
    return diff / (24 * 60 * 60 * 1000)
}

fun formatCurrencyInput(input: String): String {
    val clean = input.replace(Regex("[^\\d]"), "")
    if (clean.isEmpty()) return ""
    val parsed = clean.toDoubleOrNull() ?: 0.0
    val doubleValue = parsed / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return format.format(doubleValue)
}

fun parseCurrencyInput(input: String): Double {
    val clean = input.replace(Regex("[^\\d]"), "")
    if (clean.isEmpty()) return 0.0
    val parsed = clean.toDoubleOrNull() ?: 0.0
    return parsed / 100.0
}

fun formatInputDate(input: String): String {
    val clean = input.replace(Regex("[^\\d]"), "").take(8)
    
    // Validate / Cap the day if we have at least 2 digits
    var validatedClean = clean
    if (clean.length >= 2) {
        val day = clean.substring(0, 2).toIntOrNull()
        if (day != null && day > 31) {
            validatedClean = "31" + clean.substring(2)
        }
    }
    // Validate / Cap the month if we have at least 4 digits
    if (validatedClean.length >= 4) {
        val month = validatedClean.substring(2, 4).toIntOrNull()
        if (month != null && month > 12) {
            validatedClean = validatedClean.substring(0, 2) + "12" + validatedClean.substring(4)
        }
    }

    val sb = java.lang.StringBuilder()
    for (i in validatedClean.indices) {
        sb.append(validatedClean[i])
        if (i == 1) {
            sb.append("/")
        } else if (i == 3) {
            sb.append("/")
        }
    }

    var result = sb.toString()
    if (validatedClean.length == 1) {
        result = validatedClean
    } else if (validatedClean.length == 2) {
        if (!input.endsWith("/")) {
            result = validatedClean
        }
    } else if (validatedClean.length == 3) {
        result = result.substring(0, 4)
    } else if (validatedClean.length == 4) {
        if (!input.endsWith("/")) {
            result = result.substring(0, 5)
        }
    }
    return result
}

fun formatInputTime(input: String): String {
    val clean = input.replace(Regex("[^\\d]"), "").take(4)
    
    // Validate / Cap hour (max 23)
    var validatedClean = clean
    if (clean.length >= 2) {
        val hour = clean.substring(0, 2).toIntOrNull()
        if (hour != null && hour > 23) {
            validatedClean = "23" + clean.substring(2)
        }
    }
    // Validate / Cap minute (max 59)
    if (validatedClean.length >= 4) {
        val minute = validatedClean.substring(2, 4).toIntOrNull()
        if (minute != null && minute > 59) {
            validatedClean = validatedClean.substring(0, 2) + "59"
        }
    }

    val sb = java.lang.StringBuilder()
    for (i in validatedClean.indices) {
        sb.append(validatedClean[i])
        if (i == 1) {
            sb.append(":")
        }
    }

    var result = sb.toString()
    if (validatedClean.length == 1) {
        result = validatedClean
    } else if (validatedClean.length == 2) {
        if (!input.endsWith(":")) {
            result = validatedClean
        }
    }
    return result
}

fun parseDateTime(dateStr: String, timeStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Salário" -> Icons.Rounded.AttachMoney
        "Investimentos" -> Icons.Rounded.Timeline
        "Freelance" -> Icons.Rounded.WorkOutline
        "Presente" -> Icons.Rounded.CardGiftcard
        "Alimentação" -> Icons.Rounded.Restaurant
        "Transporte" -> Icons.Rounded.DirectionsCar
        "Moradia" -> Icons.Rounded.Home
        "Lazer" -> Icons.Rounded.TheaterComedy
        "Saúde" -> Icons.Rounded.MedicalServices
        "Educação" -> Icons.Rounded.School
        "Mercado" -> Icons.Rounded.ShoppingCart
        else -> Icons.Rounded.Category
    }
}

