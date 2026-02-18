package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.store.ZivpnStore

class ZivpnSettingsDesign(
    context: Context,
    store: ZivpnStore,
) : Design<Unit>(context) {

    private val binding = DesignSettingsCommonBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private val stringAdapter = object : NullableTextAdapter<String> {
        override fun from(value: String): String {
            return value
        }

        override fun to(text: String?): String {
            return text ?: ""
        }
    }

    private val intAdapter = object : NullableTextAdapter<Int> {
        override fun from(value: Int): String {
            return value.toString()
        }

        override fun to(text: String?): Int {
            return text?.toIntOrNull() ?: 0
        }
    }

    init {
        binding.surface = surface

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        val screen = preferenceScreen(context) {
            category(R.string.zivpn_settings)

            editableText(
                value = store::serverHost,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_dns,
                title = R.string.zivpn_host,
                placeholder = R.string.zivpn_host
            )
            
             editableText(
                value = store::serverPass,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_vpn_lock,
                title = R.string.zivpn_pass,
                placeholder = R.string.zivpn_pass
            )

             editableText(
                value = store::serverObfs,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_obfs,
                placeholder = R.string.zivpn_obfs
            )

            editableText(
                value = store::portRanges,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_apps,
                title = R.string.zivpn_ports,
                placeholder = R.string.zivpn_ports_summary
            )

            editableText(
                value = store::receiveWindow,
                adapter = intAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_receive_window,
                placeholder = R.string.zivpn_receive_window
            )

            editableText(
                value = store::receiveWindowConn,
                adapter = intAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_receive_window_conn,
                placeholder = R.string.zivpn_receive_window_conn
            )

            editableText(
                value = store::upMbps,
                adapter = intAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_up_mbps,
                placeholder = R.string.zivpn_up_mbps
            )

            editableText(
                value = store::downMbps,
                adapter = intAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_down_mbps,
                placeholder = R.string.zivpn_down_mbps
            )

            editableText(
                value = store::configYaml,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_settings,
                title = R.string.zivpn_config_yaml,
                placeholder = R.string.zivpn_config_yaml
            )

            switch(
                value = store::fastOpen,
                icon = R.drawable.ic_baseline_apps,
                title = R.string.zivpn_fast_open
            )
        }

        binding.content.addView(screen.root)
    }
}
