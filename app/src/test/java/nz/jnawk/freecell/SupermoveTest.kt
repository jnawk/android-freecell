package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SupermoveTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testCalculateMaxMovableCards_NoEmptyCells() {
        // Fill all free cells
        for (i in 0 until 4) {
            gameEngine.gameState.freeCells[i] = Card(Suit.HEARTS, Rank.ACE)
        }
        
        // No empty tableau piles
        for (i in 0 until 8) {
            if (gameEngine.gameState.tableauPiles[i].isEmpty()) {
                gameEngine.gameState.tableauPiles[i].add(Card(Suit.HEARTS, Rank.TWO))
            }
        }
        
        // With no empty cells and no empty columns, should only be able to move 1 card
        assertEquals(1, gameEngine.calculateMaxMovableCards())
    }
    
    @Test
    fun testCalculateMaxMovableCards_WithEmptyCells() {
        // 2 empty free cells, 2 filled
        gameEngine.gameState.freeCells[0] = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.HEARTS, Rank.TWO)
        gameEngine.gameState.freeCells[2] = null
        gameEngine.gameState.freeCells[3] = null
        
        // No empty tableau piles
        for (i in 0 until 8) {
            if (gameEngine.gameState.tableauPiles[i].isEmpty()) {
                gameEngine.gameState.tableauPiles[i].add(Card(Suit.HEARTS, Rank.TWO))
            }
        }
        
        // With 2 empty cells and no empty columns: (2 + 1) × 2^0 = 3
        assertEquals(3, gameEngine.calculateMaxMovableCards())
    }
    
    @Test
    fun testCalculateMaxMovableCards_WithEmptyColumns() {
        // No empty free cells
        for (i in 0 until 4) {
            gameEngine.gameState.freeCells[i] = Card(Suit.HEARTS, Rank.ACE)
        }
        
        // 2 empty tableau piles
        gameEngine.gameState.tableauPiles[0].clear()
        gameEngine.gameState.tableauPiles[1].clear()
        for (i in 2 until 8) {
            if (gameEngine.gameState.tableauPiles[i].isEmpty()) {
                gameEngine.gameState.tableauPiles[i].add(Card(Suit.HEARTS, Rank.TWO))
            }
        }
        
        // With no empty cells and 2 empty columns: (0 + 1) × 2^2 = 4
        assertEquals(4, gameEngine.calculateMaxMovableCards())
    }
    
    @Test
    fun testCalculateMaxMovableCards_WithEmptyCellsAndColumns() {
        // 3 empty free cells, 1 filled
        gameEngine.gameState.freeCells[0] = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = null
        gameEngine.gameState.freeCells[2] = null
        gameEngine.gameState.freeCells[3] = null
        
        // 2 empty tableau piles
        gameEngine.gameState.tableauPiles[0].clear()
        gameEngine.gameState.tableauPiles[1].clear()
        for (i in 2 until 8) {
            if (gameEngine.gameState.tableauPiles[i].isEmpty()) {
                gameEngine.gameState.tableauPiles[i].add(Card(Suit.HEARTS, Rank.TWO))
            }
        }
        
        // With 3 empty cells and 2 empty columns: (3 + 1) × 2^2 = 16
        assertEquals(16, gameEngine.calculateMaxMovableCards())
    }
    
    @Test
    fun testCanMoveCardSequence_WithinLimits() {
        // Setup: 2 empty free cells
        gameEngine.gameState.freeCells[0] = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.HEARTS, Rank.TWO)
        gameEngine.gameState.freeCells[2] = null
        gameEngine.gameState.freeCells[3] = null
        
        // No empty tableau piles
        for (i in 0 until 8) {
            if (gameEngine.gameState.tableauPiles[i].isEmpty()) {
                gameEngine.gameState.tableauPiles[i].add(Card(Suit.HEARTS, Rank.THREE))
            }
        }
        
        // Create a valid sequence of 3 cards
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE))    // 5♣
        sourcePile.add(Card(Suit.DIAMONDS, Rank.FOUR)) // 4♦
        sourcePile.add(Card(Suit.CLUBS, Rank.THREE))   // 3♣
        
        // Max movable cards is 3 (2 empty cells + 1)
        // Should be able to move all 3 cards
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertTrue(gameEngine.canMoveCardSequence(0, movableIndices))
    }
    
    @Test
    fun testCanMoveCardSequence_ExceedsLimits() {
        // Setup: 1 empty free cell
        gameEngine.gameState.freeCells[0] = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.HEARTS, Rank.TWO)
        gameEngine.gameState.freeCells[2] = Card(Suit.HEARTS, Rank.THREE)
        gameEngine.gameState.freeCells[3] = null
        
        // No empty tableau piles
        for (i in 0 until 8) {
            if (gameEngine.gameState.tableauPiles[i].isEmpty()) {
                gameEngine.gameState.tableauPiles[i].add(Card(Suit.HEARTS, Rank.FOUR))
            }
        }
        
        // Create a valid sequence of 4 cards
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.SIX))     // 6♣
        sourcePile.add(Card(Suit.DIAMONDS, Rank.FIVE)) // 5♦
        sourcePile.add(Card(Suit.CLUBS, Rank.FOUR))    // 4♣
        sourcePile.add(Card(Suit.DIAMONDS, Rank.THREE)) // 3♦
        
        // Max movable cards is 2 (1 empty cell + 1)
        // Should not be able to move all 4 cards
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertFalse(gameEngine.canMoveCardSequence(0, movableIndices))
        
        // But should be able to move just 2 cards
        val limitedIndices = movableIndices.takeLast(2)
        assertTrue(gameEngine.canMoveCardSequence(0, limitedIndices))
    }
}