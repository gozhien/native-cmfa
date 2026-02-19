package com.github.kr328.clash.service.model

import kotlinx.serialization.Serializable

@Serializable
data class HysteriaProfile(
    val name: String,
    val host: String,
    val pass: String
)
