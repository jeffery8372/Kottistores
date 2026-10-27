package com.kottistore.pricefinder.quantity

import com.kottistore.pricefinder.data.Item
import java.util.Locale
import kotlin.math.roundToLong

enum class QuantityKind { WEIGHT, VOLUME, COUNT }

data class PackInfo(
    val kind: QuantityKind,
    val baseSize: Double,
    val defaultUnit: String,
    val defaultQuantity: Double,
    val unitOptions: List<String>
)

data class Quote(
    val item: Item,
    val quantity: Double,
    val unit: String,
    val total: Double
)

object QuantityCalc {

    private data class Parsed(val kind: QuantityKind, val baseSize: Double)

    private val KG = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:kgs?|kilograms?)", RegexOption.IGNORE_CASE)
    private val G = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:gms?|grams?|grm|g)", RegexOption.IGNORE_CASE)
    private val ML = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:mls?|millilitres?|milliliters?)", RegexOption.IGNORE_CASE)
    private val L = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:lts?|ltrs?|litres?|liters?|l)", RegexOption.IGNORE_CASE)

    private const val MAX_PLAUSIBLE_KG = 60.0
    private const val MAX_PLAUSIBLE_LITRES = 60.0

    private val LOOSE_WEIGHT_CATEGORIES = setOf(
        "rice", "spice", "sugar", "dhal", "dal", "thinai", "wheat",
        "grams", "arice", "pulses", "gram"
    )
    private val LOOSE_VOLUME_CATEGORIES = setOf("oil")

    fun defaultQuote(item: Item): Quote? {
        val pack = packInfo(item)
        val total = total(item.selRate, pack, pack.defaultQuantity, pack.defaultUnit) ?: return null
        return Quote(item, pack.defaultQuantity, pack.defaultUnit, total)
    }

    fun packInfo(item: Item): PackInfo {
        parse(item.name)?.let { return fromParsed(it) }
        parse(item.alias)?.let { return fromParsed(it) }

        val category = item.category.trim().lowercase(Locale.ROOT)
        if (category in LOOSE_VOLUME_CATEGORIES) {
            return PackInfo(
                kind = QuantityKind.VOLUME,
                baseSize = 0.0,
                defaultUnit = "L",
                defaultQuantity = 1.0,
                unitOptions = listOf("pcs", "L", "ml")
            )
        }
        if (category in LOOSE_WEIGHT_CATEGORIES) {
            return PackInfo(
                kind = QuantityKind.WEIGHT,
                baseSize = 0.0,
                defaultUnit = "kg",
                defaultQuantity = 1.0,
                unitOptions = listOf("pcs", "kg", "g")
            )
        }
        return PackInfo(
            kind = QuantityKind.COUNT,
            baseSize = 0.0,
            defaultUnit = "pcs",
            defaultQuantity = 1.0,
            unitOptions = listOf("pcs")
        )
    }

    fun total(selRate: Double?, pack: PackInfo, quantity: Double, unit: String): Double? {
        if (selRate == null || quantity < 0) return null
        if (unit == "pcs") return round2(selRate * quantity)

        val baseQuantity = when (unit) {
            "g", "ml" -> quantity / 1000.0
            else -> quantity
        }
        val perBase = if (pack.baseSize > 0.0) selRate / pack.baseSize else selRate
        return round2(perBase * baseQuantity)
    }

    fun perUnitPrice(selRate: Double?, pack: PackInfo, unit: String): Double? {
        if (selRate == null) return null
        if (unit == "pcs") return selRate
        val baseUnit = if (pack.kind == QuantityKind.VOLUME) "L" else "kg"
        return if (unit == baseUnit) {
            if (pack.baseSize > 0.0) round2(selRate / pack.baseSize) else selRate
        } else {
            val perBase = if (pack.baseSize > 0.0) selRate / pack.baseSize else selRate
            round2(perBase / 1000.0)
        }
    }

    fun convertQuantity(quantity: Double?, from: String, to: String, pack: PackInfo): Double {
        if (from == to) return quantity ?: pack.defaultQuantity
        if (to == "pcs") return 1.0
        if (from == "pcs") return defaultQuantityInUnit(to, pack)
        val value = quantity ?: pack.defaultQuantity
        val base = if (from == "g" || from == "ml") value / 1000.0 else value
        return if (to == "g" || to == "ml") round3(base * 1000.0) else round3(base)
    }

    private fun defaultQuantityInUnit(unit: String, pack: PackInfo): Double {
        return when (unit) {
            "kg" -> if (pack.baseSize > 0.0) round3(pack.baseSize) else 1.0
            "g" -> if (pack.baseSize > 0.0) round3(pack.baseSize * 1000.0) else 1000.0
            "L" -> if (pack.baseSize > 0.0) round3(pack.baseSize) else 1.0
            "ml" -> if (pack.baseSize > 0.0) round3(pack.baseSize * 1000.0) else 1000.0
            else -> 1.0
        }
    }

    fun formatQuantity(quantity: Double): String {
        if (quantity == quantity.roundToLong().toDouble()) {
            return quantity.roundToLong().toString()
        }
        return String.format(Locale.US, "%.3f", quantity)
            .trimEnd('0')
            .trimEnd('.')
    }

    fun spokenUnit(unit: String, tamil: Boolean): String {
        if (tamil) {
            return when (unit) {
                "kg" -> "கிலோ"
                "g" -> "கிராம்"
                "L" -> "லிட்டர்"
                "ml" -> "மில்லி"
                else -> "பீஸ்"
            }
        }
        return when (unit) {
            "kg" -> "kg"
            "g" -> "grams"
            "L" -> "litres"
            "ml" -> "ml"
            else -> "pieces"
        }
    }

    private fun parse(text: String): Parsed? {
        KG.find(text)?.let { return plausibleWeight(it.groupValues[1].toDouble()) }
        ML.find(text)?.let { return plausibleVolume(it.groupValues[1].toDouble() / 1000.0) }
        L.find(text)?.let { return plausibleVolume(it.groupValues[1].toDouble()) }
        G.find(text)?.let { return plausibleWeight(it.groupValues[1].toDouble() / 1000.0) }
        return null
    }

    private fun plausibleWeight(kilograms: Double): Parsed? {
        if (kilograms <= 0.0 || kilograms > MAX_PLAUSIBLE_KG) return null
        return Parsed(QuantityKind.WEIGHT, kilograms)
    }

    private fun plausibleVolume(litres: Double): Parsed? {
        if (litres <= 0.0 || litres > MAX_PLAUSIBLE_LITRES) return null
        return Parsed(QuantityKind.VOLUME, litres)
    }

    private fun fromParsed(parsed: Parsed): PackInfo {
        return when (parsed.kind) {
            QuantityKind.WEIGHT -> {
                val grams = parsed.baseSize * 1000.0
                val useGrams = parsed.baseSize < 1.0
                PackInfo(
                    kind = QuantityKind.WEIGHT,
                    baseSize = parsed.baseSize,
                    defaultUnit = if (useGrams) "g" else "kg",
                    defaultQuantity = round3(if (useGrams) grams else parsed.baseSize),
                    unitOptions = listOf("pcs", "kg", "g")
                )
            }
            QuantityKind.VOLUME -> {
                val millilitres = parsed.baseSize * 1000.0
                val useMillilitres = parsed.baseSize < 1.0
                PackInfo(
                    kind = QuantityKind.VOLUME,
                    baseSize = parsed.baseSize,
                    defaultUnit = if (useMillilitres) "ml" else "L",
                    defaultQuantity = round3(if (useMillilitres) millilitres else parsed.baseSize),
                    unitOptions = listOf("pcs", "L", "ml")
                )
            }
            QuantityKind.COUNT -> PackInfo(
                kind = QuantityKind.COUNT,
                baseSize = 0.0,
                defaultUnit = "pcs",
                defaultQuantity = 1.0,
                unitOptions = listOf("pcs")
            )
        }
    }

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    private fun round3(value: Double): Double = (value * 1000.0).roundToLong() / 1000.0
}
