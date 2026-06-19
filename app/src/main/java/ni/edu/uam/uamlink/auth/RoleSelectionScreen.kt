package ni.edu.uam.uamlink.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.uam.uamlink.ui.theme.*

@Composable
fun RoleSelectionScreen(onNavigateHome: (Boolean) -> Unit) {
    var isSeller by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGreenBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Centramos todo verticalmente
    ) {
        // Título con más impacto
        Text(
            text = "Campus Link",
            color = VividGreen,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "¿Qué deseas hacer hoy?",
            color = TextPrimary.copy(alpha = 0.8f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Tarjetas
        RoleCard(
            title = "Quiero comprar",
            desc = "Encuentra libros, herramientas y materiales académicos.",
            icon = Icons.Default.ShoppingCart,
            isSelected = isSeller == false,
            onClick = { isSeller = false }
        )

        Spacer(modifier = Modifier.height(20.dp))

        RoleCard(
            title = "Quiero vender",
            desc = "Publica tus artículos, herramientas o productos universitarios.",
            icon = Icons.Default.Store,
            isSelected = isSeller == true,
            onClick = { isSeller = true }
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Botón con animación de color
        val buttonColor by animateColorAsState(
            targetValue = if (isSeller != null) VividGreen else Color.LightGray,
            label = "buttonColor"
        )

        Button(
            onClick = { isSeller?.let { onNavigateHome(it) } },
            enabled = isSeller != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Ingresar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun RoleCard(title: String, desc: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) VividGreen else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) VividGreen else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}