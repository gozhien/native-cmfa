package com.github.kr328.clash.service.store

import android.content.Context
import androidx.core.content.edit
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider
import com.github.kr328.clash.service.PreferenceProvider
import com.github.kr328.clash.service.model.HysteriaProfile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ZivpnStore(context: Context) {
    private val sharedPreferences = PreferenceProvider.createSharedPreferencesFromContext(context)
    private val store = Store(sharedPreferences.asStoreProvider())
    private val profilesFile = context.filesDir.resolve("zivpn_profiles.json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

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

    var profiles: List<HysteriaProfile>
        get() = try {
            val jsonString = if (profilesFile.exists()) profilesFile.readText() else "[]"
            Log.d("ZIVPN: Loading profiles from file, raw JSON: $jsonString")
            if (jsonString.isBlank() || jsonString == "null" || jsonString == "[]") {
                emptyList()
            } else {
                json.decodeFromString<List<HysteriaProfile>>(jsonString)
            }
        } catch (e: Exception) {
            Log.e("ZIVPN: Failed to decode profiles from file", e)
            emptyList()
        }
        set(value) {
            try {
                val encoded = json.encodeToString(value)
                profilesFile.writeText(encoded)
                Log.d("ZIVPN: Saved ${value.size} profiles to file")
            } catch (e: Exception) {
                Log.e("ZIVPN: Failed to encode profiles to file", e)
            }
        }

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
}
