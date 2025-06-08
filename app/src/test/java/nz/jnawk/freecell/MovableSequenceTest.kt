package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MovableSequenceTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testIdentifyMovableCards_SingleCard() {
        // Setup: Create a pile with a single card
        val pile = gameEngine.gameState.tableauPiles[0]
        pile.add(Card(Suit.SPADES, Rank.FIVE))  // 5♠
        
        // Test: Only the bottom card should be movable
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertEquals(1, movableIndices.size)
        assertEquals(0, movableIndices[0])
    }
    
    @Test
    fun testIdentifyMovableCards_ValidSequence() {
        // Setup: Create a pile with a valid sequence
        val pile = gameEngine.gameState.tableauPiles[0]
        pile.add(Card(Suit.CLUBS, Rank.TEN))     // 10♣
        pile.add(Card(Suit.DIAMONDS, Rank.NINE)) // 9♦
        pile.add(Card(Suit.CLUBS, Rank.EIGHT))   // 8♣
        pile.add(Card(Suit.DIAMONDS, Rank.SEVEN)) // 7♦
        pile.add(Card(Suit.CLUBS, Rank.SIX))     // 6♣
        pile.add(Card(Suit.DIAMONDS, Rank.FIVE)) // 5♦
        
        // Test: All cards should be movable as they form a valid sequence
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertEquals(6, movableIndices.size)
        assertEquals(0, movableIndices[0]) // 10♣
        assertEquals(1, movableIndices[1]) // 9♦
        assertEquals(2, movableIndices[2]) // 8♣
        assertEquals(3, movableIndices[3]) // 7♦
        assertEquals(4, movableIndices[4]) // 6♣
        assertEquals(5, movableIndices[5]) // 5♦
    }
    
    @Test
    fun testIdentifyMovableCards_PartialSequence() {
        // Setup: Create a pile with a partial valid sequence
        val pile = gameEngine.gameState.tableauPiles[0]
        pile.add(Card(Suit.CLUBS, Rank.TEN))     // 10♣ (index 0)
        pile.add(Card(Suit.DIAMONDS, Rank.QUEEN)) // Q♦ (index 1) - breaks sequence
        pile.add(Card(Suit.SPADES, Rank.TEN))    // 10♠ (index 2)
        pile.add(Card(Suit.HEARTS, Rank.THREE))  // 3♥ (index 3) - breaks sequence
        pile.add(Card(Suit.SPADES, Rank.NINE))   // 9♠ (index 4)
        pile.add(Card(Suit.HEARTS, Rank.SEVEN))  // 7♥ (index 5) - breaks sequence (skips 8)
        pile.add(Card(Suit.DIAMONDS, Rank.SIX))  // 6♦ (index 6)
        pile.add(Card(Suit.SPADES, Rank.FIVE))   // 5♠ (index 7)
        
        // Test: Only the bottom 3 cards form a valid sequence
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertEquals(2, movableIndices.size)
        
        // The 6♦, 5♠ form a valid sequence (7♥ and 6♦ don't form a valid sequence)
        // The indices should be in order from top to bottom of the movable sequence
        assertEquals(6, movableIndices[0]) // 6♦ (index 6)
        assertEquals(7, movableIndices[1]) // 5♠ (index 7)
    }
    
    @Test
    fun testIdentifyMovableCards_SameColorBreaksSequence() {
        // Setup: Create a pile with same color cards breaking the sequence
        val pile = gameEngine.gameState.tableauPiles[0]
        pile.add(Card(Suit.CLUBS, Rank.TEN))     // 10♣
        pile.add(Card(Suit.SPADES, Rank.NINE))   // 9♠ - same color as 10♣
        pile.add(Card(Suit.HEARTS, Rank.EIGHT))  // 8♥
        pile.add(Card(Suit.SPADES, Rank.SEVEN))  // 7♠
        
        // Test: Only the bottom 3 cards should be movable
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertEquals(3, movableIndices.size)
        assertEquals(1, movableIndices[0]) // 9♠
        assertEquals(2, movableIndices[1]) // 8♥
        assertEquals(3, movableIndices[2]) // 7♠
    }
    
    @Test
    fun testIdentifyMovableCards_NonDescendingRankBreaksSequence() {
        // Setup: Create a pile with non-descending ranks breaking the sequence
        val pile = gameEngine.gameState.tableauPiles[0]
        pile.add(Card(Suit.CLUBS, Rank.TEN))     // 10♣
        pile.add(Card(Suit.HEARTS, Rank.TEN))    // 10♥ - same rank
        pile.add(Card(Suit.CLUBS, Rank.NINE))    // 9♣
        pile.add(Card(Suit.HEARTS, Rank.EIGHT))  // 8♥
        
        // Test: Only the bottom 3 cards should be movable
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertEquals(3, movableIndices.size)
        assertEquals(1, movableIndices[0]) // 10♥
        assertEquals(2, movableIndices[1]) // 9♣
        assertEquals(3, movableIndices[2]) // 8♥
    }
    
    @Test
    fun testIdentifyMovableCards_ExampleFromDescription() {
        // Setup: Create the example pile from the description
        // 10c qd 10s 3s 9s 7s 6d 5s
        val pile = gameEngine.gameState.tableauPiles[0]
        pile.add(Card(Suit.CLUBS, Rank.TEN))      // 10♣ (index 0)
        pile.add(Card(Suit.DIAMONDS, Rank.QUEEN)) // Q♦ (index 1)
        pile.add(Card(Suit.SPADES, Rank.TEN))     // 10♠ (index 2)
        pile.add(Card(Suit.SPADES, Rank.THREE))   // 3♠ (index 3)
        pile.add(Card(Suit.SPADES, Rank.NINE))    // 9♠ (index 4)
        pile.add(Card(Suit.SPADES, Rank.SEVEN))   // 7♠ (index 5)
        pile.add(Card(Suit.DIAMONDS, Rank.SIX))   // 6♦ (index 6)
        pile.add(Card(Suit.SPADES, Rank.FIVE))    // 5♠ (index 7)
        
        // Test: Only the bottom 3 cards should be movable
        val movableIndices = gameEngine.getMovableCardIndices(0)
        assertEquals(3, movableIndices.size)
        assertEquals(5, movableIndices[0]) // 7♠ (index 5)
        assertEquals(6, movableIndices[1]) // 6♦ (index 6)
        assertEquals(7, movableIndices[2]) // 5♠ (index 7)
    }
}