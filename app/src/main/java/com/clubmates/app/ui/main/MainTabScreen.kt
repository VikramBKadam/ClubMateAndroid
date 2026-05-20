package com.clubmates.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clubmates.app.ui.discover.DiscoverScreen
import com.clubmates.app.ui.matches.MatchesScreen
import com.clubmates.app.ui.matches.MatchesViewModel
import com.clubmates.app.ui.profile.ProfileScreen

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    object Discover : Tab("tab_discover", "Discover", Icons.Default.Place)
    object Matches : Tab("tab_matches", "Matches", Icons.Default.Favorite)
    object Profile : Tab("tab_profile", "Profile", Icons.Default.Person)
}

private val tabs = listOf(Tab.Discover, Tab.Matches, Tab.Profile)

@Composable
fun MainTabScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val matchesViewModel: MatchesViewModel = hiltViewModel()
    val matchesState by matchesViewModel.matchesState.collectAsState()

    var currentRoute by remember { mutableStateOf(Tab.Discover.route) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            currentRoute = tab.route
                            navController.navigate(tab.route) {
                                popUpTo(Tab.Discover.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (tab is Tab.Matches && matchesState.matches.sumOf { it.unreadCount } > 0) {
                                BadgedBox(badge = {
                                    Badge { Text(matchesState.matches.sumOf { it.unreadCount }.toString()) }
                                }) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Discover.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Discover.route) { DiscoverScreen() }
            composable(Tab.Matches.route) { MatchesScreen(viewModel = matchesViewModel) }
            composable(Tab.Profile.route) { ProfileScreen(onLogout = onLogout) }
        }
    }
}
