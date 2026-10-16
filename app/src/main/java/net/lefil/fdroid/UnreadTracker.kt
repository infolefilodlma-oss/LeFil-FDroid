package net.lefil.fdroid

internal data class UnreadItem(val id: String, val label: String, val url: String)

internal class UnreadTracker(private val load: (String) -> Set<String>, private val save: (String, Set<String>) -> Unit) {
    fun newlyUnread(source: String, now: List<UnreadItem>): List<UnreadItem> = now.filter { it.id !in load(source) }
    fun remember(source: String, now: List<UnreadItem>) = save(source, now.map { it.id }.toSet())
}
