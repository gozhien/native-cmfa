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
                                    try {
                                        Log.d("ZIVPN: Adding new profile: ${newProfile.name}")
                                        val newProfiles = store.profiles + newProfile
                                        store.profiles = newProfiles
                                        design.updateList(newProfiles)
                                        launch {
                                            design.showToast(R.string.save, com.github.kr328.clash.design.ui.ToastDuration.Short)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ZIVPN: Failed to add profile", e)
                                    }
                                }
                            }
                            is ZivpnProfilesDesign.Request.Select -> {
                                showProfileMenu(request.profile) { action ->
                                    try {
                                        when (action) {
                                            "use" -> {
                                                Log.d("ZIVPN: Using profile ${request.profile.name}: host=${request.profile.host}, pass=***")
                                                store.serverHost = request.profile.host
                                                store.serverPass = request.profile.pass

                                                launch {
                                                    design.showToast(
                                                        getString(
                                                            R.string.format_profile_activated,
                                                            request.profile.name
                                                        ),
                                                        com.github.kr328.clash.design.ui.ToastDuration.Short
                                                    )
                                                }
                                            }

                                            "edit" -> {
                                                showEditDialog(request.profile) { editedProfile ->
                                                    try {
                                                        Log.d("ZIVPN: Editing profile at index ${request.index}: ${editedProfile.name}")
                                                        val profiles = store.profiles.toMutableList()
                                                        profiles[request.index] = editedProfile
                                                        store.profiles = profiles
                                                        design.updateList(profiles)
                                                        launch {
                                                            design.showToast(R.string.save, com.github.kr328.clash.design.ui.ToastDuration.Short)
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("ZIVPN: Failed to edit profile", e)
                                                    }
                                                }
                                            }

                                            "delete" -> {
                                                try {
                                                    Log.d("ZIVPN: Deleting profile at index ${request.index}")
                                                    val profiles = store.profiles.toMutableList()
                                                    profiles.removeAt(request.index)
                                                    store.profiles = profiles
                                                    design.updateList(profiles)
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

                // Robust port stripping while preserving IPv6
                val host = if (hostRaw.startsWith("[") && hostRaw.contains("]")) {
                    // IPv6 address
                    val closingBracketIndex = hostRaw.indexOf(']')
                    val suffix = hostRaw.substring(closingBracketIndex + 1)
                    if (suffix.startsWith(":")) {
                        hostRaw.substring(0, closingBracketIndex + 1)
                    } else {
                        hostRaw
                    }
                } else if (hostRaw.count { it == ':' } == 1) {
                    // IPv4:port or domain:port
                    hostRaw.substringBefore(':')
                } else {
                    hostRaw
                }

                Log.d("ZIVPN: Profile dialog OK: name=$name, hostRaw=$hostRaw, hostStripped=$host")

                if (name.isNotBlank() && host.isNotBlank()) {
                    onSave(HysteriaProfile(name, host, pass))
                } else {
                    launch {
                        design?.showToast(R.string.should_not_be_blank, com.github.kr328.clash.design.ui.ToastDuration.Short)
                    }
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
