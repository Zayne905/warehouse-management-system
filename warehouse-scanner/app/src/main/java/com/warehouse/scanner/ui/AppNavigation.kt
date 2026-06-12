package com.warehouse.scanner.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.warehouse.scanner.network.TokenProvider
import com.warehouse.scanner.ui.home.HomeScreen
import com.warehouse.scanner.ui.login.LoginScreen
import com.warehouse.scanner.ui.scanner.BlockUnblockScreen
import com.warehouse.scanner.ui.scanner.OutboundScannerScreen
import com.warehouse.scanner.ui.scanner.ScannerScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val OUTBOUND_SCANNER = "outbound_scanner"
    const val BLOCK_UNBLOCK = "block_unblock"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = if (TokenProvider.isLoggedIn) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToScanner = { navController.navigate(Routes.SCANNER) },
                onNavigateToOutboundScanner = { navController.navigate(Routes.OUTBOUND_SCANNER) },
                onNavigateToBlockUnblock = { navController.navigate(Routes.BLOCK_UNBLOCK) },
                onLogout = {
                    TokenProvider.clear()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SCANNER) {
            ScannerScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.OUTBOUND_SCANNER) {
            OutboundScannerScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.BLOCK_UNBLOCK) {
            BlockUnblockScreen(onBack = { navController.popBackStack() })
        }
    }
}
