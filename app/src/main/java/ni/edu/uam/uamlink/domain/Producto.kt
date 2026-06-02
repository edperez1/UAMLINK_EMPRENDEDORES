package ni.edu.uam.uamlink.domain

import kotlinx.serialization.Serializable

@Serializable
data class Producto(
    val id: Long? = null, // Se genera automáticamente en Supabase
    val vendedor_id: String, // El ID del estudiante que publica
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val categoria: String,
    val created_at: String? = null, // Fecha de creación automática
    val imagen_url: String? = null,
    val estado: String,
    val metodo_entrega: String
)