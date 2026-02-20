package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.dialog.requestModelTextInput
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.model.ZivpnServerProfile
import com.github.kr328.clash.service.store.ZivpnStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    init {
        binding.surface = surface

        binding.activityBarLayout.applyFrom(context)

        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        rebuild()
    }

    private fun rebuild() {
        binding.content.removeAllViews()

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

            category(R.string.zivpn_profiles)

            clickable(
                title = R.string.zivpn_save_profile,
                icon = R.drawable.ic_baseline_save
            ) {
                clicked {
                    launch {
                        val name = context.requestModelTextInput(
                            initial = "",
                            title = context.getString(R.string.zivpn_profile_name),
                            hint = context.getString(R.string.zivpn_profile_name)
                        )

                        if (name.isNotBlank()) {
                            val profile = ZivpnServerProfile(
                                name = name,
                                host = store.serverHost,
                                pass = store.serverPass
                            )

                            val profiles = try {
                                Json.decodeFromString<List<ZivpnServerProfile>>(store.profiles).toMutableList()
                            } catch (e: Exception) {
                                mutableListOf()
                            }
                            profiles.add(profile)
                            store.profiles = Json.encodeToString(profiles)

                            showToast(R.string.zivpn_profile_saved, ToastDuration.Short)

                            rebuild()
                        }
                    }
                }
            }

            val savedProfiles = try {
                Json.decodeFromString<List<ZivpnServerProfile>>(store.profiles)
            } catch (e: Exception) {
                emptyList()
            }

            savedProfiles.forEachIndexed { index, profile ->
                clickable(
                    title = R.string.empty,
                    icon = R.drawable.ic_baseline_dns
                ) {
                    this.title = profile.name
                    this.summary = profile.host

                    clicked {
                        launch {
                            store.serverHost = profile.host
                            store.serverPass = profile.pass

                            showToast(
                                context.getString(R.string.format_profile_activated, profile.name),
                                ToastDuration.Short
                            )

                            rebuild()
                        }
                    }

                    view.setOnLongClickListener {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.zivpn_delete_profile)
                            .setMessage(profile.name)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                launch {
                                    val profiles = try {
                                        Json.decodeFromString<List<ZivpnServerProfile>>(store.profiles).toMutableList()
                                    } catch (e: Exception) {
                                        mutableListOf()
                                    }
                                    if (index < profiles.size) {
                                        profiles.removeAt(index)
                                        store.profiles = Json.encodeToString(profiles)
                                        showToast(R.string.zivpn_profile_deleted, ToastDuration.Short)
                                        rebuild()
                                    }
                                }
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                        true
                    }
                }
            }
        }

        binding.content.addView(screen.root)
    }
}
