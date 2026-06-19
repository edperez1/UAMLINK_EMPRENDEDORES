package ni.edu.uam.uamlink.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ni.edu.uam.uamlink.ui.theme.*

@Composable
fun UAMTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            // Usamos Color.White para el fondo de los campos de texto según tu Theme.kt
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,

            // Usamos tu color principal definido en la paleta
            focusedBorderColor = VividGreen,
            unfocusedBorderColor = LightGray,
            cursorColor = VividGreen,

            // Usamos los colores de texto definidos
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,

            // Colores para el label
            focusedLabelColor = VividGreen,
            unfocusedLabelColor = TextSecondary
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
}