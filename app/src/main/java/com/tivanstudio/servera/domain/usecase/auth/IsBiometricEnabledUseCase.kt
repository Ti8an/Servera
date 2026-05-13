package com.tivanstudio.servera.domain.usecase.auth

import com.tivanstudio.servera.domain.repository.AuthRepository
import javax.inject.Inject

class IsBiometricEnabledUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean = repository.isBiometricEnabled()
}
