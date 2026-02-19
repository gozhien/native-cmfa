package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.model.HysteriaProfile
import com.github.kr328.clash.service.store.ZivpnStore

class ZivpnProfilesDesign(
    context: Context,
    private val store: ZivpnStore,
) : Design<ZivpnProfilesDesign.Request>(context) {
    sealed class Request {
        object Add : Request()
        data class Select(val index: Int, val profile: HysteriaProfile) : Request()
    }

    private val binding = DesignSettingsCommonBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.surface = surface
        binding.activityBarLayout.applyFrom(context)
        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        updateList()
    }

    fun updateList() {
        val screen = preferenceScreen(context) {
            category(R.string.zivpn_profiles)

            clickable(
                title = R.string.zivpn_add_profile,
                icon = R.drawable.ic_baseline_add,
            ) {
                clicked {
                    requests.trySend(Request.Add)
                }
            }

            store.profiles.forEachIndexed { index, profile ->
                clickable(
                    title = R.string.empty,
                    icon = R.drawable.ic_baseline_vpn_lock,
                ) {
                    title = profile.name
                    summary = profile.host

                    clicked {
                        requests.trySend(Request.Select(index, profile))
                    }
                }
            }
        }

        binding.content.removeAllViews()
        binding.content.addView(screen.root)
    }
}
