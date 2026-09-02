package com.ferforastieri.valkyris

import android.app.Application
import com.ferforastieri.valkyris.core.alarm.AlarmNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ValkyrisApplication : Application() {
    @Inject lateinit var alarmNotifier: AlarmNotifier
    override fun onCreate() {
        super.onCreate()
        alarmNotifier.createChannels()
    }
}

