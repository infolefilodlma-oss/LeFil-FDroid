package net.lefil.fdroid

/**
 * Chaine preparatoire de reception des messages prives Atrium :
 * session authentifiee -> flux valide -> reprise locale -> emission.
 *
 * Ne demarre pas de connexion, de service ou de planification toute seule.
 * L'appelant devra executer runOnce() hors du thread principal Android et
 * fournir un emetteur de notifications qui confirme l'affichage effectif.
 * Aucun contenu de message prive n'est necessaire.
 */
internal class AtriumInboxSync(
    private val fetch: () -> AtriumSessionClient.Result,
    private val checkpoint: AtriumInboxCheckpoint,
    private val publish: (AtriumInboxEvent) -> Boolean
) {
    sealed class Result {
        data class Success(val newlyPublished: Int) : Result()
        object LoginRequired : Result()
        object TransportError : Result()
        object InvalidData : Result()
        object PublishError : Result()
        object CheckpointError : Result()
    }

    fun runOnce(): Result {
        val reply = try {
            fetch()
        } catch (_: Exception) {
            return Result.TransportError
        }

        val body = when (reply) {
            is AtriumSessionClient.Result.Json -> reply.body
            AtriumSessionClient.Result.LoginRequired -> return Result.LoginRequired
            else -> return Result.TransportError
        }

        val batch = AtriumInboxDecoder.decode(body) ?: return Result.InvalidData
        val pending = checkpoint.pending(batch)
        var publishedCount = 0

        // Respecter l'ordre des identifiants : ne jamais franchir un message
        // non affiche en confirmant un message plus recent.
        for (event in pending) {
            val done = try {
                publish(event)
            } catch (_: Exception) {
                false
            }
            if (!done) return Result.PublishError
            if (!checkpoint.confirmPublished(batch, event.id)) return Result.CheckpointError
            publishedCount++
        }
        return Result.Success(publishedCount)
    }
}
