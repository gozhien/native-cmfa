package com.github.kr328.clash.service.store

import android.content.Context
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider
import com.github.kr328.clash.service.PreferenceProvider

class ZivpnStore(context: Context) {
    data class QuickProfile(
        val name: String,
        val host: String,
        val password: String
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

    private var quickProfilesRaw: String by store.string(
        key = "zivpn_quick_profiles",
        defaultValue = ""
    )

    var quickProfileEntries: List<String>?
        get() = getQuickProfiles().map { "${it.name}|${it.host}|${it.password}" }
        set(value) {
            quickProfilesRaw = value
                ?.mapNotNull { line -> parseProfileLine(line) }
                ?.joinToString("\n") { serializeProfile(it) }
                ?: ""
        }

    var selectedQuickProfile: Int by store.int(
        key = "zivpn_selected_quick_profile",
        defaultValue = 0
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

    fun getQuickProfiles(): List<QuickProfile> {
        return quickProfilesRaw
            .lineSequence()
            .mapNotNull { parseProfileLine(it) }
            .toList()
    }

    fun applyNextQuickProfile(): QuickProfile? {
        val profiles = getQuickProfiles()
        if (profiles.isEmpty()) return null

        val nextIndex = selectedQuickProfile.coerceAtLeast(0) % profiles.size
        val profile = profiles[nextIndex]

        serverHost = profile.host
        serverPass = profile.password
        selectedQuickProfile = (nextIndex + 1) % profiles.size

        return profile
    }

    fun saveCurrentToQuickProfiles(name: String): QuickProfile {
        val trimmedName = name.trim().ifBlank { "Server ${System.currentTimeMillis() / 1000}" }
        val newProfile = QuickProfile(
            name = trimmedName,
            host = serverHost,
            password = serverPass
        )

        val updated = getQuickProfiles()
            .filterNot { it.name.equals(trimmedName, ignoreCase = true) }
            .plus(newProfile)

        quickProfilesRaw = updated.joinToString("\n") { serializeProfile(it) }

        return newProfile
    }

    private fun serializeProfile(profile: QuickProfile): String {
        return "${profile.name}|${profile.host}|${profile.password}"
    }

    private fun parseProfileLine(line: String): QuickProfile? {
        val parts = line.split("|", limit = 3).map { it.trim() }
        if (parts.size < 3) return null

        val (name, host, password) = parts
        if (host.isBlank()) return null

        return QuickProfile(
            name = name.ifBlank { host },
            host = host,
            password = password
        )
    }
}
