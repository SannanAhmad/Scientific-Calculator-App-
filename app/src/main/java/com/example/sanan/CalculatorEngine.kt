package com.example.sanan

import java.util.Locale
import kotlin.math.*

enum class AngleUnit {
    DEGREES, RADIANS
}

sealed class CalculationResult {
    data class Success(val value: Double, val formatted: String) : CalculationResult()
    data class Error(val message: String) : CalculationResult()
}

class CalculatorEngine(var angleUnit: AngleUnit = AngleUnit.DEGREES) {

    fun evaluate(expression: String): CalculationResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return CalculationResult.Success(0.0, "0")
        }
        return try {
            val parser = Parser(trimmed, angleUnit)
            val result = parser.parse()
            if (result.isNaN()) {
                CalculationResult.Error("Domain Error")
            } else if (result.isInfinite()) {
                CalculationResult.Error("Overflow")
            } else {
                CalculationResult.Success(result, formatResult(result))
            }
        } catch (e: ArithmeticException) {
            CalculationResult.Error(e.message ?: "Math Error")
        } catch (e: IllegalArgumentException) {
            CalculationResult.Error(e.message ?: "Syntax Error")
        } catch (e: Exception) {
            CalculationResult.Error("Syntax Error")
        }
    }

    private fun formatResult(value: Double): String {
        if (value == 0.0) return "0"
        // Avoid -0.0
        val v = if (abs(value) < 1e-12) 0.0 else value
        
        // If it's a whole integer
        if (abs(v - round(v)) < 1e-11 && abs(v) < 1e14) {
            return round(v).toLong().toString()
        }
        
        val str = v.toString()
        return if (str.length > 14 && !str.contains("E") && !str.contains("e")) {
            String.format(Locale.US, "%.8g", v).trimEnd('0').trimEnd('.')
        } else {
            str
        }
    }

    private class Parser(private val src: String, private val angleUnit: AngleUnit) {
        private var pos = 0
        private val len = src.length

        fun parse(): Double {
            val res = parseExpression()
            skipWhitespace()
            if (pos < len) {
                throw IllegalArgumentException("Unexpected character '${src[pos]}'")
            }
            return res
        }

        // Expression = Term ( ('+' | '-') Term )*
        private fun parseExpression(): Double {
            var left = parseTerm()
            while (true) {
                skipWhitespace()
                if (match('+')) {
                    left += parseTerm()
                } else if (match('-')) {
                    left -= parseTerm()
                } else {
                    break
                }
            }
            return left
        }

        // Term = ImplicitMultiplication ( ('*' | '×' | '/' | '÷' | '%') ImplicitMultiplication )*
        private fun parseTerm(): Double {
            var left = parseImplicitMultiplication()
            while (true) {
                skipWhitespace()
                if (match('*') || match('×')) {
                    left *= parseImplicitMultiplication()
                } else if (match('/') || match('÷')) {
                    val right = parseImplicitMultiplication()
                    if (right == 0.0) throw ArithmeticException("Cannot divide by zero")
                    left /= right
                } else if (match('%')) {
                    val right = parseImplicitMultiplication()
                    if (right == 0.0) throw ArithmeticException("Cannot divide by zero")
                    left %= right
                } else {
                    break
                }
            }
            return left
        }

        // Handles implicit multiplication like 2π, 3sin(x), (2)(3), 2(3)
        private fun parseImplicitMultiplication(): Double {
            var left = parsePower()
            while (true) {
                skipWhitespace()
                if (pos < len) {
                    val ch = src[pos]
                    if (ch == '(' || ch == 'π' || (ch.isLetter() && ch != 'E' && ch != 'e')) {
                        left *= parsePower()
                        continue
                    }
                }
                break
            }
            return left
        }

        // Power = Factorial ('^' Unary)*
        private fun parsePower(): Double {
            var left = parseFactorial()
            skipWhitespace()
            if (match('^')) {
                val right = parseUnary()
                left = left.pow(right)
            }
            return left
        }

        // Factorial = Unary '!'*
        private fun parseFactorial(): Double {
            var valResult = parseUnary()
            while (true) {
                skipWhitespace()
                if (match('!')) {
                    valResult = factorial(valResult)
                } else {
                    break
                }
            }
            return valResult
        }

        private fun factorial(n: Double): Double {
            if (n < 0 || abs(n - round(n)) > 1e-9) {
                throw ArithmeticException("Factorial requires non-negative integer")
            }
            val intN = round(n).toLong()
            if (intN > 170) throw ArithmeticException("Overflow")
            var res = 1.0
            for (i in 2..intN) {
                res *= i
            }
            return res
        }

        // Unary = ('+' | '-') Unary | Primary
        private fun parseUnary(): Double {
            skipWhitespace()
            if (match('+')) {
                return parseUnary()
            }
            if (match('-')) {
                return -parseUnary()
            }
            return parsePrimary()
        }

        // Primary = Number | Constant | Function '(' Expression ')' | '(' Expression ')'
        private fun parsePrimary(): Double {
            skipWhitespace()
            if (pos >= len) {
                throw IllegalArgumentException("Unexpected end of expression")
            }

            if (match('(')) {
                val res = parseExpression()
                skipWhitespace()
                if (!match(')')) {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                return res
            }

            // Constants
            if (match('π') || matchStr("pi")) {
                return Math.PI
            }
            if (matchStr("e")) {
                return Math.E
            }

            // Functions
            val funcs = listOf("asin", "acos", "atan", "sin", "cos", "tan", "log", "ln", "sqrt", "cbrt", "abs")
            for (fn in funcs) {
                if (matchStr(fn)) {
                    skipWhitespace()
                    val hasParen = match('(')
                    val arg = if (hasParen) {
                        val inner = parseExpression()
                        skipWhitespace()
                        if (!match(')')) throw IllegalArgumentException("Missing closing parenthesis for $fn")
                        inner
                    } else {
                        parseUnary()
                    }
                    return evalFunction(fn, arg)
                }
            }

            // Numbers
            val startPos = pos
            var hasDot = false
            while (pos < len) {
                val ch = src[pos]
                if (ch.isDigit()) {
                    pos++
                } else if (ch == '.' && !hasDot) {
                    hasDot = true
                    pos++
                } else {
                    break
                }
            }

            if (startPos == pos) {
                throw IllegalArgumentException("Unexpected character '${src[pos]}'")
            }

            val numStr = src.substring(startPos, pos)
            return numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
        }

        private fun evalFunction(fn: String, arg: Double): Double {
            return when (fn) {
                "sin" -> {
                    val rad = if (angleUnit == AngleUnit.DEGREES) Math.toRadians(arg) else arg
                    val valSin = sin(rad)
                    if (abs(valSin) < 1e-15) 0.0 else valSin
                }
                "cos" -> {
                    val rad = if (angleUnit == AngleUnit.DEGREES) Math.toRadians(arg) else arg
                    val valCos = cos(rad)
                    if (abs(valCos) < 1e-15) 0.0 else valCos
                }
                "tan" -> {
                    val rad = if (angleUnit == AngleUnit.DEGREES) Math.toRadians(arg) else arg
                    val valCos = cos(rad)
                    if (abs(valCos) < 1e-15) throw ArithmeticException("Undefined")
                    val valTan = tan(rad)
                    if (abs(valTan) < 1e-15) 0.0 else valTan
                }
                "asin" -> {
                    if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain Error")
                    val rad = asin(arg)
                    if (angleUnit == AngleUnit.DEGREES) Math.toDegrees(rad) else rad
                }
                "acos" -> {
                    if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain Error")
                    val rad = acos(arg)
                    if (angleUnit == AngleUnit.DEGREES) Math.toDegrees(rad) else rad
                }
                "atan" -> {
                    val rad = atan(arg)
                    if (angleUnit == AngleUnit.DEGREES) Math.toDegrees(rad) else rad
                }
                "log" -> {
                    if (arg <= 0) throw ArithmeticException("Domain Error")
                    log10(arg)
                }
                "ln" -> {
                    if (arg <= 0) throw ArithmeticException("Domain Error")
                    ln(arg)
                }
                "sqrt" -> {
                    if (arg < 0) throw ArithmeticException("Domain Error")
                    sqrt(arg)
                }
                "cbrt" -> cbrt(arg)
                "abs" -> abs(arg)
                else -> throw IllegalArgumentException("Unknown function: $fn")
            }
        }

        private fun match(ch: Char): Boolean {
            if (pos < len && src[pos] == ch) {
                pos++
                return true
            }
            return false
        }

        private fun matchStr(str: String): Boolean {
            if (src.regionMatches(pos, str, 0, str.length, ignoreCase = true)) {
                val nextPos = pos + str.length
                if (nextPos < len && src[nextPos].isLetter()) {
                    return false
                }
                pos = nextPos
                return true
            }
            return false
        }

        private fun skipWhitespace() {
            while (pos < len && src[pos].isWhitespace()) {
                pos++
            }
        }
    }
}
