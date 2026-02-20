package com.github.kr328.clash

import com.github.kr328.clash.design.ZivpnSettingsDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.store.ZivpnStore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class ZivpnSettingsActivity : BaseActivity<ZivpnSettingsDesign>() {
    override suspend fun main() {
        val store = ZivpnStore(this)
        val design = ZivpnSettingsDesign(
            this,
            store
        )

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                design.requests.onReceive {
                    when (it) {
                        ZivpnSettingsDesign.Request.ApplyNextQuickProfile -> {
                            val profile = store.applyNextQuickProfile()

                            if (profile == null) {
                                design.showToast(
                                    R.string.zivpn_quick_profiles_empty,
                                    ToastDuration.Long
                                )
                            } else {
                                design.showToast(
                                    getString(
                                        R.string.zivpn_quick_profiles_applied,
                                        profile.name,
                                        profile.host
                                    ),
                                    ToastDuration.Short
                                )
                            }
                        }

                        ZivpnSettingsDesign.Request.SaveCurrentAsQuickProfile -> {
                            val profileName = store.serverHost.ifBlank {
                                getString(R.string.zivpn_quick_profile_default_name)
                            }

                            if (store.serverHost.isBlank()) {
                                design.showToast(
                                    R.string.zivpn_quick_profiles_host_required,
                                    ToastDuration.Long
                                )
                            } else {
                                val profile = store.saveCurrentToQuickProfiles(profileName)

                                design.showToast(
                                    getString(R.string.zivpn_quick_profiles_saved, profile.name),
                                    ToastDuration.Short
                                )
                            }
                        }
                    }
                }

                events.onReceive { }
            }
        }
    }
}
