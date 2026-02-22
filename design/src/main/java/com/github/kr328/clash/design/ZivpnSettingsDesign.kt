package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.dialog.requestModelTextInput
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.store.ZivpnStore
import com.github.kr328.clash.design.ui.ToastDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    init {
        binding.surface = surface

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        val screen = preferenceScreen(context) {
            category(R.string.zivpn_settings)

            editableText(
                value = store::activeProfileName,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_assignment,
                title = R.string.zivpn_active_profile,
                placeholder = R.string.zivpn_active_profile_hint
            )

            editableTextMap(
                value = store::serverProfiles,
                keyAdapter = stringAdapter,
                valueAdapter = stringAdapter,
                icon = R.drawable.ic_baseline_dns,
                title = R.string.zivpn_server_profiles,
                placeholder = R.string.zivpn_server_profiles_hint
            )

            editableTextMap(
                value = store::passwordProfiles,
                keyAdapter = stringAdapter,
                valueAdapter = stringAdapter,
                icon = R.drawable.ic_baseline_vpn_lock,
                title = R.string.zivpn_password_profiles,
                placeholder = R.string.zivpn_password_profiles_hint
            )

            clickable(
                icon = R.drawable.ic_outline_check_circle,
                title = R.string.zivpn_apply_profile,
                summary = R.string.zivpn_apply_profile_summary
            ) {
                clicked {
                    this@preferenceScreen.launch(Dispatchers.Main) {
                        val profileName = withContext(Dispatchers.IO) {
                            store.activeProfileName.trim()
                        }

                        if (profileName.isBlank()) {
                            this@ZivpnSettingsDesign.showToast(R.string.zivpn_active_profile_hint, ToastDuration.Short)
                            return@launch
                        }

                        val server = withContext(Dispatchers.IO) {
                            store.serverProfiles?.get(profileName)
                        }
                        val password = withContext(Dispatchers.IO) {
                            store.passwordProfiles?.get(profileName)
                        }

                        if (server.isNullOrBlank() || password.isNullOrBlank()) {
                            this@ZivpnSettingsDesign.showToast(R.string.zivpn_profile_not_found, ToastDuration.Short)
                            return@launch
                        }

                        withContext(Dispatchers.IO) {
                            store.serverHost = server
                            store.serverPass = password
                        }

                        this@ZivpnSettingsDesign.showToast(R.string.zivpn_profile_applied, ToastDuration.Short)
                    }
                }
            }

            clickable(
                icon = R.drawable.ic_baseline_add,
                title = R.string.zivpn_save_current_profile,
                summary = R.string.zivpn_save_current_profile_summary
            ) {
                clicked {
                    this@preferenceScreen.launch(Dispatchers.Main) {
                        val initialName = withContext(Dispatchers.IO) {
                            store.activeProfileName
                        }

                        val profileName = context.requestModelTextInput(
                            initial = initialName,
                            title = context.getText(R.string.zivpn_save_current_profile),
                            hint = context.getText(R.string.zivpn_active_profile_hint)
                        ).trim()

                        if (profileName.isBlank()) {
                            this@ZivpnSettingsDesign.showToast(R.string.empty, ToastDuration.Short)
                            return@launch
                        }

                        withContext(Dispatchers.IO) {
                            val updatedServers = (store.serverProfiles ?: emptyMap()).toMutableMap()
                            val updatedPasswords = (store.passwordProfiles ?: emptyMap()).toMutableMap()

                            updatedServers[profileName] = store.serverHost
                            updatedPasswords[profileName] = store.serverPass

                            store.serverProfiles = updatedServers
                            store.passwordProfiles = updatedPasswords
                            store.activeProfileName = profileName
                        }

                        this@ZivpnSettingsDesign.showToast(R.string.zivpn_profile_saved, ToastDuration.Short)
                    }
                }
            }

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
}
