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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ZivpnSettingsDesign(
    context: Context,
    private val store: ZivpnStore,
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

    private val serverProfileAdapter = object : TextAdapter<ZivpnStore.ServerProfile> {
        override fun from(value: ZivpnStore.ServerProfile): String {
            return "${value.name} | ${value.host} | ${value.password}"
        }

        override fun to(text: String): ZivpnStore.ServerProfile {
            val parts = text.split("|", limit = 3)
            val name = parts.getOrNull(0)?.trim().orEmpty()
            val host = parts.getOrNull(1)?.trim().orEmpty()
            val password = parts.getOrNull(2)?.trim().orEmpty()

            return ZivpnStore.ServerProfile(
                name = name,
                host = host,
                password = password,
            )
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

            val serverProfiles = editableTextList(
                value = ::profiles,
                adapter = serverProfileAdapter,
                icon = R.drawable.ic_baseline_view_list,
                title = R.string.zivpn_server_profiles,
                placeholder = R.string.zivpn_server_profiles_hint,
            )

            val applyNextProfile = clickable(
                title = R.string.zivpn_apply_next_profile,
                icon = R.drawable.ic_baseline_swap_vert,
            )

            fun refreshProfileSummary() {
                val profiles = store.getServerProfiles()
                applyNextProfile.summary = if (profiles.isEmpty()) {
                    context.getText(R.string.zivpn_no_saved_profiles)
                } else {
                    context.getString(R.string.zivpn_saved_profiles_count, profiles.size)
                }
            }

            applyNextProfile.clicked {
                launch(Dispatchers.IO) {
                    val profiles = store.getServerProfiles()

                    if (profiles.isEmpty()) return@launch

                    val currentHost = store.serverHost
                    val currentPassword = store.serverPass
                    val currentIndex = profiles.indexOfFirst {
                        it.host == currentHost && it.password == currentPassword
                    }
                    val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % profiles.size
                    val selected = profiles[nextIndex]

                    store.serverHost = selected.host
                    store.serverPass = selected.password

                    withContext(Dispatchers.Main) {
                        serverProfiles.summary = context.getString(
                            R.string.zivpn_active_profile_summary,
                            selected.name,
                            selected.host,
                        )
                        refreshProfileSummary()
                    }
                }
            }

            launch(Dispatchers.Main) {
                refreshProfileSummary()
            }

            editableText(
                value = store::portRanges,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_apps,
                title = R.string.zivpn_ports,
                placeholder = R.string.zivpn_ports_summary
            )

            editableText(
                value = store::recvwindow,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_recv_window,
                placeholder = R.string.zivpn_recv_window
            )

            editableText(
                value = store::recvwindowconn,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_recv_window_conn,
                placeholder = R.string.zivpn_recv_window_conn
            )

            editableText(
                value = store::up,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_up,
                placeholder = R.string.zivpn_up
            )

            editableText(
                value = store::down,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_down,
                placeholder = R.string.zivpn_down
            )

            editableText(
                value = store::clashYaml,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_info,
                title = R.string.zivpn_clash_yaml,
                placeholder = R.string.zivpn_clash_yaml
            )
        }

        binding.content.addView(screen.root)
    }

    private var profiles: List<ZivpnStore.ServerProfile>?
        get() = store.getServerProfiles().ifEmpty { null }
        set(value) {
            store.setServerProfiles(value ?: emptyList())
        }
}
