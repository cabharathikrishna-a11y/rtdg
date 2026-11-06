package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Paint.Align
import android.text.TextPaint
import android.text.TextUtils
import com.example.data.LoanCycle
import com.example.data.WeeklyPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementGenerator {
    suspend fun generateCustomerStatementBitmap(
        context: Context,
        businessName: String,
        customerName: String,
        collectionDay: String,
        activeLoan: LoanCycle,
        payments: List<WeeklyPayment>,
        themeName: String,
        customizationCode: String,
        customerPhone: String,
        isClosedLoan: Boolean = false
    ): Bitmap = withContext(Dispatchers.Default) {
        val sortedPayments = payments
            .filter { it.amountPaid > 0.0 }
            .sortedWith(compareBy<WeeklyPayment> { it.paymentDate }.thenBy { it.weekNumber })
        val width = 800
        val dynamicRowHeight = if (sortedPayments.isEmpty()) 100f else (sortedPayments.size * 45f)
        val height = maxOf(800, (540f + dynamicRowHeight + 150f).toInt())
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw Background
        val bgPaint = Paint().apply {
            color = 0xFFF8FAFC.toInt() // Slate 50
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // Header Paint
        val headerPaint = Paint().apply {
            color = 0xFF0F172A.toInt() // Dark Slate 900
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 180f, headerPaint)
        
        // Title
        val titlePaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val displayedBusinessName = truncateText(businessName.ifEmpty { "MD FINANCE" }, titlePaint, 450f)
        canvas.drawText(displayedBusinessName, 40f, 75f, titlePaint)
        
        val subtitlePaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val statementSubtitle = if (isClosedLoan) "Official Customer Loan Statement (Closed Account)" else "Official Customer Loan Statement"
        canvas.drawText(statementSubtitle, 40f, 115f, subtitlePaint)
        
        // Date
        val datePaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Align.RIGHT
        }
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText(df.format(Date()), width.toFloat() - 40f, 75f, datePaint)
        
        // Customer Info Card (Left Box)
        val cardPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val cardBottom = if (isClosedLoan) 420f else 410f
        canvas.drawRoundRect(40f, 210f, 385f, cardBottom, 16f, 16f, cardPaint)
        canvas.drawRoundRect(40f, 210f, 385f, cardBottom, 16f, 16f, borderPaint)
        
        val textPaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val labelPaint = Paint().apply {
            color = 0xFF64748B.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        // Changed "Client Profile" to "Customer Details"
        canvas.drawText("Customer Details", 60f, 245f, textPaint)
        
        val displayedCustomerName = truncateText("Name: $customerName", labelPaint, 300f)
        val displayedCustomerPhone = truncateText("Phone: $customerPhone", labelPaint, 300f)
        val displayedCollectionDay = truncateText("Collection Day: $collectionDay", labelPaint, 300f)
        
        canvas.drawText(displayedCustomerName, 60f, 285f, labelPaint)
        canvas.drawText(displayedCustomerPhone, 60f, 315f, labelPaint)
        canvas.drawText(displayedCollectionDay, 60f, 345f, labelPaint)
        
        // Loan details card (Right Box)
        val cardRight = width.toFloat() - 40f
        canvas.drawRoundRect(415f, 210f, cardRight, cardBottom, 16f, 16f, cardPaint)
        canvas.drawRoundRect(415f, 210f, cardRight, cardBottom, 16f, 16f, borderPaint)
        
        canvas.drawText("Loan Summary", 435f, 245f, textPaint)
        
        val amtPaid = payments.sumOf { it.amountPaid }.toLong()
        val totalContractVal = (activeLoan.loanAmount + activeLoan.interestAmount).toLong()
        val outstandingVal = if (isClosedLoan) 0L else maxOf(0L, totalContractVal - amtPaid)
        val dispersalDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(activeLoan.startDate))
        
        if (isClosedLoan) {
            canvas.drawText("Date of Dispersal: $dispersalDateStr", 435f, 275f, labelPaint)
            canvas.drawText("Total Contract Value: ₹$totalContractVal", 435f, 305f, labelPaint)
            canvas.drawText("Amt Paid: ₹$amtPaid", 435f, 335f, labelPaint)
            canvas.drawText("Outstanding: ₹$outstandingVal", 435f, 365f, labelPaint)
            val statusLabelPaint = Paint().apply {
                color = 0xFFDC2626.toInt()
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("Status: CLOSED", 435f, 395f, statusLabelPaint)
        } else {
            canvas.drawText("Date of Dispersal: $dispersalDateStr", 435f, 280f, labelPaint)
            canvas.drawText("Total Contract Value: ₹$totalContractVal", 435f, 310f, labelPaint)
            canvas.drawText("Amt Paid: ₹$amtPaid", 435f, 340f, labelPaint)
            canvas.drawText("Outstanding: ₹$outstandingVal", 435f, 370f, labelPaint)
        }
        
        // Payments Table Title (Simplified to "Transaction History")
        val tableTitleY = if (isClosedLoan) 450f else 440f
        canvas.drawText("Transaction History", 40f, tableTitleY, textPaint)
        
        // Table Header
        val thPaint = Paint().apply {
            color = 0xFFF1F5F9.toInt()
            style = Paint.Style.FILL
        }
        val thTop = if (isClosedLoan) 470f else 460f
        val thBottom = thTop + 45f
        canvas.drawRect(40f, thTop, width.toFloat() - 40f, thBottom, thPaint)
        canvas.drawRect(40f, thTop, width.toFloat() - 40f, thBottom, borderPaint)
        
        val thTextPaint = Paint().apply {
            color = 0xFF334155.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Week No", 60f, thTop + 30f, thTextPaint)
        canvas.drawText("Date", 190f, thTop + 30f, thTextPaint)
        canvas.drawText("Amnt Rec.", 400f, thTop + 30f, thTextPaint)
        canvas.drawText("UPI / Cash", 600f, thTop + 30f, thTextPaint)
        
        var currentY = thBottom + 35f
        
        val rowPaint = Paint().apply {
            color = 0xFF334155.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val boldRowPaint = Paint().apply {
            color = 0xFF16A34A.toInt() // Green
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val rowDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sortedPayments.forEachIndexed { index, p ->
            val pDateStr = rowDateFormat.format(Date(p.paymentDate))
            val isOnline = !p.upiTxnId.isNullOrEmpty() || 
                           p.notes.contains("Online", ignoreCase = true) || 
                           p.notes.contains("UPI", ignoreCase = true) || 
                           p.notes.contains("GPay", ignoreCase = true) || 
                           p.notes.contains("Bank", ignoreCase = true)
            val pModeStr = if (isOnline) "UPI" else "Cash"
            val weekLabel = if (p.weekNumber > 0) p.weekNumber.toString() else (index + 1).toString()
            canvas.drawText(weekLabel, 60f, currentY, rowPaint)
            canvas.drawText(pDateStr, 190f, currentY, rowPaint)
            canvas.drawText("₹${p.amountPaid.toLong()}", 400f, currentY, boldRowPaint)
            canvas.drawText(pModeStr, 600f, currentY, rowPaint)
            
            // draw divider
            canvas.drawLine(40f, currentY + 15f, width.toFloat() - 40f, currentY + 15f, borderPaint)
            currentY += 45f
        }
        
        if (sortedPayments.isEmpty()) {
            canvas.drawText("No payments found.", 120f, 550f, rowPaint)
            currentY = 600f
        }
        
        // Add "THANK YOU" at the end of the transaction history
        val thankYouY = currentY + 50f
        val thankYouPaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Align.CENTER
        }
        canvas.drawText("THANK YOU", (width / 2).toFloat(), thankYouY, thankYouPaint)
        
        bitmap
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        val textPaint = TextPaint(paint)
        return TextUtils.ellipsize(text, textPaint, maxWidth, TextUtils.TruncateAt.END).toString()
    }
}

