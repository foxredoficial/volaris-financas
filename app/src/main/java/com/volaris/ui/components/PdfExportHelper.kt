package com.volaris.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.volaris.data.*
import com.volaris.ui.screens.LoanItem
import com.volaris.ui.screens.calculateAccruedAmount
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportHelper {

    class PdfCreator(private val context: Context, private val title: String) {
        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var currentPage = document.startPage(pageInfo)
        var canvas = currentPage.canvas
        val margin = 40f
        var currentY = margin + 80f // leave room for header

        val textPaint = TextPaint().apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 9.5f
            isAntiAlias = true
        }

        val boldPaint = TextPaint().apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 9.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val valuePaint = TextPaint().apply {
            color = Color.parseColor("#49454F")
            textSize = 9.5f
            isAntiAlias = true
        }

        init {
            drawHeader()
        }

        fun drawHeader() {
            val paint = Paint().apply { isAntiAlias = true }
            // Header Primary bar
            paint.color = Color.parseColor("#381E72") // Primary Volaris color
            canvas.drawRect(margin, margin, 595f - margin, margin + 45f, paint)

            // Logo text / App Name
            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("VOLARIS FINANÇAS", margin + 15f, margin + 28f, paint)

            // Title text right-aligned
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val titleText = title
            val textWidth = paint.measureText(titleText)
            canvas.drawText(titleText, 595f - margin - 15f - textWidth, margin + 28f, paint)

            // Draw generation timestamp
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            paint.textSize = 8f
            val dateWidth = paint.measureText(dateStr)
            canvas.drawText("Gerado em: $dateStr", 595f - margin - 15f - dateWidth, margin + 40f, paint)

            // Gray separator line below header
            paint.color = Color.parseColor("#CAC4D0")
            canvas.drawLine(margin, margin + 55f, 595f - margin, margin + 55f, paint)
        }

        fun checkNewPage(neededHeight: Float) {
            if (currentY + neededHeight > 842f - margin - 35f) {
                drawFooter()
                document.finishPage(currentPage)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                currentPage = document.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = margin + 80f // leave room for header
                drawHeader()
            }
        }

        fun drawFooter() {
            val paint = Paint().apply {
                color = Color.parseColor("#79747E")
                textSize = 8f
                isAntiAlias = true
            }
            canvas.drawLine(margin, 842f - margin - 15f, 595f - margin, 842f - margin - 15f, paint)
            canvas.drawText("Página $pageNumber", margin, 842f - margin - 5f, paint)
            canvas.drawText(
                "Volaris Finanças - Inteligência e Gestão Financeira Doméstica",
                595f - margin - paint.measureText("Volaris Finanças - Inteligência e Gestão Financeira Doméstica"),
                842f - margin - 5f,
                paint
            )
        }

        fun drawSectionTitle(title: String) {
            checkNewPage(40f)
            val paint = Paint().apply {
                color = Color.parseColor("#1D1B20")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(title, margin, currentY + 15f, paint)
            paint.color = Color.parseColor("#6750A4") // accent color
            canvas.drawRect(margin, currentY + 22f, margin + 50f, currentY + 25f, paint)
            currentY += 35f
        }

        fun drawRow(label: String, value: String, isBold: Boolean = false, labelColor: String = "#1D1B20", valueColor: String = "#49454F") {
            checkNewPage(18f)
            
            val lp = if (isBold) {
                TextPaint(boldPaint).apply { color = Color.parseColor(labelColor) }
            } else {
                TextPaint(textPaint).apply { color = Color.parseColor(labelColor) }
            }

            val vp = if (isBold) {
                TextPaint(boldPaint).apply { color = Color.parseColor(valueColor) }
            } else {
                TextPaint(valuePaint).apply { color = Color.parseColor(valueColor) }
            }

            canvas.drawText(label, margin + 10f, currentY + 10f, lp)
            
            val valWidth = vp.measureText(value)
            canvas.drawText(value, 595f - margin - 10f - valWidth, currentY + 10f, vp)
            currentY += 18f
        }

        fun drawBullet(text: String) {
            checkNewPage(16f)
            val paint = Paint().apply {
                color = Color.parseColor("#6750A4")
                isAntiAlias = true
            }
            canvas.drawCircle(margin + 12f, currentY + 8f, 3f, paint)
            canvas.drawText(text, margin + 24f, currentY + 11f, textPaint)
            currentY += 16f
        }

        fun drawHorizontalLine() {
            checkNewPage(10f)
            val paint = Paint().apply {
                color = Color.parseColor("#E6E1E5")
                isAntiAlias = true
            }
            canvas.drawLine(margin, currentY + 5f, 595f - margin, currentY + 5f, paint)
            currentY += 10f
        }

        fun finishAndShare(fileName: String) {
            drawFooter()
            document.finishPage(currentPage)

            try {
                val file = File(context.cacheDir, fileName)
                val fos = FileOutputStream(file)
                document.writeTo(fos)
                document.close()
                fos.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório em PDF"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao exportar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 1. Export Transactions PDF List
    fun exportTransactionsPdf(
        context: Context,
        transactions: List<Transaction>,
        startDate: Long?,
        endDate: Long?,
        selectedCategories: List<String>
    ) {
        val creator = PdfCreator(context, "Relatório de Transações")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        creator.drawSectionTitle("Filtros de Exportação")
        if (startDate != null) {
            creator.drawRow("Data de Início:", sdf.format(Date(startDate)))
        } else {
            creator.drawRow("Data de Início:", "Sem limite")
        }
        if (endDate != null) {
            creator.drawRow("Data de Fim:", sdf.format(Date(endDate)))
        } else {
            creator.drawRow("Data de Fim:", "Sem limite")
        }
        if (selectedCategories.isNotEmpty()) {
            creator.drawRow("Categorias selecionadas:", selectedCategories.joinToString(", "))
        } else {
            creator.drawRow("Categorias selecionadas:", "Todas as categorias")
        }

        val totalIncome = transactions.filter { !it.isExpense && it.isPaid }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.isExpense && it.isPaid }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        creator.drawHorizontalLine()
        creator.drawSectionTitle("Resumo Geral das Transações")
        creator.drawRow("Total de Receitas:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalIncome), isBold = true, valueColor = "#00875A")
        creator.drawRow("Total de Despesas:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalExpense), isBold = true, valueColor = "#B3261E")
        creator.drawRow("Saldo Líquido:", "R$ " + String.format(Locale.getDefault(), "%.2f", balance), isBold = true, valueColor = if (balance >= 0) "#00875A" else "#B3261E")
        creator.drawHorizontalLine()

        creator.drawSectionTitle("Lista Detalhada de Transações")
        creator.checkNewPage(30f)

        // Draw Table Header
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = Color.parseColor("#F3EDF7")
        creator.canvas.drawRect(creator.margin, creator.currentY, 595f - creator.margin, creator.currentY + 20f, paint)

        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colDateX = creator.margin + 5f
        val colTitleX = creator.margin + 70f
        val colCatX = creator.margin + 250f
        val colPaidX = creator.margin + 370f
        val colAmtX = 595f - creator.margin - 5f

        creator.canvas.drawText("Data", colDateX, creator.currentY + 14f, paint)
        creator.canvas.drawText("Descrição / Título", colTitleX, creator.currentY + 14f, paint)
        creator.canvas.drawText("Categoria", colCatX, creator.currentY + 14f, paint)
        creator.canvas.drawText("Status", colPaidX, creator.currentY + 14f, paint)
        val headerVal = "Valor"
        creator.canvas.drawText(headerVal, colAmtX - paint.measureText(headerVal), creator.currentY + 14f, paint)

        creator.currentY += 22f

        transactions.forEachIndexed { idx, tx ->
            creator.checkNewPage(18f)

            if (idx % 2 == 1) {
                paint.color = Color.parseColor("#FAF8FC")
                creator.canvas.drawRect(creator.margin, creator.currentY, 595f - creator.margin, creator.currentY + 16f, paint)
            }

            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#49454F")

            val dStr = sdf.format(Date(tx.date))
            creator.canvas.drawText(dStr, colDateX, creator.currentY + 11f, paint)

            var tStr = tx.title
            if (paint.measureText(tStr) > 170f) {
                while (paint.measureText("$tStr...") > 170f && tStr.isNotEmpty()) {
                    tStr = tStr.substring(0, tStr.length - 1)
                }
                tStr = "$tStr..."
            }
            creator.canvas.drawText(tStr, colTitleX, creator.currentY + 11f, paint)
            creator.canvas.drawText(tx.category, colCatX, creator.currentY + 11f, paint)

            val stat = if (tx.isPaid) "Pago" else "Pendente"
            creator.canvas.drawText(stat, colPaidX, creator.currentY + 11f, paint)

            if (tx.isExpense) {
                paint.color = Color.parseColor("#B3261E")
            } else {
                paint.color = Color.parseColor("#00875A")
            }
            val sign = if (tx.isExpense) "-" else "+"
            val valStr = "$sign R$ " + String.format(Locale.getDefault(), "%.2f", tx.amount)
            creator.canvas.drawText(valStr, colAmtX - paint.measureText(valStr), creator.currentY + 11f, paint)

            creator.currentY += 16f
        }

        creator.finishAndShare("Volaris_Relatorio_Transacoes.pdf")
    }

    // 2. Export Annual Analysis PDF
    fun exportAnnualAnalysisPdf(
        context: Context,
        prevYear: Int,
        currentYear: Int,
        totalPrev: Double,
        totalCurrent: Double,
        comparisons: List<CategoryComparison>,
        maxGrowth: CategoryComparison?,
        maxReduction: CategoryComparison?
    ) {
        val creator = PdfCreator(context, "Análise Comparativa Anual")

        creator.drawSectionTitle("Resumo Geral da Comparação ($prevYear vs $currentYear)")
        creator.drawRow("Total Gasto em $prevYear:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalPrev))
        creator.drawRow("Total Gasto em $currentYear:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalCurrent), isBold = true)
        
        val diff = totalCurrent - totalPrev
        val pct = if (totalPrev > 0) (diff / totalPrev) * 100 else 0.0
        val diffSign = if (diff >= 0) "+" else ""
        creator.drawRow(
            "Variação Total:",
            "$diffSign R$ " + String.format(Locale.getDefault(), "%.2f", diff) + " (" + String.format(Locale.getDefault(), "%.1f", pct) + "%)",
            isBold = true,
            valueColor = if (diff >= 0) "#B3261E" else "#00875A"
        )
        creator.drawHorizontalLine()

        creator.drawSectionTitle("Destaques de Consumo")
        if (maxGrowth != null) {
            creator.drawRow("Maior Aumento de Categoria:", maxGrowth.category + " (+" + String.format(Locale.getDefault(), "%.1f", maxGrowth.percentageChange) + "%)", isBold = true, valueColor = "#B3261E")
            creator.drawRow("Diferença Nominal:", "+R$ " + String.format(Locale.getDefault(), "%.2f", maxGrowth.difference))
        } else {
            creator.drawBullet("Sem categorias de despesa em crescimento.")
        }

        if (maxReduction != null) {
            creator.drawRow("Maior Redução de Categoria:", maxReduction.category + " (" + String.format(Locale.getDefault(), "%.1f", maxReduction.percentageChange) + "%)", isBold = true, valueColor = "#00875A")
            creator.drawRow("Economia Nominal:", "R$ " + String.format(Locale.getDefault(), "%.2f", maxReduction.difference))
        } else {
            creator.drawBullet("Sem categorias de despesa em redução.")
        }
        creator.drawHorizontalLine()

        creator.drawSectionTitle("Comparativo por Categorias")
        creator.checkNewPage(30f)

        // Draw Headers
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = Color.parseColor("#F3EDF7")
        creator.canvas.drawRect(creator.margin, creator.currentY, 595f - creator.margin, creator.currentY + 20f, paint)

        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colCatX = creator.margin + 5f
        val colPrevX = creator.margin + 170f
        val colCurrX = creator.margin + 290f
        val colDiffX = creator.margin + 410f
        val colPctX = 595f - creator.margin - 5f

        creator.canvas.drawText("Categoria", colCatX, creator.currentY + 14f, paint)
        creator.canvas.drawText("Ano $prevYear", colPrevX, creator.currentY + 14f, paint)
        creator.canvas.drawText("Ano $currentYear", colCurrX, creator.currentY + 14f, paint)
        creator.canvas.drawText("Diferença", colDiffX, creator.currentY + 14f, paint)
        val headerPct = "Alteração"
        creator.canvas.drawText(headerPct, colPctX - paint.measureText(headerPct), creator.currentY + 14f, paint)

        creator.currentY += 22f

        comparisons.forEachIndexed { index, comp ->
            creator.checkNewPage(18f)

            if (index % 2 == 1) {
                paint.color = Color.parseColor("#FAF8FC")
                creator.canvas.drawRect(creator.margin, creator.currentY, 595f - creator.margin, creator.currentY + 16f, paint)
            }

            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#49454F")

            creator.canvas.drawText(comp.category, colCatX, creator.currentY + 11f, paint)
            creator.canvas.drawText("R$ " + String.format(Locale.getDefault(), "%.2f", comp.previousYearAmount), colPrevX, creator.currentY + 11f, paint)
            creator.canvas.drawText("R$ " + String.format(Locale.getDefault(), "%.2f", comp.currentYearAmount), colCurrX, creator.currentY + 11f, paint)

            val dfSign = if (comp.difference >= 0) "+" else ""
            creator.canvas.drawText("$dfSign R$ " + String.format(Locale.getDefault(), "%.2f", comp.difference), colDiffX, creator.currentY + 11f, paint)

            if (comp.difference >= 0) {
                paint.color = Color.parseColor("#B3261E")
            } else {
                paint.color = Color.parseColor("#00875A")
            }
            val pctStr = "$dfSign" + String.format(Locale.getDefault(), "%.1f", comp.percentageChange) + "%"
            creator.canvas.drawText(pctStr, colPctX - paint.measureText(pctStr), creator.currentY + 11f, paint)

            creator.currentY += 16f
        }

        creator.finishAndShare("Volaris_Analise_Anual.pdf")
    }

    // 3. Export Category Distribution PDF
    fun exportCategoryDistributionPdf(
        context: Context,
        transactions: List<Transaction>
    ) {
        val creator = PdfCreator(context, "Distribuição de Despesas por Categoria")
        val expenses = transactions.filter { it.isExpense && it.isPaid }
        val totalExpenses = expenses.sumOf { it.amount }

        creator.drawSectionTitle("Resumo Geral")
        creator.drawRow("Total Geral de Despesas Pagas:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalExpenses), isBold = true, valueColor = "#B3261E")
        creator.drawHorizontalLine()

        creator.drawSectionTitle("Percentual por Categoria de Gasto")
        creator.checkNewPage(30f)

        val expensesByCategory = expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }

        // Draw Headers
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = Color.parseColor("#F3EDF7")
        creator.canvas.drawRect(creator.margin, creator.currentY, 595f - creator.margin, creator.currentY + 20f, paint)

        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colCat = creator.margin + 10f
        val colAmt = creator.margin + 250f
        val colPct = 595f - creator.margin - 10f

        creator.canvas.drawText("Categoria", colCat, creator.currentY + 14f, paint)
        creator.canvas.drawText("Valor Gasto (R$)", colAmt, creator.currentY + 14f, paint)
        val headerPct = "Percentual (%)"
        creator.canvas.drawText(headerPct, colPct - paint.measureText(headerPct), creator.currentY + 14f, paint)

        creator.currentY += 22f

        expensesByCategory.forEachIndexed { index, (cat, amt) ->
            creator.checkNewPage(18f)

            if (index % 2 == 1) {
                paint.color = Color.parseColor("#FAF8FC")
                creator.canvas.drawRect(creator.margin, creator.currentY, 595f - creator.margin, creator.currentY + 16f, paint)
            }

            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#49454F")

            creator.canvas.drawText(cat, colCat, creator.currentY + 11f, paint)
            creator.canvas.drawText("R$ " + String.format(Locale.getDefault(), "%.2f", amt), colAmt, creator.currentY + 11f, paint)

            val ratio = if (totalExpenses > 0) (amt / totalExpenses) * 100.0 else 0.0
            val ratioStr = String.format(Locale.getDefault(), "%.1f", ratio) + "%"
            creator.canvas.drawText(ratioStr, colPct - paint.measureText(ratioStr), creator.currentY + 11f, paint)

            creator.currentY += 16f
        }

        creator.finishAndShare("Volaris_Distribuicao_Despesas.pdf")
    }

    // 4. Export Savings Goals PDF
    fun exportGoalsPdf(
        context: Context,
        goals: List<SavingsGoal>
    ) {
        val creator = PdfCreator(context, "Relatório de Metas de Economia")

        creator.drawSectionTitle("Visão Geral das Metas de Economia")
        val totalTarget = goals.sumOf { it.targetAmount }
        val totalSaved = goals.sumOf { it.currentAmount }
        creator.drawRow("Quantidade de Metas Cadastradas:", goals.size.toString())
        creator.drawRow("Total dos Objetivos (Alvo):", "R$ " + String.format(Locale.getDefault(), "%.2f", totalTarget), isBold = true)
        creator.drawRow("Total Economizado Até o Momento:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalSaved), isBold = true, valueColor = "#00875A")
        
        val progressPercent = if (totalTarget > 0) (totalSaved / totalTarget) * 100 else 0.0
        creator.drawRow("Percentual do Progresso Consolidado:", String.format(Locale.getDefault(), "%.1f", progressPercent) + "%", isBold = true, valueColor = "#6750A4")
        creator.drawHorizontalLine()

        creator.drawSectionTitle("Lista Detalhada das Metas")
        goals.forEach { goal ->
            creator.checkNewPage(45f)
            val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100 else 0.0
            val status = if (goal.currentAmount >= goal.targetAmount) "Meta Concluída! 🎯" else "Em andamento"
            
            creator.drawRow("Nome do Objetivo:", goal.name, isBold = true)
            creator.drawRow("Previsão (Mês/Ano):", goal.monthYear)
            creator.drawRow("Valor Atual / Alvo:", "R$ " + String.format(Locale.getDefault(), "%.2f", goal.currentAmount) + " / R$ " + String.format(Locale.getDefault(), "%.2f", goal.targetAmount))
            creator.drawRow("Status:", "$status (" + String.format(Locale.getDefault(), "%.1f", pct) + "%)", isBold = true, valueColor = if (goal.currentAmount >= goal.targetAmount) "#00875A" else "#49454F")
            creator.drawHorizontalLine()
        }

        creator.finishAndShare("Volaris_Metas_Economia.pdf")
    }

    // 5. Export Bills & Budgets PDF
    fun exportBillsAndBudgetsPdf(
        context: Context,
        bills: List<UpcomingBill>,
        budgets: List<CategoryBudget>,
        transactions: List<Transaction>
    ) {
        val creator = PdfCreator(context, "Planejamento - Orçamentos e Alertas")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        creator.drawSectionTitle("Alertas de Vencimentos (Contas a Pagar)")
        val totalUnpaid = bills.filter { !it.isPaid }.sumOf { it.amount }
        val totalPaid = bills.filter { it.isPaid }.sumOf { it.amount }
        creator.drawRow("Contas Pagas:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalPaid), valueColor = "#00875A")
        creator.drawRow("Contas Pendentes:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalUnpaid), isBold = true, valueColor = "#B3261E")
        creator.drawHorizontalLine()

        if (bills.isNotEmpty()) {
            creator.drawSectionTitle("Lista de Vencimentos")
            bills.forEach { b ->
                val status = if (b.isPaid) "Pago" else "Pendente"
                creator.drawRow(b.title + " (" + b.category + ")", "Venc: " + sdf.format(Date(b.dueDate)) + " | R$ " + String.format(Locale.getDefault(), "%.2f", b.amount) + " [" + status + "]", isBold = !b.isPaid, valueColor = if (b.isPaid) "#00875A" else "#B3261E")
            }
            creator.drawHorizontalLine()
        }

        creator.drawSectionTitle("Controle de Orçamentos por Categorias")
        if (budgets.isEmpty()) {
            creator.drawBullet("Nenhum orçamento cadastrado.")
        } else {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            budgets.forEach { budget ->
                val currentMonthExpenses = transactions.filter { t ->
                    if (t.isExpense && t.isPaid && t.category == budget.category) {
                        val tc = Calendar.getInstance().apply { timeInMillis = t.date }
                        tc.get(Calendar.MONTH) == currentMonth && tc.get(Calendar.YEAR) == currentYear
                    } else false
                }
                val spent = currentMonthExpenses.sumOf { it.amount }
                val remaining = budget.limitAmount - spent
                val statusStr = if (spent > budget.limitAmount) "Excedido!" else "Dentro do Limite"

                creator.drawRow("Categoria: " + budget.category, "Status: $statusStr", isBold = true, valueColor = if (spent > budget.limitAmount) "#B3261E" else "#00875A")
                creator.drawRow("Limite Definido:", "R$ " + String.format(Locale.getDefault(), "%.2f", budget.limitAmount))
                creator.drawRow("Gasto no Mês:", "R$ " + String.format(Locale.getDefault(), "%.2f", spent), valueColor = if (spent > budget.limitAmount) "#B3261E" else "#49454F")
                creator.drawRow("Disponível:", "R$ " + String.format(Locale.getDefault(), "%.2f", remaining), isBold = true, valueColor = if (remaining >= 0) "#00875A" else "#B3261E")
                creator.drawHorizontalLine()
            }
        }

        creator.finishAndShare("Volaris_Orcamentos_e_Contas.pdf")
    }

    // 6. Export Household Splitter PDF
    fun exportHouseholdPdf(
        context: Context,
        residents: List<Pair<String, Int>>, // Name, Percent
        bills: List<UpcomingBill>
    ) {
        val creator = PdfCreator(context, "Contas de Casa - Divisão")

        creator.drawSectionTitle("Moradores e Divisão Percentual")
        residents.forEach { (name, pct) ->
            creator.drawRow(name, "$pct% do total das contas", isBold = true)
        }
        creator.drawHorizontalLine()

        creator.drawSectionTitle("Divisão Detalhada por Morador")
        val totalUnpaidBills = bills.filter { !it.isPaid }.sumOf { it.amount }
        creator.drawRow("Total de Contas de Casa Pendentes:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalUnpaidBills), isBold = true, valueColor = "#B3261E")
        creator.drawHorizontalLine()

        residents.forEach { (name, pct) ->
            val share = totalUnpaidBills * (pct / 100.0)
            creator.checkNewPage(40f)
            creator.drawRow("Morador: $name ($pct%)", "Cota de Pagamento:", isBold = true)
            creator.drawRow("Valor Individual Devido:", "R$ " + String.format(Locale.getDefault(), "%.2f", share), isBold = true, valueColor = "#6750A4")
            creator.drawHorizontalLine()
        }

        creator.finishAndShare("Volaris_Divisao_Contas_Casa.pdf")
    }

    // 7. Export Loans PDF List
    fun exportLoansPdf(
        context: Context,
        loans: List<LoanItem>
    ) {
        val creator = PdfCreator(context, "Empréstimos e Juros")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val activeLoans = loans.filter { !it.isPaid }
        val totalDebt = activeLoans.filter { it.isDebt }.sumOf { calculateAccruedAmount(it.amount, it.interestRate, it.dueDate) }
        val totalCredit = activeLoans.filter { !it.isDebt }.sumOf { calculateAccruedAmount(it.amount, it.interestRate, it.dueDate) }

        creator.drawSectionTitle("Balanço de Débitos e Créditos")
        creator.drawRow("Total a Receber (Emprestado):", "R$ " + String.format(Locale.getDefault(), "%.2f", totalCredit), isBold = true, valueColor = "#00875A")
        creator.drawRow("Total a Pagar (Devendo):", "R$ " + String.format(Locale.getDefault(), "%.2f", totalDebt), isBold = true, valueColor = "#B3261E")
        creator.drawHorizontalLine()

        creator.drawSectionTitle("Lista Completa de Empréstimos")
        if (loans.isEmpty()) {
            creator.drawBullet("Nenhum registro de empréstimo ativo.")
        } else {
            loans.forEach { item ->
                creator.checkNewPage(50f)
                val totalWithInterest = calculateAccruedAmount(item.amount, item.interestRate, item.dueDate)
                val typeStr = if (item.isDebt) "Você Deve" else "Te Devem"
                val status = if (item.isPaid) "Pago" else "Ativo"

                creator.drawRow(item.title, "Status: $status", isBold = true, valueColor = if (item.isPaid) "#00875A" else "#B3261E")
                creator.drawRow("Pessoa envolvida:", item.person)
                creator.drawRow("Tipo de Transação:", typeStr, isBold = true, valueColor = if (item.isDebt) "#B3261E" else "#00875A")
                creator.drawRow("Valor Original:", "R$ " + String.format(Locale.getDefault(), "%.2f", item.amount))
                creator.drawRow("Taxa de Juros Simples:", String.format(Locale.getDefault(), "%.1f", item.interestRate) + "% ao mês")
                creator.drawRow("Data do Vencimento:", sdf.format(Date(item.dueDate)))
                creator.drawRow("Valor Atualizado (Com Juros):", "R$ " + String.format(Locale.getDefault(), "%.2f", totalWithInterest), isBold = true, valueColor = if (item.isDebt) "#B3261E" else "#00875A")
                creator.drawHorizontalLine()
            }
        }

        creator.finishAndShare("Volaris_Gestao_Emprestimos.pdf")
    }

    // 8. Export Financial Intelligence PDF (Tab-based)
    fun exportIntelligencePdf(
        context: Context,
        activeTab: Int,
        transactions: List<Transaction>,
        bills: List<UpcomingBill>
    ) {
        val creator = PdfCreator(context, "Análise de Inteligência Financeira")

        when (activeTab) {
            0 -> {
                // Rule 50/30/20
                creator.drawSectionTitle("Análise de Distribuição 50/30/20")
                creator.drawBullet("Necessidades Essenciais (50%): Despesas fixas obrigatórias.")
                creator.drawBullet("Desejos Pessoais (30%): Assinaturas, lazer e estilo de vida.")
                creator.drawBullet("Poupança e Investimentos (20%): Provisões, investimentos, metas.")
                creator.drawHorizontalLine()

                val totalIncomes = transactions.filter { !it.isExpense && it.isPaid }.sumOf { it.amount }
                val expenses = transactions.filter { it.isExpense && it.isPaid }

                val needs = expenses.filter {
                    val cat = it.category.lowercase()
                    cat.contains("moradia") || cat.contains("saúde") || cat.contains("saude") ||
                    cat.contains("educação") || cat.contains("educacao") || cat.contains("transporte") ||
                    cat.contains("alimentação") || cat.contains("alimentacao") || cat.contains("contas") ||
                    cat.contains("boletos") || cat.contains("luz") || cat.contains("agua") || cat.contains("água")
                }.sumOf { it.amount }

                val saves = expenses.filter {
                    val cat = it.category.lowercase()
                    cat.contains("investimento") || cat.contains("poupança") || cat.contains("poupanca") ||
                    cat.contains("reserva") || cat.contains("metas")
                }.sumOf { it.amount }

                val wants = expenses.sumOf { it.amount } - needs - saves

                creator.drawRow("Total de Receitas no Período:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalIncomes), isBold = true)
                creator.drawHorizontalLine()

                val targetNeeds = totalIncomes * 0.5
                val targetWants = totalIncomes * 0.3
                val targetSaves = totalIncomes * 0.2

                creator.drawRow("Necessidades (50%):", "R$ " + String.format(Locale.getDefault(), "%.2f", needs) + " / Alvo: R$ " + String.format(Locale.getDefault(), "%.2f", targetNeeds), isBold = true, valueColor = if (needs > targetNeeds) "#B3261E" else "#00875A")
                creator.drawRow("Desejos Pessoais (30%):", "R$ " + String.format(Locale.getDefault(), "%.2f", wants) + " / Alvo: R$ " + String.format(Locale.getDefault(), "%.2f", targetWants), isBold = true, valueColor = if (wants > targetWants) "#B3261E" else "#00875A")
                creator.drawRow("Poupança (20%):", "R$ " + String.format(Locale.getDefault(), "%.2f", saves) + " / Alvo: R$ " + String.format(Locale.getDefault(), "%.2f", targetSaves), isBold = true, valueColor = if (saves >= targetSaves) "#00875A" else "#E57373")
            }
            1 -> {
                // Investment projection simulator intro
                creator.drawSectionTitle("Simulador de Investimentos (Previsão de Juros Compostos)")
                creator.drawBullet("Este relatório ilustra a projeção matemática para o acúmulo de riqueza utilizando juros compostos.")
                creator.drawBullet("As simulações dependem da taxa de rendimento configurada e dos aportes mensais constantes.")
                creator.drawHorizontalLine()
                creator.drawRow("Projeção de Longo Prazo:", "Simulado pelo usuário", isBold = true)
            }
            2 -> {
                // Cash projection
                creator.drawSectionTitle("Fluxo de Caixa e Projeção de Saldos")
                val totalInc = transactions.filter { !it.isExpense && it.isPaid }.sumOf { it.amount }
                val totalExp = transactions.filter { it.isExpense && it.isPaid }.sumOf { it.amount }
                val currentBalance = totalInc - totalExp

                val pendingBills = bills.filter { !it.isPaid }
                val totalPending = pendingBills.sumOf { it.amount }
                val projectedBalance = currentBalance - totalPending

                creator.drawRow("Saldo Líquido em Caixa Hoje:", "R$ " + String.format(Locale.getDefault(), "%.2f", currentBalance), isBold = true, valueColor = if (currentBalance >= 0) "#00875A" else "#B3261E")
                creator.drawRow("Contas Pendentes a Vencer no Mês:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalPending), valueColor = "#B3261E")
                creator.drawRow("Projeção de Saldo Disponível Consolidado:", "R$ " + String.format(Locale.getDefault(), "%.2f", projectedBalance), isBold = true, valueColor = if (projectedBalance >= 0) "#00875A" else "#B3261E")
            }
            3 -> {
                // FIRE
                creator.drawSectionTitle("Análise de Aposentadoria Independente (Método FIRE)")
                creator.drawBullet("FIRE: Financial Independence, Retire Early.")
                creator.drawBullet("Regra dos 4%: Seu patrimônio acumulado deve ser 25x sua despesa anual.")
                creator.drawHorizontalLine()

                val totalExp = transactions.filter { it.isExpense && it.isPaid }.sumOf { it.amount }
                val annualExpense = totalExp * 12.0
                val fireNumber = annualExpense * 25.0

                creator.drawRow("Despesa Mensal Estimada:", "R$ " + String.format(Locale.getDefault(), "%.2f", totalExp))
                creator.drawRow("Despesa Anual Estimada:", "R$ " + String.format(Locale.getDefault(), "%.2f", annualExpense), isBold = true)
                creator.drawRow("Número FIRE Necessário para Aposentar:", "R$ " + String.format(Locale.getDefault(), "%.2f", fireNumber), isBold = true, valueColor = "#00875A")
            }
        }

        creator.finishAndShare("Volaris_Analise_Inteligencia.pdf")
    }
}
