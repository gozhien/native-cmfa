package com.github.kr328.clash.service.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class HysteriaProfile(
    val name: String,
    val host: String,
    val pass: String
)
