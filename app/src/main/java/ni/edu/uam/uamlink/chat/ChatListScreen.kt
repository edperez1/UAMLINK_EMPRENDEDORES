package ni.edu.uam.uamlink.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import ni.edu.uam.uamlink.core.data.SupabaseNetwork
import ni.edu.uam.uamlink.domain.ChatRoom
import ni.edu.uam.uamlink.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onChatClick: (ChatRoom) -> Unit) {
    val chatRooms = remember { mutableStateListOf<ChatRoom>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    // Cargar las salas de chat donde participa el usuario actual
    LaunchedEffect(Unit) {
        try {
            val userId = SupabaseNetwork.client.auth.currentUserOrNull()?.id
                ?: "00000000-0000-0000-0000-000000000000"

            // Traer chats donde soy el comprador OR donde soy el vendedor
            val salasDesdeBD = SupabaseNetwork.client.postgrest["chat_rooms"]
                .select {
                    filter {
                        or {
                            ChatRoom::comprador_id eq userId
                            ChatRoom::vendedor_id eq userId
                        }
                    }
                }.decodeList<ChatRoom>()

            chatRooms.clear()
            chatRooms.addAll(salasDesdeBD)
        } catch (e: Exception) {
            println("Error al cargar salas de chat: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mensajes Campus Link", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UAMBackground)
            )
        },
        containerColor = UAMBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(UAMBackground)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = UAMGreen
                )
            } else if (chatRooms.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No tienes conversaciones activas", color = Color.Gray, fontSize = 16.sp)
                    Text("Pregunta por un producto en el mercado", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatRooms) { sala ->
                        ChatRoomItem(chatRoom = sala, onClick = { onChatClick(sala) })
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomItem(chatRoom: ChatRoom, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Inicial o Avatar del producto/vendedor
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF2C2C2C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Store, contentDescription = null, tint = UAMGreen)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chatRoom.nombre_interlocutor ?: "Estudiante UAM",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Artículo: ${chatRoom.nombre_producto ?: "Producto"}",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}