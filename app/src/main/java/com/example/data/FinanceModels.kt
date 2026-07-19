package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val isPaid: Boolean = true,
    val tags: String = ""
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val monthYear: String // e.g., "07/2026"
)

@Entity(tableName = "upcoming_bills")
data class UpcomingBill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val category: String,
    val isPaid: Boolean = false
)

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val limitAmount: Double
)

object FinanceCategories {
    val incomes = listOf("Salário", "Investimentos", "Freelance", "Presente", "Outros")
    val expenses = listOf("Alimentação", "Transporte", "Moradia", "Lazer", "Saúde", "Educação", "Mercado", "Outros")
}

data class BudgetAlert(
    val category: String,
    val percentage: Double,
    val spent: Double,
    val limit: Double,
    val isCritical: Boolean
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Int = 0,
    val xpNextLevel: Int = 100,
    val streakDays: Int = 1,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val achievementsUnlocked: String = "", // Comma-separated list
    val financialTitle: String = "Iniciante Consciente"
)

@Entity(tableName = "weekly_challenges")
data class WeeklyChallenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val targetValue: Double,
    val currentValue: Double,
    val xpBonus: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val challengeType: String, // "DELIVERY", "SAVINGS", "BUDGET_LIMIT", "BILL_PAYMENT"
    val deadlineTimestamp: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) // 1 week from now
)



