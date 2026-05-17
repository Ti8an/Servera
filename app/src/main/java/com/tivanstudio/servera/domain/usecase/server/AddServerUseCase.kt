package com.tivanstudio.servera.domain.usecase.server

import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.repository.ServerRepository
import javax.inject.Inject

class AddServerUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    suspend operator fun invoke(server: Server): Result<Long> = runCatching {
        require(server.name.isNotBlank()) { "Укажите название" }
        require(server.host.isNotBlank()) { "Укажите хост" }
        require(server.port in 1..65535) { "Неверный порт (1–65535)" }
        require(server.login.isNotBlank()) { "Укажите логин" }
        repository.addServer(server)
    }
}
