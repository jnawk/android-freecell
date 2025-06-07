package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FreeCellIndexBugTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testMoveFromFirstFreeCell() {
        // Setup: Place a 5 of clubs in tableau
        val fiveOfClubs = Card(Suit.CLUBS, Rank.FIVE)
        gameEngine.gameState.tableauPiles[0].add(fiveOfClubs)
        
        // Setup: Place a 4 of hearts in the first free cell
        val fourOfHearts = Card(Suit.HEARTS, Rank.FOUR)
        gameEngine.gameState.freeCells[0] = fourOfHearts
        
        // Test: 4 can be moved to 5 of opposite color
        assertTrue("Should be able to move 4♥ from first free cell to 5♣",
            gameEngine.moveFromFreeCellToTableau(0, 0))
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(fourOfHearts, gameEngine.gameState.tableauPiles[0].last())
    }
    
    @Test
    fun testMoveFromLastFreeCell() {
        // Setup: Place a 5 of clubs in tableau
        val fiveOfClubs = Card(Suit.CLUBS, Rank.FIVE)
        gameEngine.gameState.tableauPiles[0].add(fiveOfClubs)
        
        // Setup: Place a 4 of hearts in the last free cell
        val fourOfHearts = Card(Suit.HEARTS, Rank.FOUR)
        gameEngine.gameState.freeCells[3] = fourOfHearts
        
        // Test: 4 can be moved to 5 of opposite color
        assertTrue("Should be able to move 4♥ from last free cell to 5♣",
            gameEngine.moveFromFreeCellToTableau(3, 0))
        assertNull(gameEngine.gameState.freeCells[3])
        assertEquals(fourOfHearts, gameEngine.gameState.tableauPiles[0].last())
    }
    
    @Test
    fun testMoveQueenFromLastFreeCellToKing() {
        // Setup: Place a king of clubs in tableau
        val kingOfClubs = Card(Suit.CLUBS, Rank.KING)
        gameEngine.gameState.tableauPiles[0].add(kingOfClubs)
        
        // Setup: Place a queen of hearts in the last free cell
        val queenOfHearts = Card(Suit.HEARTS, Rank.QUEEN)
        gameEngine.gameState.freeCells[3] = queenOfHearts
        
        // Test: Queen can be moved to King of opposite color
        assertTrue("Should be able to move Q♥ from last free cell to K♣",
            gameEngine.moveFromFreeCellToTableau(3, 0))
        assertNull(gameEngine.gameState.freeCells[3])
        assertEquals(queenOfHearts, gameEngine.gameState.tableauPiles[0].last())
    }
    
    @Test
    fun testMoveJackFromLastFreeCellToQueen() {
        // Setup: Place a queen of clubs in tableau
        val queenOfClubs = Card(Suit.CLUBS, Rank.QUEEN)
        gameEngine.gameState.tableauPiles[0].add(queenOfClubs)
        
        // Setup: Place a jack of hearts in the last free cell
        val jackOfHearts = Card(Suit.HEARTS, Rank.JACK)
        gameEngine.gameState.freeCells[3] = jackOfHearts
        
        // Test: Jack can be moved to Queen of opposite color
        assertTrue("Should be able to move J♥ from last free cell to Q♣",
            gameEngine.moveFromFreeCellToTableau(3, 0))
        assertNull(gameEngine.gameState.freeCells[3])
        assertEquals(jackOfHearts, gameEngine.gameState.tableauPiles[0].last())
    }
    
    @Test
    fun testMoveBetweenFreeCellsThenToTableau() {
        // Setup: Place a 5 of clubs in tableau
        val fiveOfClubs = Card(Suit.CLUBS, Rank.FIVE)
        gameEngine.gameState.tableauPiles[0].add(fiveOfClubs)
        
        // Setup: Place a 4 of hearts in the last free cell
        val fourOfHearts = Card(Suit.HEARTS, Rank.FOUR)
        gameEngine.gameState.freeCells[3] = fourOfHearts
        
        // First move the card to another free cell
        assertTrue(gameEngine.moveFromFreeCellToFreeCell(3, 0))
        assertNull(gameEngine.gameState.freeCells[3])
        assertEquals(fourOfHearts, gameEngine.gameState.freeCells[0])
        
        // Then move it to the tableau
        assertTrue("Should be able to move 4♥ from first free cell to 5♣ after moving between free cells",
            gameEngine.moveFromFreeCellToTableau(0, 0))
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(fourOfHearts, gameEngine.gameState.tableauPiles[0].last())
    }
    
    // New tests based on the specific game scenario
    
    @Test
    fun test8ClubsToFreeCellAndBack() {
        // Setup: Place 9H (hearts) and 8C (clubs) in tableau pile 1
        val nineOfHearts = Card(Suit.HEARTS, Rank.NINE)
        val eightOfClubs = Card(Suit.CLUBS, Rank.EIGHT)
        gameEngine.gameState.tableauPiles[1].add(nineOfHearts)
        gameEngine.gameState.tableauPiles[1].add(eightOfClubs)
        
        // Move 8C to free cell 1
        assertTrue(gameEngine.moveFromTableauToFreeCell(1, 1))
        assertNull(gameEngine.gameState.freeCells[0]) // First cell should be empty
        assertEquals(eightOfClubs, gameEngine.gameState.freeCells[1]) // Second cell should have 8C
        assertEquals(nineOfHearts, gameEngine.gameState.tableauPiles[1].last()) // 9H should be exposed
        
        // Try to move 8C back to tableau pile 1
        assertTrue("Should be able to move 8C back to 9H",
            gameEngine.moveFromFreeCellToTableau(1, 1))
        assertNull(gameEngine.gameState.freeCells[1])
        assertEquals(eightOfClubs, gameEngine.gameState.tableauPiles[1].last())
    }
    
    @Test
    fun testQueenClubsToFreeCellAndBack() {
        // Setup: Place KD and QC in tableau pile 2
        val kingOfDiamonds = Card(Suit.DIAMONDS, Rank.KING)
        val queenOfClubs = Card(Suit.CLUBS, Rank.QUEEN)
        gameEngine.gameState.tableauPiles[2].add(kingOfDiamonds)
        gameEngine.gameState.tableauPiles[2].add(queenOfClubs)
        
        // Move QC to free cell 2
        assertTrue(gameEngine.moveFromTableauToFreeCell(2, 2))
        assertEquals(queenOfClubs, gameEngine.gameState.freeCells[2])
        assertEquals(kingOfDiamonds, gameEngine.gameState.tableauPiles[2].last())
        
        // Try to move QC back to tableau pile 2
        assertTrue("Should be able to move QC back to KD",
            gameEngine.moveFromFreeCellToTableau(2, 2))
        assertNull(gameEngine.gameState.freeCells[2])
        assertEquals(queenOfClubs, gameEngine.gameState.tableauPiles[2].last())
    }
    
    @Test
    fun test4DiamondsToFreeCellAndBack() {
        // Setup: Place 5S and 4D in tableau pile 3
        val fiveOfSpades = Card(Suit.SPADES, Rank.FIVE)
        val fourOfDiamonds = Card(Suit.DIAMONDS, Rank.FOUR)
        gameEngine.gameState.tableauPiles[3].add(fiveOfSpades)
        gameEngine.gameState.tableauPiles[3].add(fourOfDiamonds)
        
        // Move 4D to free cell 3
        assertTrue(gameEngine.moveFromTableauToFreeCell(3, 3))
        assertEquals(fourOfDiamonds, gameEngine.gameState.freeCells[3])
        assertEquals(fiveOfSpades, gameEngine.gameState.tableauPiles[3].last())
        
        // Try to move 4D back to tableau pile 3
        assertTrue("Should be able to move 4D back to 5S",
            gameEngine.moveFromFreeCellToTableau(3, 3))
        assertNull(gameEngine.gameState.freeCells[3])
        assertEquals(fourOfDiamonds, gameEngine.gameState.tableauPiles[3].last())
    }
    
    @Test
    fun testCardTrappedInFreeCellWorkaround() {
        // Setup: Place 5S and 4D in tableau pile 3
        val fiveOfSpades = Card(Suit.SPADES, Rank.FIVE)
        val fourOfDiamonds = Card(Suit.DIAMONDS, Rank.FOUR)
        gameEngine.gameState.tableauPiles[3].add(fiveOfSpades)
        gameEngine.gameState.tableauPiles[3].add(fourOfDiamonds)
        
        // Move 4D to free cell 3
        assertTrue(gameEngine.moveFromTableauToFreeCell(3, 3))
        assertEquals(fourOfDiamonds, gameEngine.gameState.freeCells[3])
        
        // Move 4D to another free cell first
        assertTrue(gameEngine.moveFromFreeCellToFreeCell(3, 0))
        assertNull(gameEngine.gameState.freeCells[3])
        assertEquals(fourOfDiamonds, gameEngine.gameState.freeCells[0])
        
        // Now move it back to tableau pile 3
        assertTrue("Should be able to move 4D back to 5S after moving between free cells",
            gameEngine.moveFromFreeCellToTableau(0, 3))
        assertNull(gameEngine.gameState.freeCells[0])
        assertEquals(fourOfDiamonds, gameEngine.gameState.tableauPiles[3].last())
    }
}