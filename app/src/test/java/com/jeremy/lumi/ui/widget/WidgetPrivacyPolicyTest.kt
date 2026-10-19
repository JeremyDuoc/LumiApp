package com.jeremy.lumi.ui.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPrivacyPolicyTest {
    @Test fun defaultsRemainPrivate() {
        assertFalse(WidgetPrivacyPolicy.canReveal(false, true, false))
        assertFalse(WidgetPrivacyPolicy.canReveal(false, false, false))
    }

    @Test fun requiresCompletedOnboardingAndConsent() {
        assertFalse(WidgetPrivacyPolicy.canReveal(true, false, false))
        assertTrue(WidgetPrivacyPolicy.canReveal(true, true, false))
    }

    @Test fun observerNeverExposesCycleEvenWithPreviousConsent() {
        assertFalse(WidgetPrivacyPolicy.canReveal(true, true, true))
    }

    @Test fun snapshotDefaultContainsNoCycleFacts() {
        val snapshot = WidgetSnapshot()
        assertFalse(snapshot.revealed)
        assertFalse(snapshot.hasCycle)
        assertFalse(snapshot.nextValid)
        assertTrue(snapshot.week.isEmpty())
        assertTrue(snapshot.phase.isEmpty())
    }
}
