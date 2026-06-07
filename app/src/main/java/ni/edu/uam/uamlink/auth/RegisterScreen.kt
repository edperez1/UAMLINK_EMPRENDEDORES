package ni.edu.uam.uamlink.auth

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import ni.edu.uam.uamlink.core.data.SupabaseNetwork
import ni.edu.uam.uamlink.components.UAMTextField
import ni.edu.uam.uamlink.ui.theme.*

@Composable
fun RegisterScreen(onBackClick: () -> Unit, onSuccess: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var cif by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UAMBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Botón de atrás
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = UAMTextPrimary)
        }

        // Columna compacta
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            // REDUCIDO: De 16.dp a 8.dp para que los campos estén más juntos
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Crea tu Cuenta",
                color = UAMGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            // Espacio pequeño debajo del título
            Spacer(modifier = Modifier.height(4.dp))

            UAMTextField(label = "Nombre Completo", value = fullName, onValueChange = { fullName = it })
            UAMTextField(label = "CIF", value = cif, onValueChange = { cif = it })
            UAMTextField(label = "Correo Institucional", value = email, onValueChange = { email = it })
            UAMTextField(label = "Contraseña", value = password, onValueChange = { password = it }, isPassword = true)

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = UAMError, fontSize = 12.sp)
            }

            // Espacio pequeño antes del botón
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            SupabaseNetwork.client.auth.signUpWith(Email) {
                                this.email = email
                                this.password = password
                                this.data = buildJsonObject {
                                    put("full_name", fullName)
                                    put("cif", cif)
                                }
                            }
                            onSuccess()
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UAMGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Registrarse", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}