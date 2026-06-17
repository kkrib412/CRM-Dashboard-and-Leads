package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val company: String,
    val email: String,
    val phone: String,
    val dealValue: Double,
    val confidence: Int, // 0 to 100
    val pipelineStage: String, // "New Lead", "Contacted", "Proposal Sent", "In Negotiation", "Won", "Lost"
    val priority: String, // "Low", "Medium", "High"
    val notes: String = "",
    val nextFollowUpDate: Long = 0, // Date timestamp (0 if none)
    val lastInteractionDate: Long = System.currentTimeMillis(),
    val addedDate: Long = System.currentTimeMillis(),
    val stageChangedDate: Long = System.currentTimeMillis(),
    val channelPreference: String = "Email", // "Email", "Phone", "SMS", "LinkedIn"
    val ownerEmail: String = "" // Associated user
)

@Entity(tableName = "interactions")
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leadId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "Note", "Stage Change", "Follow Up Executed", "Created"
    val description: String,
    val detail: String = ""
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val businessName: String = "",
    val optInNotifications: Boolean = true
)

@Dao
interface CRMDao {
    @Query("SELECT * FROM leads ORDER BY addedDate DESC")
    fun getAllLeads(): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE ownerEmail = :ownerEmail ORDER BY addedDate DESC")
    fun getLeadsForUser(ownerEmail: String): Flow<List<Lead>>

    @Query("SELECT * FROM leads WHERE id = :id")
    suspend fun getLeadById(id: Int): Lead?

    @Query("SELECT * FROM leads ORDER BY addedDate DESC")
    suspend fun getAllLeadsList(): List<Lead>

    @Query("SELECT * FROM leads WHERE ownerEmail = :ownerEmail ORDER BY addedDate DESC")
    suspend fun getLeadsForUserList(ownerEmail: String): List<Lead>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead): Long

    @Update
    suspend fun updateLead(lead: Lead)

    @Delete
    suspend fun deleteLead(lead: Lead)

    @Query("DELETE FROM leads WHERE id IN (:ids)")
    suspend fun deleteLeadsBulk(ids: List<Int>)

    @Query("UPDATE leads SET pipelineStage = :newStage, stageChangedDate = :timestamp WHERE id IN (:ids)")
    suspend fun updateLeadsStageBulk(ids: List<Int>, newStage: String, timestamp: Long)

    @Query("SELECT * FROM interactions WHERE leadId = :leadId ORDER BY timestamp DESC")
    fun getInteractionsForLead(leadId: Int): Flow<List<Interaction>>

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC")
    fun getAllInteractions(): Flow<List<Interaction>>

    @Query("SELECT * FROM interactions WHERE leadId = :leadId ORDER BY timestamp DESC")
    suspend fun getInteractionsForLeadList(leadId: Int): List<Interaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: Interaction)

    @Query("DELETE FROM interactions WHERE leadId = :leadId")
    suspend fun deleteInteractionsForLead(leadId: Int)

    // User Operations
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}

@Database(entities = [Lead::class, Interaction::class, User::class], version = 2, exportSchema = false)
abstract class CRMDatabase : RoomDatabase() {
    abstract fun crmDao(): CRMDao
}
