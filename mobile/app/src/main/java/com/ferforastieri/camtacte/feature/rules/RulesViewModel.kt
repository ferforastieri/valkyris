package com.ferforastieri.camtacte.feature.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferforastieri.camtacte.core.database.RuleEntity
import com.ferforastieri.camtacte.core.model.Rule
import com.ferforastieri.camtacte.core.model.Camera
import com.ferforastieri.camtacte.core.model.DetectorKind
import com.ferforastieri.camtacte.core.network.CamtacteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class RulesViewModel @Inject constructor(private val repository:CamtacteRepository):ViewModel(){val rules=repository.cachedRules().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList<RuleEntity>());private val _cameras=MutableStateFlow<List<Camera>>(emptyList());val cameras=_cameras.asStateFlow();private val _detectors=MutableStateFlow<List<DetectorKind>>(emptyList());val detectors=_detectors.asStateFlow();init{refresh();viewModelScope.launch{_cameras.value=runCatching{repository.api.cameras()}.getOrDefault(emptyList());_detectors.value=runCatching{repository.api.detectors()}.getOrDefault(emptyList())}};fun refresh(){viewModelScope.launch{runCatching{repository.refreshRules()}}};fun create(rule:Rule){viewModelScope.launch{runCatching{repository.createRule(rule)};refresh()}}}
