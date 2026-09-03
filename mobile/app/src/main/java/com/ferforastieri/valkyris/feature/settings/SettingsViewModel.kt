package com.ferforastieri.valkyris.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferforastieri.valkyris.core.network.ValkyrisApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InvitationState(
    val loading: Boolean = false,
    val uri: String? = null,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val api: ValkyrisApi) : ViewModel() {
    private val _invitation = MutableStateFlow(InvitationState())
    val invitation = _invitation.asStateFlow()

    fun createInvitation() {
        viewModelScope.launch {
            _invitation.value = InvitationState(loading = true)
            runCatching { api.createPairingSession() }
                .onSuccess { _invitation.value = InvitationState(uri = api.invitationUri(it)) }
                .onFailure { _invitation.value = InvitationState(error = it.message) }
        }
    }

    fun clearInvitation() {
        _invitation.value = InvitationState()
    }
}
