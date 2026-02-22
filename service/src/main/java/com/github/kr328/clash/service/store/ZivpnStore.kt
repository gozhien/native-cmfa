package com.github.kr328.clash.service.store

import android.content.Context
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider
import com.github.kr328.clash.service.PreferenceProvider

class ZivpnStore(context: Context) {
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

    var serverProfiles: String by store.string(
        key = "zivpn_server_profiles",
        defaultValue = ""
    )

    var activeProfile: String by store.string(
        key = "zivpn_active_profile",
        defaultValue = ""
    )

    var useActiveProfile: Boolean by store.boolean(
        key = "zivpn_use_active_profile",
        defaultValue = false,
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

    data class ServerProfile(
        val name: String,
        val host: String,
        val password: String,
    )

    fun parsedProfiles(): List<ServerProfile> {
        return serverProfiles
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val chunks = line.split("|").map { it.trim() }

                if (chunks.size < 3) {
                    return@mapNotNull null
                }

                val name = chunks[0]
                val host = chunks[1]
                val password = chunks.subList(2, chunks.size).joinToString("|")

                if (name.isEmpty() || host.isEmpty() || password.isEmpty()) {
                    return@mapNotNull null
                }

                ServerProfile(name, host, password)
            }
            .toList()
    }

    fun saveProfiles(profiles: List<ServerProfile>) {
        serverProfiles = profiles.joinToString("\n") {
            "${it.name}|${it.host}|${it.password}"
        }
    }

    fun resolveConnection(): Pair<String, String> {
        val selected = activeProfile.trim()
        if (useActiveProfile && selected.isNotEmpty()) {
            val profile = parsedProfiles().firstOrNull { it.name.equals(selected, ignoreCase = true) }
            if (profile != null) {
                return profile.host to profile.password
            }
        }

        return serverHost to serverPass
    }
}
