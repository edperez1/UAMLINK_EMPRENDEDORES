package ni.edu.uam.uamlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ni.edu.uam.uamlink.auth.LoginScreen
import ni.edu.uam.uamlink.auth.RegisterScreen
import ni.edu.uam.uamlink.auth.WelcomeScreen
import ni.edu.uam.uamlink.auth.RoleSelectionScreen // Importamos la nueva pantalla
import ni.edu.uam.uamlink.ui.theme.UAMlinkTheme
import ni.edu.uam.uamlink.ui.theme.UAMBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UAMlinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UAMBackground
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome") {

        composable("welcome") {
            WelcomeScreen(
                onLoginClick = { navController.navigate("login") },
                onRegisterClick = { navController.navigate("register") },
                onSkipClick = { /* TODO: Navegar directo al Market */ }
            )
        }

        composable("login") {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = { /* TODO: Navegar al Market */ }
            )
        }

        composable("register") {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = {
                    // Al registrarse, enviamos al usuario a elegir su rol
                    navController.navigate("role_selection") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("role_selection") {
            RoleSelectionScreen(onComplete = { role ->
                // Aquí el usuario ya eligió si es comprador o vendedor
                println("Usuario configurado como: $role")
                // TODO: Navegar a la pantalla principal (Home)
            })
        }
    }
}