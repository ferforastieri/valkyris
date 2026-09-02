package com.ferforastieri.camtacte.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferforastieri.camtacte.MainViewModel
import com.ferforastieri.camtacte.R
import com.ferforastieri.camtacte.core.design.SignalLine

@Composable fun OnboardingScreen(viewModel:MainViewModel){var url by remember{mutableStateOf("https://camtacte.local:8443")};var code by remember{mutableStateOf("")};var fingerprint by remember{mutableStateOf("")};val draft by viewModel.pairingDraft.collectAsStateWithLifecycle();val error by viewModel.error.collectAsStateWithLifecycle();LaunchedEffect(draft){draft?.let{url=it.url;code=it.code.uppercase();fingerprint=it.fingerprint.uppercase()}};Box(Modifier.fillMaxSize().padding(24.dp)){Column(Modifier.align(Alignment.Center).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(16.dp)){Surface(shape=MaterialTheme.shapes.large,tonalElevation=1.dp){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Icon(Icons.Rounded.Shield,null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(40.dp));Text(stringResource(R.string.pair_title),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold);Text(stringResource(R.string.pair_body),color=MaterialTheme.colorScheme.onSurfaceVariant);SignalLine(Modifier.fillMaxWidth().height(52.dp));OutlinedTextField(url,{url=it},Modifier.fillMaxWidth(),label={Text(stringResource(R.string.server_url))},singleLine=true);OutlinedTextField(code,{code=it.uppercase()},Modifier.fillMaxWidth(),label={Text(stringResource(R.string.pair_code))},visualTransformation=PasswordVisualTransformation(),singleLine=true);OutlinedTextField(fingerprint,{fingerprint=it.uppercase()},Modifier.fillMaxWidth(),label={Text("SHA-256 fingerprint")},singleLine=true);error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button({viewModel.pair(url,code,fingerprint)},Modifier.fillMaxWidth(),enabled=code.isNotBlank()&&fingerprint.isNotBlank()){Text(stringResource(R.string.connect))}}};Text("Camtacte · local-first",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.align(Alignment.CenterHorizontally))}}}
