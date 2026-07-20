package com.volaris

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.volaris.data.*
import com.volaris.ui.*
import com.volaris.ui.theme.MyApplicationTheme
import com.volaris.ui.screens.*
import com.volaris.ui.dialogs.*
import com.volaris.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

enum class AppScreen {
    RESUMO, TRANSACOES, METAS_ORCAMENTO,
    DESAFIOS, CONQUISTAS, NIVEL,
    QUIZ,
    EXPORTAR, SOBRE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = FinanceDatabase.getDatabase(this)
        val repository = FinanceRepository(database.financeDao())

        setContent {
            val viewModel: FinanceViewModel by viewModels { FinanceViewModelFactory(repository) }
            val manualDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val useDarkTheme = manualDarkMode ?: isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = useDarkTheme) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Observe Data States
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val isDarkModeState by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle(0.0)
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle(0.0)
    val netBalance by viewModel.netBalance.collectAsStateWithLifecycle(0.0)

    val budgetAlerts by viewModel.budgetAlerts.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val isMonthlySummaryLoading by viewModel.isMonthlySummaryLoading.collectAsStateWithLifecycle()

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val levelUpCelebration by viewModel.levelUpCelebration.collectAsStateWithLifecycle()
    val weeklyChallenges by viewModel.weeklyChallenges.collectAsStateWithLifecycle()

    // Navigation and UI state
    var currentScreen by remember { mutableStateOf(AppScreen.RESUMO) }
    var hideBalances by remember { mutableStateOf(false) }

    // Dialog control states
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showBillSplitterDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showFinancialQuizDialog by remember { mutableStateOf(false) }
    var showMonthlySummaryDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }

    var showSplashScreen by remember { mutableStateOf(true) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showSplashScreen) {
        VolarisSplashScreen(onTimeout = { showSplashScreen = false })
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Volaris Finanças",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nível ${userProfile?.level ?: 1} • ${userProfile?.financialTitle ?: "Iniciante"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // --- FINANCEIRO ---
                        Text(
                            text = "FINANCEIRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                            letterSpacing = 1.sp
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                            label = { Text("Resumo (Dashboard)", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.RESUMO,
                            onClick = {
                                currentScreen = AppScreen.RESUMO
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_resumo")
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                            label = { Text("Transações", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.TRANSACOES,
                            onClick = {
                                currentScreen = AppScreen.TRANSACOES
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_transacoes")
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.TrackChanges, contentDescription = null) },
                            label = { Text("Metas & Orçamento", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.METAS_ORCAMENTO,
                            onClick = {
                                currentScreen = AppScreen.METAS_ORCAMENTO
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_planejar")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))

                        // --- GAMIFICAÇÃO ---
                        Text(
                            text = "GAMIFICAÇÃO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                            letterSpacing = 1.sp
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.Star, contentDescription = null) },
                            label = { Text("Desafios", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.DESAFIOS,
                            onClick = {
                                currentScreen = AppScreen.DESAFIOS
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_desafios")
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null) },
                            label = { Text("Conquistas", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.CONQUISTAS,
                            onClick = {
                                currentScreen = AppScreen.CONQUISTAS
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_conquistas")
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            label = { Text("Nível & Perfil", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.NIVEL,
                            onClick = {
                                currentScreen = AppScreen.NIVEL
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_nivel")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))

                        // --- EDUCAÇÃO ---
                        Text(
                            text = "EDUCAÇÃO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                            letterSpacing = 1.sp
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.School, contentDescription = null) },
                            label = { Text("Quizzes Financeiros", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.QUIZ,
                            onClick = {
                                currentScreen = AppScreen.QUIZ
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_quiz")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))

                        // --- FERRAMENTAS ---
                        Text(
                            text = "FERRAMENTAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                            letterSpacing = 1.sp
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.Backup, contentDescription = null) },
                            label = { Text("Exportar / Importar", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.EXPORTAR,
                            onClick = {
                                currentScreen = AppScreen.EXPORTAR
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_exportar")
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                            label = { Text("Sobre o Volaris", fontSize = 13.sp) },
                            selected = currentScreen == AppScreen.SOBRE,
                            onClick = {
                                currentScreen = AppScreen.SOBRE
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.testTag("drawer_item_sobre")
                        )
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Volaris Finanças",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Insights,
                                        contentDescription = "Insights",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Planejamento & Controle",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Menu Lateral")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    hideBalances = !hideBalances
                                    val statusMsg = if (hideBalances) "Valores ocultados!" else "Valores visíveis!"
                                    Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("privacy_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Alternar Privacidade",
                                    tint = if (hideBalances) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.toggleDarkMode(systemDark)
                                },
                                modifier = Modifier.testTag("dark_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkModeState == true) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Alternar Modo Escuro",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                floatingActionButton = {
                    if (currentScreen == AppScreen.RESUMO || currentScreen == AppScreen.TRANSACOES) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            // Quick Add FAB
                            FloatingActionButton(
                                onClick = { showQuickAddDialog = true },
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.testTag("quick_add_transaction_fab")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.FlashOn, contentDescription = "Adição Rápida", modifier = Modifier.size(18.dp))
                                    Text("Adição Rápida", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Standard Add FAB
                            FloatingActionButton(
                                onClick = { showAddTransactionDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("add_transaction_fab")
                            ) {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Nova Transação")
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Resumo") },
                            label = { Text("Resumo", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            selected = currentScreen == AppScreen.RESUMO,
                            onClick = { currentScreen = AppScreen.RESUMO },
                            modifier = Modifier.testTag("bottom_nav_resumo")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = "Transações") },
                            label = { Text("Transações", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            selected = currentScreen == AppScreen.TRANSACOES,
                            onClick = { currentScreen = AppScreen.TRANSACOES },
                            modifier = Modifier.testTag("bottom_nav_transacoes")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.TrackChanges, contentDescription = "Orçamentos") },
                            label = { Text("Planejar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            selected = currentScreen == AppScreen.METAS_ORCAMENTO,
                            onClick = { currentScreen = AppScreen.METAS_ORCAMENTO },
                            modifier = Modifier.testTag("bottom_nav_planejar")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.Star, contentDescription = "Desafios") },
                            label = { Text("Desafios", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            selected = currentScreen == AppScreen.DESAFIOS,
                            onClick = { currentScreen = AppScreen.DESAFIOS },
                            modifier = Modifier.testTag("bottom_nav_desafios")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.School, contentDescription = "Educação") },
                            label = { Text("Educação", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            selected = currentScreen == AppScreen.QUIZ,
                            onClick = { currentScreen = AppScreen.QUIZ },
                            modifier = Modifier.testTag("bottom_nav_quiz")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // --- FLOATING LEVEL UP CELEBRATION NOTIFICATION ---
                    AnimatedVisibility(
                        visible = levelUpCelebration != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .zIndex(99f)
                    ) {
                        levelUpCelebration?.let { level ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "PARABÉNS! VOCÊ SUBIU!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Alcançou o Nível $level!",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Seu domínio financeiro continua crescendo! 🚀",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { viewModel.levelUpCelebration.value = null }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Fechar",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.RESUMO -> DashboardScreen(
                                transactions = transactions,
                                goals = goals,
                                bills = bills,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                netBalance = netBalance,
                                hideBalances = hideBalances,
                                onToggleHideBalances = { hideBalances = !hideBalances },
                                onNavigateToTransactions = { currentScreen = AppScreen.TRANSACOES },
                                onNavigateToGoals = { currentScreen = AppScreen.METAS_ORCAMENTO },
                                onNavigateToBills = { currentScreen = AppScreen.METAS_ORCAMENTO },
                                onOpenBillSplitter = { showBillSplitterDialog = true },
                                onOpenAiAdvisor = { showAiAdvisorDialog = true },
                                onOpenMonthlySummary = { showMonthlySummaryDialog = true },
                                onOpenCalculator = { showCalculatorDialog = true },
                                budgetAlerts = budgetAlerts,
                                userProfile = userProfile ?: UserProfile(),
                                onToggleBillPaid = { viewModel.toggleBillPayment(it) }
                            )
                            AppScreen.TRANSACOES -> TransactionsScreen(
                                transactions = transactions,
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                hideBalances = hideBalances
                            )
                            AppScreen.METAS_ORCAMENTO -> PlanningScreen(
                                goals = goals,
                                onAddGoalClick = { showAddGoalDialog = true },
                                onContributeGoal = { goal, amt ->
                                    viewModel.updateGoal(goal.copy(currentAmount = goal.currentAmount + amt))
                                },
                                onDeleteGoal = { viewModel.deleteGoal(it) },
                                bills = bills,
                                budgets = budgets,
                                transactions = transactions,
                                onAddBillClick = { showAddBillDialog = true },
                                onToggleBillPaid = { viewModel.toggleBillPayment(it) },
                                onDeleteBill = { viewModel.deleteBill(it) },
                                onAddBudgetClick = { showAddBudgetDialog = true },
                                onDeleteBudget = { viewModel.deleteBudget(it) },
                                onAddTransaction = { title, amount, isExpense, category, date, desc, tags ->
                                    viewModel.addTransaction(
                                        title = title,
                                        amount = amount,
                                        isExpense = isExpense,
                                        category = category,
                                        date = date,
                                        description = desc,
                                        isPaid = true,
                                        tags = tags
                                    )
                                }
                            )
                            AppScreen.DESAFIOS -> EvolutionChallengesScreen(
                                userProfile = userProfile ?: UserProfile(),
                                weeklyChallenges = weeklyChallenges,
                                onClaimChallengeReward = { id -> viewModel.claimChallengeReward(id) }
                            )
                            AppScreen.CONQUISTAS -> EvolutionAchievementsScreen(
                                userProfile = userProfile ?: UserProfile()
                            )
                            AppScreen.NIVEL -> EvolutionLevelScreen(
                                userProfile = userProfile ?: UserProfile()
                            )
                            AppScreen.QUIZ -> EvolutionQuizScreen(
                                onQuizCompleted = { score -> viewModel.completeQuiz(score) }
                            )
                            AppScreen.EXPORTAR -> ExportScreen(
                                onExportCsv = { start, end, cats -> viewModel.shareCsvReport(context, start, end, cats) },
                                onExportPdf = { start, end, cats ->
                                    val filtered = viewModel.transactions.value.filter { tx ->
                                        val matchStart = start == null || tx.date >= start
                                        val matchEnd = end == null || tx.date <= end
                                        val matchCat = cats.isEmpty() || cats.contains(tx.category)
                                        matchStart && matchEnd && matchCat
                                    }
                                    com.volaris.ui.components.PdfExportHelper.exportTransactionsPdf(
                                        context = context,
                                        transactions = filtered,
                                        startDate = start,
                                        endDate = end,
                                        selectedCategories = cats
                                    )
                                },
                                onExportBackup = { viewModel.exportBackupJson(context) },
                                onImportBackup = { json ->
                                    viewModel.importBackupJson(context, json) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Cópia de segurança restaurada com sucesso!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Erro ao processar arquivo. Verifique o formato do backup.", Toast.LENGTH_LONG).show()
                                        }
                                     }
                                }
                            )
                            AppScreen.SOBRE -> AboutScreen(
                                onVisitGithub = { uriHandler.openUri("https://github.com/foxredoficial") }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTransactionDialog = false },
            onConfirm = { title, amount, isExpense, category, date, desc, tags ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    isExpense = isExpense,
                    category = category,
                    date = date,
                    description = desc,
                    tags = tags
                )
                showAddTransactionDialog = false
                Toast.makeText(context, "Transação cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, target, current, monthYear ->
                viewModel.addGoal(name, target, current, monthYear)
                showAddGoalDialog = false
                Toast.makeText(context, "Meta criada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddBillDialog) {
        AddBillDialog(
            onDismiss = { showAddBillDialog = false },
            onConfirm = { title, amount, dueDate, category ->
                viewModel.addBill(title, amount, dueDate, category)
                showAddBillDialog = false
                Toast.makeText(context, "Conta de vencimento adicionada!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { category, limit ->
                viewModel.addBudget(category, limit)
                showAddBudgetDialog = false
                Toast.makeText(context, "Orçamento configurado com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showBillSplitterDialog) {
        BillSplitterDialog(
            onDismiss = { showBillSplitterDialog = false },
            onConfirmSplit = { title, amt, category ->
                viewModel.addTransaction(
                    title = "Rachador: $title",
                    amount = amt,
                    isExpense = true,
                    category = category,
                    date = System.currentTimeMillis(),
                    description = "Minha parte rachada de '$title'."
                )
                showBillSplitterDialog = false
                Toast.makeText(context, "Minha parte registrada com sucesso!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    if (showAiAdvisorDialog) {
        AiAdvisorDialog(
            response = aiResponse,
            isLoading = isAiLoading,
            chatMessages = chatMessages,
            isChatLoading = isChatLoading,
            onDismiss = { showAiAdvisorDialog = false },
            onRefresh = { viewModel.getAiFinancialAdvice() },
            onSendChatMessage = { text -> viewModel.sendChatUserMessage(text) },
            onClearChat = { viewModel.clearChat() },
            onInitChat = { viewModel.initChatIfEmpty() }
        )
    }

    if (showFinancialQuizDialog) {
        FinancialQuizDialog(
            onDismiss = { showFinancialQuizDialog = false },
            onQuizCompleted = { score -> viewModel.completeQuiz(score) }
        )
    }

    if (levelUpCelebration != null) {
        LevelUpCelebrationDialog(
            level = levelUpCelebration!!,
            onDismiss = { viewModel.levelUpCelebration.value = null }
        )
    }

    if (showQuickAddDialog) {
        QuickAddDialog(
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { title, amount, isExpense, category, date ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    isExpense = isExpense,
                    category = category,
                    date = date,
                    description = "Adição rápida inteligente."
                )
                showQuickAddDialog = false
                Toast.makeText(context, "Transação adicionada!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showMonthlySummaryDialog) {
        MonthlySummaryDialog(
            response = monthlySummary,
            isLoading = isMonthlySummaryLoading,
            onDismiss = { showMonthlySummaryDialog = false },
            onRefresh = { viewModel.getMonthlyTextualSummary() }
        )
    }

    if (showCalculatorDialog) {
        AdvancedFinancialCalculatorDialog(
            onDismiss = { showCalculatorDialog = false }
        )
    }
}

@Composable
fun VolarisSplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.7f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = LinearOutSlowInEasing
        ),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0C15))
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A162B),
                                Color(0xFF0D0C15)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f), CircleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Volaris Finanças Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "VOLARIS FINANÇAS",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seu Controle Financeiro Inteligente",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD0BCFF).copy(alpha = 0.8f),
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color(0xFFD0BCFF),
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(alpha)
            )
        }
    }
}
