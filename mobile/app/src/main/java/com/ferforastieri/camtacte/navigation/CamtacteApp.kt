package com.ferforastieri.camtacte.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ferforastieri.camtacte.MainViewModel
import com.ferforastieri.camtacte.R
import com.ferforastieri.camtacte.feature.cameras.CameraLiveScreen
import com.ferforastieri.camtacte.feature.cameras.CamerasScreen
import com.ferforastieri.camtacte.feature.events.EventsScreen
import com.ferforastieri.camtacte.feature.events.EventDetailScreen
import com.ferforastieri.camtacte.feature.onboarding.OnboardingScreen
import com.ferforastieri.camtacte.feature.rules.RulesScreen
import com.ferforastieri.camtacte.feature.settings.SettingsScreen

private data class Destination(val route:String,val label:Int,val icon:ImageVector)
private val destinations=listOf(Destination("cameras",R.string.cameras,Icons.Rounded.Videocam),Destination("events",R.string.events,Icons.Rounded.Notifications),Destination("rules",R.string.rules,Icons.Rounded.Tune),Destination("settings",R.string.settings,Icons.Rounded.Settings))
@Composable fun CamtacteApp(main:MainViewModel){val paired by main.paired.collectAsStateWithLifecycle();if(!paired){OnboardingScreen(main);return};val nav=rememberNavController();val pendingEvent by main.pendingEvent.collectAsStateWithLifecycle();LaunchedEffect(pendingEvent){pendingEvent?.let{nav.navigate("event/$it"){launchSingleTop=true};main.eventOpened()}};Scaffold(bottomBar={NavigationBar{val entry by nav.currentBackStackEntryAsState();destinations.forEach{d->NavigationBarItem(selected=entry?.destination?.route==d.route,onClick={nav.navigate(d.route){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}},icon={Icon(d.icon,null)},label={Text(stringResource(d.label))})}}}){padding->NavHost(nav,"cameras",Modifier.padding(padding)){composable("cameras"){CamerasScreen(onCamera={nav.navigate("camera/$it")})};composable("camera/{id}"){CameraLiveScreen(cameraId=it.arguments?.getString("id").orEmpty(),onBack={nav.popBackStack()})};composable("events"){EventsScreen(onEvent={nav.navigate("event/$it")})};composable("event/{id}"){EventDetailScreen(onBack={nav.popBackStack()})};composable("rules"){RulesScreen()};composable("settings"){SettingsScreen(main)}}}}
