package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val appearance: String,
    val personality: String,
    val background: String,
    val greeting: String,
    val avatarUri: String? = null, // URI representation of image/icon
    val isPredefined: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val tags: String = "", // e.g., "Anime, Yandere, Romance"
    val chatCount: String = "15K", // Simulated chat counts like OurDream AI
    val baseMemory: String = "" // Base persona memory block that can be imported/exported
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val characterId: Int, // references CharacterEntity (0 if Group Chat)
    val lastUpdated: Long = System.currentTimeMillis(),
    val sessionName: String = "Sesi Baru",
    val isGroup: Boolean = false,
    val groupName: String = "",
    val participantIds: String = "", // Comma-separated list of Character IDs (e.g. "1,2,3")
    val sessionMemory: String = "" // Session memory state to hold extracted memories or context
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int, // references ChatSessionEntity
    val role: String, // "user", "model", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentName: String? = null,
    val attachmentType: String? = null, // "image/*", "text/plain", "application/pdf"
    val attachmentBase64: String? = null, // For images or file text preview
    val senderName: String? = null, // Display name for group chat
    val senderAvatar: String? = null, // Avatar URI for group chat
    val isPinned: Boolean = false
)

@Entity(tableName = "app_settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val provider: String, // "GEMINI", "GROQ"
    val apiKey: String,
    val isAvailable: Boolean = true, // True if active / false if limit/exhausted
    val isUsed: Boolean = false // True if selected
)

// --- DAO (Data Access Object) ---

@Dao
interface RoleplayDao {
    // Characters
    @Query("SELECT * FROM characters ORDER BY isPredefined DESC, timestamp DESC")
    fun getAllCharactersFlow(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Int): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    // Chat Sessions
    @Query("SELECT * FROM chat_sessions ORDER BY lastUpdated DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE characterId = :characterId AND isGroup = 0 ORDER BY lastUpdated DESC")
    fun getSessionsForCharacterFlow(characterId: Int): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE characterId = :characterId AND isGroup = 0 ORDER BY lastUpdated DESC")
    suspend fun getSessionsForCharacter(characterId: Int): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE isGroup = 1 ORDER BY lastUpdated DESC")
    fun getGroupSessionsFlow(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE characterId = :characterId LIMIT 1")
    suspend fun getSessionByCharacterId(characterId: Int): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET lastUpdated = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionTimestamp(sessionId: Int, timestamp: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionIdFlow(sessionId: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesBySessionId(sessionId: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Int): ChatMessageEntity?

    @Query("UPDATE chat_messages SET isPinned = :isPinned WHERE id = :messageId")
    suspend fun updateMessagePinStatus(messageId: Int, isPinned: Boolean)

    @Query("SELECT id FROM chat_messages WHERE attachmentBase64 IS NOT NULL AND attachmentBase64 NOT LIKE 'local_attachment://%'")
    suspend fun getMessageIdsWithAttachments(): List<Int>

    @Query("SELECT substr(attachmentBase64, :offset, :length) FROM chat_messages WHERE id = :messageId")
    suspend fun getAttachmentChunk(messageId: Int, offset: Int, length: Int): String?

    @Query("UPDATE chat_messages SET attachmentBase64 = :newPath WHERE id = :messageId")
    suspend fun updateMessageAttachmentPath(messageId: Int, newPath: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Int)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND id > :messageId")
    suspend fun deleteMessagesAfterMessageId(sessionId: Int, messageId: Int)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySessionId(sessionId: Int)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE role = 'model' AND timestamp >= :sinceTimestamp")
    suspend fun countModelMessagesSince(sinceTimestamp: Long): Int

    // App Settings
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: SettingEntity)

    // API Keys
    @Query("SELECT * FROM api_keys WHERE provider = :provider ORDER BY id ASC")
    fun getApiKeysByProviderFlow(provider: String): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE provider = :provider ORDER BY id ASC")
    suspend fun getApiKeysByProvider(provider: String): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE id = :id")
    suspend fun getApiKeyById(id: Int): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyEntity): Long

    @Update
    suspend fun updateApiKey(apiKey: ApiKeyEntity)

    @Delete
    suspend fun deleteApiKey(apiKey: ApiKeyEntity)

    @Query("UPDATE api_keys SET isUsed = 0 WHERE provider = :provider")
    suspend fun clearActiveApiKeysForProvider(provider: String)

    @Query("UPDATE api_keys SET isUsed = 1 WHERE id = :id")
    suspend fun setApiKeyAsUsed(id: Int)

    @Query("UPDATE api_keys SET isAvailable = :isAvailable WHERE id = :id")
    suspend fun updateApiKeyAvailability(id: Int, isAvailable: Boolean)

    @Query("UPDATE api_keys SET isAvailable = 1")
    suspend fun resetAllApiKeysAvailability()
}

// --- Database ---

@Database(
    entities = [
        CharacterEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SettingEntity::class,
        ApiKeyEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class RoleplayDatabase : RoomDatabase() {
    abstract val roleplayDao: RoleplayDao

    companion object {
        @Volatile
        private var INSTANCE: RoleplayDatabase? = null

        fun getDatabase(context: Context): RoleplayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoleplayDatabase::class.java,
                    "roleplay_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
