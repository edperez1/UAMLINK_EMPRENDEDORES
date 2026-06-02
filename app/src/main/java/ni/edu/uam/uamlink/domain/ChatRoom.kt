package ni.edu.uam.uamlink.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChatRoom(
    val id: String,                    // ID único de la sala (UUID)
    val producto_id: Long,             // ID del producto por el cual están negociando
    val comprador_id: String,          // ID del estudiante interesado
    val vendedor_id: String,           // ID del estudiante dueño del producto
    val nombre_producto: String? = null, // Auxiliar para mostrar el título en la UI
    val nombre_interlocutor: String? = null // Auxiliar para mostrar el nombre del otro usuario
)