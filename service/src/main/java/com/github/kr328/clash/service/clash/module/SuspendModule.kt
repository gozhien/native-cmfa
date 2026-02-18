package com.github.kr328.clash.service.clash.module

import android.app.Service
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.core.Clash
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class SuspendModule(service: Service) : Module<Unit>(service) {
    override suspend fun run() {
        // FORCE KEEP ALIVE
        Clash.suspendCore(false)

        val screenToggle = receiveBroadcast(false, Channel.CONFLATED) {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        try {
            while (true) {
                when (screenToggle.receive().action) {
                    Intent.ACTION_SCREEN_ON -> {
                        Clash.suspendCore(false)

                        Log.d("Clash resumed")
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        // FORCE KEEP ALIVE: Do not suspend core when screen is off
                        Clash.suspendCore(false) 

                        Log.d("Clash kept alive (Screen Off)")
                    }
                    else -> {
                        // unreachable

                        Clash.healthCheckAll()
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                Clash.suspendCore(false)
            }
        }
    }
}