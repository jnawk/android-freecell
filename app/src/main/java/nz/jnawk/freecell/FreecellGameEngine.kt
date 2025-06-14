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
     * Represents a move in the Freecell game.
     */
    sealed class SupermoveStep {
        data class TableauToTableau(val fromPileIndex: Int, val toPileIndex: Int, val card: Card) : SupermoveStep()
        data class TableauToFreeCell(val fromPileIndex: Int, val toCellIndex: Int, val card: Card) : SupermoveStep()
        data class FreeCellToTableau(val fromCellIndex: Int, val toPileIndex: Int, val card: Card) : SupermoveStep()
    }

    /**
     * Moves a sequence of cards from one tableau pile to another.
     * Each card is moved individually and recorded separately in the undo stack.
     * @param fromPileIndex The index of the source tableau pile
     * @param toPileIndex The index of the destination tableau pile
     * @param cardIndices The indices of the cards to move, in order from top to bottom
     * @return List of move steps if successful, empty list if the move failed
     */
    fun moveCardSequence(fromPileIndex: Int, toPileIndex: Int, cardIndices: List<Int>): List<SupermoveStep> {
        // If there are no cards to move, return empty list
        if (cardIndices.isEmpty()) {
            return emptyList()
        }

        val fromPile = gameState.tableauPiles[fromPileIndex]
        val toPile = gameState.tableauPiles[toPileIndex]

        // Check if the sequence can be moved based on available resources
        if (!canMoveCardSequence(fromPileIndex, cardIndices)) {
            return emptyList()
        }

        if (fromPile.size < cardIndices.size) {
            // can't make this move if it's trying to move more cards than are present on the pile.
            return emptyList()
        }

        // Get the first card in the sequence (the one that will be placed on the destination)
        val firstCardIndex = cardIndices.first()
        val firstCard = fromPile[firstCardIndex]
        val topDestCard = toPile.lastOrNull()

        // Check if the card can be placed on the destination
        if (!canMoveToTableau(firstCard, topDestCard)) {
            return emptyList()
        }

        // For a single card move, just move it directly
        if (cardIndices.size == 1) {
            val cardIndex = cardIndices[0]
            val card = fromPile[cardIndex]
            fromPile.removeAt(cardIndex)
            toPile.add(card)
            undoStack.push(Move.TableauToTableau(fromPileIndex, toPileIndex, card))
            moveCount++
            return listOf(SupermoveStep.TableauToTableau(fromPileIndex, toPileIndex, card))
        }

        // For multiple cards, we need to use free cells and empty tableau piles as temporary storage
        return executeSupermove(fromPileIndex, toPileIndex, cardIndices)
    }

    /**
     * Execute the moves in a supermove sequence.
     * @param moves The list of move steps to execute
     */
    fun executeMoveSequence(moves: List<SupermoveStep>) {
        for (move in moves) {
            when (move) {
                is SupermoveStep.TableauToTableau -> {
                    val fromPile = gameState.tableauPiles[move.fromPileIndex]
                    val toPile = gameState.tableauPiles[move.toPileIndex]
                    val cardIndex = fromPile.indexOf(move.card)
                    if (cardIndex != -1) {
                        fromPile.removeAt(cardIndex)
                        toPile.add(move.card)
                        undoStack.push(Move.TableauToTableau(move.fromPileIndex, move.toPileIndex, move.card))
                        moveCount++
                    }
                }
                is SupermoveStep.TableauToFreeCell -> {
                    val fromPile = gameState.tableauPiles[move.fromPileIndex]
                    val cardIndex = fromPile.indexOf(move.card)
                    if (cardIndex != -1) {
                        fromPile.removeAt(cardIndex)
                        gameState.freeCells[move.toCellIndex] = move.card
                        undoStack.push(Move.TableauToFreeCell(move.fromPileIndex, move.toCellIndex, move.card))
                        moveCount++
                    }
                }
                is SupermoveStep.FreeCellToTableau -> {
                    val card = gameState.freeCells[move.fromCellIndex]
                    if (card == move.card) {
                        gameState.freeCells[move.fromCellIndex] = null
                        gameState.tableauPiles[move.toPileIndex].add(move.card)
                        undoStack.push(Move.FreeCellToTableau(move.fromCellIndex, move.toPileIndex, move.card))
                        moveCount++
                    }
                }
            }
        }
    }

    /**
     * Calculates the sequence of moves needed for a supermove.
     * This simulates how a player would actually perform the moves.
     * @param fromPileIndex The index of the source tableau pile
     * @param toPileIndex The index of the destination tableau pile
     * @param cardIndices The indices of the cards to move, in order from top to bottom
     * @return List of move steps if successful, empty list if the move failed
     */
    private fun executeSupermove(fromPileIndex: Int, toPileIndex: Int, cardIndices: List<Int>): List<SupermoveStep> {
        val fromPile = gameState.tableauPiles[fromPileIndex]
        val moves = mutableListOf<SupermoveStep>()

        // If we don't have enough resources, fail
        val maxMovableCards = calculateMaxMovableCards()
        if (cardIndices.size > maxMovableCards) {
            return emptyList()
        }

        // Get the cards to move
        val cardsToMove = cardIndices.map { fromPile[it] }

        // Check if the first card can be placed on the destination
        val firstCard = cardsToMove.first()
        val destPile = gameState.tableauPiles[toPileIndex]
        val topDestCard = destPile.lastOrNull()

        if (!canMoveToTableau(firstCard, topDestCard)) {
            return emptyList()
        }

        // For a sequence like Q-J that we want to move to K:
        // 1. Move J to a free cell
        // 2. Move Q to K
        // 3. Move J from free cell to Q

        // First, move all cards except the first one to free cells
        // We need to remove them in reverse order to maintain indices
        val tempStorage = mutableListOf<Pair<Int, Boolean>>() // (index, isTableau)

        // Find available free cells and empty tableau piles
        val availableFreeCells = mutableListOf<Int>()
        for (i in gameState.freeCells.indices) {
            if (gameState.freeCells[i] == null) {
                availableFreeCells.add(i)
            }
        }

        val availableEmptyTableau = mutableListOf<Int>()
        for (i in gameState.tableauPiles.indices) {
            if (i != fromPileIndex && i != toPileIndex && gameState.tableauPiles[i].isEmpty()) {
                availableEmptyTableau.add(i)
            }
        }

        // Calculate the moves for all cards except the first one to temporary storage
        for (i in cardIndices.size - 1 downTo 1) {
            val cardIndex = cardIndices[i]
            val card = fromPile[cardIndex]

            // Store in a free cell or empty tableau
            if (availableFreeCells.isNotEmpty()) {
                val freeCellIndex = availableFreeCells.removeAt(0)
                moves.add(SupermoveStep.TableauToFreeCell(fromPileIndex, freeCellIndex, card))
                tempStorage.add(Pair(freeCellIndex, false))
            } else if (availableEmptyTableau.isNotEmpty()) {
                val emptyTableauIndex = availableEmptyTableau.removeAt(0)
                moves.add(SupermoveStep.TableauToTableau(fromPileIndex, emptyTableauIndex, card))
                tempStorage.add(Pair(emptyTableauIndex, true))
            } else {
                // This shouldn't happen if we checked resources correctly
                return emptyList()
            }
        }

        // Add move for the first card to the destination
        moves.add(SupermoveStep.TableauToTableau(fromPileIndex, toPileIndex, firstCard))

        // Add moves from temporary storage to the destination
        // We need to move them in reverse order of how we stored them
        for (i in tempStorage.indices.reversed()) {
            val (storageIndex, isTableau) = tempStorage[i]
            val card = cardsToMove[cardsToMove.size - 1 - i]
            if (isTableau) {
                moves.add(SupermoveStep.TableauToTableau(storageIndex, toPileIndex, card))
            } else {
                moves.add(SupermoveStep.FreeCellToTableau(storageIndex, toPileIndex, card))
            }
        }

        // Execute all the moves
        executeMoveSequence(moves)

        return moves
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
        // If destination pile is empty, any card can be moved there
        if (destinationPileTopCard == null) {
            return true
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

    /**
     * Undo the last move with animation.
     * @param animateCard Function to animate a card movement
     * @param onComplete Callback to execute when all animations are complete
     * @return true if a move was undone, false if there are no moves to undo
     */
    fun undoWithAnimation(
        animateCard: (Card, CardLocation<Source>, CardLocation<nz.jnawk.freecell.Destination>, () -> Unit) -> Unit,
        onComplete: () -> Unit
    ): Boolean {
        if (undoStack.isEmpty()) {
            onComplete()
            return false
        }

        val lastMove = undoStack.pop()

        when (lastMove) {
            is Move.TableauToTableau -> {
                // Get source and destination locations
                val sourceLocation = CardLocation.Tableau<Source>(lastMove.toPileIndex)
                val destLocation = CardLocation.Tableau<nz.jnawk.freecell.Destination>(lastMove.fromPileIndex)

                // Remove card from source pile
                val toPile = gameState.tableauPiles[lastMove.toPileIndex]
                toPile.removeAt(toPile.size - 1)

                // Animate the card movement
                animateCard(lastMove.card, sourceLocation, destLocation) {
                    // Add card to destination pile after animation completes
                    gameState.tableauPiles[lastMove.fromPileIndex].add(lastMove.card)

                    // Increment move counter for the undo action
                    moveCount++

                    // Complete the undo operation
                    onComplete()
                }
            }

            is Move.TableauToFreeCell -> {
                // Get source and destination locations
                val sourceLocation = CardLocation.FreeCell<Source>(lastMove.toCellIndex)
                val destLocation = CardLocation.Tableau<nz.jnawk.freecell.Destination>(lastMove.fromPileIndex)

                // Remove card from free cell
                gameState.freeCells[lastMove.toCellIndex] = null

                // Animate the card movement
                animateCard(lastMove.card, sourceLocation, destLocation) {
                    // Add card to destination pile after animation completes
                    gameState.tableauPiles[lastMove.fromPileIndex].add(lastMove.card)

                    // Increment move counter for the undo action
                    moveCount++

                    // Complete the undo operation
                    onComplete()
                }
            }

            is Move.TableauToFoundation -> {
                // Get source and destination locations
                val sourceLocation = CardLocation.Foundation<Source>(lastMove.toFoundationSuit)
                val destLocation = CardLocation.Tableau<nz.jnawk.freecell.Destination>(lastMove.fromPileIndex)

                // Remove card from foundation pile
                val foundationPile = gameState.foundationPiles[lastMove.toFoundationSuit]!!
                foundationPile.removeAt(foundationPile.size - 1)

                // Animate the card movement
                animateCard(lastMove.card, sourceLocation, destLocation) {
                    // Add card to destination pile after animation completes
                    gameState.tableauPiles[lastMove.fromPileIndex].add(lastMove.card)

                    // Increment move counter for the undo action
                    moveCount++

                    // Complete the undo operation
                    onComplete()
                }
            }

            is Move.FreeCellToTableau -> {
                // Get source and destination locations
                val sourceLocation = CardLocation.Tableau<Source>(lastMove.toPileIndex)
                val destLocation = CardLocation.FreeCell<nz.jnawk.freecell.Destination>(lastMove.fromCellIndex)

                // Remove card from tableau pile
                val toPile = gameState.tableauPiles[lastMove.toPileIndex]
                toPile.removeAt(toPile.size - 1)

                // Animate the card movement
                animateCard(lastMove.card, sourceLocation, destLocation) {
                    // Add card to free cell after animation completes
                    gameState.freeCells[lastMove.fromCellIndex] = lastMove.card

                    // Increment move counter for the undo action
                    moveCount++

                    // Complete the undo operation
                    onComplete()
                }
            }

            is Move.FreeCellToFoundation -> {
                // Get source and destination locations
                val sourceLocation = CardLocation.Foundation<Source>(lastMove.toFoundationSuit)
                val destLocation = CardLocation.FreeCell<nz.jnawk.freecell.Destination>(lastMove.fromCellIndex)

                // Remove card from foundation pile
                val foundationPile = gameState.foundationPiles[lastMove.toFoundationSuit]!!
                foundationPile.removeAt(foundationPile.size - 1)

                // Animate the card movement
                animateCard(lastMove.card, sourceLocation, destLocation) {
                    // Add card to free cell after animation completes
                    gameState.freeCells[lastMove.fromCellIndex] = lastMove.card

                    // Increment move counter for the undo action
                    moveCount++

                    // Complete the undo operation
                    onComplete()
                }
            }

            is Move.FreeCellToFreeCell -> {
                // Get source and destination locations
                val sourceLocation = CardLocation.FreeCell<Source>(lastMove.toCellIndex)
                val destLocation = CardLocation.FreeCell<nz.jnawk.freecell.Destination>(lastMove.fromCellIndex)

                // Remove card from destination free cell
                gameState.freeCells[lastMove.toCellIndex] = null

                // Animate the card movement
                animateCard(lastMove.card, sourceLocation, destLocation) {
                    // Add card to source free cell after animation completes
                    gameState.freeCells[lastMove.fromCellIndex] = lastMove.card

                    // Increment move counter for the undo action
                    moveCount++

                    // Complete the undo operation
                    onComplete()
                }
            }
        }

        return true
    }



    /**
     * Find valid tableau destinations for a sequence of cards.
     * @param sourcePileIndex The index of the source tableau pile
     * @return List of tableau pile indices that are valid destinations for the entire sequence
     */
    fun findValidTableauDestinationsForSequence(sourcePileIndex: Int): List<Int> {
        val validDestinations = mutableListOf<Int>()
        val sourcePile = gameState.tableauPiles[sourcePileIndex]
        if (sourcePile.isEmpty()) return validDestinations

        // Get movable indices to determine the sequence
        val movableIndices = getMovableCardIndices(sourcePileIndex)
        if (movableIndices.isEmpty()) return validDestinations

        // Get the first card in the sequence
        val firstCardIndex = movableIndices.first()
        val firstCard = sourcePile[firstCardIndex]

        // Check if we have enough resources to move the entire sequence
        val sequenceLength = movableIndices.size
        val maxMovableCards = calculateMaxMovableCards()

        // If we can't move the entire sequence, return empty list
        if (sequenceLength > maxMovableCards) {
            return validDestinations
        }

        for (i in gameState.tableauPiles.indices) {
            if (i == sourcePileIndex) continue // Skip source pile

            val destPile = gameState.tableauPiles[i]
            val topCard = destPile.lastOrNull()

            if (canMoveToTableau(firstCard, topCard)) {
                validDestinations.add(i)
            }
        }

        return validDestinations
    }

    /**
     * Find valid tableau destinations for a specific card in a pile.
     * @param sourcePileIndex The index of the source tableau pile
     * @param cardIndex The index of the card in the pile
     * @return List of tableau pile indices that are valid destinations
     */
    fun findValidTableauDestinationsForCard(sourcePileIndex: Int, cardIndex: Int): List<Int> {
        val validDestinations = mutableListOf<Int>()
        val sourcePile = gameState.tableauPiles[sourcePileIndex]
        if (sourcePile.isEmpty() || cardIndex >= sourcePile.size) return validDestinations

        val card = sourcePile[cardIndex]

        for (i in gameState.tableauPiles.indices) {
            if (i == sourcePileIndex) continue // Skip source pile

            val destPile = gameState.tableauPiles[i]
            val topCard = destPile.lastOrNull()

            if (canMoveToTableau(card, topCard)) {
                validDestinations.add(i)
            }
        }

        return validDestinations
    }

    /**
     * Find valid tableau destinations for the bottom card of a pile.
     * @param sourcePileIndex The index of the source tableau pile
     * @return List of tableau pile indices that are valid destinations
     */
    fun findValidTableauDestinations(sourcePileIndex: Int): List<Int> {
        val validDestinations = mutableListOf<Int>()
        val sourcePile = gameState.tableauPiles[sourcePileIndex]
        if (sourcePile.isEmpty()) return validDestinations

        val card = sourcePile.last()

        for (i in gameState.tableauPiles.indices) {
            if (i == sourcePileIndex) continue // Skip source pile

            val destPile = gameState.tableauPiles[i]
            val topCard = destPile.lastOrNull()

            if (canMoveToTableau(card, topCard)) {
                validDestinations.add(i)
            }
        }

        return validDestinations
    }

    /**
     * Find valid tableau destinations considering resource constraints.
     * @param sourcePileIndex The index of the source tableau pile
     * @return Map of destination pile indices to the maximum length of sequence that can be moved there
     */
    fun findValidTableauDestinationsWithConstraints(sourcePileIndex: Int): Map<Int, Int> {
        val validDestinations = mutableMapOf<Int, Int>()
        val sourcePile = gameState.tableauPiles[sourcePileIndex]
        if (sourcePile.isEmpty()) return validDestinations

        // Get movable indices to determine the sequence
        val movableIndices = getMovableCardIndices(sourcePileIndex)
        if (movableIndices.isEmpty()) return validDestinations

        // Calculate max movable cards
        val maxMovableCards = calculateMaxMovableCards()

        // If we can't move any cards due to resource constraints, return empty map
        if (maxMovableCards <= 0) {
            return validDestinations
        }

        // We can only move subsequences up to maxMovableCards in length
        val maxPossibleLength = minOf(maxMovableCards, movableIndices.size)

        // For each destination pile
        for (i in gameState.tableauPiles.indices) {
            if (i == sourcePileIndex) continue // Skip source pile

            val destPile = gameState.tableauPiles[i]
            val topCard = destPile.lastOrNull()

            // Check subsequences from longest to shortest
            for (length in maxPossibleLength downTo 1) {
                // Get the subsequence indices (from the bottom of the pile)
                val subSequenceIndices = movableIndices.takeLast(length)
                // Get the first (top) card of this subsequence
                val firstCardIndex = subSequenceIndices.first()
                val firstCard = sourcePile[firstCardIndex]

                // Check if this subsequence can be moved to the destination
                if (canMoveToTableau(firstCard, topCard)) {
                    validDestinations[i] = length
                    break
                }
            }
        }

        return validDestinations
    }

    /**
     * Helper class to represent a destination with the cards that would be moved there.
     */
    data class Destination(val pileIndex: Int, val cardsToMove: List<Card>)

    /**
     * Get the cards that would be moved in a supermove.
     * @param sourcePileIndex The index of the source tableau pile
     * @param cardIndex The index of the card in the pile to start the sequence from
     * @param maxLength The maximum number of cards that can be moved
     * @return List of cards that would be moved
     */
    fun getCardsToMove(sourcePileIndex: Int, cardIndex: Int, maxLength: Int): List<Card> {
        val sourcePile = gameState.tableauPiles[sourcePileIndex]
        if (sourcePile.isEmpty() || cardIndex >= sourcePile.size) return emptyList()

        // Get movable indices to determine possible sequences
        val movableIndices = getMovableCardIndices(sourcePileIndex)

        // If the card is not part of a movable sequence, return just that card
        if (!movableIndices.contains(cardIndex)) {
            return listOf(sourcePile[cardIndex])
        }

        // Get the indices from the specified card to the bottom of the pile
        val cardIndices = movableIndices.dropWhile { it < cardIndex }

        // Limit to the maximum allowed length
        val limitedIndices = if (cardIndices.size > maxLength) {
            cardIndices.takeLast(maxLength)
        } else {
            cardIndices
        }

        // Return the cards in the sequence
        return limitedIndices.map { sourcePile[it] }
    }

    /**
     * Find valid tableau destinations for the bottom card of a pile, including which cards would be moved.
     * @param sourcePileIndex The index of the source tableau pile
     * @return Map of tableau pile indices to Destination objects containing the cards that would be moved
     */
    fun findValidTableauDestinationsForBottomCard(sourcePileIndex: Int): Map<Int, Destination> {
        val validDestinations = mutableMapOf<Int, Destination>()
        val sourcePile = gameState.tableauPiles[sourcePileIndex]
        if (sourcePile.isEmpty()) return validDestinations

        // Get movable indices to determine possible sequences
        val movableIndices = getMovableCardIndices(sourcePileIndex)
        if (movableIndices.isEmpty()) return validDestinations

        // Calculate max movable cards
        val maxMovableCards = calculateMaxMovableCards()

        for (i in gameState.tableauPiles.indices) {
            if (i == sourcePileIndex) continue // Skip source pile

            val destPile = gameState.tableauPiles[i]
            val topCard = destPile.lastOrNull()

            // Check for each possible subsequence length
            for (length in 1..minOf(maxMovableCards, movableIndices.size)) {
                val subSequenceIndices = movableIndices.takeLast(length)
                val firstCardIndex = subSequenceIndices.first()
                val firstCard = sourcePile[firstCardIndex]

                if (canMoveToTableau(firstCard, topCard)) {
                    // Create list of cards that would be moved
                    val cardsToMove = subSequenceIndices.map { sourcePile[it] }
                    validDestinations[i] = Destination(i, cardsToMove)
                    break
                }
            }
        }

        return validDestinations
    }
}
