package com.tivanstudio.servera.domain.usecase.server

import com.tivanstudio.servera.domain.repository.ServerRepository
import javax.inject.Inject

class DeleteServerUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteServer(id)
}
