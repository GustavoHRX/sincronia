package com.sincronia.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import com.sincronia.app.data.demo.DemoPreferences
import com.sincronia.app.data.sync.SyncScheduler
import com.sincronia.app.ui.agenda.AgendaScreen
import com.sincronia.app.ui.onboarding.DemoOnboarding
import com.sincronia.app.ui.settings.SettingsScreen
import com.sincronia.app.ui.tasks.TasksScreen
import com.sincronia.app.ui.today.TodayScreen
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun SincroniaNavHost(demoPreferences: DemoPreferences) {
    val demoEnabled by demoPreferences.enabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    if (!demoEnabled) {
        DemoOnboarding(onEnter = {
            scope.launch {
                demoPreferences.enable()
                SyncScheduler.enqueue(context)
            }
        })
        return
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // O rótulo ao lado já descreve o ícone para o leitor de tela.
                        icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.TODAY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.TODAY.route) { TodayScreen() }
            composable(TopLevelDestination.AGENDA.route) { AgendaScreen() }
            composable(TopLevelDestination.TASKS.route) { TasksScreen() }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
