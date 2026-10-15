package com.example.sanan

import org.junit.Assert.*
import org.junit.Test

class CalculatorEngineTest {

    private val engine = CalculatorEngine()

    @Test
    fun testBasicOperations() {
        val res1 = engine.evaluate("2 + 3 * 4")
        assertTrue(res1 is CalculationResult.Success)
        assertEquals("14", (res1 as CalculationResult.Success).formatted)

        val res2 = engine.evaluate("(2 + 3) * 4")
        assertTrue(res2 is CalculationResult.Success)
        assertEquals("20", (res2 as CalculationResult.Success).formatted)

        val res3 = engine.evaluate("10 / 4")
        assertTrue(res3 is CalculationResult.Success)
        assertEquals("2.5", (res3 as CalculationResult.Success).formatted)
    }

    @Test
    fun testTrigonometryDegrees() {
        engine.angleUnit = AngleUnit.DEGREES

        val sin30 = engine.evaluate("sin(30)")
        assertTrue(sin30 is CalculationResult.Success)
        assertEquals(0.5, (sin30 as CalculationResult.Success).value, 1e-6)

        val cos60 = engine.evaluate("cos(60)")
        assertTrue(cos60 is CalculationResult.Success)
        assertEquals(0.5, (cos60 as CalculationResult.Success).value, 1e-6)

        val tan45 = engine.evaluate("tan(45)")
        assertTrue(tan45 is CalculationResult.Success)
        assertEquals(1.0, (tan45 as CalculationResult.Success).value, 1e-6)
    }

    @Test
    fun testTrigonometryRadians() {
        engine.angleUnit = AngleUnit.RADIANS

        val sinPi2 = engine.evaluate("sin(π / 2)")
        assertTrue(sinPi2 is CalculationResult.Success)
        assertEquals(1.0, (sinPi2 as CalculationResult.Success).value, 1e-6)
    }

    @Test
    fun testFunctionsAndConstants() {
        engine.angleUnit = AngleUnit.DEGREES

        val logRes = engine.evaluate("log(100)")
        assertTrue(logRes is CalculationResult.Success)
        assertEquals("2", (logRes as CalculationResult.Success).formatted)

        val lnRes = engine.evaluate("ln(e)")
        assertTrue(lnRes is CalculationResult.Success)
        assertEquals("1", (lnRes as CalculationResult.Success).formatted)

        val sqrtRes = engine.evaluate("sqrt(16)")
        assertTrue(sqrtRes is CalculationResult.Success)
        assertEquals("4", (sqrtRes as CalculationResult.Success).formatted)

        val cbrtRes = engine.evaluate("cbrt(27)")
        assertTrue(cbrtRes is CalculationResult.Success)
        assertEquals("3", (cbrtRes as CalculationResult.Success).formatted)
    }

    @Test
    fun testFactorial() {
        val res = engine.evaluate("5!")
        assertTrue(res is CalculationResult.Success)
        assertEquals("120", (res as CalculationResult.Success).formatted)

        val zeroFact = engine.evaluate("0!")
        assertTrue(zeroFact is CalculationResult.Success)
        assertEquals("1", (zeroFact as CalculationResult.Success).formatted)
    }

    @Test
    fun testImplicitMultiplication() {
        engine.angleUnit = AngleUnit.DEGREES
        val res1 = engine.evaluate("2π")
        assertTrue(res1 is CalculationResult.Success)
        assertEquals(2 * Math.PI, (res1 as CalculationResult.Success).value, 1e-6)

        val res2 = engine.evaluate("2sin(30)")
        assertTrue(res2 is CalculationResult.Success)
        assertEquals(1.0, (res2 as CalculationResult.Success).value, 1e-6)
    }

    @Test
    fun testErrorHandling() {
        val divZero = engine.evaluate("5 / 0")
        assertTrue(divZero is CalculationResult.Error)
        assertEquals("Cannot divide by zero", (divZero as CalculationResult.Error).message)

        val sqrtNeg = engine.evaluate("sqrt(-4)")
        assertTrue(sqrtNeg is CalculationResult.Error)
        assertEquals("Domain Error", (sqrtNeg as CalculationResult.Error).message)
    }
}
