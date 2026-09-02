package com.tivanstudio.servera.domain.analytics

/**
 * Reports anonymous usage events. [AnalyticsEvent] is a closed set, which is the whole point:
 * there is no overload taking a free-form name or payload, so this interface cannot be used to
 * ship user data off the device.
 */
interface Analytics {
    fun log(event: AnalyticsEvent)
}
