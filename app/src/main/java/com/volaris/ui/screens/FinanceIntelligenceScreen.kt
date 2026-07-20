package com.volaris.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volaris.data.Transaction
import com.volaris.data.UpcomingBill
import com.volaris.data.SavingsGoal
import com.volaris.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FinanceIntelligenceScreen(
    transactions: List<Transaction>,
    bills: List<UpcomingBill>,
    goals: List<SavingsGoal>
) {
    var activeSubTab by remember { mutableStateOf(0) }
    val tabs = listOf("Regra 50/30/20", "Investimentos", "Previsão", "FIRE")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inteligência & Planejamento",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Simuladores e regras para otimizar suas finanças",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(
                onClick = {
                    com.volaris.ui.components.PdfExportHelper.exportIntelligencePdf(
                        context = context,
                        activeTab = activeSubTab,
                        transactions = transactions,
                        bills = bills
                    )
                },
                modifier = Modifier.size(36.dp).testTag("intelligence_export_pdf_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = "Exportar Inteligência para PDF",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = activeSubTab == index
                FilterChip(
                    selected = isSelected,
                    onClick = { activeSubTab = index },
                    label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (activeSubTab) {
            0 -> Rule503020Tab(transactions)
            1 -> InvestmentSimulatorTab()
            2 -> CashProjectionTab(transactions, bills)
            3 -> FireRetirementTab(transactions)
        }
    }
}

data class Rule503020Analysis(
    val incomesTotal: Double,
    val expensesTotal: Double,
    val isUsingFallback: Boolean,
    val referenceIncome: Double,
    val needsSpent: Double,
    val wantsSpent: Double,
    val savingsSpent: Double,
    val needsPercentage: Double,
    val wantsPercentage: Double,
    val savingsPercentage: Double
)

@Composable
fun Rule503020Tab(transactions: List<Transaction>) {
    val analysis = remember(transactions) {
        val incomes = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val expenses = transactions.filter { it.isExpense }.sumOf { it.amount }

        val isUsingFb = incomes == 0.0
        val refInc = if (isUsingFb) 3000.0 else incomes

        val nSpent = transactions.filter { 
            it.isExpense && it.category in listOf("Alimentação", "Transporte", "Moradia", "Saúde", "Mercado")
        }.sumOf { it.amount }

        val wSpent = transactions.filter {
            it.isExpense && it.category in listOf("Lazer", "Outros", "Educação")
        }.sumOf { it.amount }

        val sSpent = transactions.filter {
            it.isExpense && it.category == "Investimentos"
        }.sumOf { it.amount } + maxOf(0.0, refInc - expenses)

        val nPct = if (refInc > 0) (nSpent / refInc) * 100 else 0.0
        val wPct = if (refInc > 0) (wSpent / refInc) * 100 else 0.0
        val sPct = if (refInc > 0) (sSpent / refInc) * 100 else 0.0

        Rule503020Analysis(
            incomesTotal = incomes,
            expensesTotal = expenses,
            isUsingFallback = isUsingFb,
            referenceIncome = refInc,
            needsSpent = nSpent,
            wantsSpent = wSpent,
            savingsSpent = sSpent,
            needsPercentage = nPct,
            wantsPercentage = wPct,
            savingsPercentage = sPct
        )
    }

    val incomesTotal = analysis.incomesTotal
    val expensesTotal = analysis.expensesTotal
    val isUsingFallback = analysis.isUsingFallback
    val referenceIncome = analysis.referenceIncome
    val needsSpent = analysis.needsSpent
    val wantsSpent = analysis.wantsSpent
    val savingsSpent = analysis.savingsSpent
    val needsPercentage = analysis.needsPercentage
    val wantsPercentage = analysis.wantsPercentage
    val savingsPercentage = analysis.savingsPercentage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Visão Geral da Regra 50/30/20",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Esta regra divide sua renda líquida em 3 grandes categorias para um orçamento equilibrado.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isUsingFallback) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nenhuma receita ativa cadastrada. Usando renda simulada de R$ 3.000,00 para demonstração.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Distribuição Atual (Renda: ${formatCurrency(referenceIncome)})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val totalCombined = needsPercentage + wantsPercentage + savingsPercentage
            val normNeeds = if (totalCombined > 0) (needsPercentage / totalCombined).toFloat() else 0.33f
            val normWants = if (totalCombined > 0) (wantsPercentage / totalCombined).toFloat() else 0.33f
            val normSavings = if (totalCombined > 0) (savingsPercentage / totalCombined).toFloat() else 0.34f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                if (normNeeds > 0.05f) {
                    Box(
                        modifier = Modifier
                            .weight(normNeeds.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${needsPercentage.toInt()}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (normWants > 0.05f) {
                    Box(
                        modifier = Modifier
                            .weight(normWants.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(Color(0xFFFFA726)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${wantsPercentage.toInt()}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (normSavings > 0.05f) {
                    Box(
                        modifier = Modifier
                            .weight(normSavings.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(Color(0xFF26A69A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${savingsPercentage.toInt()}%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Text("Necessidades (50%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFA726)))
                    Text("Desejos (30%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF26A69A)))
                    Text("Poupança (20%)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RuleCategoryDetailCard(
            title = "Necessidades (Essencial)",
            targetPct = 50,
            actualAmount = needsSpent,
            actualPct = needsPercentage,
            referenceIncome = referenceIncome,
            color = MaterialTheme.colorScheme.primary,
            description = "Moradia, alimentação, contas básicas, saúde e transporte.",
            recommendation = if (needsPercentage > 50) {
                "Seus gastos essenciais ultrapassaram os 50%. Tente renegociar tarifas recorrentes, cortar assinaturas ou economizar no mercado."
            } else {
                "Excelente! Seus custos essenciais estão saudáveis e sob controle."
            }
        )

        RuleCategoryDetailCard(
            title = "Desejos Pessoais (Estilo de Vida)",
            targetPct = 30,
            actualAmount = wantsSpent,
            actualPct = wantsPercentage,
            referenceIncome = referenceIncome,
            color = Color(0xFFFFA726),
            description = "Lazer, jantares fora, cinema, viagens e compras por impulso.",
            recommendation = if (wantsPercentage > 30) {
                "Você está gastando mais do que 30% em lazer e supérfluos. Experimente estabelecer um limite semanal fixo de 'dinheiro livre' para diversão."
            } else {
                "Seu estilo de vida cabe perfeitamente no seu orçamento. Continue mantendo o equilíbrio!"
            }
        )

        RuleCategoryDetailCard(
            title = "Poupança e Investimentos",
            targetPct = 20,
            actualAmount = savingsSpent,
            actualPct = savingsPercentage,
            referenceIncome = referenceIncome,
            color = Color(0xFF26A69A),
            description = "Aportes em investimentos, reserva de emergência e quitação de dívidas.",
            recommendation = if (savingsPercentage < 20) {
                "Sua poupança está abaixo da meta ideal de 20%. Tente adotar o hábito de 'pagar-se primeiro' logo que receber seu salário."
            } else {
                "Parabéns! Você está construindo sua liberdade financeira em um ritmo acelerado!"
            }
        )
    }
}

@Composable
fun RuleCategoryDetailCard(
    title: String,
    targetPct: Int,
    actualAmount: Double,
    actualPct: Double,
    referenceIncome: Double,
    color: Color,
    description: String,
    recommendation: String
) {
    val targetAmount = referenceIncome * (targetPct / 100.0)
    val isOverLimit = actualPct > targetPct && targetPct != 20
    val isUnderSavings = targetPct == 20 && actualPct < 20

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isOverLimit || isUnderSavings) MaterialTheme.colorScheme.errorContainer
                            else Color(0xFFE8F5E9)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isOverLimit) "Acima do Alvo" else if (isUnderSavings) "Abaixo do Alvo" else "Saudável",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverLimit || isUnderSavings) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Alvo Recomendado ($targetPct%)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(targetAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Seu Gasto Atual (${actualPct.toInt()}%)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(actualAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Dica: $recommendation",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

data class InvestmentSimulatorAnalysis(
    val totalAccumulated: Double,
    val totalInvested: Double,
    val interestEarned: Double
)

@Composable
fun InvestmentSimulatorTab() {
    var initialInput by remember { mutableStateOf("") }
    var monthlyInput by remember { mutableStateOf("") }
    var interestRateStr by remember { mutableStateOf("10.5") }
    var timeYears by remember { mutableStateOf(5f) }

    val analysis = remember(initialInput, monthlyInput, interestRateStr, timeYears) {
        val initialCapital = parseCurrencyInput(initialInput)
        val monthlySavings = parseCurrencyInput(monthlyInput)
        val annualRate = interestRateStr.toDoubleOrNull() ?: 0.0

        val months = (timeYears * 12).toInt()
        val monthlyRate = Math.pow(1.0 + (annualRate / 100.0), 1.0 / 12.0) - 1.0

        var totalAccumulatedVal = initialCapital
        var totalInvestedVal = initialCapital

        if (months > 0) {
            if (monthlyRate > 0) {
                val fvCapital = initialCapital * Math.pow(1.0 + monthlyRate, months.toDouble())
                val fvContributions = monthlySavings * ((Math.pow(1.0 + monthlyRate, months.toDouble()) - 1.0) / monthlyRate) * (1.0 + monthlyRate)
                totalAccumulatedVal = fvCapital + fvContributions
                totalInvestedVal = initialCapital + (monthlySavings * months)
            } else {
                totalAccumulatedVal = initialCapital + (monthlySavings * months)
                totalInvestedVal = totalAccumulatedVal
            }
        }

        val interestEarnedVal = maxOf(0.0, totalAccumulatedVal - totalInvestedVal)
        
        InvestmentSimulatorAnalysis(
            totalAccumulated = totalAccumulatedVal,
            totalInvested = totalInvestedVal,
            interestEarned = interestEarnedVal
        )
    }

    val totalAccumulated = analysis.totalAccumulated
    val totalInvested = analysis.totalInvested
    val interestEarned = analysis.interestEarned

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Simulador de Juros Compostos",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Compare o poder do tempo e dos aportes frequentes no seu patrimônio.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = initialInput,
                onValueChange = { initialInput = formatCurrencyInput(it) },
                label = { Text("Aporte Inicial") },
                placeholder = { Text("R$ 0,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = monthlyInput,
                onValueChange = { monthlyInput = formatCurrencyInput(it) },
                label = { Text("Aporte Mensal") },
                placeholder = { Text("R$ 0,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = interestRateStr,
                    onValueChange = { interestRateStr = it },
                    label = { Text("Juros (% ao ano)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Column(modifier = Modifier.weight(1.5f)) {
                    Text("Tempo: ${timeYears.toInt()} anos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = timeYears,
                        onValueChange = { timeYears = it },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sugestões:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    "CDB (100% CDI)" to "10.5",
                    "Poupança" to "6.17",
                    "Ações / FIIs" to "12.0"
                ).forEach { (label, rate) ->
                    AssistChip(
                        onClick = { interestRateStr = rate },
                        label = { Text(label, fontSize = 9.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gráfico de Crescimento do Patrimônio",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val maxVal = maxOf(totalAccumulated, 1.0)
            val investedRatio = (totalInvested / maxVal).toFloat()
            val interestRatio = (interestEarned / maxVal).toFloat()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .fillMaxHeight(investedRatio.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Capital Investido", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalInvested), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .fillMaxHeight(interestRatio.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(Color(0xFFFFB300))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Juros Acumulados", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+ " + formatCurrency(interestEarned), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Acumulado", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = formatCurrency(totalAccumulated),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

data class CashProjectionAnalysis(
    val totalIncomes: Double,
    val totalExpenses: Double,
    val currentBalance: Double,
    val avgMonthlyIncome: Double,
    val avgMonthlyExpense: Double,
    val monthlyNetSavings: Double,
    val unpaidBillsTotal: Double,
    val proj30Days: Double,
    val proj60Days: Double,
    val proj90Days: Double
)

@Composable
fun CashProjectionTab(transactions: List<Transaction>, bills: List<UpcomingBill>) {
    val analysis = remember(transactions, bills) {
        val totalIncomesVal = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val totalExpensesVal = transactions.filter { it.isExpense }.sumOf { it.amount }
        val currentBalanceVal = totalIncomesVal - totalExpensesVal

        val formats = transactions.map { SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(it.date) }.distinct()
        val distinctMonthsCount = maxOf(formats.size, 1)

        val avgMonthlyIncomeVal = totalIncomesVal / distinctMonthsCount
        val avgMonthlyExpenseVal = totalExpensesVal / distinctMonthsCount
        val monthlyNetSavingsVal = avgMonthlyIncomeVal - avgMonthlyExpenseVal

        val unpaidBillsTotalVal = bills.filter { !it.isPaid }.sumOf { it.amount }

        val proj30DaysVal = currentBalanceVal + monthlyNetSavingsVal - unpaidBillsTotalVal
        val proj60DaysVal = currentBalanceVal + (monthlyNetSavingsVal * 2.0) - unpaidBillsTotalVal
        val proj90DaysVal = currentBalanceVal + (monthlyNetSavingsVal * 3.0) - unpaidBillsTotalVal

        CashProjectionAnalysis(
            totalIncomes = totalIncomesVal,
            totalExpenses = totalExpensesVal,
            currentBalance = currentBalanceVal,
            avgMonthlyIncome = avgMonthlyIncomeVal,
            avgMonthlyExpense = avgMonthlyExpenseVal,
            monthlyNetSavings = monthlyNetSavingsVal,
            unpaidBillsTotal = unpaidBillsTotalVal,
            proj30Days = proj30DaysVal,
            proj60Days = proj60DaysVal,
            proj90Days = proj90DaysVal
        )
    }

    val totalIncomes = analysis.totalIncomes
    val totalExpenses = analysis.totalExpenses
    val currentBalance = analysis.currentBalance
    val avgMonthlyIncome = analysis.avgMonthlyIncome
    val avgMonthlyExpense = analysis.avgMonthlyExpense
    val monthlyNetSavings = analysis.monthlyNetSavings
    val unpaidBillsTotal = analysis.unpaidBillsTotal
    val proj30Days = analysis.proj30Days
    val proj60Days = analysis.proj60Days
    val proj90Days = analysis.proj90Days

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Previsão Inteligente de Saldo",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Entenda onde você estará financeiramente com base em seus hábitos atuais e contas ativas.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Saldo Atual", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(currentBalance), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Economia Mensal Média", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(monthlyNetSavings),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (monthlyNetSavings >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                    )
                }
            }

            if (unpaidBillsTotal > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Contas em Aberto a Pagar:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("- ${formatCurrency(unpaidBillsTotal)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Linha do Tempo Projetada",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProjectionStepItem(
                label = "Hoje",
                amount = currentBalance,
                color = MaterialTheme.colorScheme.primary,
                desc = "Seu ponto de partida atual"
            )

            ProjectionLineConnector()

            ProjectionStepItem(
                label = "Em 30 Dias",
                amount = proj30Days,
                color = if (proj30Days >= currentBalance) Color(0xFF00C853) else Color(0xFFFF9100),
                desc = if (proj30Days >= currentBalance) "Crescimento previsto" else "Redução prevista devido a contas pendentes"
            )

            ProjectionLineConnector()

            ProjectionStepItem(
                label = "Em 60 Dias",
                amount = proj60Days,
                color = if (proj60Days >= currentBalance) Color(0xFF00C853) else Color(0xFFFF9100),
                desc = "Resultado acumulado de 2 meses"
            )

            ProjectionLineConnector()

            ProjectionStepItem(
                label = "Em 90 Dias",
                amount = proj90Days,
                color = if (proj90Days >= currentBalance) Color(0xFF00C853) else Color(0xFFFF9100),
                desc = "Sua perspectiva para o próximo trimestre"
            )

            Spacer(modifier = Modifier.height(20.dp))

            val isHealthy = monthlyNetSavings >= 0.0 && proj90Days > currentBalance
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isHealthy) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, if (isHealthy) Color(0xFF81C784) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (isHealthy) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHealthy) "Diagnóstico: Superávit Saudável" else "Diagnóstico: Risco de Déficit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHealthy) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isHealthy) {
                                "Seu padrão de gastos permite que seu saldo cresça. Continue investindo a diferença para aumentar seus rendimentos!"
                            } else {
                                "Seu saldo projetado está em declínio ou negativo. Avalie cortar despesas não-essenciais ou procure aumentar suas receitas."
                            },
                            fontSize = 10.sp,
                            color = if (isHealthy) Color(0xFF388E3C) else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectionStepItem(label: String, amount: Double, color: Color, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = formatCurrency(amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (amount >= 0) color else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun ProjectionLineConnector() {
    Row(modifier = Modifier.padding(start = 5.dp)) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    }
}

data class FireRetirementAnalysis(
    val fireNumber: Double,
    val yearsToFire: Int,
    val monthsToFire: Int,
    val finalAccumulated: Double,
    val currentSavings: Double,
    val calculatedMonths: Int
)

@Composable
fun FireRetirementTab(transactions: List<Transaction>) {
    var monthlySpendStr by remember { mutableStateOf("") }
    var currentSavingsStr by remember { mutableStateOf("") }
    var monthlyInvestStr by remember { mutableStateOf("") }
    var returnRateStr by remember { mutableStateOf("6.0") }

    val analysis = remember(monthlySpendStr, currentSavingsStr, monthlyInvestStr, returnRateStr) {
        val monthlySpend = parseCurrencyInput(monthlySpendStr)
        val currentSavings = parseCurrencyInput(currentSavingsStr)
        val monthlyInvest = parseCurrencyInput(monthlyInvestStr)
        val realAnnualRate = returnRateStr.toDoubleOrNull() ?: 0.0

        val fireNumber = maxOf(monthlySpend * 300.0, 1000.0)
        val monthlyReturnRate = Math.pow(1.0 + (realAnnualRate / 100.0), 1.0 / 12.0) - 1.0

        var calculatedMonths = 0
        var tempAccumulated = currentSavings

        if (tempAccumulated < fireNumber && (monthlyInvest > 0 || monthlyReturnRate > 0)) {
            while (tempAccumulated < fireNumber && calculatedMonths < 1200) {
                tempAccumulated = tempAccumulated * (1.0 + monthlyReturnRate) + monthlyInvest
                calculatedMonths++
            }
        }

        FireRetirementAnalysis(
            fireNumber = fireNumber,
            yearsToFire = calculatedMonths / 12,
            monthsToFire = calculatedMonths % 12,
            finalAccumulated = tempAccumulated,
            currentSavings = currentSavings,
            calculatedMonths = calculatedMonths
        )
    }

    val fireNumber = analysis.fireNumber
    val yearsToFire = analysis.yearsToFire
    val monthsToFire = analysis.monthsToFire
    val currentSavings = analysis.currentSavings
    val calculatedMonths = analysis.calculatedMonths

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Calculadora de Independência Financeira (Movimento FIRE)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Descubra qual o patrimônio necessário para viver apenas dos rendimentos dos seus investimentos.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = monthlySpendStr,
                onValueChange = { monthlySpendStr = formatCurrencyInput(it) },
                label = { Text("Seu Gasto Mensal Desejado na Aposentadoria") },
                placeholder = { Text("R$ 3.000,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = currentSavingsStr,
                onValueChange = { currentSavingsStr = formatCurrencyInput(it) },
                label = { Text("Patrimônio Acumulado Atual") },
                placeholder = { Text("R$ 10.000,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = monthlyInvestStr,
                onValueChange = { monthlyInvestStr = formatCurrencyInput(it) },
                label = { Text("Aporte Mensal Previsto") },
                placeholder = { Text("R$ 500,00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = returnRateStr,
                onValueChange = { returnRateStr = it },
                label = { Text("Taxa de Juros Real Estimada (% ao ano)") },
                placeholder = { Text("6.0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Seu Número FIRE (Alvo de Liberdade)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(fireNumber), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val progress = if (fireNumber > 0) (currentSavings / fireNumber).toFloat().coerceIn(0f, 1f) else 0f
            Text(
                text = "Progresso até a Independência: ${(progress * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0F7FA))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Tempo Estimado Até a Liberdade",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006064)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentSavings >= fireNumber) {
                            "Parabéns! Você já atingiu a Independência Financeira! Seus rendimentos podem cobrir totalmente seus gastos previstos."
                        } else if (calculatedMonths >= 1200) {
                            "Com o aporte mensal atual, levará mais de 100 anos. Considere aumentar seu aporte ou economizar um pouco mais para acelerar o processo."
                        } else {
                            "Faltam aproximadamente $yearsToFire anos e $monthsToFire meses para você alcançar a sua meta financeira de viver de renda."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF006064),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
