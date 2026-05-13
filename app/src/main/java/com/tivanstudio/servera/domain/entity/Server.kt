package com.tivanstudio.servera.domain.entity

data class Server(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val login: String,
    val password: String,
    val privateKey: String? = null,
    val timeout: Int = 30,
    val createdAt: Long = System.currentTimeMillis()
)
