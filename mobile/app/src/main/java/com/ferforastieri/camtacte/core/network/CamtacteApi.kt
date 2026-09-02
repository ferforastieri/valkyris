package com.ferforastieri.camtacte.core.network

import com.ferforastieri.camtacte.core.model.*
import com.ferforastieri.camtacte.core.security.Session
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class CamtacteApi(private val session:()->Session?) {
    private val json=Json{ignoreUnknownKeys=true;explicitNulls=false}
    fun mediaHttpClient()=pinnedClient(requireNotNull(session()).fingerprint)
    private fun pinnedClient(fingerprint:String)=okhttp3.OkHttpClient.Builder().apply{if(fingerprint.isNotBlank()){val trust=PinnedTrustManager(fingerprint);val ssl=SSLContext.getInstance("TLS").apply{init(null,arrayOf(trust),null)};sslSocketFactory(ssl.socketFactory,trust);hostnameVerifier { _, _ -> true }}}.build()
    private fun client(fingerprint:String)=HttpClient(OkHttp){expectSuccess=true;engine{preconfigured=pinnedClient(fingerprint)};install(ContentNegotiation){json(json)}}
    private fun base()=requireNotNull(session()){ "Camtacte is not paired" }.baseUrl+"/api/v1"
    private suspend inline fun <reified T> get(path:String):T=client(requireNotNull(session()).fingerprint).use{it.get(base()+path){bearerAuth(requireNotNull(session()).token)}.body()}
    private suspend inline fun <reified T,reified B> post(path:String,body:B):T=client(requireNotNull(session()).fingerprint).use{it.post(base()+path){bearerAuth(requireNotNull(session()).token);contentType(ContentType.Application.Json);setBody(body)}.body()}
    suspend fun pair(baseUrl:String,fingerprint:String,request:PairRequest):PairResponse=client(fingerprint).use{it.post(baseUrl.trimEnd('/')+"/api/v1/pair"){contentType(ContentType.Application.Json);setBody(request)}.body()}
    suspend fun cameras():List<Camera> = get("/cameras")
    suspend fun createCamera(camera:CreateCameraRequest):Camera = post("/cameras",camera)
    suspend fun events():List<CamtacteEvent> = get("/events?limit=100")
    suspend fun event(id:String):CamtacteEvent = get("/events/$id")
    suspend fun rules():List<Rule> = get("/rules")
    suspend fun detectors():List<DetectorKind> = get("/detectors")
    suspend fun createRule(rule:Rule):Rule = post("/rules",rule)
    suspend fun acknowledge(id:String){client(requireNotNull(session()).fingerprint).use{it.post(base()+"/events/$id/acknowledge"){bearerAuth(requireNotNull(session()).token)}}}
    suspend fun ptz(cameraId:String,command:PTZCommand){client(requireNotNull(session()).fingerprint).use{it.post(base()+"/cameras/$cameraId/ptz"){bearerAuth(requireNotNull(session()).token);contentType(ContentType.Application.Json);setBody(command)}}}
    suspend fun registerPush(registration:PushRegistration){client(requireNotNull(session()).fingerprint).use{it.post(base()+"/devices/push"){bearerAuth(requireNotNull(session()).token);contentType(ContentType.Application.Json);setBody(registration)}}}
    fun snapshotUrl(cameraId:String)=base()+"/cameras/$cameraId/snapshot"
    fun liveUrl(cameraId:String)=base()+"/cameras/$cameraId/live/index.m3u8"
    fun clipUrl(eventId:String)=base()+"/events/$eventId/clip"
    fun eventSnapshotUrl(eventId:String)=base()+"/events/$eventId/snapshot"
    fun token()=requireNotNull(session()).token
    fun realtime(onMessage:()->Unit,onDisconnect:()->Unit={}):WebSocket {val current=requireNotNull(session());val wsUrl=current.baseUrl.replaceFirst("https://","wss://").replaceFirst("http://","ws://").trimEnd('/')+"/api/v1/realtime";val request=Request.Builder().url(wsUrl).header("Authorization","Bearer ${current.token}").build();return pinnedClient(current.fingerprint).newWebSocket(request,object:WebSocketListener(){override fun onMessage(webSocket:WebSocket,text:String){onMessage()};override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?){onDisconnect()};override fun onClosed(webSocket:WebSocket,code:Int,reason:String){onDisconnect()}})}
}

private class PinnedTrustManager(fingerprint:String):X509TrustManager{
    private val expected=fingerprint.replace(":","").uppercase()
    override fun checkClientTrusted(chain:Array<out X509Certificate>?,authType:String?)=Unit
    override fun checkServerTrusted(chain:Array<out X509Certificate>?,authType:String?){val certificate=chain?.firstOrNull()?:throw java.security.cert.CertificateException("Server sent no certificate");val actual=MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString(""){"%02X".format(it)};if(actual!=expected)throw java.security.cert.CertificateException("Camtacte certificate fingerprint does not match")}
    override fun getAcceptedIssuers():Array<X509Certificate> = emptyArray()
}
