package com.tivanstudio.servera.domain.usecase.server

import com.tivanstudio.servera.domain.entity.Server
import com.tivanstudio.servera.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServersUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    operator fun invoke(): Flow<List<Server>> = repository.getServers()
}
