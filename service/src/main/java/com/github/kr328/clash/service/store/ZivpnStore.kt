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

    var serverObfs: String by store.string(
        key = "zivpn_server_obfs",
        defaultValue = "hu``hqb`c"
    )
    
    // Comma separated ranges
    var portRanges: String by store.string(
        key = "zivpn_port_ranges",
        defaultValue = "6000-7750,7751-9500,9501-11250,11251-13000,13001-14750,14751-16500,16501-18250,18251-19999"
    )

    var hysteriaReceiveWindow: String by store.string(
        key = "zivpn_hysteria_receive_window",
        defaultValue = "327680"
    )

    var hysteriaRecvWindowConn: String by store.string(
        key = "zivpn_hysteria_recv_window_conn",
        defaultValue = "131072"
    )

    var hysteriaUp: String by store.string(
        key = "zivpn_hysteria_up",
        defaultValue = ""
    )

    var hysteriaDown: String by store.string(
        key = "zivpn_hysteria_down",
        defaultValue = ""
    )

    var clashYaml: String by store.string(
        key = "zivpn_clash_yaml",
        defaultValue = ""
    )
}
