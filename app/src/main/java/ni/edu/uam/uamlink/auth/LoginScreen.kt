package ni.edu.uam.uamlink.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import ni.edu.uam.uamlink.R
import ni.edu.uam.uamlink.components.UAMTextField
import ni.edu.uam.uamlink.core.data.SupabaseNetwork
import ni.edu.uam.uamlink.ui.theme.*

@Composable
fun LoginScreen(onBackClick: () -> Unit, onSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(UAMBackground)) {

        // 1. Banner Integrado
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Image(
                painter = painterResource(id = R.drawable.iniciodesesionbanner),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, UAMBackground),
                            startY = 100f
                        )
                    )
            )

            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
        }

        // 2. Formulario compacto
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Espaciado compacto para unir los elementos
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Inicia Sesión",
                color = UAMGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            UAMTextField(label = "Correo Institucional", value = email, onValueChange = { email = it })
            UAMTextField(label = "Contraseña", value = password, onValueChange = { password = it }, isPassword = true)

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = UAMError,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            SupabaseNetwork.client.auth.signInWith(Email) { this.email = email; this.password = password }
                            onSuccess()
                        } catch (e: Exception) { errorMessage = "Error: ${e.message}" }
                        finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UAMGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Iniciar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}