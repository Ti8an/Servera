package com.tivanstudio.servera.domain.usecase.auth

import com.tivanstudio.servera.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(password: String): Boolean =
        repository.verifyPassword(password)
}
