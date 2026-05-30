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
            focusedContainerColor = UAMSurface,
            unfocusedContainerColor = UAMSurface,
            focusedBorderColor = UAMGreen,
            unfocusedBorderColor = UAMBorder,
            cursorColor = UAMGreen,
            focusedTextColor = UAMTextPrimary,
            unfocusedTextColor = UAMTextPrimary,
            focusedLabelColor = UAMGreen,
            unfocusedLabelColor = UAMTextSecondary
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
}