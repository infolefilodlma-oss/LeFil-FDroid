package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class PushNoticeLedgerTest {
    private val stored = mutableSetOf<String>()
    private fun notice(source: String, id: String) =
        PushNotice(source, id, "https://agora.le-fil.net/display/1", "Nouvelle activité", 12)

    @Test fun notifiesOnceForRepeatedWebsocketEventEvenAfterRecreation() {
        var deliveries = 0
        val load = { stored.toSet() }
        val save = { values:Set<String> -> stored.clear();stored.addAll(values) }
        val first = PushNoticeLedger(load, save) { deliveries++;true }
        assertTrue(first.accept(notice("agora","event-1")))
        assertFalse(first.accept(notice("agora","event-1")))
        val restarted = PushNoticeLedger(load,save) { deliveries++;true }
        assertFalse(restarted.accept(notice("agora","event-1")))
        assertEquals(1,deliveries)
    }

    @Test fun sameEventIdOnDifferentServiceStillNotifies() {
        var deliveries=0
        val ledger = PushNoticeLedger({stored.toSet()},{v -> stored.clear();stored.addAll(v)}) { deliveries++;true }
        assertTrue(ledger.accept(notice("agora","42")))
        assertTrue(ledger.accept(notice("atrium","42")))
        assertEquals(2,deliveries)
    }

    @Test fun permissionFailureDoesNotLoseUndeliveredEvent() {
        var canNotify=false
        val ledger = PushNoticeLedger({stored.toSet()},{v -> stored.clear();stored.addAll(v)}) { canNotify }
        assertFalse(ledger.accept(notice("agora","pending")))
        assertTrue(stored.isEmpty())
        canNotify=true
        assertTrue(ledger.accept(notice("agora","pending")))
        assertEquals(1,stored.size)
    }

    @Test fun storesOnlyBoundedEventIds() {
        val ledger = PushNoticeLedger({stored.toSet()},{v -> stored.clear();stored.addAll(v)}) { true }
        repeat(600) { ledger.accept(notice("agora","e$it")) }
        assertTrue(stored.size<=512)
    }
}
