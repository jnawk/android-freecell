package nz.jnawk.freecell

import org.junit.Assert.*
import org.junit.Test

class CardTest {
    
    @Test
    fun testIsOppositeColor() {
        // Hearts and Diamonds are both red
        val heartsCard = Card(Suit.HEARTS, Rank.ACE)
        val diamondsCard = Card(Suit.DIAMONDS, Rank.ACE)
        assertFalse(heartsCard.isOppositeColor(diamondsCard))
        assertFalse(diamondsCard.isOppositeColor(heartsCard))
        
        // Clubs and Spades are both black
        val clubsCard = Card(Suit.CLUBS, Rank.ACE)
        val spadesCard = Card(Suit.SPADES, Rank.ACE)
        assertFalse(clubsCard.isOppositeColor(spadesCard))
        assertFalse(spadesCard.isOppositeColor(clubsCard))
        
        // Hearts and Clubs are opposite colors
        assertTrue(heartsCard.isOppositeColor(clubsCard))
        assertTrue(clubsCard.isOppositeColor(heartsCard))
        
        // Hearts and Spades are opposite colors
        assertTrue(heartsCard.isOppositeColor(spadesCard))
        assertTrue(spadesCard.isOppositeColor(heartsCard))
        
        // Diamonds and Clubs are opposite colors
        assertTrue(diamondsCard.isOppositeColor(clubsCard))
        assertTrue(clubsCard.isOppositeColor(diamondsCard))
        
        // Diamonds and Spades are opposite colors
        assertTrue(diamondsCard.isOppositeColor(spadesCard))
        assertTrue(spadesCard.isOppositeColor(diamondsCard))
    }
}