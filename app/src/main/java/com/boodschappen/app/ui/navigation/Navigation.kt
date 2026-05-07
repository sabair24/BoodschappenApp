package com.boodschappen.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.boodschappen.app.ui.screens.AddEditItemScreen
import com.boodschappen.app.ui.screens.ScannerScreen
import com.boodschappen.app.ui.screens.ShoppingListScreen
import com.boodschappen.app.viewmodel.ShoppingViewModel

sealed class Screen(val route: String) {
    object ShoppingList : Screen("shopping_list")
    object AddItem : Screen("add_item")
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: Long) = "edit_item/$itemId"
    }
    object Scanner : Screen("scanner")
}

@Composable
fun BoodschappenNavigation() {
    val navController = rememberNavController()
    val viewModel: ShoppingViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.ShoppingList.route
    ) {
        composable(Screen.ShoppingList.route) {
            ShoppingListScreen(
                viewModel = viewModel,
                onAddItem = { navController.navigate(Screen.AddItem.route) },
                onEditItem = { id -> navController.navigate(Screen.EditItem.createRoute(id)) },
                onScanBarcode = { navController.navigate(Screen.Scanner.route) }
            )
        }

        composable(Screen.AddItem.route) {
            AddEditItemScreen(
                viewModel = viewModel,
                itemId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId")
            AddEditItemScreen(
                viewModel = viewModel,
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdd = {
                    navController.navigate(Screen.AddItem.route) {
                        popUpTo(Screen.Scanner.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
