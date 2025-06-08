package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for identifying valid destinations for cards and sequences.
 */
class ValidDestinationsTest {

    private lateinit var gameEngine: FreecellGameEngine

    @Before
    fun setUp() {
        gameEngine = FreecellGameEngine()
        gameEngine.gameState.reset()
    }

    @Test
    fun testSingleCardToEmptyTableau() {
        // Setup: Create a card and an empty tableau pile
        val card = Card(Suit.HEARTS, Rank.SEVEN) // 7♥
        gameEngine.gameState.tableauPiles[1].clear() // Ensure pile 1 is empty
        
        // Test: Any card should be able to move to an empty tableau pile
        assertTrue(gameEngine.canMoveToTableau(card, null))
    }
    
    @Test
    fun testSingleCardToNonEmptyTableau_ValidMove() {
        // Setup: Create a card and a destination pile with a valid target
        val card = Card(Suit.HEARTS, Rank.SEVEN) // 7♥
        val destCard = Card(Suit.SPADES, Rank.EIGHT) // 8♠
        
        // Test: Card should be able to move to a card of opposite color and one rank higher
        assertTrue(gameEngine.canMoveToTableau(card, destCard))
    }
    
    @Test
    fun testSingleCardToNonEmptyTableau_InvalidColor() {
        // Setup: Create a card and a destination pile with same color
        val card = Card(Suit.HEARTS, Rank.SEVEN) // 7♥
        val destCard = Card(Suit.DIAMONDS, Rank.EIGHT) // 8♦
        
        // Test: Card should not be able to move to a card of same color
        assertFalse(gameEngine.canMoveToTableau(card, destCard))
    }
    
    @Test
    fun testSingleCardToNonEmptyTableau_InvalidRank() {
        // Setup: Create a card and a destination pile with wrong rank
        val card = Card(Suit.HEARTS, Rank.SEVEN) // 7♥
        val destCard = Card(Suit.SPADES, Rank.NINE) // 9♠
        
        // Test: Card should not be able to move to a card that's not one rank higher
        assertFalse(gameEngine.canMoveToTableau(card, destCard))
    }
    
    @Test
    fun testFindValidDestinationsForSingleCard() {
        // Setup: Create a card in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.SEVEN)) // 7♥
        
        // Create valid destinations in other piles
        gameEngine.gameState.tableauPiles[1].add(Card(Suit.SPADES, Rank.EIGHT)) // 8♠
        gameEngine.gameState.tableauPiles[2].add(Card(Suit.CLUBS, Rank.EIGHT)) // 8♣
        
        // Create invalid destinations
        gameEngine.gameState.tableauPiles[3].add(Card(Suit.DIAMONDS, Rank.EIGHT)) // 8♦ (same color)
        gameEngine.gameState.tableauPiles[4].add(Card(Suit.SPADES, Rank.NINE)) // 9♠ (wrong rank)
        
        // Leave pile 5 empty (should be valid for any card)
        gameEngine.gameState.tableauPiles[5].clear()
        
        // Create a method to find valid tableau destinations for a card
        val validDestinations = findValidTableauDestinations(0)
        
        // Test: Should find piles 1, 2, and 5 as valid destinations
        assertEquals(3, validDestinations.size)
        assertTrue(validDestinations.contains(1)) // 8♠
        assertTrue(validDestinations.contains(2)) // 8♣
        assertTrue(validDestinations.contains(5)) // Empty pile
        
        // Test: Should not find piles 3 and 4 as valid destinations
        assertFalse(validDestinations.contains(3)) // 8♦ (same color)
        assertFalse(validDestinations.contains(4)) // 9♠ (wrong rank)
    }
    
    @Test
    fun testFindValidDestinationsForSequence() {
        // Setup: Create a sequence in a tableau pile
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.NINE)) // 9♥
        sourcePile.add(Card(Suit.SPADES, Rank.EIGHT)) // 8♠
        sourcePile.add(Card(Suit.HEARTS, Rank.SEVEN)) // 7♥
        
        // Create valid destinations for the sequence
        gameEngine.gameState.tableauPiles[1].add(Card(Suit.CLUBS, Rank.TEN)) // 10♣
        
        // Create valid destination for just the bottom card
        gameEngine.gameState.tableauPiles[2].add(Card(Suit.CLUBS, Rank.EIGHT)) // 8♣
        
        // Create invalid destinations
        gameEngine.gameState.tableauPiles[3].add(Card(Suit.DIAMONDS, Rank.TEN)) // 10♦ (same color as 9♥)
        gameEngine.gameState.tableauPiles[4].add(Card(Suit.CLUBS, Rank.NINE)) // 9♣ (wrong rank for sequence)
        
        // Leave pile 5 empty (should be valid for any card/sequence)
        gameEngine.gameState.tableauPiles[5].clear()
        
        // Create a method to find valid tableau destinations for the sequence
        val validDestinationsForSequence = findValidTableauDestinationsForSequence(0)
        val validDestinationsForBottomCard = findValidTableauDestinationsForCard(0, 2) // 7♥
        
        // Test: Should find piles 1 and 5 as valid destinations for the sequence
        assertEquals(2, validDestinationsForSequence.size)
        assertTrue(validDestinationsForSequence.contains(1)) // 10♣
        assertTrue(validDestinationsForSequence.contains(5)) // Empty pile
        
        // Test: Should find piles 1, 2, and 5 as valid destinations for the bottom card
        assertEquals(3, validDestinationsForBottomCard.size)
        assertTrue(validDestinationsForBottomCard.contains(1)) // 10♣ (can take the sequence)
        assertTrue(validDestinationsForBottomCard.contains(2)) // 8♠ (can take just 7♥)
        assertTrue(validDestinationsForBottomCard.contains(5)) // Empty pile
    }
    
    @Test
    fun testFindValidDestinationsWithResourceConstraints() {
        // This test verifies that resource constraints are properly considered when finding valid destinations
        // Scenario: A 5-card sequence with no free cells and 1 empty tableau pile
        // Expected behavior:
        // - Only destinations that can accept 2 or fewer cards should be valid (max movable = (0+1) × 2^1 = 2)
        // - Destinations requiring more than 2 cards should be excluded
        // - Both single card destinations and empty tableau piles should be valid
        val sourcePile = gameEngine.gameState.tableauPiles[0]
        sourcePile.add(Card(Suit.HEARTS, Rank.JACK)) // J♥
        sourcePile.add(Card(Suit.SPADES, Rank.TEN)) // 10♠
        sourcePile.add(Card(Suit.HEARTS, Rank.NINE)) // 9♥
        sourcePile.add(Card(Suit.SPADES, Rank.EIGHT)) // 8♠
        sourcePile.add(Card(Suit.HEARTS, Rank.SEVEN)) // 7♥
        
        // Fill all free cells to limit resources
        for (i in 0 until 4) {
            gameEngine.gameState.freeCells[i] = Card(Suit.CLUBS, Rank.ACE)
        }
        
        // Create valid destinations
        gameEngine.gameState.tableauPiles[1].add(Card(Suit.CLUBS, Rank.QUEEN)) // Q♣
        gameEngine.gameState.tableauPiles[2].add(Card(Suit.CLUBS, Rank.EIGHT)) // 8♣
        
        // Leave pile 3 empty to increase resources
        gameEngine.gameState.tableauPiles[3].clear()
        
        // With no empty free cells and 1 empty tableau:
        // Max movable cards = (0+1) × 2^1 = 2
        
        // Create a method to find valid tableau destinations considering resource constraints
        val validDestinationsForSequence = findValidTableauDestinationsWithConstraints(0)
        
        // Test: Should find pile 2 as valid destination for a 2-card subsequence (8♠, 7♥)
        // and pile 3 (empty) for any subsequence up to 2 cards
        assertEquals(2, validDestinationsForSequence.size)
        assertTrue(validDestinationsForSequence.contains(2)) // 8♣ can take 7♥
        assertTrue(validDestinationsForSequence.contains(3)) // Empty pile can take any 2 cards
        
        // Test: Should not find pile 1 as valid destination because the sequence is too long
        assertFalse(validDestinationsForSequence.contains(1)) // Q♣ would need all 5 cards
    }
    

    
    // Helper method to find valid tableau destinations for a card in a pile
    private fun findValidTableauDestinations(sourcePileIndex: Int): List<Int> {
        val validDestinations = mutableListOf<Int>()
        val sourcePile = gameEngine.gameState.tableauPiles[sourcePileIndex]
        if (sourcePile.isEmpty()) return validDestinations
        
        val card = sourcePile.last()
        
        for (i in gameEngine.gameState.tableauPiles.indices) {
            if (i == sourcePileIndex) continue // Skip source pile
            
            val destPile = gameEngine.gameState.tableauPiles[i]
            val topCard = destPile.lastOrNull()
            
            if (gameEngine.canMoveToTableau(card, topCard)) {
                validDestinations.add(i)
            }
        }
        
        return validDestinations
    }
    
    // Helper method to find valid tableau destinations for a sequence
    private fun findValidTableauDestinationsForSequence(sourcePileIndex: Int): List<Int> {
        // This is a placeholder - the actual implementation would need to check
        // if the sequence can be moved as a unit to each destination
        return emptyList()
    }
    
    // Helper method to find valid tableau destinations for a specific card in a pile
    private fun findValidTableauDestinationsForCard(sourcePileIndex: Int, cardIndex: Int): List<Int> {
        // This is a placeholder - the actual implementation would need to check
        // if the specific card can be moved to each destination
        return emptyList()
    }
    
    // Helper method to find valid tableau destinations considering resource constraints
    private fun findValidTableauDestinationsWithConstraints(sourcePileIndex: Int): List<Int> {
        // This is a placeholder - the actual implementation would need to check
        // resource constraints for moving sequences
        return emptyList()
    }
    
    // Helper class to represent a destination with the cards that would be moved there
    private data class Destination(val pileIndex: Int, val cardsToMove: List<Card>)
    
    // Helper method to find valid tableau destinations for the bottom card of a pile
    private fun findValidTableauDestinationsForBottomCard(sourcePileIndex: Int): Map<Int, Destination> {
        // This is a placeholder - the actual implementation would need to determine
        // which cards would be moved to each valid destination
        return emptyMap()
    }
}