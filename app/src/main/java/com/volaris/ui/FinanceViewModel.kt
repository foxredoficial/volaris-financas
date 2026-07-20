package com.volaris.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.volaris.data.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    // --- User Profile & Leveling ---
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val levelUpCelebration = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            val profile = repository.getUserProfile()
            if (profile == null) {
                repository.insertUserProfile(UserProfile(lastActiveTimestamp = System.currentTimeMillis()))
            } else {
                checkDailyStreak(profile)
            }
            initializeWeeklyChallengesIfEmpty()
        }
    }

    private suspend fun checkDailyStreak(profile: UserProfile) {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calLast = Calendar.getInstance().apply { timeInMillis = profile.lastActiveTimestamp }
        
        // Reset to midnight for exact calendar date comparisons
        calNow.set(Calendar.HOUR_OF_DAY, 0)
        calNow.set(Calendar.MINUTE, 0)
        calNow.set(Calendar.SECOND, 0)
        calNow.set(Calendar.MILLISECOND, 0)
        
        calLast.set(Calendar.HOUR_OF_DAY, 0)
        calLast.set(Calendar.MINUTE, 0)
        calLast.set(Calendar.SECOND, 0)
        calLast.set(Calendar.MILLISECOND, 0)
        
        val daysDiff = ((calNow.timeInMillis - calLast.timeInMillis) / oneDayMillis).toInt()
        
        if (daysDiff == 1) {
            val newStreak = profile.streakDays + 1
            val xpBonus = 30 + (newStreak * 5)
            val updatedProfile = profile.copy(
                streakDays = newStreak,
                lastActiveTimestamp = now
            )
            repository.insertUserProfile(updatedProfile)
            gainXpDirect(xpBonus, updatedProfile)

            if (newStreak >= 3) unlockAchievementSuspend("STREAK_3")
            if (newStreak >= 7) unlockAchievementSuspend("STREAK_7")
            if (newStreak >= 15) unlockAchievementSuspend("STREAK_15")
        } else if (daysDiff > 1) {
            val updatedProfile = profile.copy(
                streakDays = 1,
                lastActiveTimestamp = now
            )
            repository.insertUserProfile(updatedProfile)
            gainXpDirect(20, updatedProfile)
        } else if (daysDiff == 0) {
            val updatedProfile = profile.copy(
                lastActiveTimestamp = now
            )
            repository.insertUserProfile(updatedProfile)
        }
    }

    fun gainXp(amount: Int) {
        viewModelScope.launch {
            val profile = repository.getUserProfile() ?: UserProfile()
            gainXpDirect(amount, profile)
        }
    }

    private suspend fun gainXpDirect(amount: Int, currentProfile: UserProfile) {
        var xp = currentProfile.xp + amount
        var level = currentProfile.level
        var xpNextLevel = currentProfile.xpNextLevel
        var leveledUp = false
        
        while (xp >= xpNextLevel) {
            xp -= xpNextLevel
            level += 1
            xpNextLevel = 150 + (level * 100)
            leveledUp = true
        }
        
        val newTitle = when {
            level >= 60 -> "Deus das Finanças ⚡"
            level >= 55 -> "Mestre Supremo de Ativos 👑"
            level >= 50 -> "Oráculo da Independência 🌌"
            level >= 45 -> "Dominador de Mercado 🌊"
            level >= 40 -> "Lorde dos Ativos 🏰"
            level >= 35 -> "Soberano dos Dividendos 🍇"
            level >= 30 -> "Estrategista Implacável ♟️"
            level >= 28 -> "Líder de Patrimônio 🏔️"
            level >= 25 -> "Grão-Mestre das Finanças 💎"
            level >= 24 -> "Visionário Financeiro 👁️"
            level >= 22 -> "Multiplicador de Capital 🧪"
            level >= 20 -> "Investidor Lendário 👑"
            level >= 19 -> "Estrategista de Metas 🗺️"
            level >= 18 -> "Acumulador de Ativos 🏦"
            level >= 17 -> "Poupador Ouro 🥇"
            level >= 16 -> "Analista de Gastos 📊"
            level >= 15 -> "Mago da Poupança 🧙‍♂️"
            level >= 14 -> "Mestre das Contas 🧾"
            level >= 13 -> "Investidor Novato 🎯"
            level >= 12 -> "Caçador de Descontos 🏷️"
            level >= 11 -> "Poupador Prata 🥈"
            level >= 10 -> "Guardião do Orçamento ⚔️"
            level >= 9 -> "Defensor do Saldo 🛡️"
            level >= 8 -> "Consumidor Inteligente 🧠"
            level >= 7 -> "Fugitivo dos Juros 💸"
            level >= 6 -> "Organizador Eficiente 🗂️"
            level >= 5 -> "Planejador Consciente 📈"
            level >= 4 -> "Poupador Iniciante 🐷"
            level >= 3 -> "Aprendiz de Orçamento 📝"
            level >= 2 -> "Iniciante Consciente 🌱"
            else -> "Recruta da Poupança 🪙"
        }

        val achievements = currentProfile.achievementsUnlocked.split(",").filter { it.isNotEmpty() }.toMutableList()
        
        if (level >= 5 && !achievements.contains("LEVEL_5")) achievements.add("LEVEL_5")
        if (level >= 10 && !achievements.contains("LEVEL_10")) achievements.add("LEVEL_10")
        if (level >= 15 && !achievements.contains("LEVEL_15")) achievements.add("LEVEL_15")
        if (level >= 20 && !achievements.contains("LEVEL_20")) achievements.add("LEVEL_20")
        if (level >= 25 && !achievements.contains("LEVEL_25")) achievements.add("LEVEL_25")
        if (level >= 30 && !achievements.contains("LEVEL_30")) achievements.add("LEVEL_30")
        if (level >= 40 && !achievements.contains("LEVEL_40")) achievements.add("LEVEL_40")
        if (level >= 50 && !achievements.contains("LEVEL_50")) achievements.add("LEVEL_50")

        val updatedProfile = currentProfile.copy(
            level = level,
            xp = xp,
            xpNextLevel = xpNextLevel,
            financialTitle = newTitle,
            achievementsUnlocked = achievements.joinToString(",")
        )
        
        repository.insertUserProfile(updatedProfile)
        
        if (leveledUp) {
            levelUpCelebration.value = level
        }
    }

    fun unlockAchievement(achievementId: String) {
        viewModelScope.launch {
            val profile = repository.getUserProfile() ?: UserProfile()
            val list = profile.achievementsUnlocked.split(",").filter { it.isNotEmpty() }.toMutableSet()
            if (!list.contains(achievementId)) {
                list.add(achievementId)
                val updated = profile.copy(achievementsUnlocked = list.joinToString(","))
                repository.insertUserProfile(updated)
                gainXpDirect(50, updated)
            }
        }
    }

    fun completeQuiz(score: Int) {
        viewModelScope.launch {
            val xpGained = 20 + (score * 15)
            gainXp(xpGained)
            if (score == 5) {
                unlockAchievement("QUIZ_MASTER")
            } else if (score >= 3) {
                unlockAchievement("QUIZ_PASS")
            }
        }
    }

    private suspend fun gainXpSuspend(amount: Int) {
        val profile = repository.getUserProfile() ?: UserProfile()
        gainXpDirect(amount, profile)
    }

    private suspend fun unlockAchievementSuspend(achievementId: String) {
        val profile = repository.getUserProfile() ?: UserProfile()
        val list = profile.achievementsUnlocked.split(",").filter { it.isNotEmpty() }.toMutableSet()
        if (!list.contains(achievementId)) {
            list.add(achievementId)
            val updated = profile.copy(achievementsUnlocked = list.joinToString(","))
            repository.insertUserProfile(updated)
            gainXpDirect(50, updated)
        }
    }

    private suspend fun initializeWeeklyChallengesIfEmpty() {
        val current = repository.getWeeklyChallenges()
        val now = System.currentTimeMillis()
        if (current.size < 25 || current.any { it.deadlineTimestamp < now }) {
            repository.clearWeeklyChallenges()
            val nextWeek = now + (7 * 24 * 60 * 60 * 1000L)
            val defaultChallenges = listOf(
                WeeklyChallenge(
                    title = "Delivery Controlado 🍕",
                    description = "Registre uma despesa de até R$ 35 em Delivery ou Restaurante.",
                    targetValue = 35.0,
                    currentValue = 0.0,
                    xpBonus = 120,
                    challengeType = "DELIVERY",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Chef de Cozinha 🍳",
                    description = "Registre uma refeição econômica de no máximo R$ 25 (Alimentação ou Mercado).",
                    targetValue = 25.0,
                    currentValue = 0.0,
                    xpBonus = 100,
                    challengeType = "CHEF_COZINHA",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Poupança Firme 🎯",
                    description = "Deposite pelo menos R$ 50 acumulados em suas metas de poupança.",
                    targetValue = 50.0,
                    currentValue = 0.0,
                    xpBonus = 150,
                    challengeType = "SAVINGS",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Super Poupador 🚀",
                    description = "Guarde R$ 150 ou mais em suas metas de poupança para turbinar o futuro.",
                    targetValue = 150.0,
                    currentValue = 0.0,
                    xpBonus = 250,
                    challengeType = "SUPER_SAVINGS",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Orçamento Inteligente 🛡️",
                    description = "Defina um teto de gastos (limite de orçamento) para qualquer categoria.",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 100,
                    challengeType = "BUDGET_LIMIT",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Mestre dos Tetos 📊",
                    description = "Configure teto de gastos (orçamento) para 3 ou mais categorias.",
                    targetValue = 3.0,
                    currentValue = 0.0,
                    xpBonus = 200,
                    challengeType = "BUDGET_LIMIT_3",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Compromisso em Dia 🧾",
                    description = "Efetue o pagamento de pelo menos 1 conta futura.",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 80,
                    challengeType = "BILL_PAYMENT",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Faturamento Extra 💰",
                    description = "Registre receitas acumuladas (salário, freelance, investimentos) de R$ 100+.",
                    targetValue = 100.0,
                    currentValue = 0.0,
                    xpBonus = 150,
                    challengeType = "INCOME_LOG",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Hábitos de Registro ✍️",
                    description = "Mantenha a constância registrando pelo menos 5 movimentações nesta semana.",
                    targetValue = 5.0,
                    currentValue = 0.0,
                    xpBonus = 120,
                    challengeType = "TRANSACTION_COUNT",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Lazer Consciente 🍿",
                    description = "Registre uma despesa de Lazer de no máximo R$ 40 para evitar desperdícios.",
                    targetValue = 40.0,
                    currentValue = 0.0,
                    xpBonus = 110,
                    challengeType = "EXPENSE_CONTROL",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Dia Sem Compras 🚫",
                    description = "Passe um dia inteiro sem compras supérfluas (toque abaixo para declarar!).",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 120,
                    challengeType = "NO_SPEND_DAY",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Antecipação de Faturas ⚡",
                    description = "Pague uma conta futura com antecedência (pelo menos 1 dia antes do prazo).",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 140,
                    challengeType = "EARLY_BILL",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Foco no Futuro 📈",
                    description = "Deposite pelo menos R$ 10 acumulados na categoria de Investimentos.",
                    targetValue = 10.0,
                    currentValue = 0.0,
                    xpBonus = 150,
                    challengeType = "INVESTMENT_LOG",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Mestre dos Investimentos 💎",
                    description = "Faça um aporte em Investimentos de R$ 150 ou mais para acelerar seu futuro.",
                    targetValue = 150.0,
                    currentValue = 0.0,
                    xpBonus = 250,
                    challengeType = "BIG_INVESTMENT",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Supermercado Consciente 🛒",
                    description = "Faça compras inteligentes registrando um Mercado menor ou igual a R$ 100.",
                    targetValue = 100.0,
                    currentValue = 0.0,
                    xpBonus = 100,
                    challengeType = "GROCERY_CONTROL",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Transporte Econômico 🚗",
                    description = "Economize no transporte registrando uma corrida ou passagem de no máximo R$ 25.",
                    targetValue = 25.0,
                    currentValue = 0.0,
                    xpBonus = 80,
                    challengeType = "TRANSPORT_CONTROL",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Estudos em Primeiro Lugar 📚",
                    description = "Invista em si mesmo registrando pelo menos 1 despesa em Educação ou Saúde.",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 100,
                    challengeType = "EDUCATION_LOG",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Reserva Protegida 🛡️",
                    description = "Deposite R$ 100 ou mais em uma poupança contendo 'Reserva' no nome.",
                    targetValue = 100.0,
                    currentValue = 0.0,
                    xpBonus = 180,
                    challengeType = "EMERGENCY_FUND",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Meta Dupla 👥",
                    description = "Contribua para duas metas de poupança diferentes nesta semana.",
                    targetValue = 2.0,
                    currentValue = 0.0,
                    xpBonus = 160,
                    challengeType = "DOUBLE_GOAL",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Freelancer Ativo 💼",
                    description = "Fature uma grana extra registrando pelo menos 1 receita de Freelance.",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 110,
                    challengeType = "FREELANCE_INCOME",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Lazer Inteligente 🍿",
                    description = "Evite luxos registrando um Lazer menor ou igual a R$ 20 nesta semana.",
                    targetValue = 20.0,
                    currentValue = 0.0,
                    xpBonus = 90,
                    challengeType = "LEISURE_STRICT",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Consultoria com a IA 🤖",
                    description = "Abra e peça conselhos ou faça perguntas ao Consultor Financeiro IA.",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 80,
                    challengeType = "AI_ADVICE_CHECK",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Quiz de Educação 📝",
                    description = "Participe e complete as 5 perguntas do Quiz de Educação Financeira.",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 100,
                    challengeType = "QUIZ_COMPLETE",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Análise de Fim de Mês 📊",
                    description = "Utilize a IA para gerar e analisar o Resumo Mensal IA de suas finanças.",
                    targetValue = 1.0,
                    currentValue = 0.0,
                    xpBonus = 100,
                    challengeType = "MONTHLY_SUMMARY_CHECK",
                    deadlineTimestamp = nextWeek
                ),
                WeeklyChallenge(
                    title = "Boletos Liquidados 🧾",
                    description = "Mantenha o controle de suas despesas pagando 2 ou mais contas no planejador.",
                    targetValue = 2.0,
                    currentValue = 0.0,
                    xpBonus = 130,
                    challengeType = "BILL_COUNT",
                    deadlineTimestamp = nextWeek
                )
            )
            repository.insertWeeklyChallengesList(defaultChallenges)
        }
    }

    private suspend fun updateChallengeProgress(type: String, valueToAdd: Double, isDirectSet: Boolean = false) {
        val challenges = repository.getWeeklyChallenges()
        val now = System.currentTimeMillis()
        challenges.forEach { challenge ->
            if (challenge.challengeType == type && !challenge.isClaimed && challenge.deadlineTimestamp > now) {
                val newValue = if (isDirectSet) {
                    valueToAdd
                } else {
                    challenge.currentValue + valueToAdd
                }
                
                val finalValue = newValue.coerceAtMost(challenge.targetValue)
                val isCompletedNow = finalValue >= challenge.targetValue
                
                val updated = challenge.copy(
                    currentValue = finalValue,
                    isCompleted = isCompletedNow
                )
                repository.insertWeeklyChallenge(updated)
            }
        }
    }

    fun claimChallengeReward(challengeId: Int) {
        viewModelScope.launch {
            val challenges = repository.getWeeklyChallenges()
            val challenge = challenges.find { it.id == challengeId }
            if (challenge != null && challenge.isCompleted && !challenge.isClaimed) {
                val updated = challenge.copy(isClaimed = true)
                repository.insertWeeklyChallenge(updated)
                gainXp(challenge.xpBonus)
                unlockAchievement("CHALLENGE_COMPLETED")

                val claimedCount = challenges.count { it.isClaimed } + 1
                if (claimedCount >= 5) {
                    unlockAchievement("CHALLENGE_MASTER")
                }
            }
        }
    }

    fun triggerChallengeProgress(type: String, valueToAdd: Double) {
        viewModelScope.launch {
            updateChallengeProgress(type, valueToAdd)
        }
    }

    // --- State Observables ---
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyChallenges: StateFlow<List<WeeklyChallenge>> = repository.weeklyChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<SavingsGoal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<UpcomingBill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<CategoryBudget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null means follow system
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // --- Computed Values ---
    val totalIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { !it.isExpense && it.isPaid }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions.map { list ->
        list.filter { it.isExpense && it.isPaid }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- App theme configuration ---
    fun toggleDarkMode(currentSystemDark: Boolean) {
        val current = _isDarkMode.value ?: currentSystemDark
        _isDarkMode.value = !current
    }

    fun useSystemTheme() {
        _isDarkMode.value = null
    }

    // --- Transaction Actions ---
    fun addTransaction(
        title: String,
        amount: Double,
        isExpense: Boolean,
        category: String,
        date: Long,
        description: String = "",
        isPaid: Boolean = true,
        tags: String = ""
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    title = title,
                    amount = amount,
                    isExpense = isExpense,
                    category = category,
                    date = date,
                    description = description,
                    isPaid = isPaid,
                    tags = tags
                )
            )
            gainXpSuspend(15)
            unlockAchievementSuspend("FIRST_TRANSACTION")

            // 1. Transaction Count challenge
            updateChallengeProgress("TRANSACTION_COUNT", 1.0)

            // Check if they reached 10 transactions overall
            val transCount = transactions.value.size + 1
            if (transCount >= 10) {
                unlockAchievementSuspend("MULTIPLE_TRANSACTIONS")
            }

            // 2. Delivery Controlado challenge
            if (isExpense && (category.equals("Delivery", ignoreCase = true) || 
                             category.equals("Alimentação", ignoreCase = true) || 
                             category.equals("Restaurante", ignoreCase = true))) {
                if (amount <= 35.0) {
                    updateChallengeProgress("DELIVERY", 35.0, isDirectSet = true)
                }
            }

            // 3. Chef de Cozinha challenge
            if (isExpense && (category.equals("Alimentação", ignoreCase = true) || 
                             category.equals("Mercado", ignoreCase = true) || 
                             category.equals("Restaurante", ignoreCase = true))) {
                if (amount <= 25.0) {
                    updateChallengeProgress("CHEF_COZINHA", 25.0, isDirectSet = true)
                }
            }

            // 4. Lazer Consciente challenge
            if (isExpense && category.equals("Lazer", ignoreCase = true)) {
                if (amount <= 40.0) {
                    updateChallengeProgress("EXPENSE_CONTROL", 40.0, isDirectSet = true)
                }
            }

            // 5. Income Log challenge
            if (!isExpense) {
                updateChallengeProgress("INCOME_LOG", amount)
            }

            // New 11. Dia Sem Compras challenge (handled by button click in UI or transaction checks)

            // New 13. Foco no Futuro challenge
            if (category.equals("Investimentos", ignoreCase = true)) {
                updateChallengeProgress("INVESTMENT_LOG", amount)
            }

            // New 14. Mestre dos Investimentos challenge
            if (category.equals("Investimentos", ignoreCase = true) && amount >= 150.0) {
                updateChallengeProgress("BIG_INVESTMENT", amount)
            }

            // New 15. Supermercado Consciente challenge
            if (isExpense && category.equals("Mercado", ignoreCase = true)) {
                if (amount <= 100.0) {
                    updateChallengeProgress("GROCERY_CONTROL", 100.0, isDirectSet = true)
                }
            }

            // New 16. Transporte Econômico challenge
            if (isExpense && category.equals("Transporte", ignoreCase = true)) {
                if (amount <= 25.0) {
                    updateChallengeProgress("TRANSPORT_CONTROL", 25.0, isDirectSet = true)
                }
            }

            // New 17. Estudos em Primeiro Lugar challenge
            if (isExpense && (category.equals("Educação", ignoreCase = true) || category.equals("Saúde", ignoreCase = true))) {
                updateChallengeProgress("EDUCATION_LOG", 1.0)
            }

            // New 20. Freelancer Ativo challenge
            if (!isExpense && category.equals("Freelance", ignoreCase = true)) {
                updateChallengeProgress("FREELANCE_INCOME", 1.0)
            }

            // New 21. Lazer Inteligente challenge
            if (isExpense && category.equals("Lazer", ignoreCase = true)) {
                if (amount <= 20.0) {
                    updateChallengeProgress("LEISURE_STRICT", 20.0, isDirectSet = true)
                }
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    // --- Savings Goal Actions ---
    fun addGoal(name: String, targetAmount: Double, currentAmount: Double, monthYear: String) {
        viewModelScope.launch {
            repository.insertGoal(
                SavingsGoal(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    monthYear = monthYear
                )
            )
            gainXpSuspend(20)
            unlockAchievementSuspend("FIRST_GOAL")

            if (targetAmount >= 1000.0) {
                unlockAchievementSuspend("BIG_SAVER")
            }
        }
    }

    fun updateGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            val oldGoal = goals.value.find { it.id == goal.id }
            val diff = if (oldGoal != null) {
                (goal.currentAmount - oldGoal.currentAmount).coerceAtMost(goal.targetAmount).coerceAtLeast(0.0)
            } else {
                0.0
            }
            repository.updateGoal(goal)
            if (diff > 0.0) {
                updateChallengeProgress("SAVINGS", diff)
                updateChallengeProgress("SUPER_SAVINGS", diff)
                if (goal.name.contains("Reserva", ignoreCase = true)) {
                    updateChallengeProgress("EMERGENCY_FUND", diff)
                }
                updateChallengeProgress("DOUBLE_GOAL", 1.0)
            }
            if (goal.currentAmount >= goal.targetAmount) {
                gainXpSuspend(100)
                unlockAchievementSuspend("GOAL_COMPLETED")
            } else {
                gainXpSuspend(15)
            }
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // --- Upcoming Bill Actions ---
    fun addBill(title: String, amount: Double, dueDate: Long, category: String) {
        viewModelScope.launch {
            repository.insertBill(
                UpcomingBill(
                    title = title,
                    amount = amount,
                    dueDate = dueDate,
                    category = category,
                    isPaid = false
                )
            )
            gainXpSuspend(10)
        }
    }

    fun updateBill(bill: UpcomingBill) {
        viewModelScope.launch {
            repository.updateBill(bill)
        }
    }

    fun deleteBill(bill: UpcomingBill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun toggleBillPayment(bill: UpcomingBill) {
        viewModelScope.launch {
            val updatedBill = bill.copy(isPaid = !bill.isPaid)
            repository.updateBill(updatedBill)

            // If the bill was marked as paid, automatically log it as an expense transaction!
            if (updatedBill.isPaid) {
                repository.insertTransaction(
                    Transaction(
                        title = "Pagamento: ${bill.title}",
                        amount = bill.amount,
                        isExpense = true,
                        category = bill.category,
                        date = System.currentTimeMillis(),
                        description = "Vencimento liquidado automaticamente",
                        isPaid = true
                    )
                )
                gainXpSuspend(25)
                unlockAchievementSuspend("BILL_PAID")
                updateChallengeProgress("BILL_PAYMENT", 1.0)
                updateChallengeProgress("TRANSACTION_COUNT", 1.0)
                if (bill.dueDate > System.currentTimeMillis()) {
                    updateChallengeProgress("EARLY_BILL", 1.0)
                }
                updateChallengeProgress("BILL_COUNT", 1.0)

                val transCount = transactions.value.size + 1
                if (transCount >= 10) {
                    unlockAchievementSuspend("MULTIPLE_TRANSACTIONS")
                }

                val paidBillsCount = bills.value.count { it.isPaid } + 1
                if (paidBillsCount >= 3) {
                    unlockAchievementSuspend("DEBT_FREE")
                }
            }
        }
    }

    // --- Category Budget Actions ---
    fun addBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.insertBudget(CategoryBudget(category = category, limitAmount = limitAmount))
            gainXpSuspend(10)
            unlockAchievementSuspend("BUDGET_CREATED")
            updateChallengeProgress("BUDGET_LIMIT", 1.0)

            val newSize = budgets.value.size + 1
            updateChallengeProgress("BUDGET_LIMIT_3", newSize.toDouble(), isDirectSet = true)
            if (newSize >= 3) {
                unlockAchievementSuspend("BUDGET_MASTER")
            }
        }
    }

    fun updateBudget(budget: CategoryBudget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: CategoryBudget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // --- Report Exporter (Share CSV and Text Summaries) ---
    fun shareCsvReport(context: Context, startDate: Long? = null, endDate: Long? = null, selectedCategories: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val csvFile = File(context.cacheDir, "VolarisFinancas_Planilha.csv")
                val writer = FileWriter(csvFile)
                writer.append("ID;Titulo;Valor (R$);Tipo;Categoria;Data;Pago;Descricao\n")

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                
                var filtered = transactions.value
                if (startDate != null) {
                    filtered = filtered.filter { it.date >= startDate }
                }
                if (endDate != null) {
                    filtered = filtered.filter { it.date <= endDate }
                }
                if (selectedCategories.isNotEmpty()) {
                    filtered = filtered.filter { selectedCategories.contains(it.category) }
                }

                filtered.forEach { t ->
                    val type = if (t.isExpense) "Despesa" else "Receita"
                    val dateStr = sdf.format(Date(t.date))
                    val paidStr = if (t.isPaid) "Sim" else "Nao"
                    writer.append("${t.id};${t.title};${String.format(Locale.US, "%.2f", t.amount)};$type;${t.category};$dateStr;$paidStr;${t.description}\n")
                }
                writer.flush()
                writer.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Volaris Finanças - Planilha de Transações")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Exportar Planilha (CSV)"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shareTextReport(context: Context, startDate: Long? = null, endDate: Long? = null, selectedCategories: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val reportFile = File(context.cacheDir, "VolarisFinancas_Relatorio.txt")
                val writer = FileWriter(reportFile)

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val todayStr = sdf.format(Date())

                writer.append("=========================================\n")
                writer.append("           RELATÓRIO FINANCEIRO          \n")
                writer.append("             VOLARIS FINANÇAS            \n")
                writer.append("=========================================\n")
                writer.append("Gerado em: $todayStr\n")

                if (startDate != null || endDate != null || selectedCategories.isNotEmpty()) {
                    writer.append("Filtros Aplicados:\n")
                    if (startDate != null) writer.append("- Data Inicio: ${sdf.format(Date(startDate))}\n")
                    if (endDate != null) writer.append("- Data Fim: ${sdf.format(Date(endDate))}\n")
                    if (selectedCategories.isNotEmpty()) writer.append("- Categorias: ${selectedCategories.joinToString(", ")}\n")
                } else {
                    writer.append("Filtros Aplicados: Nenhum (Todos os dados)\n")
                }
                writer.append("=========================================\n\n")

                var filtered = transactions.value
                if (startDate != null) {
                    filtered = filtered.filter { it.date >= startDate }
                }
                if (endDate != null) {
                    filtered = filtered.filter { it.date <= endDate }
                }
                if (selectedCategories.isNotEmpty()) {
                    filtered = filtered.filter { selectedCategories.contains(it.category) }
                }

                val totalInc = filtered.filter { !it.isExpense && it.isPaid }.sumOf { it.amount }
                val totalExp = filtered.filter { it.isExpense && it.isPaid }.sumOf { it.amount }
                val balance = totalInc - totalExp

                writer.append("--- RESUMO GERAL DO PERIODO/FILTRO ---\n")
                writer.append("Total de Receitas: R$ ${String.format(Locale.getDefault(), "%.2f", totalInc)}\n")
                writer.append("Total de Despesas: R$ ${String.format(Locale.getDefault(), "%.2f", totalExp)}\n")
                writer.append("Saldo Liquido: R$ ${String.format(Locale.getDefault(), "%.2f", balance)}\n\n")

                writer.append("--- METAS DE ECONOMIA ---\n")
                if (goals.value.isEmpty()) {
                    writer.append("Nenhuma meta ativa cadastrada.\n\n")
                } else {
                    goals.value.forEach { g ->
                        val pct = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount) * 100 else 0.0
                        writer.append("- ${g.name} (${g.monthYear}): R$ ${String.format(Locale.getDefault(), "%.2f", g.currentAmount)} / R$ ${String.format(Locale.getDefault(), "%.2f", g.targetAmount)} (${String.format(Locale.getDefault(), "%.1f", pct)}%)\n")
                    }
                    writer.append("\n")
                }

                writer.append("--- CONTAS VENCENDO EM BREVE ---\n")
                val pendingBills = bills.value.filter { !it.isPaid }
                if (pendingBills.isEmpty()) {
                    writer.append("Nenhuma conta pendente vencendo em breve.\n\n")
                } else {
                    pendingBills.forEach { b ->
                        writer.append("- ${b.title}: R$ ${String.format(Locale.getDefault(), "%.2f", b.amount)} (Vence em: ${sdf.format(Date(b.dueDate))})\n")
                    }
                    writer.append("\n")
                }

                writer.append("--- DETALHAMENTO DE TRANSACOES FILTRADAS ---\n")
                if (filtered.isEmpty()) {
                    writer.append("Nenhuma transacao registrada para os filtros selecionados.\n")
                } else {
                    filtered.forEach { t ->
                        val sign = if (t.isExpense) "[-]" else "[+]"
                        writer.append("$sign ${sdf.format(Date(t.date))} | ${t.category.padEnd(12)} | ${t.title.padEnd(25)} | R$ ${String.format(Locale.getDefault(), "%.2f", t.amount)}\n")
                    }
                }

                writer.append("\n\n=========================================\n")
                writer.append("Volaris Financas\n")
                writer.append("=========================================\n")

                writer.flush()
                writer.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    reportFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Volaris Finanças - Relatório de Finanças")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Exportar Relatório (PDF / Texto)"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Full JSON Backup & Restore ---
    fun exportBackupJson(context: Context) {
        viewModelScope.launch {
            try {
                val transactionsList = repository.getTransactionsList()
                val goalsList = repository.getGoalsList()
                val billsList = repository.getBillsList()
                val budgetsList = repository.getBudgetsList()

                val rootObj = org.json.JSONObject()
                rootObj.put("backup_version", 1)
                rootObj.put("app_identifier", "volaris_financas")
                rootObj.put("backup_date", System.currentTimeMillis())

                // Serialize Transactions
                val transArray = org.json.JSONArray()
                transactionsList.forEach { t ->
                    val tObj = org.json.JSONObject().apply {
                        put("title", t.title)
                        put("amount", t.amount)
                        put("isExpense", t.isExpense)
                        put("category", t.category)
                        put("date", t.date)
                        put("description", t.description)
                        put("isPaid", t.isPaid)
                    }
                    transArray.put(tObj)
                }
                rootObj.put("transactions", transArray)

                // Serialize Goals
                val goalsArray = org.json.JSONArray()
                goalsList.forEach { g ->
                    val gObj = org.json.JSONObject().apply {
                        put("name", g.name)
                        put("targetAmount", g.targetAmount)
                        put("currentAmount", g.currentAmount)
                        put("monthYear", g.monthYear)
                    }
                    goalsArray.put(gObj)
                }
                rootObj.put("savings_goals", goalsArray)

                // Serialize Bills
                val billsArray = org.json.JSONArray()
                billsList.forEach { b ->
                    val bObj = org.json.JSONObject().apply {
                        put("title", b.title)
                        put("amount", b.amount)
                        put("dueDate", b.dueDate)
                        put("category", b.category)
                        put("isPaid", b.isPaid)
                    }
                    billsArray.put(bObj)
                }
                rootObj.put("upcoming_bills", billsArray)

                // Serialize Budgets
                val budgetsArray = org.json.JSONArray()
                budgetsList.forEach { bd ->
                    val bdObj = org.json.JSONObject().apply {
                        put("category", bd.category)
                        put("limitAmount", bd.limitAmount)
                    }
                    budgetsArray.put(bdObj)
                }
                rootObj.put("category_budgets", budgetsArray)

                // Write file to cache and share
                val backupFile = File(context.cacheDir, "VolarisFinancas_Backup.json")
                val writer = FileWriter(backupFile)
                writer.write(rootObj.toString(2))
                writer.flush()
                writer.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Volaris Finanças - Backup de Dados")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Salvar/Compartilhar Backup (.json)"))
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Erro ao exportar backup: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun importBackupJson(context: Context, jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val rootObj = org.json.JSONObject(jsonString)
                if (!rootObj.has("app_identifier") || rootObj.getString("app_identifier") != "volaris_financas") {
                    onComplete(false)
                    return@launch
                }

                // Parse Transactions
                val transList = mutableListOf<Transaction>()
                if (rootObj.has("transactions")) {
                    val array = rootObj.getJSONArray("transactions")
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        transList.add(
                            Transaction(
                                title = o.getString("title"),
                                amount = o.getDouble("amount"),
                                isExpense = o.getBoolean("isExpense"),
                                category = o.getString("category"),
                                date = o.getLong("date"),
                                description = o.optString("description", ""),
                                isPaid = o.optBoolean("isPaid", true)
                            )
                        )
                    }
                }

                // Parse Goals
                val goalsList = mutableListOf<SavingsGoal>()
                if (rootObj.has("savings_goals")) {
                    val array = rootObj.getJSONArray("savings_goals")
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        goalsList.add(
                            SavingsGoal(
                                name = o.getString("name"),
                                targetAmount = o.getDouble("targetAmount"),
                                currentAmount = o.optDouble("currentAmount", 0.0),
                                monthYear = o.getString("monthYear")
                            )
                        )
                    }
                }

                // Parse Bills
                val billsList = mutableListOf<UpcomingBill>()
                if (rootObj.has("upcoming_bills")) {
                    val array = rootObj.getJSONArray("upcoming_bills")
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        billsList.add(
                            UpcomingBill(
                                title = o.getString("title"),
                                amount = o.getDouble("amount"),
                                dueDate = o.getLong("dueDate"),
                                category = o.getString("category"),
                                isPaid = o.optBoolean("isPaid", false)
                            )
                        )
                    }
                }

                // Parse Budgets
                val budgetsList = mutableListOf<CategoryBudget>()
                if (rootObj.has("category_budgets")) {
                    val array = rootObj.getJSONArray("category_budgets")
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        budgetsList.add(
                            CategoryBudget(
                                category = o.getString("category"),
                                limitAmount = o.getDouble("limitAmount")
                            )
                        )
                    }
                }

                // Restore in Repository
                repository.restoreData(transList, goalsList, billsList, budgetsList)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    // --- Intelligent Budget Alerts ---
    val budgetAlerts: StateFlow<List<BudgetAlert>> = combine(
        repository.allTransactions,
        repository.allBudgets
    ) { transList, budgetsList ->
        val alerts = mutableListOf<BudgetAlert>()
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        val currentYear = cal.get(java.util.Calendar.YEAR)

        // Filter transactions of current month
        val currentMonthExpenses = transList.filter { t ->
            t.isExpense && t.isPaid && {
                val tCal = java.util.Calendar.getInstance()
                tCal.timeInMillis = t.date
                tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear
            }()
        }

        budgetsList.forEach { budget ->
            val spent = currentMonthExpenses.filter { it.category == budget.category }.sumOf { it.amount }
            if (budget.limitAmount > 0.0) {
                val pct = spent / budget.limitAmount
                if (pct >= 0.80) {
                    alerts.add(
                        BudgetAlert(
                            category = budget.category,
                            percentage = pct,
                            spent = spent,
                            limit = budget.limitAmount,
                            isCritical = pct >= 1.0
                        )
                    )
                }
            }
        }
        alerts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Gemini AI Monthly Textual Summary ---
    private val _monthlySummary = MutableStateFlow<String?>(null)
    val monthlySummary: StateFlow<String?> = _monthlySummary.asStateFlow()

    private val _isMonthlySummaryLoading = MutableStateFlow(false)
    val isMonthlySummaryLoading: StateFlow<Boolean> = _isMonthlySummaryLoading.asStateFlow()

    fun getMonthlyTextualSummary(apiKey: String = com.volaris.BuildConfig.GEMINI_API_KEY) {
        if (_isMonthlySummaryLoading.value) return
        _isMonthlySummaryLoading.value = true
        _monthlySummary.value = null

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val transList = repository.getTransactionsList()
                val budgetsList = repository.getBudgetsList()
                val goalsList = repository.getGoalsList()

                val cal = java.util.Calendar.getInstance()
                val currentMonth = cal.get(java.util.Calendar.MONTH)
                val currentYear = cal.get(java.util.Calendar.YEAR)
                
                val currentMonthTrans = transList.filter { t ->
                    val tCal = java.util.Calendar.getInstance()
                    tCal.timeInMillis = t.date
                    tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear
                }

                val totalInc = currentMonthTrans.filter { !it.isExpense }.sumOf { it.amount }
                val totalExp = currentMonthTrans.filter { it.isExpense }.sumOf { it.amount }
                val balance = totalInc - totalExp

                val contextBuilder = java.lang.StringBuilder()
                contextBuilder.append("Você é um analista financeiro pessoal avançado. Analise os dados do mês atual (${currentMonth + 1}/$currentYear) e crie um Relatório de Resumo Mensal detalhado em português (PT-BR):\n\n")
                contextBuilder.append("RESUMO FINANCEIRO DO MÊS:\n")
                contextBuilder.append("- Total Recebido: R$ ${String.format(Locale.US, "%.2f", totalInc)}\n")
                contextBuilder.append("- Total Gasto: R$ ${String.format(Locale.US, "%.2f", totalExp)}\n")
                contextBuilder.append("- Saldo Líquido: R$ ${String.format(Locale.US, "%.2f", balance)}\n\n")

                contextBuilder.append("GASTOS DETALHADOS POR CATEGORIA NESTE MÊS:\n")
                val expByCategory = currentMonthTrans.filter { it.isExpense }.groupBy { it.category }
                if (expByCategory.isEmpty()) {
                    contextBuilder.append("Sem despesas registradas neste mês.\n")
                } else {
                    expByCategory.forEach { (cat, list) ->
                        val sum = list.sumOf { it.amount }
                        val budgetLimit = budgetsList.find { it.category == cat }?.limitAmount ?: 0.0
                        if (budgetLimit > 0.0) {
                            contextBuilder.append("- $cat: R$ ${String.format(Locale.US, "%.2f", sum)} (Limite Orçado: R$ ${String.format(Locale.US, "%.2f", budgetLimit)})\n")
                        } else {
                            contextBuilder.append("- $cat: R$ ${String.format(Locale.US, "%.2f", sum)}\n")
                        }
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("METAS FINANCEIRAS:\n")
                if (goalsList.isEmpty()) {
                    contextBuilder.append("Nenhuma meta ativa.\n")
                } else {
                    goalsList.forEach { g ->
                        contextBuilder.append("- ${g.name}: Alvo R$ ${g.targetAmount} (Atual: R$ ${g.currentAmount})\n")
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("Por favor, estruture sua resposta com os seguintes tópicos formatados em Markdown amigável e refinado:\n")
                contextBuilder.append("1. **Análise de Saúde Financeira**: Uma avaliação honesta do saldo e dos hábitos deste mês.\n")
                contextBuilder.append("2. **Identificação de Padrões e Alertas**: Quais categorias pesaram mais no bolso ou estão estourando orçamentos.\n")
                contextBuilder.append("3. **Recomendações e Dicas de Economia Personalizadas**: 3 ações concretas e personalizadas para economizar este mês baseado nos dados acima e no progresso das metas.\n")
                contextBuilder.append("\nSeja direto, encorajador e evite introduções formais longas. Vá direto ao ponto.")

                val prompt = contextBuilder.toString()

                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(1500)
                    _monthlySummary.value = generateLocalOfflineMonthlySummary(totalInc, totalExp, balance, expByCategory, budgetsList, goalsList)
                    _isMonthlySummaryLoading.value = false
                    return@launch
                }

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val requestJson = org.json.JSONObject().apply {
                    val contentsArray = org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    }
                    put("contents", contentsArray)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestJson.toString().toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBodyStr = response.body?.string()
                    if (responseBodyStr != null) {
                        val responseJson = org.json.JSONObject(responseBodyStr)
                        val text = responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        _monthlySummary.value = text
                    } else {
                        _monthlySummary.value = "Não foi possível obter um resumo do assistente inteligente. Tente novamente."
                    }
                } else {
                    _monthlySummary.value = generateLocalOfflineMonthlySummary(totalInc, totalExp, balance, expByCategory, budgetsList, goalsList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _monthlySummary.value = "⚠️ *Resumo Mensal Offline*\n\n" + generateLocalOfflineMonthlySummary(0.0, 0.0, 0.0, emptyMap(), emptyList(), emptyList())
            } finally {
                _isMonthlySummaryLoading.value = false
            }
        }
    }

    private fun generateLocalOfflineMonthlySummary(
        totalInc: Double,
        totalExp: Double,
        balance: Double,
        expByCategory: Map<String, List<Transaction>>,
        budgetsList: List<CategoryBudget>,
        goalsList: List<SavingsGoal>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("### 📊 Relatório de Resumo Mensal Automatizado\n\n")
        
        sb.append("1. **Análise de Saúde Financeira**\n")
        sb.append("Neste mês corrente, você acumulou um total de **R$ ${String.format(Locale.US, "%.2f", totalInc)}** em receitas e realizou **R$ ${String.format(Locale.US, "%.2f", totalExp)}** em despesas líquidas. ")
        if (balance >= 0) {
            sb.append("O seu saldo atual está positivo em **R$ ${String.format(Locale.US, "%.2f", balance)}**, o que indica uma boa saúde financeira e capacidade de poupar. Continue assim!\n\n")
        } else {
            sb.append("Atenção! Você está com saldo negativo de **R$ ${String.format(Locale.US, "%.2f", balance)}** este mês. Recomendamos revisar despesas de lazer e mercado imediatamente para equilibrar o fluxo de caixa.\n\n")
        }

        sb.append("2. **Identificação de Padrões e Alertas**\n")
        if (expByCategory.isEmpty()) {
            sb.append("Nenhum gasto foi registrado no mês até o momento. Excelente oportunidade para começar a categorizar tudo desde o início!\n\n")
        } else {
            val highest = expByCategory.maxByOrNull { it.value.sumOf { t -> t.amount } }
            if (highest != null) {
                sb.append("A categoria com maior volume de gastos foi **${highest.key}** com um total de **R$ ${String.format(Locale.US, "%.2f", highest.value.sumOf { it.amount })}**. ")
            }
            
            val activeAlerts = budgetsList.filter { b ->
                val spent = expByCategory[b.category]?.sumOf { it.amount } ?: 0.0
                spent >= b.limitAmount * 0.8
            }
            if (activeAlerts.isNotEmpty()) {
                sb.append("Alerta orçamentário: as categorias ")
                sb.append(activeAlerts.joinToString { "**${it.category}**" })
                sb.append(" atingiram ou ultrapassaram a marca de 80% do teto definido.\n\n")
            } else {
                sb.append("Seus gastos estão sob controle dentro dos limites estipulados para todas as categorias.\n\n")
            }
        }

        sb.append("3. **Recomendações e Dicas de Economia Personalizadas**\n")
        sb.append("- 💡 **Defina Metas Realistas**: ")
        if (goalsList.isNotEmpty()) {
            sb.append("Continue poupando para a meta **${goalsList.first().name}** que está em progresso. Tente destinar pelo menos 10% do saldo positivo mensal a ela.\n")
        } else {
            sb.append("Considere criar uma meta de economia hoje mesmo para canalizar seu dinheiro de forma inteligente.\n")
        }
        sb.append("- 💡 **Corte Despesas Supérfluas**: Reduza em 15% os gastos de lazer e alimentação externa para aumentar seu potencial de investimento.\n")
        sb.append("- 💡 **Monitore os Limites**: Acompanhe de perto as notificações e alertas inteligentes de limite orçamentário para evitar surpresas no fim do mês.")
        
        return sb.toString()
    }

    // --- Gemini AI Financial Advisor ---
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun getAiFinancialAdvice(apiKey: String = com.volaris.BuildConfig.GEMINI_API_KEY) {
        if (_isAiLoading.value) return
        _isAiLoading.value = true
        _aiResponse.value = null

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Compile financial status as text context for Gemini
                val transList = repository.getTransactionsList()
                val goalsList = repository.getGoalsList()
                val billsList = repository.getBillsList()
                val budgetsList = repository.getBudgetsList()

                val totalInc = transList.filter { !it.isExpense && it.isPaid }.sumOf { it.amount }
                val totalExp = transList.filter { it.isExpense && it.isPaid }.sumOf { it.amount }
                val balance = totalInc - totalExp

                val contextBuilder = java.lang.StringBuilder()
                contextBuilder.append("Você é o consultor financeiro inteligente da Volaris Finanças. Analise os seguintes dados financeiros consolidados de forma profissional, objetiva e motivadora:\n\n")
                contextBuilder.append("- Saldo Atual Líquido: R$ ${String.format(Locale.US, "%.2f", balance)}\n")
                contextBuilder.append("- Total de Receitas: R$ ${String.format(Locale.US, "%.2f", totalInc)}\n")
                contextBuilder.append("- Total de Despesas: R$ ${String.format(Locale.US, "%.2f", totalExp)}\n\n")

                contextBuilder.append("Orçamentos de Limite Mensal por Categoria:\n")
                if (budgetsList.isEmpty()) {
                    contextBuilder.append("Nenhum orçamento definido.\n")
                } else {
                    budgetsList.forEach { b ->
                        val spent = transList.filter { it.isExpense && it.category == b.category }.sumOf { it.amount }
                        contextBuilder.append("- ${b.category}: Limite R$ ${b.limitAmount} / Consumido R$ $spent\n")
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("Metas de Economia Ativas:\n")
                if (goalsList.isEmpty()) {
                    contextBuilder.append("Nenhuma meta cadastrada.\n")
                } else {
                    goalsList.forEach { g ->
                        contextBuilder.append("- ${g.name}: Guardado R$ ${g.currentAmount} de R$ ${g.targetAmount}\n")
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("Contas Vencendo em breve:\n")
                val pendingBills = billsList.filter { !it.isPaid }
                if (pendingBills.isEmpty()) {
                    contextBuilder.append("Nenhuma conta pendente.\n")
                } else {
                    pendingBills.forEach { bi ->
                        contextBuilder.append("- ${bi.title}: R$ ${bi.amount} (vencendo em breve)\n")
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("Com base nisso, por favor, forneça em português (PT-BR) 3 conselhos ou insights personalizados extremamente práticos, diretos e sem rodeios para ajudar esse usuário a economizar, otimizar seus gastos ou alcançar suas metas mais rápido. Responda em tópicos formatados com Markdown limpo e amigável. Evite introduções longas.")

                val prompt = contextBuilder.toString()

                // Check if API key is blank or placeholder
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(1500) // Simulate analysis delay
                    _aiResponse.value = generateLocalOfflineAdvice(balance, totalInc, totalExp, budgetsList, transList, goalsList, pendingBills)
                    _isAiLoading.value = false
                    return@launch
                }

                // Make real API call to Gemini
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val requestJson = org.json.JSONObject().apply {
                    val contentsArray = org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    }
                    put("contents", contentsArray)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestJson.toString().toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBodyStr = response.body?.string()
                    if (responseBodyStr != null) {
                        val responseJson = org.json.JSONObject(responseBodyStr)
                        val text = responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        _aiResponse.value = text
                    } else {
                        _aiResponse.value = "Não foi possível obter uma resposta do assistente inteligente. Tente novamente."
                    }
                } else {
                    _aiResponse.value = generateLocalOfflineAdvice(balance, totalInc, totalExp, budgetsList, transList, goalsList, pendingBills)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiResponse.value = "⚠️ *Consultoria Offline Ativada*\n\n" + generateLocalOfflineAdvice(
                    netBalance = 0.0,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    budgetsList = emptyList(),
                    transList = emptyList(),
                    goalsList = emptyList(),
                    pendingBills = emptyList()
                )
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun generateLocalOfflineAdvice(
        netBalance: Double,
        totalIncome: Double,
        totalExpense: Double,
        budgetsList: List<CategoryBudget>,
        transList: List<Transaction>,
        goalsList: List<SavingsGoal>,
        pendingBills: List<UpcomingBill>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("### 💡 Seus Insights de Finanças Personalizados (Offline)\n\n")

        // Advice 1: Budget Alert
        var limitExceededCategory: String? = null
        var limitCloseCategory: String? = null
        for (b in budgetsList) {
            val spent = transList.filter { it.isExpense && it.category == b.category }.sumOf { it.amount }
            if (spent > b.limitAmount) {
                limitExceededCategory = b.category
                break
            } else if (spent >= b.limitAmount * 0.8) {
                limitCloseCategory = b.category
            }
        }

        if (limitExceededCategory != null) {
            sb.append("1. **🔴 Orçamento Estourado:** Você ultrapassou o limite estabelecido para a categoria **$limitExceededCategory**. Tente pausar gastos nessa área imediatamente ou realocar limites de outras categorias menos essenciais para equilibrar as contas.\n\n")
        } else if (limitCloseCategory != null) {
            sb.append("1. **⚠️ Atenção ao Limite:** Seus gastos em **$limitCloseCategory** estão próximos de atingir 80% do limite mensal. Considere adiar compras não urgentes nessa categoria até o próximo mês.\n\n")
        } else {
            sb.append("1. **🟢 Orçamentos Saudáveis:** Parabéns! Todos os seus limites por categoria estão sob perfeito controle hoje. Continue monitorando suas faturas e compras recorrentes para fechar o mês no azul.\n\n")
        }

        // Advice 2: Cashflow and Saving Ratio
        if (totalIncome > 0) {
            val savingRate = (netBalance / totalIncome) * 100
            if (savingRate >= 20) {
                sb.append("2. **📈 Excelente Taxa de Poupança:** Você economizou ${String.format(Locale.getDefault(), "%.1f", savingRate)}% de tudo o que recebeu este mês! Isso é fantástico e supera a recomendação média de 10-15%. Considere destinar esse saldo positivo para as suas **Metas de Economia** ativas.\n\n")
            } else if (savingRate > 0) {
                sb.append("2. **📊 Margem de Poupança Estreita:** Você economizou cerca de ${String.format(Locale.getDefault(), "%.1f", savingRate)}% dos seus ganhos. Para acelerar seus sonhos, tente encontrar pequenas despesas supérfluas (como assinaturas não utilizadas ou entregas extras) que possam ser reduzidas.\n\n")
            } else {
                sb.append("2. **🚨 Alerta de Déficit:** Suas despesas excederam suas receitas este mês. Quando gastamos mais do que ganhamos, criamos uma dívida silenciosa. Analise seus maiores gastos nos últimos 7 dias e defina orçamentos mais rígidos.\n\n")
            }
        } else {
            sb.append("2. **✍️ Adicione suas Receitas:** Cadastre seu salário ou rendimentos na aba de Transações. Ter o registro de suas entradas é indispensável para que o assistente analise sua saúde financeira global.\n\n")
        }

        // Advice 3: Savings goals contributions
        if (goalsList.isNotEmpty()) {
            val goal = goalsList.first()
            val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100 else 0.0
            if (progress < 100) {
                val remainingVal = goal.targetAmount - goal.currentAmount
                val formattedRemaining = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(remainingVal)
                sb.append("3. **🎯 Foco na Meta '${goal.name}':** Você já completou ${String.format(Locale.getDefault(), "%.1f", progress)}% dessa meta! Faltam apenas **$formattedRemaining** para realizá-la. Se tiver qualquer saldo residual, contribua hoje mesmo para manter o hábito constante.")
            } else {
                sb.append("3. **🏆 Meta '${goal.name}' Concluída:** Incrível! Você atingiu 100% da sua meta. É hora de celebrar sua disciplina financeira e, quem sabe, criar um novo desafio de economia para o próximo período!")
            }
        } else {
            sb.append("3. **🎯 Defina uma Meta:** Pessoas com objetivos claros economizam até 4x mais rápido. Crie uma meta simples na aba 'Metas' (como um Fundo de Emergência ou uma viagem de fim de ano) para dar propósito a cada real poupado.")
        }

        return sb.toString()
    }

    // --- Gemini AI Interactive Chatbot ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun initChatIfEmpty() {
        if (_chatMessages.value.isEmpty()) {
            _chatMessages.value = listOf(
                ChatMessage(
                    text = "Olá! Sou o seu **Consultor Financeiro Inteligente Volaris**. Posso analisar seus hábitos de consumo, ajudar a otimizar seus orçamentos e planejar suas metas de economia.\n\nComo posso ajudar você hoje?",
                    isUser = false
                )
            )
        }
    }

    fun sendChatUserMessage(userText: String, apiKey: String = com.volaris.BuildConfig.GEMINI_API_KEY) {
        if (userText.isBlank() || _isChatLoading.value) return

        val userMsg = ChatMessage(text = userText, isUser = true)
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList

        _isChatLoading.value = true

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Fetch context
                val transList = repository.getTransactionsList()
                val goalsList = repository.getGoalsList()
                val billsList = repository.getBillsList()
                val budgetsList = repository.getBudgetsList()

                val totalInc = transList.filter { !it.isExpense && it.isPaid }.sumOf { it.amount }
                val totalExp = transList.filter { it.isExpense && it.isPaid }.sumOf { it.amount }
                val balance = totalInc - totalExp

                val contextBuilder = java.lang.StringBuilder()
                contextBuilder.append("Você é o consultor financeiro inteligente da Volaris Finanças. O usuário está conversando interativamente com você. Aqui estão os dados financeiros reais consolidados dele para contextualizar sua resposta:\n\n")
                contextBuilder.append("- Saldo Líquido Atual: R$ ${String.format(Locale.US, "%.2f", balance)}\n")
                contextBuilder.append("- Total de Receitas: R$ ${String.format(Locale.US, "%.2f", totalInc)}\n")
                contextBuilder.append("- Total de Despesas: R$ ${String.format(Locale.US, "%.2f", totalExp)}\n\n")

                contextBuilder.append("Orçamentos de Limite Mensal por Categoria:\n")
                if (budgetsList.isEmpty()) {
                    contextBuilder.append("Nenhum orçamento definido.\n")
                } else {
                    budgetsList.forEach { b ->
                        val spent = transList.filter { it.isExpense && it.category == b.category }.sumOf { it.amount }
                        contextBuilder.append("- ${b.category}: Limite R$ ${b.limitAmount} / Consumido R$ $spent\n")
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("Metas de Economia Ativas:\n")
                if (goalsList.isEmpty()) {
                    contextBuilder.append("Nenhuma meta cadastrada.\n")
                } else {
                    goalsList.forEach { g ->
                        contextBuilder.append("- ${g.name}: Guardado R$ ${g.currentAmount} de R$ ${g.targetAmount}\n")
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("Contas Pendentes:\n")
                val pendingBills = billsList.filter { !it.isPaid }
                if (pendingBills.isEmpty()) {
                    contextBuilder.append("Nenhuma conta pendente.\n")
                } else {
                    pendingBills.forEach { bi ->
                        contextBuilder.append("- ${bi.title}: R$ ${bi.amount}\n")
                    }
                }
                contextBuilder.append("\n")

                contextBuilder.append("Histórico recente da conversa:\n")
                // Take up to last 8 messages
                val lastMsgs = currentList.takeLast(8)
                lastMsgs.forEach { msg ->
                    val role = if (msg.isUser) "Usuário" else "Consultor"
                    contextBuilder.append("$role: ${msg.text}\n")
                }

                contextBuilder.append("\nPergunta do Usuário: $userText\n\n")
                contextBuilder.append("Responda em português (PT-BR) de forma amigável, clara, direta, motivadora e sem rodeios. Use formatação Markdown limpa e legível (tópicos, negrito). Mantenha as dicas altamente práticas e focadas em controle financeiro.")

                val prompt = contextBuilder.toString()

                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(1200) // Simulate analysis delay
                    val offlineReplyText = generateOfflineChatReply(userText)
                    val offlineReply = ChatMessage(text = offlineReplyText, isUser = false)
                    val updatedList = _chatMessages.value.toMutableList()
                    updatedList.add(offlineReply)
                    _chatMessages.value = updatedList
                    _isChatLoading.value = false
                    return@launch
                }

                // Make real API call to Gemini
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val requestJson = org.json.JSONObject().apply {
                    val contentsArray = org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    }
                    put("contents", contentsArray)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestJson.toString().toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBodyStr = response.body?.string()
                    if (responseBodyStr != null) {
                        val responseJson = org.json.JSONObject(responseBodyStr)
                        val text = responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        
                        val aiMsg = ChatMessage(text = text, isUser = false)
                        val updatedList = _chatMessages.value.toMutableList()
                        updatedList.add(aiMsg)
                        _chatMessages.value = updatedList
                    } else {
                        val errorMsg = ChatMessage(text = "Não consegui obter uma resposta. Por favor, tente enviar novamente.", isUser = false)
                        val updatedList = _chatMessages.value.toMutableList()
                        updatedList.add(errorMsg)
                        _chatMessages.value = updatedList
                    }
                } else {
                    val offlineReplyText = "⚠️ *Modo Offline Ativado*:\n\n" + generateOfflineChatReply(userText)
                    val offlineReply = ChatMessage(text = offlineReplyText, isUser = false)
                    val updatedList = _chatMessages.value.toMutableList()
                    updatedList.add(offlineReply)
                    _chatMessages.value = updatedList
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val offlineReplyText = "⚠️ *Modo Offline Ativado (Sem Conexão)*:\n\n" + generateOfflineChatReply(userText)
                val offlineReply = ChatMessage(text = offlineReplyText, isUser = false)
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(offlineReply)
                _chatMessages.value = updatedList
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    private fun generateOfflineChatReply(userMsg: String): String {
        val msg = userMsg.lowercase()
        return when {
            msg.contains("ajuda") || msg.contains("como funciona") || msg.contains("olá") || msg.contains("ola") || msg.contains("bom dia") || msg.contains("boa tarde") -> {
                "Sou o seu **Consultor Financeiro Volaris**. Você pode me fazer perguntas sobre:\n\n" +
                "- **Economizar:** Como poupar mais dinheiro ou reduzir despesas.\n" +
                "- **Metas:** Como se planejar para atingir seus objetivos de vida.\n" +
                "- **Orçamentos:** Dicas para respeitar seus limites de categoria.\n" +
                "- **Investimentos:** Princípios básicos de educação financeira.\n\n" +
                "Como posso te ajudar hoje?"
            }
            msg.contains("gasto") || msg.contains("despesa") || msg.contains("reduzir") || msg.contains("cortar") || msg.contains("economizar") || msg.contains("alimentação") || msg.contains("alimentacao") -> {
                "Para reduzir seus gastos de forma eficaz, recomendo:\n\n" +
                "1. **Regra dos 3 dias:** Diante de uma compra supérflua, espere 3 dias. Se ainda fizer sentido, compre.\n" +
                "2. **Audite assinaturas:** Cancele serviços de streaming ou aplicativos que não usou nos últimos 30 dias.\n" +
                "3. **Alimentação fora:** Este costuma ser o maior 'ralo' de dinheiro silencioso. Tente cozinhar mais em casa.\n\n" +
                "Defina limites rígidos na aba **Orçamentos** para ajudar no controle automático!"
            }
            msg.contains("meta") || msg.contains("poupar") || msg.contains("economizar") || msg.contains("guardar") -> {
                "Para juntar dinheiro com mais consistência:\n\n" +
                "- **Automatize:** Separe de 10% a 20% das suas receitas logo no início do mês, antes de gastar.\n" +
                "- **Reserva de Emergência:** Seu primeiro objetivo deve ser juntar de 3 a 6 meses do seu custo de vida básico.\n" +
                "- **Acompanhe o Progresso:** Monitore suas conquistas na aba **Metas**. Cada pequena vitória conta!"
            }
            msg.contains("investir") || msg.contains("investimento") || msg.contains("onde coloco") -> {
                "Para quem está começando a investir, a regra de ouro é a **segurança e liquidez**:\n\n" +
                "1. **Tesouro Selic / CDB 100% DI:** Excelentes para a sua reserva de emergência, pois rendem mais que a poupança e você pode resgatar a qualquer momento.\n" +
                "2. **Diversificação:** Nunca coloque todos os ovos na mesma cesta. Comece na renda fixa e estude renda variável aos poucos.\n" +
                "3. **Conhecimento:** O melhor investimento inicial é em livros e cursos de finanças!"
            }
            else -> {
                "Excelente pergunta! Para otimizar suas finanças na Volaris, lembre-se de:\n\n" +
                "1. Registrar todas as suas receitas e despesas diariamente na aba Transações.\n" +
                "2. Configurar limites mensais na aba **Orçamentos** para suas categorias mais pesadas.\n" +
                "3. Ativar o modo online com uma **chave de API Gemini** nas configurações do app para ter uma consultoria 100% personalizada e inteligente baseada no seu perfil real!"
            }
        }
    }
}

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
