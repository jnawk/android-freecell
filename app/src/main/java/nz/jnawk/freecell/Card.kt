package nz.jnawk.freecell

enum class Suit {
    HEARTS, DIAMONDS, CLUBS, SPADES
}

enum class Rank(val value: Int) {
    ACE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
    EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13);

    // Optional: Helper to get a display string, e.g., "A", "K", "7"
    fun getDisplayName(): String {
        return when (this) {
            ACE -> "A"
            JACK -> "J"
            QUEEN -> "Q"
            KING -> "K"
            else -> value.toString()
        }
    }
}

data class Card(val suit: Suit, val rank: Rank) {
    // Function to check if another card is of the opposite color.
    // Useful for tableau stacking rules in Freecell.
    fun isOppositeColor(other: Card): Boolean {
        val isThisRed = suit == Suit.HEARTS || suit == Suit.DIAMONDS
        val isOtherRed = other.suit == Suit.HEARTS || other.suit == Suit.DIAMONDS
        return isThisRed != isOtherRed
    }

    // Optional: A simple string representation for debugging
    override fun toString(): String {
        return "${rank.getDisplayName()}${suit.name.first()}" // e.g., "AH", "7S", "KD"
    }
}
