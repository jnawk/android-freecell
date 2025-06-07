package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FreecellGameEngineTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testCanMoveToTableau_EmptyPile_OnlyKingAllowed() {
        // Only kings can be moved to empty tableau piles
        assertTrue(gameEngine.canMoveToTableau(Card(Suit.HEARTS, Rank.KING), null))
        assertTrue(gameEngine.canMoveToTableau(Card(Suit.CLUBS, Rank.KING), null))
        
        assertFalse(gameEngine.canMoveToTableau(Card(Suit.HEARTS, Rank.QUEEN), null))
        assertFalse(gameEngine.canMoveToTableau(Card(Suit.CLUBS, Rank.ACE), null))
    }

    @Test
    fun testCanMoveToTableau_NonEmptyPile_OppositeColorAndOneRankLower() {
        // Red queen can be placed on black king
        assertTrue(gameEngine.canMoveToTableau(
            Card(Suit.HEARTS, Rank.QUEEN), 
            Card(Suit.CLUBS, Rank.KING)
        ))
        assertTrue(gameEngine.canMoveToTableau(
            Card(Suit.DIAMONDS, Rank.QUEEN), 
            Card(Suit.SPADES, Rank.KING)
        ))
        
        // Black jack can be placed on red queen
        assertTrue(gameEngine.canMoveToTableau(
            Card(Suit.CLUBS, Rank.JACK), 
            Card(Suit.HEARTS, Rank.QUEEN)
        ))
        assertTrue(gameEngine.canMoveToTableau(
            Card(Suit.SPADES, Rank.JACK), 
            Card(Suit.DIAMONDS, Rank.QUEEN)
        ))
        
        // Same color not allowed
        assertFalse(gameEngine.canMoveToTableau(
            Card(Suit.HEARTS, Rank.QUEEN), 
            Card(Suit.DIAMONDS, Rank.KING)
        ))
        
        // Same rank not allowed
        assertFalse(gameEngine.canMoveToTableau(
            Card(Suit.CLUBS, Rank.KING), 
            Card(Suit.HEARTS, Rank.KING)
        ))
        
        // Wrong rank difference not allowed
        assertFalse(gameEngine.canMoveToTableau(
            Card(Suit.CLUBS, Rank.JACK), 
            Card(Suit.HEARTS, Rank.KING)
        ))
    }

    @Test
    fun testMoveFromFreeCellToTableau_EmptyPile_OnlyKingAllowed() {
        // Setup: Place a king in a free cell
        val kingOfHearts = Card(Suit.HEARTS, Rank.KING)
        gameEngine.gameState.freeCells[0] = kingOfHearts
        
        // Setup: Place a queen in a free cell
        val queenOfClubs = Card(Suit.CLUBS, Rank.QUEEN)
        gameEngine.gameState.freeCells[1] = queenOfClubs
        
        // Test: King can be moved to empty tableau pile
        assertTrue(gameEngine.moveFromFreeCellToTableau(0, 0))
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(kingOfHearts, gameEngine.gameState.tableauPiles[0].last())
        
        // Test: Queen cannot be moved to empty tableau pile
        assertFalse(gameEngine.moveFromFreeCellToTableau(1, 1))
        assertEquals(queenOfClubs, gameEngine.gameState.freeCells[1])
        assertTrue(gameEngine.gameState.tableauPiles[1].isEmpty())
    }

    @Test
    fun testMoveFromFreeCellToTableau_NonEmptyPile_QueenOnKing() {
        // Setup: Place a king in tableau
        val kingOfClubs = Card(Suit.CLUBS, Rank.KING)
        gameEngine.gameState.tableauPiles[0].add(kingOfClubs)
        
        // Setup: Place a red queen in a free cell
        val queenOfHearts = Card(Suit.HEARTS, Rank.QUEEN)
        gameEngine.gameState.freeCells[0] = queenOfHearts
        
        // Test: Queen can be moved to king of opposite color
        assertTrue(gameEngine.moveFromFreeCellToTableau(0, 0))
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(queenOfHearts, gameEngine.gameState.tableauPiles[0].last())
    }

    @Test
    fun testMoveFromFreeCellToTableau_NonEmptyPile_JackOnQueen() {
        // Setup: Place a queen in tableau
        val queenOfDiamonds = Card(Suit.DIAMONDS, Rank.QUEEN)
        gameEngine.gameState.tableauPiles[0].add(queenOfDiamonds)
        
        // Setup: Place a black jack in a free cell
        val jackOfSpades = Card(Suit.SPADES, Rank.JACK)
        gameEngine.gameState.freeCells[0] = jackOfSpades
        
        // Test: Jack can be moved to queen of opposite color
        assertTrue(gameEngine.moveFromFreeCellToTableau(0, 0))
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(jackOfSpades, gameEngine.gameState.tableauPiles[0].last())
    }
}