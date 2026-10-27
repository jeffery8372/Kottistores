package com.kottistore.pricefinder.match

import com.kottistore.pricefinder.data.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealDatasetTest {

    private fun loadItems(): List<Item> {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("items.tsv")) {
            "items.tsv test resource is missing"
        }
        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 6) return@mapNotNull null
                Item(
                    id = parts[0],
                    name = parts[1],
                    alias = parts[2],
                    category = parts[3],
                    purRate = parts[4].toDoubleOrNull(),
                    selRate = parts[5].toDoubleOrNull()
                )
            }.toList()
        }
    }

    private val matcher = FuzzyMatcher(loadItems())

    @Test
    fun loadsFullDataset() {
        assertEquals(1564, matcher.count)
    }

    @Test
    fun exactSugar() {
        val top = matcher.search("sugar").first()
        assertEquals("sugar", top.item.name)
        assertEquals(65.0, top.item.selRate!!, 0.001)
    }

    @Test
    fun transliteratedShakthiManjalThool() {
        assertEquals("shakthiManjalthul", matcher.search("shakthi manjal thool").first().item.name)
    }

    @Test
    fun tamilAliasForSugar() {
        assertEquals("sugar", matcher.search("சர்கரை").first().item.name)
    }

    @Test
    fun giragathulMatches() {
        assertTrue(matcher.search("giragathul").first().item.name.startsWith("giragathul"))
    }

    @Test
    fun colgatePrefixMatchesColgate() {
        val top = matcher.search("colgate").first()
        assertTrue(top.item.name.startsWith("colgate"))
        assertTrue(top.score > 0.9)
    }

    @Test
    fun fillerWordsWorkOnRealData() {
        assertEquals("sugar", matcher.search("enna price of sugar evlo").first().item.name)
    }

    @Test
    fun resultsAreSortedByConfidence() {
        val results = matcher.search("milk")
        assertTrue(results.size >= 2)
        assertTrue(results[0].score >= results[1].score)
    }
}
