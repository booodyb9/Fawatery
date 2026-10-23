package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.example.export.InvoiceExporter
import com.example.model.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File

@RunWith(AndroidJUnit4::class)
class InvoicePdfTest {
    @Test fun invoiceWithoutTaxNumberStillExports() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val seller = CompanyProfile(companyName = "منشأة اختبار", vatNumber = "")
        val invoice = pdfSampleInvoice().copy(seller = seller, issuedAt = "2026-10-23T08:30:00Z")
        val file = InvoiceExporter.generatePdf(context, invoice, seller)
        assertTrue(file.length() > 1000)
        val artifacts = File(context.filesDir, "validation-artifacts").apply { mkdirs() }
        file.copyTo(File(artifacts, "invoice-without-tax-number.pdf"), overwrite = true)
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use {
            assertTrue(it.pageCount > 0)
        }
    }

    @Test fun pdfUsesTheSelectedCompanyLogo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val image = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.RED)
        }
        val bytes = java.io.ByteArrayOutputStream().use { out ->
            image.compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray()
        }
        image.recycle()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val name = "logo_${hash}.png"
        File(context.filesDir, "company_logos").apply { mkdirs() }.resolve(name).writeBytes(bytes)
        // JSON also exercises backward-compatible profile loading, before and after this feature.
        val seller = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            .adapter(CompanyProfile::class.java).fromJson(
                """{"companyName":"منشأة اختبار","vatNumber":"300000000000003","logoFileName":"$name"}"""
            )!!
        val invoice = pdfSampleInvoice().copy(seller = seller, issuedAt = "2026-10-23T08:30:00Z")
        val file = InvoiceExporter.generatePdf(context, invoice, seller)
        val artifacts = File(context.filesDir, "validation-artifacts").apply { mkdirs() }
        file.copyTo(File(artifacts, "custom-logo-invoice.pdf"), overwrite = true)
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            renderer.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val pixel = bitmap.getPixel(510 * 2, 45 * 2)
                assertTrue("Selected logo must be printed", android.graphics.Color.red(pixel) > 200 &&
                    android.graphics.Color.green(pixel) < 50 && android.graphics.Color.blue(pixel) < 50)
                bitmap.recycle()
            }
        }
    }

    @Test fun importedLogoSurvivesSourceRemovalAndKeepsAspectRatio() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("logo-source-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLUE)
        }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", source)
        val name = com.example.branding.CompanyLogoStore.importFromUri(context, uri)
        source.delete()
        val loaded = com.example.branding.CompanyLogoStore.load(context, name)
        assertEquals(256, loaded.width)
        assertEquals(128, loaded.height)
        assertEquals(android.graphics.Color.BLUE, loaded.getPixel(100, 50))
        loaded.recycle()
    }

    @Test fun logoImportRejectsNonImageFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("invalid-logo-", ".txt", context.cacheDir)
        source.writeText("synthetic invalid image")
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", source)
        try {
            com.example.branding.CompanyLogoStore.importFromUri(context, uri)
            fail("Invalid input must leave the selected logo unchanged")
        } catch (_: IllegalArgumentException) {
        } finally { source.delete() }
    }

    @Test fun logoImportAppliesPhotoOrientation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("rotated-logo-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.GREEN)
        }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        androidx.exifinterface.media.ExifInterface(source.path).apply {
            setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", source)
        val name = com.example.branding.CompanyLogoStore.importFromUri(context, uri)
        source.delete()
        val loaded = com.example.branding.CompanyLogoStore.load(context, name)
        assertEquals(40, loaded.width)
        assertEquals(80, loaded.height)
        loaded.recycle()
    }

    @Test fun longArabicInvoicePaginatesAndItsPrintedQrDecodes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val company = CompanyProfile(companyName = "منشأة تجريبية للفحص — TEST ONLY", vatNumber = "300000000000003",
            commercialRegistration = "", phone = "", address = "عنوان تجريبي", iban = "")
        val invoice = pdfSampleInvoice().copy(
            seller = company, issuedAt = "2026-10-23T08:30:00Z",
            items = (1..35).map { InvoiceItem("$it", "بند تجريبي طويل باللغة العربية مع أرقام 123 وأحرف English ".repeat(3), 2.5, 10.0) },
            notes = "ملاحظة عربية طويلة لا يجب قصها ".repeat(70)
        )
        val file = InvoiceExporter.generatePdf(context, invoice, company)
        assertTrue(file.length() > 0)
        assertTrue("The 35-line fixture should remain practical to share", file.length() < 5_000_000)
        val artifacts = File(context.filesDir, "validation-artifacts").apply { mkdirs() }
        file.copyTo(File(artifacts, "sample-arabic-invoice.pdf"), overwrite = true)
        val renderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        renderer.use {
            assertTrue("Long invoices require multiple pages", it.pageCount > 1)
            for (index in listOf(0, it.pageCount - 1)) {
                it.openPage(index).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    File(artifacts, if (index == 0) "first-page.png" else "last-page.png").outputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    if (index == it.pageCount - 1) {
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        val result = MultiFormatReader().decode(
                            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))),
                            mapOf(DecodeHintType.TRY_HARDER to true)
                        )
                        assertEquals(invoice.generateZatcaTlvBase64(company.companyName, company.vatNumber), result.text)
                    }
                    bitmap.recycle()
                }
            }
        }
    }
}

// Synthetic fixture only; the test never opens the invoice repository.
private fun pdfSampleInvoice() = Invoice(
    id = "PDF-TEST", invoiceNumber = "TEST-001",
    clientName = "عميل تجريبي", clientCompany = "شركة اختبار", clientPhone = "",
    clientEmail = "", clientVatNumber = "", clientAddress = "عنوان اختبار",
    issueDate = "2026-10-01", dueDate = "2026-10-31", projectCategory = "اختبار",
    items = listOf(InvoiceItem("i", "خدمة تجريبية", 1.0, 100.0)), status = InvoiceStatus.PENDING
)
