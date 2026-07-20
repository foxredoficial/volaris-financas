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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volaris.data.Transaction
import com.volaris.data.UpcomingBill
import com.volaris.ui.components.*
import java.util.*

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
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Organizador Doméstico",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val ctx = LocalContext.current
            IconButton(
                onClick = {
                    val sp = ctx.getSharedPreferences("volaris_domestic", Context.MODE_PRIVATE)
                    val rString = sp.getString("residents_list", "Você:50") ?: "Você:50"
                    val rList = if (rString.isEmpty()) emptyList()
                    else {
                        rString.split(",").mapNotNull {
                            val parts = it.split(":")
                            if (parts.size >= 2) {
                                val name = parts[0]
                                val pct = parts[1].toIntOrNull() ?: 0
                                Pair(name, pct)
                            } else null
                        }
                    }
                    com.volaris.ui.components.PdfExportHelper.exportHouseholdPdf(
                        context = ctx,
                        residents = rList,
                        bills = bills
                    )
                },
                modifier = Modifier.size(36.dp).testTag("domestic_export_pdf_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = "Exportar Divisão de Contas para PDF",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

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
                        onValueChange = { newItemPrice = formatCurrencyInput(it) },
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
                            val p = parseCurrencyInput(newItemPrice)
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
