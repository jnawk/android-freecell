package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for moving partial sequences in supermoves.
 */
class PartialSequenceTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testMovePartialSequence_ValidMoveWithinResourceLimits() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.NINE)) // 9♥
        sourcePile.add(Card(Suit.SPADES, Rank.EIGHT)) // 8♠
        sourcePile.add(Card(Suit.HEARTS, Rank.SEVEN)) // 7♥
        sourcePile.add(Card(Suit.SPADES, Rank.SIX)) // 6♠
        sourcePile.add(Card(Suit.HEARTS, Rank.FIVE)) // 5♥

        // Create a destination pile with a valid target
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.CLUBS, Rank.SIX)) // 6♣

        // Limit resources to only allow moving 2 cards
        // Fill 3 free cells
        gameEngine.gameState.freeCells[0] = Card(Suit.CLUBS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.CLUBS, Rank.TWO)
        gameEngine.gameState.freeCells[2] = Card(Suit.CLUBS, Rank.THREE)

        // Max movable cards = (1+1) × 2^0 = 2

        // Test: Should be able to move the bottom card (5♥) to 6♣
        // This is a valid move: 5♥ can go on 6♣ (opposite color, one rank lower)
        val bottomCardIndex = sourcePile.size - 1 // 5♥
        val singleCardIndices = listOf(bottomCardIndex)

        // Test the move with just the bottom card
        val result = gameEngine.moveCardSequence(0, 1, singleCardIndices)

        // Verify the move succeeded
        assertTrue(result)

        // Verify the cards were moved correctly
        assertEquals(4, sourcePile.size) // 9♥, 8♠, 7♥, 6♠ remain
        assertEquals(2, destPile.size) // 6♣, 5♥

        // Verify the cards are in the right order
        assertEquals(Rank.SIX, destPile[0].rank) // 6♣
        assertEquals(Rank.FIVE, destPile[1].rank) // 5♥
    }

    @Test
    fun testMovePartialSequence_InvalidMove() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.NINE)) // 9♥
        sourcePile.add(Card(Suit.SPADES, Rank.EIGHT)) // 8♠
        sourcePile.add(Card(Suit.HEARTS, Rank.SEVEN)) // 7♥
        sourcePile.add(Card(Suit.SPADES, Rank.SIX)) // 6♠
        sourcePile.add(Card(Suit.HEARTS, Rank.FIVE)) // 5♥

        // Create a destination pile with an invalid target
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.CLUBS, Rank.SEVEN)) // 7♣

        // Get the bottom card index
        val bottomCardIndex = sourcePile.size - 1 // 5♥
        val singleCardIndices = listOf(bottomCardIndex)

        // Test the move with just the bottom card
        val result = gameEngine.moveCardSequence(0, 1, singleCardIndices)

        // Verify the move failed because 5♥ can't go on 7♣ (wrong rank)
        assertFalse(result)

        // Verify no cards were moved
        assertEquals(5, sourcePile.size)
        assertEquals(1, destPile.size)
    }

    @Test
    fun testMovePartialSequence_ExceedsResourceLimits() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.NINE)) // 9♣
        sourcePile.add(Card(Suit.HEARTS, Rank.EIGHT)) // 8♥
        sourcePile.add(Card(Suit.CLUBS, Rank.SEVEN)) // 7♣
        sourcePile.add(Card(Suit.HEARTS, Rank.SIX)) // 6♥
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE)) // 5♣

        // Create a destination pile
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.SPADES, Rank.TEN)) // 10♠

        // Limit resources to only allow moving 2 cards
        // Fill 3 free cells
        gameEngine.gameState.freeCells[0] = Card(Suit.CLUBS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.CLUBS, Rank.TWO)
        gameEngine.gameState.freeCells[2] = Card(Suit.CLUBS, Rank.THREE)

        // Max movable cards = (1+1) × 2^0 = 2

        // Test: Should not be able to move all 5 cards to 10♠
        val movableIndices = gameEngine.getMovableCardIndices(0)

        // Verify we have all 5 cards in the sequence
        assertEquals(5, movableIndices.size)

        // Test the move with all 5 cards
        val result = gameEngine.moveCardSequence(0, 1, movableIndices)

        // Verify the move failed due to resource constraints
        assertFalse(result)

        // Verify no cards were moved
        assertEquals(5, sourcePile.size)
        assertEquals(1, destPile.size)
    }

    @Test
    fun testMovePartialSequence_WithinResourceLimits() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.CLUBS, Rank.NINE)) // 9♣
        sourcePile.add(Card(Suit.HEARTS, Rank.EIGHT)) // 8♥
        sourcePile.add(Card(Suit.CLUBS, Rank.SEVEN)) // 7♣
        sourcePile.add(Card(Suit.HEARTS, Rank.SIX)) // 6♥
        sourcePile.add(Card(Suit.CLUBS, Rank.FIVE)) // 5♣

        // Create a destination pile with a valid target for the sequence
        val destPile = gameEngine.gameState.tableauPiles[1]
        destPile.add(Card(Suit.SPADES, Rank.SEVEN)) // 7♠

        // Limit resources to only allow moving 2 cards
        // Fill 3 free cells
        gameEngine.gameState.freeCells[0] = Card(Suit.CLUBS, Rank.ACE)
        gameEngine.gameState.freeCells[1] = Card(Suit.CLUBS, Rank.TWO)
        gameEngine.gameState.freeCells[2] = Card(Suit.CLUBS, Rank.THREE)

        // Max movable cards = (1+1) × 2^0 = 2

        // Try with just 2 cards (within resource limits)
        val movableIndices = gameEngine.getMovableCardIndices(0)
        val bottomTwoIndices = movableIndices.takeLast(2)

        // Verify we're testing with the right cards
        assertEquals(2, bottomTwoIndices.size)
        assertEquals(3, bottomTwoIndices[0]) // 6♥
        assertEquals(4, bottomTwoIndices[1]) // 5♣

        val result = gameEngine.moveCardSequence(0, 1, bottomTwoIndices)

        // This should succeed
        assertTrue(result)

        // Verify the cards were moved correctly
        assertEquals(3, sourcePile.size) // 9♣, 8♥, 7♣ remain
        assertEquals(3, destPile.size) // 7♠, 6♥, 5♣

        // Verify the cards are in the right order
        assertEquals(Suit.SPADES, destPile[0].suit) // 7♠
        assertEquals(Rank.SEVEN, destPile[0].rank)
        assertEquals(Suit.HEARTS, destPile[1].suit) // 6♥
        assertEquals(Rank.SIX, destPile[1].rank)
        assertEquals(Suit.CLUBS, destPile[2].suit) // 5♣
        assertEquals(Rank.FIVE, destPile[2].rank)
    }

    @Test
    fun testComplexExample_MultipleValidDestinations() {
        // Setup: Create the complex example from the requirements
        // Tableau pile 1: ♥J, ♠10, ♥9, ♠8 (top to bottom)
        var sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.JACK)) // J♥
        sourcePile.add(Card(Suit.SPADES, Rank.TEN)) // 10♠
        sourcePile.add(Card(Suit.HEARTS, Rank.NINE)) // 9♥
        sourcePile.add(Card(Suit.SPADES, Rank.EIGHT)) // 8♠

        // Tableau pile 2: ♣Q
        val destPile1 = gameEngine.gameState.tableauPiles[1]
        destPile1.add(Card(Suit.CLUBS, Rank.QUEEN)) // Q♣

        // Tableau pile 3: ♦9
        val destPile2 = gameEngine.gameState.tableauPiles[2]
        destPile2.add(Card(Suit.DIAMONDS, Rank.NINE)) // 9♦

        // Ensure we have enough resources
        // Empty all free cells
        for (i in 0 until 4) {
            gameEngine.gameState.freeCells[i] = null
        }

        // Get the movable indices
        val movableIndices = gameEngine.getMovableCardIndices(0)

        // Verify we have the right sequence
        assertEquals(4, movableIndices.size)
        assertEquals(0, movableIndices[0]) // J♥
        assertEquals(1, movableIndices[1]) // 10♠
        assertEquals(2, movableIndices[2]) // 9♥
        assertEquals(3, movableIndices[3]) // 8♠

        // Test: Moving to pile 2 (Q♣) should work for J♥
        val result1 = gameEngine.moveCardSequence(0, 1, listOf(movableIndices[0]))

        // Reset for the next test
        gameEngine.gameState.reset()
        setUp()
        sourcePile = gameEngine.gameState.tableauPiles[0]
        assertTrue(sourcePile == gameEngine.gameState.tableauPiles[0])
        assertTrue(sourcePile.isEmpty())

        // Setup the same scenario again
        sourcePile.add(Card(Suit.HEARTS, Rank.JACK)) // J♥
        sourcePile.add(Card(Suit.SPADES, Rank.TEN)) // 10♠
        sourcePile.add(Card(Suit.HEARTS, Rank.NINE)) // 9♥
        sourcePile.add(Card(Suit.SPADES, Rank.EIGHT)) // 8♠
        assertFalse(sourcePile.isEmpty())


        destPile1.add(Card(Suit.CLUBS, Rank.QUEEN)) // Q♣
        destPile2.add(Card(Suit.DIAMONDS, Rank.NINE)) // 9♦

        // Test: Moving to pile 3 (9♦) should work for just the bottom card (8♠)
        val bottomCardIndex = sourcePile.size - 1  // This is the index of the bottom card (8♠)
        val result2 = gameEngine.moveCardSequence(0, 2, listOf(bottomCardIndex))

        // Verify the move succeeded
        assertTrue(result2)

        // Verify both moves should succeed
        assertTrue(result1)
        assertTrue(result2)
    }
}
