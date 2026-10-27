package com.kottistore.pricefinder.match

import com.kottistore.pricefinder.data.Item

class FuzzyMatcher(items: List<Item>) {

    private data class Entry(
        val item: Item,
        val normName: String,
        val normAlias: String,
        val phonName: String,
        val phonAlias: String,
        val skelName: String,
        val skelAlias: String,
        val nameTokens: Set<String>,
        val aliasTokens: Set<String>
    )

    data class SearchResult(
        val item: Item,
        val score: Double,
        val matchedAlias: Boolean
    )

    private val entries: List<Entry> = items.mapNotNull { item ->
        val normName = Normalizer.normalize(item.name)
        if (normName.isBlank()) return@mapNotNull null
        Entry(
            item = item,
            normName = normName,
            normAlias = Normalizer.normalize(item.alias),
            phonName = Normalizer.latinPhonetic(item.name),
            phonAlias = Normalizer.latinPhonetic(item.alias),
            skelName = Normalizer.skeleton(item.name),
            skelAlias = Normalizer.skeleton(item.alias),
            nameTokens = Normalizer.tokens(item.name).toSet(),
            aliasTokens = Normalizer.tokens(item.alias).toSet()
        )
    }

    val count: Int
        get() = entries.size

    fun search(query: String, limit: Int = 3): List<SearchResult> {
        val cleaned = Normalizer.cleanQuery(query)
        if (cleaned.isBlank()) return emptyList()

        val tamil = Normalizer.isTamil(cleaned)
        val queryPhon = if (tamil) "" else Normalizer.latinPhonetic(cleaned)
        val querySkel = Normalizer.skeleton(cleaned)
        val queryTokens = Normalizer.tokens(cleaned).toSet()

        val results = ArrayList<SearchResult>()
        for (entry in entries) {
            var best = 0.0
            var alias = false

            val nameScore = scoreAgainst(
                cleaned, queryPhon, querySkel, queryTokens,
                entry.normName, entry.phonName, entry.skelName, entry.nameTokens
            )
            if (nameScore > best) {
                best = nameScore
                alias = false
            }

            if (entry.normAlias.isNotBlank() || entry.skelAlias.isNotBlank()) {
                val aliasScore = scoreAgainst(
                    cleaned, queryPhon, querySkel, queryTokens,
                    entry.normAlias, entry.phonAlias, entry.skelAlias, entry.aliasTokens
                )
                if (aliasScore > best) {
                    best = aliasScore
                    alias = true
                }
            }

            if (best >= 0.35) {
                results.add(SearchResult(entry.item, best, alias))
            }
        }

        results.sortWith(
            compareByDescending<SearchResult> { it.score }
                .thenBy { it.item.name }
        )
        return results.take(limit)
    }

    private fun scoreAgainst(
        queryNorm: String,
        queryPhon: String,
        querySkel: String,
        queryTokens: Set<String>,
        norm: String,
        phon: String,
        skel: String,
        tokens: Set<String>
    ): Double {
        var score = similarity(queryNorm, norm)
        score = maxOf(score, similarity(queryNorm, skel) * 0.95)
        if (queryPhon.isNotEmpty() && phon.isNotEmpty()) {
            score = maxOf(score, similarity(queryPhon, phon) * 0.99)
        }
        if (querySkel.isNotEmpty() && skel.isNotEmpty()) {
            score = maxOf(score, similarity(querySkel, skel) * 0.97)
        }
        if (queryTokens.isNotEmpty() && tokens.isNotEmpty()) {
            score = maxOf(score, tokenContainment(queryTokens, tokens))
        }
        return score.coerceIn(0.0, 1.0)
    }

    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0

        val distance = levenshtein(a, b)
        val maxLength = maxOf(a.length, b.length)
        var best = 1.0 - distance.toDouble() / maxLength

        val shorter = if (a.length <= b.length) a else b
        val longer = if (a.length <= b.length) b else a
        if (shorter.length >= 3 && longer.contains(shorter)) {
            val lengthFactor = shorter.length.toDouble() / longer.length
            best = maxOf(best, 0.88 + 0.10 * lengthFactor)
        }
        return best.coerceIn(0.0, 1.0)
    }

    private fun tokenContainment(queryTokens: Set<String>, itemTokens: Set<String>): Double {
        val matched = queryTokens.count { it in itemTokens }
        if (matched == 0) return 0.0
        val ratio = matched.toDouble() / queryTokens.size
        return if (matched == queryTokens.size) {
            val sizeFactor = queryTokens.size.toDouble() / itemTokens.size
            0.90 + 0.07 * minOf(1.0, sizeFactor)
        } else {
            ratio * 0.85
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val x = a.codePoints().toArray()
        val y = b.codePoints().toArray()
        if (x.isEmpty()) return y.size
        if (y.isEmpty()) return x.size

        var previous = IntArray(y.size + 1) { it }
        var current = IntArray(y.size + 1)

        for (i in 1..x.size) {
            current[0] = i
            for (j in 1..y.size) {
                val cost = if (x[i - 1] == y[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[y.size]
    }
}
