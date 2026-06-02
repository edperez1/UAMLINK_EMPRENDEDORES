package ni.edu.uam.uamlink.domain

import kotlinx.serialization.Serializable

@Serializable
data class Mensaje(
    val id: Long? = null,              // Autogenerado por Supabase (Primary Key)
    val chat_room_id: String,          // Vinculado a la sala de chat
    val remitente_id: String,          // ID del estudiante que envía el mensaje
    val contenido: String,             // El texto del mensaje
    val created_at: String? = null     // Fecha y hora autogenerada por la BD
)