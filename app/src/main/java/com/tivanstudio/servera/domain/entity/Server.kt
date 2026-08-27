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
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * True when the stored secrets could not be decrypted -- a row left behind by an older key,
     * or corrupt ciphertext. [password] and [privateKey] are empty in that case, so the server
     * cannot be connected to and only deleting it makes sense.
     */
    val isCorrupted: Boolean = false
)
