package com.tivanstudio.servera.domain.usecase.auth

import com.tivanstudio.servera.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(oldPassword: String, newPassword: String): Boolean =
        repository.changePassword(oldPassword, newPassword)
}
