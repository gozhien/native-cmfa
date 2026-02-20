package com.github.kr328.clash.service.store

import android.content.Context
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider
import com.github.kr328.clash.service.PreferenceProvider

class ZivpnStore(context: Context) {
    data class ServerProfile(
        val name: String,
        val host: String,
        val password: String,
    )

    private val store = Store(
        PreferenceProvider
            .createSharedPreferencesFromContext(context)
            .asStoreProvider()
    )

    var serverHost: String by store.string(
        key = "zivpn_server_host",
        defaultValue = ""
    )

    var serverPass: String by store.string(
        key = "zivpn_server_pass",
        defaultValue = ""
    )

    var serverObfs: String by store.string(
        key = "zivpn_server_obfs",
        defaultValue = "hu``hqb`c"
    )
    
    // Comma separated ranges
    var portRanges: String by store.string(
        key = "zivpn_port_ranges",
        defaultValue = "6000-7750,7751-9500,9501-11250,11251-13000,13001-14750,14751-16500,16501-18250,18251-19999"
    )

    var recvwindow: String by store.string(
        key = "zivpn_recvwindow",
        defaultValue = "3145728"
    )

    var recvwindowconn: String by store.string(
        key = "zivpn_recvwindowconn",
        defaultValue = "12582912"
    )

    var up: String by store.string(
        key = "zivpn_up",
        defaultValue = "1 mbps"
    )

    var down: String by store.string(
        key = "zivpn_down",
        defaultValue = "3 mbps"
    )

    var clashYaml: String by store.string(
        key = "zivpn_clash_yaml",
        defaultValue = ""
    )

    var serverProfilesRaw: String by store.string(
        key = "zivpn_server_profiles",
        defaultValue = ""
    )

    init {
        migrate("zivpn_hysteria_up", "zivpn_up")
        migrate("zivpn_hysteria_down", "zivpn_down")
        migrate("zivpn_hysteria_receive_window", "zivpn_recvwindow")
        migrate("zivpn_hysteria_recv_window_conn", "zivpn_recvwindowconn")
    }

    private fun migrate(oldKey: String, newKey: String) {
        val oldValue = store.provider.getString(oldKey, "")
        if (oldValue.isNotEmpty()) {
            val newValue = store.provider.getString(newKey, "__NOT_SET__")
            if (newValue == "__NOT_SET__") {
                store.provider.setString(newKey, oldValue)
            }
        }
    }

    fun getServerProfiles(): List<ServerProfile> {
        return serverProfilesRaw
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { raw ->
                val parts = raw.split("|", limit = 3)

                if (parts.size < 3) return@mapNotNull null

                val name = parts[0].trim()
                val host = parts[1].trim()
                val password = parts[2]

                if (name.isBlank() || host.isBlank() || password.isBlank()) {
                    null
                } else {
                    ServerProfile(name, host, password)
                }
            }
            .toList()
    }

    fun setServerProfiles(profiles: List<ServerProfile>) {
        serverProfilesRaw = profiles
            .joinToString("\n") { "${it.name}|${it.host}|${it.password}" }
    }
}
