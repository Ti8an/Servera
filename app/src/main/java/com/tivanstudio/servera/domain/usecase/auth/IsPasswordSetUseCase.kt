package com.tivanstudio.servera.domain.usecase.auth

import com.tivanstudio.servera.domain.repository.AuthRepository
import javax.inject.Inject

class IsPasswordSetUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Boolean = repository.isPasswordSet()
}
