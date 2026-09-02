package com.ferforastieri.camtacte

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferforastieri.camtacte.core.network.CamtacteApi
import com.ferforastieri.camtacte.core.preferences.AppPreferences
import com.ferforastieri.camtacte.core.security.Session
import com.ferforastieri.camtacte.core.security.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class PairingDraft(val url:String,val code:String,val fingerprint:String)

@HiltViewModel class MainViewModel @Inject constructor(private val api:CamtacteApi,private val sessions:SessionStore,private val preferences:AppPreferences):ViewModel(){
    private val _paired=MutableStateFlow(sessions.get()!=null);val paired=_paired.asStateFlow()
    private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
    private val _pairingDraft=MutableStateFlow<PairingDraft?>(null);val pairingDraft=_pairingDraft.asStateFlow()
    private val _pendingEvent=MutableStateFlow<String?>(null);val pendingEvent=_pendingEvent.asStateFlow()
    val theme=preferences.theme.stateIn(viewModelScope,SharingStarted.Eagerly,"system")
    val language=preferences.language.stateIn(viewModelScope,SharingStarted.Eagerly,"system")
    fun acceptPairingLink(uri:Uri?){if(uri?.scheme!="camtacte"||uri.host!="pair")return;val url=uri.getQueryParameter("url").orEmpty();val code=uri.getQueryParameter("code").orEmpty();val fingerprint=uri.getQueryParameter("fingerprint").orEmpty();if(url.startsWith("https://")&&code.isNotBlank()&&fingerprint.isNotBlank())_pairingDraft.value=PairingDraft(url,code,fingerprint)}
    fun acceptLaunch(uri:Uri?,eventId:String?){acceptPairingLink(uri);val deepLinkId=if(uri?.scheme=="camtacte"&&uri.host=="event")uri.lastPathSegment else null;_pendingEvent.value=eventId?.takeIf(String::isNotBlank)?:deepLinkId?.takeIf(String::isNotBlank)}
    fun eventOpened(){_pendingEvent.value=null}
    fun pair(url:String,code:String,fingerprint:String){viewModelScope.launch{runCatching{api.pair(url,fingerprint,com.ferforastieri.camtacte.core.model.PairRequest(code,android.os.Build.MODEL,Locale.getDefault().toLanguageTag()))}.onSuccess{sessions.save(Session(url,it.token,fingerprint));_paired.value=true;_error.value=null}.onFailure{_error.value=it.message}}}
    fun signOut(){sessions.clear();_paired.value=false}
    fun setTheme(value:String){viewModelScope.launch{preferences.setTheme(value)}}
    fun setLanguage(value:String){viewModelScope.launch{preferences.setLanguage(value)}}
}
