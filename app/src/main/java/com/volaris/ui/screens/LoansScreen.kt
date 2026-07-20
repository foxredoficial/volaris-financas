package com.volaris.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.volaris.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(modifier = Modifier.width(8.dp))
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        com.volaris.ui.components.PdfExportHelper.exportLoansPdf(
                            context = context,
                            loans = loans
                        )
                    },
                    modifier = Modifier.size(36.dp).testTag("loans_export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Exportar Empréstimos para PDF",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
            onConfirm = { title, person, amount, rate, due, isDebt ->
                val newLoan = LoanItem(
                    title = title,
                    person = person,
                    amount = amount,
                    interestRate = rate,
                    dueDate = due,
                    isDebt = isDebt,
                    isPaid = false,
                    id = System.currentTimeMillis().toString() + "_" + (100..999).random()
                )
                val updated = loans + newLoan
                saveLoans(updated)
                showAddLoanDialog = false
                Toast.makeText(context, "Empréstimo registrado!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, person: String, amount: Double, interestRate: Double, dueDate: Long, isDebt: Boolean) -> Unit
) {
    var isDebt by remember { mutableStateOf(true) }
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
                        onClick = { isDebt = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDebt) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Devo para alguém", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { isDebt = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDebt) Color(0xFF00C853) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isDebt) Color.White else MaterialTheme.colorScheme.onSurface
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
                                onConfirm(title.trim(), person.trim(), cleanAmount, rate, parsedDue, isDebt)
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
