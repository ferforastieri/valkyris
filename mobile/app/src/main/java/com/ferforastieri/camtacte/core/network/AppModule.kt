package com.ferforastieri.camtacte.core.network

import android.content.Context
import androidx.room.Room
import com.ferforastieri.camtacte.core.database.CamtacteDatabase
import com.ferforastieri.camtacte.core.security.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun api(session:SessionStore)=CamtacteApi{session.get()}
    @Provides @Singleton fun database(@ApplicationContext context:Context)=Room.databaseBuilder(context,CamtacteDatabase::class.java,"camtacte-cache.db").fallbackToDestructiveMigration(false).build()
    @Provides fun dao(database:CamtacteDatabase)=database.dao()
}

