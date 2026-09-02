package com.tivanstudio.servera.domain.entity

/** Why an SSH call failed, in terms the UI can turn into a readable message. */
enum class SshErrorType { AUTH, TIMEOUT, UNREACHABLE, HOST_NOT_FOUND, UNKNOWN }

class SshException(val type: SshErrorType, cause: Throwable? = null) : Exception(cause)
