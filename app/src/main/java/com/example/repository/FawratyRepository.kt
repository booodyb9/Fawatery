package com.example.repository

import com.example.model.CatalogProduct
import com.example.model.Client
import com.example.model.CompanyProfile
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FawratyRepository {

    private val _companyProfile = MutableStateFlow(CompanyProfile())
    val companyProfile: StateFlow<CompanyProfile> = _companyProfile.asStateFlow()

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()

    private val _catalog = MutableStateFlow<List<CatalogProduct>>(emptyList())
    val catalog: StateFlow<List<CatalogProduct>> = _catalog.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    init {
        initSampleData()
    }

    private fun initSampleData() {
        val sampleClients = listOf(
            Client(
                id = "C-101",
                name = "م. سلطان القحطاني",
                company = "مجموعة الرواد للمقاولات والتطوير",
                phone = "+966 50 123 4567",
                email = "sultan@alrowad-sa.com",
                vatNumber = "300456789100003",
                address = "الرياض - حي الملقا، طريق أنس بن مالك",
                totalBilled = 384500.0,
                outstandingBalance = 0.0,
                invoicesCount = 3
            ),
            Client(
                id = "C-102",
                name = "عبد العزيز بن فهد آل سعود",
                company = "مكتب العمارة النجدية الحديثة",
                phone = "+966 55 987 6543",
                email = "abdulaziz@najd-arch.com",
                vatNumber = "300987654300003",
                address = "الرياض - حي حطين، مشروع الفلل الفاخرة",
                totalBilled = 145000.0,
                outstandingBalance = 52500.0,
                invoicesCount = 2
            ),
            Client(
                id = "C-103",
                name = "د. نورة الشمري",
                company = "شركة آفاق العقارية للمجمعات التجارية",
                phone = "+966 54 321 8899",
                email = "noura@afaq-realestate.sa",
                vatNumber = "310555666700003",
                address = "الرياض - حي العليا، طريق الملك عبد العزيز",
                totalBilled = 89000.0,
                outstandingBalance = 89000.0,
                invoicesCount = 1
            ),
            Client(
                id = "C-104",
                name = "م. خالد التميمي",
                company = "استشارات الديكور المعماري الداخلي",
                phone = "+966 56 777 4433",
                email = "khaled@tamimi-design.com",
                vatNumber = "300222333400003",
                address = "الرياض - حي النخيل الغربي",
                totalBilled = 34500.0,
                outstandingBalance = 0.0,
                invoicesCount = 2
            )
        )
        _clients.value = sampleClients

        val sampleCatalog = listOf(
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
        _catalog.value = sampleCatalog

        val sampleInvoices = listOf(
            Invoice(
                id = "INV-2026-001",
                invoiceNumber = "INV-2026-001",
                clientName = "م. سلطان القحطاني",
                clientCompany = "مجموعة الرواد للمقاولات والتطوير",
                clientPhone = "+966 50 123 4567",
                clientEmail = "sultan@alrowad-sa.com",
                clientVatNumber = "300456789100003",
                clientAddress = "الرياض - طريق أنس بن مالك",
                issueDate = "2026-10-10",
                dueDate = "2026-10-24",
                projectCategory = "واجهات أبراج ومباني تجارية",
                items = listOf(
                    InvoiceItem(
                        id = "ITEM-1",
                        description = "واجهات زجاجية مزدوجة معزولة حرارياً Low-E (برج الرواد التجاري)",
                        quantity = 350.0,
                        unitPrice = 450.0,
                        unit = "متر مربع",
                        discount = 5000.0
                    ),
                    InvoiceItem(
                        id = "ITEM-2",
                        description = "درابزين زجاجي إستركشر للشرفات البانورامية",
                        quantity = 60.0,
                        unitPrice = 580.0,
                        unit = "متر طولي",
                        discount = 0.0
                    )
                ),
                status = InvoiceStatus.PAID,
                notes = "تم اعتماد المواصفات الهندسية من الاستشاري الهندسي. شامل التوريد والتركيب والرافعات الهيدروليكية."
            ),
            Invoice(
                id = "INV-2026-002",
                invoiceNumber = "INV-2026-002",
                clientName = "عبد العزيز بن فهد آل سعود",
                clientCompany = "مكتب العمارة النجدية الحديثة",
                clientPhone = "+966 55 987 6543",
                clientEmail = "abdulaziz@najd-arch.com",
                clientVatNumber = "300987654300003",
                clientAddress = "الرياض - حي حطين",
                issueDate = "2026-10-12",
                dueDate = "2026-10-26",
                projectCategory = "فلل سكنية فاخرة",
                items = listOf(
                    InvoiceItem(
                        id = "ITEM-3",
                        description = "زجاج ذكي قابل للتبديل Smart Glass لغرف الاجتماعات والصالة الرئيسية",
                        quantity = 28.0,
                        unitPrice = 1200.0,
                        unit = "متر مربع",
                        discount = 1100.0
                    ),
                    InvoiceItem(
                        id = "ITEM-4",
                        description = "مرايا ديكورية معمارية برونزية مع شريط LED مخفي",
                        quantity = 40.0,
                        unitPrice = 340.0,
                        unit = "متر مربع",
                        discount = 0.0
                    )
                ),
                status = InvoiceStatus.PENDING,
                notes = "الدفعة المستحقة هي الدفعة الثانية لتوريد الزجاج الذكي والمرايا بعد الانتهاء من المقاسات الليزرية."
            ),
            Invoice(
                id = "INV-2026-003",
                invoiceNumber = "INV-2026-003",
                clientName = "د. نورة الشمري",
                clientCompany = "شركة آفاق العقارية للمجمعات التجارية",
                clientPhone = "+966 54 321 8899",
                clientEmail = "noura@afaq-realestate.sa",
                clientVatNumber = "310555666700003",
                clientAddress = "الرياض - حي العليا",
                issueDate = "2026-09-28",
                dueDate = "2026-10-05",
                projectCategory = "مجمعات تجارية ومطاعم",
                items = listOf(
                    InvoiceItem(
                        id = "ITEM-5",
                        description = "قواطع مكاتب زجاجية بدون إطار لمقر الإدارة العامة",
                        quantity = 140.0,
                        unitPrice = 360.0,
                        unit = "متر مربع",
                        discount = 2000.0
                    ),
                    InvoiceItem(
                        id = "ITEM-6",
                        description = "أبواب زجاجية سيكوريت مع أجهزة غلق هيدروليكية ألمانية",
                        quantity = 8.0,
                        unitPrice = 2400.0,
                        unit = "قطعة",
                        discount = 0.0
                    )
                ),
                status = InvoiceStatus.OVERDUE,
                notes = "يرجى تسوية المبلغ المستحق في أقرب وقت لتفادي تأخير جدول التوريد النهائي."
            ),
            Invoice(
                id = "INV-2026-004",
                invoiceNumber = "INV-2026-004",
                clientName = "م. خالد التميمي",
                clientCompany = "استشارات الديكور المعماري الداخلي",
                clientPhone = "+966 56 777 4433",
                clientEmail = "khaled@tamimi-design.com",
                clientVatNumber = "300222333400003",
                clientAddress = "الرياض - حي النخيل الغربي",
                issueDate = "2026-10-14",
                dueDate = "2026-10-28",
                projectCategory = "ديكورات داخلية خاصة",
                items = listOf(
                    InvoiceItem(
                        id = "ITEM-7",
                        description = "كبائن شاور زجاجية مقاومة للتكلس مع إكسسوارات ذهبية مطفية",
                        quantity = 6.0,
                        unitPrice = 1850.0,
                        unit = "قطعة",
                        discount = 600.0
                    )
                ),
                status = InvoiceStatus.DRAFT,
                notes = "مسودة عرض سعر مبدئية قيد المراجعة الفنية مع المكتب الاستشاري."
            )
        )
        _invoices.value = sampleInvoices
    }

    fun addInvoice(invoice: Invoice) {
        val list = _invoices.value.toMutableList()
        list.add(0, invoice)
        _invoices.value = list
    }

    fun updateInvoiceStatus(invoiceId: String, newStatus: InvoiceStatus) {
        _invoices.value = _invoices.value.map {
            if (it.id == invoiceId) it.copy(status = newStatus) else it
        }
    }

    fun deleteInvoice(invoiceId: String) {
        _invoices.value = _invoices.value.filter { it.id != invoiceId }
    }

    fun duplicateInvoice(invoiceId: String): Invoice? {
        val source = _invoices.value.find { it.id == invoiceId } ?: return null
        val nextNum = "INV-2026-${(_invoices.value.size + 1).toString().padStart(3, '0')}"
        val dup = source.copy(
            id = UUID.randomUUID().toString(),
            invoiceNumber = nextNum,
            issueDate = "2026-10-14",
            dueDate = "2026-10-28",
            status = InvoiceStatus.DRAFT
        )
        addInvoice(dup)
        return dup
    }

    fun addClient(client: Client) {
        val list = _clients.value.toMutableList()
        list.add(0, client)
        _clients.value = list
    }

    fun addCatalogProduct(product: CatalogProduct) {
        val list = _catalog.value.toMutableList()
        list.add(0, product)
        _catalog.value = list
    }

    fun updateCompanyProfile(profile: CompanyProfile) {
        _companyProfile.value = profile
    }

    fun generateNextInvoiceNumber(): String {
        val count = _invoices.value.size + 1
        return "INV-2026-${count.toString().padStart(3, '0')}"
    }
}
