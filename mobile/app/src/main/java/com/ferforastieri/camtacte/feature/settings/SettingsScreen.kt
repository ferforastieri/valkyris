package com.ferforastieri.camtacte.feature.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ferforastieri.camtacte.MainViewModel
import com.ferforastieri.camtacte.R
import org.unifiedpush.android.connector.UnifiedPush

@Composable fun SettingsScreen(main:MainViewModel){val context=LocalContext.current;var pushStatusRes by remember{mutableIntStateOf(R.string.push_not_configured)};val theme by main.theme.collectAsStateWithLifecycle();val language by main.language.collectAsStateWithLifecycle();Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(stringResource(R.string.settings),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(6.dp));SettingsCard(Icons.Rounded.Notifications,stringResource(R.string.notification_setup),stringResource(pushStatusRes)){UnifiedPush.tryUseCurrentOrDefaultDistributor(context){success->if(success){UnifiedPush.register(context,"Camtacte home alerts",null);pushStatusRes=R.string.push_waiting}else pushStatusRes=R.string.push_install_ntfy}};if(Build.VERSION.SDK_INT>=34)SettingsCard(Icons.Rounded.Alarm,stringResource(R.string.full_screen_alarms),stringResource(if(context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent())R.string.permission_allowed else R.string.permission_required)){context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).setData(android.net.Uri.parse("package:${context.packageName}")))};SettingsCard(Icons.Rounded.Language,stringResource(R.string.language),languageLabel(language)){val next=when(language){"system"->"pt-BR";"pt-BR"->"en";else->"system"};main.setLanguage(next);AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if(next=="system")"" else next))};SettingsCard(Icons.Rounded.DarkMode,stringResource(R.string.theme),themeLabel(theme)){main.setTheme(when(theme){"system"->"light";"light"->"dark";else->"system"})};SettingsInfoCard(Icons.Rounded.Storage,stringResource(R.string.media_retention),stringResource(R.string.media_retention_value));Spacer(Modifier.weight(1f));OutlinedButton({main.signOut()},Modifier.fillMaxWidth()){Text(stringResource(R.string.disconnect_phone))};Text(stringResource(R.string.local_only),Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)} }
@Composable private fun themeLabel(value:String)=when(value){"light"->stringResource(R.string.light_theme);"dark"->stringResource(R.string.dark_theme);else->stringResource(R.string.system_default)}
@Composable private fun languageLabel(value:String)=when(value){"pt-BR"->"Português (Brasil)";"en"->"English";else->stringResource(R.string.system_default)}
@Composable private fun SettingsCard(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Card(onClick=onClick,Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Medium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Rounded.ChevronRight,null)}}}
@Composable private fun SettingsInfoCard(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String){Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null);Spacer(Modifier.width(14.dp));Column{Text(title,fontWeight=FontWeight.Medium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
