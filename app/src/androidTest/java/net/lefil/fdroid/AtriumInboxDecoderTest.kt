package net.lefil.fdroid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Tous les evenements sont fictifs ; aucun acces a HumHub ou a un compte. */
@RunWith(AndroidJUnit4::class)
class AtriumInboxDecoderTest {
    @Test fun fluxMinimalValideEtTrie() {
        val parsed = AtriumInboxDecoder.decode(
            """{"recipient":"guid-123","events":[{"id":9},{"id":7}]}"""
        )
        assertNotNull(parsed)
        assertEquals("guid-123", parsed!!.recipient)
        assertEquals(listOf(7L, 9L), parsed.events.map { it.id })
    }

    @Test fun historiqueVideAccepte() {
        val batch = AtriumInboxDecoder.decode("""{"recipient":"guid-123","events":[]}""")
        assertNotNull(batch)
        assertTrue(batch!!.events.isEmpty())
    }

    @Test fun messageSansDestinataireEstRefuse() {
        assertNull(AtriumInboxDecoder.decode("""{"events":[{"id":3}]}"""))
        assertNull(AtriumInboxDecoder.decode("""{"recipient":"","events":[{"id":3}]}"""))
    }

    @Test fun evenementSansIdentifiantOuMalformeEstRefuse() {
        assertNull(AtriumInboxDecoder.decode("""{"recipient":"x","events":[{"message":"Bonjour"}]}"""))
        assertNull(AtriumInboxDecoder.decode("""{"recipient":"x","events":[{"id":-1}]}"""))
        assertNull(AtriumInboxDecoder.decode("""{"recipient":"x","events":[{"id":2.5}]}"""))
        assertNull(AtriumInboxDecoder.decode("<html>Connexion</html>"))
    }

    @Test fun doublonDansUnLotEstRefuse() {
        assertNull(AtriumInboxDecoder.decode("""{"recipient":"x","events":[{"id":7},{"id":7}]}"""))
    }

    @Test fun lotDemesureOuSansTableauEstRefuse() {
        assertNull(AtriumInboxDecoder.decode("""{"recipient":"x","events":{}}"""))
        val events = (1..101).joinToString(",") { """{"id":$it}""" }
        assertNull(AtriumInboxDecoder.decode("""{"recipient":"x","events":[$events]}"""))
    }

    @Test fun premierLotNeDeclencheAucuneNotificationPuisNouveauMessageOui() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("atrium_inbox_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val checkpoint = AtriumInboxCheckpoint(prefs)
            val first = AtriumInboxDecoder.decode(
                """{"recipient":"compte-A","events":[{"id":41}]}"""
            )!!
            assertTrue(checkpoint.pending(first).isEmpty())

            val next = AtriumInboxDecoder.decode(
                """{"recipient":"compte-A","events":[{"id":41},{"id":42}]}"""
            )!!
            assertEquals(listOf(42L), checkpoint.pending(next).map { it.id })
            // Sans confirmation, l'evenement reste a signaler.
            assertEquals(listOf(42L), checkpoint.pending(next).map { it.id })
            assertTrue(checkpoint.confirmPublished(next, 42L))
            assertTrue(checkpoint.pending(next).isEmpty())
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun changementDeCompteNeRejouePasHistoriquePrecedent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("atrium_inbox_test_accounts", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val checkpoint = AtriumInboxCheckpoint(prefs)
            val a = AtriumInboxDecoder.decode("""{"recipient":"A","events":[{"id":15}]}""")!!
            val b = AtriumInboxDecoder.decode("""{"recipient":"B","events":[{"id":400}]}""")!!
            assertTrue(checkpoint.pending(a).isEmpty())
            assertTrue(checkpoint.pending(b).isEmpty())
            assertFalse(checkpoint.confirmPublished(a, 15L))
            val newB = AtriumInboxDecoder.decode("""{"recipient":"B","events":[{"id":401}]}""")!!
            assertEquals(listOf(401L), checkpoint.pending(newB).map { it.id })
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
