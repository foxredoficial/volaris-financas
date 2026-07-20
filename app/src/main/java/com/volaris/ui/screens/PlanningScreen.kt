package com.volaris.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volaris.data.CategoryBudget
import com.volaris.data.SavingsGoal
import com.volaris.data.Transaction
import com.volaris.data.UpcomingBill

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
