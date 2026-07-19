package com.example.ui

import java.util.Calendar
import java.util.regex.Pattern

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val isExpense: Boolean,
    val category: String,
    val date: Long
)

object ParsedTransactionHelper {
    fun parseQuickAddInput(text: String): ParsedTransaction {
        var amount = 0.0
        var isExpense = true
        val cal = Calendar.getInstance()
        var date = cal.timeInMillis
        var title = text.trim()

        // 1. Extract values / numbers
        // Matches R$ 15,50 or 15.50 or 15,50
        val valuePattern = Pattern.compile("(?:R\\$\\s*)?(\\d+(?:[.,]\\d{2})?)")
        val matcher = valuePattern.matcher(text)
        var foundAmountStr = ""
        if (matcher.find()) {
            foundAmountStr = matcher.group(0) ?: ""
            val numberStr = matcher.group(1) ?: ""
            amount = numberStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        // 2. Extract Type (Expense vs Income)
        val incomeKeywords = listOf("recebi", "salario", "salário", "ganhei", "venda", "freelance", "presente", "receita", "reembolso", "pix de")
        val textLower = text.lowercase()
        if (incomeKeywords.any { textLower.contains(it) }) {
            isExpense = false
        }

        // 3. Extract Date
        if (textLower.contains("ontem")) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            date = cal.timeInMillis
        } else if (textLower.contains("anteontem")) {
            cal.add(Calendar.DAY_OF_YEAR, -2)
            date = cal.timeInMillis
        } else {
            // Check for dd/mm/yyyy or dd/mm
            val datePattern = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?")
            val dateMatcher = datePattern.matcher(text)
            if (dateMatcher.find()) {
                val day = dateMatcher.group(1)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                val month = (dateMatcher.group(2)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                val year = dateMatcher.group(3)?.let {
                    if (it.length == 2) 2000 + it.toInt() else it.toInt()
                } ?: cal.get(Calendar.YEAR)
                
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                date = cal.timeInMillis
            }
        }

        // 4. Extract Title
        var cleanTitle = text
        if (foundAmountStr.isNotEmpty()) {
            cleanTitle = cleanTitle.replace(foundAmountStr, "")
        }
        cleanTitle = cleanTitle.replace("ontem", "", ignoreCase = true)
        cleanTitle = cleanTitle.replace("anteontem", "", ignoreCase = true)
        cleanTitle = cleanTitle.replace("hoje", "", ignoreCase = true)
        cleanTitle = cleanTitle.replace(Pattern.compile("\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?").toRegex(), "")
        cleanTitle = cleanTitle.replace(Regex("[,;\\s\\s+]+"), " ").trim()
        
        if (cleanTitle.isBlank()) {
            cleanTitle = if (isExpense) "Despesa Rápida" else "Receita Rápida"
        } else {
            cleanTitle = cleanTitle.replaceFirstChar { it.uppercase() }
        }

        // 5. Categorize
        var category = "Outros"
        if (isExpense) {
            if (textLower.contains("uber") || textLower.contains("taxi") || textLower.contains("táxi") || textLower.contains("onibus") || textLower.contains("ônibus") || textLower.contains("metro") || textLower.contains("metrô") || textLower.contains("combustivel") || textLower.contains("combustível") || textLower.contains("gasolina") || textLower.contains("transporte")) {
                category = "Transporte"
            } else if (textLower.contains("lanche") || textLower.contains("almoço") || textLower.contains("almoco") || textLower.contains("jantar") || textLower.contains("cafe") || textLower.contains("café") || textLower.contains("restaurante") || textLower.contains("ifood") || textLower.contains("pizza") || textLower.contains("hamburguer")) {
                category = "Alimentação"
            } else if (textLower.contains("mercado") || textLower.contains("supermercado") || textLower.contains("compras") || textLower.contains("feira")) {
                category = "Mercado"
            } else if (textLower.contains("aluguel") || textLower.contains("luz") || textLower.contains("agua") || textLower.contains("água") || textLower.contains("internet") || textLower.contains("condominio") || textLower.contains("condomínio") || textLower.contains("moradia")) {
                category = "Moradia"
            } else if (textLower.contains("cinema") || textLower.contains("show") || textLower.contains("festa") || textLower.contains("jogo") || textLower.contains("cerveja") || textLower.contains("lazer") || textLower.contains("viagem")) {
                category = "Lazer"
            } else if (textLower.contains("farmacia") || textLower.contains("farmácia") || textLower.contains("remedio") || textLower.contains("remédio") || textLower.contains("medico") || textLower.contains("médico") || textLower.contains("saude") || textLower.contains("saúde") || textLower.contains("dentista")) {
                category = "Saúde"
            } else if (textLower.contains("escola") || textLower.contains("curso") || textLower.contains("faculdade") || textLower.contains("livro") || textLower.contains("educaçao") || textLower.contains("educação") || textLower.contains("estudo")) {
                category = "Educação"
            }
        } else {
            if (textLower.contains("salario") || textLower.contains("salário") || textLower.contains("pagamento") || textLower.contains("firma")) {
                category = "Salário"
            } else if (textLower.contains("investimento") || textLower.contains("rendimento") || textLower.contains("juros") || textLower.contains("dividendos") || textLower.contains("rendimentos")) {
                category = "Investimentos"
            } else if (textLower.contains("freelance") || textLower.contains("freela") || textLower.contains("bico") || textLower.contains("job")) {
                category = "Freelance"
            } else if (textLower.contains("presente") || textLower.contains("ganhei") || textLower.contains("doação")) {
                category = "Presente"
            }
        }

        return ParsedTransaction(cleanTitle, amount, isExpense, category, date)
    }
}
