package com.volaris.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volaris.data.FinanceCategories
import com.volaris.data.Transaction
import com.volaris.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Histórico de Transações",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val context = LocalContext.current
            IconButton(
                onClick = {
                    val startTimestamp = try {
                        if (startDateStr.length == 10) sdfDate.parse(startDateStr)?.time else null
                    } catch (e: Exception) {
                        null
                    }
                    val endTimestamp = try {
                        if (endDateStr.length == 10) sdfDate.parse(endDateStr)?.time else null
                    } catch (e: Exception) {
                        null
                    }
                    val cats = if (selectedCategoryFilter == "Todas") emptyList() else listOf(selectedCategoryFilter)
                    
                    com.volaris.ui.components.PdfExportHelper.exportTransactionsPdf(
                        context = context,
                        transactions = filteredList,
                        startDate = startTimestamp,
                        endDate = endTimestamp,
                        selectedCategories = cats
                    )
                },
                modifier = Modifier.size(36.dp).testTag("transactions_export_pdf_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = "Exportar Histórico de Transações para PDF",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

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
