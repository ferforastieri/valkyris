package com.ferforastieri.camtacte.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.camtactePreferences by preferencesDataStore("camtacte_preferences")

@Singleton class AppPreferences @Inject constructor(@param:ApplicationContext private val context:Context){
    private val themeKey=stringPreferencesKey("theme")
    private val languageKey=stringPreferencesKey("language")
    val theme=context.camtactePreferences.data.map{it[themeKey]?:"system"}
    val language=context.camtactePreferences.data.map{it[languageKey]?:"system"}
    suspend fun setTheme(value:String)=context.camtactePreferences.edit{it[themeKey]=value}
    suspend fun setLanguage(value:String)=context.camtactePreferences.edit{it[languageKey]=value}
}
