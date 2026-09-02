package com.ferforastieri.valkyris.feature.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferforastieri.valkyris.MainViewModel
import com.ferforastieri.valkyris.R
import com.ferforastieri.valkyris.core.design.SignalLine
import com.ferforastieri.valkyris.core.design.ValkyrisMark
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    var url by remember { mutableStateOf("https://valkyris.local:8443") }
    var code by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("") }
    val draft by viewModel.pairingDraft.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val scanPrompt = stringResource(R.string.scan_qr_prompt)
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.acceptPairingLink(Uri.parse(it)) }
    }

    LaunchedEffect(draft) {
        draft?.let {
            url = it.url
            code = it.code.uppercase()
            fingerprint = it.fingerprint.uppercase()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, shadowElevation = 8.dp, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ValkyrisMark(Modifier.size(78.dp))
                    Text(stringResource(R.string.pair_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.pair_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SignalLine(Modifier.fillMaxWidth().height(52.dp))
                    Button(
                        onClick = {
                            scanner.launch(
                                ScanOptions()
                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt(scanPrompt)
                                    .setBeepEnabled(false)
                                    .setOrientationLocked(false),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, null)
                        Text(stringResource(R.string.scan_qr), Modifier.padding(start = 8.dp))
                    }
                    OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.server_url)) }, singleLine = true)
                    OutlinedTextField(code, { code = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.pair_code)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(fingerprint, { fingerprint = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text("SHA-256 fingerprint") }, singleLine = true)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    OutlinedButton({ viewModel.pair(url, code, fingerprint) }, Modifier.fillMaxWidth(), enabled = code.isNotBlank() && fingerprint.isNotBlank()) {
                        Text(stringResource(R.string.connect))
                    }
                }
            }
            Text("Valkyris · local-first", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
