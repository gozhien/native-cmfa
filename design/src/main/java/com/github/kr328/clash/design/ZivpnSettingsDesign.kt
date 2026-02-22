package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.databinding.DialogZivpnProfileBinding
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.store.ZivpnStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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

            switch(
                value = store::useActiveProfile,
                icon = R.drawable.ic_baseline_assignment,
                title = R.string.zivpn_use_active_profile,
                summary = R.string.zivpn_use_active_profile_summary,
            )

            val activeProfilePreference = clickable(
                title = R.string.zivpn_active_profile,
                icon = R.drawable.ic_baseline_assignment,
            )

            val profileManagerPreference = clickable(
                title = R.string.zivpn_profile_manager,
                icon = R.drawable.ic_baseline_view_list,
            )

            tips(text = R.string.zivpn_profiles_hint)

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

            suspend fun refreshProfileSummary() {
                val profiles = withContext(Dispatchers.IO) { store.parsedProfiles() }
                val activeProfile = withContext(Dispatchers.IO) { store.activeProfile.trim() }

                profileManagerPreference.summary = context.getString(
                    R.string.zivpn_profile_count_summary,
                    profiles.size,
                )

                activeProfilePreference.summary = if (activeProfile.isEmpty()) {
                    context.getString(R.string.not_set)
                } else {
                    activeProfile
                }
            }

            launch(Dispatchers.Main) {
                refreshProfileSummary()

                activeProfilePreference.clicked {
                    launch(Dispatchers.Main) {
                        val profiles = withContext(Dispatchers.IO) { store.parsedProfiles() }
                        if (profiles.isEmpty()) {
                            return@launch
                        }

                        val selected = chooseProfile(context, profiles, context.getString(R.string.zivpn_active_profile))
                        if (selected != null) {
                            withContext(Dispatchers.IO) {
                                store.activeProfile = selected.name
                            }
                            refreshProfileSummary()
                        }
                    }
                }

                profileManagerPreference.clicked {
                    launch(Dispatchers.Main) {
                        when (chooseProfileAction(context)) {
                            ProfileAction.ADD -> {
                                val model = editProfileDialog(context, null)
                                if (model != null) {
                                    withContext(Dispatchers.IO) {
                                        val profiles = store.parsedProfiles().toMutableList().apply { add(model) }
                                        store.saveProfiles(profiles)
                                        if (store.activeProfile.isBlank()) store.activeProfile = model.name
                                    }
                                }
                            }

                            ProfileAction.EDIT -> {
                                val profiles = withContext(Dispatchers.IO) { store.parsedProfiles() }
                                val selected = chooseProfile(context, profiles, context.getString(R.string.edit))
                                if (selected != null) {
                                    val edited = editProfileDialog(context, selected)
                                    if (edited != null) {
                                        withContext(Dispatchers.IO) {
                                            val updated = store.parsedProfiles().map {
                                                if (it.name.equals(selected.name, ignoreCase = true)) edited else it
                                            }
                                            store.saveProfiles(updated)
                                            if (store.activeProfile.equals(selected.name, ignoreCase = true)) {
                                                store.activeProfile = edited.name
                                            }
                                        }
                                    }
                                }
                            }

                            ProfileAction.DELETE -> {
                                val profiles = withContext(Dispatchers.IO) { store.parsedProfiles() }
                                val selected = chooseProfile(context, profiles, context.getString(R.string.delete))
                                if (selected != null) {
                                    withContext(Dispatchers.IO) {
                                        val updated = store.parsedProfiles().filterNot {
                                            it.name.equals(selected.name, ignoreCase = true)
                                        }
                                        store.saveProfiles(updated)
                                        if (store.activeProfile.equals(selected.name, ignoreCase = true)) {
                                            store.activeProfile = ""
                                        }
                                    }
                                }
                            }

                            null -> Unit
                        }

                        refreshProfileSummary()
                    }
                }
            }
        }

        binding.content.addView(screen.root)
    }
}

private enum class ProfileAction {
    ADD,
    EDIT,
    DELETE,
}

private suspend fun chooseProfileAction(context: Context): ProfileAction? {
    val labels = listOf(
        context.getString(R.string.add),
        context.getString(R.string.edit),
        context.getString(R.string.delete),
    )

    return suspendCancellableCoroutine { continuation ->
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.zivpn_profile_manager)
            .setItems(labels.toTypedArray()) { _, which ->
                if (!continuation.isCompleted) continuation.resume(
                    when (which) {
                        0 -> ProfileAction.ADD
                        1 -> ProfileAction.EDIT
                        2 -> ProfileAction.DELETE
                        else -> null
                    }
                )
            }
            .setOnCancelListener { if (!continuation.isCompleted) continuation.resume(null) }
            .show()
    }
}

private suspend fun chooseProfile(
    context: Context,
    profiles: List<ZivpnStore.ServerProfile>,
    title: String,
): ZivpnStore.ServerProfile? {
    if (profiles.isEmpty()) return null

    return suspendCancellableCoroutine { continuation ->
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(profiles.map { "${it.name} (${it.host})" }.toTypedArray()) { _, which ->
                if (!continuation.isCompleted) continuation.resume(profiles.getOrNull(which))
            }
            .setOnCancelListener { if (!continuation.isCompleted) continuation.resume(null) }
            .show()
    }
}

private suspend fun editProfileDialog(
    context: Context,
    initial: ZivpnStore.ServerProfile?,
): ZivpnStore.ServerProfile? {
    return suspendCancellableCoroutine { continuation ->
        val dialogBinding = DialogZivpnProfileBinding.inflate(context.layoutInflater, context.root, false)

        dialogBinding.nameField.setText(initial?.name ?: "")
        dialogBinding.serverField.setText(initial?.host ?: "")
        dialogBinding.passwordField.setText(initial?.password ?: "")

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (initial == null) R.string.zivpn_profile_add else R.string.zivpn_profile_edit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel) { _, _ ->
                if (!continuation.isCompleted) continuation.resume(null)
            }
            .setOnCancelListener { if (!continuation.isCompleted) continuation.resume(null) }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.nameField.text?.toString()?.trim().orEmpty()
                val host = dialogBinding.serverField.text?.toString()?.trim().orEmpty()
                val password = dialogBinding.passwordField.text?.toString()?.trim().orEmpty()

                if (name.isEmpty()) {
                    dialogBinding.nameLayout.error = context.getString(R.string.zivpn_profile_name_required)
                    return@setOnClickListener
                }
                if (host.isEmpty()) {
                    dialogBinding.serverLayout.error = context.getString(R.string.zivpn_profile_server_required)
                    return@setOnClickListener
                }
                if (password.isEmpty()) {
                    dialogBinding.passwordLayout.error = context.getString(R.string.zivpn_profile_password_required)
                    return@setOnClickListener
                }

                if (!continuation.isCompleted) continuation.resume(
                    ZivpnStore.ServerProfile(
                        name = name,
                        host = host,
                        password = password,
                    )
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
