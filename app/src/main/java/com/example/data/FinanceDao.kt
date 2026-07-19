package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)


    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)


    // --- Upcoming Bills ---
    @Query("SELECT * FROM upcoming_bills ORDER BY dueDate ASC")
    fun getAllBills(): Flow<List<UpcomingBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: UpcomingBill)

    @Update
    suspend fun updateBill(bill: UpcomingBill)

    @Delete
    suspend fun deleteBill(bill: UpcomingBill)

    // --- Category Budgets ---
    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)

    @Update
    suspend fun updateBudget(budget: CategoryBudget)

    @Delete
    suspend fun deleteBudget(budget: CategoryBudget)

    // --- Bulk Backup & Restore ---
    @Query("SELECT * FROM transactions")
    suspend fun getTransactionsList(): List<Transaction>

    @Query("SELECT * FROM savings_goals")
    suspend fun getGoalsList(): List<SavingsGoal>

    @Query("SELECT * FROM upcoming_bills")
    suspend fun getBillsList(): List<UpcomingBill>

    @Query("SELECT * FROM category_budgets")
    suspend fun getBudgetsList(): List<CategoryBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionsList(list: List<Transaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalsList(list: List<SavingsGoal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillsList(list: List<UpcomingBill>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetsList(list: List<CategoryBudget>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM savings_goals")
    suspend fun clearGoals()

    @Query("DELETE FROM upcoming_bills")
    suspend fun clearBills()

    @Query("DELETE FROM category_budgets")
    suspend fun clearBudgets()

    // --- User Profile (Leveling / Gamification) ---
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profiles")
    suspend fun clearUserProfile()

    // --- Weekly Challenges ---
    @Query("SELECT * FROM weekly_challenges")
    fun getWeeklyChallengesFlow(): Flow<List<WeeklyChallenge>>

    @Query("SELECT * FROM weekly_challenges")
    suspend fun getWeeklyChallenges(): List<WeeklyChallenge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyChallenge(challenge: WeeklyChallenge)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyChallengesList(challenges: List<WeeklyChallenge>)

    @Query("DELETE FROM weekly_challenges")
    suspend fun clearWeeklyChallenges()
}
