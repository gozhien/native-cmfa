package com.github.kr328.clash.service.clash.module

import android.app.Service
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.StatusProvider
import com.github.kr328.clash.service.data.Imported
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.data.SelectionDao
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.util.importedDir
import com.github.kr328.clash.service.util.sendProfileLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*

class ConfigurationModule(service: Service) : Module<ConfigurationModule.LoadException>(service) {
    data class LoadException(val message: String)

    private val store = ServiceStore(service)
    private val zivpnStore = com.github.kr328.clash.service.store.ZivpnStore(service)
    private val reload = Channel<Unit>(Channel.CONFLATED)
    
    // ZIVPN Fixed UUID (Zero Config)
    private val ZIVPN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    override suspend fun run() {
        val broadcasts = receiveBroadcast {
            addAction(Intents.ACTION_PROFILE_CHANGED)
            addAction(Intents.ACTION_OVERRIDE_CHANGED)
        }

        var loaded: UUID? = null

        reload.trySend(Unit)

        while (true) {
            val changed: UUID? = select {
                broadcasts.onReceive {
                    // Ignore profile changes from UI, always force ZIVPN
                    reload.trySend(Unit)
                    null
                }
                reload.onReceive {
                    null
                }
            }

            try {
                // FORCE LOCK: Always use ZIVPN UUID
                store.activeProfile = ZIVPN_UUID
                
                // SYNC DB: Ensure UI knows about this profile
                val dao = com.github.kr328.clash.service.data.Database.database.openImportedDao()
                if (!dao.exists(ZIVPN_UUID)) {
                    val zivpnProfile = Imported(
                        uuid = ZIVPN_UUID,
                        name = "ZIVPN Native",
                        type = Profile.Type.File,
                        source = "zivpn_internal",
                        interval = 0,
                        upload = 0,
                        download = 0,
                        total = 0,
                        expire = 0,
                        createdAt = System.currentTimeMillis()
                    )
                    dao.insert(zivpnProfile)
                    Log.i("ConfigurationModule: Registered ZIVPN profile to DB")
                }
                
                if (ZIVPN_UUID == loaded && changed != null && changed != loaded)
                    continue

                loaded = ZIVPN_UUID

                // 1. Prepare Directory
                val profileDir = service.importedDir.resolve(ZIVPN_UUID.toString())
                profileDir.mkdirs()
                
                // 2. FORCE WRITE Valid Config (Reset every time)
                val configFile = profileDir.resolve("config.yaml")
                val zivpnConfig = zivpnStore.configYaml.takeIf { it.isNotBlank() } ?: """
mixed-port: 7890
allow-lan: false
mode: rule
log-level: debug
external-controller: 127.0.0.1:9090
ipv6: false
geo-auto-update: false
geodata-mode: true

dns:
  enable: true
  ipv6: false
  listen: 0.0.0.0:1053
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  nameserver:
    - https://1.1.1.1/dns-query
    - https://8.8.8.8/dns-query
  fallback:
    - https://1.0.0.1/dns-query
    - https://8.8.4.4/dns-query
  fallback-filter:
    geoip: false
    ipcidr:
      - 240.0.0.0/4

proxies:
  - name: "ZIVPN-Core"
    type: socks5
    server: 127.0.0.1
    port: 7777
    udp: false

proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - "ZIVPN-Core"

rules:
  - MATCH,PROXY
                """.trimIndent()
                
                configFile.writeText(zivpnConfig)

                // 3. Load to Clash Core
                Clash.load(profileDir).await()

                // 4. Update Status
                StatusProvider.currentProfile = "ZIVPN Native"
                service.sendProfileLoaded(ZIVPN_UUID)

                Log.i("ConfigurationModule: ZIVPN Single-Mode Loaded Successfully (Clean Config)")
            } catch (e: Exception) {
                Log.e("ConfigurationModule: Failed to load ZIVPN config", e)
                return enqueueEvent(LoadException(e.message ?: "Unknown"))
            }
        }
    }
}