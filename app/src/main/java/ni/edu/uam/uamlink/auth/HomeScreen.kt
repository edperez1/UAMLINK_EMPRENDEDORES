package ni.edu.uam.uamlink.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import ni.edu.uam.uamlink.core.data.SupabaseNetwork
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import ni.edu.uam.uamlink.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(isSeller: Boolean) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentIsSellerMode by remember { mutableStateOf(isSeller) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPublishSheet by remember { mutableStateOf(false) }

    // Estado reactivo para la UI
    val productosPublicados = remember { mutableStateListOf<Producto>() }
    val coroutineScope = rememberCoroutineScope()

    // LECTURA: Se ejecuta al abrir la pantalla para traer los datos de Supabase
    LaunchedEffect(Unit) {
        try {
            val productosDesdeBD = SupabaseNetwork.client.postgrest["productos"]
                .select()
                .decodeList<Producto>()

            productosPublicados.clear()
            productosPublicados.addAll(productosDesdeBD)
        } catch (e: Exception) {
            println("Error al cargar productos del campus: ${e.message}")
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(32.dp)),
                color = Color(0xFF1E1E1E)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Store, contentDescription = "Mercado") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UAMGreen,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UAMGreen,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(UAMBackground)
        ) {
            if (selectedTab == 0) {
                if (currentIsSellerMode) {
                    SellerDashboardContent(
                        onBackToBuyer = { currentIsSellerMode = false },
                        onOpenPublish = { showPublishSheet = true }
                    )
                } else {
                    BuyerMarketContent(
                        productos = productosPublicados,
                        onToggleMode = { currentIsSellerMode = true }
                    )
                }
            } else {
                ProfileContent()
            }
        }

        if (showPublishSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPublishSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF121212),
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                PublishProductForm(
                    onClose = { showPublishSheet = false },
                    onPublish = { nombre, precio, facultad, estado, entrega ->
                        coroutineScope.launch {
                            try {
                                // 1. Extraemos el usuario logueado o usamos uno por defecto de seguridad
                                val userId = SupabaseNetwork.client.auth.currentUserOrNull()?.id
                                    ?: "00000000-0000-0000-0000-000000000000"

                                // 2. Creamos el objeto basado en tu clase Producto
                                val nuevoProducto = Producto(
                                    vendedor_id = userId,
                                    nombre = nombre,
                                    precio = precio,
                                    categoria = facultad,
                                    estado = estado,
                                    metodo_entrega = entrega
                                )

                                // 3. ESCRITURA: Mandamos el producto a Supabase
                                SupabaseNetwork.client.postgrest["productos"].insert(nuevoProducto)

                                // 4. Actualizamos la interfaz instantáneamente
                                productosPublicados.add(nuevoProducto)
                                showPublishSheet = false
                                println("Producto insertado con éxito en Supabase")

                            } catch (e: Exception) {
                                println("Error al insertar producto: ${e.message}")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BuyerMarketContent(productos: List<Producto>, onToggleMode: () -> Unit) {
    val scrollState = rememberScrollState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf<String?>(null) }
    var selectedFaculty by remember { mutableStateOf("Todos") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mercado Campus Link", color = UAMGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleMode) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar Modo", tint = UAMGreen)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                UAMTextField(
                    label = "Buscar tu próximo artículo UAM...",
                    value = searchQuery,
                    onValueChange = { searchQuery = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Filtrar por:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val conditions = listOf("Precio Máx", "Nuevo", "Como nuevo", "Buen estado")
            items(conditions) { condition ->
                FilterChip(
                    selected = selectedCondition == condition,
                    onClick = { selectedCondition = if (selectedCondition == condition) null else condition },
                    label = { Text(condition, color = if (selectedCondition == condition) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E1E1E),
                        selectedContainerColor = UAMGreen
                    ),
                    shape = CircleShape
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Categorías de Facultad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val faculties = listOf("Todos", "Medicina y Odontología", "Libros", "Ingeniería", "Arte", "Arquitectura")
            items(faculties) { faculty ->
                FilterChip(
                    selected = selectedFaculty == faculty,
                    onClick = { selectedFaculty = faculty },
                    label = { Text(faculty, color = if (selectedFaculty == faculty) Color.Black else Color.LightGray) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E1E1E),
                        selectedContainerColor = UAMGreen
                    ),
                    shape = CircleShape
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Productos Destacados", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (productos.isEmpty()) {
                items(2) { index -> FeaturedProductPlaceholder(index) }
            } else {
                items(productos) { producto ->
                    FeaturedProductCard(producto)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Parece que no hay nada más por aquí...", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SellerDashboardContent(onBackToBuyer: () -> Unit, onOpenPublish: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenPublish,
                containerColor = UAMGreen,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Publicar Nuevo", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToBuyer) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ventas en Campus", color = UAMGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Tus Publicaciones en el Campus", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF888888), RoundedCornerShape(20.dp))
                        .clickable { onOpenPublish() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿Qué vas a vender hoy en la UAM?", color = Color.Gray, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.weight(1.5f))
        }
    }
}

@Composable
fun PublishProductForm(onClose: () -> Unit, onPublish: (String, Double, String, String, String) -> Unit) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var selectedFaculty by remember { mutableStateOf("Ingeniería") }
    var selectedCondition by remember { mutableStateOf("Como nuevo") }
    var deliveryMethod by remember { mutableStateOf("Entrega a mano") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detalles del Artículo", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                .border(1.dp, UAMGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .clickable { /* Abrir galería */ },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = UAMGreen, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sube la mejor foto de tu producto", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("¿Qué vas a vender?", color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UAMGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = productPrice,
            onValueChange = { productPrice = it },
            label = { Text("Precio (C$)", color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UAMGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Categoría / Facultad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val faculties = listOf("Ingeniería", "Medicina", "Odontología", "Arte", "Derecho")
            items(faculties) { faculty ->
                FilterChip(
                    selected = selectedFaculty == faculty,
                    onClick = { selectedFaculty = faculty },
                    label = { Text(faculty, color = if (selectedFaculty == faculty) Color.Black else Color.LightGray) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E1E1E),
                        selectedContainerColor = UAMGreen
                    ),
                    shape = CircleShape
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Estado del producto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val conditions = listOf("Nuevo", "Como nuevo", "Buen estado", "Usado")
            items(conditions) { condition ->
                FilterChip(
                    selected = selectedCondition == condition,
                    onClick = { selectedCondition = condition },
                    label = { Text(condition, color = if (selectedCondition == condition) Color.Black else Color.LightGray) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E1E1E),
                        selectedContainerColor = UAMGreen
                    ),
                    shape = CircleShape
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Método de entrega preferido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val methods = listOf("Entrega a mano", "Punto acordado", "Por envío")
            items(methods) { method ->
                FilterChip(
                    selected = deliveryMethod == method,
                    onClick = { deliveryMethod = method },
                    label = { Text(method, color = if (deliveryMethod == method) Color.Black else Color.LightGray) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E1E1E),
                        selectedContainerColor = UAMGreen
                    ),
                    shape = CircleShape
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                val precioNum = productPrice.toDoubleOrNull() ?: 0.0
                if (productName.isNotBlank() && productPrice.isNotBlank()) {
                    onPublish(productName, precioNum, selectedFaculty, selectedCondition, deliveryMethod)
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UAMGreen),
            shape = CircleShape
        ) {
            Text("Publicar Artículo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun FeaturedProductCard(producto: Producto) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(producto.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text("C$ ${producto.precio}", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun FeaturedProductPlaceholder(index: Int) {
    Card(
        modifier = Modifier.width(150.dp).height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Artículo UAM ${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("C$ 250.00", color = UAMGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun ProfileContent() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Mi Perfil UAM", color = UAMGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.Red.copy(alpha = 0.8f))
        }
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier.size(100.dp).background(Color(0xFF1E1E1E), CircleShape).border(2.dp, UAMGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Estudiante UAM", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Comunidad Campus Link v1.2", color = Color.Gray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(40.dp))
        Text("Mis Compras Recientes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("¡Aún no has comprado nada!", color = Color.Gray)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}