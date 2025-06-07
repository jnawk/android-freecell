package nz.jnawk.freecell

class GameState {
    /**
     * The 8 tableau piles where cards are dealt.
     * Each pile is a list of cards. The last card in the list is the topmost card.
     */
    val tableauPiles: List<MutableList<Card>> = List(8) { mutableListOf() }

    /**
     * The 4 FreeCells. Each can hold one card or be empty (null).
     */
    val freeCells: Array<Card?> = arrayOfNulls(4)

    /**
     * The 4 Foundation piles, one for each suit.
     * We'll store them in a Map where the key is the Suit,
     * and the value is a list of cards in that foundation pile (Ace on bottom).
     * Alternatively, you could just store the top rank for each suit.
     */
    val foundationPiles: MutableMap<Suit, MutableList<Card>> = mutableMapOf(
        Suit.HEARTS to mutableListOf(),
        Suit.DIAMONDS to mutableListOf(),
        Suit.CLUBS to mutableListOf(),
        Suit.SPADES to mutableListOf()
    )

    // --- Alternative for Foundation Piles (simpler if you only care about the top card) ---
    // You could use this instead of the foundationPiles MutableMap above if you prefer.
    // Just make sure to use it consistently in your game logic.
    /*
    val foundationTops: MutableMap<Suit, Rank?> = mutableMapOf(
        Suit.HEARTS to null,
        Suit.DIAMONDS to null,
        Suit.CLUBS to null,
        Suit.SPADES to null
    )
    */

    /**
     * Resets the game state to its initial empty configuration.
     * Useful for starting a new game.
     */
    fun reset() {
        tableauPiles.forEach { it.clear() }
        for (i in freeCells.indices) {
            freeCells[i] = null
        }
        foundationPiles.values.forEach { it.clear() }

        // If using foundationTops alternative:
        // foundationTops.keys.forEach { foundationTops[it] = null }
    }
}
