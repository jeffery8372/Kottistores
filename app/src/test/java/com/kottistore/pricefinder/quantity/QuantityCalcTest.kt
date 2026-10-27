package com.kottistore.pricefinder.quantity

import com.kottistore.pricefinder.data.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityCalcTest {

    private fun item(id: String, name: String, alias: String, category: String, sel: Double?) =
        Item(id, name, alias, category, 0.0, sel)

    private val rice = item("r", "rice", "rice", "rice", 80.0)
    private val sugar250 = item("s", "sugar250g", "சர்கரை250g", "SAKTHI", 18.0)
    private val dalda = item("d", "dalda 1lt", "Dalda1lt", "oil", 130.0)
    private val colgate = item("c", "colgate", "colgate", "colgate", 86.0)
    private val sambarava = item("v", "sambarava", "சம்பாரவை500g", "SPICE", 75.0)
    private val goldWinner5L = item("g", "goldwiner5lt", "GoldWinner5lt", "oil", 900.0)
    private val typoWeight = item("t", "oorvasipowder5ookg", "500kg", "soap", 30.0)

    @Test
    fun looseRiceDefaultsToOneKilogram() {
        val pack = QuantityCalc.packInfo(rice)
        assertEquals("kg", pack.defaultUnit)
        assertEquals(1.0, pack.defaultQuantity, 0.0001)
        assertEquals(80.0, QuantityCalc.total(80.0, pack, 1.0, "kg")!!, 0.0001)
    }

    @Test
    fun riceScalesWithFractionalQuantities() {
        val pack = QuantityCalc.packInfo(rice)
        assertEquals(160.0, QuantityCalc.total(80.0, pack, 2.0, "kg")!!, 0.0001)
        assertEquals(40.0, QuantityCalc.total(80.0, pack, 0.5, "kg")!!, 0.0001)
        assertEquals(4.0, QuantityCalc.total(80.0, pack, 0.05, "kg")!!, 0.0001)
    }

    @Test
    fun gramPackDefaultsToItsOwnWeight() {
        val pack = QuantityCalc.packInfo(sugar250)
        assertEquals("g", pack.defaultUnit)
        assertEquals(250.0, pack.defaultQuantity, 0.0001)
        assertEquals(18.0, QuantityCalc.total(18.0, pack, 250.0, "g")!!, 0.0001)
    }

    @Test
    fun gramPackSupportsKilogramsAndPieces() {
        val pack = QuantityCalc.packInfo(sugar250)
        assertEquals(144.0, QuantityCalc.total(18.0, pack, 2.0, "kg")!!, 0.0001)
        assertEquals(36.0, QuantityCalc.total(18.0, pack, 2.0, "pcs")!!, 0.0001)
    }

    @Test
    fun litrePackSupportsMillilitres() {
        val pack = QuantityCalc.packInfo(dalda)
        assertEquals("L", pack.defaultUnit)
        assertEquals(130.0, QuantityCalc.total(130.0, pack, 1.0, "L")!!, 0.0001)
        assertEquals(65.0, QuantityCalc.total(130.0, pack, 500.0, "ml")!!, 0.0001)
        assertEquals(65.0, QuantityCalc.total(130.0, pack, 0.5, "L")!!, 0.0001)
    }

    @Test
    fun fiveLitrePackHasCorrectPerLitrePrice() {
        val pack = QuantityCalc.packInfo(goldWinner5L)
        assertEquals(5.0, pack.defaultQuantity, 0.0001)
        assertEquals(900.0, QuantityCalc.total(900.0, pack, 5.0, "L")!!, 0.0001)
        assertEquals(180.0, QuantityCalc.total(900.0, pack, 1.0, "L")!!, 0.0001)
    }

    @Test
    fun aliasPackSizeIsDetected() {
        val pack = QuantityCalc.packInfo(sambarava)
        assertEquals("g", pack.defaultUnit)
        assertEquals(500.0, pack.defaultQuantity, 0.0001)
        assertEquals(75.0, QuantityCalc.total(75.0, pack, 500.0, "g")!!, 0.0001)
        assertEquals(150.0, QuantityCalc.total(75.0, pack, 1.0, "kg")!!, 0.0001)
    }

    @Test
    fun countItemUsesPieces() {
        val pack = QuantityCalc.packInfo(colgate)
        assertEquals(listOf("pcs"), pack.unitOptions)
        assertEquals(86.0, QuantityCalc.total(86.0, pack, 1.0, "pcs")!!, 0.0001)
        assertEquals(172.0, QuantityCalc.total(86.0, pack, 2.0, "pcs")!!, 0.0001)
    }

    @Test
    fun implausiblePackSizeFallsBackToCount() {
        val pack = QuantityCalc.packInfo(typoWeight)
        assertEquals(QuantityKind.COUNT, pack.kind)
        assertEquals(30.0, QuantityCalc.total(30.0, pack, 1.0, "pcs")!!, 0.0001)
    }

    @Test
    fun unitChangesConvertQuantity() {
        val sugarPack = QuantityCalc.packInfo(sugar250)
        assertEquals(0.25, QuantityCalc.convertQuantity(250.0, "g", "kg", sugarPack), 0.0001)
        assertEquals(250.0, QuantityCalc.convertQuantity(0.25, "kg", "g", sugarPack), 0.0001)
        assertEquals(1.0, QuantityCalc.convertQuantity(250.0, "g", "pcs", sugarPack), 0.0001)
        assertEquals(250.0, QuantityCalc.convertQuantity(1.0, "pcs", "g", sugarPack), 0.0001)

        val ricePack = QuantityCalc.packInfo(rice)
        assertEquals(1.0, QuantityCalc.convertQuantity(1.0, "pcs", "kg", ricePack), 0.0001)
        assertEquals(2000.0, QuantityCalc.convertQuantity(2.0, "kg", "g", ricePack), 0.0001)

        val daldaPack = QuantityCalc.packInfo(dalda)
        assertEquals(2000.0, QuantityCalc.convertQuantity(2.0, "L", "ml", daldaPack), 0.0001)
        assertEquals(0.5, QuantityCalc.convertQuantity(500.0, "ml", "L", daldaPack), 0.0001)
    }

    @Test
    fun nullSellRateHasNoTotal() {
        assertNull(QuantityCalc.total(null, QuantityCalc.packInfo(rice), 2.0, "kg"))
    }

    @Test
    fun quantityFormattingOmitsTrailingZeros() {
        assertEquals("2", QuantityCalc.formatQuantity(2.0))
        assertEquals("0.5", QuantityCalc.formatQuantity(0.5))
        assertEquals("0.05", QuantityCalc.formatQuantity(0.05))
        assertEquals("0.125", QuantityCalc.formatQuantity(0.125))
    }
}
