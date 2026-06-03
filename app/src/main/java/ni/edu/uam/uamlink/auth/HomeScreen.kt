package ni.edu.uam.uamlink.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ni.edu.uam.uamlink.components.UAMTextField
import ni.edu.uam.uamlink.domain.Producto
import ni.edu.uam.uamlink.domain.ChatRoom
import ni.edu.uam.uamlink.chat.ChatDetailScreen
import ni.edu.uam.uamlink.core.data.SupabaseNetwork
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import ni.edu.uam.uamlink.ui.theme.*

// Modelo local para simular la persistencia de mensajes dentro del historial
data class SimulatedMessage(val text: String, val isMe: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(isSeller: Boolean, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentIsSellerMode by remember { mutableStateOf(isSeller) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPublishSheet by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Producto?>(null) }
    var selectedProductToShow by remember { mutableStateOf<Producto?>(null) }

    var activeChatRoom by remember { mutableStateOf<ChatRoom?>(null) }
    var showUAMBot by remember { mutableStateOf(false) }
    var showOrderSuccessDialog by remember { mutableStateOf<Producto?>(null) }

    val productosPublicados = remember { mutableStateListOf<Producto>() }
    val comprasRealizadas = remember { mutableStateListOf<Producto>() }
    val ventasRealizadas = remember { mutableStateListOf<Producto>() }

    // Control de estados estilo PedidosYa (ID de Producto -> "En curso" o "Finalizado")
    val estadoPedidos = remember { mutableStateMapOf<Long, String>() }

    // Persistencia de los mensajes simulados indexados por el id de la sala de chat
    val conversacionesSimuladas = remember { mutableStateMapOf<String, List<SimulatedMessage>>() }

    val historialChats = remember { mutableStateListOf<ChatRoom>() }
    var hasNewNotification by remember { mutableStateOf(false) }
    var simulatedSellerNotification by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val miUsuarioId = SupabaseNetwork.client.auth.currentUserOrNull()?.id
        ?: "00000000-0000-0000-0000-000000000000"

    val handleLogout: () -> Unit = {
        coroutineScope.launch {
            try {
                SupabaseNetwork.client.auth.signOut()
            } catch (e: Exception) {
                println("Error al cerrar sesión: ${e.message}")
            } finally {
                onLogout()
            }
        }
    }

    val iniciarChat: (Producto) -> Unit = { producto ->
        val existente = historialChats.find { it.producto_id == producto.id }
        if (existente != null) {
            activeChatRoom = existente
        } else {
            val nuevaSalaChat = ChatRoom(
                id = java.util.UUID.randomUUID().toString(),
                producto_id = producto.id ?: 0L,
                comprador_id = miUsuarioId,
                vendedor_id = producto.vendedor_id,
                nombre_producto = producto.nombre,
                nombre_interlocutor = if (producto.vendedor_id == miUsuarioId) "Comprador UAM" else "Vendedor UAM"
            )
            activeChatRoom = nuevaSalaChat
            if (!historialChats.any { it.id == nuevaSalaChat.id }) {
                historialChats.add(0, nuevaSalaChat)
            }
            coroutineScope.launch {
                try {
                    SupabaseNetwork.client.postgrest["chat_rooms"].insert(nuevaSalaChat)
                } catch (e: Exception) {
                    println("Error al crear la sala de chat en BD: ${e.message}")
                }
            }
        }
        selectedProductToShow = null
        showOrderSuccessDialog = null
    }

    LaunchedEffect(Unit) {
        try {
            val productosDesdeBD = SupabaseNetwork.client.postgrest["productos"].select().decodeList<Producto>()
            productosPublicados.clear()
            productosPublicados.addAll(productosDesdeBD)

            val chatsDesdeBD = SupabaseNetwork.client.postgrest["chat_rooms"]
                .select { filter { or { eq("comprador_id", miUsuarioId); eq("vendedor_id", miUsuarioId) } } }
                .decodeList<ChatRoom>()
            historialChats.clear()
            historialChats.addAll(chatsDesdeBD)

        } catch (e: Exception) {
            println("Error al cargar datos del campus: ${e.message}")
        }
    }

    when {
        activeChatRoom != null -> {
            SimulatedBuyerChatScreen(
                chatRoom = activeChatRoom!!,
                conversaciones = conversacionesSimuladas,
                onBackClick = { activeChatRoom = null }
            )
        }
        showUAMBot -> {
            UAMBotScreen(onBackClick = { showUAMBot = false })
        }
        else -> {
            Scaffold(
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .shadow(8.dp, RoundedCornerShape(32.dp))
                            .clip(RoundedCornerShape(32.dp)),
                        color = Color(0xFF1E1E1E)
                    ) {
                        NavigationBar(containerColor = Color.Transparent, contentColor = Color.White, tonalElevation = 0.dp) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Store, contentDescription = "Mercado") },
                                label = { Text("Campus") },
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = UAMGreen, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Mensajes") },
                                label = { Text("Mensajes") },
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1; hasNewNotification = false },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = UAMGreen, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                                label = { Text("Perfil") },
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = UAMGreen, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(UAMBackground)) {
                    when (selectedTab) {
                        0 -> {
                            if (currentIsSellerMode) {
                                SellerDashboardContent(
                                    miUsuarioId = miUsuarioId,
                                    productos = productosPublicados,
                                    onBackToBuyer = { currentIsSellerMode = false },
                                    onOpenPublish = { productToEdit = null; showPublishSheet = true },
                                    onEditProduct = { prod -> productToEdit = prod; showPublishSheet = true },
                                    onDeleteProduct = { prod ->
                                        coroutineScope.launch {
                                            try {
                                                SupabaseNetwork.client.postgrest["productos"].delete { filter { eq("id", prod.id ?: 0L) } }
                                                productosPublicados.remove(prod)
                                            } catch(e: Exception) { println("Error eliminando") }
                                        }
                                    },
                                    onLogoutClick = handleLogout
                                )
                            } else {
                                BuyerMarketContent(
                                    productos = productosPublicados,
                                    hasNotification = hasNewNotification,
                                    onToggleMode = { currentIsSellerMode = true },
                                    onProductClick = { producto -> selectedProductToShow = producto },
                                    onLogoutClick = handleLogout
                                )
                            }
                        }
                        1 -> {
                            MessagesHistoryContent(
                                chats = historialChats,
                                onChatClick = { chatRoom -> activeChatRoom = chatRoom },
                                onBotClick = { showUAMBot = true }
                            )
                        }
                        2 -> {
                            ProfileContent(
                                compras = comprasRealizadas,
                                ventas = ventasRealizadas,
                                estadoPedidos = estadoPedidos,
                                onMarcarEntregado = { prod ->
                                    prod.id?.let { id -> estadoPedidos[id] = "Finalizado" }
                                },
                                onProductClick = { producto -> selectedProductToShow = producto },
                                onLogoutClick = handleLogout
                            )
                        }
                    }
                }

                if (simulatedSellerNotification != null && currentIsSellerMode) {
                    AlertDialog(
                        onDismissRequest = { simulatedSellerNotification = null },
                        containerColor = Color(0xFF1E1E1E),
                        title = { Text("¡Nueva Venta Detectada!", color = UAMGreen, fontWeight = FontWeight.Bold) },
                        text = { Text(simulatedSellerNotification!!, color = Color.White) },
                        confirmButton = {
                            Button(onClick = { simulatedSellerNotification = null }, colors = ButtonDefaults.buttonColors(containerColor = UAMGreen)) {
                                Text("Revisar Historial", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                if (showOrderSuccessDialog != null) {
                    val prod = showOrderSuccessDialog!!
                    AlertDialog(
                        onDismissRequest = { showOrderSuccessDialog = null },
                        containerColor = Color(0xFF1E1E1E),
                        titleContentColor = UAMGreen,
                        textContentColor = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        title = { Text("¡Pedido Confirmado!", fontWeight = FontWeight.Bold) },
                        text = { Text("Has solicitado exitosamente el artículo: ${prod.nombre}. El vendedor ha sido notificado y el pedido está En curso. ¿Deseas coordinar la entrega?") },
                        confirmButton = {
                            Button(onClick = { iniciarChat(prod) }, colors = ButtonDefaults.buttonColors(containerColor = UAMGreen)) {
                                Text("Contactar Vendedor", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOrderSuccessDialog = null }) { Text("Cerrar", color = Color.Gray) }
                        }
                    )
                }

                if (showPublishSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showPublishSheet = false; productToEdit = null },
                        sheetState = sheetState,
                        containerColor = Color(0xFF121212),
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
                    ) {
                        PublishProductForm(
                            productoAEditar = productToEdit,
                            onClose = { showPublishSheet = false; productToEdit = null },
                            onPublish = { nombre, precio, facultad, estado, entrega, uriString ->
                                coroutineScope.launch {
                                    try {
                                        if (productToEdit == null) {
                                            val nuevoProducto = Producto(
                                                vendedor_id = miUsuarioId,
                                                nombre = nombre, precio = precio,
                                                categoria = facultad, estado = estado,
                                                metodo_entrega = entrega, descripcion = uriString ?: "Sin imagen"
                                            )
                                            SupabaseNetwork.client.postgrest["productos"].insert(nuevoProducto)
                                            val refresh = SupabaseNetwork.client.postgrest["productos"].select().decodeList<Producto>()
                                            productosPublicados.clear()
                                            productosPublicados.addAll(refresh)
                                        } else {
                                            val prodActualizado = productToEdit!!.copy(
                                                nombre = nombre, precio = precio,
                                                categoria = facultad, estado = estado,
                                                metodo_entrega = entrega, descripcion = uriString ?: productToEdit!!.descripcion
                                            )
                                            SupabaseNetwork.client.postgrest["productos"].update(prodActualizado) {
                                                filter { eq("id", productToEdit!!.id ?: 0L) }
                                            }
                                            val index = productosPublicados.indexOfFirst { it.id == prodActualizado.id }
                                            if(index != -1) productosPublicados[index] = prodActualizado
                                        }
                                        showPublishSheet = false
                                        productToEdit = null
                                    } catch (e: Exception) {
                                        println("Error en BD: ${e.message}")
                                    }
                                }
                                Unit
                            }
                        )
                    }
                }

                if (selectedProductToShow != null) {
                    ModalBottomSheet(
                        onDismissRequest = { selectedProductToShow = null },
                        sheetState = sheetState,
                        containerColor = Color(0xFF1E1E1E),
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
                    ) {
                        ProductDetailsSheet(
                            producto = selectedProductToShow!!,
                            onClose = { selectedProductToShow = null },
                            onBuyProduct = {
                                val productoComprado = selectedProductToShow!!
                                coroutineScope.launch {
                                    try {
                                        SupabaseNetwork.client.postgrest["productos"].delete { filter { eq("id", productoComprado.id ?: 0L) } }
                                        productosPublicados.remove(productoComprado)
                                        comprasRealizadas.add(productoComprado)
                                        ventasRealizadas.add(productoComprado)

                                        val generatedId = productoComprado.id ?: java.util.UUID.randomUUID().mostSignificantBits
                                        estadoPedidos[generatedId] = "En curso"

                                        val nuevaSalaChat = ChatRoom(
                                            id = java.util.UUID.randomUUID().toString(),
                                            producto_id = generatedId,
                                            comprador_id = miUsuarioId,
                                            vendedor_id = productoComprado.vendedor_id,
                                            nombre_producto = productoComprado.nombre,
                                            nombre_interlocutor = "Vendedor UAM"
                                        )
                                        if (!historialChats.any { it.nombre_producto == productoComprado.nombre }) {
                                            historialChats.add(0, nuevaSalaChat)
                                        }

                                        simulatedSellerNotification = "Tu artículo '${productoComprado.nombre}' fue comprado por un estudiante. El estado del pedido se encuentra: EN CURSO."
                                        hasNewNotification = true

                                        selectedProductToShow = null
                                        showOrderSuccessDialog = productoComprado.copy(id = generatedId)
                                    } catch (e: Exception) { println("Error al procesar compra: ${e.message}") }
                                }
                            },
                            onContactSeller = { iniciarChat(selectedProductToShow!!) }
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// SISTEMA DE CHAT SIMULADO CON BOTONES DE RESPUESTA RÁPIDA
// --------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatedBuyerChatScreen(
    chatRoom: ChatRoom,
    conversaciones: MutableMap<String, List<SimulatedMessage>>,
    onBackClick: () -> Unit
) {
    val messages = conversaciones[chatRoom.id] ?: listOf(
        SimulatedMessage("¡Hola! Me interesa el artículo: ${chatRoom.nombre_producto}. ¿Sigue disponible? ¿Dónde nos vemos?", false)
    )

    var currentMessages by remember(chatRoom.id) { mutableStateOf(messages) }

    val quickReplies = listOf(
        "¡Hola! Sí, hoy en la cafetería",
        "Mañana en la biblioteca",
        "Escríbeme por WhatsApp",
        "Solo acepto transferencias"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatRoom.nombre_interlocutor ?: "Contacto UAM", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = UAMGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF1E1E1E)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = false) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                items(currentMessages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (msg.isMe) UAMGreen else Color(0xFF2C2C2C),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (msg.isMe) Color.Black else Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF121212)).padding(12.dp)) {
                Text("Respuestas Rápidas", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickReplies) { reply ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val updatedWithMe = currentMessages + SimulatedMessage(reply, true)
                                val respuestaBot = when {
                                    reply.contains("cafetería") -> "Dale de viaje, ahí te espero en la cafetería."
                                    reply.contains("biblioteca") -> "Perfecto, me avisas cuando estés por los cubículos."
                                    reply.contains("WhatsApp") -> "Dale, te paso mi número por interno."
                                    else -> "Entendido, sin problema. Quedamos así."
                                }
                                val finalMessages = updatedWithMe + SimulatedMessage(respuestaBot, false)
                                currentMessages = finalMessages
                                conversaciones[chatRoom.id] = finalMessages
                            },
                            label = { Text(reply, color = Color.White) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF2C2C2C)),
                            shape = CircleShape
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// FORMULARIO DE PUBLICACIÓN DE PRODUCTOS
// --------------------------------------------------------
@Composable
fun PublishProductForm(
    productoAEditar: Producto?,
    onClose: () -> Unit,
    onPublish: (String, Double, String, String, String, String?) -> Unit
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var productName by remember { mutableStateOf(productoAEditar?.nombre ?: "") }
    var productPrice by remember { mutableStateOf(productoAEditar?.precio?.toString() ?: "") }
    var selectedFaculty by remember { mutableStateOf(productoAEditar?.categoria ?: "Ingeniería") }
    var selectedCondition by remember { mutableStateOf(productoAEditar?.estado ?: "Como nuevo") }
    var deliveryMethod by remember { mutableStateOf(productoAEditar?.metodo_entrega ?: "Entrega a mano") }
    var showError by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if(productoAEditar == null) "Publicar Nuevo Artículo" else "Editar Artículo", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                .border(1.dp, if (selectedImageUri != null) UAMGreen else UAMGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .clickable { photoPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Text("✅ Imagen Seleccionada", color = UAMGreen, fontWeight = FontWeight.Bold)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = UAMGreen, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Toca para subir una foto de tu galería", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = productName, onValueChange = { productName = it; showError = false }, label = { Text("¿Qué vas a vender?", color = Color.Gray) },
            singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UAMGreen, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            isError = showError && productName.isBlank()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = productPrice, onValueChange = { productPrice = it; showError = false }, label = { Text("Precio (C$)", color = Color.Gray) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UAMGreen, unfocusedBorderColor = Color.DarkGray, focusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            isError = showError && productPrice.isBlank()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Categoría / Facultad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val faculties = listOf("Ingeniería", "Medicina", "Odontología", "Arte", "Derecho")
            items(faculties) { faculty ->
                FilterChip(selected = selectedFaculty == faculty, onClick = { selectedFaculty = faculty }, label = { Text(faculty, color = if (selectedFaculty == faculty) Color.Black else Color.LightGray) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1E1E1E), selectedContainerColor = UAMGreen), shape = CircleShape)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Estado del producto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val conditions = listOf("Nuevo", "Como nuevo", "Buen estado", "Usado")
            items(conditions) { condition ->
                FilterChip(selected = selectedCondition == condition, onClick = { selectedCondition = condition }, label = { Text(condition, color = if (selectedCondition == condition) Color.Black else Color.LightGray) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1E1E1E), selectedContainerColor = UAMGreen), shape = CircleShape)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Método de entrega", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val methods = listOf("Entrega a mano", "Punto acordado", "Por envío")
            items(methods) { method ->
                FilterChip(selected = deliveryMethod == method, onClick = { deliveryMethod = method }, label = { Text(method, color = if (deliveryMethod == method) Color.Black else Color.LightGray) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1E1E1E), selectedContainerColor = UAMGreen), shape = CircleShape)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                val precioNum = productPrice.toDoubleOrNull()
                if (productName.isNotBlank() && precioNum != null) {
                    onPublish(productName, precioNum, selectedFaculty, selectedCondition, deliveryMethod, selectedImageUri?.toString())
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = UAMGreen), shape = CircleShape
        ) {
            Text(if(productoAEditar == null) "Publicar Artículo" else "Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --------------------------------------------------------
// PANEL DE ADMINISTRACIÓN DEL VENDEDOR
// --------------------------------------------------------
@Composable
fun SellerDashboardContent(
    miUsuarioId: String,
    productos: List<Producto>,
    onBackToBuyer: () -> Unit,
    onOpenPublish: () -> Unit,
    onEditProduct: (Producto) -> Unit,
    onDeleteProduct: (Producto) -> Unit,
    onLogoutClick: () -> Unit
) {
    val misProductos = productos.filter { it.vendedor_id == miUsuarioId }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenPublish, containerColor = UAMGreen, shape = CircleShape, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Publicar Nuevo", tint = Color.Black)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToBuyer) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ventas en Campus", color = UAMGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Tus Publicaciones Activas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (misProductos.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(80.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(20.dp)).clickable { onOpenPublish() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = UAMGreen, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No tienes artículos a la venta aún", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(misProductos) { prod ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(60.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                    Text("C$ ${prod.precio}", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                IconButton(onClick = { onEditProduct(prod) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                                }
                                IconButton(onClick = { onDeleteProduct(prod) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// MERCADO (COMPRADOR), MENSAJES Y COMPONENTES
// --------------------------------------------------------
@Composable
fun BuyerMarketContent(
    productos: List<Producto>,
    hasNotification: Boolean,
    onToggleMode: () -> Unit,
    onProductClick: (Producto) -> Unit,
    onLogoutClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf<String?>(null) }
    var selectedFaculty by remember { mutableStateOf("Todos") }

    val productosFiltrados = remember(productos, searchQuery, selectedCondition, selectedFaculty) {
        productos.filter { prod ->
            val matchesSearch = prod.nombre.contains(searchQuery, ignoreCase = true)
            if (searchQuery.isNotBlank()) {
                matchesSearch
            } else {
                val matchesFaculty = selectedFaculty == "Todos" || prod.categoria.trim().contains(selectedFaculty.trim(), ignoreCase = true)
                val matchesCondition = selectedCondition == null || selectedCondition == "Precio Máx" || prod.estado.trim().equals(selectedCondition!!.trim(), ignoreCase = true)
                matchesFaculty && matchesCondition
            }
        }.let { listaFiltrada ->
            if (selectedCondition == "Precio Máx" && searchQuery.isBlank()) listaFiltrada.sortedBy { it.precio } else listaFiltrada
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mercado Campus Link", color = UAMGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasNotification) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Alerta", tint = UAMGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onToggleMode) { Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar Modo", tint = UAMGreen) }
                    IconButton(onClick = onLogoutClick) { Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f)) }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { UAMTextField(label = "Buscar tu próximo artículo UAM...", value = searchQuery, onValueChange = { searchQuery = it }) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Filtrar por:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val conditions = listOf("Precio Máx", "Nuevo", "Como nuevo", "Buen estado", "Usado")
                items(conditions) { condition ->
                    FilterChip(selected = selectedCondition == condition, onClick = { selectedCondition = if (selectedCondition == condition) null else condition }, label = { Text(condition, color = if (selectedCondition == condition) Color.Black else Color.White) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1E1E1E), selectedContainerColor = UAMGreen), shape = CircleShape)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Categorías de Facultad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val faculties = listOf("Todos", "Medicina", "Odontología", "Ingeniería", "Arte", "Derecho")
                items(faculties) { faculty ->
                    FilterChip(selected = selectedFaculty == faculty, onClick = { selectedFaculty = faculty }, label = { Text(faculty, color = if (selectedFaculty == faculty) Color.Black else Color.LightGray) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1E1E1E), selectedContainerColor = UAMGreen), shape = CircleShape)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item { Text("Productos Destacados", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (productos.isEmpty()) {
                    items(2) { index -> FeaturedProductPlaceholder(index) }
                } else {
                    items(productos.take(4)) { producto -> FeaturedProductCard(producto, onClick = { onProductClick(producto) }) }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item { Text("Todos los artículos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        if (productosFiltrados.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay artículos que coincidan con los filtros", color = Color.Gray)
                }
            }
        } else {
            items(productosFiltrados) { producto -> ProductListItem(producto, onClick = { onProductClick(producto) }) }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun MessagesHistoryContent(chats: List<ChatRoom>, onChatClick: (ChatRoom) -> Unit, onBotClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item { Text("Tus Mensajes", color = UAMGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { onBotClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), border = BorderStroke(1.dp, UAMGreen.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).background(UAMGreen.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Text("🤖", fontSize = 24.sp) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("UAMBot", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Asistente de Campus Link", color = UAMGreen, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Conversaciones Recientes", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        if (chats.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aún no tienes mensajes directos", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            items(chats) { chat ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onChatClick(chat) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(Color(0xFF2C2C2C), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = chat.nombre_interlocutor ?: "Usuario", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Sobre: ${chat.nombre_producto}", color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UAMBotScreen(onBackClick: () -> Unit) {
    data class BotMsg(val text: String, val isUser: Boolean)
    val messages = remember { mutableStateListOf(BotMsg("¡Hola! Soy UAMBot 🤖. ¿En qué te puedo ayudar hoy con Campus Link?", false)) }
    val commonQuestions = listOf("¿Cómo publico un artículo?", "¿Es seguro comprar aquí?", "¿Dónde se entrega el producto?")

    Scaffold(
        topBar = { TopAppBar(title = { Text("UAMBot Asistente", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = UAMGreen) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))) },
        containerColor = UAMBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = false) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                items(messages) { msg ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                        Box(modifier = Modifier.background(color = if (msg.isUser) UAMGreen else Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp)).padding(12.dp).widthIn(max = 280.dp)) {
                            Text(text = msg.text, color = if (msg.isUser) Color.Black else Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
            LazyRow(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commonQuestions) { question ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            messages.add(BotMsg(question, true))
                            val respuesta = when(question) {
                                "¿Cómo publico un artículo?" -> "Ve a la pestaña 'Campus', cambia al modo vendedor (ícono de flechas arriba) y presiona '+'."
                                "¿Es seguro comprar aquí?" -> "¡Sí! Las entregas se hacen dentro de la seguridad del campus de la UAM."
                                else -> "Recomendamos acordar las entregas cerca de la cafetería o la biblioteca."
                            }
                            messages.add(BotMsg(respuesta, false))
                        },
                        label = { Text(question, color = Color.White) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1E1E1E)), shape = CircleShape
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailsSheet(producto: Producto, onClose: () -> Unit, onBuyProduct: () -> Unit, onContactSeller: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(text = producto.nombre, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(text = "C$ ${producto.precio}", color = UAMGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(producto.categoria)
            InfoChip(producto.estado)
        }
        Spacer(modifier = Modifier.height(8.dp))
        InfoChip(icon = Icons.Default.LocationOn, text = producto.metodo_entrega)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Reseña del vendedor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = producto.descripcion ?: "Este artículo no tiene una descripción detallada.", color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBuyProduct, modifier = Modifier.weight(1.2f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = UAMGreen), shape = CircleShape) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pedir", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Button(onClick = onContactSeller, modifier = Modifier.weight(1f).height(50.dp).border(1.5.dp, UAMGreen, CircleShape), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = CircleShape) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = UAMGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chat", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun InfoChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(modifier = Modifier.background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)) }
        Text(text, color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
fun ProductListItem(producto: Producto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(producto.categoria, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("C$ ${producto.precio}", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun FeaturedProductCard(producto: Producto, onClick: () -> Unit) {
    Card(modifier = Modifier.width(150.dp).height(200.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(producto.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text("C$ ${producto.precio}", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun FeaturedProductPlaceholder(index: Int) {
    Card(modifier = Modifier.width(150.dp).height(200.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Artículo UAM ${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("C$ 250.00", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// --------------------------------------------------------
// MODIFICADO: PERFIL DEL ESTUDIANTE CON HISTORIALES ESTILO PEDIDOS YA
// --------------------------------------------------------
@Composable
fun ProfileContent(
    compras: List<Producto>,
    ventas: List<Producto>,
    estadoPedidos: Map<Long, String>,
    onMarcarEntregado: (Producto) -> Unit,
    onProductClick: (Producto) -> Unit,
    onLogoutClick: () -> Unit
) {
    val totalGastado = remember(compras.size) { compras.sumOf { it.precio } }
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Compras, 1 = Ventas

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mi Perfil UAM", color = UAMGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Box(modifier = Modifier.size(90.dp).background(Color(0xFF1E1E1E), CircleShape).border(2.dp, UAMGreen, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { Text("Estudiante UAM", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item { Text("Comunidad Campus Link v1.6", color = Color.Gray, fontSize = 12.sp) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).background(UAMGreen.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.History, contentDescription = null, tint = UAMGreen)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Total Invertido en Campus", color = Color.Gray, fontSize = 13.sp)
                        Text("C$ $totalGastado", color = UAMGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PESTAÑAS DE NAVEGACIÓN INTERNA PARA EL HISTORIAL
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF121212), RoundedCornerShape(12.dp)).padding(4.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == 0) UAMGreen else Color.Transparent)
                        .clickable { activeSubTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Mis Compras", color = if (activeSubTab == 0) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == 1) UAMGreen else Color.Transparent)
                        .clickable { activeSubTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Mis Ventas (Pedidos)", color = if (activeSubTab == 1) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // VISTA SELECCIONADA: HISTORIAL DE COMPRAS
        if (activeSubTab == 0) {
            if (compras.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("¡Aún no has solicitado productos!", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(compras) { articulo ->
                    val estado = estadoPedidos[articulo.id] ?: "En curso"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onProductClick(articulo) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(articulo.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(if (estado == "En curso") Color(0xFFFFB300) else UAMGreen, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(estado, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("C$ ${articulo.precio}", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // VISTA SELECCIONADA: HISTORIAL DE VENTAS CON BOTÓN PARA MARCAR ENTREGA
        else {
            if (ventas.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nadie ha comprado tus productos todavía", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(ventas) { articulo ->
                    val estado = estadoPedidos[articulo.id] ?: "En curso"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(50.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(articulo.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(if (estado == "En curso") Color(0xFFFFB300) else UAMGreen, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(estado, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("C$ ${articulo.precio}", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (estado == "En curso") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onMarcarEntregado(articulo) },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = UAMGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Marcar como Entregado", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}