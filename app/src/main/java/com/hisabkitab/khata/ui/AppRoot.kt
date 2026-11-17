package com.hisabkitab.khata.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hisabkitab.khata.R

object Routes {
    const val HOME = "home"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"
    const val SHOPS = "shops"
    const val CUSTOMER = "customer/{id}"
    fun customer(id: Long) = "customer/$id"
}

@Composable
fun AppRoot(vm: MainViewModel) {
    val shops by vm.shops.collectAsState()
    val list = shops
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        val start = remember { if (list.isEmpty()) Routes.SHOPS else Routes.HOME }
        MainNav(vm, start)
    }
}

@Composable
private fun MainNav(vm: MainViewModel, start: String) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(
        Triple(Routes.HOME, R.string.nav_home, Icons.Default.Home),
        Triple(Routes.CUSTOMERS, R.string.nav_customers, Icons.Default.Person),
        Triple(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tabs.any { it.first == route }) {
                NavigationBar {
                    tabs.forEach { (r, label, icon) ->
                        NavigationBarItem(
                            selected = route == r,
                            onClick = {
                                nav.navigate(r) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = start, modifier = Modifier.padding(pad)) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm,
                    onManageShops = { nav.navigate(Routes.SHOPS) { launchSingleTop = true } },
                    onCustomers = {
                        nav.navigate(Routes.CUSTOMERS) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.CUSTOMERS) {
                CustomersScreen(vm, onOpen = { nav.navigate(Routes.customer(it)) })
            }
            composable(Routes.SETTINGS) { SettingsScreen(vm) }
            composable(Routes.SHOPS) {
                val canGoBack = nav.previousBackStackEntry != null
                ShopsScreen(
                    vm,
                    onBack = if (canGoBack) fun() { nav.popBackStack() } else null,
                    onSelected = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.SHOPS) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                Routes.CUSTOMER,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                CustomerDetailScreen(vm, entry.arguments?.getLong("id") ?: 0L, onBack = { nav.popBackStack() })
            }
        }
    }
}
