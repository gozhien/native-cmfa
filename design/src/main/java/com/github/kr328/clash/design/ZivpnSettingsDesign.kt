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
) : Design<ZivpnSettingsDesign.Request>(context) {
    sealed class Request {
        object ApplyNextQuickProfile : Request()
        object SaveCurrentAsQuickProfile : Request()
    }

    private val binding = DesignSettingsCommonBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private val nullableStringAdapter = object : NullableTextAdapter<String> {
        override fun from(value: String): String {
            return value
        }

        override fun to(text: String?): String {
            return text ?: ""
        }
    }

    init {
        binding.surface = surface

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        val screen = preferenceScreen(context) {
            category(R.string.zivpn_settings)

            category(R.string.zivpn_quick_profiles)

            editableTextList(
                value = store::quickProfileEntries,
                adapter = TextAdapter.String,
                icon = R.drawable.ic_baseline_view_list,
                title = R.string.zivpn_quick_profiles,
                placeholder = R.string.zivpn_quick_profiles_placeholder
            )

            clickable(
                icon = R.drawable.ic_baseline_save,
                title = R.string.zivpn_save_current_profile,
                summary = R.string.zivpn_save_current_profile_summary
            ) {
                clicked {
                    requests.trySend(Request.SaveCurrentAsQuickProfile)
                }
            }

            clickable(
                icon = R.drawable.ic_baseline_swap_vert,
                title = R.string.zivpn_apply_next_profile,
                summary = R.string.zivpn_apply_next_profile_summary
            ) {
                clicked {
                    requests.trySend(Request.ApplyNextQuickProfile)
                }
            }

            editableText(
                value = store::serverHost,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_dns,
                title = R.string.zivpn_host,
                placeholder = R.string.zivpn_host
            )

            editableText(
                value = store::serverPass,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_vpn_lock,
                title = R.string.zivpn_pass,
                placeholder = R.string.zivpn_pass
            )

            editableText(
                value = store::serverObfs,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_obfs,
                placeholder = R.string.zivpn_obfs
            )

            editableText(
                value = store::portRanges,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_apps,
                title = R.string.zivpn_ports,
                placeholder = R.string.zivpn_ports_summary
            )

            editableText(
                value = store::recvwindow,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_recv_window,
                placeholder = R.string.zivpn_recv_window
            )

            editableText(
                value = store::recvwindowconn,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_recv_window_conn,
                placeholder = R.string.zivpn_recv_window_conn
            )

            editableText(
                value = store::up,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_up,
                placeholder = R.string.zivpn_up
            )

            editableText(
                value = store::down,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_down,
                placeholder = R.string.zivpn_down
            )

            editableText(
                value = store::clashYaml,
                adapter = nullableStringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_clash_yaml,
                placeholder = R.string.zivpn_clash_yaml
            )
        }

        binding.content.addView(screen.root)
    }
}
