package com.example.repository

import android.content.Context
import com.example.FawratyApplication
import com.example.model.*
import com.example.reminders.ReminderOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal data class InvoiceStore(
    val version: Int = 1,
    val company: CompanyProfile = CompanyProfile(),
    val clients: List<Client> = emptyList(),
    val catalog: List<CatalogProduct> = defaultCatalog(),
    val invoices: List<Invoice> = emptyList(),
    val nextNumbers: Map<String, Int> = emptyMap(),
    val reminderOptions: ReminderOptions = ReminderOptions(),
    val quotes: List<Quote> = emptyList()
)

/** Offline durable storage. Existing snapshots are never replaced when loading fails. */
class FawratyRepository(context: Context = FawratyApplication.appContext) : java.io.Closeable {
    private val legacyPrefs = context.getSharedPreferences("fawraty_records", Context.MODE_PRIVATE)
    private val database = InvoiceDatabase(context.applicationContext)
    private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(InvoiceStore::class.java)
    private val _companyProfile = MutableStateFlow(CompanyProfile())
    val companyProfile = _companyProfile.asStateFlow()
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients = _clients.asStateFlow()
    private val _catalog = MutableStateFlow<List<CatalogProduct>>(emptyList())
    val catalog = _catalog.asStateFlow()
    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices = _invoices.asStateFlow()
    private val _quotes = MutableStateFlow<List<Quote>>(emptyList())
    val quotes = _quotes.asStateFlow()
    private val _reminderOptions = MutableStateFlow(ReminderOptions())
    val reminderOptions = _reminderOptions.asStateFlow()
    private val _storageError = MutableStateFlow<String?>(null)
    val storageError = _storageError.asStateFlow()

    init {
        synchronized(storageLock) {
            try { publish(read()) }
            catch (_: Exception) { _storageError.value = "تعذر قراءة البيانات المحفوظة. لم يتم حذفها أو استبدالها." }
        }
    }

    private fun read(db: android.database.sqlite.SQLiteDatabase = database.readableDatabase): InvoiceStore {
        val stored = db.rawQuery("SELECT payload FROM invoice_store WHERE id = 1", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        // Preserve any snapshot written by earlier development builds. Never delete the original.
        val raw = stored ?: legacyPrefs.getString("snapshot", null) ?: return InvoiceStore()
        return requireNotNull(adapter.fromJson(raw)) { "تعذر قراءة البيانات المحفوظة" }.also {
            require(it.version == 1) { "إصدار البيانات غير مدعوم" }
        }
    }

    private fun publish(data: InvoiceStore) {
        val invoices = data.invoices.map { it.copy(status = it.effectiveStatus()) }
        _companyProfile.value = data.company
        _catalog.value = data.catalog
        _invoices.value = invoices
        _quotes.value = data.quotes
        _reminderOptions.value = data.reminderOptions
        _clients.value = data.clients.map { client ->
            val related = invoices.filter { it.clientId == client.id && it.status != InvoiceStatus.DRAFT }
            client.copy(
                totalBilled = moneySum(related.map { it.grandTotal }),
                outstandingBalance = moneySum(related.filter { it.status != InvoiceStatus.PAID }.map { it.grandTotal }),
                invoicesCount = related.size
            )
        }
    }

    private fun update(transform: (InvoiceStore) -> InvoiceStore) = synchronized(storageLock) {
        check(_storageError.value == null) { _storageError.value ?: "التخزين غير متاح" }
        val db = database.writableDatabase
        var next: InvoiceStore? = null
        db.beginTransaction()
        try {
            val updated = transform(read(db))
            val values = android.content.ContentValues().apply {
                put("id", 1)
                put("payload", adapter.toJson(updated))
            }
            db.insertWithOnConflict("invoice_store", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                .also { check(it != -1L) { "تعذر الحفظ المحلي؛ لم تُؤكد العملية" } }
            db.setTransactionSuccessful()
            next = updated
        } finally {
            db.endTransaction()
        }
        // Publish only after SQLite has successfully committed its transaction.
        publish(requireNotNull(next))
    }

    fun refreshDates() = synchronized(storageLock) {
        if (_storageError.value == null) {
            try { publish(read()) }
            catch (_: Exception) { _storageError.value = "تعذر قراءة البيانات المحفوظة. لم يتم حذفها أو استبدالها." }
        }
    }

    private fun validateSeller(company: CompanyProfile) {
        require(company.companyName.isNotBlank() &&
            company.companyName.toByteArray(Charsets.UTF_8).size <= 255 &&
            (company.vatNumber.isBlank() || Regex("3[0-9]{13}3").matches(company.vatNumber))) {
            "راجع اسم المنشأة والرقم الضريبي إن أدخلته"
        }
    }

    override fun close() { database.close() }

    /** Reserve persistently; deleting invoices never reuses a reserved number. */
    fun generateNextInvoiceNumber(): String {
        var result = ""
        update { data ->
            val year = InvoiceDates.today().take(4)
            val prefix = "INV-$year-"
            val maximum = data.invoices.mapNotNull { it.invoiceNumber.takeIf { n -> n.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull() }.maxOrNull() ?: 0
            val next = maxOf(data.nextNumbers[year] ?: 1, maximum + 1)
            require(next < Int.MAX_VALUE)
            result = prefix + next.toString().padStart(3, '0')
            data.copy(nextNumbers = data.nextNumbers + (year to next + 1))
        }
        return result
    }

    fun addInvoice(invoice: Invoice): Invoice {
        var saved = invoice
        update { data ->
            require(data.invoices.none { it.id == invoice.id || it.invoiceNumber == invoice.invoiceNumber }) { "رقم الفاتورة مستخدم بالفعل" }
            val existing = data.clients.find { it.id == invoice.clientId }
                ?: data.clients.find { it.name == invoice.clientName && it.company == invoice.clientCompany && it.phone == invoice.clientPhone }
            val client = existing ?: Client(UUID.randomUUID().toString(), invoice.clientName, invoice.clientCompany,
                invoice.clientPhone, invoice.clientEmail, invoice.clientVatNumber, invoice.clientAddress)
            val seller = if (invoice.status == InvoiceStatus.DRAFT) null else invoice.seller ?: data.company
            if (seller != null) validateSeller(seller)
            saved = invoice.copy(
                clientId = client.id,
                seller = seller,
                issuedAt = if (invoice.status == InvoiceStatus.DRAFT) "" else invoice.issuedAt.ifBlank { InvoiceDates.utcNow() }
            )
            data.copy(
                invoices = listOf(saved) + data.invoices,
                clients = if (existing == null) listOf(client) + data.clients else data.clients
            )
        }
        return saved
    }

    fun updateInvoiceStatus(invoiceId: String, newStatus: InvoiceStatus) {
        update { data -> data.copy(invoices = data.invoices.map { invoice ->
            if (invoice.id != invoiceId) invoice else {
                val issuing = invoice.issuedAt.isBlank() && newStatus != InvoiceStatus.DRAFT
                if (issuing) validateSeller(data.company)
                invoice.copy(
                    status = newStatus,
                    issuedAt = if (issuing) InvoiceDates.utcNow() else invoice.issuedAt,
                    seller = if (issuing) data.company else invoice.seller,
                    bankName = if (issuing) data.company.bankName else invoice.bankName,
                    iban = if (issuing) data.company.iban else invoice.iban
                )
            }
        }) }
    }

    fun deleteInvoice(invoiceId: String) {
        update { it.copy(invoices = it.invoices.filterNot { invoice -> invoice.id == invoiceId }) }
    }

    fun duplicateInvoice(invoiceId: String): Invoice? {
        val source = _invoices.value.find { it.id == invoiceId } ?: return null
        return addInvoice(source.copy(
            id = UUID.randomUUID().toString(), invoiceNumber = generateNextInvoiceNumber(),
            issueDate = InvoiceDates.today(), dueDate = InvoiceDates.plusDays(InvoiceDates.today(), 14),
            status = InvoiceStatus.DRAFT, issuedAt = "", seller = null,
            bankName = companyProfile.value.bankName, iban = companyProfile.value.iban
        ))
    }


    /** Separate durable numbering, so offers never consume invoice numbers or reuse deleted ones. */
    fun generateNextQuoteNumber(): String {
        var result = ""
        update { data ->
            val year = InvoiceDates.today().take(4)
            val prefix = "QTE-$year-"
            val maximum = data.quotes.mapNotNull {
                it.number.takeIf { number -> number.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull()
            }.maxOrNull() ?: 0
            val next = maxOf(data.nextNumbers[prefix] ?: 1, maximum + 1)
            require(next < Int.MAX_VALUE) { "تعذر حجز رقم عرض جديد" }
            result = prefix + next.toString().padStart(3, '0')
            data.copy(nextNumbers = data.nextNumbers + (prefix to next + 1))
        }
        return result
    }

    fun saveQuote(document: Invoice): Quote {
        require(document.status == InvoiceStatus.DRAFT) { "حالة عرض السعر غير صالحة" }
        var saved: Quote? = null
        update { data ->
            val old = data.quotes.find { it.id == document.id }
            require(old?.convertedInvoiceId == null) { "تم تحويل هذا العرض لفاتورة؛ لا يمكن تعديل العرض بعد التحويل" }
            require(data.quotes.none { it.id != document.id && it.number == document.invoiceNumber }) { "رقم عرض السعر مستخدم بالفعل" }
            val seller = document.seller ?: data.company
            validateSeller(seller)
            val existing = data.clients.find { it.id == document.clientId }
                ?: data.clients.find { it.name == document.clientName && it.company == document.clientCompany && it.phone == document.clientPhone }
            val client = existing ?: Client(UUID.randomUUID().toString(), document.clientName, document.clientCompany,
                document.clientPhone, document.clientEmail, document.clientVatNumber, document.clientAddress)
            val normalized = document.copy(clientId = client.id, seller = seller,
                issuedAt = old?.document?.issuedAt ?: document.issuedAt.ifBlank { InvoiceDates.utcNow() })
            saved = Quote(normalized)
            data.copy(quotes = listOf(requireNotNull(saved)) + data.quotes.filterNot { it.id == document.id },
                clients = if (existing == null) listOf(client) + data.clients else data.clients)
        }
        return requireNotNull(saved)
    }

    fun deleteQuote(quoteId: String) {
        update { data -> data.copy(quotes = data.quotes.filterNot { it.id == quoteId }) }
    }

    /** Reserve the invoice number, create it and link the offer in one SQLite transaction. */
    fun convertQuoteToInvoice(quoteId: String): Invoice {
        var result: Invoice? = null
        update { data ->
            val quote = requireNotNull(data.quotes.find { it.id == quoteId }) { "عرض السعر غير موجود" }
            val linked = quote.convertedInvoiceId
            if (linked != null) {
                result = requireNotNull(data.invoices.find { it.id == linked }) {
                    "تم حذف الفاتورة المرتبطة؛ لن يتم إنشاء فاتورة أخرى تلقائيًا"
                }
                data
            } else {
                require(!quote.isExpired()) { "انتهت صلاحية العرض؛ حدّث تاريخ الصلاحية قبل التحويل" }
                val seller = quote.document.seller ?: data.company
                validateSeller(seller)
                val year = InvoiceDates.today().take(4)
                val prefix = "INV-$year-"
                val maximum = data.invoices.mapNotNull {
                    it.invoiceNumber.takeIf { number -> number.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull()
                }.maxOrNull() ?: 0
                val next = maxOf(data.nextNumbers[year] ?: 1, maximum + 1)
                require(next < Int.MAX_VALUE) { "تعذر حجز رقم فاتورة جديد" }
                val document = quote.document
                val existing = data.clients.find { it.id == document.clientId }
                    ?: data.clients.find { it.name == document.clientName && it.company == document.clientCompany && it.phone == document.clientPhone }
                val client = existing ?: Client(UUID.randomUUID().toString(), document.clientName, document.clientCompany,
                    document.clientPhone, document.clientEmail, document.clientVatNumber, document.clientAddress)
                val invoice = document.copy(id = UUID.randomUUID().toString(),
                    invoiceNumber = prefix + next.toString().padStart(3, '0'),
                    issueDate = InvoiceDates.today(), dueDate = InvoiceDates.plusDays(InvoiceDates.today(), 14),
                    status = InvoiceStatus.PENDING, issuedAt = InvoiceDates.utcNow(), seller = seller, clientId = client.id)
                result = invoice
                data.copy(invoices = listOf(invoice) + data.invoices,
                    quotes = data.quotes.map { if (it.id == quoteId) it.copy(convertedInvoiceId = invoice.id) else it },
                    nextNumbers = data.nextNumbers + (year to next + 1),
                    clients = if (existing == null) listOf(client) + data.clients else data.clients)
            }
        }
        return requireNotNull(result)
    }

    fun addClient(client: Client) {
        require(client.name.isNotBlank()) { "اسم العميل مطلوب" }
        update { data ->
            require(data.clients.none { it.id == client.id })
            data.copy(clients = listOf(client) + data.clients)
        }
    }

    fun addCatalogProduct(product: CatalogProduct) {
        require(product.defaultPrice.isFinite() && product.defaultPrice >= 0 && java.math.BigDecimal.valueOf(product.defaultPrice) <= MAX_INVOICE_AMOUNT)
        update { it.copy(catalog = listOf(product) + it.catalog) }
    }

    fun updateCompanyProfile(profile: CompanyProfile) {
        require(profile.companyName.isNotBlank() && profile.companyName.toByteArray(Charsets.UTF_8).size <= 255) { "اسم المنشأة مطلوب ويجب ألا يتجاوز 255 بايت" }
        require(profile.vatNumber.isBlank() || Regex("3[0-9]{13}3").matches(profile.vatNumber)) { "راجع الرقم الضريبي للمنشأة" }
        require(profile.logoFileName == null || com.example.branding.CompanyLogoStore.isValidFileName(profile.logoFileName)) { "ملف الشعار غير صالح" }
        require(profile.defaultTaxRate.isFinite() && profile.defaultTaxRate in 0.0..1.0)
        update { it.copy(company = profile) }
    }

    fun updateReminderOptions(options: ReminderOptions) { update { it.copy(reminderOptions = options) } }

    companion object { private val storageLock = Any() }
}

private class InvoiceDatabase(context: Context) : android.database.sqlite.SQLiteOpenHelper(
    context, "fawraty_records.db", null, 1,
    // Android's default corruption handler deletes the file. Preserve it for recovery instead.
    android.database.DatabaseErrorHandler { throw IllegalStateException("قاعدة البيانات غير قابلة للقراءة؛ تم الاحتفاظ بالملف للاستعادة") }
) {
    override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
        db.execSQL("CREATE TABLE invoice_store (id INTEGER PRIMARY KEY CHECK(id = 1), payload TEXT NOT NULL)")
    }
    override fun onUpgrade(db: android.database.sqlite.SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("يلزم ترحيل صريح للبيانات؛ لم يتم حذف البيانات الحالية")
    }
}

private fun defaultCatalog(): List<CatalogProduct> {
    return listOf(
            CatalogProduct(
                id = "CAT-01",
                name = "واجهات زجاجية مزدوجة معزولة حرارياً Low-E",
                nameEn = "Structural Double Glazed Low-E Facades",
                category = "واجهات معمارية",
                description = "زجاج سيكوريت مزدوج فائق العزل للحرارة والصوت مع طبقات الفضة المنخفضة الانبعاثية ومقاومة الرياح للأبراج والمباني الذكية.",
                defaultPrice = 450.0,
                unit = "متر مربع",
                specifications = "سماكة: 28 ملم (6+16+6)، عزل حراري U-Value 1.4، مقاوم للصدمات",
                warranty = "10 سنوات ضمان معتمد من كود البناء السعودي"
            ),
            CatalogProduct(
                id = "CAT-02",
                name = "زجاج مقسى فائق الشفافية Ultra-Clear Tempered",
                nameEn = "Ultra-Clear Starphire Tempered Glass 12mm",
                category = "زجاج مقسى",
                description = "زجاج نقي منخفض الحديد بنسبة شفافية تفوق 91% خالي من الظلال الخضراء مثالي للواجهات الفاخرة والمعارض.",
                defaultPrice = 280.0,
                unit = "متر مربع",
                specifications = "سماكة: 12 ملم، معالجة حرارية كاملة Fully Tempered، حواف مصقولة CNC",
                warranty = "5 سنوات ضمان كسر عفوي ومقاومة تشوهات"
            ),
            CatalogProduct(
                id = "CAT-03",
                name = "زجاج ذكي قابل للتبديل للخصوصية Smart Glass",
                nameEn = "Switchable Smart PDLC Privacy Glass",
                category = "تقنية ذكية",
                description = "يتحول بين الشفافية والعتامة بضغطة زر أو تحكم ذكي عبر الهاتف أو أنظمة أتمتة المباني الحديثة.",
                defaultPrice = 1200.0,
                unit = "متر مربع",
                specifications = "تقنية PDLC، استهلاك كهربائي منخفض 5 واط/م²، استجابة فورية 0.1 ثانية",
                warranty = "3 سنوات ضمان نظام التعتيم الإلكتروني"
            ),
            CatalogProduct(
                id = "CAT-04",
                name = "قواطع مكاتب زجاجية بدون إطار Frameless",
                nameEn = "Frameless Acoustic Glass Partitions",
                category = "قواطع مكاتب",
                description = "نظام قواطع زجاجية متكامل مع إكسسوارات ستانلس ستيل 316 وعوازل صوتية مدمجة للمقرات التنفيذية.",
                defaultPrice = 360.0,
                unit = "متر مربع",
                specifications = "سماكة 10-12 ملم، عزل صوتي 38dB، إكسسوارات ألمانية فاخرة",
                warranty = "7 سنوات على الإكسسوارات والتركيب"
            ),
            CatalogProduct(
                id = "CAT-05",
                name = "درابزين زجاجي إستركشر للأبراج والفلل",
                nameEn = "Architectural Glass Balustrades & Railings",
                category = "درابزين وسلالم",
                description = "نظام درابزين بدون أعمدة يعتمد على قطاعات الألمنيوم الغاطسة وزجاج مصفح آمن Laminated Glass.",
                defaultPrice = 580.0,
                unit = "متر طولي",
                specifications = "سماكة: 8+1.52+8 ملم مصفح، قدرة تحمل حمل صدمات 1.5 kN",
                warranty = "10 سنوات ضمان سلامة إنشائية"
            ),
            CatalogProduct(
                id = "CAT-06",
                name = "مرايا معمارية فاخرة عسلي ورمادي بإضاءة LED",
                nameEn = "Luxury Architectural Tinted Mirrors",
                category = "مرايا فاخرة",
                description = "مرايا بلجيكية أصلية خالية من النحاس والرصاص مع شريط إضاءة محيطي دافئ 3000K وزجاج عاكس برونزي أو رمادي.",
                defaultPrice = 340.0,
                unit = "متر مربع",
                specifications = "سماكة 6 ملم، حواف مشطوفة Beveled Edge، شريط LED مقاوم للرطوبة IP65",
                warranty = "5 سنوات ضد الرطوبة وتغير اللون"
            ),
            CatalogProduct(
                id = "CAT-07",
                name = "كبائن شاور زجاجية مقاومة للتكلس Minimalist",
                nameEn = "Premium Frameless Shower Enclosures",
                category = "شاور وحمامات",
                description = "شاور زجاجي سيكوريت مع طبقة نانو سيراميك طاردة للماء والترسبات الكلسية مع مفصلات ذهبية غير قابلة للصدأ.",
                defaultPrice = 1850.0,
                unit = "قطعة",
                specifications = "سماكة 10 ملم مقسى، طلاء Nano Easy-Clean، إكسسوارات PVD ذهبية",
                warranty = "5 سنوات ضد التسريب وصدأ الإكسسوارات"
            ),
            CatalogProduct(
                id = "CAT-08",
                name = "قبب وسماوي زجاجية للمشاريع الكبرى Skylights",
                nameEn = "Structural Architectural Glass Skylights",
                category = "قبب وسماوي",
                description = "حلول إنارة طبيعية ذات أداء إنشائي عالي وزجاج مزدوج مع طبقات حماية من الأشعة فوق البنفسجية UV.",
                defaultPrice = 780.0,
                unit = "متر مربع",
                specifications = "هيكل ألمنيوم حراري ثيرمال بريك، زجاج أمان مقاوم للبَرَد والعواصف",
                warranty = "10 سنوات عزل مائي وحراري كامل"
            )
        )
}
