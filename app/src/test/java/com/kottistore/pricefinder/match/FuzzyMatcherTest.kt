package com.kottistore.pricefinder.match

import com.kottistore.pricefinder.data.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    private fun item(
        id: String,
        name: String,
        alias: String,
        category: String = "test",
        purRate: Double = 0.0,
        selRate: Double = 10.0
    ) = Item(id, name, alias, category, purRate, selRate)

    private val matcher = FuzzyMatcher(
        listOf(
            item("10", "sugar", "சர்கரை", "SAKTHI", 55.5, 65.0),
            item("1", "shakthiManjalthul", "மஞ்.தூள்", "SAKTHI", 242.0, 300.0),
            item("100", "shakthimillaguthool", "மிளகுதூள்", "SAKTHI", 930.0, 1100.0),
            item("101", "giragathul", "சிரகதுள்", "SAKTHI", 700.0, 800.0),
            item("200", "colgate strong teeth 100g", "கோல்கேட்", "colgate", 90.0, 100.0),
            item("300", "red rice 1kg", "சிகப்பு அரிசி", "rice", 60.0, 70.0)
        )
    )

    @Test
    fun exactEnglishNameMatchesWithFullScore() {
        val results = matcher.search("sugar")
        assertEquals("sugar", results.first().item.name)
        assertEquals(1.0, results.first().score, 0.001)
    }

    @Test
    fun misspelledEnglishStillMatches() {
        assertEquals("sugar", matcher.search("suger").first().item.name)
    }

    @Test
    fun transliteratedPhraseMatches() {
        assertEquals("shakthiManjalthul", matcher.search("shakthi manjal thool").first().item.name)
    }

    @Test
    fun tamilAliasMatches() {
        assertEquals("sugar", matcher.search("சர்கரை").first().item.name)
    }

    @Test
    fun fillerWordsAreIgnored() {
        assertEquals("sugar", matcher.search("price of sugar").first().item.name)
    }

    @Test
    fun partialBrandMatches() {
        assertEquals("colgate strong teeth 100g", matcher.search("colgate").first().item.name)
    }

    @Test
    fun emptyQueryReturnsNothing() {
        assertTrue(matcher.search("").isEmpty())
    }

    @Test
    fun nonsenseQueryHasNoConfidentMatch() {
        val results = matcher.search("zzzzzz")
        assertTrue(results.isEmpty() || results.first().score < 0.5)
    }
}
