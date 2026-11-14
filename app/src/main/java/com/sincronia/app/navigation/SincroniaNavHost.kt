package com.sincronia.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.sincronia.app.data.settings.SettingsRepository
import com.sincronia.app.ui.permissions.PermissionsScreen
import kotlinx.coroutines.launch
import com.sincronia.app.ui.components.ItemActions
import com.sincronia.app.ui.editor.EditorScreen
import com.sincronia.app.data.edit.ItemType
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.remember
import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.core.auth.AuthState
import com.sincronia.app.ui.onboarding.Onboarding
import com.sincronia.app.ui.settings.SettingsScreen
import com.sincronia.app.ui.tasks.TasksScreen
import com.sincronia.app.ui.today.TodayScreen
import androidx.compose.ui.platform.LocalContext

@Composable
fun SincroniaNavHost(demoPreferences: DemoPreferences, accountStore: AccountStore, settings: SettingsRepository) {
    val demoEnabled by demoPreferences.enabled.collectAsState(initial = false)
    val account by accountStore.account.collectAsState(initial = null)
    val context = LocalContext.current
    // NEEDS_REAUTH keeps the local data on screen (spec 6.4, step 6).
    val signedIn = account?.authState == AuthState.AUTHORIZED || account?.authState == AuthState.NEEDS_REAUTH
    if (!demoEnabled && !signedIn) {
        Onboarding(onDemoEntered = { SyncScheduler.enqueue(context) })
        return
    }
    val permissionsSeen by settings.permissionsStepSeen.collectAsState(initial = true)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    if (!permissionsSeen) {
        PermissionsScreen(onDone = { scope.launch { settings.markPermissionsSeen() } })
        return
    }
    // First entry after sign-in or demo, and again whenever the account changes.
    LaunchedEffect(account?.accountId, demoEnabled) {
        SyncScheduler.enqueue(context)
        SyncScheduler.schedulePeriodic(context, settings.current().syncIntervalMinutes.toLong())
        SyncScheduler.scheduleAlarmRefresh(context)
    }
    val navController = rememberNavController()
    val actions = remember(navController) {
        ItemActions(
            openEvent = { navController.navigate("editor?type=${ItemType.EVENT}&id=$it") },
            openTask = { navController.navigate("editor?type=${ItemType.TASK}&id=$it") },
            newItem = { date, type, list ->
                navController.navigate("editor?type=${type.orEmpty()}&date=$date&list=${list.orEmpty()}")
            },
        )
    }
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
            composable(TopLevelDestination.TODAY.route) { TodayScreen(actions) }
            composable(TopLevelDestination.AGENDA.route) { AgendaScreen(actions) }
            composable(TopLevelDestination.TASKS.route) { TasksScreen(actions) }
            composable(
                "editor?type={type}&id={id}&date={date}&list={list}",
                arguments = listOf("type", "id", "date", "list").map { name ->
                    navArgument(name) { type = NavType.StringType; nullable = true; defaultValue = null }
                },
            ) { EditorScreen(onClose = { navController.popBackStack() }) }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
