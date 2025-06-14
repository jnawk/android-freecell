package nz.jnawk.freecell

/**
 * Represents a location of a card in the game for animation purposes.
 * Type parameter T is used to differentiate between source and destination locations.
 */
sealed class CardLocation<T: LocationType> {
    data class Tableau<T: LocationType>(val pileIndex: Int) : CardLocation<T>()
    data class FreeCell<T: LocationType>(val cellIndex: Int) : CardLocation<T>()
    data class Foundation<T: LocationType>(val suit: Suit) : CardLocation<T>()
}

// Marker interfaces for type safety
interface LocationType
interface Source: LocationType
interface Destination: LocationType
