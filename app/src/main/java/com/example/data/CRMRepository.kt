package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Calendar

class CRMRepository(context: Context) {
    private val database: CRMDatabase = Room.databaseBuilder(
        context.applicationContext,
        CRMDatabase::class.java,
        "crm_pipeline_database"
    ).fallbackToDestructiveMigration().build()

    private val dao = database.crmDao()

    val allLeads: Flow<List<Lead>> = dao.getAllLeads()

    fun getLeadsForUser(ownerEmail: String): Flow<List<Lead>> = dao.getLeadsForUser(ownerEmail)

    fun getInteractionsForLead(leadId: Int): Flow<List<Interaction>> {
        return dao.getInteractionsForLead(leadId)
    }

    fun getAllInteractions(): Flow<List<Interaction>> {
        return dao.getAllInteractions()
    }

    suspend fun getUserByEmail(email: String): User? = dao.getUserByEmail(email)

    suspend fun insertUser(user: User) = dao.insertUser(user)

    suspend fun updateUser(user: User) = dao.updateUser(user)

    suspend fun getLeadById(id: Int): Lead? = dao.getLeadById(id)

    suspend fun insertLead(lead: Lead): Long {
        val id = dao.insertLead(lead)
        dao.insertInteraction(
            Interaction(
                leadId = id.toInt(),
                type = "Created",
                description = "Lead added to pipeline: ${lead.pipelineStage}"
            )
        )
        return id
    }

    suspend fun updateLead(lead: Lead, stageChanged: Boolean = false, noteUpdated: Boolean = false, nextActionUpdated: Boolean = false) {
        val original = dao.getLeadById(lead.id)
        dao.updateLead(lead)
        
        if (original != null) {
            if (stageChanged || original.pipelineStage != lead.pipelineStage) {
                dao.insertInteraction(
                    Interaction(
                        leadId = lead.id,
                        type = "Stage Change",
                        description = "Pipeline stage changed: ${original.pipelineStage} ➔ ${lead.pipelineStage}"
                    )
                )
            }
            if (noteUpdated && original.notes != lead.notes) {
                dao.insertInteraction(
                    Interaction(
                        leadId = lead.id,
                        type = "Note",
                        description = "Lead notes updated",
                        detail = lead.notes
                    )
                )
            }
            if (nextActionUpdated && (original.nextFollowUpDate != lead.nextFollowUpDate || original.channelPreference != lead.channelPreference)) {
                val formattedDate = if (lead.nextFollowUpDate > 0) {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = lead.nextFollowUpDate
                    "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.YEAR)}"
                } else "None"
                dao.insertInteraction(
                    Interaction(
                        leadId = lead.id,
                        type = "Note",
                        description = "Next follow-up scheduled",
                        detail = "Date: $formattedDate | Channel: ${lead.channelPreference}"
                    )
                )
            }
        }
    }

    suspend fun logFollowUpExecuted(leadId: Int, channel: String, templateUsed: String, detailSummary: String) {
        val lead = dao.getLeadById(leadId)
        if (lead != null) {
            val updatedLead = lead.copy(
                lastInteractionDate = System.currentTimeMillis()
            )
            dao.updateLead(updatedLead)
            
            dao.insertInteraction(
                Interaction(
                    leadId = leadId,
                    type = "Follow Up Executed",
                    description = "Followed up via $channel",
                    detail = "$templateUsed\n\n$detailSummary"
                )
            )
        }
    }

    suspend fun deleteLead(lead: Lead) {
        dao.deleteInteractionsForLead(lead.id)
        dao.deleteLead(lead)
    }

    suspend fun bulkDelete(ids: List<Int>) {
        ids.forEach { id ->
            dao.deleteInteractionsForLead(id)
        }
        dao.deleteLeadsBulk(ids)
    }

    suspend fun bulkChangeStage(ids: List<Int>, stage: String) {
        val now = System.currentTimeMillis()
        dao.updateLeadsStageBulk(ids, stage, now)
        ids.forEach { id ->
            dao.insertInteraction(
                Interaction(
                    leadId = id,
                    type = "Stage Change",
                    description = "Bulk pipeline stage changed ➔ $stage"
                )
            )
        }
    }

    suspend fun exportLeadsJson(): String {
        val leads = dao.getAllLeadsList()
        val builder = StringBuilder()
        builder.append("[")
        leads.forEachIndexed { index, lead ->
            val interactions = dao.getInteractionsForLeadList(lead.id)
            builder.append("{")
            builder.append("\"name\":\"${escape(lead.name)}\",")
            builder.append("\"company\":\"${escape(lead.company)}\",")
            builder.append("\"email\":\"${escape(lead.email)}\",")
            builder.append("\"phone\":\"${escape(lead.phone)}\",")
            builder.append("\"dealValue\":${lead.dealValue},")
            builder.append("\"confidence\":${lead.confidence},")
            builder.append("\"pipelineStage\":\"${escape(lead.pipelineStage)}\",")
            builder.append("\"priority\":\"${escape(lead.priority)}\",")
            builder.append("\"notes\":\"${escape(lead.notes)}\",")
            builder.append("\"nextFollowUpDate\":${lead.nextFollowUpDate},")
            builder.append("\"lastInteractionDate\":${lead.lastInteractionDate},")
            builder.append("\"addedDate\":${lead.addedDate},")
            builder.append("\"stageChangedDate\":${lead.stageChangedDate},")
            builder.append("\"channelPreference\":\"${escape(lead.channelPreference)}\",")
            
            builder.append("\"interactions\":[")
            interactions.forEachIndexed { iIndex, inter ->
                builder.append("{")
                builder.append("\"timestamp\":${inter.timestamp},")
                builder.append("\"type\":\"${escape(inter.type)}\",")
                builder.append("\"description\":\"${escape(inter.description)}\",")
                builder.append("\"detail\":\"${escape(inter.detail)}\"")
                builder.append("}")
                if (iIndex < interactions.size - 1) builder.append(",")
            }
            builder.append("]")
            
            builder.append("}")
            if (index < leads.size - 1) builder.append(",")
        }
        builder.append("]")
        return builder.toString()
    }

    suspend fun importLeadsJson(json: String): Boolean {
        return try {
            // Very basic manual JSON parser to avoid importing heavy libraries or failing in Unit tests,
            // we will parse list of objects
            val leadsList = mutableListOf<Map<String, Any>>()
            
            // Clean simple parsing (or standard library). In Kotlin, we can do some simple string patterns
            // or regex to extract leads and their interactions!
            // Let's do regex-based extraction of lead objects.
            val objRegex = Regex("\\{[\\s\\S]*?\\}")
            val matches = objRegex.findAll(json).toList()
            
            for (match in matches) {
                val text = match.value
                val name = extractString(text, "name") ?: continue
                val company = extractString(text, "company") ?: ""
                val email = extractString(text, "email") ?: ""
                val phone = extractString(text, "phone") ?: ""
                val dealValue = extractDouble(text, "dealValue") ?: 0.0
                val confidence = extractInt(text, "confidence") ?: 50
                val pipelineStage = extractString(text, "pipelineStage") ?: "New Lead"
                val priority = extractString(text, "priority") ?: "Medium"
                val notes = extractString(text, "notes") ?: ""
                val nextFollowUpDate = extractLong(text, "nextFollowUpDate") ?: 0L
                val lastInteractionDate = extractLong(text, "lastInteractionDate") ?: System.currentTimeMillis()
                val addedDate = extractLong(text, "addedDate") ?: System.currentTimeMillis()
                val stageChangedDate = extractLong(text, "stageChangedDate") ?: System.currentTimeMillis()
                val channelPreference = extractString(text, "channelPreference") ?: "Email"

                val lead = Lead(
                    name = name,
                    company = company,
                    email = email,
                    phone = phone,
                    dealValue = dealValue,
                    confidence = confidence,
                    pipelineStage = pipelineStage,
                    priority = priority,
                    notes = notes,
                    nextFollowUpDate = nextFollowUpDate,
                    lastInteractionDate = lastInteractionDate,
                    addedDate = addedDate,
                    stageChangedDate = stageChangedDate,
                    channelPreference = channelPreference
                )
                
                val parentId = insertLead(lead)
                
                // Parse optional interactions
                if (text.contains("\"interactions\"")) {
                    val interPart = text.substringAfter("\"interactions\"", "")
                    val interObjs = objRegex.findAll(interPart).toList()
                    for (iObj in interObjs) {
                        val iText = iObj.value
                        val timestamp = extractLong(iText, "timestamp") ?: System.currentTimeMillis()
                        val type = extractString(iText, "type") ?: "Note"
                        val description = extractString(iText, "description") ?: ""
                        val detail = extractString(iText, "detail") ?: ""
                        
                        dao.insertInteraction(
                            Interaction(
                                leadId = parentId.toInt(),
                                timestamp = timestamp,
                                type = type,
                                description = description,
                                detail = detail
                            )
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractDouble(json: String, key: String): Double? {
        val pattern = "\"$key\"\\s*:\\s*([0-9.]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractInt(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*([0-9]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val pattern = "\"$key\"\\s*:\\s*([0-9]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun escape(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
    }

    suspend fun seedDatabaseIfEmpty(ownerEmail: String = "") {
        // Double check if database already has leads for this owner
        val currentLeads = if (ownerEmail.isEmpty()) dao.getAllLeadsList() else dao.getLeadsForUserList(ownerEmail)
        if (currentLeads.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Seeding 7 diverse mock leads:
        // 1. Alice Vance (Apex Labs) - In Negotiation, High Priority, Follow-up Overdue by 1 day
        val id1 = dao.insertLead(
            Lead(
                name = "Alice Vance",
                company = "Apex Labs Inc",
                email = "alice@apexlabs.co",
                phone = "+1 (555) 019-2831",
                dealValue = 12500.0,
                confidence = 80,
                pipelineStage = "In Negotiation",
                priority = "High",
                notes = "Needs updated service agreement based on the security assessment. Preparing technical proposal amendment.",
                nextFollowUpDate = now - oneDayMs, // Overdue by 1 day
                lastInteractionDate = now - 2 * oneDayMs,
                addedDate = now - 12 * oneDayMs,
                stageChangedDate = now - 4 * oneDayMs, // 4 Days stagnant
                channelPreference = "Email",
                ownerEmail = ownerEmail
            )
        )
        dao.insertInteraction(Interaction(leadId = id1.toInt(), type = "Created", description = "Lead added to pipeline: New Lead", timestamp = now - 12 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id1.toInt(), type = "Stage Change", description = "Pipeline stage changed: New Lead ➔ Contacted", timestamp = now - 10 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id1.toInt(), type = "Stage Change", description = "Pipeline stage changed: Contacted ➔ Proposal Sent", timestamp = now - 8 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id1.toInt(), type = "Stage Change", description = "Pipeline stage changed: Proposal Sent ➔ In Negotiation", timestamp = now - 4 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id1.toInt(), type = "Note", description = "Added note", detail = "Requested minor changes to standard SLA terms to accommodate their net-60 billing guidelines.", timestamp = now - 2 * oneDayMs))

        // 2. David Chen (Starlight Biotech) - Proposal Sent, High Priority, Follow-up Overdue by 3 days, Stagnant 12 days
        val id2 = dao.insertLead(
            Lead(
                name = "David Chen",
                company = "Starlight Biotech",
                email = "dchen@starlightbiotech.com",
                phone = "+1 (555) 014-9988",
                dealValue = 27000.0,
                confidence = 55,
                pipelineStage = "Proposal Sent",
                priority = "High",
                notes = "Sent main enterprise proposal package. Client requested more detail on HIPAA compliance.",
                nextFollowUpDate = now - 3 * oneDayMs, // Overdue by 3 days
                lastInteractionDate = now - 3 * oneDayMs,
                addedDate = now - 20 * oneDayMs,
                stageChangedDate = now - 12 * oneDayMs, // 12 Days stagnant
                channelPreference = "Phone",
                ownerEmail = ownerEmail
            )
        )
        dao.insertInteraction(Interaction(leadId = id2.toInt(), type = "Created", description = "Lead added to pipeline: New Lead", timestamp = now - 20 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id2.toInt(), type = "Stage Change", description = "Pipeline stage changed: New Lead ➔ Contacted", timestamp = now - 16 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id2.toInt(), type = "Stage Change", description = "Pipeline stage changed: Contacted ➔ Proposal Sent", timestamp = now - 12 * oneDayMs))

        // 3. Sarah Jenkins (Nexa Flow) - New Lead, Medium Priority, Follow-up Overdue by 2 days
        val id3 = dao.insertLead(
            Lead(
                name = "Sarah Jenkins",
                company = "Nexa Flow",
                email = "sarah@nexaflow.io",
                phone = "+1 (555) 021-1254",
                dealValue = 8500.0,
                confidence = 15,
                pipelineStage = "New Lead",
                priority = "Medium",
                notes = "Inbound signup. Downloaded our pricing brochure. Left initial voicemail, sent automated follow-up text.",
                nextFollowUpDate = now - 2 * oneDayMs, // Overdue by 2 days
                lastInteractionDate = now - 2 * oneDayMs,
                addedDate = now - 5 * oneDayMs,
                stageChangedDate = now - 5 * oneDayMs, // 5 Days stagnant
                channelPreference = "SMS",
                ownerEmail = ownerEmail
            )
        )
        dao.insertInteraction(Interaction(leadId = id3.toInt(), type = "Created", description = "Lead imported from website lead form", timestamp = now - 5 * oneDayMs))

        // 4. Robert Kovac (Titan Heavy Industries) - Won, Medium Priority, No Follow-up
        val id4 = dao.insertLead(
            Lead(
                name = "Robert Kovac",
                company = "Titan Heavy Industries",
                email = "r.kovac@titanindustries.org",
                phone = "+1 (555) 032-9012",
                dealValue = 48000.0,
                confidence = 100,
                pipelineStage = "Won",
                priority = "Medium",
                notes = "Contract signed! Retainer invoice sent and fully settled by wire transfer. Safe delivery finalized.",
                nextFollowUpDate = 0, // Completed
                lastInteractionDate = now,
                addedDate = now - 15 * oneDayMs,
                stageChangedDate = now - 2 * oneDayMs,
                channelPreference = "Email",
                ownerEmail = ownerEmail
            )
        )
        dao.insertInteraction(Interaction(leadId = id4.toInt(), type = "Created", description = "Lead added to pipeline: New Lead", timestamp = now - 15 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id4.toInt(), type = "Stage Change", description = "Pipeline stage changed: New Lead ➔ Contacted", timestamp = now - 13 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id4.toInt(), type = "Stage Change", description = "Pipeline stage changed: Contacted ➔ Proposal Sent", timestamp = now - 10 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id4.toInt(), type = "Stage Change", description = "Pipeline stage changed: Proposal Sent ➔ In Negotiation", timestamp = now - 6 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id4.toInt(), type = "Stage Change", description = "Pipeline stage changed: In Negotiation ➔ Won", timestamp = now - 2 * oneDayMs))

        // 5. Chloe Fontaine (Lumina Creative) - Contacted, Low Priority, Due today (in 2 hours)
        val id5 = dao.insertLead(
            Lead(
                name = "Chloe Fontaine",
                company = "Lumina Creative",
                email = "chloe@luminacreative.design",
                phone = "+1 (555) 040-3941",
                dealValue = 6200.0,
                confidence = 45,
                pipelineStage = "Contacted",
                priority = "Low",
                notes = "Messaged on LinkedIn about our logo and rebranding design package. Connect scheduled to explore parameters.",
                nextFollowUpDate = now + 2 * 60 * 60 * 1000L, // Due in 2 hours
                lastInteractionDate = now - 1 * oneDayMs,
                addedDate = now - 8 * oneDayMs,
                stageChangedDate = now - 3 * oneDayMs,
                channelPreference = "LinkedIn",
                ownerEmail = ownerEmail
            )
        )
        dao.insertInteraction(Interaction(leadId = id5.toInt(), type = "Created", description = "Lead created", timestamp = now - 8 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id5.toInt(), type = "Stage Change", description = "Pipeline stage changed: New Lead ➔ Contacted", timestamp = now - 3 * oneDayMs))

        // 6. Elena Rostova (Cosmo Scale) - In Negotiation, High Priority, Due Tomorrow
        val id6 = dao.insertLead(
            Lead(
                name = "Elena Rostova",
                company = "Cosmo Scale",
                email = "elena@cosmoscale.io",
                phone = "+1 (555) 078-4321",
                dealValue = 31500.0,
                confidence = 75,
                pipelineStage = "In Negotiation",
                priority = "High",
                notes = "Refining custom cloud storage volume limits. Meeting scheduled to detail service parameters.",
                nextFollowUpDate = now + oneDayMs, // Tomorrow
                lastInteractionDate = now,
                addedDate = now - 10 * oneDayMs,
                stageChangedDate = now - 1 * oneDayMs,
                channelPreference = "Email",
                ownerEmail = ownerEmail
            )
        )
        dao.insertInteraction(Interaction(leadId = id6.toInt(), type = "Created", description = "Lead added", timestamp = now - 10 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id6.toInt(), type = "Stage Change", description = "Pipeline stage changed: New Lead ➔ Contacted", timestamp = now - 7 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id6.toInt(), type = "Stage Change", description = "Pipeline stage changed: Contacted ➔ Proposal Sent", timestamp = now - 4 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id6.toInt(), type = "Stage Change", description = "Pipeline stage changed: Proposal Sent ➔ In Negotiation", timestamp = now - 1 * oneDayMs))

        // 7. Marcus Reed (Vortex Energy) - Lost, Low Priority, No Follow-up
        val id7 = dao.insertLead(
            Lead(
                name = "Marcus Reed",
                company = "Vortex Energy",
                email = "mreed@vortexenergy.net",
                phone = "+1 (555) 012-9844",
                dealValue = 18000.0,
                confidence = 0,
                pipelineStage = "Lost",
                priority = "Low",
                notes = "Decided to build internal analytics tools instead of outsourcing. Will keep in touch for version 2.",
                nextFollowUpDate = 0,
                lastInteractionDate = now - 5 * oneDayMs,
                addedDate = now - 14 * oneDayMs,
                stageChangedDate = now - 5 * oneDayMs,
                channelPreference = "Email",
                ownerEmail = ownerEmail
            )
        )
        dao.insertInteraction(Interaction(leadId = id7.toInt(), type = "Created", description = "Lead created", timestamp = now - 14 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id7.toInt(), type = "Stage Change", description = "Pipeline stage changed: New Lead ➔ Contacted", timestamp = now - 10 * oneDayMs))
        dao.insertInteraction(Interaction(leadId = id7.toInt(), type = "Stage Change", description = "Pipeline stage changed: Contacted ➔ Lost", timestamp = now - 5 * oneDayMs))
    }
}
