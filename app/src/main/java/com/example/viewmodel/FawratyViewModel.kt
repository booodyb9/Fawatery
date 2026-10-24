package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CatalogProduct
import com.example.model.Client
import com.example.model.CompanyProfile
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.repository.FawratyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import com.example.model.*
import com.example.reminders.ReminderOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class NavigationTab(val titleAr: String, val titleEn: String) {
    DASHBOARD("الرئيسية", "Dashboard"),
    INVOICES("الفواتير", "Invoices"),
    QUOTES("عروض الأسعار", "Quotes"),
    CLIENTS("العملاء", "Clients"),
    CATALOG("الكتالوج", "Catalog"),
    REPORTS("التقارير", "Reports"),
    SETTINGS("المنشأة", "Profile")
}

enum class SubScreen {
    NONE,
    CREATE_INVOICE,
    INVOICE_DETAIL,
    CREATE_QUOTE,
    QUOTE_DETAIL
}

data class FinancialStats(
    val totalInvoiced: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalVat: Double = 0.0,
    val totalCount: Int = 0,
    val paidCount: Int = 0,
    val pendingCount: Int = 0,
    val overdueCount: Int = 0,
    val draftCount: Int = 0
)

class FawratyViewModel(
    private val repository: FawratyRepository = FawratyRepository()
) : ViewModel() {

    val companyProfile: StateFlow<CompanyProfile> = repository.companyProfile
    val clients: StateFlow<List<Client>> = repository.clients
    val catalog: StateFlow<List<CatalogProduct>> = repository.catalog
    val allInvoices: StateFlow<List<Invoice>> = repository.invoices
    val quotes = repository.quotes
    private val _selectedQuote = MutableStateFlow<Quote?>(null)
    val selectedQuote = _selectedQuote.asStateFlow()
    private val _editingQuote = MutableStateFlow<Quote?>(null)
    val editingQuote = _editingQuote.asStateFlow()

    private val _selectedTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    private val _activeSubScreen = MutableStateFlow(SubScreen.NONE)
    val activeSubScreen: StateFlow<SubScreen> = _activeSubScreen.asStateFlow()

    private val _selectedInvoice = MutableStateFlow<Invoice?>(null)
    val selectedInvoice: StateFlow<Invoice?> = _selectedInvoice.asStateFlow()

    private val _statusFilter = MutableStateFlow<InvoiceStatus?>(null)
    val statusFilter: StateFlow<InvoiceStatus?> = _statusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _prefillProduct = MutableStateFlow<CatalogProduct?>(null)
    val prefillProduct: StateFlow<CatalogProduct?> = _prefillProduct.asStateFlow()

    val reminderOptions = repository.reminderOptions
    val storageError = repository.storageError
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()
    fun clearError() { _lastError.value = null }
    override fun onCleared() { repository.close(); super.onCleared() }
    private fun change(action: () -> Unit): Boolean = try {
        action()
        true
    } catch (error: Exception) {
        _lastError.value = error.message ?: "تعذر حفظ التغيير"
        false
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                repository.refreshDates()
            }
        }
    }

    val stats: StateFlow<FinancialStats> = allInvoices.combine(companyProfile) { invoices, _ ->
        calculateFinancialStats(invoices)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialStats())

    val filteredInvoices: StateFlow<List<Invoice>> = combine(
        allInvoices,
        statusFilter,
        searchQuery
    ) { invoices, filter, query ->
        invoices.filter { invoice ->
            val matchesFilter = filter == null || invoice.status == filter
            val matchesQuery = query.isBlank() ||
                invoice.invoiceNumber.contains(query, ignoreCase = true) ||
                invoice.clientName.contains(query, ignoreCase = true) ||
                invoice.clientCompany.contains(query, ignoreCase = true) ||
                invoice.projectCategory.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: NavigationTab) {
        _selectedTab.value = tab
        _activeSubScreen.value = SubScreen.NONE
    }

    fun setStatusFilter(status: InvoiceStatus?) {
        _statusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openInvoiceDetail(invoice: Invoice) {
        _selectedInvoice.value = invoice
        _activeSubScreen.value = SubScreen.INVOICE_DETAIL
    }

    fun openCreateInvoice(product: CatalogProduct? = null) {
        _editingQuote.value = null
        _prefillProduct.value = product
        _activeSubScreen.value = SubScreen.CREATE_INVOICE
    }

    fun navigateBack(): Boolean {
        if (_activeSubScreen.value != SubScreen.NONE) {
            _activeSubScreen.value = SubScreen.NONE
            _selectedInvoice.value = null
            _prefillProduct.value = null
            _editingQuote.value = null
            _selectedQuote.value = null
            return true
        }
        if (_selectedTab.value != NavigationTab.DASHBOARD) {
            _selectedTab.value = NavigationTab.DASHBOARD
            return true
        }
        return false
    }

    fun createInvoice(
        clientName: String,
        clientCompany: String,
        clientPhone: String,
        clientEmail: String,
        clientVatNumber: String,
        clientAddress: String,
        issueDate: String,
        dueDate: String,
        projectCategory: String,
        items: List<InvoiceItem>,
        notes: String,
        paymentTerms: String,
        status: InvoiceStatus = InvoiceStatus.PENDING,
        clientId: String? = null,
        logoFileName: String? = companyProfile.value.logoFileName
    ): Invoice {
        val nextNumber = repository.generateNextInvoiceNumber()
        val invoice = Invoice(
            id = UUID.randomUUID().toString(),
            invoiceNumber = nextNumber,
            clientName = clientName,
            clientCompany = clientCompany,
            clientPhone = clientPhone,
            clientEmail = clientEmail,
            clientVatNumber = clientVatNumber,
            clientAddress = clientAddress,
            issueDate = issueDate,
            dueDate = dueDate,
            projectCategory = projectCategory,
            items = items,
            taxRate = companyProfile.value.defaultTaxRate,
            status = status,
            notes = notes,
            paymentTerms = paymentTerms,
            bankName = companyProfile.value.bankName,
            iban = companyProfile.value.iban,
            issuedAt = if (status == InvoiceStatus.DRAFT) "" else InvoiceDates.utcNow(),
            seller = if (status == InvoiceStatus.DRAFT) null else companyProfile.value.copy(logoFileName = logoFileName),
            clientId = clientId
        )
        val saved = repository.addInvoice(invoice)
        _selectedInvoice.value = saved
        _activeSubScreen.value = SubScreen.INVOICE_DETAIL
        return saved
    }


    fun openQuoteDetail(quote: Quote) {
        _selectedQuote.value = quote
        _activeSubScreen.value = SubScreen.QUOTE_DETAIL
    }

    fun openCreateQuote(quote: Quote? = null) {
        _selectedTab.value = NavigationTab.QUOTES
        _editingQuote.value = quote
        _prefillProduct.value = null
        _activeSubScreen.value = SubScreen.CREATE_QUOTE
    }

    fun createQuote(
        clientName: String, clientCompany: String, clientPhone: String, clientEmail: String,
        clientVatNumber: String, clientAddress: String, issueDate: String, dueDate: String,
        projectCategory: String, items: List<InvoiceItem>, notes: String, paymentTerms: String,
        clientId: String? = null, logoFileName: String? = companyProfile.value.logoFileName
    ): Quote {
        val editing = _editingQuote.value
        val seller = (editing?.document?.seller ?: companyProfile.value).copy(logoFileName = logoFileName)
        val document = Invoice(
            id = editing?.id ?: UUID.randomUUID().toString(),
            invoiceNumber = editing?.number ?: repository.generateNextQuoteNumber(),
            clientName = clientName, clientCompany = clientCompany, clientPhone = clientPhone,
            clientEmail = clientEmail, clientVatNumber = clientVatNumber, clientAddress = clientAddress,
            issueDate = issueDate, dueDate = dueDate, projectCategory = projectCategory, items = items,
            taxRate = editing?.document?.taxRate ?: companyProfile.value.defaultTaxRate,
            status = InvoiceStatus.DRAFT, notes = notes, paymentTerms = paymentTerms,
            bankName = editing?.document?.bankName ?: seller.bankName,
            iban = editing?.document?.iban ?: seller.iban, issuedAt = editing?.document?.issuedAt ?: InvoiceDates.utcNow(),
            seller = seller, clientId = clientId
        )
        val saved = repository.saveQuote(document)
        _editingQuote.value = null
        openQuoteDetail(saved)
        return saved
    }

    fun deleteQuote(quoteId: String): Boolean = change {
        repository.deleteQuote(quoteId)
        _selectedQuote.value = null
        _activeSubScreen.value = SubScreen.NONE
    }

    fun convertQuote(quoteId: String): Boolean = change {
        val invoice = repository.convertQuoteToInvoice(quoteId)
        _selectedQuote.value = repository.quotes.value.find { it.id == quoteId }
        openInvoiceDetail(invoice)
    }

    fun openQuoteInvoice(quote: Quote): Boolean = change {
        val invoice = requireNotNull(allInvoices.value.find { it.id == quote.convertedInvoiceId }) {
            "الفاتورة المرتبطة غير موجودة"
        }
        openInvoiceDetail(invoice)
    }

    fun updateInvoiceStatus(invoiceId: String, status: InvoiceStatus): Boolean = change {
        repository.updateInvoiceStatus(invoiceId, status)
        if (_selectedInvoice.value?.id == invoiceId) {
            _selectedInvoice.value = repository.invoices.value.find { it.id == invoiceId }
        }
    }

    fun duplicateInvoice(invoiceId: String): Boolean = change {
        val dup = repository.duplicateInvoice(invoiceId)
        if (dup != null) {
            _selectedInvoice.value = dup
            _activeSubScreen.value = SubScreen.INVOICE_DETAIL
        }
    }

    fun deleteInvoice(invoiceId: String): Boolean = change {
        repository.deleteInvoice(invoiceId)
        if (_selectedInvoice.value?.id == invoiceId) {
            _selectedInvoice.value = null
            _activeSubScreen.value = SubScreen.NONE
        }
    }

    fun addNewClient(
        name: String,
        company: String,
        phone: String,
        email: String,
        vatNumber: String,
        address: String
    ): Boolean = change {
        require(name.isNotBlank()) { "اسم العميل مطلوب" }
        require(vatNumber.isBlank() || Regex("3[0-9]{13}3").matches(vatNumber)) { "الرقم الضريبي للعميل غير صالح" }
        val client = Client(
            id = UUID.randomUUID().toString(),
            name = name,
            company = company,
            phone = phone,
            email = email,
            vatNumber = vatNumber,
            address = address
        )
        repository.addClient(client)
    }

    fun updateReminderOptions(options: ReminderOptions): Boolean = change { repository.updateReminderOptions(options) }

    fun updateCompanyProfile(profile: CompanyProfile): Boolean = change {
        repository.updateCompanyProfile(profile)
    }
}

fun calculateFinancialStats(invoices: List<Invoice>): FinancialStats {
    val finalInvoices = invoices.filter { it.status != InvoiceStatus.DRAFT }
    return FinancialStats(
        totalInvoiced = moneySum(finalInvoices.map { it.grandTotal }),
        totalPaid = moneySum(finalInvoices.filter { it.status == InvoiceStatus.PAID }.map { it.grandTotal }),
        totalPending = moneySum(finalInvoices.filter { it.status != InvoiceStatus.PAID }.map { it.grandTotal }),
        totalVat = moneySum(finalInvoices.map { it.vatAmount }),
        totalCount = invoices.size,
        paidCount = finalInvoices.count { it.status == InvoiceStatus.PAID },
        pendingCount = finalInvoices.count { it.status == InvoiceStatus.PENDING },
        overdueCount = finalInvoices.count { it.status == InvoiceStatus.OVERDUE },
        draftCount = invoices.count { it.status == InvoiceStatus.DRAFT }
    )
}
