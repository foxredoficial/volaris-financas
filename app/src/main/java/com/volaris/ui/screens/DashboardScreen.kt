package com.volaris.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.volaris.data.*
import com.volaris.ui.components.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    transactions: List<Transaction>,
    goals: List<SavingsGoal>,
    bills: List<UpcomingBill>,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    hideBalances: Boolean,
    onToggleHideBalances: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToBills: () -> Unit,
    onOpenBillSplitter: () -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenMonthlySummary: () -> Unit,
    onOpenCalculator: () -> Unit,
    budgetAlerts: List<BudgetAlert>,
    userProfile: UserProfile,
    onToggleBillPaid: (UpcomingBill) -> Unit
) {
    val scrollState = rememberScrollState()
    var isFocusMode by remember { mutableStateOf(false) }

    val currentYearNum = Calendar.getInstance().get(Calendar.YEAR)
    val prevYearNum = currentYearNum - 1

    val annualAnalysisData = remember(transactions) {
        val expenses = transactions.filter { it.isExpense && it.isPaid }
        
        fun getYearOfTimestamp(timestamp: Long): Int {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            return cal.get(Calendar.YEAR)
        }

        val currYearExpenses = expenses.filter { getYearOfTimestamp(it.date) == currentYearNum }
        val prevYearExpenses = expenses.filter { getYearOfTimestamp(it.date) == prevYearNum }

        val totalCurrYear = currYearExpenses.sumOf { it.amount }
        val totalPrevYear = prevYearExpenses.sumOf { it.amount }

        // Group by category
        val currByCat = currYearExpenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val prevByCat = prevYearExpenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }

        // All unique categories present in either year
        val allCats = (currByCat.keys + prevByCat.keys).toList()

        val categoryComparisons = allCats.map { cat ->
            val currVal = currByCat[cat] ?: 0.0
            val prevVal = prevByCat[cat] ?: 0.0
            val diff = currVal - prevVal
            val pctChange = if (prevVal > 0.0) {
                (diff / prevVal) * 100.0
            } else {
                if (currVal > 0.0) 100.0 else 0.0
            }
            CategoryComparison(
                category = cat,
                currentYearAmount = currVal,
                previousYearAmount = prevVal,
                difference = diff,
                percentageChange = pctChange
            )
        }.sortedByDescending { kotlin.math.abs(it.percentageChange) }

        val maxGrowth = categoryComparisons.filter { it.difference > 0 }.maxByOrNull { it.percentageChange }
        val maxReduction = categoryComparisons.filter { it.difference < 0 }.minByOrNull { it.percentageChange }

        AnnualAnalysis(
            totalCurrentYear = totalCurrYear,
            totalPreviousYear = totalPrevYear,
            comparisons = categoryComparisons,
            maxGrowth = maxGrowth,
            maxReduction = maxReduction
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {




        // Balance Card (Premium Gradient)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF381E72),
                                Color(0xFF4F378B)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Resumo de Saldos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "SALDO TOTAL ATUAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD0BCFF).copy(alpha = 0.8f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterCenterFocus,
                                    contentDescription = null,
                                    tint = Color(0xFFD0BCFF).copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Foco",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD0BCFF).copy(alpha = 0.8f)
                                )
                                Switch(
                                    checked = isFocusMode,
                                    onCheckedChange = { isFocusMode = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFD0BCFF),
                                        uncheckedThumbColor = Color(0xFFD0BCFF).copy(alpha = 0.5f),
                                        uncheckedTrackColor = Color.Transparent
                                    ),
                                    modifier = Modifier.scale(0.7f).testTag("focus_mode_switch")
                                )
                            }

                            IconButton(onClick = onToggleHideBalances) {
                                Icon(
                                    imageVector = if (hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Esconder/Mostrar Saldo",
                                    tint = Color(0xFFD0BCFF).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (hideBalances) "R$ ••••••" else formatCurrency(netBalance),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netBalance >= 0) Color.White else Color(0xFFFFB4AB)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Income Summary
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFFB3FFB3),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Receitas",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEADDFF).copy(alpha = 0.8f)
                                )
                                Text(
                                    text = if (hideBalances) "R$ •••" else formatCurrency(totalIncome),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB3FFB3)
                                )
                            }
                        }

                        // Expense Summary
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TrendingDown,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB4AB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Despesas",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEADDFF).copy(alpha = 0.8f)
                                )
                                Text(
                                    text = if (hideBalances) "R$ •••" else formatCurrency(totalExpense),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB4AB)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SEÇÃO: PROGRESSO DE METAS/XP ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("progresso_metas_xp_section"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Progresso de Metas/XP",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Level and Title Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Nível ${userProfile.level}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = userProfile.financialTitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    val xpProgress = if (userProfile.xpNextLevel > 0) userProfile.xp.toFloat() / userProfile.xpNextLevel else 0f
                    Text(
                        text = "${userProfile.xp}/${userProfile.xpNextLevel} XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // XP Progress Bar
                val xpProgress = if (userProfile.xpNextLevel > 0) userProfile.xp.toFloat() / userProfile.xpNextLevel else 0f
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Active Savings Goals summary inside Progresso de Metas/XP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Metas de Poupança Ativas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onNavigateToGoals,
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Ver Todas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (goals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma meta criada. Toque acima para começar a poupar!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        goals.take(2).forEach { goal ->
                            val goalProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                            val pct = (goalProgress * 100).coerceIn(0f, 100f)
                            
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = goal.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${formatCurrency(goal.currentAmount)} / ${formatCurrency(goal.targetAmount)} (${String.format(Locale.getDefault(), "%.0f", pct)}%)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { goalProgress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (goalProgress >= 1f) Color(0xFF00C853) else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card de Análise Anual (only shown when NOT in Focus Mode)
        if (!isFocusMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Análise Anual ($prevYearNum vs $currentYearNum)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        val context = LocalContext.current
                        IconButton(
                            onClick = {
                                com.volaris.ui.components.PdfExportHelper.exportAnnualAnalysisPdf(
                                    context = context,
                                    prevYear = prevYearNum,
                                    currentYear = currentYearNum,
                                    totalPrev = annualAnalysisData.totalPreviousYear,
                                    totalCurrent = annualAnalysisData.totalCurrentYear,
                                    comparisons = annualAnalysisData.comparisons,
                                    maxGrowth = annualAnalysisData.maxGrowth,
                                    maxReduction = annualAnalysisData.maxReduction
                                )
                            },
                            modifier = Modifier.size(36.dp).testTag("dashboard_export_annual_pdf")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = "Exportar Análise Anual para PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comparison of totals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Gastos em $prevYearNum", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(annualAnalysisData.totalPreviousYear), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gastos em $currentYearNum", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(annualAnalysisData.totalCurrentYear), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Total comparison sentence
                    val diffTotal = annualAnalysisData.totalCurrentYear - annualAnalysisData.totalPreviousYear
                    val pctTotal = if (annualAnalysisData.totalPreviousYear > 0) (diffTotal / annualAnalysisData.totalPreviousYear) * 100 else 0.0
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (diffTotal <= 0) Color(0xFF00C853).copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (diffTotal <= 0) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = if (diffTotal <= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (diffTotal <= 0) {
                                    "Você gastou ${formatCurrency(kotlin.math.abs(diffTotal))} a menos (-${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(pctTotal))}%) este ano!"
                                } else {
                                    "Seus gastos subiram ${formatCurrency(diffTotal)} (+${String.format(Locale.getDefault(), "%.1f", pctTotal)}%) comparados ao ano passado."
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (diffTotal <= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Highlight Section (Crescimento / Redução)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Maior Crescimento Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Maior Aumento", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val item = annualAnalysisData.maxGrowth
                                if (item != null) {
                                    Text(item.category, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("+${String.format(Locale.getDefault(), "%.1f", item.percentageChange)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Sem dados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Maior Redução Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Maior Redução", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val item = annualAnalysisData.maxReduction
                                if (item != null) {
                                    Text(item.category, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${String.format(Locale.getDefault(), "%.1f", item.percentageChange)}%", fontSize = 11.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Sem dados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (annualAnalysisData.comparisons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Detalhamento por Categoria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        annualAnalysisData.comparisons.take(4).forEach { comp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(comp.category, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text("Ano Ant: ${formatCurrency(comp.previousYearAmount)} | Ano Atu: ${formatCurrency(comp.currentYearAmount)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isUp = comp.difference > 0
                                    Icon(
                                        imageVector = if (isUp) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                        contentDescription = null,
                                        tint = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF00C853),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${if (isUp) "+" else ""}${String.format(Locale.getDefault(), "%.1f", comp.percentageChange)}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- UPCOMING FIXED BILLS ALERTS ---
        val upcomingBillAlerts = remember(bills) {
            bills.filter { !it.isPaid && getDaysRemaining(it.dueDate) <= 3 }
        }
        if (upcomingBillAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("upcoming_bills_alerts_section"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = "Lembrete de Contas Próximas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Evite multas! Pague em dia as suas contas fixas:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcomingBillAlerts.forEach { bill ->
                            val daysLeft = getDaysRemaining(bill.dueDate)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bill.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "Vence em: ${formatDate(bill.dueDate)}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = when {
                                                daysLeft < 0 -> "⚠️ ATRASADA HÁ ${kotlin.math.abs(daysLeft)} DIAS!"
                                                daysLeft == 0L -> "🚨 VENCE HOJE!"
                                                daysLeft == 1L -> "⏰ Vence amanhã!"
                                                else -> "📅 Restam $daysLeft dias para o vencimento"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (daysLeft <= 1) MaterialTheme.colorScheme.error else Color(0xFFE65100)
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatCurrency(bill.amount),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { onToggleBillPaid(bill) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Text("Pagar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- INTELLIGENT BUDGET ALERTS ---
        if (budgetAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Alertas Inteligentes de Orçamento",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    budgetAlerts.forEach { alert ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "•",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (alert.isCritical) {
                                    "Crítico: Limite atingido em ${alert.category}! Gasto: R$ ${String.format(Locale.US, "%.2f", alert.spent)} / Limite: R$ ${String.format(Locale.US, "%.2f", alert.limit)}"
                                } else {
                                    "Alerta: Gastos em ${alert.category} atingiram ${String.format(Locale.US, "%.0f", alert.percentage * 100)}% do limite! Gasto: R$ ${String.format(Locale.US, "%.2f", alert.spent)} / Limite: R$ ${String.format(Locale.US, "%.2f", alert.limit)}"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = if (alert.isCritical) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // --- SMART TOOLS QUICK ACTIONS ROW ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Atalhos de Ferramentas Inteligentes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // AI Advisor button
                        Button(
                            onClick = onOpenAiAdvisor,
                            modifier = Modifier.weight(1f).testTag("dashboard_ai_advisor_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consultor IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Monthly summary button
                        Button(
                            onClick = onOpenMonthlySummary,
                            modifier = Modifier.weight(1f).testTag("dashboard_monthly_summary_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resumo Mensal IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bill splitter button
                        Button(
                            onClick = onOpenBillSplitter,
                            modifier = Modifier.weight(1f).testTag("dashboard_bill_splitter_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rachar Conta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Advanced Calculator button
                        Button(
                            onClick = onOpenCalculator,
                            modifier = Modifier.weight(1f).testTag("dashboard_calculator_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calculadora Fin.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (!isFocusMode) {
            // Charts Section (Gráficos detalhados de despesas)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distribuição de Despesas por Categoria",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val context = LocalContext.current
                        IconButton(
                            onClick = {
                                com.volaris.ui.components.PdfExportHelper.exportCategoryDistributionPdf(
                                    context = context,
                                    transactions = transactions
                                )
                            },
                            modifier = Modifier.size(36.dp).testTag("dashboard_export_category_pdf")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = "Exportar Distribuição por Categoria para PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val expenses = remember(transactions) {
                        transactions.filter { it.isExpense && it.isPaid }
                    }

                    if (expenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma despesa para exibir no gráfico.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val catMap = remember(expenses) {
                            expenses.groupBy { it.category }
                                .mapValues { entry -> entry.value.sumOf { it.amount } }
                        }

                        ExpenseDonutChart(categoryAmounts = catMap)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    CashFlowBarChart(totalIncome = totalIncome, totalExpense = totalExpense)
                }
            }

            // Line Chart of Month-Over-Month Balance Evolution
            MoMBalanceLineChart(
                transactions = transactions,
                hideBalances = hideBalances
            )

            // Active Goals Section Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metas de Economia Ativas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToGoals) {
                            Text("Ver Todas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (goals.isEmpty()) {
                        Text(
                            text = "Crie metas de economia mensais para poupar de forma focada.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        GoalsBarChart(goals = goals)
                    }
                }
            }
        }

        // Upcoming Bills Section Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contas a Vencer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToBills) {
                        Text("Gerenciar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                val pendingBills = remember(bills) { bills.filter { !it.isPaid } }

                if (pendingBills.isEmpty()) {
                    Text(
                        text = "Tudo em dia! Nenhuma conta com vencimento pendente.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    pendingBills.take(2).forEach { bill ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(bill.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "Vence em: ${formatDate(bill.dueDate)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatCurrency(bill.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Atividades Recentes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("Ver Histórico", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (transactions.isEmpty()) {
                    Text(
                        text = "Nenhuma transação registrada. Toque no botão de '+' para adicionar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    transactions.take(3).forEach { t ->
                        TransactionRowItem(transaction = t, onDelete = {})
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
fun ExpenseDonutChart(categoryAmounts: Map<String, Double>) {
    val total = remember(categoryAmounts) { categoryAmounts.values.sum() }
    if (total == 0.0) return

    // Order categories by amount descending
    val sortedCategories = remember(categoryAmounts) { categoryAmounts.toList().sortedByDescending { it.second } }

    // Modern material color list
    val colors = listOf(
        Color(0xFF00C853), // Emerald Green
        Color(0xFF3F51B5), // Indigo Blue
        Color(0xFFE53935), // Red
        Color(0xFFFFB300), // Amber Gold
        Color(0xFF00ACC1), // Cyan/Teal
        Color(0xFF8E24AA), // Purple
        Color(0xFFD81B60), // Pink
        Color(0xFFF4511E)  // Orange
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Chart Draw
        Box(
            modifier = Modifier
                .size(140.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                sortedCategories.forEachIndexed { index, pair ->
                    val sweepAngle = ((pair.second / total) * 360f).toFloat()
                    val color = colors[index % colors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = Stroke(width = 34f, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
            // Inner text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Pago",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(total),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Chart Legends
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            sortedCategories.take(5).forEachIndexed { index, pair ->
                val pct = (pair.second / total) * 100
                val color = colors[index % colors.size]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pair.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", pct)}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (sortedCategories.size > 5) {
                Text(
                    text = "+ ${sortedCategories.size - 5} outras categorias",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 18.dp)
                )
            }
        }
    }
}

@Composable
fun CashFlowBarChart(
    totalIncome: Double,
    totalExpense: Double
) {
    val maxVal = maxOf(totalIncome, totalExpense, 1.0)
    val incomePct = (totalIncome / maxVal).toFloat()
    val expensePct = (totalExpense / maxVal).toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Comparativo de Fluxo Mensal",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-Axis labels (Valores proporcionais)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(formatCurrency(maxVal), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCurrency(maxVal * 0.75), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCurrency(maxVal * 0.5), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCurrency(maxVal * 0.25), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("R$ 0,00", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Grid lines & Bars Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Background grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        )
                    }
                }

                // Bars Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Income Bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = formatCurrency(totalIncome),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00C853)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight(incomePct.coerceIn(0.05f, 1f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF00C853),
                                            Color(0xFF00E676)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Receitas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Expense Bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = formatCurrency(totalExpense),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight(expensePct.coerceIn(0.05f, 1f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFE53935),
                                            Color(0xFFFF5252)
                                        )
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Despesas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsBarChart(goals: List<SavingsGoal>) {
    if (goals.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Progresso Individual das Metas",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-Axis labels (Progresso de 0 a 100%)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("100%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("75%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("50%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("25%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Grid lines & Bars Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Background grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        )
                    }
                }

                // Bars Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    goals.take(3).forEach { goal ->
                        val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                        val pct = (progress * 100).toInt().coerceIn(0, 100)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = "$pct%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // The vertical bar
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(progress.coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = goal.name,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

data class MonthlyFinancials(
    val monthIndex: Int,
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val cumulative: Double
)

@Composable
fun MoMBalanceLineChart(
    transactions: List<Transaction>,
    hideBalances: Boolean
) {
    var selectedChartTab by remember { mutableStateOf(0) } // 0 = Receitas vs Despesas, 1 = Evolução do Saldo

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("mom_balance_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Evolução e Sazonalidade Mensal",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Segmented-like Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Fluxo Comparativo", "Evolução do Saldo").forEachIndexed { idx, title ->
                    val isSelected = selectedChartTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedChartTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val cal = Calendar.getInstance()
            val paidTx = transactions.filter { it.isPaid }

            if (paidTx.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Insira transações para ver a evolução mensal.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Group by year * 12 + month
                val grouped = paidTx.groupBy { tx ->
                    cal.timeInMillis = tx.date
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH)
                    year * 12 + month
                }

                val sortedKeys = grouped.keys.sorted()
                var runningCumulative = 0.0
                val monthLabelsSdf = SimpleDateFormat("MMM/yy", Locale("pt", "BR"))

                val monthlyFinancialsList = sortedKeys.map { key ->
                    val txs = grouped[key] ?: emptyList()
                    val inc = txs.filter { !it.isExpense }.sumOf { it.amount }
                    val exp = txs.filter { it.isExpense }.sumOf { it.amount }
                    val net = inc - exp
                    runningCumulative += net

                    val year = key / 12
                    val month = key % 12
                    cal.clear()
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val label = monthLabelsSdf.format(cal.time).replaceFirstChar { it.uppercase() }

                    MonthlyFinancials(
                        monthIndex = key,
                        label = label,
                        income = inc,
                        expense = exp,
                        net = net,
                        cumulative = runningCumulative
                    )
                }

                val chartData = monthlyFinancialsList.takeLast(6)

                // Render Canvas Chart
                val maxVal = if (selectedChartTab == 0) {
                    chartData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
                } else {
                    chartData.maxOfOrNull { kotlin.math.abs(it.cumulative) } ?: 1.0
                }.let { if (it == 0.0) 1.0 else it }

                val minVal = if (selectedChartTab == 0) {
                    0.0
                } else {
                    chartData.minOfOrNull { it.cumulative } ?: 0.0
                }

                val chartRange = if (selectedChartTab == 0) maxVal else (maxVal - minVal).let { if (it == 0.0) 1.0 else it }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingLeft = 110f
                        val paddingRight = 40f
                        val paddingTop = 45f
                        val paddingBottom = 60f

                        val usableWidth = width - paddingLeft - paddingRight
                        val usableHeight = height - paddingTop - paddingBottom

                        // Draw Grid lines (Y axis)
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val fraction = i.toFloat() / gridCount
                            val y = paddingTop + usableHeight * (1f - fraction)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.15f),
                                start = Offset(paddingLeft, y),
                                end = Offset(width - paddingRight, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw line(s)
                        if (chartData.size > 1) {
                            val xStep = usableWidth / (chartData.size - 1)

                            if (selectedChartTab == 0) {
                                // Draw Incomes (Green) and Expenses (Red)
                                val incomePoints = chartData.mapIndexed { idx, item ->
                                    val x = paddingLeft + idx * xStep
                                    val y = paddingTop + usableHeight * (1f - (item.income.toFloat() / maxVal.toFloat()))
                                    Offset(x, y)
                                }
                                val expensePoints = chartData.mapIndexed { idx, item ->
                                    val x = paddingLeft + idx * xStep
                                    val y = paddingTop + usableHeight * (1f - (item.expense.toFloat() / maxVal.toFloat()))
                                    Offset(x, y)
                                }

                                // Draw path for Income
                                val incomePath = Path().apply {
                                    incomePoints.forEachIndexed { idx, pt ->
                                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                    }
                                }
                                drawPath(
                                    path = incomePath,
                                    color = Color(0xFF00C853),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Draw path for Expense
                                val expensePath = Path().apply {
                                    expensePoints.forEachIndexed { idx, pt ->
                                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                    }
                                }
                                drawPath(
                                    path = expensePath,
                                    color = Color(0xFFE53935),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Draw dots for both
                                incomePoints.forEach { pt ->
                                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                                    drawCircle(color = Color(0xFF00C853), radius = 3.dp.toPx(), center = pt)
                                }
                                expensePoints.forEach { pt ->
                                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                                    drawCircle(color = Color(0xFFE53935), radius = 3.dp.toPx(), center = pt)
                                }

                            } else {
                                // Draw Cumulative Balance (Blue)
                                val balancePoints = chartData.mapIndexed { idx, item ->
                                    val x = paddingLeft + idx * xStep
                                    val factor = ((item.cumulative - minVal) / chartRange).toFloat()
                                    val y = paddingTop + usableHeight * (1f - factor)
                                    Offset(x, y)
                                }

                                val balancePath = Path().apply {
                                    balancePoints.forEachIndexed { idx, pt ->
                                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                    }
                                }

                                // Draw filled gradient under curve
                                val fillPath = Path().apply {
                                    addPath(balancePath)
                                    lineTo(balancePoints.last().x, paddingTop + usableHeight)
                                    lineTo(balancePoints.first().x, paddingTop + usableHeight)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF3F51B5).copy(alpha = 0.3f),
                                            Color(0xFF3F51B5).copy(alpha = 0.0f)
                                        )
                                    )
                                )

                                drawPath(
                                    path = balancePath,
                                    color = Color(0xFF3F51B5),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                balancePoints.forEach { pt ->
                                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                                    drawCircle(color = Color(0xFF3F51B5), radius = 3.dp.toPx(), center = pt)
                                }
                            }
                        }
                    }

                    // Months labels row
                    if (chartData.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(start = 38.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            chartData.forEach { item ->
                                Text(
                                    text = item.label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Left Y-axis labels
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.TopStart)
                            .padding(top = 10.dp, bottom = 20.dp, start = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        val stepVal = chartRange / 4
                        for (i in 0..4) {
                            val v = if (selectedChartTab == 0) maxVal - i * stepVal else minVal + (4 - i) * stepVal
                            Text(
                                text = if (hideBalances) "R$ •••" else formatCurrency(v),
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legend indicator
                if (selectedChartTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00C853)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Receitas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE53935)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Despesas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3F51B5)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Saldo Acumulado", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Readable table data row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mês", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("Receitas", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        Text("Despesas", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        Text("Saldo", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                    }
                    chartData.reversed().forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text(if (hideBalances) "R$ •••" else formatCurrency(item.income), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            Text(if (hideBalances) "R$ •••" else formatCurrency(item.expense), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            Text(if (hideBalances) "R$ •••" else formatCurrency(item.cumulative), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (item.cumulative >= 0) Color(0xFF00C853) else Color(0xFFE53935), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        }
                    }
                }
            }
        }
    }
}

