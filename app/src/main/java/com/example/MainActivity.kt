package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
                                onExportPdf = { start, end, cats -> viewModel.shareTextReport(context, start, end, cats) },
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

// ==========================================
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
                    Text(
                        text = "Distribuição de Despesas por Categoria",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

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

// ==========================================
// 2. TRANSACTIONS HISTORIC SCREEN
// ==========================================
@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    onDeleteTransaction: (Transaction) -> Unit,
    hideBalances: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }
    var selectedTagFilter by remember { mutableStateOf("Todas") }
    var showAdvancedFilters by remember { mutableStateOf(false) }

    var minAmountStr by remember { mutableStateOf("") }
    var maxAmountStr by remember { mutableStateOf("") }
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }

    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Derive unique tags from transactions
    val allTags = remember(transactions) {
        transactions.flatMap { tx ->
            tx.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }

    val filteredList = remember(
        transactions, searchQuery, selectedCategoryFilter, selectedTagFilter,
        minAmountStr, maxAmountStr, startDateStr, endDateStr
    ) {
        val minAmt = parseCurrencyInput(minAmountStr)
        val maxAmt = parseCurrencyInput(maxAmountStr)

        val startTimestamp = try {
            if (startDateStr.length == 10) sdfDate.parse(startDateStr)?.time ?: 0L else 0L
        } catch (e: Exception) {
            0L
        }

        val endTimestamp = try {
            if (endDateStr.length == 10) {
                val parsed = sdfDate.parse(endDateStr)
                if (parsed != null) parsed.time + 86399999L else Long.MAX_VALUE
            } else {
                Long.MAX_VALUE
            }
        } catch (e: Exception) {
            Long.MAX_VALUE
        }

        transactions.filter { transaction ->
            val matchQuery = transaction.title.contains(searchQuery, ignoreCase = true) ||
                    transaction.category.contains(searchQuery, ignoreCase = true) ||
                    transaction.tags.contains(searchQuery, ignoreCase = true) ||
                    transaction.description.contains(searchQuery, ignoreCase = true)

            val matchCat = selectedCategoryFilter == "Todas" || transaction.category == selectedCategoryFilter

            val transactionTagList = transaction.tags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val matchTag = selectedTagFilter == "Todas" || transactionTagList.contains(selectedTagFilter.lowercase())

            val matchMinAmt = minAmt == 0.0 || transaction.amount >= minAmt
            val matchMaxAmt = maxAmt == 0.0 || transaction.amount <= maxAmt

            val matchStartDate = startTimestamp == 0L || transaction.date >= startTimestamp
            val matchEndDate = endTimestamp == Long.MAX_VALUE || transaction.date <= endTimestamp

            matchQuery && matchCat && matchTag && matchMinAmt && matchMaxAmt && matchStartDate && matchEndDate
        }
    }

    val categories = remember {
        listOf("Todas") + FinanceCategories.incomes + FinanceCategories.expenses
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Histórico de Transações",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por título, categoria, tag...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                    Icon(
                        imageVector = if (showAdvancedFilters) Icons.Rounded.Tune else Icons.Rounded.FilterList,
                        contentDescription = "Filtros Avançados",
                        tint = if (showAdvancedFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_search_input")
                .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Advanced Filters panel
        AnimatedVisibility(
            visible = showAdvancedFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Filtros Avançados",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Date range filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startDateStr,
                            onValueChange = { startDateStr = formatInputDate(it) },
                            label = { Text("De (DD/MM/AAAA)", fontSize = 11.sp) },
                            placeholder = { Text("01/01/2026", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = endDateStr,
                            onValueChange = { endDateStr = formatInputDate(it) },
                            label = { Text("Até (DD/MM/AAAA)", fontSize = 11.sp) },
                            placeholder = { Text("31/12/2026", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Amount range filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minAmountStr,
                            onValueChange = { minAmountStr = formatCurrencyInput(it) },
                            label = { Text("Valor Mínimo", fontSize = 11.sp) },
                            placeholder = { Text("R$ 0,00", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = maxAmountStr,
                            onValueChange = { maxAmountStr = formatCurrencyInput(it) },
                            label = { Text("Valor Máximo", fontSize = 11.sp) },
                            placeholder = { Text("R$ 0,00", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }

                    // Tags filter section
                    if (allTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Agrupamento por Tags",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedTagFilter == "Todas",
                                    onClick = { selectedTagFilter = "Todas" },
                                    label = { Text("Todas as Tags", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                            items(allTags) { tag ->
                                FilterChip(
                                    selected = selectedTagFilter == tag,
                                    onClick = { selectedTagFilter = tag },
                                    label = { Text("#$tag", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Reset Filters Button
                    val isAnyFilterActive = searchQuery.isNotEmpty() ||
                            selectedCategoryFilter != "Todas" ||
                            selectedTagFilter != "Todas" ||
                            minAmountStr.isNotEmpty() ||
                            maxAmountStr.isNotEmpty() ||
                            startDateStr.isNotEmpty() ||
                            endDateStr.isNotEmpty()

                    if (isAnyFilterActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedCategoryFilter = "Todas"
                                selectedTagFilter = "Todas"
                                minAmountStr = ""
                                maxAmountStr = ""
                                startDateStr = ""
                                endDateStr = ""
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Rounded.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpar Filtros", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Horizontal Category Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.distinct()) { cat ->
                val isSelected = cat == selectedCategoryFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_$cat")
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma transação encontrada.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { transaction ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    TransactionRowItem(
                        transaction = transaction,
                        onDelete = { showDeleteConfirm = true },
                        hideBalances = hideBalances
                    )

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Excluir Transação") },
                            text = { Text("Tem certeza que deseja excluir '${transaction.title}'?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onDeleteTransaction(transaction)
                                        showDeleteConfirm = false
                                    }
                                ) {
                                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    hideBalances: Boolean = false
) {
    val tagList = remember(transaction.tags) {
        transaction.tags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (transaction.isExpense) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = if (transaction.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.category} • ${formatDate(transaction.date)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (tagList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        tagList.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = if (hideBalances) "${if (transaction.isExpense) "-" else "+"} R$ •••" else "${if (transaction.isExpense) "-" else "+"} ${formatCurrency(transaction.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (transaction.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ==========================================
// 3. SAVINGS GOALS SCREEN
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(
    goals: List<SavingsGoal>,
    onAddGoalClick: () -> Unit,
    onContributeGoal: (SavingsGoal, Double) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Metas de Economia",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onAddGoalClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("create_goal_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nova Meta", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma meta definida. Comece a planejar suas economias mensais!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    var showContributeDialog by remember { mutableStateOf(false) }
                    var contributionAmount by remember { mutableStateOf("") }
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showDeleteConfirm = true }
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        goal.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Referência: ${goal.monthYear}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val pctVal = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100 else 0.0
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", pctVal)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Economizado: ${formatCurrency(goal.currentAmount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Alvo: ${formatCurrency(goal.targetAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { showContributeDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("contribute_goal_${goal.id}")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Poupar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (showContributeDialog) {
                        AlertDialog(
                            onDismissRequest = { showContributeDialog = false },
                            title = { Text("Poupar para: ${goal.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("Quanto deseja destinar para esta meta agora?", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                                    OutlinedTextField(
                                        value = contributionAmount,
                                        onValueChange = { contributionAmount = formatCurrencyInput(it) },
                                        label = { Text("Valor") },
                                        placeholder = { Text("R$ 0,00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("goal_contribution_input")
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val amt = parseCurrencyInput(contributionAmount)
                                        if (amt > 0) {
                                            onContributeGoal(goal, amt)
                                            showContributeDialog = false
                                            contributionAmount = ""
                                        }
                                    }
                                ) {
                                    Text("Confirmar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showContributeDialog = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Excluir Meta") },
                            text = { Text("Deseja realmente excluir a meta '${goal.name}'?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onDeleteGoal(goal)
                                        showDeleteConfirm = false
                                    }
                                ) {
                                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- INTERACTIVE SAVINGS CALCULATOR ---
        Spacer(modifier = Modifier.height(16.dp))

        var isCalcExpanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("savings_calculator_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCalcExpanded = !isCalcExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Simulador de Juros Compostos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Planeje seus investimentos e veja o poder do tempo.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isCalcExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Expandir/Recolher",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isCalcExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))

                    var initialCapitalStr by remember { mutableStateOf("") }
                    var monthlySavingsStr by remember { mutableStateOf("") }
                    var annualRateStr by remember { mutableStateOf("10") }
                    var yearsStr by remember { mutableStateOf("5") }

                    val initialCapital = parseCurrencyInput(initialCapitalStr)
                    val monthlySavings = parseCurrencyInput(monthlySavingsStr)
                    val annualRate = (annualRateStr.toDoubleOrNull() ?: 0.0) / 100.0
                    val years = yearsStr.toIntOrNull() ?: 0

                    val months = years * 12
                    val monthlyRate = annualRate / 12.0

                    var totalAccumulated = initialCapital
                    var totalInvested = initialCapital

                    if (months > 0) {
                        if (monthlyRate > 0) {
                            val compoundFactor = Math.pow(1.0 + monthlyRate, months.toDouble())
                            val initialPart = initialCapital * compoundFactor
                            val savingsPart = monthlySavings * ((compoundFactor - 1.0) / monthlyRate) * (1.0 + monthlyRate)
                            totalAccumulated = initialPart + savingsPart
                        } else {
                            totalAccumulated = initialCapital + (monthlySavings * months)
                        }
                        totalInvested = initialCapital + (monthlySavings * months)
                    }
                    val totalInterest = (totalAccumulated - totalInvested).coerceAtLeast(0.0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = initialCapitalStr,
                            onValueChange = { initialCapitalStr = formatCurrencyInput(it) },
                            label = { Text("Valor Inicial", fontSize = 11.sp) },
                            placeholder = { Text("R$ 0,00", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = monthlySavingsStr,
                            onValueChange = { monthlySavingsStr = formatCurrencyInput(it) },
                            label = { Text("Aporte Mensal", fontSize = 11.sp) },
                            placeholder = { Text("R$ 0,00", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = annualRateStr,
                            onValueChange = { annualRateStr = it },
                            label = { Text("Taxa Anual (%)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = yearsStr,
                            onValueChange = { yearsStr = it },
                            label = { Text("Período (Anos)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Valor Total Acumulado:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(formatCurrency(totalAccumulated), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Investido (Aportes):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalInvested), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Juros Acumulados Rendidos:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalInterest), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. BILLS AND BUDGETS SCREEN
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillsAndBudgetsScreen(
    bills: List<UpcomingBill>,
    budgets: List<CategoryBudget>,
    transactions: List<Transaction>,
    onAddBillClick: () -> Unit,
    onToggleBillPaid: (UpcomingBill) -> Unit,
    onDeleteBill: (UpcomingBill) -> Unit,
    onAddBudgetClick: () -> Unit,
    onDeleteBudget: (CategoryBudget) -> Unit
) {
    val scrollState = rememberScrollState()
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val currentMonthExpenses = remember(transactions, currentMonth, currentYear) {
        transactions.filter { t ->
            if (t.isExpense && t.isPaid) {
                val tc = Calendar.getInstance().apply { timeInMillis = t.date }
                tc.get(Calendar.MONTH) == currentMonth && tc.get(Calendar.YEAR) == currentYear
            } else {
                false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- MONTHLY CALENDAR VIEW ---
        var selectedDay by remember { mutableStateOf<Int?>(null) }
        val calendarInstance = remember { Calendar.getInstance() }
        val daysInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonthName = calendarInstance.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR")) ?: ""
        val currentYearName = calendarInstance.get(Calendar.YEAR)
        
        // Find bills and transactions on each day of this month
        val itemsByDay = remember(bills, transactions) {
            val map = mutableMapOf<Int, MutableList<Any>>()
            
            // Map bills (by due day)
            bills.forEach { b ->
                val tc = Calendar.getInstance().apply { timeInMillis = b.dueDate }
                val day = tc.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(day) { mutableListOf() }.add(b)
            }
            
            // Map transactions
            transactions.forEach { t ->
                val tc = Calendar.getInstance().apply { timeInMillis = t.date }
                if (tc.get(Calendar.MONTH) == calendarInstance.get(Calendar.MONTH) && 
                    tc.get(Calendar.YEAR) == calendarInstance.get(Calendar.YEAR)) {
                    val day = tc.get(Calendar.DAY_OF_MONTH)
                    map.getOrPut(day) { mutableListOf() }.add(t)
                }
            }
            map
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("monthly_calendar_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Calendar Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calendário Financeiro",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${currentMonthName.replaceFirstChar { it.uppercase() }} de $currentYearName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekdays header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid calculation
                val firstDayCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val firstDayOfWeekIndex = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sunday) to 6 (Saturday)
                
                val totalSlots = daysInMonth + firstDayOfWeekIndex
                val rowsCount = (totalSlots + 6) / 7

                for (row in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0..6) {
                            val slotIndex = row * 7 + col
                            val dayNumber = slotIndex - firstDayOfWeekIndex + 1
                            
                            if (dayNumber in 1..daysInMonth) {
                                val hasActivity = itemsByDay.containsKey(dayNumber)
                                val dayItems = itemsByDay[dayNumber] ?: emptyList()
                                
                                val hasIncome = dayItems.any { it is Transaction && !it.isExpense }
                                val hasExpense = dayItems.any { (it is Transaction && it.isExpense) || it is UpcomingBill }
                                
                                val isToday = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == dayNumber &&
                                        Calendar.getInstance().get(Calendar.MONTH) == calendarInstance.get(Calendar.MONTH) &&
                                        Calendar.getInstance().get(Calendar.YEAR) == calendarInstance.get(Calendar.YEAR)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable { selectedDay = dayNumber }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (hasActivity) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasIncome) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF00C853))
                                                    )
                                                }
                                                if (hasExpense) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.error)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Details Dialog
        selectedDay?.let { day ->
            val dayItems = itemsByDay[day] ?: emptyList()
            Dialog(onDismissRequest = { selectedDay = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Movimentações: $day de ${currentMonthName.replaceFirstChar { it.uppercase() }}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { selectedDay = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Fechar")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (dayItems.isEmpty()) {
                            Text(
                                text = "Nenhuma movimentação ou conta a pagar registrada para este dia.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(dayItems.size) { index ->
                                    val item = dayItems[index]
                                    when (item) {
                                        is UpcomingBill -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Text(
                                                        text = "Conta a Pagar | Categoria: ${item.category}",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = formatCurrency(item.amount),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        is Transaction -> {
                                            val isExp = item.isExpense
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isExp) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                        else Color(0xFF00C853).copy(alpha = 0.08f)
                                                    )
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = item.description,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${if (isExp) "Despesa" else "Receita"} | Categoria: ${item.category}",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "${if (isExp) "-" else "+"}${formatCurrency(item.amount)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isExp) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // --- UPCOMING BILLS SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alertas de Vencimentos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onAddBillClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_bill_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Vencimento", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (bills.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Nenhuma fatura ou boleto pendente cadastrado. Adicione seus vencimentos recorrentes para monitorar datas de pagamento com alertas.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                bills.forEach { bill ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showDeleteConfirm = true }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (bill.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (bill.isPaid) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = if (bill.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    bill.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Vencimento: ${formatDate(bill.dueDate)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val daysDiff = getDaysRemaining(bill.dueDate)
                                    if (!bill.isPaid) {
                                        val chipColor = when {
                                            daysDiff < 0 -> MaterialTheme.colorScheme.error
                                            daysDiff <= 2 -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Text(
                                            text = if (daysDiff < 0) "Vencido" else if (daysDiff == 0L) "Hoje" else "Em $daysDiff dias",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = chipColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatCurrency(bill.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { onToggleBillPaid(bill) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (bill.isPaid) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(24.dp).testTag("pay_bill_${bill.id}")
                                ) {
                                    Text(if (bill.isPaid) "Estornar" else "Pagar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Excluir Conta") },
                            text = { Text("Excluir o registro de '${bill.title}'?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onDeleteBill(bill)
                                        showDeleteConfirm = false
                                    }
                                ) {
                                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- CATEGORY BUDGETS SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Limites por Categoria",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Acompanhe seus orçamentos de gastos",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddBudgetClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("add_budget_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Orçamento", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (budgets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Nenhum limite de gastos definido para as categorias. Defina um orçamento mensal para acompanhar e controlar suas despesas em tempo real.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                budgets.forEach { budget ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    val spent = remember(currentMonthExpenses) {
                        currentMonthExpenses.filter { it.category == budget.category }.sumOf { it.amount }
                    }
                    val pct = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
                    val remaining = budget.limitAmount - spent

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showDeleteConfirm = true }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(budget.category),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = budget.category,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Limite: ${formatCurrency(budget.limitAmount)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.size(32.dp).testTag("delete_budget_${budget.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Excluir orçamento",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val progressFloat = pct.toFloat().coerceIn(0f, 1f)
                            val progressColor = when {
                                pct > 1.0 -> MaterialTheme.colorScheme.error
                                pct >= 0.8 -> Color(0xFFFF9E00)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            LinearProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = progressColor,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consumido: ${formatCurrency(spent)} (${String.format(Locale.getDefault(), "%.1f", pct * 100)}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (pct > 1.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = if (remaining >= 0) "Restam ${formatCurrency(remaining)}" else "Excedeu em ${formatCurrency(-remaining)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Excluir Orçamento") },
                            text = { Text("Deseja realmente remover o orçamento da categoria '${budget.category}'?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onDeleteBudget(budget)
                                        showDeleteConfirm = false
                                    }
                                ) {
                                    Text("Remover", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. REPORTS EXPORT & CREDITS SCREEN
// ==========================================
@Composable
fun ExportScreen(
    onExportCsv: (startDate: Long?, endDate: Long?, selectedCategories: List<String>) -> Unit,
    onExportPdf: (startDate: Long?, endDate: Long?, selectedCategories: List<String>) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var filterByDate by remember { mutableStateOf(false) }
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }
    val selectedCategories = remember { mutableStateListOf<String>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    val jsonContent = it.readText()
                    onImportBackup(jsonContent)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao ler backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Exportação de Relatórios",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Card de Filtros Avançados
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                            imageVector = Icons.Rounded.FilterAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filtros de Exportação",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ativar período", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = filterByDate,
                            onCheckedChange = { filterByDate = it },
                            modifier = Modifier.scale(0.8f).testTag("export_filter_date_switch")
                        )
                    }
                }

                if (filterByDate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startDateStr,
                            onValueChange = { startDateStr = formatInputDate(it) },
                            label = { Text("Início (DD/MM/AAAA)", fontSize = 11.sp) },
                            placeholder = { Text("Ex: 01/01/2026", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("export_start_date_input")
                        )

                        OutlinedTextField(
                            value = endDateStr,
                            onValueChange = { endDateStr = formatInputDate(it) },
                            label = { Text("Fim (DD/MM/AAAA)", fontSize = 11.sp) },
                            placeholder = { Text("Ex: 31/01/2026", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("export_end_date_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Quick period choices
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.DAY_OF_MONTH, 1)
                                startDateStr = sdf.format(cal.time)
                                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                endDateStr = sdf.format(cal.time)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Este Mês", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                endDateStr = sdf.format(cal.time)
                                cal.add(Calendar.DAY_OF_YEAR, -30)
                                startDateStr = sdf.format(cal.time)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Últimos 30 Dias", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.DAY_OF_YEAR, 1)
                                startDateStr = sdf.format(cal.time)
                                cal.set(Calendar.MONTH, 11)
                                cal.set(Calendar.DAY_OF_MONTH, 31)
                                endDateStr = sdf.format(cal.time)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Ano Atual", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                startDateStr = ""
                                endDateStr = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Limpar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                // Categories Selection Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filtrar por Categorias (${selectedCategories.size} sel.)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (selectedCategories.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedCategories.clear() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Limpar", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable row of category filter chips
                val allCategories = remember { FinanceCategories.incomes + FinanceCategories.expenses }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allCategories) { cat ->
                        val isSelected = selectedCategories.contains(cat)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedCategories.remove(cat)
                                else selectedCategories.add(cat)
                            },
                            label = { Text(cat, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // CSV Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Planilhas do Excel (CSV)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Gera um arquivo estruturado de transações pronto para abrir no Google Sheets ou Excel.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val parsedStart = if (filterByDate && startDateStr.isNotEmpty()) parseDateTime(startDateStr, "00:00") else null
                        val parsedEnd = if (filterByDate && endDateStr.isNotEmpty()) parseDateTime(endDateStr, "23:59") else null
                        onExportCsv(parsedStart, parsedEnd, selectedCategories.toList())
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Text Report (PDF-formatted plain summary) Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Relatório Financeiro PDF / Texto", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Exporta um resumo do saldo, metas alcançadas, boletos e transações completas.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val parsedStart = if (filterByDate && startDateStr.isNotEmpty()) parseDateTime(startDateStr, "00:00") else null
                        val parsedEnd = if (filterByDate && endDateStr.isNotEmpty()) parseDateTime(endDateStr, "23:59") else null
                        onExportPdf(parsedStart, parsedEnd, selectedCategories.toList())
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // --- BACKUP & RESTORE SECTION ---
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cópia de Segurança (Backup)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gerencie seus dados de forma segura. Exporte tudo para um arquivo JSON local, ou restaure de um arquivo de backup anteriormente salvo no seu dispositivo.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onExportBackup,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("export_backup_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { filePickerLauncher.launch("application/json") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("import_backup_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restaurar JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    onVisitGithub: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- CREATOR SECTION ---
        Text(
            text = "Sobre o Aplicativo",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Volaris Finanças Premium",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Versão 1.1.0 (Totalmente Offline e Privado)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Este aplicativo foi projetado para assegurar sigilo bancário completo e autonomia absoluta ao usuário. Os dados gravados ficam salvos unicamente em seu próprio aparelho e jamais cruzam a rede para servidores remotos.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "CRÉDITOS DE CRIAÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "fox__red",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onVisitGithub,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("github_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Acessar GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "github.com/foxredoficial",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// 3. PLANNING SCREEN
// ==========================================
@Composable
fun PlanningScreen(
    goals: List<SavingsGoal>,
    onAddGoalClick: () -> Unit,
    onContributeGoal: (SavingsGoal, Double) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    bills: List<UpcomingBill>,
    budgets: List<CategoryBudget>,
    transactions: List<Transaction>,
    onAddBillClick: () -> Unit,
    onToggleBillPaid: (UpcomingBill) -> Unit,
    onDeleteBill: (UpcomingBill) -> Unit,
    onAddBudgetClick: () -> Unit,
    onDeleteBudget: (CategoryBudget) -> Unit,
    onAddTransaction: (String, Double, Boolean, String, Long, String, String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Metas de Poupança", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Rounded.Savings, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Orçamentos e Contas", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("Contas de Casa", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 3,
                onClick = { selectedSubTab = 3 },
                text = { Text("Empréstimos", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Rounded.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 4,
                onClick = { selectedSubTab = 4 },
                text = { Text("Inteligência Financeira", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                icon = { Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
        
        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> GoalsScreen(
                    goals = goals,
                    onAddGoalClick = onAddGoalClick,
                    onContributeGoal = onContributeGoal,
                    onDeleteGoal = onDeleteGoal
                )
                1 -> BillsAndBudgetsScreen(
                    bills = bills,
                    budgets = budgets,
                    transactions = transactions,
                    onAddBillClick = onAddBillClick,
                    onToggleBillPaid = onToggleBillPaid,
                    onDeleteBill = onDeleteBill,
                    onAddBudgetClick = onAddBudgetClick,
                    onDeleteBudget = onDeleteBudget
                )
                2 -> HouseholdOrganizerScreen(
                    bills = bills,
                    transactions = transactions,
                    onAddTransaction = onAddTransaction
                )
                3 -> LoansScreen(
                    onAddTransaction = onAddTransaction
                )
                4 -> FinanceIntelligenceScreen(
                    transactions = transactions,
                    bills = bills,
                    goals = goals
                )
            }
        }
    }
}

// ==========================================
// 3.5 HOUSEHOLD ORGANIZER SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdOrganizerScreen(
    bills: List<UpcomingBill>,
    transactions: List<Transaction>,
    onAddTransaction: (String, Double, Boolean, String, Long, String, String) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("volaris_domestic", Context.MODE_PRIVATE) }
    
    var activeFeature by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Feature Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val features = listOf(
                Triple(0, Icons.Rounded.People, "Moradores"),
                Triple(1, Icons.Rounded.BarChart, "Consumo"),
                Triple(2, Icons.Rounded.Lightbulb, "Simulador"),
                Triple(3, Icons.Rounded.ShoppingCart, "Mercado")
            )
            
            features.forEach { (index, icon, label) ->
                val isSelected = activeFeature == index
                FilterChip(
                    selected = isSelected,
                    onClick = { activeFeature = index },
                    label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        when (activeFeature) {
            0 -> ResidentsTab(bills, sharedPrefs)
            1 -> UtilityConsumptionTab(bills, transactions)
            2 -> EcoSimulatorTab()
            3 -> GroceryListTab(sharedPrefs, onAddTransaction)
        }
    }
}

@Composable
fun ResidentsTab(
    bills: List<UpcomingBill>,
    sharedPrefs: android.content.SharedPreferences
) {
    var residentsString by remember { mutableStateOf(sharedPrefs.getString("residents_list", "Você:50") ?: "Você:50") }
    var newResidentName by remember { mutableStateOf("") }
    var newResidentPercent by remember { mutableStateOf("") }
    
    val residents = remember(residentsString) {
        if (residentsString.isEmpty()) emptyList()
        else {
            residentsString.split(",").mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) {
                    Pair(parts[0], parts[1].toDoubleOrNull() ?: 0.0)
                } else null
            }
        }
    }
    
    val saveResidents = { newList: List<Pair<String, Double>> ->
        val str = newList.joinToString(",") { "${it.first}:${it.second}" }
        residentsString = str
        sharedPrefs.edit().putString("residents_list", str).apply()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Add resident card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("👥 Adicionar Morador", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newResidentName,
                        onValueChange = { newResidentName = it },
                        label = { Text("Nome", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = newResidentPercent,
                        onValueChange = { newResidentPercent = it },
                        label = { Text("Quota (%)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    Button(
                        onClick = {
                            if (newResidentName.isNotBlank()) {
                                val pct = newResidentPercent.toDoubleOrNull() ?: 0.0
                                val updated = residents + Pair(newResidentName.trim(), pct)
                                saveResidents(updated)
                                newResidentName = ""
                                newResidentPercent = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (residents.isNotEmpty()) {
                            val equalShare = 100.0 / residents.size
                            val updated = residents.map { Pair(it.first, equalShare) }
                            saveResidents(updated)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dividir Quotas por Igual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // List of Residents
        Text("Moradores Cadastrados", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        if (residents.isEmpty()) {
            Text("Nenhum morador cadastrado.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            residents.forEachIndexed { idx, res ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(res.first, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Quota: ${String.format("%.1f", res.second)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            val updated = residents.toMutableList().apply { removeAt(idx) }
                            saveResidents(updated)
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        
        // Split Upcoming Bills Card
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🧾 Divisão de Contas Mensais", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Valor proporcional que cada morador deve contribuir para as contas pendentes do mês.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                val unpaidBills = bills.filter { !it.isPaid }
                val totalUnpaid = unpaidBills.sumOf { it.amount }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Pendente:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(formatCurrency(totalUnpaid), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                
                if (residents.isEmpty()) {
                    Text("Cadastre moradores acima para calcular a divisão.", fontSize = 12.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    residents.forEach { res ->
                        val shareAmount = (totalUnpaid * res.second) / 100.0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(res.first, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(shareAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                if (unpaidBills.isNotEmpty() && residents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Detalhamento por Conta:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    unpaidBills.forEach { bill ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(bill.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(formatCurrency(bill.amount), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                residents.forEach { res ->
                                    val part = (bill.amount * res.second) / 100.0
                                    Text("• ${res.first}: ${formatCurrency(part)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun UtilityConsumptionTab(
    bills: List<UpcomingBill>,
    transactions: List<Transaction>
) {
    val utilityKeywords = listOf("luz", "água", "energia", "gás", "internet", "telefone", "saneamento", "copel", "enel", "sabesp", "comgás", "aluguel", "condomínio")
    val calendar = Calendar.getInstance()
    
    val monthlyUtilities = remember(bills, transactions) {
        val map = mutableMapOf<String, Double>()
        
        transactions.filter { t ->
            t.isExpense && (t.category.lowercase() == "moradia" || utilityKeywords.any { kw -> t.title.lowercase().contains(kw) })
        }.forEach { t ->
            calendar.timeInMillis = t.date
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val key = String.format("%02d/%d", month, year)
            map[key] = (map[key] ?: 0.0) + t.amount
        }
        
        bills.filter { b ->
            b.category.lowercase() == "moradia" || utilityKeywords.any { kw -> b.title.lowercase().contains(kw) }
        }.forEach { b ->
            calendar.timeInMillis = b.dueDate
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val key = String.format("%02d/%d", month, year)
            map[key] = (map[key] ?: 0.0) + b.amount
        }
        
        map.toList().sortedBy { pair ->
            val parts = pair.first.split("/")
            if (parts.size == 2) {
                val m = parts[0].toIntOrNull() ?: 0
                val y = parts[1].toIntOrNull() ?: 0
                y * 12 + m
            } else 0
        }.takeLast(6)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 Histórico de Contas de Consumo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Histórico mensal de gastos com água, energia, internet, gás e aluguel.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (monthlyUtilities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum dado de consumo encontrado nesta conta.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val maxVal = maxOf(monthlyUtilities.maxOf { it.second }, 1.0)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(formatCurrency(maxVal), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(maxVal * 0.5), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("R$ 0,00", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                repeat(3) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                monthlyUtilities.forEach { (month, total) ->
                                    val pct = (total / maxVal).toFloat()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = formatCurrency(total),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .fillMaxHeight(pct.coerceIn(0.05f, 1f))
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                        )
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = month,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Text("Contas Identificadas como Consumo", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        val utilitiesList = remember(bills, transactions) {
            val fromBills = bills.filter { b ->
                b.category.lowercase() == "moradia" || utilityKeywords.any { kw -> b.title.lowercase().contains(kw) }
            }.map { Triple(it.title, it.amount, it.isPaid) }
            
            val fromTrans = transactions.filter { t ->
                t.isExpense && (t.category.lowercase() == "moradia" || utilityKeywords.any { kw -> t.title.lowercase().contains(kw) })
            }.map { Triple(it.title, it.amount, true) }
            
            (fromBills + fromTrans).take(10)
        }
        
        if (utilitiesList.isEmpty()) {
            Text("Nenhuma conta ou despesa de consumo identificada ainda.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            utilitiesList.forEach { (title, amount, isPaid) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (title.lowercase().contains("luz") || title.lowercase().contains("energia")) Icons.Rounded.ElectricBolt
                                else if (title.lowercase().contains("água") || title.lowercase().contains("saneamento")) Icons.Rounded.Lightbulb
                                else Icons.Rounded.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = formatCurrency(amount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun EcoSimulatorTab() {
    var showerMinutes by remember { mutableStateOf(15f) }
    var lightsCount by remember { mutableStateOf(5f) }
    var ecoWashingMode by remember { mutableStateOf(false) }
    var acHours by remember { mutableStateOf(4f) }
    
    val showerSavings = (15f - showerMinutes) * 0.064 * 30.0
    val lightsSavings = lightsCount * 5.0 * 30.0 * 0.010 * 0.85
    val washingSavings = if (ecoWashingMode) (30.0 * 0.008 * 8) + (0.2 * 0.85 * 8) else 0.0
    val acSavings = (4f - acHours).coerceAtLeast(0f) * 1.2 * 30.0 * 0.85
    val totalSavings = showerSavings + lightsSavings + washingSavings + acSavings
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Simulador de Economia Doméstica", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Simule pequenos hábitos e veja a estimativa de economia mensal em suas contas de água e luz.", fontSize = 11.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Economia Estimada:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(totalSavings),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00C853)
                )
                Text("por mês economizados", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        Text("Ajuste seus Hábitos:", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
        
        // Shower
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🚿 Tempo de Banho Diário", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${showerMinutes.toInt()} minutos", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = showerMinutes,
                    onValueChange = { showerMinutes = it },
                    valueRange = 5f..15f,
                    steps = 9
                )
                Text("Reduzir de 15 min para ${showerMinutes.toInt()} min economiza energia elétrica e água quente.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        // Lights
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("💡 Lâmpadas Desligadas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${lightsCount.toInt()} lâmpadas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = lightsCount,
                    onValueChange = { lightsCount = it },
                    valueRange = 0f..10f,
                    steps = 10
                )
                Text("Evitar deixar acesas lâmpadas em cômodos vazios por 5 horas diárias.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        // A/C hours
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ar Condicionado", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${acHours.toInt()} horas/dia", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = acHours,
                    onValueChange = { acHours = it },
                    valueRange = 0f..4f,
                    steps = 4
                )
                Text("Reduzir o tempo de funcionamento do ar condicionado em dias quentes.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        // Eco washing
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🧺 Lavar Roupa em Modo Ecológico", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Economiza até 30% de água por ciclo completo na máquina de lavar.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = ecoWashingMode,
                    onCheckedChange = { ecoWashingMode = it }
                )
            }
        }
    }
}

@Composable
fun GroceryListTab(
    sharedPrefs: android.content.SharedPreferences,
    onAddTransaction: (String, Double, Boolean, String, Long, String, String) -> Unit
) {
    val context = LocalContext.current
    var groceryString by remember { mutableStateOf(sharedPrefs.getString("grocery_list", "") ?: "") }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf("Alimentos") }
    var groceryLimit by remember { mutableStateOf(sharedPrefs.getFloat("grocery_limit", 300f)) }
    var isEditingLimit by remember { mutableStateOf(false) }
    var tempLimit by remember { mutableStateOf(groceryLimit.toString()) }
    
    val categoriesList = listOf("Alimentos", "Hortifrúti", "Carnes/Frios", "Bebidas", "Limpeza", "Higiene", "Outros")
    
    val items = remember(groceryString) {
        if (groceryString.isEmpty()) emptyList()
        else {
            groceryString.split(",").mapNotNull {
                val parts = it.split(":")
                when (parts.size) {
                    3 -> {
                        GroceryItem(parts[0], parts[1].toDoubleOrNull() ?: 0.0, parts[2].toBoolean(), "Alimentos", 1)
                    }
                    5 -> {
                        GroceryItem(parts[0], parts[1].toDoubleOrNull() ?: 0.0, parts[2].toBoolean(), parts[3], parts[4].toIntOrNull() ?: 1)
                    }
                    else -> null
                }
            }
        }
    }
    
    val saveItems = { newList: List<GroceryItem> ->
        val str = newList.joinToString(",") { "${it.name}:${it.unitPrice}:${it.isBought}:${it.category}:${it.quantity}" }
        groceryString = str
        sharedPrefs.edit().putString("grocery_list", str).apply()
    }
    
    val totalInCart = items.filter { it.isBought }.sumOf { it.unitPrice * it.quantity }
    val totalEstimated = items.sumOf { it.unitPrice * it.quantity }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🛒 Orçamento de Mercado", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Monitore seus gastos com mercado antes de passar no caixa.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    if (isEditingLimit) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = tempLimit,
                                onValueChange = { tempLimit = it },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )
                            IconButton(onClick = {
                                val l = tempLimit.toFloatOrNull() ?: 300f
                                groceryLimit = l
                                sharedPrefs.edit().putFloat("grocery_limit", l).apply()
                                isEditingLimit = false
                            }) {
                                Icon(Icons.Rounded.Check, contentDescription = "Salvar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatCurrency(groceryLimit.toDouble()), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = {
                                tempLimit = groceryLimit.toString()
                                isEditingLimit = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val progress = if (groceryLimit > 0) (totalEstimated / groceryLimit).toFloat() else 0f
                val color = if (progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                
                LinearProgressIndicator(
                    progress = { progress.coerceAtMost(1f) },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("No carrinho: ${formatCurrency(totalInCart)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Est. Total: ${formatCurrency(totalEstimated)} / ${formatCurrency(groceryLimit.toDouble())}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (totalEstimated > groceryLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("➕ Adicionar Item Planejado", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Nome do Item (ex: Leite, Arroz)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it },
                        label = { Text("Preço Unit. (Est.)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it },
                        label = { Text("Quantidade", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }
                
                Text("Categoria:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoriesList.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            val p = newItemPrice.toDoubleOrNull() ?: 0.0
                            val q = newItemQty.toIntOrNull() ?: 1
                            val updated = items + GroceryItem(newItemName.trim(), p, false, selectedCategory, q)
                            saveItems(updated)
                            newItemName = ""
                            newItemPrice = ""
                            newItemQty = "1"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar à Lista", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Text("Itens na Lista (Agrupados)", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
        
        if (items.isEmpty()) {
            Text("Sua lista de mercado está vazia.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        } else {
            val grouped = items.groupBy { it.category }
            grouped.forEach { (cat, catItems) ->
                Text(
                    text = "📁 $cat (${formatCurrency(catItems.sumOf { it.unitPrice * it.quantity })})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                
                catItems.forEach { item ->
                    val globalIdx = items.indexOf(item)
                    if (globalIdx != -1) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = item.isBought,
                                        onCheckedChange = { checked ->
                                            val updated = items.toMutableList().apply {
                                                this[globalIdx] = item.copy(isBought = checked)
                                            }
                                            saveItems(updated)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            style = if (item.isBought) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default,
                                            color = if (item.isBought) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.quantity}x de ${formatCurrency(item.unitPrice)} | Total: ${formatCurrency(item.unitPrice * item.quantity)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                IconButton(onClick = {
                                    val updated = items.toMutableList().apply { removeAt(globalIdx) }
                                    saveItems(updated)
                                }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val boughtItems = items.filter { it.isBought }
                    val totalBought = boughtItems.sumOf { it.unitPrice * it.quantity }
                    if (totalBought > 0.0) {
                        onAddTransaction(
                            "Mercado - Compras de Casa",
                            totalBought,
                            true, // Is Expense
                            "Mercado", // Category
                            System.currentTimeMillis(),
                            "Compra de mercado finalizada pelo Planejador. Itens comprados: " + boughtItems.joinToString { "${it.name} (x${it.quantity})" },
                            "Mercado,Planejador"
                        )
                        // Keep unchecked items, discard bought items
                        val remaining = items.filter { !it.isBought }
                        saveItems(remaining)
                        Toast.makeText(context, "Compra de ${formatCurrency(totalBought)} registrada! Orçamento atualizado.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Marque os itens comprados antes de finalizar!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finalizar Compra (${formatCurrency(totalInCart)})", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

data class GroceryItem(
    val name: String,
    val unitPrice: Double,
    val isBought: Boolean,
    val category: String,
    val quantity: Int
)

@Composable
fun LoansScreen(
    onAddTransaction: (String, Double, Boolean, String, Long, String, String) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("volaris_domestic", Context.MODE_PRIVATE) }
    
    var loansString by remember { mutableStateOf(sharedPrefs.getString("loans_list_v2", "") ?: "") }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    
    val loans = remember(loansString) {
        if (loansString.isEmpty()) emptyList()
        else {
            loansString.split(";;;").mapNotNull {
                val parts = it.split(":::")
                if (parts.size >= 8) {
                    LoanItem(
                        title = parts[0],
                        amount = parts[1].toDoubleOrNull() ?: 0.0,
                        isDebt = parts[2].toBoolean(),
                        person = parts[3],
                        dueDate = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
                        interestRate = parts[5].toDoubleOrNull() ?: 0.0,
                        isPaid = parts[6].toBoolean(),
                        id = parts[7]
                    )
                } else null
            }
        }
    }
    
    val saveLoans = { newList: List<LoanItem> ->
        val str = newList.joinToString(";;;") { 
            "${it.title}:::${it.amount}:::${it.isDebt}:::${it.person}:::${it.dueDate}:::${it.interestRate}:::${it.isPaid}:::${it.id}"
        }
        loansString = str
        sharedPrefs.edit().putString("loans_list_v2", str).apply()
    }
    
    val activeLoans = loans.filter { !it.isPaid }
    val totalDebt = activeLoans.filter { it.isDebt }.sumOf { calculateAccruedAmount(it.amount, it.interestRate, it.dueDate) }
    val totalCredit = activeLoans.filter { !it.isDebt }.sumOf { calculateAccruedAmount(it.amount, it.interestRate, it.dueDate) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gerenciador de Empréstimos",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Acompanhe débitos e créditos com terceiros",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = { showAddLoanDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Novo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gráfico de Saldo Devedor / A Receber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LoansChart(totalCredit = totalCredit, totalDebt = totalDebt)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Crédito (A receber)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalCredit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Débito (A pagar)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalDebt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Empréstimos Ativos",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (loans.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhum empréstimo cadastrado ainda.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                loans.forEach { loan ->
                    val accruedAmount = if (loan.isPaid) loan.amount else calculateAccruedAmount(loan.amount, loan.interestRate, loan.dueDate)
                    val interestDiff = accruedAmount - loan.amount
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (loan.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp, 
                            if (loan.isPaid) MaterialTheme.colorScheme.outline.copy(alpha = 0.1f) 
                            else if (loan.isDebt) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) 
                            else Color(0xFF00C853).copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (loan.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else if (loan.isDebt) MaterialTheme.colorScheme.error else Color(0xFF00C853))
                                        )
                                        Text(
                                            text = loan.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (loan.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (loan.isDebt) "Devedor para: ${loan.person}" else "Credor para: ${loan.person}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Text(
                                    text = formatCurrency(accruedAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (loan.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else if (loan.isDebt) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Vence em: ${formatDate(loan.dueDate)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (loan.interestRate > 0.0) {
                                        Text(
                                            text = "Juros: ${loan.interestRate}%/mês | Acumulado: +${formatCurrency(interestDiff)}",
                                            fontSize = 10.sp,
                                            color = if (loan.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFE65100),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!loan.isPaid) {
                                        Button(
                                            onClick = {
                                                val updated = loans.map {
                                                    if (it.id == loan.id) it.copy(isPaid = true) else it
                                                }
                                                saveLoans(updated)
                                                
                                                onAddTransaction(
                                                    if (loan.isDebt) "Pagamento: ${loan.title}" else "Recebimento: ${loan.title}",
                                                    accruedAmount,
                                                    loan.isDebt,
                                                    "Empréstimo",
                                                    System.currentTimeMillis(),
                                                    "Liquidado empréstimo com ${loan.person}. Valor original: ${formatCurrency(loan.amount)} mais ${formatCurrency(interestDiff)} de juros acumulados.",
                                                    "Empréstimo,Liquidado"
                                                )
                                                Toast.makeText(context, "Empréstimo liquidado e transação gerada!", Toast.LENGTH_LONG).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Text("Liquidar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            val updated = loans.filter { it.id != loan.id }
                                            saveLoans(updated)
                                            Toast.makeText(context, "Registro excluído!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Excluir",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
    
    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { title, person, amount, rate, due ->
                val newLoan = LoanItem(
                    title = title,
                    person = person,
                    amount = amount,
                    interestRate = rate,
                    dueDate = due,
                    isDebt = true,
                    isPaid = false,
                    id = System.currentTimeMillis().toString() + "_" + (100..999).random()
                )
                val flexibleLoan = newLoan.copy(isDebt = selectedTypeIsDebt)
                val updated = loans + flexibleLoan
                saveLoans(updated)
                showAddLoanDialog = false
                Toast.makeText(context, "Empréstimo registrado!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Global helper for dialog state since dialog has selectedTypeIsDebt state
var selectedTypeIsDebt by mutableStateOf(true)

@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, person: String, amount: Double, interestRate: Double, dueDate: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var interestStr by remember { mutableStateOf("") }
    
    val initialDueDateStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }
    var dueDateStr by remember { mutableStateOf(initialDueDateStr) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Novo Empréstimo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { selectedTypeIsDebt = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTypeIsDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedTypeIsDebt) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Devo para alguém", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { selectedTypeIsDebt = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!selectedTypeIsDebt) Color(0xFF00C853) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!selectedTypeIsDebt) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Alguém me deve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descrição (ex: Reforma, Carro)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Nome da Pessoa") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = formatCurrencyInput(it) },
                    label = { Text("Valor Original") },
                    placeholder = { Text("R$ 0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = interestStr,
                    onValueChange = { interestStr = it },
                    label = { Text("Taxa de Juros Simples (% ao mês)") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = dueDateStr,
                    onValueChange = { dueDateStr = formatInputDate(it) },
                    label = { Text("Data de Vencimento (DD/MM/AAAA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val cleanAmount = parseCurrencyInput(amount)
                            val rate = interestStr.toDoubleOrNull() ?: 0.0
                            val parsedDue = try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dueDateStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            if (title.isNotBlank() && person.isNotBlank() && cleanAmount > 0.0) {
                                onConfirm(title.trim(), person.trim(), cleanAmount, rate, parsedDue)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LoansChart(totalCredit: Double, totalDebt: Double) {
    val maxVal = maxOf(totalCredit, totalDebt, 1.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            val ratio = (totalCredit / maxVal).toFloat()
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight(ratio.coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Color(0xFF00C853))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("A Receber", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(formatCurrency(totalCredit), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00C853))
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            val ratio = (totalDebt / maxVal).toFloat()
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight(ratio.coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.error)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("A Pagar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(formatCurrency(totalDebt), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
        }
    }
}

data class LoanItem(
    val title: String,
    val amount: Double,
    val isDebt: Boolean,
    val person: String,
    val dueDate: Long,
    val interestRate: Double,
    val isPaid: Boolean,
    val id: String
)

fun calculateAccruedAmount(originalAmount: Double, interestRate: Double, dueDate: Long): Double {
    if (interestRate <= 0.0) return originalAmount
    val elapsedMs = System.currentTimeMillis() - dueDate
    if (elapsedMs <= 0) return originalAmount
    val elapsedDays = elapsedMs / (1000 * 60 * 60 * 24)
    val months = elapsedDays / 30.4375
    val accruedInterest = originalAmount * (interestRate / 100.0) * months
    return originalAmount + accruedInterest
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Column {
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
                progress = progress,
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

// ==========================================
// 4. EVOLUTION SCREEN
// ==========================================

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
        val completedCount = weeklyChallenges.count { it.isCompleted && it.isClaimed }
        val totalCount = weeklyChallenges.size
        val claimableCount = weeklyChallenges.count { it.isCompleted && !it.isClaimed }
        
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
                                    val pct = if (challenge.targetValue > 0) (challenge.currentValue / challenge.targetValue).toFloat().coerceIn(0f, 1f) else 0f
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
    val achievements = listOf(
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

    val unlockedCount = achievements.count { unlockedList.contains(it.first) }
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
    val xpProgressPct = (userProfile.xp.toFloat() / xpRequired.toFloat()).coerceIn(0f, 1f)

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
                            Brush.sweepGradient(
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

                val milestones = listOf(
                    Pair(5, "Planejador Consciente 📈"),
                    Pair(10, "Guardião do Orçamento 🛡️"),
                    Pair(15, "Mago da Poupança 🧙‍♂️"),
                    Pair(20, "Investidor Lendário 👑"),
                    Pair(25, "Grão-Mestre das Finanças 💎")
                )

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
                    "Ter a conta bancária encerrada automaticamente pela instituição.",
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

// ==========================================
// DIALOGS & UTILITIES
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, isExpense: Boolean, category: String, date: Long, description: String, tags: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Alimentação") }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    val todayDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val todayTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    var dateStr by remember { mutableStateOf(todayDateStr) }
    var timeStr by remember { mutableStateOf(todayTimeStr) }

    val categories = if (isExpense) FinanceCategories.expenses else FinanceCategories.incomes

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Nova Transação",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Income / Expense selector chips (no emojis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isExpense = true
                            selectedCategory = "Alimentação"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("dialog_type_expense"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Despesa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isExpense = false
                            selectedCategory = "Salário"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isExpense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("dialog_type_income"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Receita", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título / Descrição Curta") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = formatCurrencyInput(it) },
                    label = { Text("Valor") },
                    placeholder = { Text("R$ 0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Smart formatted date and time row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = formatInputDate(it) },
                        label = { Text("Data (DD/MM/AAAA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f).testTag("transaction_date_input")
                    )

                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = formatInputTime(it) },
                        label = { Text("Hora (HH:MM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f).testTag("transaction_time_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Horizontal Category list select
                Text("Selecione a Categoria", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                  selectedContainerColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                  selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("dialog_chip_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags personalizadas (separadas por vírgula)") },
                    placeholder = { Text("ex: uber, trabalho, viagem") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_tags_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notas adicionais (opcional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(80.dp).testTag("transaction_desc_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val value = parseCurrencyInput(amount)
                            val timestamp = parseDateTime(dateStr, timeStr)
                            if (title.isNotBlank() && value > 0) {
                                onConfirm(title, value, isExpense, selectedCategory, timestamp, description, tags)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_add_confirm_button")
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmount: Double, currentAmount: Double, monthYear: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var currentAmount by remember { mutableStateOf("") }

    val formatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    val monthYearStr = formatter.format(Date())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Nova Meta de Economia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Meta (Ex: Viagem, Notebook)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = formatCurrencyInput(it) },
                    label = { Text("Alvo de Economia") },
                    placeholder = { Text("R$ 0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = currentAmount,
                    onValueChange = { currentAmount = formatCurrencyInput(it) },
                    label = { Text("Valor Iniciado Poupar (Opcional)") },
                    placeholder = { Text("R$ 0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("goal_start_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val target = parseCurrencyInput(targetAmount)
                            val current = parseCurrencyInput(currentAmount)
                            if (name.isNotBlank() && target > 0) {
                                onConfirm(name, target, current, monthYearStr)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_add_goal_confirm")
                    ) {
                        Text("Criar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, dueDate: Long, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isReceitaType by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Moradia") }

    val initialDueDateStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 5)
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }
    var dueDateStr by remember { mutableStateOf(initialDueDateStr) }

    val categories = if (isReceitaType) FinanceCategories.incomes else FinanceCategories.expenses

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Novo Vencimento de Conta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type selector: Pagar vs Receber
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isReceitaType = false
                            selectedCategory = "Moradia"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isReceitaType) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isReceitaType) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("bill_type_pagar"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("A Pagar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isReceitaType = true
                            selectedCategory = "Salário"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReceitaType) Color(0xFF00C853) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isReceitaType) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("bill_type_receber"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("A Receber", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descrição (Ex: Luz, Aluguel, Pro-labore)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("bill_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = formatCurrencyInput(it) },
                    label = { Text("Valor") },
                    placeholder = { Text("R$ 0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("bill_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dueDateStr,
                    onValueChange = { dueDateStr = formatInputDate(it) },
                    label = { Text("Data de Vencimento (DD/MM/AAAA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("bill_days_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Selecione a Categoria", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val value = parseCurrencyInput(amount)
                            val dueDateMillis = try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dueDateStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            if (title.isNotBlank() && value > 0) {
                                onConfirm(title, value, dueDateMillis, selectedCategory)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_add_bill_confirm")
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

// Helper icons mapping
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

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return format.format(amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getDaysRemaining(dueDate: Long): Long {
    val dueCal = Calendar.getInstance().apply { timeInMillis = dueDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val todayCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

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
    val sb = java.lang.StringBuilder()
    for (i in clean.indices) {
        sb.append(clean[i])
        if ((i == 1 && clean.length > 2) || (i == 3 && clean.length > 4)) {
            sb.append("/")
        }
    }
    return sb.toString()
}

fun formatInputTime(input: String): String {
    val clean = input.replace(Regex("[^\\d]"), "").take(4)
    val sb = java.lang.StringBuilder()
    for (i in clean.indices) {
        sb.append(clean[i])
        if (i == 1 && clean.length > 2) {
            sb.append(":")
        }
    }
    return sb.toString()
}

fun parseDateTime(dateStr: String, timeStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, limit: Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(FinanceCategories.expenses.first()) }
    var limitAmount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Definir Orçamento de Categoria",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        FinanceCategories.expenses.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Limit Input
                OutlinedTextField(
                    value = limitAmount,
                    onValueChange = { limitAmount = formatCurrencyInput(it) },
                    label = { Text("Limite de Gasto Mensal") },
                    placeholder = { Text("R$ 0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limit = parseCurrencyInput(limitAmount)
                            if (limit > 0) {
                                onConfirm(selectedCategory, limit)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterDialog(
    onDismiss: () -> Unit,
    onConfirmSplit: (title: String, amount: Double, category: String) -> Unit
) {
    var totalAmountStr by remember { mutableStateOf("") }
    var peopleCountStr by remember { mutableStateOf("2") }
    var billTitle by remember { mutableStateOf("Jantar com amigos") }
    var selectedCategory by remember { mutableStateOf("Alimentação") }

    val totalAmount = parseCurrencyInput(totalAmountStr)
    val peopleCount = peopleCountStr.toIntOrNull() ?: 1
    val splitAmount = if (peopleCount > 0) totalAmount / peopleCount else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Rachador de Contas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Divida o valor total de uma conta entre amigos e registre sua fatia rapidamente como uma despesa local.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = billTitle,
                    onValueChange = { billTitle = it },
                    label = { Text("Nome da Conta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = totalAmountStr,
                        onValueChange = { totalAmountStr = formatCurrencyInput(it) },
                        label = { Text("Valor Total") },
                        placeholder = { Text("R$ 0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )

                    OutlinedTextField(
                        value = peopleCountStr,
                        onValueChange = { peopleCountStr = it },
                        label = { Text("Amigos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Split result card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Sua Parte da Conta", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            formatCurrency(splitAmount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Total: ${formatCurrency(totalAmount)} dividido por $peopleCount pessoas",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (splitAmount > 0) {
                                onConfirmSplit(billTitle, splitAmount, selectedCategory)
                            }
                        },
                        enabled = splitAmount > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Registrar Despesa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AiAdvisorDialog(
    response: String?,
    isLoading: Boolean,
    chatMessages: List<com.example.data.ChatMessage>,
    isChatLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onInitChat: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var chatInputText by remember { mutableStateOf("") }
    val chatScrollState = rememberLazyListState()

    // Trigger chat initialization once when the Chat tab is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            onInitChat()
            if (chatMessages.isNotEmpty()) {
                chatScrollState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatScrollState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .height(550.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Assistente Financeiro IA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("💡 Insights Rápidos", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("💬 Chat Interativo", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == 0) {
                        // Monthly insights panel
                        if (isLoading) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Analisando seu padrão de consumo...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (response != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MarkdownText(text = response)
                            }
                        } else {
                            Text(
                                "Clique em 'Analisar' para gerar conselhos financeiros personalizados baseados nas suas despesas, orçamentos e metas do mês.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Chat panel
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Message bubbles list
                            LazyColumn(
                                state = chatScrollState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chatMessages) { msg ->
                                    val bubbleBg = if (msg.isUser) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }
                                    val bubbleTextCol = if (msg.isUser) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
                                    val roundedCorners = if (msg.isUser) {
                                        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
                                    } else {
                                        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = alignment
                                    ) {
                                        Card(
                                            shape = roundedCorners,
                                            colors = CardDefaults.cardColors(containerColor = bubbleBg),
                                            modifier = Modifier.widthIn(max = 240.dp)
                                        ) {
                                            Box(modifier = Modifier.padding(12.dp)) {
                                                if (msg.isUser) {
                                                    Text(text = msg.text, fontSize = 13.sp, color = bubbleTextCol)
                                                } else {
                                                    MarkdownText(text = msg.text)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isChatLoading) {
                                    item {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Consultor digitando...",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Message Input Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text("Tire suas dúvidas financeiras...", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (chatInputText.isNotBlank() && !isChatLoading) {
                                            onSendChatMessage(chatInputText)
                                            chatInputText = ""
                                        }
                                    })
                                )

                                IconButton(
                                    onClick = {
                                        if (chatInputText.isNotBlank() && !isChatLoading) {
                                            onSendChatMessage(chatInputText)
                                            chatInputText = ""
                                        }
                                    },
                                    enabled = chatInputText.isNotBlank() && !isChatLoading,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (chatInputText.isNotBlank() && !isChatLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Send,
                                        contentDescription = "Enviar",
                                        tint = if (chatInputText.isNotBlank() && !isChatLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }

                    if (selectedTab == 0) {
                        if (response != null && !isLoading) {
                            Button(
                                onClick = onRefresh,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Atualizar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (response == null && !isLoading) {
                            Button(
                                onClick = onRefresh,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Analisar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Clear chat button for interactive chat
                        TextButton(
                            onClick = onClearChat,
                            enabled = chatMessages.isNotEmpty() && !isChatLoading
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpar Conversa", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("###")) {
                val titleText = trimmedLine.replace("###", "").trim()
                Text(
                    text = titleText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            } else if (trimmedLine.startsWith("##")) {
                val titleText = trimmedLine.replace("##", "").trim()
                Text(
                    text = titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            } else if (trimmedLine.startsWith("#")) {
                val titleText = trimmedLine.replace("#", "").trim()
                Text(
                    text = titleText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
            } else if (trimmedLine.startsWith("-") || trimmedLine.startsWith("*") || (trimmedLine.isNotEmpty() && trimmedLine.first().isDigit() && trimmedLine.contains("."))) {
                val isNumbered = trimmedLine.first().isDigit()
                val itemText = if (isNumbered) trimmedLine else trimmedLine.drop(1).trim()
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    if (!isNumbered) {
                        Text(
                            "•",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = parseBoldMarkdown(itemText),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            } else if (trimmedLine.isNotEmpty()) {
                Text(
                    text = parseBoldMarkdown(trimmedLine),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

fun parseBoldMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    val parts = text.split("**")
    for (i in parts.indices) {
        if (i % 2 == 1) {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
            builder.append(parts[i])
            builder.pop()
        } else {
            builder.append(parts[i])
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun MonthlySummaryDialog(
    response: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(max = 550.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Resumo Mensal IA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.tertiary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Analisando seus gastos com o Gemini...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (response != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            MarkdownText(text = response)
                        }
                    } else {
                        Text(
                            "Clique em 'Gerar Resumo' para que a inteligência artificial analise suas finanças deste mês e sugira dicas de economia.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }

                    Button(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (response != null) "Atualizar" else "Gerar Resumo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, isExpense: Boolean, category: String, date: Long) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    
    val parsed = remember(textInput) {
        ParsedTransactionHelper.parseQuickAddInput(textInput)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Adição Rápida Inteligente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("O que você comprou ou recebeu?") },
                    placeholder = { Text("ex: Almoço 35,50 hoje") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_input_field")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (parsed.amount > 0) {
                            if (parsed.isExpense) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Campos Reconhecidos:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (parsed.isExpense) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (parsed.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Valor: " + if (parsed.amount > 0) formatCurrency(parsed.amount) else "Não detectado",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (parsed.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(if (parsed.isExpense) "Despesa" else "Receita", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Data: ${formatDate(parsed.date)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Categoria: ${parsed.category}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Title, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Título: ${parsed.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (parsed.amount > 0 && parsed.title.isNotBlank()) {
                                onConfirm(parsed.title, parsed.amount, parsed.isExpense, parsed.category, parsed.date)
                            }
                        },
                        enabled = parsed.amount > 0 && parsed.title.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.testTag("quick_add_confirm_button")
                    ) {
                        Text("Adicionar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedFinancialCalculatorDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Juros Compostos", "Amortização")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .heightIn(max = 580.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Calculadora Avançada",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Juros Compostos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Amortização", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedTab == 0) {
                    CompoundInterestCalculator()
                } else {
                    AmortizationCalculator()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

@Composable
fun CompoundInterestCalculator() {
    var initialCapitalStr by remember { mutableStateOf("") }
    var monthlySavingsStr by remember { mutableStateOf("") }
    var interestRateStr by remember { mutableStateOf("") }
    var periodStr by remember { mutableStateOf("") }
    var isPeriodInYears by remember { mutableStateOf(false) }

    val initialCapital = initialCapitalStr.toDoubleOrNull() ?: 0.0
    val monthlySavings = monthlySavingsStr.toDoubleOrNull() ?: 0.0
    val interestRate = interestRateStr.toDoubleOrNull() ?: 0.0
    val period = periodStr.toIntOrNull() ?: 0

    val months = if (isPeriodInYears) period * 12 else period
    val r = (interestRate / 100.0) / 12.0

    var totalAccumulated = initialCapital
    var totalInvested = initialCapital

    if (months > 0) {
        if (r > 0) {
            val fvCapital = initialCapital * Math.pow(1.0 + r, months.toDouble())
            val fvContributions = monthlySavings * ((Math.pow(1.0 + r, months.toDouble()) - 1.0) / r) * (1.0 + r)
            totalAccumulated = fvCapital + fvContributions
            totalInvested = initialCapital + (monthlySavings * months)
        } else {
            totalAccumulated = initialCapital + (monthlySavings * months)
            totalInvested = totalAccumulated
        }
    }
    val totalInterest = totalAccumulated - totalInvested

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = initialCapitalStr,
            onValueChange = { initialCapitalStr = it },
            label = { Text("Capital Inicial (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = monthlySavingsStr,
            onValueChange = { monthlySavingsStr = it },
            label = { Text("Aporte Mensal (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = interestRateStr,
                onValueChange = { interestRateStr = it },
                label = { Text("Taxa de Juros (% ao ano)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.3f)
            )

            OutlinedTextField(
                value = periodStr,
                onValueChange = { periodStr = it },
                label = { Text(if (isPeriodInYears) "Período (Anos)" else "Período (Meses)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Período em anos:", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = isPeriodInYears,
                onCheckedChange = { isPeriodInYears = it }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Resultados da Projeção", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Valor Total Acumulado:", fontSize = 12.sp)
                    Text(formatCurrency(totalAccumulated), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Valor Total Investido:", fontSize = 12.sp)
                    Text(formatCurrency(totalInvested), fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Ganho em Juros:", fontSize = 12.sp)
                    Text(formatCurrency(Math.max(0.0, totalInterest)), fontWeight = FontWeight.Bold, color = Color(0xFF00C853), fontSize = 12.sp)
                }
            }
        }
    }
}

data class AmortizationRow(
    val month: Int,
    val payment: Double,
    val interest: Double,
    val amortization: Double,
    val balance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmortizationCalculator() {
    var loanAmountStr by remember { mutableStateOf("") }
    var interestRateStr by remember { mutableStateOf("") }
    var termStr by remember { mutableStateOf("") }
    var isSacType by remember { mutableStateOf(true) }

    val loanAmount = loanAmountStr.toDoubleOrNull() ?: 0.0
    val interestRate = interestRateStr.toDoubleOrNull() ?: 0.0
    val term = termStr.toIntOrNull() ?: 0

    val r = (interestRate / 100.0) / 12.0
    val schedule = remember(loanAmount, interestRate, term, isSacType) {
        val list = mutableListOf<AmortizationRow>()
        if (loanAmount > 0.0 && term > 0) {
            var balance = loanAmount
            if (isSacType) {
                val amort = loanAmount / term
                for (m in 1..term) {
                    val interest = balance * r
                    val payment = amort + interest
                    balance -= amort
                    list.add(
                        AmortizationRow(
                            month = m,
                            payment = payment,
                            interest = interest,
                            amortization = amort,
                            balance = Math.max(0.0, balance)
                        )
                    )
                }
            } else {
                if (r > 0) {
                    val payment = loanAmount * (r * Math.pow(1.0 + r, term.toDouble())) / (Math.pow(1.0 + r, term.toDouble()) - 1.0)
                    for (m in 1..term) {
                        val interest = balance * r
                        val amort = payment - interest
                        balance -= amort
                        list.add(
                            AmortizationRow(
                                month = m,
                                payment = payment,
                                interest = interest,
                                amortization = amort,
                                balance = Math.max(0.0, balance)
                            )
                        )
                    }
                } else {
                    val payment = loanAmount / term
                    for (m in 1..term) {
                        balance -= payment
                        list.add(
                            AmortizationRow(
                                month = m,
                                payment = payment,
                                interest = 0.0,
                                amortization = payment,
                                balance = Math.max(0.0, balance)
                            )
                        )
                    }
                }
            }
        }
        list
    }

    val totalPaid = schedule.sumOf { it.payment }
    val totalInterest = schedule.sumOf { it.interest }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = loanAmountStr,
            onValueChange = { loanAmountStr = it },
            label = { Text("Valor do Financiamento (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = interestRateStr,
                onValueChange = { interestRateStr = it },
                label = { Text("Juros (% ao ano)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )

            OutlinedTextField(
                value = termStr,
                onValueChange = { termStr = it },
                label = { Text("Prazo (Meses)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sistema:", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = isSacType,
                    onClick = { isSacType = true },
                    label = { Text("SAC", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = !isSacType,
                    onClick = { isSacType = false },
                    label = { Text("PRICE", fontSize = 11.sp) }
                )
            }
        }

        if (schedule.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Pago (com juros):", fontSize = 11.sp)
                        Text(formatCurrency(totalPaid), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total de Juros Pagos:", fontSize = 11.sp)
                        Text(formatCurrency(totalInterest), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Primeira Parcela:", fontSize = 11.sp)
                        Text(formatCurrency(schedule.first().payment), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Última Parcela:", fontSize = 11.sp)
                        Text(formatCurrency(schedule.last().payment), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Tabela de Amortização:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(schedule.size) { index ->
                    val row = schedule[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Parc. ${row.month}", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Prest: ${formatCurrency(row.payment)}", fontSize = 10.sp, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                        Text("Juros: ${formatCurrency(row.interest)}", fontSize = 10.sp, modifier = Modifier.weight(1.8f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.error)
                        Text("Saldo: ${formatCurrency(row.balance)}", fontSize = 10.sp, modifier = Modifier.weight(2.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

data class CategoryComparison(
    val category: String,
    val currentYearAmount: Double,
    val previousYearAmount: Double,
    val difference: Double,
    val percentageChange: Double
)

data class AnnualAnalysis(
    val totalCurrentYear: Double,
    val totalPreviousYear: Double,
    val comparisons: List<CategoryComparison>,
    val maxGrowth: CategoryComparison?,
    val maxReduction: CategoryComparison?
)

@Composable
fun VolarisSplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.7f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        )
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = LinearOutSlowInEasing
        )
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

@Composable
fun FinancialQuizDialog(
    onDismiss: () -> Unit,
    onQuizCompleted: (score: Int) -> Unit
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

    val questions = remember {
        listOf(
            QuizQuestion(
                question = "Qual o valor recomendado para uma Reserva de Emergência para um trabalhador assalariado?",
                options = listOf(
                    "1 a 2 meses de despesas básicas.",
                    "3 a 6 meses de despesas básicas.",
                    "12 a 24 meses de despesas básicas.",
                    "Não há necessidade de reserva se tiver limite no cartão."
                ),
                correctAnswerIndex = 1,
                explanation = "A recomendação clássica é de 3 a 6 meses de custos básicos para suprir imprevistos sem precisar contrair dívidas."
            ),
            QuizQuestion(
                question = "O que são Juros Compostos?",
                options = listOf(
                    "Juros calculados apenas sobre o valor investido inicialmente.",
                    "Juros calculados sobre o valor investido acrescido dos juros já acumulados de períodos anteriores.",
                    "Taxas fixas de administração cobradas por bancos tradicionais.",
                    "Cobrança ilegal de tarifas em contas correntes."
                ),
                correctAnswerIndex = 1,
                explanation = "Também conhecidos como 'juros sobre juros', os juros compostos fazem com que seu dinheiro cresça de forma exponencial ao longo do tempo!"
            ),
            QuizQuestion(
                question = "Na popular regra de planejamento financeiro 50/30/20, a que se referem os 20%?",
                options = listOf(
                    "Despesas Essenciais (como moradia e alimentação).",
                    "Despesas Variáveis / Estilo de Vida (como lazer e jantares).",
                    "Poupança, investimentos e pagamento de dívidas focadas em futuro.",
                    "Dízimos, doações e impostos federais."
                ),
                correctAnswerIndex = 2,
                explanation = "A regra divide suas receitas em: 50% para necessidades, 30% para desejos pessoais e 20% para o seu futuro financeiro (poupar/investir/pagar dívidas)."
            ),
            QuizQuestion(
                question = "Qual a principal desvantagem de pagar apenas o valor mínimo da fatura do cartão de crédito?",
                options = listOf(
                    "O cartão é cancelado imediatamente.",
                    "O score de crédito do usuário sobe de forma descontrolada.",
                    "Incidência de juros do crédito rotativo, que estão entre as taxas mais caras do mercado.",
                    "Não há desvantagem, é uma boa prática financeira."
                ),
                correctAnswerIndex = 2,
                explanation = "O crédito rotativo possui juros altíssimos que geram o famoso efeito 'bola de neve'. Tente sempre quitar a fatura cheia ou parcelar em condições favoráveis se necessário."
            ),
            QuizQuestion(
                question = "Qual o primeiro passo ideal antes de começar a investir em ativos de maior risco (como ações)?",
                options = listOf(
                    "Comprar ações de empresas de tecnologia na bolsa de valores.",
                    "Formar a sua Reserva de Emergência em ativos seguros e de liquidez diária.",
                    "Pegar um empréstimo para operar alavancado.",
                    "Comprar criptomoedas de baixa capitalização."
                ),
                correctAnswerIndex = 1,
                explanation = "Construir uma base sólida com a reserva de emergência garante que você não precisará resgatar investimentos de risco com prejuízo no caso de um imprevisto."
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 530.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Quiz de Educação Financeira",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isQuizFinished) {
                    val currentQuestion = questions[currentQuestionIndex]

                    // Progress indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pergunta ${currentQuestionIndex + 1} de ${questions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Acertos: $score",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Question Text
                        Text(
                            text = currentQuestion.question,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Options List
                        currentQuestion.options.forEachIndexed { index, option ->
                            val isSelected = selectedAnswerIndex == index
                            val containerCol = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                            val borderCol = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }

                            Card(
                                onClick = {
                                    if (selectedAnswerIndex == null) {
                                        selectedAnswerIndex = index
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = containerCol),
                                border = if (borderCol != Color.Transparent) BorderStroke(1.5.dp, borderCol) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val letter = when (index) {
                                        0 -> "A"
                                        1 -> "B"
                                        2 -> "C"
                                        else -> "D"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letter,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = option,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Explanation if answered
                        if (selectedAnswerIndex != null) {
                            val isCorrect = selectedAnswerIndex == currentQuestion.correctAnswerIndex
                            val infoContainerCol = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            val infoBorderCol = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = infoContainerCol),
                                border = BorderStroke(1.dp, infoBorderCol.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                            contentDescription = null,
                                            tint = infoBorderCol,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isCorrect) "Resposta Correta!" else "Ops! Resposta Incorreta.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = infoBorderCol
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentQuestion.explanation,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dialog actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Sair")
                        }

                        Button(
                            onClick = {
                                if (selectedAnswerIndex != null) {
                                    if (selectedAnswerIndex == currentQuestion.correctAnswerIndex) {
                                        score++
                                    }
                                    if (currentQuestionIndex < questions.size - 1) {
                                        currentQuestionIndex++
                                        selectedAnswerIndex = null
                                    } else {
                                        isQuizFinished = true
                                        onQuizCompleted(score)
                                    }
                                }
                            },
                            enabled = selectedAnswerIndex != null,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (currentQuestionIndex == questions.size - 1) "Finalizar" else "Próxima",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Quiz finished panel
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Quiz Concluído!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Sua pontuação: $score de ${questions.size} acertos",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Dynamic advice card depending on score
                        val (title, textAdvice) = when (score) {
                            5 -> Pair(
                                "🏆 Mestre das Finanças",
                                "Excelente! Você tem total domínio sobre planejamento, reserva de emergência, juros compostos e boas práticas financeiras. Continue liderando sua vida financeira com maestria!"
                            )
                            3, 4 -> Pair(
                                "📈 Investidor Consciente",
                                "Muito bom! Seus fundamentos financeiros são bem sólidos. Continue acompanhando seus relatórios e orçamentos na Volaris para refinar ainda mais o seu progresso!"
                            )
                            else -> Pair(
                                "🌱 Aprendiz Financeiro",
                                "Bom começo! Lidar com finanças é um aprendizado constante. Continue monitorando suas receitas, despesas e metas na Volaris para ganhar cada vez mais prática e confiança!"
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                currentQuestionIndex = 0
                                selectedAnswerIndex = null
                                score = 0
                                isQuizFinished = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reiniciar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fechar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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

@Composable
fun LevelUpCelebrationDialog(
    level: Int,
    onDismiss: () -> Unit
) {
    val title = when {
        level >= 25 -> "Grão-Mestre das Finanças 💎"
        level >= 20 -> "Investidor Lendário 👑"
        level >= 15 -> "Mago da Poupança 🧙‍♂️"
        level >= 10 -> "Guardião do Orçamento 🛡️"
        level >= 5 -> "Planejador Consciente 📈"
        else -> "Iniciante Consciente 🌱"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SUBIU DE NÍVEL!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Nível $level",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Seu progresso e disciplina financeira estão dando ótimos frutos! Você está construindo uma jornada sólida.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NOVO TÍTULO DESBLOQUEADO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_levelup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continuar Jornada", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
