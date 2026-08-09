package com.tivanstudio.servera.presentation.common

import androidx.annotation.StringRes
import com.tivanstudio.servera.R
import com.tivanstudio.servera.domain.entity.SshErrorType
import com.tivanstudio.servera.domain.entity.SshException

/** Single place that turns an SSH failure into user-facing text. */
@StringRes
fun SshErrorType?.toMessageRes(): Int = when (this) {
    SshErrorType.AUTH           -> R.string.ssh_error_auth
    SshErrorType.TIMEOUT        -> R.string.ssh_error_timeout
    SshErrorType.UNREACHABLE    -> R.string.ssh_error_unreachable
    SshErrorType.HOST_NOT_FOUND -> R.string.ssh_error_host
    else                        -> R.string.ssh_error_unknown
}

/** Anything that is not a typed [SshException] falls back to the generic message. */
@StringRes
fun Throwable.toSshErrorRes(): Int = (this as? SshException)?.type.toMessageRes()
