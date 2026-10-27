package com.kottistore.pricefinder.match

import java.text.Normalizer as UnicodeNormalizer
import java.util.Locale

object Normalizer {

    private val FILLER_WORDS = setOf(
        "price", "rate", "cost", "mrp", "selling", "sell", "current",
        "of", "the", "for", "what", "whats", "is", "how", "much",
        "tell", "me", "show", "please", "item", "items",
        "rupee", "rupees", "rubai",
        "enna", "evlo", "evvalavu", "sollu", "solu", "kudunga",
        "venum", "venu", "vanakkam"
    )

    private val NON_WORD = Regex("[^\\p{L}\\p{N}\\p{M}\\s]")
    private val SPACES = Regex("\\s+")
    private val LATIN_NON_WORD = Regex("[^a-z\\u0B80-\\u0BFF]")
    private val DUPLICATES = Regex("([a-z])\\1+")

    fun normalize(input: String): String {
        val n = UnicodeNormalizer.normalize(input, UnicodeNormalizer.Form.NFC)
            .lowercase(Locale.ROOT)
        return n.replace(NON_WORD, " ")
            .replace(SPACES, " ")
            .trim()
    }

    fun cleanQuery(input: String): String {
        return normalize(input)
            .split(' ')
            .filter { it.isNotBlank() && it !in FILLER_WORDS }
            .joinToString(" ")
    }

    fun tokens(input: String): List<String> {
        return normalize(input).split(' ').filter { it.isNotBlank() }
    }

    fun isTamil(input: String): Boolean {
        return input.any { it in '\u0B80'..'\u0BFF' }
    }

    fun latinPhonetic(input: String): String {
        var s = normalize(input)
        val replacements = listOf(
            "zh" to "l", "sh" to "s", "ch" to "c", "th" to "t",
            "dh" to "d", "kh" to "k", "gh" to "g", "ph" to "p",
            "bh" to "b", "jh" to "j", "ck" to "k", "qu" to "k",
            "x" to "ks"
        )
        for ((from, to) in replacements) {
            s = s.replace(from, to)
        }
        s = s.replace(LATIN_NON_WORD, "")
        s = s.replace(DUPLICATES, "$1")

        val vowels = listOf(
            "aa" to "a", "ee" to "i", "ii" to "i", "oo" to "u",
            "uu" to "u", "ae" to "e", "ai" to "ei", "ou" to "u"
        )
        for ((from, to) in vowels) {
            s = s.replace(from, to)
        }
        s = s.replace(DUPLICATES, "$1")
        s = s.replace("h", "")
        s = s.replace("w", "v")
        s = s.replace("z", "s")
        s = s.replace("f", "p")
        s = s.replace("q", "k")
        s = s.replace("c", "k")
        return s
    }

    fun skeleton(input: String): String {
        val n = UnicodeNormalizer.normalize(input, UnicodeNormalizer.Form.NFC)
            .lowercase(Locale.ROOT)
        val builder = StringBuilder(n.length)
        for (ch in n) {
            val type = Character.getType(ch)
            if (type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt()
            ) {
                continue
            }
            if (ch.isWhitespace() || ch == '\u200C' || ch == '\u200D') {
                continue
            }
            builder.append(ch)
        }
        return builder.toString()
    }
}
