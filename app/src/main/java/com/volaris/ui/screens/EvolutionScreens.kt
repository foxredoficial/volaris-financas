package com.volaris.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volaris.data.UserProfile
import com.volaris.data.WeeklyChallenge
import java.util.Locale

@Composable
fun EvolutionChallengesScreen(
    userProfile: UserProfile,
    weeklyChallenges: List<WeeklyChallenge>,
    onClaimChallengeReward: (challengeId: Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simple and elegant header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Desafios Semanais",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pratique hábitos financeiros saudáveis e ganhe XP",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nível ${userProfile.level}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Active Challenges Count Card
        val completedCount = remember(weeklyChallenges) { weeklyChallenges.count { it.isCompleted && it.isClaimed } }
        val totalCount = weeklyChallenges.size
        val claimableCount = remember(weeklyChallenges) { weeklyChallenges.count { it.isCompleted && !it.isClaimed } }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Progresso de Conclusão",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedCount de $totalCount concluídos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (claimableCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$claimableCount Recompensa(s) pendente(s)!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
        }

        // List of challenges
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (weeklyChallenges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum desafio semanal disponível.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                weeklyChallenges.forEach { challenge ->
                    val isDone = challenge.isCompleted
                    val isClaimed = challenge.isClaimed
                    
                    val challengeIcon = when (challenge.challengeType) {
                        "DELIVERY" -> Icons.Rounded.LocalPizza
                        "CHEF_COZINHA" -> Icons.Rounded.Restaurant
                        "SAVINGS" -> Icons.Rounded.Savings
                        "SUPER_SAVINGS" -> Icons.Rounded.AutoAwesome
                        "BUDGET_LIMIT" -> Icons.Rounded.Shield
                        "BUDGET_LIMIT_3" -> Icons.Rounded.Analytics
                        "BILL_PAYMENT" -> Icons.Rounded.ReceiptLong
                        "INCOME_LOG" -> Icons.Rounded.ShowChart
                        "TRANSACTION_COUNT" -> Icons.Rounded.SwapHoriz
                        "EXPENSE_CONTROL" -> Icons.Rounded.Star
                        "NO_SPEND_DAY" -> Icons.Rounded.Block
                        "EARLY_BILL" -> Icons.Rounded.Bolt
                        "INVESTMENT_LOG" -> Icons.Rounded.ShowChart
                        "BIG_INVESTMENT" -> Icons.Rounded.Diamond
                        "GROCERY_CONTROL" -> Icons.Rounded.ShoppingCart
                        "TRANSPORT_CONTROL" -> Icons.Rounded.DirectionsCar
                        "EDUCATION_LOG" -> Icons.Rounded.School
                        "EMERGENCY_FUND" -> Icons.Rounded.HealthAndSafety
                        "DOUBLE_GOAL" -> Icons.Rounded.Filter2
                        "FREELANCE_INCOME" -> Icons.Rounded.Work
                        "LEISURE_STRICT" -> Icons.Rounded.LocalPlay
                        "AI_ADVICE_CHECK" -> Icons.Rounded.SmartToy
                        "QUIZ_COMPLETE" -> Icons.Rounded.Assignment
                        "MONTHLY_SUMMARY_CHECK" -> Icons.Rounded.BarChart
                        "BILL_COUNT" -> Icons.Rounded.AccountBalanceWallet
                        else -> Icons.Rounded.TaskAlt
                    }

                    val iconColor = when (challenge.challengeType) {
                        "DELIVERY" -> Color(0xFFFF5722)
                        "CHEF_COZINHA" -> Color(0xFFFF9800)
                        "SAVINGS" -> Color(0xFF4CAF50)
                        "SUPER_SAVINGS" -> Color(0xFF009688)
                        "BUDGET_LIMIT" -> Color(0xFF2196F3)
                        "BUDGET_LIMIT_3" -> Color(0xFF3F51B5)
                        "BILL_PAYMENT" -> Color(0xFF9C27B0)
                        "INCOME_LOG" -> Color(0xFF8BC34A)
                        "TRANSACTION_COUNT" -> Color(0xFF607D8B)
                        "EXPENSE_CONTROL" -> Color(0xFFE91E63)
                        "NO_SPEND_DAY" -> Color(0xFFE53935)
                        "EARLY_BILL" -> Color(0xFFFFB300)
                        "INVESTMENT_LOG" -> Color(0xFF00E676)
                        "BIG_INVESTMENT" -> Color(0xFF00B0FF)
                        "GROCERY_CONTROL" -> Color(0xFF8E24AA)
                        "TRANSPORT_CONTROL" -> Color(0xFF0288D1)
                        "EDUCATION_LOG" -> Color(0xFFFF3D00)
                        "EMERGENCY_FUND" -> Color(0xFF2E7D32)
                        "DOUBLE_GOAL" -> Color(0xFFFF4081)
                        "FREELANCE_INCOME" -> Color(0xFF3949AB)
                        "LEISURE_STRICT" -> Color(0xFFD81B60)
                        "AI_ADVICE_CHECK" -> Color(0xFF00ACC1)
                        "QUIZ_COMPLETE" -> Color(0xFF43A047)
                        "MONTHLY_SUMMARY_CHECK" -> Color(0xFF795548)
                        "BILL_COUNT" -> Color(0xFF5E35B1)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("challenge_item_${challenge.challengeType}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isClaimed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDone && !isClaimed) iconColor.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(iconColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = challengeIcon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = challenge.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isClaimed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = challenge.description,
                                    fontSize = 11.sp,
                                    color = if (isClaimed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                
                                // Progress bar if not finished
                                if (!isDone) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val pct = remember(challenge.currentValue, challenge.targetValue) {
                                        if (challenge.targetValue > 0) (challenge.currentValue / challenge.targetValue).toFloat().coerceIn(0f, 1f) else 0f
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = iconColor,
                                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.0f", challenge.currentValue)}/${String.format(Locale.getDefault(), "%.0f", challenge.targetValue)}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Action Button
                            if (isClaimed) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Concluído",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (isDone) {
                                Button(
                                    onClick = { onClaimChallengeReward(challenge.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = iconColor),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("claim_reward_button_${challenge.id}")
                                ) {
                                    Text("Coletar +${challenge.xpBonus} XP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text("+${challenge.xpBonus} XP", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EvolutionAchievementsScreen(
    userProfile: UserProfile
) {
    val scrollState = rememberScrollState()
    val unlockedList = remember(userProfile.achievementsUnlocked) {
        userProfile.achievementsUnlocked.split(",").filter { it.isNotEmpty() }.toSet()
    }
    
    // Define achievements mapped to actual database IDs
    val achievements = remember {
        listOf(
            Triple("FIRST_TRANSACTION", "💰 Primeiro Lançamento", "Registre sua primeira movimentação de entrada ou saída."),
            Triple("FIRST_GOAL", "🎯 Planejador Iniciante", "Crie seu primeiro objetivo ou meta de reserva de poupança."),
            Triple("GOAL_COMPLETED", "🏆 Objetivo Alcançado", "Atinja 100% do progresso de uma meta de poupança estabelecida."),
            Triple("BUDGET_CREATED", "🛡️ Orçamento Blindado", "Defina seu primeiro teto de orçamento mensal para uma categoria."),
            Triple("BILL_PAID", "⚡ Compromisso Honrado", "Marque uma conta de vencimento futuro como totalmente paga."),
            Triple("QUIZ_PASS", "🎓 Estudante Consciente", "Acerte pelo menos 3 perguntas do Quiz de Educação Financeira."),
            Triple("LEVEL_10", "👑 Guardião Financeiro", "Tenha consistência e atinja o respeitável Nível 10."),
            Triple("LEVEL_20", "💎 Investidor Lendário", "Demonstre maestria absoluta alcançando o Nível 20."),
            Triple("CHALLENGE_COMPLETED", "🌟 Desafiante", "Resgate a recompensa de seu primeiro desafio semanal.")
        )
    }

    val unlockedCount = remember(unlockedList, achievements) { achievements.count { unlockedList.contains(it.first) } }
    val totalCount = achievements.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Minhas Conquistas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Seus marcos históricos de superação e foco",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Progress bar card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Conquistas Desbloqueadas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$unlockedCount de $totalCount", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val pct = if (totalCount > 0) (unlockedCount.toFloat() / totalCount.toFloat()) else 0f
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            }
        }

        // Achievements list
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            achievements.forEach { (id, title, desc) ->
                val isUnlocked = unlockedList.contains(id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                lineHeight = 15.sp
                            )
                        }

                        if (isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "+100 XP",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EvolutionLevelScreen(
    userProfile: UserProfile
) {
    val scrollState = rememberScrollState()
    
    // XP math
    val xpRequired = userProfile.level * 1000
    val xpProgressPct = remember(userProfile.xp, xpRequired) { (userProfile.xp.toFloat() / xpRequired.toFloat()).coerceIn(0f, 1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Nível & Perfil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Acompanhe seus títulos e privilégios conquistados",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile Card with Level and dynamic Titles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big Level Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NÍVEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${userProfile.level}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = userProfile.financialTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Título Financeiro Oficial",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // XP Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progresso para o Nível ${userProfile.level + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${userProfile.xp} / $xpRequired XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { xpProgressPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // Milestone achievements benefits
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Próximos Títulos Desbloqueáveis",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val milestones = remember {
                    listOf(
                        Pair(5, "Planejador Consciente 📈"),
                        Pair(10, "Guardião do Orçamento 🛡️"),
                        Pair(15, "Mago da Poupança 🧙‍♂️"),
                        Pair(20, "Investidor Lendário 👑"),
                        Pair(25, "Grão-Mestre das Finanças 💎")
                    )
                }

                milestones.forEach { (lvl, title) ->
                    val isReached = userProfile.level >= lvl
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isReached) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nível $lvl: $title",
                                fontSize = 12.sp,
                                color = if (isReached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = if (isReached) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (isReached) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Ativo", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EvolutionQuizScreen(
    onQuizCompleted: (score: Int) -> Unit
) {
    // 15 personal finance questions
    val questions = remember {
        listOf(
            QuizQuestion(
                question = "Qual é o valor recomendado para formar uma Reserva de Emergência sólida?",
                options = listOf(
                    "O equivalente a 1 mês de seus custos fixos.",
                    "Entre 3 a 6 meses de suas despesas mensais essenciais.",
                    "Sempre guardar o equivalente a 12 meses do seu salário bruto.",
                    "Não é recomendável ter reserva, e sim investir tudo em ações de risco."
                ),
                correctAnswerIndex = 1,
                explanation = "Especialistas recomendam guardar entre 3 a 6 meses de despesas fixas para cobrir imprevistos cotidianos, garantindo paz e liquidez sem precisar contrair dívidas."
            ),
            QuizQuestion(
                question = "O que caracteriza os Juros Compostos a favor de quem investe?",
                options = listOf(
                    "Os juros incidem apenas sobre o valor que você depositou inicialmente.",
                    "Eles cobram taxas abusivas do investidor ao longo do tempo.",
                    "Eles calculam juros sobre o valor inicial acumulado mais os juros gerados anteriormente (juros sobre juros).",
                    "São taxas fixadas pelo governo que nunca mudam com a inflação."
                ),
                correctAnswerIndex = 2,
                explanation = "Juros Compostos calculam rendimentos sobre rendimentos já acumulados, gerando um efeito exponencial de crescimento de capital ao longo do tempo!"
            ),
            QuizQuestion(
                question = "Na popular regra de orçamento '50/30/20', o que representam os 20%?",
                options = listOf(
                    "Despesas de subsistência e moradia essenciais.",
                    "Lazer, saídas, jantares e assinaturas supérfluas.",
                    "Poupança, investimentos, quitação de dívidas ou reserva futura.",
                    "Doações de caridade ou pagamento de impostos anuais."
                ),
                correctAnswerIndex = 2,
                explanation = "A regra dita que 50% vai para Necessidades, 30% para Desejos e 20% para Poupança ou investimentos de longo prazo."
            ),
            QuizQuestion(
                question = "Qual é o maior perigo de realizar apenas o pagamento mínimo da fatura do cartão de crédito?",
                options = listOf(
                    "Entrar no crédito rotativo, cujos juros são dos mais altos e agressivos do mercado.",
                    "Ter a conta bancária eletrônica suspensa pelo governo.",
                    "Perder imediatamente os pontos ou milhas que acumulou na compra.",
                    "Nenhum perigo, pois o banco parcela o saldo de graça para o mês seguinte."
                ),
                correctAnswerIndex = 0,
                explanation = "O crédito rotativo possui juros abusivos de mais de 10% a 15% ao mês, o que pode rapidamente transformar uma dívida pequena em um efeito bola de neve impagável."
            ),
            QuizQuestion(
                question = "Antes de começar a investir em ativos de alta oscilação (renda variável), qual deve ser seu primeiro passo?",
                options = listOf(
                    "Comprar o carro dos seus sonhos parcelado.",
                    "Pegar um empréstimo pessoal rápido para alavancar a carteira.",
                    "Formar uma reserva de emergência estável e de baixo risco.",
                    "Fazer um curso avançado de day trade e usar o saldo do aluguel."
                ),
                correctAnswerIndex = 2,
                explanation = "A base da saúde financeira é a segurança. Ter uma reserva com liquidez diária evita que você seja forçado a vender ações ou fundos em um momento de queda do mercado."
            ),
            QuizQuestion(
                question = "O que representa o fenômeno econômico da Inflação?",
                options = listOf(
                    "O aumento repentino na taxa de juros básica SELIC.",
                    "A desvalorização do dinheiro e aumento contínuo e generalizado dos preços de bens e serviços.",
                    "Uma promoção generalizada em lojas de departamentos no fim do ano.",
                    "O crescimento do Produto Interno Bruto (PIB) de um país."
                ),
                correctAnswerIndex = 1,
                explanation = "A inflação reduz o poder de compra da moeda. R$ 100 hoje compram bem menos produtos de supermercado do que há 5 anos devido à inflação constante."
            ),
            QuizQuestion(
                question = "O que significa 'Diversificar' sua carteira de investimentos?",
                options = listOf(
                    "Concentrar todo o capital na melhor empresa da bolsa.",
                    "Colocar dinheiro em apenas uma meta e sacar no dia seguinte.",
                    "Distribuir o capital entre diferentes classes e emissores de ativos (ex: renda fixa, ações, FIIs) para mitigar riscos.",
                    "Investir apenas em moedas estrangeiras flutuantes."
                ),
                correctAnswerIndex = 2,
                explanation = "Diversificar dilui os riscos: se um setor da economia for mal, seus outros investimentos em setores saudáveis compensam e estabilizam os ganhos."
            ),
            QuizQuestion(
                question = "A taxa CDI está diretamente atrelada e muito próxima a qual indicador econômico do Brasil?",
                options = listOf(
                    "Taxa Selic (Taxa básica de juros).",
                    "IPCA (Índice de preços ao consumidor).",
                    "Dólar comercial americano.",
                    "Ibovespa."
                ),
                correctAnswerIndex = 0,
                explanation = "O CDI (Certificado de Depósito Interbancário) anda praticamente lado a lado com a taxa Selic. Se a Selic sobe, o CDI também sobe, melhorando os ganhos da renda fixa."
            ),
            QuizQuestion(
                question = "Qual é a principal diferença entre Gestão Ativa e Gestão Passiva de Fundos de Investimentos?",
                options = listOf(
                    "A gestão ativa não cobra taxa de administração nenhuma do cotista.",
                    "A gestão passiva tenta superar o mercado, enquanto a ativa tenta apenas se manter estável.",
                    "A gestão ativa busca superar o benchmark (índice de referência) selecionando papéis, enquanto a passiva apenas replica o índice.",
                    "Fundos de gestão ativa investem somente em poupança comum."
                ),
                correctAnswerIndex = 2,
                explanation = "Na gestão ativa, o gestor escolhe a dedo os ativos para bater a meta de rendimento. Na passiva (ex: ETFs), o fundo segue passivamente um índice como o Ibovespa."
            ),
            QuizQuestion(
                question = "Sobre a tabela regressiva do Imposto de Renda em investimentos de Renda Fixa de longo prazo, o que ocorre com a alíquota tributária?",
                options = listOf(
                    "Ela aumenta quanto mais tempo você deixa o dinheiro investido.",
                    "Ela diminui gradativamente ao longo do tempo (de 22,5% até 15% após 2 anos).",
                    "Permanece fixa em 27,5% independente de qualquer prazo.",
                    "Renda fixa nunca é tributada pelo Imposto de Renda."
                ),
                correctAnswerIndex = 1,
                explanation = "A tabela regressiva beneficia quem investe a longo prazo: começa em 22.5% (até 180 dias) e cai até o patamar mínimo de 15.0% após 720 dias investidos."
            ),
            QuizQuestion(
                question = "O que é o programa federal Tesouro Direto?",
                options = listOf(
                    "Um site para comprar ações da Petrobras sem pagar taxas.",
                    "Uma plataforma do governo que permite a pessoas físicas emprestar dinheiro à União em troca de juros futuros.",
                    "O caixa físico do Banco Central onde se saca dinheiro de graça.",
                    "Um financiamento habitacional de juros baixos para famílias carentes."
                ),
                correctAnswerIndex = 1,
                explanation = "Investir no Tesouro Direto é emprestar dinheiro para o governo brasileiro financiar suas atividades, sendo um dos investimentos mais seguros do país."
            ),
            QuizQuestion(
                question = "Qual é a melhor estratégia para acumular patrimônio consistente para a aposentadoria?",
                options = listOf(
                    "Comprar bilhetes de loteria semanalmente.",
                    "Deixar todo o dinheiro na caderneta de poupança clássica.",
                    "Investimentos regulares e diversificados mantidos de forma consistente ao longo de décadas.",
                    "Tentar adivinhar a ação que vai explodir amanhã e vender no mesmo dia."
                ),
                correctAnswerIndex = 2,
                explanation = "Investimentos constantes acionam o efeito dos juros compostos. No longo prazo, a disciplina e a paciência vencem as especulações rápidas."
            ),
            QuizQuestion(
                question = "O que são Fundos de Investimento Imobiliário (FIIs)?",
                options = listOf(
                    "Financiamentos de imóveis residenciais de bancos públicos.",
                    "Condomínios fechados construídos pelo governo.",
                    "Fundos que reúnem investidores para aplicar em imóveis comerciais e distribuem aluguéis mensais isentos de IR.",
                    "Leilões judiciais de casas em atraso de pagamento."
                ),
                correctAnswerIndex = 2,
                explanation = "Os FIIs dividem o aluguel de shoppings, galpões e prédios comerciais entre os donos de cotas, pagando rendimentos mensais direto na sua conta."
            ),
            QuizQuestion(
                question = "O que significa o conceito de Liquidez em finanças?",
                options = listOf(
                    "A facilidade e agilidade em transformar um ativo de volta em dinheiro em espécie sem perda de valor.",
                    "A quantidade de dinheiro em circulação nos bancos físicos.",
                    "A solubilidade de ativos químicos em bolsas agrícolas.",
                    "O montante total de imposto cobrado sobre lucros de empresas."
                ),
                correctAnswerIndex = 0,
                explanation = "Liquidez diária significa resgatar o dinheiro no mesmo dia (ex: Tesouro Selic). Já imóveis têm baixíssima liquidez, pois levam meses ou anos para serem vendidos."
            ),
            QuizQuestion(
                question = "Qual é a melhor estratégia para evitar juros e multas desnecessários?",
                options = listOf(
                    "Deixar para pagar as contas apenas quando receber o segundo aviso de cobrança.",
                    "Programar o pagamento de contas recorrentes no débito automático ou agendamento financeiro antecipado.",
                    "Ignorar as faturas que chegam por e-mail e focar apenas no extrato bancário impresso.",
                    "Pedir empréstimos rotativos mensais para cobrir despesas fixas recorrentes."
                ),
                correctAnswerIndex = 1,
                explanation = "A organização prévia e o agendamento de faturas evitam esquecimentos e multas de atraso, que corroem silenciosamente sua capacidade de poupança."
            )
        )
    }

    // Active state of quiz
    var isQuizStarted by remember { mutableStateOf(false) }
    var selectedQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }

    // Shuffles and selects 5 questions on start
    val startQuiz = {
        selectedQuestions = questions.shuffled().take(5)
        currentQuestionIndex = 0
        selectedAnswerIndex = null
        score = 0
        isQuizStarted = true
        isQuizFinished = false
        showExplanation = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Quizzes Financeiros",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Desafie sua mente e ganhe XP dominando conceitos",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isQuizStarted) {
            // Welcome screen inside Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Volaris Arena Quiz",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Responda a 5 perguntas aleatórias selecionadas de nosso banco de dados especializado. Ganhe até +100 XP por concluir e consolide seu conhecimento econômico!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = startQuiz,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_quiz_screen_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Desafio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (isQuizFinished) {
            // Result Screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Quiz Concluído!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Sua pontuação: $score de 5 acertos",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Dynamic advice depending on score
                    val (titleAdvice, textAdvice) = when (score) {
                        5 -> Pair("🏆 Mestre das Finanças", "Excelente! Você tem total domínio sobre planejamento, reserva de emergência, juros compostos e boas práticas financeiras. Continue liderando sua vida com maestria!")
                        3, 4 -> Pair("📈 Investidor Consciente", "Muito bom! Seus fundamentos financeiros são bem sólidos. Continue acompanhando seus relatórios e orçamentos na Volaris para refinar ainda mais o seu progresso!")
                        else -> Pair("🌱 Aprendiz Financeiro", "Bom começo! Lidar com finanças é um aprendizado constante. Continue monitorando suas receitas, despesas e metas na Volaris para ganhar cada vez mais prática e confiança!")
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = titleAdvice,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = textAdvice,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = startQuiz,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tentar Novamente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onQuizCompleted(score)
                                isQuizStarted = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fechar & Salvar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Live Quiz Question Screen
            val questionObj = selectedQuestions[currentQuestionIndex]
            
            Text(
                text = "Pergunta ${currentQuestionIndex + 1} de 5",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            // Linear Progress indicators
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1) / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = questionObj.question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Options list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        questionObj.options.forEachIndexed { idx, optionText ->
                            val isSelected = selectedAnswerIndex == idx
                            val isCorrect = idx == questionObj.correctAnswerIndex
                            val showAsCorrect = showExplanation && isCorrect
                            val showAsIncorrect = showExplanation && isSelected && !isCorrect
                            
                            val containerColor = when {
                                showAsCorrect -> Color(0xFF00C853).copy(alpha = 0.12f)
                                showAsIncorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                            
                            val borderColor = when {
                                showAsCorrect -> Color(0xFF00C853)
                                showAsIncorrect -> MaterialTheme.colorScheme.error
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            }

                            val textColor = when {
                                showAsCorrect -> Color(0xFF00C853)
                                showAsIncorrect -> MaterialTheme.colorScheme.error
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !showExplanation) {
                                        selectedAnswerIndex = idx
                                    }
                                    .testTag("quiz_option_$idx"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(borderColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ('A'.code + idx).toChar().toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = optionText,
                                        fontSize = 12.sp,
                                        color = if (showExplanation) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Explanation block
                    if (showExplanation) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Explicação da IA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = questionObj.explanation,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!showExplanation) {
                    Button(
                        onClick = {
                            if (selectedAnswerIndex != null) {
                                if (selectedAnswerIndex == questionObj.correctAnswerIndex) {
                                    score += 1
                                }
                                showExplanation = true
                            }
                        },
                        enabled = selectedAnswerIndex != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp).testTag("quiz_confirm_button")
                    ) {
                        Text("Verificar Resposta", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentQuestionIndex < 4) {
                                currentQuestionIndex += 1
                                selectedAnswerIndex = null
                                showExplanation = false
                            } else {
                                isQuizFinished = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp).testTag("quiz_next_button")
                    ) {
                        Text(
                            text = if (currentQuestionIndex < 4) {
                                "Próxima Pergunta"
                            } else {
                                "Ver Resultado"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)
