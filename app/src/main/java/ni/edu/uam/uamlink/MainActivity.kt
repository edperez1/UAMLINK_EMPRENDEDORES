package ni.edu.uam.uamlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ni.edu.uam.uamlink.auth.LoginScreen
import ni.edu.uam.uamlink.auth.RegisterScreen
import ni.edu.uam.uamlink.auth.WelcomeScreen
import ni.edu.uam.uamlink.auth.RoleSelectionScreen
import ni.edu.uam.uamlink.auth.HomeScreen
import ni.edu.uam.uamlink.ui.theme.UAMlinkTheme
import ni.edu.uam.uamlink.ui.theme.UAMBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UAMlinkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = UAMBackground) {
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
                onSkipClick = {
                    navController.navigate("home/false") { popUpTo("welcome") { inclusive = true } }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate("role_selection") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate("role_selection") { popUpTo("register") { inclusive = true } }
                }
            )
        }

        composable("role_selection") {
            RoleSelectionScreen(
                onNavigateHome = { isSeller ->
                    navController.navigate("home/$isSeller") {
                        popUpTo("role_selection") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "home/{isSeller}",
            arguments = listOf(navArgument("isSeller") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isSeller = backStackEntry.arguments?.getBoolean("isSeller") ?: false

            HomeScreen(
                isSeller = isSeller,
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}