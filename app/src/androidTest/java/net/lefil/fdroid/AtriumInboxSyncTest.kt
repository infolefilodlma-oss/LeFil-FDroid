package net.lefil.fdroid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Banc d'essai local : aucun appel au VPS, aucun compte ni donnees privees. */
@RunWith(AndroidJUnit4::class)
class AtriumInboxSyncTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private fun withCheckpoint(name: String, block: (AtriumInboxCheckpoint) -> Unit) {
        val prefs = context.getSharedPreferences("lefil_sync_$name", Context.MODE_PRIVATE)
        check(prefs.edit().clear().commit())
        try {
            block(AtriumInboxCheckpoint(prefs))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    private fun response(recipient: String, vararg ids: Int): AtriumSessionClient.Result {
        val events = ids.joinToString(",") { "{\"id\":$it}" }
        return AtriumSessionClient.Result.Json(
            "{\"recipient\":\"$recipient\",\"events\":[$events]}"
        )
    }

    @Test fun premierLotIgnoreHistoriquePuisPublieNouveauxMessages() = withCheckpoint("new") { checkpoint ->
        var current = response("compte-A", 10)
        val emitted = mutableListOf<Long>()
        val sync = AtriumInboxSync(
            fetch = { current },
            checkpoint = checkpoint,
            publish = { emitted.add(it.id); true }
        )
        assertEquals(AtriumInboxSync.Result.Success(0), sync.runOnce())
        current = response("compte-A", 10, 11, 12)
        assertEquals(AtriumInboxSync.Result.Success(2), sync.runOnce())
        assertEquals(listOf(11L, 12L), emitted)
        assertEquals(AtriumInboxSync.Result.Success(0), sync.runOnce())
    }

    @Test fun erreurEmissionNIgnorePasLeMessageManque() = withCheckpoint("retry") { checkpoint ->
        var current = response("A", 20)
        val emitted = mutableListOf<Long>()
        var failOn22 = true
        val sync = AtriumInboxSync(
            fetch = { current },
            checkpoint = checkpoint,
            publish = {
                if (it.id == 22L && failOn22) false
                else { emitted.add(it.id); true }
            }
        )
        sync.runOnce()
        current = response("A", 20, 21, 22, 23)
        assertTrue(sync.runOnce() === AtriumInboxSync.Result.PublishError)
        assertEquals(listOf(21L), emitted)
        failOn22 = false
        assertEquals(AtriumInboxSync.Result.Success(2), sync.runOnce())
        assertEquals(listOf(21L, 22L, 23L), emitted)
    }

    @Test fun sessionExpireeNePublieRien() = withCheckpoint("expired") { checkpoint ->
        var calls = 0
        val sync = AtriumInboxSync(
            fetch = { AtriumSessionClient.Result.LoginRequired },
            checkpoint = checkpoint,
            publish = { calls++; true }
        )
        assertTrue(sync.runOnce() === AtriumInboxSync.Result.LoginRequired)
        assertEquals(0, calls)
    }

    @Test fun donneesInvalidesNePublientRien() = withCheckpoint("invalid") { checkpoint ->
        var calls = 0
        val sync = AtriumInboxSync(
            fetch = { AtriumSessionClient.Result.Json("{\"recipient\":\"A\",\"events\":[{\"text\":\"prive\"}]}") },
            checkpoint = checkpoint,
            publish = { calls++; true }
        )
        assertTrue(sync.runOnce() === AtriumInboxSync.Result.InvalidData)
        assertEquals(0, calls)
    }

    @Test fun changementDeCompteNeRejouePasAncienHistorique() = withCheckpoint("switch") { checkpoint ->
        var current = response("A", 7)
        val emitted = mutableListOf<Long>()
        val sync = AtriumInboxSync(
            fetch = { current },
            checkpoint = checkpoint,
            publish = { emitted.add(it.id); true }
        )
        sync.runOnce()
        current = response("A", 7, 8)
        assertEquals(AtriumInboxSync.Result.Success(1), sync.runOnce())
        current = response("B", 100)
        assertEquals(AtriumInboxSync.Result.Success(0), sync.runOnce())
        current = response("B", 100, 101)
        assertEquals(AtriumInboxSync.Result.Success(1), sync.runOnce())
        assertEquals(listOf(8L, 101L), emitted)
    }

    @Test fun erreurReseauOuExceptionEstGeree() = withCheckpoint("network") { checkpoint ->
        var calls = 0
        val sync = AtriumInboxSync(
            fetch = { throw IllegalStateException("simulation panne reseau") },
            checkpoint = checkpoint,
            publish = { calls++; true }
        )
        assertTrue(sync.runOnce() === AtriumInboxSync.Result.TransportError)
        assertEquals(0, calls)
    }
}
