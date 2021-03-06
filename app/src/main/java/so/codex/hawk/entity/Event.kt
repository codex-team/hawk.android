package so.codex.hawk.entity

data class Event(
    val id: String = "",
    val title: String = "",
    val groupHash: String = "",
    val timestamp: Double = 0.0
) {
    companion object {
        val NO_LAST_EVENT = Event(title = "No one catcher connected")
    }
}