package com.github.kr328.clash.service.model

import kotlinx.serialization.Serializable

@Serializable
data class ZivpnServerProfile(
    val name: String,
    val host: String,
    val pass: String,
)
