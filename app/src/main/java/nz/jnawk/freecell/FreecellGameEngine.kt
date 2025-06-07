package nz.jnawk.freecell

// No need to import java.util.Collections if using Kotlin's .shuffle() on MutableList

class FreecellGameEngine {
    val gameState = GameState()

    fun startNewGame() {
        // 1. Reset the game state to ensure it's clean
        gameState.reset()

        // 2. Create a standard 52-card deck
        val deck = mutableListOf<Card>()
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                deck.add(Card(suit, rank))
            }
        }

        // 3. Shuffle the deck
        // Kotlin's built-in shuffle for MutableList is idiomatic
        deck.shuffle()

        // 4. Deal cards to the tableau piles
        // Freecell deal: 4 piles of 7 cards, 4 piles of 6 cards
        var cardIndex = 0
        for (pileIndex in 0 until 8) { // 8 tableau piles
            val cardsInThisPile = if (pileIndex < 4) 7 else 6
            for (j in 0 until cardsInThisPile) {
                if (cardIndex < deck.size) { // Ensure we don't go out of bounds
                    gameState.tableauPiles[pileIndex].add(deck[cardIndex++])
                }
            }
        }

        // At this point, the game state is initialized with a shuffled deck
        // ready for the player to start making moves.
    }

    // --- We will add move validation and execution methods later ---

    /**
     * Check if a card can be moved to a destination tableau pile.
     */
    fun canMoveToTableau(cardToMove: Card, destinationPileTopCard: Card?): Boolean {
        // If destination pile is empty, Kings can be moved there
        if (destinationPileTopCard == null) {
            return cardToMove.rank == Rank.KING
        }

        // Card must be opposite color and one rank lower than the destination card
        return cardToMove.isOppositeColor(destinationPileTopCard) &&
               cardToMove.rank.value == destinationPileTopCard.rank.value - 1
    }

    /**
     * Check if a card can be moved to a foundation pile.
     */
    fun canMoveToFoundation(cardToMove: Card, foundationPile: List<Card>?): Boolean {
        // If foundation is empty, only Ace can be placed
        if (foundationPile.isNullOrEmpty()) {
            return cardToMove.rank == Rank.ACE
        }

        // Card must be same suit and one rank higher than the top card
        val topCard = foundationPile.last()
        return cardToMove.suit == topCard.suit &&
               cardToMove.rank.value == topCard.rank.value + 1
    }

    /**
     * Check if a card can be moved to a free cell.
     */
    fun canMoveToFreeCell(freeCells: Array<Card?>): Boolean {
        // Check if there's at least one empty free cell
        return freeCells.any { it == null }
    }

    /**
     * Move a card from a tableau pile to another tableau pile.
     */
    fun moveFromTableauToTableau(fromPileIndex: Int, toPileIndex: Int): Boolean {
        val fromPile = gameState.tableauPiles[fromPileIndex]
        if (fromPile.isEmpty()) return false
        
        val card = fromPile.last()
        val toPile = gameState.tableauPiles[toPileIndex]
        val topCard = toPile.lastOrNull()
        
        if (canMoveToTableau(card, topCard)) {
            // Remove from source pile
            fromPile.removeAt(fromPile.size - 1)
            // Add to destination pile
            toPile.add(card)
            return true
        }
        
        return false
    }

    /**
     * Move a card from a tableau pile to a free cell.
     */
    fun moveFromTableauToFreeCell(fromPileIndex: Int, toCellIndex: Int): Boolean {
        val fromPile = gameState.tableauPiles[fromPileIndex]
        if (fromPile.isEmpty()) return false
        
        val card = fromPile.last()
        
        if (gameState.freeCells[toCellIndex] == null) {
            // Remove from source pile
            fromPile.removeAt(fromPile.size - 1)
            // Add to free cell
            gameState.freeCells[toCellIndex] = card
            return true
        }
        
        return false
    }

    /**
     * Move a card from a tableau pile to a foundation pile.
     */
    fun moveFromTableauToFoundation(fromPileIndex: Int, toFoundationSuit: Suit): Boolean {
        val fromPile = gameState.tableauPiles[fromPileIndex]
        if (fromPile.isEmpty()) return false
        
        val card = fromPile.last()
        val foundationPile = gameState.foundationPiles[toFoundationSuit]
        
        if (canMoveToFoundation(card, foundationPile)) {
            // Remove from source pile
            fromPile.removeAt(fromPile.size - 1)
            // Add to foundation pile
            if (foundationPile == null) {
                gameState.foundationPiles[toFoundationSuit] = mutableListOf(card)
            } else {
                foundationPile.add(card)
            }
            return true
        }
        
        return false
    }

    /**
     * Placeholder for checking the win condition.
     */
    fun checkWinCondition(): Boolean {
        // TODO: Check if all foundation piles are complete (King on top for each suit)
        // Example: gameState.foundationPiles.all { it.value.lastOrNull()?.rank == Rank.KING }
        return false // Placeholder
    }
}
