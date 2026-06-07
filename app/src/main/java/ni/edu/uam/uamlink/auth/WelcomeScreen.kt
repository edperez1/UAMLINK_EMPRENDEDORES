package ni.edu.uam.uamlink.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.uam.uamlink.R
import ni.edu.uam.uamlink.ui.theme.*

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UAMBackground)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- BOTÓN OMITIR (ARRIBA) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkipClick) {
                Text(
                    text = "Omitir",
                    color = UAMTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- BLOQUE CENTRAL (LOGO + TEXTOS UNIDOS) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()), // Evita que se corte en pantallas chicas
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // LOGO ENORME: Solucionado el error de compilación manteniendo el mismo tamaño visual
            Image(
                painter = painterResource(id = R.drawable.logouamlinkprincipal),
                contentDescription = "Logo UAM Link",
                modifier = Modifier
                    .requiredWidth(360.dp) // Usa un ancho fijo grande para mantenerlo gigante sin usar fracciones ilegales
                    .height(180.dp),
                contentScale = ContentScale.FillWidth
            )

            // ESPACIO PEQUEÑO: Para que el texto quede justo abajo del logo
            Spacer(modifier = Modifier.height(16.dp))

            // Título pegado al logo
            Text(
                text = "¡Te damos la bienvenida!",
                color = UAMTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtítulo
            Text(
                text = "El mercado exclusivo de la comunidad universitaria. Compra, vende y conecta.",
                color = UAMTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // --- BOTONES (ABAJO) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón Iniciar sesión
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = UAMGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Iniciar sesión",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Registrarse
            OutlinedButton(
                onClick = onRegisterClick,
                border = BorderStroke(2.dp, UAMTextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Registrarse",
                    color = UAMTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}