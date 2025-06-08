package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SupermoveUndoTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testSupermoveUndo_TwoCards() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.THREE)) // 3♥
        sourcePile.add(Card(Suit.SPADES, Rank.TWO))   // 2♠

        // Create a destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.DIAMONDS, Rank.FOUR))  // 4♦

        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Move the sequence
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertTrue(result.isNotEmpty())
        assertEquals(0, sourcePile.size)
        assertEquals(3, destPile.size)
        assertEquals(Card(Suit.SPADES, Rank.TWO), destPile.last())
        
        // Verify the move count reflects the actual number of moves made
        assertTrue(gameEngine.moveCount > 2) // Should be more than just 2 moves
        
        // Record the move count before undo
        val moveCountBeforeUndo = gameEngine.moveCount
        
        // Undo the supermove
        while (destPile.size > 1) {
            assertTrue(gameEngine.undo())
        }
        
        // Verify cards are back in the original position and order
        assertEquals(2, sourcePile.size)
        assertEquals(1, destPile.size)
        assertEquals(Card(Suit.HEARTS, Rank.THREE), sourcePile[0])
        assertEquals(Card(Suit.SPADES, Rank.TWO), sourcePile[1])
        assertEquals(Card(Suit.DIAMONDS, Rank.FOUR), destPile[0])
        
        // Verify the move count increased by the number of undos
        assertEquals(moveCountBeforeUndo + (destPile.size - 1), gameEngine.moveCount)
    }
    
    @Test
    fun testSupermoveUndo_ThreeCards() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.FOUR))  // 4♥
        sourcePile.add(Card(Suit.SPADES, Rank.THREE)) // 3♠
        sourcePile.add(Card(Suit.HEARTS, Rank.TWO))   // 2♥

        // Create a destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.CLUBS, Rank.FIVE))     // 5♣

        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Move the sequence
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertTrue(result.isNotEmpty())
        assertEquals(0, sourcePile.size)
        assertEquals(4, destPile.size)
        assertEquals(Card(Suit.HEARTS, Rank.TWO), destPile.last())
        
        // Verify the move count reflects the actual number of moves made
        assertTrue(gameEngine.moveCount > 3) // Should be more than just 3 moves
        
        // Record the move count before undo
        val moveCountBeforeUndo = gameEngine.moveCount
        
        // Undo the supermove
        while (destPile.size > 1) {
            assertTrue(gameEngine.undo())
        }
        
        // Verify cards are back in the original position and order
        assertEquals(3, sourcePile.size)
        assertEquals(1, destPile.size)
        assertEquals(Card(Suit.HEARTS, Rank.FOUR), sourcePile[0])
        assertEquals(Card(Suit.SPADES, Rank.THREE), sourcePile[1])
        assertEquals(Card(Suit.HEARTS, Rank.TWO), sourcePile[2])
        assertEquals(Card(Suit.CLUBS, Rank.FIVE), destPile[0])
    }
    
    @Test
    fun testSupermoveWithFreeCells() {
        // Setup: Fill some free cells to limit resources
        gameEngine.gameState.freeCells[0] = Card(Suit.CLUBS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.CLUBS, Rank.TWO)
        
        // Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.FOUR))  // 4♥
        sourcePile.add(Card(Suit.SPADES, Rank.THREE)) // 3♠
        sourcePile.add(Card(Suit.HEARTS, Rank.TWO))   // 2♥

        // Create a destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.CLUBS, Rank.FIVE))     // 5♣

        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Move the sequence
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertTrue(result.isNotEmpty())
        assertEquals(0, sourcePile.size)
        assertEquals(4, destPile.size)
        
        // Verify free cells were used during the move
        assertTrue(gameEngine.gameState.freeCells.contains(null))
        
        // Undo the supermove
        while (destPile.size > 1) {
            assertTrue(gameEngine.undo())
        }
        
        // Verify cards are back in the original position and order
        assertEquals(3, sourcePile.size)
        assertEquals(1, destPile.size)
        
        // Verify free cells are back to their original state
        assertEquals(Card(Suit.CLUBS, Rank.ACE), gameEngine.gameState.freeCells[0])
        assertEquals(Card(Suit.CLUBS, Rank.TWO), gameEngine.gameState.freeCells[1])
        assertNull(gameEngine.gameState.freeCells[2])
        assertNull(gameEngine.gameState.freeCells[3])
    }
    
    @Test
    fun testSupermoveWithEmptyTableau() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.FOUR))  // 4♥
        sourcePile.add(Card(Suit.SPADES, Rank.THREE)) // 3♠
        sourcePile.add(Card(Suit.HEARTS, Rank.TWO))   // 2♥

        // Create a destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.CLUBS, Rank.FIVE))     // 5♣
        
        // Ensure we have an empty tableau pile
        gameEngine.gameState.tableauPiles[2].clear()
        
        // Get movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)
        
        // Move the sequence
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)
        
        // Verify the move was successful
        assertTrue(result.isNotEmpty())
        assertEquals(0, sourcePile.size)
        assertEquals(4, destPile.size)
        
        // Undo the supermove
        while (destPile.size > 1) {
            assertTrue(gameEngine.undo())
        }
        
        // Verify cards are back in the original position and order
        assertEquals(3, sourcePile.size)
        assertEquals(1, destPile.size)
        assertEquals(Card(Suit.HEARTS, Rank.FOUR), sourcePile[0])
        assertEquals(Card(Suit.SPADES, Rank.THREE), sourcePile[1])
        assertEquals(Card(Suit.HEARTS, Rank.TWO), sourcePile[2])
        assertEquals(Card(Suit.CLUBS, Rank.FIVE), destPile[0])
        
        // Verify the empty tableau pile is still empty
        assertTrue(gameEngine.gameState.tableauPiles[2].isEmpty())
    }
}