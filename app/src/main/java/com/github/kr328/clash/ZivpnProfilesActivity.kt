package com.github.kr328.clash

import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.design.ZivpnProfilesDesign
import com.github.kr328.clash.design.databinding.DialogZivpnProfileBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.service.model.HysteriaProfile
import com.github.kr328.clash.service.store.ZivpnStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
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
                    try {
                        when (request) {
                            is ZivpnProfilesDesign.Request.Add -> {
                                showEditDialog(null) { newProfile ->
                                    launch(Dispatchers.Main) {
                                        try {
                                            store.profiles = store.profiles + newProfile
                                            design.updateList()
                                        } catch (e: Exception) {
                                            Log.e("ZIVPN: Failed to add profile", e)
                                        }
                                    }
                                }
                            }
                            is ZivpnProfilesDesign.Request.Select -> {
                                showProfileMenu(request.profile) { action ->
                                    launch(Dispatchers.Main) {
                                        try {
                                            when (action) {
                                                "use" -> {
                                                    store.serverHost = request.profile.host
                                                    store.serverPass = request.profile.pass

                                                    design.showToast(
                                                        getString(
                                                            R.string.format_profile_activated,
                                                            request.profile.name
                                                        ),
                                                        com.github.kr328.clash.design.ui.ToastDuration.Short
                                                    )
                                                }

                                                "edit" -> {
                                                    showEditDialog(request.profile) { editedProfile ->
                                                        launch(Dispatchers.Main) {
                                                            try {
                                                                val profiles =
                                                                    store.profiles.toMutableList()
                                                                profiles[request.index] =
                                                                    editedProfile
                                                                store.profiles = profiles
                                                                design.updateList()
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "ZIVPN: Failed to edit profile",
                                                                    e
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                "delete" -> {
                                                    try {
                                                        val profiles = store.profiles.toMutableList()
                                                        profiles.removeAt(request.index)
                                                        store.profiles = profiles
                                                        design.updateList()
                                                    } catch (e: Exception) {
                                                        Log.e("ZIVPN: Failed to delete profile", e)
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ZIVPN: Failed to handle profile selection", e)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ZIVPN: Error processing profile request", e)
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

        MaterialAlertDialogBuilder(this@ZivpnProfilesActivity)
            .setTitle(if (profile == null) R.string.zivpn_add_profile else R.string.zivpn_edit_profile)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                val name = binding.nameField.text.toString().trim()
                val hostRaw = binding.hostField.text.toString().trim()
                val pass = binding.passField.text.toString().trim()

                val host = if (hostRaw.contains('[') && hostRaw.contains(']')) {
                    if (hostRaw.substringAfterLast(']').contains(':'))
                        hostRaw.substringBeforeLast(':')
                    else hostRaw
                } else if (hostRaw.count { it == ':' } == 1) {
                    hostRaw.substringBefore(':')
                } else {
                    hostRaw
                }

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

        MaterialAlertDialogBuilder(this@ZivpnProfilesActivity)
            .setTitle(profile.name)
            .setItems(options) { _, which ->
                onAction(actions[which])
            }
            .show()
    }
}
