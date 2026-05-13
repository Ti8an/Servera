package com.tivanstudio.servera.domain.usecase.auth

import com.tivanstudio.servera.domain.repository.AuthRepository
import javax.inject.Inject

class SetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(password: String) = repository.setPassword(password)
}
