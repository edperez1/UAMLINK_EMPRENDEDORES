package ni.edu.uam.uamlink.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.uam.uamlink.ui.theme.*

@Composable
fun RoleSelectionScreen(onComplete: (String) -> Unit) {
    var selectedRole by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(UAMBackground).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text("Campus Link", color = UAMGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("¿Qué deseas hacer hoy?", color = UAMTextPrimary, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(40.dp))

        // Opción Comprar
        Card(
            onClick = { selectedRole = "comprador" },
            colors = CardDefaults.cardColors(containerColor = if (selectedRole == "comprador") UAMGreen else UAMSurface),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Quiero comprar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Opción Vender
        Card(
            onClick = { selectedRole = "vendedor" },
            colors = CardDefaults.cardColors(containerColor = if (selectedRole == "vendedor") UAMGreen else UAMSurface),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Store, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Quiero vender", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { if (selectedRole.isNotEmpty()) onComplete(selectedRole) },
            enabled = selectedRole.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UAMGreen)
        ) {
            Text("Ingresar a la plataforma")
        }
    }
}