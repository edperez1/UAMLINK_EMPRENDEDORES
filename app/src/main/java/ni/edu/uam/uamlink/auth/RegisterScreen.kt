package ni.edu.uam.uamlink.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
            .padding(24.dp)
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = UAMTextPrimary)
        }

        Text(
            text = "Crea tu Cuenta",
            color = UAMGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(UAMSurface, CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Foto", tint = UAMGreen, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        UAMTextField(label = "Nombre Completo", value = fullName, onValueChange = { fullName = it })
        UAMTextField(label = "CIF", value = cif, onValueChange = { cif = it })
        UAMTextField(label = "Correo Institucional", value = email, onValueChange = { email = it })
        UAMTextField(label = "Contraseña", value = password, onValueChange = { password = it }, isPassword = true)

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = UAMError, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Validación estricta
                val esCorreoValido = email.endsWith("@uamv.edu.ni")
                val esPassValida = password.length > 5

                if (!esCorreoValido) {
                    errorMessage = "Debes usar tu correo institucional (@uamv.edu.ni)"
                } else if (!esPassValida) {
                    errorMessage = "La contraseña debe tener más de 5 caracteres"
                } else {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            SupabaseNetwork.client.auth.signUpWith(Email) {
                                this.email = email
                                this.password = password
                            }
                            onSuccess()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Error al registrar la cuenta."
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = UAMGreen),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Registrarse", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}