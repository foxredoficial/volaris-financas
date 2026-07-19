package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {

    // --- Transactions ---
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        financeDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }


    // --- Savings Goals ---
    val allGoals: Flow<List<SavingsGoal>> = financeDao.getAllGoals()

    suspend fun insertGoal(goal: SavingsGoal) {
        financeDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingsGoal) {
        financeDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: SavingsGoal) {
        financeDao.deleteGoal(goal)
    }


    // --- Upcoming Bills ---
    val allBills: Flow<List<UpcomingBill>> = financeDao.getAllBills()

    suspend fun insertBill(bill: UpcomingBill) {
        financeDao.insertBill(bill)
    }

    suspend fun updateBill(bill: UpcomingBill) {
        financeDao.updateBill(bill)
    }

    suspend fun deleteBill(bill: UpcomingBill) {
        financeDao.deleteBill(bill)
    }

    // --- Category Budgets ---
    val allBudgets: Flow<List<CategoryBudget>> = financeDao.getAllBudgets()

    suspend fun insertBudget(budget: CategoryBudget) {
        financeDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: CategoryBudget) {
        financeDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: CategoryBudget) {
        financeDao.deleteBudget(budget)
    }

    // --- Bulk Operations for Backup & Restore ---
    suspend fun getTransactionsList(): List<Transaction> = financeDao.getTransactionsList()
    suspend fun getGoalsList(): List<SavingsGoal> = financeDao.getGoalsList()
    suspend fun getBillsList(): List<UpcomingBill> = financeDao.getBillsList()
    suspend fun getBudgetsList(): List<CategoryBudget> = financeDao.getBudgetsList()

    suspend fun restoreData(
        transactions: List<Transaction>,
        goals: List<SavingsGoal>,
        bills: List<UpcomingBill>,
        budgets: List<CategoryBudget>
    ) {
        // Clear all existing data
        financeDao.clearTransactions()
        financeDao.clearGoals()
        financeDao.clearBills()
        financeDao.clearBudgets()
        financeDao.clearUserProfile()

        // Insert new restored lists
        if (transactions.isNotEmpty()) financeDao.insertTransactionsList(transactions)
        if (goals.isNotEmpty()) financeDao.insertGoalsList(goals)
        if (bills.isNotEmpty()) financeDao.insertBillsList(bills)
        if (budgets.isNotEmpty()) financeDao.insertBudgetsList(budgets)
    }

    // --- User Profile (Leveling / Gamification) ---
    val userProfile: Flow<UserProfile?> = financeDao.getUserProfileFlow()

    suspend fun getUserProfile(): UserProfile? = financeDao.getUserProfile()

    suspend fun insertUserProfile(profile: UserProfile) {
        financeDao.insertUserProfile(profile)
    }

    suspend fun clearUserProfile() {
        financeDao.clearUserProfile()
    }

    // --- Weekly Challenges ---
    val weeklyChallenges: Flow<List<WeeklyChallenge>> = financeDao.getWeeklyChallengesFlow()

    suspend fun getWeeklyChallenges(): List<WeeklyChallenge> = financeDao.getWeeklyChallenges()

    suspend fun insertWeeklyChallenge(challenge: WeeklyChallenge) {
        financeDao.insertWeeklyChallenge(challenge)
    }

    suspend fun insertWeeklyChallengesList(challenges: List<WeeklyChallenge>) {
        financeDao.insertWeeklyChallengesList(challenges)
    }

    suspend fun clearWeeklyChallenges() {
        financeDao.clearWeeklyChallenges()
    }
}
