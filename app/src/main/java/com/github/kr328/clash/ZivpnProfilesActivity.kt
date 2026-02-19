package com.github.kr328.clash

import com.github.kr328.clash.design.ZivpnProfilesDesign
import com.github.kr328.clash.design.databinding.DialogZivpnProfileBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.service.model.HysteriaProfile
import com.github.kr328.clash.service.store.ZivpnStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import com.github.kr328.clash.design.R

class ZivpnProfilesActivity : BaseActivity<ZivpnProfilesDesign>() {
    override suspend fun main() {
        val store = ZivpnStore(this)
        val design = ZivpnProfilesDesign(this, store)

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                design.requests.onReceive { request ->
                    when (request) {
                        is ZivpnProfilesDesign.Request.Add -> {
                            showEditDialog(null) { newProfile ->
                                store.profiles = store.profiles + newProfile
                                design.updateList()
                            }
                        }
                        is ZivpnProfilesDesign.Request.Select -> {
                            showProfileMenu(request.profile) { action ->
                                when (action) {
                                    "use" -> {
                                        store.serverHost = request.profile.host
                                        store.serverPass = request.profile.pass
                                        launch {
                                            design.showToast(getString(R.string.format_profile_activated, request.profile.name), com.github.kr328.clash.design.ui.ToastDuration.Short)
                                        }
                                    }
                                    "edit" -> {
                                        showEditDialog(request.profile) { editedProfile ->
                                            val profiles = store.profiles.toMutableList()
                                            profiles[request.index] = editedProfile
                                            store.profiles = profiles
                                            design.updateList()
                                        }
                                    }
                                    "delete" -> {
                                        val profiles = store.profiles.toMutableList()
                                        profiles.removeAt(request.index)
                                        store.profiles = profiles
                                        design.updateList()
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showEditDialog(profile: HysteriaProfile?, onSave: (HysteriaProfile) -> Unit) {
        val binding = DialogZivpnProfileBinding.inflate(layoutInflater)

        profile?.let {
            binding.nameField.setText(it.name)
            binding.hostField.setText(it.host)
            binding.passField.setText(it.pass)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (profile == null) R.string.zivpn_add_profile else R.string.zivpn_edit_profile)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = binding.nameField.text.toString()
                val host = binding.hostField.text.toString()
                val pass = binding.passField.text.toString()
                if (name.isNotBlank() && host.isNotBlank()) {
                    onSave(HysteriaProfile(name, host, pass))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showProfileMenu(profile: HysteriaProfile, onAction: (String) -> Unit) {
        val options = arrayOf(
            getString(R.string.zivpn_use_profile),
            getString(R.string.edit),
            getString(R.string.delete)
        )
        val actions = arrayOf("use", "edit", "delete")

        MaterialAlertDialogBuilder(this)
            .setTitle(profile.name)
            .setItems(options) { _, which ->
                onAction(actions[which])
            }
            .show()
    }
}
