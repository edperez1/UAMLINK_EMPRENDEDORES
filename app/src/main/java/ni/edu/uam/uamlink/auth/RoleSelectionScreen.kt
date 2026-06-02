package ni.edu.uam.uamlink.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.uam.uamlink.ui.theme.*

@Composable
fun RoleSelectionScreen(onNavigateHome: (Boolean) -> Unit) {
    // Estado para saber qué seleccionó: false = Comprador, true = Vendedor
    var isSeller by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(UAMBackground).padding(24.dp)) {
        Spacer(modifier = Modifier.height(60.dp))
        Text("Campus Link", color = UAMGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(8.dp))
        Text("¿Qué deseas hacer el día de hoy dentro del campus?", color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Spacer(modifier = Modifier.height(40.dp))

        // Tarjeta Comprar
        RoleCard(
            title = "Quiero comprar",
            desc = "Buscar libros, herramientas, materiales clínicos o de arte.",
            icon = Icons.Default.ShoppingCart,
            isSelected = isSeller == false,
            onClick = { isSeller = false }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta Vender
        RoleCard(
            title = "Quiero vender / ofertar",
            desc = "Publicar artículos académicos, herramientas o productos universitarios.",
            icon = Icons.Default.Store,
            isSelected = isSeller == true,
            onClick = { isSeller = true }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { isSeller?.let { onNavigateHome(it) } },
            enabled = isSeller != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = UAMGreen,
                disabledContainerColor = Color.DarkGray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Ingresar a la plataforma", color = if (isSeller != null) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RoleCard(title: String, desc: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) UAMGreen else Color.Transparent
    val bgColor = Color(0xFF1E1E1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}