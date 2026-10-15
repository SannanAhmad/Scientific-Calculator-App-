package com.example.sanan

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var scrollExpression: HorizontalScrollView
    private lateinit var tvModeDegRad: TextView
    private lateinit var tvModeInv: TextView

    private lateinit var btnDegRad: Button
    private lateinit var btnInv: Button
    private lateinit var btnSin: Button
    private lateinit var btnCos: Button
    private lateinit var btnTan: Button
    private lateinit var btnLn: Button
    private lateinit var btnLog: Button

    private var expression = StringBuilder()
    private var lastAnswer = "0"
    private var isInvMode = false
    private var isEvaluated = false
    private val calculatorEngine = CalculatorEngine(AngleUnit.DEGREES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()
        updateDisplay()
    }

    private fun initViews() {
        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
        scrollExpression = findViewById(R.id.scrollExpression)
        tvModeDegRad = findViewById(R.id.tvModeDegRad)
        tvModeInv = findViewById(R.id.tvModeInv)

        btnDegRad = findViewById(R.id.btnDegRad)
        btnInv = findViewById(R.id.btnInv)
        btnSin = findViewById(R.id.btnSin)
        btnCos = findViewById(R.id.btnCos)
        btnTan = findViewById(R.id.btnTan)
        btnLn = findViewById(R.id.btnLn)
        btnLog = findViewById(R.id.btnLog)
    }

    private fun setupClickListeners() {
        // Number buttons
        val numButtons = mapOf(
            R.id.btn0 to "0",
            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9",
            R.id.btn00 to "00",
            R.id.btnDot to "."
        )

        for ((id, valStr) in numButtons) {
            findViewById<Button>(id).setOnClickListener {
                if (isEvaluated) {
                    expression.clear()
                    isEvaluated = false
                }
                expression.append(valStr)
                updateDisplay()
            }
        }

        // Basic operators
        val opButtons = mapOf(
            R.id.btnAdd to " + ",
            R.id.btnSubtract to " - ",
            R.id.btnMultiply to " × ",
            R.id.btnDivide to " ÷ ",
            R.id.btnPercent to " % ",
            R.id.btnParenOpen to "(",
            R.id.btnParenClose to ")",
            R.id.btnE to "e",
            R.id.btnPi to "π",
            R.id.btnFactorial to "!",
            R.id.btnPower to "^",
            R.id.btnAbs to "abs("
        )

        for ((id, opStr) in opButtons) {
            findViewById<Button>(id).setOnClickListener {
                isEvaluated = false
                expression.append(opStr)
                updateDisplay()
            }
        }

        // Scientific special buttons
        findViewById<Button>(R.id.btnSquare).setOnClickListener {
            isEvaluated = false
            expression.append("^2")
            updateDisplay()
        }

        findViewById<Button>(R.id.btnSqrt).setOnClickListener {
            isEvaluated = false
            expression.append("sqrt(")
            updateDisplay()
        }

        findViewById<Button>(R.id.btnCbrt).setOnClickListener {
            isEvaluated = false
            expression.append("cbrt(")
            updateDisplay()
        }

        findViewById<Button>(R.id.btnReciprocal).setOnClickListener {
            isEvaluated = false
            expression.append("1/(")
            updateDisplay()
        }

        findViewById<Button>(R.id.btnAns).setOnClickListener {
            if (isEvaluated) {
                expression.clear()
                isEvaluated = false
            }
            expression.append(lastAnswer)
            updateDisplay()
        }

        findViewById<Button>(R.id.btnPlusMinus).setOnClickListener {
            isEvaluated = false
            if (expression.isEmpty()) {
                expression.append("-")
            } else if (expression.startsWith("-")) {
                expression.deleteCharAt(0)
            } else {
                expression.insert(0, "-")
            }
            updateDisplay()
        }

        // Function buttons sensitive to INV mode
        btnSin.setOnClickListener {
            isEvaluated = false
            expression.append(if (isInvMode) "asin(" else "sin(")
            updateDisplay()
        }

        btnCos.setOnClickListener {
            isEvaluated = false
            expression.append(if (isInvMode) "acos(" else "cos(")
            updateDisplay()
        }

        btnTan.setOnClickListener {
            isEvaluated = false
            expression.append(if (isInvMode) "atan(" else "tan(")
            updateDisplay()
        }

        btnLn.setOnClickListener {
            isEvaluated = false
            expression.append(if (isInvMode) "e^" else "ln(")
            updateDisplay()
        }

        btnLog.setOnClickListener {
            isEvaluated = false
            expression.append(if (isInvMode) "10^" else "log(")
            updateDisplay()
        }

        // Mode Toggles
        btnDegRad.setOnClickListener {
            calculatorEngine.angleUnit = if (calculatorEngine.angleUnit == AngleUnit.DEGREES) {
                AngleUnit.RADIANS
            } else {
                AngleUnit.DEGREES
            }
            val unitName = calculatorEngine.angleUnit.name.substring(0, 3)
            btnDegRad.text = unitName
            tvModeDegRad.text = unitName
            updateDisplay()
        }

        btnInv.setOnClickListener {
            isInvMode = !isInvMode
            tvModeInv.visibility = if (isInvMode) View.VISIBLE else View.GONE
            btnSin.text = if (isInvMode) "asin" else "sin"
            btnCos.text = if (isInvMode) "acos" else "cos"
            btnTan.text = if (isInvMode) "atan" else "tan"
            btnLn.text = if (isInvMode) "eˣ" else "ln"
            btnLog.text = if (isInvMode) "10ˣ" else "log"
        }

        // AC & DEL
        findViewById<Button>(R.id.btnAC).setOnClickListener {
            expression.clear()
            isEvaluated = false
            tvResult.text = ""
            updateDisplay()
        }

        findViewById<Button>(R.id.btnDel).setOnClickListener {
            if (expression.isNotEmpty()) {
                isEvaluated = false
                // Handle multi-char functions like asin(, sqrt(, etc.
                val text = expression.toString()
                val multiCharTokens = listOf("asin(", "acos(", "atan(", "sqrt(", "cbrt(", "sin(", "cos(", "tan(", "log(", "abs(", "ln(", "10^", "e^")
                var removedToken = false
                for (token in multiCharTokens) {
                    if (text.endsWith(token)) {
                        expression.delete(expression.length - token.length, expression.length)
                        removedToken = true
                        break
                    }
                }
                if (!removedToken) {
                    // Check if ends with trailing spaces around operators e.g. " + "
                    if (text.endsWith(" ") && text.length >= 3) {
                        expression.delete(expression.length - 3, expression.length)
                    } else {
                        expression.deleteCharAt(expression.length - 1)
                    }
                }
                updateDisplay()
            }
        }

        // Equals
        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            if (expression.isNotEmpty()) {
                val res = calculatorEngine.evaluate(expression.toString())
                when (res) {
                    is CalculationResult.Success -> {
                        tvResult.text = res.formatted
                        tvResult.setTextColor(getColor(R.color.text_result_final))
                        lastAnswer = res.formatted
                        isEvaluated = true
                    }
                    is CalculationResult.Error -> {
                        tvResult.text = res.message
                        tvResult.setTextColor(getColor(R.color.btn_action_text))
                    }
                }
            }
        }
    }

    private fun updateDisplay() {
        val exprStr = expression.toString()
        tvExpression.text = if (exprStr.isEmpty()) "0" else exprStr

        // Scroll to end of expression
        scrollExpression.post {
            scrollExpression.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }

        // Live calculation preview
        if (exprStr.isNotEmpty() && !isEvaluated) {
            val res = calculatorEngine.evaluate(exprStr)
            when (res) {
                is CalculationResult.Success -> {
                    tvResult.text = res.formatted
                    tvResult.setTextColor(getColor(R.color.text_result))
                }
                is CalculationResult.Error -> {
                    // Don't show error while typing incomplete expressions, keep last valid result or blank
                }
            }
        } else if (exprStr.isEmpty()) {
            tvResult.text = ""
        }
    }
}
