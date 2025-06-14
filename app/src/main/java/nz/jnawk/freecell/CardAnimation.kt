package nz.jnawk.freecell

/**
 * Represents a location of a card in the game for animation purposes.
 * Type parameter T is used to differentiate between source and destination locations.
 */
sealed class CardLocation<T> {
    data class Tableau<T>(val pileIndex: Int) : CardLocation<T>()
    data class FreeCell<T>(val cellIndex: Int) : CardLocation<T>()
    data class Foundation<T>(val suit: Suit) : CardLocation<T>()
}

// Marker interfaces for type safety
interface Source
interface Destination