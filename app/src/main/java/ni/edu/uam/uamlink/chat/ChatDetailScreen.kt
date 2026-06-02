package ni.edu.uam.uamlink.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest
import ni.edu.uam.uamlink.core.data.SupabaseNetwork
import ni.edu.uam.uamlink.domain.ChatRoom
import ni.edu.uam.uamlink.domain.Mensaje
import ni.edu.uam.uamlink.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// ACTUALIZACIÓN: Se agregó miUsuarioId a la firma de la función
fun ChatDetailScreen(
    chatRoom: ChatRoom,
    miUsuarioId: String,
    onBackClick: () -> Unit
) {
    val mensajes = remember { mutableStateListOf<Mensaje>() }
    var nuevoMensajeTexto by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Cargar el historial de mensajes de esta sala desde Supabase
    LaunchedEffect(chatRoom.id) {
        try {
            val mensajesDesdeBD = SupabaseNetwork.client.postgrest["mensajes"]
                .select {
                    filter {
                        Mensaje::chat_room_id eq chatRoom.id
                    }
                }.decodeList<Mensaje>()

            mensajes.clear()
            mensajes.addAll(mensajesDesdeBD)

            if (mensajes.isNotEmpty()) {
                listState.scrollToItem(mensajes.size - 1)
            }
        } catch (e: Exception) {
            println("Error al cargar mensajes: ${e.message}")
        }
    }

    // LÓGICA DE ENVÍO CENTRALIZADA
    val enviarMensajeAccion = {
        if (nuevoMensajeTexto.isNotBlank()) {
            val textoAEnviar = nuevoMensajeTexto
            nuevoMensajeTexto = ""

            coroutineScope.launch {
                try {
                    val mensajeObj = Mensaje(
                        chat_room_id = chatRoom.id,
                        remitente_id = miUsuarioId,
                        contenido = textoAEnviar
                    )

                    SupabaseNetwork.client.postgrest["mensajes"].insert(mensajeObj)

                    mensajes.add(mensajeObj)
                    listState.animateScrollToItem(mensajes.size - 1)
                } catch (e: Exception) {
                    println("Error al enviar mensaje: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chatRoom.nombre_interlocutor ?: "Estudiante UAM", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(chatRoom.nombre_producto ?: "Artículo", color = UAMGreen, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                color = Color(0xFF1E1E1E)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nuevoMensajeTexto,
                        onValueChange = { nuevoMensajeTexto = it },
                        placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            enviarMensajeAccion()
                            focusManager.clearFocus()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UAMGreen,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { enviarMensajeAccion() },
                        modifier = Modifier
                            .background(UAMGreen, CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.Black)
                    }
                }
            }
        },
        containerColor = UAMBackground
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(UAMBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mensajes) { mensaje ->
                val esMio = mensaje.remitente_id == miUsuarioId
                MessageBubble(mensaje = mensaje, esMio = esMio)
            }
        }
    }
}

@Composable
fun MessageBubble(mensaje: Mensaje, esMio: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (esMio) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (esMio) UAMGreen else Color(0xFF1E1E1E),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (esMio) 16.dp else 0.dp,
                bottomEnd = if (esMio) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = mensaje.contenido,
                color = if (esMio) Color.Black else Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}