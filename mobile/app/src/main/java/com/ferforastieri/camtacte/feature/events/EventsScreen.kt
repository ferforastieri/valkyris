package com.ferforastieri.camtacte.feature.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferforastieri.camtacte.R
import com.ferforastieri.camtacte.core.database.EventEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun EventsScreen(onEvent:(String)->Unit={},vm:EventsViewModel=hiltViewModel()){val events=vm.events.collectAsStateWithLifecycle().value;Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Spacer(Modifier.height(18.dp));Text(stringResource(R.string.events),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold);Text(stringResource(R.string.event_media_summary),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(18.dp));if(events.isEmpty())Box(Modifier.fillMaxSize()){Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.NotificationsNone,null,Modifier.size(42.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));Text(stringResource(R.string.no_events),fontWeight=FontWeight.SemiBold);Text(stringResource(R.string.no_events_body),color=MaterialTheme.colorScheme.onSurfaceVariant)}}else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)){items(events,key={it.id}){EventCard(it,onOpen={onEvent(it.id)},onAck={vm.acknowledge(it)})}}}}
@Composable private fun EventCard(event:EventEntity,onOpen:()->Unit,onAck:()->Unit){Card(onClick=onOpen,modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(if(event.acknowledgedAt==null)MaterialTheme.colorScheme.error.copy(alpha=.12f)else MaterialTheme.colorScheme.secondary.copy(alpha=.15f),CircleShape)){Box(Modifier.size(9.dp).background(if(event.acknowledgedAt==null)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,CircleShape).align(Alignment.Center))};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(event.type.replace('_',' ').replaceFirstChar{it.uppercase()},fontWeight=FontWeight.SemiBold);Text(formatTime(event.occurredAt),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("${(event.confidence*100).toInt()}% confidence",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(event.acknowledgedAt==null)IconButton(onAck){Icon(Icons.Rounded.Check,stringResource(R.string.acknowledge))}}}}
private fun formatTime(value:String)=runCatching{DateTimeFormatter.ofPattern("dd MMM · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))}.getOrDefault(value)
