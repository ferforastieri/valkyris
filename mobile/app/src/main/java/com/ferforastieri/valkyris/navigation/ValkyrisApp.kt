package com.ferforastieri.valkyris.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ferforastieri.valkyris.MainViewModel
import com.ferforastieri.valkyris.R
import com.ferforastieri.valkyris.feature.cameras.CameraLiveScreen
import com.ferforastieri.valkyris.feature.cameras.CamerasScreen
import com.ferforastieri.valkyris.feature.events.EventDetailScreen
import com.ferforastieri.valkyris.feature.events.EventsScreen
import com.ferforastieri.valkyris.feature.onboarding.OnboardingScreen
import com.ferforastieri.valkyris.feature.rules.RulesScreen
import com.ferforastieri.valkyris.feature.settings.SettingsScreen

private data class Destination(val route: String, val label: Int, val icon: ImageVector)

private val destinations = listOf(
    Destination("cameras", R.string.cameras, Icons.Rounded.Videocam),
    Destination("events", R.string.events, Icons.Rounded.Notifications),
    Destination("rules", R.string.rules, Icons.Rounded.Tune),
    Destination("settings", R.string.settings, Icons.Rounded.Settings),
)

@Composable
fun ValkyrisApp(main: MainViewModel) {
    val paired by main.paired.collectAsStateWithLifecycle()
    if (!paired) {
        OnboardingScreen(main)
        return
    }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val showBottomBar = destinations.any { it.route == entry?.destination?.route }
    val pendingEvent by main.pendingEvent.collectAsStateWithLifecycle()
    LaunchedEffect(pendingEvent) {
        pendingEvent?.let {
            nav.navigate("event/$it") { launchSingleTop = true }
            main.eventOpened()
        }
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    tonalElevation = 1.dp,
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = entry?.destination?.route == destination.route,
                                onClick = {
                                    nav.navigate(destination.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, null) },
                                label = { Text(stringResource(destination.label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondary,
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, "cameras", Modifier.padding(padding)) {
            composable("cameras") { CamerasScreen(onCamera = { nav.navigate("camera/$it") }) }
            composable("camera/{id}") { CameraLiveScreen(cameraId = it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) }
            composable("events") { EventsScreen(onEvent = { nav.navigate("event/$it") }) }
            composable("event/{id}") { EventDetailScreen(onBack = { nav.popBackStack() }) }
            composable("rules") { RulesScreen() }
            composable("settings") { SettingsScreen(main) }
        }
    }
}
