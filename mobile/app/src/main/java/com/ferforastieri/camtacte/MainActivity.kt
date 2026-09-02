package com.ferforastieri.camtacte

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.ferforastieri.camtacte.core.design.CamtacteTheme
import com.ferforastieri.camtacte.navigation.CamtacteApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint class MainActivity:ComponentActivity(){
    private val viewModel:MainViewModel by viewModels()
    private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);viewModel.acceptLaunch(intent?.data,intent?.getStringExtra("eventId"));enableEdgeToEdge();if(Build.VERSION.SDK_INT>=33)notifications.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{val theme by viewModel.theme.collectAsStateWithLifecycle();CamtacteTheme(theme){CamtacteApp(viewModel)}}}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);viewModel.acceptLaunch(intent.data,intent.getStringExtra("eventId"))}
}
