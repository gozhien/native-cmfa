package com.github.kr328.clash

import com.github.kr328.clash.design.ZivpnSettingsDesign
import com.github.kr328.clash.service.store.ZivpnStore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class ZivpnSettingsActivity : BaseActivity<ZivpnSettingsDesign>() {
    override suspend fun main() {
        val design = ZivpnSettingsDesign(
            this,
            ZivpnStore(this)
        )

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    // Handle events if necessary
                }
            }
        }
    }
}
