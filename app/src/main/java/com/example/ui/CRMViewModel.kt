package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Interaction
import com.example.data.Lead
import com.example.data.User
import com.example.data.CRMRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CRMViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CRMRepository(application)

    // User Session and Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Auth Screen Inputs
    val authEmail = MutableStateFlow("")
    val authPassword = MutableStateFlow("")
    val authBusinessName = MutableStateFlow("")
    val isSignUpMode = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)

    // Navigation and UI state
    private val _currentTab = MutableStateFlow("dashboard") // dashboard, kanban, list, settings
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isCommandPaletteOpen = MutableStateFlow(false)
    val isCommandPaletteOpen: StateFlow<Boolean> = _isCommandPaletteOpen.asStateFlow()

    // Leads & Filters (Partitioned by the authenticated user)
    val allLeads: StateFlow<List<Lead>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getLeadsForUser(user.email)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _filterStage = MutableStateFlow<String?>(null)
    val filterStage: StateFlow<String?> = _filterStage.asStateFlow()

    private val _filterPriority = MutableStateFlow<String?>(null)
    val filterPriority: StateFlow<String?> = _filterPriority.asStateFlow()

    // Multi-select bulk actions
    private val _selectedLeadIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedLeadIds: StateFlow<Set<Int>> = _selectedLeadIds.asStateFlow()

    // Drawer / Selected Lead Details
    private val _selectedLeadId = MutableStateFlow<Int?>(null)
    val selectedLeadId: StateFlow<Int?> = _selectedLeadId.asStateFlow()

    val selectedLead: StateFlow<Lead?> = combine(allLeads, _selectedLeadId) { leads, id ->
        leads.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedLeadInteractions: StateFlow<List<Interaction>> = _selectedLeadId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getInteractionsForLead(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Follow Up Dialog / Modal state
    private val _followUpLeadId = MutableStateFlow<Int?>(null)
    val followUpLeadId: StateFlow<Int?> = _followUpLeadId.asStateFlow()

    val followUpLead: StateFlow<Lead?> = combine(allLeads, _followUpLeadId) { leads, id ->
        leads.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeTemplateId = MutableStateFlow(1) // 1: Warm Intro, 2: Proposal Nudge, 3: Breakup
    val activeTemplateId: StateFlow<Int> = _activeTemplateId.asStateFlow()

    private val _generatedMessageText = MutableStateFlow("")
    val generatedMessageText: StateFlow<String> = _generatedMessageText.asStateFlow()

    // Form inputs (Adding/Editing Lead)
    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingLeadId = MutableStateFlow<Int?>(null) // null = adding, non-null = editing
    val editingLeadId: StateFlow<Int?> = _editingLeadId.asStateFlow()

    val formName = MutableStateFlow("")
    val formCompany = MutableStateFlow("")
    val formEmail = MutableStateFlow("")
    val formPhone = MutableStateFlow("")
    val formDealValue = MutableStateFlow("")
    val formConfidence = MutableStateFlow("50")
    val formStage = MutableStateFlow("New Lead")
    val formPriority = MutableStateFlow("Medium")
    val formNotes = MutableStateFlow("")
    val formNextFollowUp = MutableStateFlow<Long>(0L)
    val formChannelPref = MutableStateFlow("Email")

    // Validation Errors
    val errorName = MutableStateFlow<String?>(null)
    val errorEmail = MutableStateFlow<String?>(null)
    val errorPhone = MutableStateFlow<String?>(null)
    val errorDealValue = MutableStateFlow<String?>(null)

    // Settings actions state
    private val _settingsMessage = MutableStateFlow<String?>(null)
    val settingsMessage: StateFlow<String?> = _settingsMessage.asStateFlow()

    val optInNotifications: StateFlow<Boolean> = _currentUser.map { it?.optInNotifications != false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val businessName: StateFlow<String> = _currentUser.map { it?.businessName ?: "Our Brand" }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Our Brand")

    // --- INTEGRATIONS AND WEBHOOKS ENGINE ---
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    val allInteractions: StateFlow<List<Interaction>> = repository.getAllInteractions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Retrieve last active session from SharedPreferences
        viewModelScope.launch {
            val sharedPref = getApplication<Application>().getSharedPreferences("crm_auth", Context.MODE_PRIVATE)
            val savedEmail = sharedPref.getString("logged_in_email", null)
            if (savedEmail != null) {
                val dbUser = repository.getUserByEmail(savedEmail)
                if (dbUser != null) {
                    _currentUser.value = dbUser
                    // Trigger overdue check
                    checkForOverdueFollowUps()
                }
            }
        }

        // Observe currentUser session to sync unique API Key
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user != null) {
                    val sharedPref = getApplication<Application>().getSharedPreferences("crm_auth", Context.MODE_PRIVATE)
                    var key = sharedPref.getString("api_key_for_${user.email}", null)
                    if (key == null) {
                        val hex = user.email.hashCode().toString(16).padStart(8, '0')
                        key = "aura_live_$hex"
                        sharedPref.edit().putString("api_key_for_${user.email}", key).apply()
                    }
                    _apiKey.value = key
                } else {
                    _apiKey.value = ""
                }
            }
        }

        // Generate tailored template when followUpLead changes or templateId changes
        combine(followUpLead, _activeTemplateId) { lead, templateId ->
            if (lead != null) {
                _generatedMessageText.value = getTemplateCopy(lead, templateId)
            }
        }.launchIn(viewModelScope)
    }

    // Navigation and tab changing
    fun navigateTo(tab: String) {
        _currentTab.value = tab
    }

    fun toggleCommandPalette(open: Boolean) {
        _isCommandPaletteOpen.value = open
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Set filters
    fun setStageFilter(stage: String?) {
        _filterStage.value = stage
    }

    fun setPriorityFilter(priority: String?) {
        _filterPriority.value = priority
    }

    // Selected lead in detail drawer
    fun selectLead(leadId: Int?) {
        _selectedLeadId.value = leadId
    }

    // Search results combining query and filter
    val filteredLeads: StateFlow<List<Lead>> = combine(allLeads, _searchQuery, _filterStage, _filterPriority) { leads, query, stage, priority ->
        leads.filter { lead ->
            val matchQuery = query.isEmpty() ||
                    lead.name.contains(query, ignoreCase = true) ||
                    lead.company.contains(query, ignoreCase = true) ||
                    lead.email.contains(query, ignoreCase = true) ||
                    lead.notes.contains(query, ignoreCase = true)
            val matchStage = stage == null || lead.pipelineStage == stage
            val matchPriority = priority == null || lead.priority == priority
            matchQuery && matchStage && matchPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // "Due Today" follow up queue (overdue or scheduled for today, excluding Completed columns Won/Lost)
    val dueTodayLeads: StateFlow<List<Lead>> = allLeads.map { leads ->
        val now = System.currentTimeMillis()
        // Today ends at 23:59:59 of current day
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfToday = cal.timeInMillis

        leads.filter { lead ->
            lead.pipelineStage != "Won" && lead.pipelineStage != "Lost" &&
                    lead.nextFollowUpDate > 0 && lead.nextFollowUpDate <= endOfToday
        }.sortedWith(compareBy<Lead> {
            when (it.priority) {
                "High" -> 1
                "Medium" -> 2
                else -> 3
            }
        }.thenBy { it.nextFollowUpDate })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary KPIs: Total Pipeline Value, Active Leads, Conversion Rate, Avg Sales Cycle Length
    val pipelineValue: StateFlow<Double> = allLeads.map { leads ->
        leads.filter { it.pipelineStage != "Lost" }.sumOf { it.dealValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeLeadsCount: StateFlow<Int> = allLeads.map { leads ->
        leads.count { it.pipelineStage != "Won" && it.pipelineStage != "Lost" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val conversionRate: StateFlow<Double> = allLeads.map { leads ->
        val wonCount = leads.count { it.pipelineStage == "Won" }.toDouble()
        val totalClosed = leads.count { it.pipelineStage == "Won" || it.pipelineStage == "Lost" }.toDouble()
        if (totalClosed > 0) (wonCount / totalClosed) * 100.0 else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val avgSalesCycle: StateFlow<Int> = allLeads.map { leads ->
        // Simulate average days from creation to "Won" or "Lost" based on actual items
        val closedLeads = leads.filter { it.pipelineStage == "Won" || it.pipelineStage == "Lost" }
        if (closedLeads.isNotEmpty()) {
            val totalDays = closedLeads.map { lead ->
                val diffMs = lead.stageChangedDate - lead.addedDate
                val days = (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0L)
                days
            }.sum()
            (totalDays.toDouble() / closedLeads.size).coerceAtLeast(1.0).toInt()
        } else {
            7 // default mock cycle
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)

    // Multi-select bulk management
    fun toggleSelectLead(id: Int) {
        val current = _selectedLeadIds.value
        _selectedLeadIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAllLeads(ids: List<Int>) {
        _selectedLeadIds.value = ids.toSet()
    }

    fun clearSelections() {
        _selectedLeadIds.value = emptySet()
    }

    fun bulkDeleteSelected() {
        val ids = _selectedLeadIds.value.toList()
        viewModelScope.launch {
            repository.bulkDelete(ids)
            clearSelections()
        }
    }

    fun bulkChangeSelectedStage(newStage: String) {
        val ids = _selectedLeadIds.value.toList()
        viewModelScope.launch {
            repository.bulkChangeStage(ids, newStage)
            clearSelections()
        }
    }

    // Stage movement for Kanban
    fun updateLeadStage(lead: Lead, newStage: String) {
        viewModelScope.launch {
            val updated = lead.copy(
                pipelineStage = newStage,
                stageChangedDate = System.currentTimeMillis()
            )
            repository.updateLead(updated, stageChanged = true)
        }
    }

    // Communication triggers
    fun openFollowUp(leadId: Int) {
        _followUpLeadId.value = leadId
        _activeTemplateId.value = 1
    }

    fun closeFollowUpDetail() {
        _followUpLeadId.value = null
    }

    fun setTemplateId(id: Int) {
        _activeTemplateId.value = id
    }

    fun updateGeneratedMessage(text: String) {
        _generatedMessageText.value = text
    }

    fun executeFollowUpAction(lead: Lead) {
        viewModelScope.launch {
            val templateName = when (activeTemplateId.value) {
                1 -> "The Warm Intro Check-In"
                2 -> "The Proposal Gentle Nudge"
                else -> "The Breakup Template"
            }
            repository.logFollowUpExecuted(
                leadId = lead.id,
                channel = lead.channelPreference,
                templateUsed = templateName,
                detailSummary = _generatedMessageText.value
            )
            closeFollowUpDetail()
        }
    }

    fun addManualNote(leadId: Int, note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            val lead = repository.getLeadById(leadId)
            if (lead != null) {
                // Update primary notes too
                val updated = lead.copy(notes = note, lastInteractionDate = System.currentTimeMillis())
                repository.updateLead(updated, noteUpdated = true)
            }
        }
    }

    // Form inputs and Add/Edit Operations
    fun openAddEdit(leadId: Int? = null) {
        clearFormErrors()
        if (leadId == null) {
            // Add Lead
            _editingLeadId.value = null
            formName.value = ""
            formCompany.value = ""
            formEmail.value = ""
            formPhone.value = ""
            formDealValue.value = ""
            formConfidence.value = "50"
            formStage.value = "New Lead"
            formPriority.value = "Medium"
            formNotes.value = ""
            formNextFollowUp.value = 0L
            formChannelPref.value = "Email"
        } else {
            // Edit Lead
            viewModelScope.launch {
                val lead = repository.getLeadById(leadId)
                if (lead != null) {
                    _editingLeadId.value = leadId
                    formName.value = lead.name
                    formCompany.value = lead.company
                    formEmail.value = lead.email
                    formPhone.value = lead.phone
                    formDealValue.value = lead.dealValue.toInt().toString()
                    formConfidence.value = lead.confidence.toString()
                    formStage.value = lead.pipelineStage
                    formPriority.value = lead.priority
                    formNotes.value = lead.notes
                    formNextFollowUp.value = lead.nextFollowUpDate
                    formChannelPref.value = lead.channelPreference
                }
            }
        }
        _isAddEditOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
    }

    fun saveLead(): Boolean {
        if (!validateForm()) return false

        val valueDouble = formDealValue.value.toDoubleOrNull() ?: 0.0
        val confidenceInt = formConfidence.value.toIntOrNull() ?: 50
        val isEdit = _editingLeadId.value != null

        viewModelScope.launch {
            if (isEdit) {
                val existing = repository.getLeadById(_editingLeadId.value!!)
                if (existing != null) {
                    val stageChanged = existing.pipelineStage != formStage.value
                    val noteUpdated = existing.notes != formNotes.value
                    val nextActionUpdated = existing.nextFollowUpDate != formNextFollowUp.value || existing.channelPreference != formChannelPref.value

                    val updated = existing.copy(
                        name = formName.value.trim(),
                        company = formCompany.value.trim(),
                        email = formEmail.value.trim(),
                        phone = formPhone.value.trim(),
                        dealValue = valueDouble,
                        confidence = confidenceInt,
                        pipelineStage = formStage.value,
                        priority = formPriority.value,
                        notes = formNotes.value.trim(),
                        nextFollowUpDate = formNextFollowUp.value,
                        channelPreference = formChannelPref.value,
                        lastInteractionDate = System.currentTimeMillis(),
                        stageChangedDate = if (stageChanged) System.currentTimeMillis() else existing.stageChangedDate
                    )
                    repository.updateLead(updated, stageChanged, noteUpdated, nextActionUpdated)
                }
            } else {
                val owner = _currentUser.value?.email ?: ""
                val newLead = Lead(
                    name = formName.value.trim(),
                    company = formCompany.value.trim(),
                    email = formEmail.value.trim(),
                    phone = formPhone.value.trim(),
                    dealValue = valueDouble,
                    confidence = confidenceInt,
                    pipelineStage = formStage.value,
                    priority = formPriority.value,
                    notes = formNotes.value.trim(),
                    nextFollowUpDate = formNextFollowUp.value,
                    addedDate = System.currentTimeMillis(),
                    stageChangedDate = System.currentTimeMillis(),
                    channelPreference = formChannelPref.value,
                    ownerEmail = owner
                )
                repository.insertLead(newLead)

                // Trigger a push notification for new lead assignment!
                sendPushNotification(
                    title = "New Lead Assigned",
                    message = "${newLead.name} from ${newLead.company} has been added to your CRM pipeline!"
                )
            }
            closeAddEdit()
        }
        return true
    }

    fun deleteLead(lead: Lead) {
        viewModelScope.launch {
            repository.deleteLead(lead)
            if (_selectedLeadId.value == lead.id) {
                _selectedLeadId.value = null
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (formName.value.trim().isEmpty()) {
            errorName.value = "Lead name is required"
            isValid = false
        } else {
            errorName.value = null
        }

        val emailTrimmed = formEmail.value.trim()
        if (emailTrimmed.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            errorEmail.value = "Enter a valid email address"
            isValid = false
        } else {
            errorEmail.value = null
        }

        if (formDealValue.value.trim().isNotEmpty() && formDealValue.value.toDoubleOrNull() == null) {
            errorDealValue.value = "Deal value must be a valid number"
            isValid = false
        } else {
            errorDealValue.value = null
        }

        return isValid
    }

    private fun clearFormErrors() {
        errorName.value = null
        errorEmail.value = null
        errorPhone.value = null
        errorDealValue.value = null
    }

    // Portability Methods (Export / Import JSON)
    fun exportData(onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportLeadsJson()
            onCompleted(json)
            showSettingsMessage("Data exported successfully to clipboard!")
        }
    }

    fun importData(json: String, onCompleted: (Boolean) -> Unit) {
        if (json.isBlank()) {
            showSettingsMessage("Invalid database import file data.")
            onCompleted(false)
            return
        }
        viewModelScope.launch {
            val success = repository.importLeadsJson(json)
            if (success) {
                showSettingsMessage("Database imported successfully!")
            } else {
                showSettingsMessage("Failed to parse imported data. Make sure it is valid CRM JSON.")
            }
            onCompleted(success)
        }
    }

    private fun showSettingsMessage(msg: String) {
        _settingsMessage.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            if (_settingsMessage.value == msg) {
                _settingsMessage.value = null
            }
        }
    }

    // Contextual generated texts
    private fun getTemplateCopy(lead: Lead, templateId: Int): String {
        val contactName = lead.name.substringBefore(" ")
        val userBusinessName = "Our Team" // Default fallback representation
        return when (templateId) {
            1 -> {
                "Hi $contactName,\n\n" +
                        "This is from $userBusinessName! I loved learning about other initiatives at ${lead.company} recently. " +
                        "I wanted to connect to schedule a brief discovery call and explore how we can support you.\n\n" +
                        "Are you available for a 15-minute chat either Wednesday or Thursday?\n\n" +
                        "Best regards,\n" +
                        "Partner Support Team"
            }
            2 -> {
                "Hi $contactName,\n\n" +
                        "I hope you're having a productive week! " +
                        "I'm following up on the service proposal we sent over regarding the ${lead.company} onboarding project. " +
                        "I'd love to see if you or the key stakeholders had any questions or needed amendments on pricing or deliverables.\n\n" +
                        "We can finalize details at your convenience. Let me know what you think!\n\n" +
                        "Warmly,\n" +
                        "Commercial Representative"
            }
            else -> {
                "Hi $contactName,\n\n" +
                        "Since I haven't heard back from you in a little while regarding our ${lead.company} proposal, " +
                        "I am going to assume priorities have shifted and close out our active inquiry for now.\n\n" +
                        "No hard feelings! If you ever want to pick this back up in the future, please don't hesitate to reach directly.\n\n" +
                        "All the best,\n" +
                        "Business Development"
            }
        }
    }

    // --- USER AUTHENTICATION ACTIONS ---
    fun handleAuthentication(onSuccess: () -> Unit = {}) {
        val email = authEmail.value.trim()
        val password = authPassword.value
        val business = authBusinessName.value.trim()
        val signUp = isSignUpMode.value

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            authError.value = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            authError.value = "Password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            try {
                if (signUp) {
                    if (business.isEmpty()) {
                        authError.value = "Please enter a business profile name."
                        return@launch
                    }
                    val existing = repository.getUserByEmail(email)
                    if (existing != null) {
                        authError.value = "An account with this email already exists."
                        return@launch
                    }
                    val newUser = User(
                        email = email,
                        passwordHash = password,
                        businessName = business,
                        optInNotifications = true
                    )
                    repository.insertUser(newUser)
                    repository.seedDatabaseIfEmpty(email)
                    persistSession(email)
                    _currentUser.value = newUser
                    authError.value = null
                    onSuccess()
                    
                    sendPushNotification(
                        title = "Welcome to CRM Hub",
                        message = "Welcome, $business! Your account has been securely setup and customized."
                    )
                } else {
                    val existing = repository.getUserByEmail(email)
                    if (existing == null || existing.passwordHash != password) {
                        authError.value = "Invalid email or password."
                        return@launch
                    }
                    persistSession(email)
                    _currentUser.value = existing
                    authError.value = null
                    // Trigger overdue check upon login
                    checkForOverdueFollowUps()
                    onSuccess()
                }
            } catch (e: Exception) {
                authError.value = "Authentication error: ${e.message}"
            }
        }
    }

    fun handleLogout() {
        clearPersistedSession()
        _currentUser.value = null
        authEmail.value = ""
        authPassword.value = ""
        authBusinessName.value = ""
        authError.value = null
    }

    fun toggleAuthMode() {
        isSignUpMode.value = !isSignUpMode.value
        authError.value = null
    }

    private fun persistSession(email: String) {
        val sharedPref = getApplication<Application>().getSharedPreferences("crm_auth", Context.MODE_PRIVATE)
        sharedPref.edit().putString("logged_in_email", email).apply()
    }

    private fun clearPersistedSession() {
        val sharedPref = getApplication<Application>().getSharedPreferences("crm_auth", Context.MODE_PRIVATE)
        sharedPref.edit().remove("logged_in_email").apply()
    }

    // --- PUSH NOTIFICATION ENGINE ---
    fun toggleNotificationOptIn(enabled: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(optInNotifications = enabled)
            repository.updateUser(updated)
            _currentUser.value = updated
            
            if (enabled) {
                sendPushNotification(
                    title = "Notifications Enabled",
                    message = "You have opted-in to critical lead alerts and overdue follow-up notifications."
                )
            }
        }
    }

    fun sendPushNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val channelId = "crm_alerts"
        
        // Match user's opt-in setting
        val isOptedIn = _currentUser.value?.optInNotifications != false
        if (!isOptedIn) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "CRM Alerts"
            val desc = "Notifications for lead assignments and critical overdue events"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        try {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val managerCompat = NotificationManagerCompat.from(context)
            managerCompat.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e("CRM_NOTIF", "Security exception posting notification", e)
        } catch (e: Exception) {
            Log.e("CRM_NOTIF", "Error posting notification", e)
        }
    }

    fun checkForOverdueFollowUps() {
        val user = _currentUser.value ?: return
        if (!user.optInNotifications) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val leads = allLeads.value
            val overdueCount = leads.count { lead ->
                lead.pipelineStage != "Won" && lead.pipelineStage != "Lost" &&
                        lead.nextFollowUpDate > 0 && lead.nextFollowUpDate < now
            }
            if (overdueCount > 0) {
                sendPushNotification(
                    title = "Overdue Actions Pending",
                    message = "You have $overdueCount pending follow-ups that require attention."
                )
            }
        }
    }

    // --- IN-APP EMAIL COMPOSER STATE & ACTORS ---
    private val _isEmailComposerOpen = MutableStateFlow(false)
    val isEmailComposerOpen: StateFlow<Boolean> = _isEmailComposerOpen.asStateFlow()

    private val _emailComposerLeadId = MutableStateFlow<Int?>(null)
    val emailComposerLeadId: StateFlow<Int?> = _emailComposerLeadId.asStateFlow()

    val emailComposerLead: StateFlow<Lead?> = combine(allLeads, _emailComposerLeadId) { leads, id ->
        leads.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val emailToField = MutableStateFlow("")
    val emailSubjectField = MutableStateFlow("")
    val emailBodyField = MutableStateFlow("")

    fun openEmailComposer(leadId: Int) {
        _emailComposerLeadId.value = leadId
        viewModelScope.launch {
            val lead = repository.getLeadById(leadId)
            if (lead != null) {
                emailToField.value = lead.email
                emailSubjectField.value = "Follow up regarding ${lead.company.ifBlank { "our proposal" }}"
                emailBodyField.value = getTemplateCopy(lead, _activeTemplateId.value)
                _isEmailComposerOpen.value = true
            }
        }
    }

    fun closeEmailComposer() {
        _isEmailComposerOpen.value = false
        _emailComposerLeadId.value = null
    }

    fun sendEmail(onSendIntentTriggered: (recipient: String, subject: String, body: String) -> Unit) {
        val leadId = _emailComposerLeadId.value ?: return
        viewModelScope.launch {
            val lead = repository.getLeadById(leadId)
            if (lead != null) {
                // Log interaction in lead timeline
                repository.logFollowUpExecuted(
                    leadId = lead.id,
                    channel = "Email",
                    templateUsed = "Emailed: ${emailSubjectField.value}",
                    detailSummary = emailBodyField.value
                )

                // Trigger callback to construct real ACTION_SENDTO intent
                onSendIntentTriggered(
                    emailToField.value,
                    emailSubjectField.value,
                    emailBodyField.value
                )
                
                closeEmailComposer()
            }
        }
    }

    // --- INTEGRATIONS AND WEBHOOKS HANDLERS ---
    fun regenerateApiKey() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val sharedPref = getApplication<Application>().getSharedPreferences("crm_auth", Context.MODE_PRIVATE)
            val randomSuffix = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
            val newKey = "aura_live_$randomSuffix"
            sharedPref.edit().putString("api_key_for_${user.email}", newKey).apply()
            _apiKey.value = newKey
            _settingsMessage.value = "Your unique API Key was regenerated. Please update Twilio/Zapier."
            
            // Auto clear settings message after delay
            kotlinx.coroutines.delay(4000)
            if (_settingsMessage.value == "Your unique API Key was regenerated. Please update Twilio/Zapier.") {
                _settingsMessage.value = null
            }
        }
    }

    fun triggerSimulatedWebhook(userKey: String, payloadJson: String): String {
        val currentKey = _apiKey.value
        if (currentKey.isBlank() || userKey.trim() != currentKey) {
            return "❌ Authorization Failed: The provided API key does not match your active account API key."
        }

        try {
            val json = org.json.JSONObject(payloadJson)
            val event = json.optString("event", "generic_webhook")
            
            // Extract fields gracefully
            val name = json.optString("name", json.optString("caller_name", "New Incoming Lead"))
            val phone = json.optString("phone", json.optString("from", "+15550000"))
            val email = json.optString("email", "webhook@incoming.io")
            val company = json.optString("company", "Web Lead")
            val notes = json.optString("notes", json.optString("transcription", json.optString("message", "Missed call notification")))
            val dealValue = json.optDouble("deal_value", 0.0)

            val owner = _currentUser.value?.email ?: ""
            
            viewModelScope.launch {
                val newLead = Lead(
                    name = name.trim(),
                    company = company.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    dealValue = if (dealValue.isNaN() || dealValue <= 0.0) 25000.0 else dealValue, // Default deal value
                    confidence = 50,
                    pipelineStage = "New Lead",
                    priority = "High",
                    notes = notes,
                    ownerEmail = owner,
                    channelPreference = if (event.contains("call") || notes.contains("Call", ignoreCase = true)) "Phone" else "Email"
                )
                repository.insertLead(newLead)
                
                // Formulate a beautiful alert title and message based on event type
                val notificationTitle = when (event) {
                    "missed_call" -> "📞 Missed Call Assigned"
                    "facebook_lead" -> "⚡ Zapier FB Lead"
                    "contact_form" -> "📥 Web Contact Inquiry"
                    else -> "🔌 Webhook Route Success"
                }
                
                val notificationMsg = when (event) {
                    "missed_call" -> "Missed call from $name ($phone). Voicemail: $notes"
                    "facebook_lead" -> "New Zapier Form: $name ($company) - Value: $${dealValue.toInt()}"
                    "contact_form" -> "Website Inquirer: $name ($phone) - Message: $notes"
                    else -> "New webhook routed contact: $name from $company"
                }

                sendPushNotification(
                    title = notificationTitle,
                    message = notificationMsg
                )
            }
            return "✅ Success: Webhook authenticated & processed successfully! A new deal for '$name' was created in your CRM pipeline. Native push alert dispatched!"
        } catch (e: Exception) {
            return "❌ Payload Parse Error: Invalid JSON structure or missing fields. (${e.message})"
        }
    }
}
