package com.jeremy.lumi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    // Más reciente primero — es como se lee un chat normal (recientes abajo
    // en la UI, pero la query trae descendente para luego invertir o usar
    // reverseLayout en el LazyColumn, según prefieras en la pantalla)
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("UPDATE chat_messages SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM chat_messages WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)
}