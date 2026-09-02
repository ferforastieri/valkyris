package com.ferforastieri.camtacte.core.network

import com.ferforastieri.camtacte.core.database.CamtacteDao
import com.ferforastieri.camtacte.core.database.EventEntity
import com.ferforastieri.camtacte.core.database.RuleEntity
import com.ferforastieri.camtacte.core.database.PendingActionEntity
import com.ferforastieri.camtacte.core.model.Rule
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class CamtacteRepository @Inject constructor(val api:CamtacteApi,private val dao:CamtacteDao){
    private val json=Json{ignoreUnknownKeys=true;explicitNulls=false}
    fun cachedEvents()=dao.events()
    fun cachedRules()=dao.rules()
    suspend fun refreshEvents(){flushPending();val events=api.events();dao.syncEvents(events.map{EventEntity(it.id,it.cameraId,it.type,it.confidence,it.occurredAt,it.snapshotPath,it.clipPath,it.acknowledgedAt)})}
    suspend fun refreshRules(){flushPending();val rules=api.rules();dao.replaceRules(rules.map{RuleEntity(it.id,it.cameraId,it.name,it.detectorTypes.joinToString(","),it.enabled)})}
    suspend fun acknowledge(eventId:String){runCatching{api.acknowledge(eventId)}.onFailure{dao.savePending(PendingActionEntity("ack:$eventId","ack",eventId,System.currentTimeMillis()))}.getOrThrow()}
    suspend fun createRule(rule:Rule){runCatching{api.createRule(rule)}.onFailure{dao.savePending(PendingActionEntity(UUID.randomUUID().toString(),"rule",json.encodeToString(Rule.serializer(),rule),System.currentTimeMillis()))}.getOrThrow()}
    suspend fun flushPending(){dao.pendingActions().forEach{action->val delivered=runCatching{when(action.kind){"ack"->api.acknowledge(action.payload);"rule"->api.createRule(json.decodeFromString(Rule.serializer(),action.payload));else->error("unknown pending action")}}.isSuccess;if(delivered)dao.deletePending(action.id)}}
}
