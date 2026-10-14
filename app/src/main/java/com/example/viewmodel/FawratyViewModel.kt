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

enum class NavigationTab(val titleAr: String, val titleEn: String) {
    DASHBOARD("الرئيسية", "Dashboard"),
    INVOICES("الفواتير", "Invoices"),
    CLIENTS("العملاء", "Clients"),
    CATALOG("الكتالوج", "Catalog"),
    REPORTS("التقارير", "Reports"),
    SETTINGS("المنشأة", "Profile")
}

enum class SubScreen {
    NONE,
    CREATE_INVOICE,
    INVOICE_DETAIL
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

    val stats: StateFlow<FinancialStats> = allInvoices.combine(companyProfile) { invoices, _ ->
        val totalInvoiced = invoices.sumOf { it.grandTotal }
        val totalPaid = invoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.grandTotal }
        val totalPending = invoices.filter { it.status == InvoiceStatus.PENDING || it.status == InvoiceStatus.OVERDUE }.sumOf { it.grandTotal }
        val totalVat = invoices.sumOf { it.vatAmount }

        FinancialStats(
            totalInvoiced = totalInvoiced,
            totalPaid = totalPaid,
            totalPending = totalPending,
            totalVat = totalVat,
            totalCount = invoices.size,
            paidCount = invoices.count { it.status == InvoiceStatus.PAID },
            pendingCount = invoices.count { it.status == InvoiceStatus.PENDING },
            overdueCount = invoices.count { it.status == InvoiceStatus.OVERDUE },
            draftCount = invoices.count { it.status == InvoiceStatus.DRAFT }
        )
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
        _prefillProduct.value = product
        _activeSubScreen.value = SubScreen.CREATE_INVOICE
    }

    fun navigateBack(): Boolean {
        if (_activeSubScreen.value != SubScreen.NONE) {
            _activeSubScreen.value = SubScreen.NONE
            _selectedInvoice.value = null
            _prefillProduct.value = null
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
        status: InvoiceStatus = InvoiceStatus.PENDING
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
            iban = companyProfile.value.iban
        )
        repository.addInvoice(invoice)
        _selectedInvoice.value = invoice
        _activeSubScreen.value = SubScreen.INVOICE_DETAIL
        return invoice
    }

    fun updateInvoiceStatus(invoiceId: String, status: InvoiceStatus) {
        repository.updateInvoiceStatus(invoiceId, status)
        if (_selectedInvoice.value?.id == invoiceId) {
            _selectedInvoice.value = _selectedInvoice.value?.copy(status = status)
        }
    }

    fun duplicateInvoice(invoiceId: String) {
        val dup = repository.duplicateInvoice(invoiceId)
        if (dup != null) {
            _selectedInvoice.value = dup
            _activeSubScreen.value = SubScreen.INVOICE_DETAIL
        }
    }

    fun deleteInvoice(invoiceId: String) {
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
    ) {
        val client = Client(
            id = "C-${(clients.value.size + 101)}",
            name = name,
            company = company,
            phone = phone,
            email = email,
            vatNumber = vatNumber,
            address = address
        )
        repository.addClient(client)
    }

    fun updateCompanyProfile(profile: CompanyProfile) {
        repository.updateCompanyProfile(profile)
    }
}
