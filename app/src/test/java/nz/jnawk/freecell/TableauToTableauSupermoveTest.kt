package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TableauToTableauSupermoveTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testMoveCardSequence_SingleCard() {
        // Setup source pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE))    // 5♣
        
        // Setup destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.HEARTS, Rank.SIX))      // 6♥
        
        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Move the card
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertTrue(result)
        assertEquals(0, sourcePile.size)
        assertEquals(2, destPile.size)
        assertEquals(Card(Suit.CLUBS, Rank.FIVE), destPile.last())
        
        // Verify move was recorded for undo
        assertEquals(1, gameEngine.moveCount)
        assertTrue(gameEngine.canUndo())
    }
    
    @Test
    fun testMoveCardSequence_MultipleCards() {
        // Setup source pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE))    // 5♣
        sourcePile.add(Card(Suit.DIAMONDS, Rank.FOUR)) // 4♦
        sourcePile.add(Card(Suit.CLUBS, Rank.THREE))   // 3♣
        
        // Setup destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.HEARTS, Rank.SIX))      // 6♥
        
        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Move the cards
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertTrue(result)
        assertEquals(0, sourcePile.size)
        assertEquals(4, destPile.size)
        assertEquals(Card(Suit.CLUBS, Rank.THREE), destPile.last())
        
        // Verify each card move was recorded separately
        assertEquals(3, gameEngine.moveCount)
        assertTrue(gameEngine.canUndo())
    }
    
    @Test
    fun testMoveCardSequence_InvalidDestination() {
        // Setup source pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE))    // 5♣
        sourcePile.add(Card(Suit.DIAMONDS, Rank.FOUR)) // 4♦
        
        // Setup destination pile with invalid parent (same color)
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.SPADES, Rank.SIX))      // 6♠ (black, same as 5♣)
        
        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Try to move the cards
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was not successful
        assertFalse(result)
        assertEquals(2, sourcePile.size)
        assertEquals(1, destPile.size)
        assertEquals(0, gameEngine.moveCount)
    }
    
    @Test
    fun testMoveCardSequence_ExceedsMaxMovable() {
        // Fill all free cells except one
        gameEngine.gameState.freeCells[0] = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.HEARTS, Rank.TWO)
        gameEngine.gameState.freeCells[2] = Card(Suit.HEARTS, Rank.THREE)
        
        // Fill all tableau piles to avoid empty columns
        for (i in 0 until 8) {
            if (i != 0 && i != 1) { // Skip the piles we're using for the test
                gameEngine.gameState.tableauPiles[i].add(Card(Suit.HEARTS, Rank.FOUR))
            }
        }
        
        // Setup source pile with 3 cards
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE))    // 5♣
        sourcePile.add(Card(Suit.DIAMONDS, Rank.FOUR)) // 4♦
        sourcePile.add(Card(Suit.CLUBS, Rank.THREE))   // 3♣
        
        // Setup destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.HEARTS, Rank.SIX))      // 6♥
        
        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Try to move the cards (should fail as max movable is 2 with 1 empty free cell)
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was not successful
        assertFalse(result)
        assertEquals(3, sourcePile.size)
        assertEquals(1, destPile.size)
        assertEquals(0, gameEngine.moveCount)
    }
    
    @Test
    fun testMoveCardSequence_ToEmptyTableau() {
        // Setup source pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.KING))    // K♣
        sourcePile.add(Card(Suit.DIAMONDS, Rank.QUEEN)) // Q♦
        
        // Setup empty destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        
        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Move the cards
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertTrue(result)
        assertEquals(0, sourcePile.size)
        assertEquals(2, destPile.size)
        assertEquals(Card(Suit.DIAMONDS, Rank.QUEEN), destPile.last())
        
        // Verify each card move was recorded separately
        assertEquals(2, gameEngine.moveCount)
        assertTrue(gameEngine.canUndo())
    }
    
    @Test
    fun testMoveCardSequence_UndoMultipleCards() {
        // Setup source pile with cards in reverse order (bottom to top)
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE))    // 5♣ (index 0)
        sourcePile.add(Card(Suit.DIAMONDS, Rank.FOUR)) // 4♦ (index 1)
        sourcePile.add(Card(Suit.CLUBS, Rank.THREE))   // 3♣ (index 2)
        
        // Setup destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.HEARTS, Rank.SIX))      // 6♥ (index 0)
        
        // Get movable indices and move the cards
        val movableIndices = gameEngine.getMovableCardIndices(0)
        gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertEquals(0, sourcePile.size)
        assertEquals(4, destPile.size)
        
        // Undo the move (3 cards were moved)
        for (i in 0 until 3) {
            gameEngine.undo()
        }
        
        // Verify all cards are back in the original position
        assertEquals(3, sourcePile.size)
        assertEquals(1, destPile.size)
        
        // Print the cards for debugging
        println("Source pile after undo: ${sourcePile.joinToString()}")
        println("Dest pile after undo: ${destPile.joinToString()}")
        
        // Check each card individually
        assertTrue(sourcePile.contains(Card(Suit.CLUBS, Rank.FIVE)))
        assertTrue(sourcePile.contains(Card(Suit.DIAMONDS, Rank.FOUR)))
        assertTrue(sourcePile.contains(Card(Suit.CLUBS, Rank.THREE)))
        assertEquals(Card(Suit.HEARTS, Rank.SIX), destPile[0])
    }
}