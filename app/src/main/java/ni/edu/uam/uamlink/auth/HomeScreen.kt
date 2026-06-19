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
import androidx.compose.ui.text.style.TextOverflow
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

// 🔴 1. AQUI ESTAN LOS 6 PRODUCTOS ACADÉMICOS POR DEFAULT
val productosDestacadosDefault = listOf(
    Producto(
        id = -1L,
        vendedor_id = "vendedor_uam_1",
        nombre = "Calculadora Científica",
        precio = 850.0,
        categoria = "Ingeniería",
        estado = "Como nuevo",
        metodo_entrega = "Entrega a mano",
        descripcion = "calculadora|Calculadora fx-991LA ideal para las clases de cálculo y física avanzada."
    ),
    Producto(
        id = -2L,
        vendedor_id = "vendedor_uam_2",
        nombre = "Kit de Odontologia",
        precio = 1200.0,
        categoria = "Odontología",
        estado = "Nuevo",
        metodo_entrega = "Punto acordado",
        descripcion = "kitodonto|Kit de exploración completo, acero inoxidable, alta durabilidad."
    ),
    Producto(
        id = -3L,
        vendedor_id = "vendedor_uam_3",
        nombre = "Libro de Anatomía",
        precio = 900.0,
        categoria = "Medicina",
        estado = "Buen estado",
        metodo_entrega = "Entrega a mano",
        descripcion = "libroanatomia|Atlas de Anatomía Humana de Netter. Edición con notas de estudio."
    ),
    Producto(
        id = -4L,
        vendedor_id = "vendedor_uam_4",
        nombre = "Guía de Física",
        precio = 450.0,
        categoria = "Ingeniería",
        estado = "Nuevo",
        metodo_entrega = "Por envío",
        descripcion = "librofisica|Guía completa de ejercicios resueltos para laboratorio de Física I y II."
    ),
    Producto(
        id = -5L,
        vendedor_id = "vendedor_uam_5",
        nombre = "Código Civil",
        precio = 300.0,
        categoria = "Derecho",
        estado = "Usado",
        metodo_entrega = "Entrega a mano",
        descripcion = "codigocivil|Código Civil actualizado, ideal para estudiantes de primer año."
    ),
    Producto(
        id = -6L,
        vendedor_id = "vendedor_uam_6",
        nombre = "Bata de Laboratorio",
        precio = 350.0,
        categoria = "Medicina",
        estado = "Como nuevo",
        metodo_entrega = "Punto acordado",
        descripcion = "bata|Bata blanca manga larga talla M, usada un solo semestre."
    )
)

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

    val estadoPedidos = remember { mutableStateMapOf<Long, String>() }

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
                nombre_interlocutor = "Vendedor UAM"
            )
            activeChatRoom = nuevaSalaChat

            if (!historialChats.any { it.id == nuevaSalaChat.id }) {
                historialChats.add(0, nuevaSalaChat)
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
                        color = Color.White
                    ) {
                        NavigationBar(containerColor = Color.Transparent, contentColor = TextPrimary, tonalElevation = 0.dp) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Store, contentDescription = "Mercado") },
                                label = { Text("Campus") },
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = VividGreen, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Mensajes") },
                                label = { Text("Mensajes") },
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = VividGreen, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                icon = {
                                    BadgedBox(badge = { if (hasNewNotification) Badge { Text("!") } }) {
                                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                                    }
                                },
                                label = { Text("Perfil") },
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2; hasNewNotification = false },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = VividGreen, unselectedIconColor = Color.Gray, indicatorColor = Color.Transparent)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(LightGreenBg)) {
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
                                onMarcarEntregado = { prod -> prod.id?.let { id -> estadoPedidos[id] = "Finalizado" } },
                                onProductClick = { producto -> selectedProductToShow = producto },
                                onLogoutClick = handleLogout
                            )
                        }
                    }
                }

                if (simulatedSellerNotification != null && currentIsSellerMode) {
                    AlertDialog(
                        onDismissRequest = { simulatedSellerNotification = null },
                        containerColor = Color.White,
                        title = { Text("¡Nueva Venta Detectada!", color = VividGreen, fontWeight = FontWeight.Bold) },
                        text = { Text(simulatedSellerNotification!!, color = TextPrimary) },
                        confirmButton = {
                            Button(onClick = { simulatedSellerNotification = null }, colors = ButtonDefaults.buttonColors(containerColor = VividGreen)) {
                                Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                if (showOrderSuccessDialog != null) {
                    val prod = showOrderSuccessDialog!!
                    AlertDialog(
                        onDismissRequest = { showOrderSuccessDialog = null },
                        containerColor = Color.White,
                        titleContentColor = VividGreen,
                        textContentColor = TextPrimary,
                        shape = RoundedCornerShape(24.dp),
                        title = { Text("¡Pedido Confirmado!", fontWeight = FontWeight.Bold) },
                        text = { Text("Has solicitado exitosamente el artículo: ${prod.nombre}. El vendedor ha sido notificado y el pedido está En curso. ¿Deseas coordinar la entrega?") },
                        confirmButton = {
                            Button(onClick = { iniciarChat(prod) }, colors = ButtonDefaults.buttonColors(containerColor = VividGreen)) {
                                Text("Contactar Vendedor", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOrderSuccessDialog = null }) { Text("Cerrar", color = TextSecondary) }
                        }
                    )
                }

                if (showPublishSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showPublishSheet = false; productToEdit = null },
                        sheetState = sheetState,
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
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
                        containerColor = Color.White,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
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

                                        val pId = productoComprado.id ?: 0L
                                        estadoPedidos[pId] = "En curso"

                                        simulatedSellerNotification = "Tu artículo '${productoComprado.nombre}' fue pedido por un estudiante. El estado del pedido se encuentra: EN CURSO."
                                        hasNewNotification = true

                                        selectedProductToShow = null
                                        showOrderSuccessDialog = productoComprado
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
// SISTEMA DE CHAT SIMULADO CON ROLES CORREGIDOS (COMPRADOR)
// --------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatedBuyerChatScreen(
    chatRoom: ChatRoom,
    conversaciones: MutableMap<String, List<SimulatedMessage>>,
    onBackClick: () -> Unit
) {
    val messages = conversaciones[chatRoom.id] ?: listOf(
        SimulatedMessage("¡Hola! Me interesa tu artículo: ${chatRoom.nombre_producto}. ¿Sigue disponible para entrega?", true),
        SimulatedMessage("¡Hola! Sí, claro. Sigue disponible. ¿Dónde te gustaría que te lo entregue?", false)
    )

    var currentMessages by remember(chatRoom.id) { mutableStateOf(messages) }

    val quickReplies = listOf(
        "Hoy en la cafetería",
        "Mañana en la biblioteca",
        "Te escribo por WhatsApp",
        "¿Aceptas transferencia?"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatRoom.nombre_interlocutor ?: "Vendedor UAM", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = VividGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = LightGreenBg
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
                                    color = if (msg.isMe) VividGreen else Color.White,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (msg.isMe) Color.White else TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                Text("Tus Respuestas Rápidas", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickReplies) { reply ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val updatedWithMe = currentMessages + SimulatedMessage(reply, true)
                                val respuestaBot = when {
                                    reply.contains("cafetería") -> "¡Nítido! Dale de viaje, ahí nos vemos en la cafetería."
                                    reply.contains("biblioteca") -> "Perfecto, me avisas cuando estés por los cubículos."
                                    reply.contains("WhatsApp") -> "Dale, mi número es 8888-8888. Escribime."
                                    reply.contains("transferencia") -> "Sí claro, acepto transferencias sin problema."
                                    else -> "Entendido, sin problema. Quedamos así."
                                }
                                val finalMessages = updatedWithMe + SimulatedMessage(respuestaBot, false)
                                currentMessages = finalMessages
                                conversaciones[chatRoom.id] = finalMessages
                            },
                            label = { Text(reply, color = TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFF5F5F5)),
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
        Text(if(productoAEditar == null) "Publicar Nuevo Artículo" else "Editar Artículo", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp))
                .border(1.dp, if (selectedImageUri != null) VividGreen else VividGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .clickable { photoPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Text("✅ Imagen Seleccionada", color = VividGreen, fontWeight = FontWeight.Bold)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = VividGreen, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Toca para subir una foto de tu galería", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = productName, onValueChange = { productName = it; showError = false }, label = { Text("¿Qué vas a vender?", color = TextSecondary) },
            singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VividGreen, unfocusedBorderColor = Color.LightGray, focusedTextColor = TextPrimary),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            isError = showError && productName.isBlank()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = productPrice, onValueChange = { productPrice = it; showError = false }, label = { Text("Precio (C$)", color = TextSecondary) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VividGreen, unfocusedBorderColor = Color.LightGray, focusedTextColor = TextPrimary),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            isError = showError && productPrice.isBlank()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Categoría / Facultad", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val faculties = listOf("Ingeniería", "Medicina", "Odontología", "Arte", "Derecho")
            items(faculties) { faculty ->
                FilterChip(selected = selectedFaculty == faculty, onClick = { selectedFaculty = faculty }, label = { Text(faculty, color = if (selectedFaculty == faculty) Color.White else TextPrimary) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, selectedContainerColor = VividGreen), shape = CircleShape)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Estado del producto", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val conditions = listOf("Nuevo", "Como nuevo", "Buen estado", "Usado")
            items(conditions) { condition ->
                FilterChip(selected = selectedCondition == condition, onClick = { selectedCondition = condition }, label = { Text(condition, color = if (selectedCondition == condition) Color.White else TextPrimary) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, selectedContainerColor = VividGreen), shape = CircleShape)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Método de entrega", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val methods = listOf("Entrega a mano", "Punto acordado", "Por envío")
            items(methods) { method ->
                FilterChip(selected = deliveryMethod == method, onClick = { deliveryMethod = method }, label = { Text(method, color = if (deliveryMethod == method) Color.White else TextPrimary) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, selectedContainerColor = VividGreen), shape = CircleShape)
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
            modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = VividGreen), shape = CircleShape
        ) {
            Text(if(productoAEditar == null) "Publicar Artículo" else "Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            FloatingActionButton(onClick = onOpenPublish, containerColor = VividGreen, shape = CircleShape, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Publicar Nuevo", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToBuyer) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary) }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ventas en Campus", color = VividGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Tus Publicaciones Activas", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (misProductos.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(80.dp).background(Color.White, RoundedCornerShape(20.dp)).clickable { onOpenPublish() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = VividGreen, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No tienes artículos a la venta aún", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(misProductos) { prod ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(60.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.nombre, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                    Text("C$ ${prod.precio}", color = VividGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                IconButton(onClick = { onEditProduct(prod) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Gray)
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
            val matchesSearch = searchQuery.isBlank() || prod.nombre.contains(searchQuery.trim(), ignoreCase = true)
            val matchesFaculty = selectedFaculty == "Todos" || prod.categoria.trim().contains(selectedFaculty.trim(), ignoreCase = true)
            val matchesCondition = selectedCondition == null || selectedCondition == "Precio Máx" || prod.estado.trim().equals(selectedCondition!!.trim(), ignoreCase = true)
            matchesSearch && matchesFaculty && matchesCondition
        }.let { listaFiltrada ->
            if (selectedCondition == "Precio Máx" && searchQuery.isBlank()) listaFiltrada.sortedBy { it.precio } else listaFiltrada
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mercado Campus Link", color = VividGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasNotification) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Alerta", tint = VividGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onToggleMode) { Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar Modo", tint = VividGreen) }
                    IconButton(onClick = onLogoutClick) { Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f)) }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { UAMTextField(label = "Buscar tu próximo artículo UAM...", value = searchQuery, onValueChange = { searchQuery = it }) }
        item { Spacer(modifier = Modifier.height(24.dp)) }

        item { Text("Filtrar por:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val conditions = listOf("Precio Máx", "Nuevo", "Como nuevo", "Buen estado", "Usado")
                items(conditions) { condition ->
                    FilterChip(selected = selectedCondition == condition, onClick = { selectedCondition = if (selectedCondition == condition) null else condition }, label = { Text(condition, color = if (selectedCondition == condition) Color.White else TextPrimary) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, selectedContainerColor = VividGreen), shape = CircleShape)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Categorías de Facultad", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val faculties = listOf("Todos", "Medicina", "Odontología", "Ingeniería", "Arte", "Derecho")
                items(faculties) { faculty ->
                    FilterChip(selected = selectedFaculty == faculty, onClick = { selectedFaculty = faculty }, label = { Text(faculty, color = if (selectedFaculty == faculty) Color.White else TextPrimary) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, selectedContainerColor = VividGreen), shape = CircleShape)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }

        // 🔴 2. SECCIÓN DE PRODUCTOS DESTACADOS VINCULADA AL 'onClick'
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Productos Destacados", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Ver todos",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VividGreen,
                    modifier = Modifier.clickable { /* Acción opcional */ }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                // Iteramos la lista default académica y le pasamos tu función onProductClick
                items(productosDestacadosDefault) { producto ->
                    ProductoDestacadoCard(
                        producto = producto,
                        onClick = { onProductClick(producto) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
        item { Text("Todos los artículos", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        if (productosFiltrados.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay artículos que coincidan con los filtros", color = TextSecondary)
                }
            }
        } else {
            items(productosFiltrados) { producto -> ProductListItem(producto, onClick = { onProductClick(producto) }) }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

// 🔴 3. COMPONENTE VISUAL CON EL "COSITO DE PEDIR" Y LOGICA COMPLETA

@Composable
fun ProductoDestacadoCard(producto: Producto, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val datos = producto.descripcion?.split("|") ?: listOf("placeholder", "Sin descripción")
    val nombreImagen = datos.firstOrNull() ?: "placeholder"

    val imageResId = remember(nombreImagen) {
        context.resources.getIdentifier(nombreImagen, "drawable", context.packageName)
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFE0E0E0))
            ) {
                if (imageResId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = producto.categoria,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = producto.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = producto.estado,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "C$ ${producto.precio}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VividGreen
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(VividGreen, CircleShape)
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Pedir",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesHistoryContent(chats: List<ChatRoom>, onChatClick: (ChatRoom) -> Unit, onBotClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item { Text("Tus Mensajes", color = VividGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth().clickable { onBotClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).background(VividGreen.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Text("🤖", fontSize = 24.sp) }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("UAMBot", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Asistente de Campus Link", color = VividGreen, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { Text("Conversaciones Recientes", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        if (chats.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aún no tienes mensajes directos", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            items(chats) { chat ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onChatClick(chat) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(Color(0xFFF5F5F5), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = chat.nombre_interlocutor ?: "Usuario", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Sobre: ${chat.nombre_producto}", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
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

    val messages = remember { mutableStateListOf(BotMsg("¡Hola! Soy UAMBot 🤖.\n¿En qué te puedo ayudar hoy con Campus Link?", false)) }
    val commonQuestions = listOf("¿Cómo publico un artículo?", "¿Es seguro comprar aquí?", "¿Dónde se entrega el producto?")

    Scaffold(
        topBar = { TopAppBar(title = { Text("UAMBot Asistente", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = VividGreen) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) },
        containerColor = LightGreenBg
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = false) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                items(messages) { msg ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                        Box(modifier = Modifier.background(color = if (msg.isUser) VividGreen else Color.White, shape = RoundedCornerShape(16.dp)).padding(12.dp).widthIn(max = 280.dp)) {
                            Text(text = msg.text, color = if (msg.isUser) Color.White else TextPrimary, fontSize = 14.sp)
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
                        label = { Text(question, color = TextPrimary) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.White), shape = CircleShape
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailsSheet(producto: Producto, onClose: () -> Unit, onBuyProduct: () -> Unit, onContactSeller: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(text = producto.nombre, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(text = "C$ ${producto.precio}", color = VividGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(producto.categoria)
            InfoChip(producto.estado)
        }
        Spacer(modifier = Modifier.height(8.dp))
        InfoChip(icon = Icons.Default.LocationOn, text = producto.metodo_entrega)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Reseña del vendedor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = producto.descripcion ?: "Este artículo no tiene una descripción detallada.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBuyProduct, modifier = Modifier.weight(1.2f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = VividGreen), shape = CircleShape) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pedir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Button(onClick = onContactSeller, modifier = Modifier.weight(1f).height(50.dp).border(1.5.dp, VividGreen, CircleShape), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = CircleShape) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = VividGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chat", color = VividGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun InfoChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)) }
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun ProductListItem(producto: Producto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(producto.categoria, color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("C$ ${producto.precio}", color = VividGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// --------------------------------------------------------
// PERFIL DEL ESTUDIANTE CON HISTORIALES Y PAGOS COMENTADOS
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
                Text("Mi Perfil UAM", color = VividGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Box(modifier = Modifier.size(90.dp).background(Color.White, CircleShape).border(2.dp, VividGreen, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { Text("Estudiante UAM", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item { Text("Comunidad Campus Link v1.6", color = TextSecondary, fontSize = 12.sp) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).background(VividGreen.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.History, contentDescription = null, tint = VividGreen)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Total Invertido en Campus", color = TextSecondary, fontSize = 13.sp)
                        Text("C$ $totalGastado", color = VividGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PESTAÑAS DE NAVEGACIÓN INTERNA PARA EL HISTORIAL
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)).padding(4.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == 0) VividGreen else Color.Transparent)
                        .clickable { activeSubTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Mis Compras", color = if (activeSubTab == 0) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeSubTab == 1) VividGreen else Color.Transparent)
                        .clickable { activeSubTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Mis Ventas (Pedidos)", color = if (activeSubTab == 1) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // VISTA SELECCIONADA: HISTORIAL DE COMPRAS
        if (activeSubTab == 0) {
            if (compras.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("¡Aún no has solicitado productos!", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(compras) { articulo ->
                    val estado = estadoPedidos[articulo.id] ?: "En curso"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onProductClick(articulo) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(articulo.nombre, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(if (estado == "En curso") Color(0xFFFFB300) else VividGreen, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(estado, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("C$ ${articulo.precio}", color = VividGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nadie ha comprado tus productos todavía", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(ventas) { articulo ->
                    val estado = estadoPedidos[articulo.id] ?: "En curso"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(articulo.nombre, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(if (estado == "En curso") Color(0xFFFFB300) else VividGreen, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(estado, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("C$ ${articulo.precio}", color = VividGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (estado == "En curso") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onMarcarEntregado(articulo) },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VividGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Marcar como Entregado", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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