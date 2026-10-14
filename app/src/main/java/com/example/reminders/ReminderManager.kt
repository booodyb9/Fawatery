package com.example.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.export.InvoiceExporter
import com.example.export.ReminderType
import com.example.model.CompanyProfile
import com.example.model.Invoice
import com.example.model.InvoiceStatus
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ScheduledReminder(
    val id: String,
    val invoice: Invoice,
    val type: ReminderType,
    val targetDate: String,
    val daysUntilDue: Long,
    val isDueNow: Boolean,
    var isSent: Boolean = false,
    var sentAt: String? = null
)

object ReminderManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Scans invoices and calculates automated reminders:
     * - 3 days before due date
     * - On due date (0 days)
     * - Overdue (< 0 days)
     */
    fun evaluateReminders(
        invoices: List<Invoice>,
        todayDate: Date = dateFormat.parse("2026-10-14") ?: Date()
    ): List<ScheduledReminder> {
        val reminders = mutableListOf<ScheduledReminder>()

        invoices.filter { it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.DRAFT }
            .forEach { invoice ->
                try {
                    val dueDate = dateFormat.parse(invoice.dueDate) ?: return@forEach
                    val diffMillis = dueDate.time - todayDate.time
                    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

                    when {
                        // 3 days before due date
                        diffDays in 1..3 -> {
                            reminders.add(
                                ScheduledReminder(
                                    id = "REM-3D-${invoice.id}",
                                    invoice = invoice,
                                    type = ReminderType.THREE_DAYS_BEFORE,
                                    targetDate = invoice.dueDate,
                                    daysUntilDue = diffDays,
                                    isDueNow = true
                                )
                            )
                        }
                        // On the due date
                        diffDays == 0L -> {
                            reminders.add(
                                ScheduledReminder(
                                    id = "REM-TODAY-${invoice.id}",
                                    invoice = invoice,
                                    type = ReminderType.ON_DUE_DATE,
                                    targetDate = invoice.dueDate,
                                    daysUntilDue = 0,
                                    isDueNow = true
                                )
                            )
                        }
                        // Past due date
                        diffDays < 0 -> {
                            reminders.add(
                                ScheduledReminder(
                                    id = "REM-OVERDUE-${invoice.id}",
                                    invoice = invoice,
                                    type = ReminderType.OVERDUE,
                                    targetDate = invoice.dueDate,
                                    daysUntilDue = diffDays,
                                    isDueNow = true
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

        return reminders.sortedBy { it.daysUntilDue }
    }

    /**
     * Dispatches reminder via WhatsApp with clean phone number formatting.
     */
    fun sendWhatsAppReminder(
        context: Context,
        invoice: Invoice,
        company: CompanyProfile,
        type: ReminderType
    ) {
        val message = InvoiceExporter.createReminderMessage(invoice, company, type)
        val cleanPhone = invoice.clientPhone.replace("+", "").replace(" ", "").replace("-", "")

        try {
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general share intent if WhatsApp is not directly installed
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "إرسال التذكير للعميل"))
        }
    }

    /**
     * Dispatches reminder via SMS.
     */
    fun sendSmsReminder(
        context: Context,
        invoice: Invoice,
        company: CompanyProfile,
        type: ReminderType
    ) {
        val message = InvoiceExporter.createReminderMessage(invoice, company, type)
        val smsUri = Uri.parse("smsto:${invoice.clientPhone}")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "إرسال التذكير عبر SMS"))
        }
    }
}
