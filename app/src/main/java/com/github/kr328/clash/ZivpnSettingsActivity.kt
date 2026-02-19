package com.github.kr328.clash

import android.content.Intent
import com.github.kr328.clash.design.ZivpnSettingsDesign
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

        design.render(store)

        while (isActive) {
            select<Unit> {
                design.requests.onReceive {
                    when (it) {
                        ZivpnSettingsDesign.Request.OpenProfiles -> {
                            startActivity(Intent(this@ZivpnSettingsActivity, ZivpnProfilesActivity::class.java))
                        }
                    }
                }
                events.onReceive {
                    when (it) {
                        Event.ActivityStart -> {
                            design.render(store)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
