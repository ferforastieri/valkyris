package com.ferforastieri.camtacte

import android.app.Application
import com.ferforastieri.camtacte.core.alarm.AlarmNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CamtacteApplication : Application() {
    @Inject lateinit var alarmNotifier: AlarmNotifier
    override fun onCreate() {
        super.onCreate()
        alarmNotifier.createChannels()
    }
}

