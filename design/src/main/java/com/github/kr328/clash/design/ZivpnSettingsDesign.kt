package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.databinding.DialogZivpnProfilesBinding
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.store.ZivpnStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        val screen = preferenceScreen(context) {
            category(R.string.zivpn_settings)

            val serverUsed = clickable(
                title = R.string.zivpn_server_used,
                icon = R.drawable.ic_baseline_dns,
            )

            val switchProfile = clickable(
                title = R.string.zivpn_switch_profile_popup,
                icon = R.drawable.ic_baseline_swap_vert,
            )

            fun refreshProfileSummary() {
                val currentName = store.serverProfileName
                val currentHost = store.serverHost
                serverUsed.summary = if (currentHost.isBlank()) {
                    context.getText(R.string.zivpn_no_server_used)
                } else {
                    if (currentName.isBlank()) currentHost else "$currentName ($currentHost)"
                }

                val savedCount = store.getServerProfiles().size
                switchProfile.summary = if (savedCount == 0) {
                    context.getText(R.string.zivpn_no_saved_profiles)
                } else {
                    context.getString(R.string.zivpn_saved_profiles_count, savedCount)
                }
            }

            val openProfilePopup = {
                showProfilesPopup {
                    refreshProfileSummary()
                }
            }

            serverUsed.clicked(openProfilePopup)
            switchProfile.clicked(openProfilePopup)

            launch(Dispatchers.Main) {
                refreshProfileSummary()
            }

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

    private fun showProfilesPopup(onChanged: () -> Unit) {
        val popupBinding = DialogZivpnProfilesBinding.inflate(context.layoutInflater)
        val profiles = store.getServerProfiles().toMutableList()
        var selectedIndex = -1

        val listAdapter = ArrayAdapter<String>(
            context,
            android.R.layout.simple_list_item_single_choice,
            mutableListOf(),
        )

        fun refreshList() {
            listAdapter.clear()
            listAdapter.addAll(profiles.map { "${it.name} (${it.host})" })
            listAdapter.notifyDataSetChanged()

            if (selectedIndex in profiles.indices) {
                popupBinding.profileListView.setItemChecked(selectedIndex, true)
            } else {
                popupBinding.profileListView.clearChoices()
                selectedIndex = -1
            }
        }

        fun fillInput(profile: ZivpnStore.ServerProfile) {
            popupBinding.profileNameView.setText(profile.name)
            popupBinding.profileHostView.setText(profile.host)
            popupBinding.profilePasswordView.setText(profile.password)
        }

        popupBinding.profileListView.adapter = listAdapter
        popupBinding.profileListView.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
            fillInput(profiles[position])
        }

        popupBinding.saveProfileView.setOnClickListener {
            val name = popupBinding.profileNameView.text?.toString()?.trim().orEmpty()
            val host = popupBinding.profileHostView.text?.toString()?.trim().orEmpty()
            val password = popupBinding.profilePasswordView.text?.toString()?.trim().orEmpty()

            if (name.isBlank() || host.isBlank() || password.isBlank()) return@setOnClickListener

            val profile = ZivpnStore.ServerProfile(name, host, password)
            val index = profiles.indexOfFirst { it.name.equals(name, ignoreCase = true) }

            if (index >= 0) {
                profiles[index] = profile
                selectedIndex = index
            } else {
                profiles.add(profile)
                selectedIndex = profiles.lastIndex
            }

            store.setServerProfiles(profiles)
            refreshList()
            onChanged()
        }

        popupBinding.editProfileView.setOnClickListener {
            if (selectedIndex !in profiles.indices) return@setOnClickListener

            val name = popupBinding.profileNameView.text?.toString()?.trim().orEmpty()
            val host = popupBinding.profileHostView.text?.toString()?.trim().orEmpty()
            val password = popupBinding.profilePasswordView.text?.toString()?.trim().orEmpty()

            if (name.isBlank() || host.isBlank() || password.isBlank()) return@setOnClickListener

            profiles[selectedIndex] = ZivpnStore.ServerProfile(name, host, password)
            store.setServerProfiles(profiles)
            refreshList()
            onChanged()
        }

        popupBinding.deleteProfileView.setOnClickListener {
            if (selectedIndex !in profiles.indices) return@setOnClickListener

            profiles.removeAt(selectedIndex)
            store.setServerProfiles(profiles)
            selectedIndex = -1
            popupBinding.profileNameView.setText("")
            popupBinding.profileHostView.setText("")
            popupBinding.profilePasswordView.setText("")
            refreshList()
            onChanged()
        }

        refreshList()

        AlertDialog.Builder(context)
            .setTitle(R.string.zivpn_server_profiles)
            .setView(popupBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (selectedIndex !in profiles.indices) return@setPositiveButton

                val selected = profiles[selectedIndex]
                store.serverProfileName = selected.name
                store.serverHost = selected.host
                store.serverPass = selected.password
                onChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
