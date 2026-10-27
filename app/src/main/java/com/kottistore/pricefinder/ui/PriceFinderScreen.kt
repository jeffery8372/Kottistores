@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kottistore.pricefinder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kottistore.pricefinder.AppLanguage
import com.kottistore.pricefinder.data.Item
import com.kottistore.pricefinder.formatPrice
import com.kottistore.pricefinder.match.FuzzyMatcher
import com.kottistore.pricefinder.quantity.QuantityCalc
import com.kottistore.pricefinder.quantity.Quote
import java.util.Locale
import kotlin.math.roundToInt

private val Primary = Color(0xFF1B5E20)
private val PrimarySoft = Color(0xFFE4F0E4)
private val Muted = Color(0xFF6C6C6C)
private val PageBackground = Color(0xFFF7F5F0)
private val ChipBackground = Color(0xFFEDEAE3)
private val QuantityPattern = Regex("\\d{0,6}(\\.\\d{0,3})?")

@Composable
fun KottistoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Primary,
            onPrimary = Color.White,
            primaryContainer = PrimarySoft,
            onPrimaryContainer = Color(0xFF0B3D0F),
            background = PageBackground,
            surface = Color.White,
            onSurface = Color(0xFF1F1F1F),
            error = Color(0xFFB3261E)
        ),
        content = content
    )
}

@Composable
fun PriceFinderScreen(
    itemCount: Int,
    language: AppLanguage,
    listening: Boolean,
    transcript: String,
    results: List<FuzzyMatcher.SearchResult>,
    status: String?,
    onLanguageChange: (AppLanguage) -> Unit,
    onMicClick: () -> Unit,
    onSpeak: (Quote) -> Unit,
    onClear: () -> Unit,
    onTextSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedId by remember(results) {
        mutableStateOf(results.firstOrNull()?.item?.id)
    }

    LaunchedEffect(transcript) {
        if (transcript.isNotBlank()) {
            query = transcript
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KOTTISTORES",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 3.sp
        )
        Text(
            text = "Voice Price Finder",
            fontSize = 14.sp,
            color = Muted
        )

        Spacer(Modifier.height(20.dp))

        Row {
            LanguagePill(
                label = AppLanguage.ENGLISH.label,
                selected = language == AppLanguage.ENGLISH,
                onClick = { onLanguageChange(AppLanguage.ENGLISH) }
            )
            Spacer(Modifier.width(10.dp))
            LanguagePill(
                label = AppLanguage.TAMIL.label,
                selected = language == AppLanguage.TAMIL,
                onClick = { onLanguageChange(AppLanguage.TAMIL) }
            )
        }

        Spacer(Modifier.height(20.dp))

        SearchRow(
            query = query,
            onQueryChange = { query = it },
            onSearch = { if (query.isNotBlank()) onTextSearch(query) }
        )

        Spacer(Modifier.height(22.dp))

        MicButton(listening = listening, onClick = onMicClick)

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (listening) "Listening..." else "Tap to speak, or type above",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (listening) MaterialTheme.colorScheme.error else Muted
        )

        Spacer(Modifier.height(22.dp))

        if (transcript.isNotBlank()) {
            TranscriptCard(transcript)
            Spacer(Modifier.height(12.dp))
        }

        status?.let { message ->
            Text(
                text = message,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = if (results.isEmpty()) MaterialTheme.colorScheme.error else Muted
            )
            Spacer(Modifier.height(12.dp))
        }

        results.forEach { result ->
            ResultCard(
                result = result,
                selected = result.item.id == selectedId,
                onSelect = { selectedId = result.item.id },
                onSpeak = onSpeak
            )
            Spacer(Modifier.height(10.dp))
        }

        if (results.isNotEmpty()) {
            TextButton(onClick = {
                query = ""
                onClear()
            }) {
                Text("Clear")
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = String.format(Locale.US, "%,d items stored offline", itemCount),
            fontSize = 11.sp,
            color = Muted
        )
    }
}

@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Type item name", fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onSearch,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Text("Search", fontSize = 14.sp)
        }
    }
}

@Composable
private fun LanguagePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        shadowElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF444444)
        )
    }
}

@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(132.dp)
            .shadow(if (listening) 0.dp else 10.dp, CircleShape)
            .clip(CircleShape)
            .background(if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MicGlyph(Color.White)
    }
}

@Composable
private fun MicGlyph(color: Color) {
    Canvas(modifier = Modifier.size(54.dp)) {
        val w = size.width
        val h = size.height
        val bodyW = w * 0.32f
        val bodyH = h * 0.44f
        val top = h * 0.10f
        val strokeWidth = w * 0.07f

        drawRoundRect(
            color = color,
            topLeft = Offset((w - bodyW) / 2f, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW / 2f, bodyW / 2f)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset((w - bodyW * 2f) / 2f, top + bodyH * 0.42f),
            size = Size(bodyW * 2f, bodyH * 1.05f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(w / 2f, top + bodyH * 1.47f),
            end = Offset(w / 2f, h * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.89f),
            end = Offset(w * 0.68f, h * 0.89f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun TranscriptCard(transcript: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "YOU SAID",
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = Muted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "\"$transcript\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ResultCard(
    result: FuzzyMatcher.SearchResult,
    selected: Boolean,
    onSelect: () -> Unit,
    onSpeak: (Quote) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = result.item.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (result.item.alias.isNotBlank() && result.item.alias != result.item.name) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = result.item.alias,
                            fontSize = 13.sp,
                            color = Muted
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result.item.category.uppercase(Locale.ROOT),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "\u20B9${formatPrice(result.item.selRate)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "pack price",
                        fontSize = 10.sp,
                        color = Muted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (selected) {
                QuantityEditor(item = result.item, onSpeak = onSpeak)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(result.score * 100).roundToInt()}% match",
                        fontSize = 11.sp,
                        color = Muted
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        val quote = QuantityCalc.defaultQuote(result.item)
                        if (quote != null) onSpeak(quote)
                    }) {
                        Text("Hear price", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityEditor(item: Item, onSpeak: (Quote) -> Unit) {
    val pack = remember(item.id) { QuantityCalc.packInfo(item) }
    var unit by remember(item.id) { mutableStateOf(pack.defaultUnit) }
    var quantityText by remember(item.id) {
        mutableStateOf(QuantityCalc.formatQuantity(pack.defaultQuantity))
    }

    val quantity = quantityText.toDoubleOrNull()
    val total = if (quantity != null) {
        QuantityCalc.total(item.selRate, pack, quantity, unit)
    } else {
        null
    }
    val perUnit = QuantityCalc.perUnitPrice(item.selRate, pack, unit)

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Qty",
                fontSize = 12.sp,
                color = Muted
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = quantityText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(QuantityPattern)) {
                        quantityText = value
                    }
                },
                modifier = Modifier.width(88.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(Modifier.width(8.dp))
            pack.unitOptions.forEach { option ->
                SmallChip(
                    label = option,
                    selected = unit == option,
                    onClick = {
                        val current = quantityText.toDoubleOrNull()
                        quantityText = QuantityCalc.formatQuantity(
                            QuantityCalc.convertQuantity(current, unit, option, pack)
                        )
                        unit = option
                    }
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            quickQuantities(unit).forEach { quick ->
                SmallChip(
                    label = QuantityCalc.formatQuantity(quick),
                    selected = quantity == quick,
                    onClick = { quantityText = QuantityCalc.formatQuantity(quick) }
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTAL",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = Muted
                )
                Text(
                    text = "\u20B9${formatPrice(total)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = perUnitLabel(perUnit, unit),
                    fontSize = 10.sp,
                    color = Muted
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    if (quantity != null && total != null) {
                        onSpeak(Quote(item, quantity, unit, total))
                    }
                }
            ) {
                Text("Hear total", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SmallChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else ChipBackground,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF444444)
        )
    }
}

private fun quickQuantities(unit: String): List<Double> {
    return when (unit) {
        "kg", "L" -> listOf(0.05, 0.5, 1.0, 2.0, 5.0)
        "g", "ml" -> listOf(50.0, 100.0, 250.0, 500.0, 1000.0)
        else -> listOf(1.0, 2.0, 5.0, 10.0, 25.0)
    }
}

private fun perUnitLabel(perUnit: Double?, unit: String): String {
    if (perUnit == null) return "price unavailable"
    val base = if (unit == "pcs") "pc" else unit
    return "\u20B9${formatPrice(perUnit)} / $base"
}
