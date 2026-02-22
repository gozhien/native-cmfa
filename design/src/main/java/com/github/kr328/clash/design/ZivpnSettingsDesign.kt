package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.adapter.EditableTextListAdapter
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.dialog.requestZivpnServerProfileInput
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.model.ZivpnServerProfile
import com.github.kr328.clash.service.store.ZivpnStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ZivpnSettingsDesign(
    context: Context,
    store: ZivpnStore,
) : Design<Unit>(context) {

    private val binding = DesignSettingsCommonBinding
        .inflate(context.layoutInflater, context.root, false)

    private var hostPref: EditableTextPreference? = null
    private var passPref: EditableTextPreference? = null

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

            clickable(
                title = R.string.zivpn_server_profiles,
                icon = R.drawable.ic_baseline_dns,
            ) {
                clicked {
                    launch {
                        manageProfiles(context, store)
                    }
                }
            }

            clickable(
                title = R.string.zivpn_select_profile,
                icon = R.drawable.ic_baseline_dns,
            ) {
                clicked {
                    launch {
                        selectProfile(context, store)
                    }
                }
            }

            hostPref = editableText(
                value = store::serverHost,
                adapter = stringAdapter,
                icon = R.drawable.ic_baseline_dns,
                title = R.string.zivpn_host,
                placeholder = R.string.zivpn_host
            )

            passPref = editableText(
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

    private suspend fun manageProfiles(context: Context, store: ZivpnStore) {
        val profiles = store.getProfiles().toMutableList()
        val adapter = EditableTextListAdapter(context, profiles, object : TextAdapter<ZivpnServerProfile> {
            override fun from(value: ZivpnServerProfile): String = value.name
            override fun to(text: String): ZivpnServerProfile = ZivpnServerProfile(text, "", "")
        })

        adapter.onEdit = { profile ->
            launch {
                val edited = context.requestZivpnServerProfileInput(profile, context.getString(R.string.zivpn_edit_profile))
                if (edited != null) {
                    val index = profiles.indexOf(profile)
                    if (index >= 0) {
                        profiles[index] = edited
                        adapter.notifyItemChanged(index)
                    }
                }
            }
        }

        val result = requestEditableListOverlay(context, adapter, context.getString(R.string.zivpn_server_profiles)) {
            val newProfile = context.requestZivpnServerProfileInput(null, context.getString(R.string.zivpn_add_profile))
            if (newProfile != null) {
                profiles.add(newProfile)
                adapter.notifyItemInserted(profiles.size - 1)
            }
        }

        if (result == EditableListOverlayResult.Apply) {
            store.setProfiles(profiles)
        }
    }

    private suspend fun selectProfile(
        context: Context,
        store: ZivpnStore,
    ) {
        val profiles = store.getProfiles()
        if (profiles.isEmpty()) return

        val names = profiles.map { it.name }.toTypedArray()

        val selectedIndex = suspendCancellableCoroutine<Int> { ctx ->
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.zivpn_select_profile)
                .setItems(names) { _, which ->
                    ctx.resume(which)
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    ctx.resume(-1)
                }
                .setOnDismissListener {
                    if (!ctx.isCompleted) ctx.resume(-1)
                }
                .show()
        }

        if (selectedIndex >= 0) {
            val selected = profiles[selectedIndex]
            store.serverHost = selected.host
            store.serverPass = selected.pass

            hostPref?.text = selected.host
            passPref?.text = selected.pass

            showToast("Profile selected: ${selected.name}")
        }
    }

    private fun showToast(message: String) {
        // Design usually has a way to show messages or I can use context.
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
