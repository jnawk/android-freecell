package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UndoTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testUndoTableauToTableau() {
        // Setup: Place a king and queen in tableau piles
        val kingOfClubs = Card(Suit.CLUBS, Rank.KING)
        val queenOfHearts = Card(Suit.HEARTS, Rank.QUEEN)
        gameEngine.gameState.tableauPiles[0].add(kingOfClubs)
        gameEngine.gameState.tableauPiles[1].add(queenOfHearts)
        
        // Move queen to king
        assertTrue(gameEngine.moveFromTableauToTableau(1, 0))
        assertEquals(1, gameEngine.moveCount)
        assertEquals(queenOfHearts, gameEngine.gameState.tableauPiles[0].last())
        assertTrue(gameEngine.gameState.tableauPiles[1].isEmpty())
        
        // Undo the move
        assertTrue(gameEngine.undo())
        assertEquals(2, gameEngine.moveCount) // Undo counts as a move
        assertEquals(kingOfClubs, gameEngine.gameState.tableauPiles[0].last())
        assertEquals(queenOfHearts, gameEngine.gameState.tableauPiles[1].last())
    }
    
    @Test
    fun testUndoTableauToFreeCell() {
        // Setup: Place a card in tableau
        val aceOfSpades = Card(Suit.SPADES, Rank.ACE)
        gameEngine.gameState.tableauPiles[0].add(aceOfSpades)
        
        // Move card to free cell
        assertTrue(gameEngine.moveFromTableauToFreeCell(0, 0))
        assertEquals(1, gameEngine.moveCount)
        assertEquals(aceOfSpades, gameEngine.gameState.freeCells[0])
        assertTrue(gameEngine.gameState.tableauPiles[0].isEmpty())
        
        // Undo the move
        assertTrue(gameEngine.undo())
        assertEquals(2, gameEngine.moveCount) // Undo counts as a move
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(aceOfSpades, gameEngine.gameState.tableauPiles[0].last())
    }
    
    @Test
    fun testUndoTableauToFoundation() {
        // Setup: Place an ace in tableau
        val aceOfHearts = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.tableauPiles[0].add(aceOfHearts)
        
        // Move ace to foundation
        assertTrue(gameEngine.moveFromTableauToFoundation(0, Suit.HEARTS))
        assertEquals(1, gameEngine.moveCount)
        assertEquals(aceOfHearts, gameEngine.gameState.foundationPiles[Suit.HEARTS]?.last())
        assertTrue(gameEngine.gameState.tableauPiles[0].isEmpty())
        
        // Undo the move
        assertTrue(gameEngine.undo())
        assertEquals(2, gameEngine.moveCount) // Undo counts as a move
        assertTrue(gameEngine.gameState.foundationPiles[Suit.HEARTS]?.isEmpty() ?: true)
        assertEquals(aceOfHearts, gameEngine.gameState.tableauPiles[0].last())
    }
    
    @Test
    fun testUndoFreeCellToTableau() {
        // Setup: Place a king in free cell and a card in tableau
        val kingOfSpades = Card(Suit.SPADES, Rank.KING)
        gameEngine.gameState.freeCells[0] = kingOfSpades
        
        // Move king to empty tableau
        assertTrue(gameEngine.moveFromFreeCellToTableau(0, 0))
        assertEquals(1, gameEngine.moveCount)
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(kingOfSpades, gameEngine.gameState.tableauPiles[0].last())
        
        // Undo the move
        assertTrue(gameEngine.undo())
        assertEquals(2, gameEngine.moveCount) // Undo counts as a move
        assertEquals(kingOfSpades, gameEngine.gameState.freeCells[0])
        assertTrue(gameEngine.gameState.tableauPiles[0].isEmpty())
    }
    
    @Test
    fun testUndoFreeCellToFoundation() {
        // Setup: Place an ace in free cell
        val aceOfDiamonds = Card(Suit.DIAMONDS, Rank.ACE)
        gameEngine.gameState.freeCells[0] = aceOfDiamonds
        
        // Move ace to foundation
        assertTrue(gameEngine.moveFromFreeCellToFoundation(0, Suit.DIAMONDS))
        assertEquals(1, gameEngine.moveCount)
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(aceOfDiamonds, gameEngine.gameState.foundationPiles[Suit.DIAMONDS]?.last())
        
        // Undo the move
        assertTrue(gameEngine.undo())
        assertEquals(2, gameEngine.moveCount) // Undo counts as a move
        assertEquals(aceOfDiamonds, gameEngine.gameState.freeCells[0])
        assertTrue(gameEngine.gameState.foundationPiles[Suit.DIAMONDS]?.isEmpty() ?: true)
    }
    
    @Test
    fun testUndoFreeCellToFreeCell() {
        // Setup: Place a card in free cell
        val twoOfClubs = Card(Suit.CLUBS, Rank.TWO)
        gameEngine.gameState.freeCells[0] = twoOfClubs
        
        // Move card to another free cell
        assertTrue(gameEngine.moveFromFreeCellToFreeCell(0, 1))
        assertEquals(1, gameEngine.moveCount)
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(twoOfClubs, gameEngine.gameState.freeCells[1])
        
        // Undo the move
        assertTrue(gameEngine.undo())
        assertEquals(2, gameEngine.moveCount) // Undo counts as a move
        assertEquals(twoOfClubs, gameEngine.gameState.freeCells[0])
        assertNull(gameEngine.gameState.freeCells[1])
    }
    
    @Test
    fun testCanUndo() {
        // Initially no moves to undo
        assertFalse(gameEngine.canUndo())
        
        // Make a move
        val card = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.tableauPiles[0].add(card)
        assertTrue(gameEngine.moveFromTableauToFreeCell(0, 0))
        
        // Now we should be able to undo
        assertTrue(gameEngine.canUndo())
        
        // After undoing, we should not be able to undo again
        assertTrue(gameEngine.undo())
        assertFalse(gameEngine.canUndo())
    }
    
    @Test
    fun testUndoMultipleMoves() {
        // Setup: Place cards in tableau
        val kingOfClubs = Card(Suit.CLUBS, Rank.KING)
        val queenOfHearts = Card(Suit.HEARTS, Rank.QUEEN)
        val jackOfSpades = Card(Suit.SPADES, Rank.JACK)
        
        gameEngine.gameState.tableauPiles[0].add(kingOfClubs)
        gameEngine.gameState.tableauPiles[1].add(queenOfHearts)
        gameEngine.gameState.tableauPiles[2].add(jackOfSpades)
        
        // Make multiple moves
        assertTrue(gameEngine.moveFromTableauToTableau(1, 0)) // Queen to King
        assertTrue(gameEngine.moveFromTableauToTableau(2, 0)) // Jack to Queen
        assertEquals(2, gameEngine.moveCount)
        
        // Verify final state
        assertEquals(3, gameEngine.gameState.tableauPiles[0].size)
        assertEquals(jackOfSpades, gameEngine.gameState.tableauPiles[0].last())
        assertTrue(gameEngine.gameState.tableauPiles[1].isEmpty())
        assertTrue(gameEngine.gameState.tableauPiles[2].isEmpty())
        
        // Undo last move
        assertTrue(gameEngine.undo())
        assertEquals(3, gameEngine.moveCount) // Undo counts as a move
        assertEquals(2, gameEngine.gameState.tableauPiles[0].size)
        assertEquals(queenOfHearts, gameEngine.gameState.tableauPiles[0].last())
        assertTrue(gameEngine.gameState.tableauPiles[1].isEmpty())
        assertEquals(jackOfSpades, gameEngine.gameState.tableauPiles[2].last())
        
        // Undo first move
        assertTrue(gameEngine.undo())
        assertEquals(4, gameEngine.moveCount) // Undo counts as a move
        assertEquals(kingOfClubs, gameEngine.gameState.tableauPiles[0].last())
        assertEquals(queenOfHearts, gameEngine.gameState.tableauPiles[1].last())
        assertEquals(jackOfSpades, gameEngine.gameState.tableauPiles[2].last())
        
        // No more moves to undo
        assertFalse(gameEngine.undo())
    }
    
    @Test
    fun testNewGameClearsUndoStack() {
        // Setup: Place a card and make a move
        val card = Card(Suit.HEARTS, Rank.ACE)
        gameEngine.gameState.tableauPiles[0].add(card)
        assertTrue(gameEngine.moveFromTableauToFreeCell(0, 0))
        assertTrue(gameEngine.canUndo())
        
        // Start a new game
        gameEngine.startNewGame()
        
        // Undo stack should be cleared
        assertFalse(gameEngine.canUndo())
    }
}