package com.tivanstudio.servera.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.tivanstudio.servera.domain.analytics.Analytics
import com.tivanstudio.servera.domain.analytics.AnalyticsEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends [AnalyticsEvent] to Firebase.
 *
 * Only [AnalyticsEvent.name] and [AnalyticsEvent.params] ever leave here, and both come from
 * constants declared in [AnalyticsEvent] — nothing derived from user input is read, so no host,
 * login, password, command or server name can reach the wire through this class.
 *
 * [firebaseAnalytics] is nullable for the same reason FirebaseRemoteConfig is: a build without
 * `app/google-services.json` has no FirebaseApp, and analytics is a nice-to-have that must not
 * fail injection for the whole app.
 */
@Singleton
class FirebaseAnalyticsImpl @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics?
) : Analytics {

    override fun log(event: AnalyticsEvent) {
        val analytics = firebaseAnalytics ?: return
        val bundle = Bundle(event.params.size).apply {
            event.params.forEach { (key, value) -> putString(key, value) }
        }
        analytics.logEvent(event.name, bundle)
    }
}
