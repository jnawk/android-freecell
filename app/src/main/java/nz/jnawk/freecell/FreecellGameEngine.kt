package nz.jnawk.freecell

import java.util.Stack

/**
 * Represents a move in the Freecell game for undo functionality.
 */
sealed class Move {
    /**
     * Move from tableau pile to another tableau pile.
     */
    data class TableauToTableau(val fromPileIndex: Int, val toPileIndex: Int, val card: Card) : Move()
    
    /**
     * Move from tableau pile to a free cell.
     */
    data class TableauToFreeCell(val fromPileIndex: Int, val toCellIndex: Int, val card: Card) : Move()
    
    /**
     * Move from tableau pile to a foundation pile.
     */
    data class TableauToFoundation(val fromPileIndex: Int, val toFoundationSuit: Suit, val card: Card) : Move()
    
    /**
     * Move from free cell to a tableau pile.
     */
    data class FreeCellToTableau(val fromCellIndex: Int, val toPileIndex: Int, val card: Card) : Move()
    
    /**
     * Move from free cell to a foundation pile.
     */
    data class FreeCellToFoundation(val fromCellIndex: Int, val toFoundationSuit: Suit, val card: Card) : Move()
    
    /**
     * Move from one free cell to another.
     */
    data class FreeCellToFreeCell(val fromCellIndex: Int, val toCellIndex: Int, val card: Card) : Move()
}

class FreecellGameEngine {
    val gameState = GameState()
    
    // Move counter
    var moveCount = 0
        private set
        
    // Undo stack to store moves for undo functionality
    private val undoStack = Stack<Move>()
    
    /**
     * Returns a list of indices of cards that can be moved as a sequence from a tableau pile.
     * The indices are returned in order from top to bottom of the movable sequence.
     * @param pileIndex The index of the tableau pile to check
     * @return List of indices of cards that can be moved, empty if none
     */
    fun getMovableCardIndices(pileIndex: Int): List<Int> {
        val pile = gameState.tableauPiles[pileIndex]
        if (pile.isEmpty()) {
            return emptyList()
        }
        
        val movableIndices = mutableListOf<Int>()
        
        // The bottom card is always movable
        movableIndices.add(pile.size - 1)
        
        // Check for valid sequences from bottom up
        for (i in pile.size - 2 downTo 0) {
            val currentCard = pile[i]
            val nextCard = pile[i + 1]
            
            // Check if cards form a valid sequence (alternating colors, descending ranks)
            if (currentCard.isOppositeColor(nextCard) && 
                currentCard.rank.value == nextCard.rank.value + 1) {
                // Add this card to the beginning of our movable sequence
                movableIndices.add(0, i)
            } else {
                // Sequence is broken
                break
            }
        }
        
        return movableIndices
    }
    
    /**
     * Calculates the maximum number of cards that can be moved based on the Freecell power move formula:
     * (# empty freecells + 1) × 2^(# empty columns)
     * @return The maximum number of cards that can be moved
     */
    fun calculateMaxMovableCards(): Int {
        // Count empty free cells
        val emptyFreeCells = gameState.freeCells.count { it == null }
        
        // Count empty tableau columns
        val emptyColumns = gameState.tableauPiles.count { it.isEmpty() }
        
        // Apply the formula: (# empty freecells + 1) × 2^(# empty columns)
        return (emptyFreeCells + 1) * (1 shl emptyColumns) // 1 shl n is equivalent to 2^n
    }
    
    /**
     * Checks if a sequence of cards can be moved based on the current game state.
     * @param pileIndex The index of the tableau pile containing the cards
     * @param cardIndices The indices of the cards to move, in order from top to bottom
     * @return True if the cards can be moved, false otherwise
     */
    fun canMoveCardSequence(pileIndex: Int, cardIndices: List<Int>): Boolean {
        // If there are no cards to move, return false
        if (cardIndices.isEmpty()) {
            return false
        }
        
        // If only moving one card, it's always allowed
        if (cardIndices.size == 1) {
            return true
        }
        
        // Check if the number of cards to move exceeds the maximum allowed
        val maxMovableCards = calculateMaxMovableCards()
        return cardIndices.size <= maxMovableCards
    }
    
    /**
     * Moves a sequence of cards from one tableau pile to another.
     * Each card is moved individually and recorded separately in the undo stack.
     * @param fromPileIndex The index of the source tableau pile
     * @param toPileIndex The index of the destination tableau pile
     * @param cardIndices The indices of the cards to move, in order from top to bottom
     * @return True if the move was successful, false otherwise
     */
    fun moveCardSequence(fromPileIndex: Int, toPileIndex: Int, cardIndices: List<Int>): Boolean {
        // If there are no cards to move, return false
        if (cardIndices.isEmpty()) {
            return false
        }
        
        val fromPile = gameState.tableauPiles[fromPileIndex]
        val toPile = gameState.tableauPiles[toPileIndex]
        
        // Check if the sequence can be moved based on available resources
        if (!canMoveCardSequence(fromPileIndex, cardIndices)) {
            return false
        }
        
        // Check if the destination is valid for the first card in the sequence
        val firstCardIndex = cardIndices.first()
        val firstCard = fromPile[firstCardIndex]
        val topDestCard = toPile.lastOrNull()
        
        // Check if the card can be placed on the destination
        if (!canMoveToTableau(firstCard, topDestCard)) {
            return false
        }
        
        // Store the cards to move
        val cardsToMove = cardIndices.map { fromPile[it] }
        
        // Remove cards from source pile (in reverse order to maintain indices)
        for (i in cardIndices.size - 1 downTo 0) {
            val cardIndex = cardIndices[i]
            fromPile.removeAt(cardIndex)
        }
        
        // Add cards to destination pile and record each move
        for (card in cardsToMove) {
            toPile.add(card)
            undoStack.push(Move.TableauToTableau(fromPileIndex, toPileIndex, card))
            moveCount++
        }
        
        return true
    }

    fun startNewGame() {
        // 1. Reset the game state to ensure it's clean
        gameState.reset()
        
        // Reset move counter and clear undo stack
        moveCount = 0
        undoStack.clear()

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
        // If destination pile is empty, only Kings can be moved there
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
            // Record move for undo
            undoStack.push(Move.TableauToTableau(fromPileIndex, toPileIndex, card))
            // Increment move counter
            moveCount++
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
            // Record move for undo
            undoStack.push(Move.TableauToFreeCell(fromPileIndex, toCellIndex, card))
            // Increment move counter
            moveCount++
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
            // Record move for undo
            undoStack.push(Move.TableauToFoundation(fromPileIndex, toFoundationSuit, card))
            // Increment move counter
            moveCount++
            return true
        }

        return false
    }

    /**
     * Move a card from a free cell to a tableau pile.
     */
    fun moveFromFreeCellToTableau(fromCellIndex: Int, toPileIndex: Int): Boolean {
        val card = gameState.freeCells[fromCellIndex] ?: return false
        val toPile = gameState.tableauPiles[toPileIndex]

        if (canMoveToTableau(card, toPile.lastOrNull())) {
            gameState.freeCells[fromCellIndex] = null
            toPile.add(card)
            // Record move for undo
            undoStack.push(Move.FreeCellToTableau(fromCellIndex, toPileIndex, card))
            // Increment move counter
            moveCount++
            return true
        }
        return false
    }

    /**
     * Move a card from a free cell to a foundation pile.
     */
    fun moveFromFreeCellToFoundation(fromCellIndex: Int, toFoundationSuit: Suit): Boolean {
        val card = gameState.freeCells[fromCellIndex] ?: return false

        if (canMoveToFoundation(card, gameState.foundationPiles[toFoundationSuit])) {
            gameState.freeCells[fromCellIndex] = null
            // Add to foundation pile
            val foundationPile = gameState.foundationPiles[toFoundationSuit]
            if (foundationPile == null) {
                gameState.foundationPiles[toFoundationSuit] = mutableListOf(card)
            } else {
                foundationPile.add(card)
            }
            // Record move for undo
            undoStack.push(Move.FreeCellToFoundation(fromCellIndex, toFoundationSuit, card))
            // Increment move counter
            moveCount++
            return true
        }
        return false
    }

    /**
     * Move a card from one free cell to another.
     */
    fun moveFromFreeCellToFreeCell(fromCellIndex: Int, toCellIndex: Int): Boolean {
        val card = gameState.freeCells[fromCellIndex] ?: return false

        if (gameState.freeCells[toCellIndex] == null) {
            gameState.freeCells[fromCellIndex] = null
            gameState.freeCells[toCellIndex] = card
            // Record move for undo
            undoStack.push(Move.FreeCellToFreeCell(fromCellIndex, toCellIndex, card))
            // Increment move counter
            moveCount++
            return true
        }
        return false
    }

    /**
     * Check if the game has been won.
     * The game is won when all cards are in the foundation piles.
     */
    fun checkWinCondition(): Boolean {
        // Check if all foundation piles have 13 cards (Ace through King)
        return gameState.foundationPiles.all { (_, pile) -> 
            pile.size == 13 && pile.last().rank == Rank.KING
        }
    }
    
    /**
     * Check if an undo operation is available.
     */
    fun canUndo(): Boolean {
        return undoStack.isNotEmpty()
    }
    
    /**
     * Undo the last move.
     * @return true if a move was undone, false if there are no moves to undo
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) {
            return false
        }
        
        val lastMove = undoStack.pop()
        
        when (lastMove) {
            is Move.TableauToTableau -> {
                // Remove card from destination pile
                val toPile = gameState.tableauPiles[lastMove.toPileIndex]
                toPile.removeAt(toPile.size - 1)
                // Add card back to source pile
                gameState.tableauPiles[lastMove.fromPileIndex].add(lastMove.card)
            }
            
            is Move.TableauToFreeCell -> {
                // Remove card from free cell
                gameState.freeCells[lastMove.toCellIndex] = null
                // Add card back to tableau pile
                gameState.tableauPiles[lastMove.fromPileIndex].add(lastMove.card)
            }
            
            is Move.TableauToFoundation -> {
                // Remove card from foundation pile
                val foundationPile = gameState.foundationPiles[lastMove.toFoundationSuit]!!
                foundationPile.removeAt(foundationPile.size - 1)
                // Add card back to tableau pile
                gameState.tableauPiles[lastMove.fromPileIndex].add(lastMove.card)
            }
            
            is Move.FreeCellToTableau -> {
                // Remove card from tableau pile
                val toPile = gameState.tableauPiles[lastMove.toPileIndex]
                toPile.removeAt(toPile.size - 1)
                // Add card back to free cell
                gameState.freeCells[lastMove.fromCellIndex] = lastMove.card
            }
            
            is Move.FreeCellToFoundation -> {
                // Remove card from foundation pile
                val foundationPile = gameState.foundationPiles[lastMove.toFoundationSuit]!!
                foundationPile.removeAt(foundationPile.size - 1)
                // Add card back to free cell
                gameState.freeCells[lastMove.fromCellIndex] = lastMove.card
            }
            
            is Move.FreeCellToFreeCell -> {
                // Remove card from destination free cell
                gameState.freeCells[lastMove.toCellIndex] = null
                // Add card back to source free cell
                gameState.freeCells[lastMove.fromCellIndex] = lastMove.card
            }
        }
        
        // Increment move counter for the undo action
        moveCount++
        
        return true
    }
}
