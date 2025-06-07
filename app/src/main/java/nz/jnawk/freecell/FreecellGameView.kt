package nz.jnawk.freecell

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FreecellGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    // We'll pass the game engine to the view so it can access game state
    // This could also be done via a ViewModel later for better architecture
    private val gameEngine: FreecellGameEngine
) : View(context, attrs, defStyleAttr) {

    // Paints for drawing different elements
    private val cardBackgroundPaint = Paint().apply {
        color = Color.LTGRAY // Light gray for card background
        style = Paint.Style.FILL
    }
    private val cardBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f // Adjust as needed
        textAlign = Paint.Align.CENTER // Center text horizontally
    }
    private val redTextPaint = Paint().apply {
        color = Color.RED
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    // Basic dimensions - these will need refinement for different screen sizes
    private val cardWidth = 120f
    private val cardHeight = 180f
    private val padding = 20f
    private val tableauCardOffset = 40f // How much cards in a tableau pile overlap vertically

    init {
        // Initialize the game when the view is created if it hasn't been started
        // This is a simple approach; later, game start might be triggered by user action
        if (gameEngine.gameState.tableauPiles.all { it.isEmpty() }) {
            gameEngine.startNewGame()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // It's good practice to null-check canvas, though it's rarely null here
        canvas ?: return

        drawFreeCells(canvas)
        drawFoundationPiles(canvas)
        drawTableauPiles(canvas)
    }

    private fun drawFreeCells(canvas: Canvas) {
        for (i in 0 until 4) {
            val card = gameEngine.gameState.freeCells[i]
            val x = padding + i * (cardWidth + padding)
            val y = padding

            // Draw placeholder for the cell
            canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBackgroundPaint)
            canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBorderPaint)

            if (card != null) {
                drawCard(canvas, card, x, y)
            } else {
                // Optionally draw something to indicate it's an empty freecell slot
                // canvas.drawText("Free", x + cardWidth / 2, y + cardHeight / 2, textPaint)
            }
        }
    }

    private fun drawFoundationPiles(canvas: Canvas) {
        val suitsOrder = Suit.values() // Define an order for drawing foundations
        for (i in suitsOrder.indices) {
            val suit = suitsOrder[i]
            val pile = gameEngine.gameState.foundationPiles[suit]
            // Position foundations to the right of freecells, or on a new row
            val x = padding + (4 * (cardWidth + padding)) + i * (cardWidth + padding) // Example positioning
            val y = padding

            // Draw placeholder for the cell
            canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBackgroundPaint)
            canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBorderPaint)

            val topCard = pile?.lastOrNull()
            if (topCard != null) {
                drawCard(canvas, topCard, x, y)
            } else {
                canvas.drawText(suit.name.first().toString(), x + cardWidth / 2, y + cardHeight / 2 + textPaint.textSize / 3, textPaint)
            }

//            pile?.lastOrNull()?.let { topCard ->
//                drawCard(canvas, topCard, x, y)
//            } else {
//                // Optionally draw suit symbol or "Ace" placeholder
//                canvas.drawText(suit.name.first().toString(), x + cardWidth / 2, y + cardHeight / 2 + textPaint.textSize/3, textPaint)
//            }
        }
    }

    private fun drawTableauPiles(canvas: Canvas) {
        val tableauStartY = padding + cardHeight + padding * 2 // Start Y below freecells/foundations

        for (pileIndex in gameEngine.gameState.tableauPiles.indices) {
            val pile = gameEngine.gameState.tableauPiles[pileIndex]
            val pileX = padding + pileIndex * (cardWidth + padding)
            var currentY = tableauStartY

            if (pile.isEmpty()) {
                // Draw placeholder for empty tableau pile
                canvas.drawRect(pileX, currentY, pileX + cardWidth, currentY + cardHeight, cardBorderPaint)
                // canvas.drawText("Empty", pileX + cardWidth / 2, currentY + cardHeight / 2, textPaint)
            } else {
                for (cardIndex in pile.indices) {
                    val card = pile[cardIndex]
                    val cardTopY = currentY + (cardIndex * tableauCardOffset) // Overlap cards
                    drawCard(canvas, card, pileX, cardTopY)
                }
            }
        }
    }

    private fun drawCard(canvas: Canvas, card: Card, x: Float, y: Float) {
        // Draw card background and border
        canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBackgroundPaint)
        canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBorderPaint)

        // Determine text paint based on suit color
        val currentTextPaint = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) {
            redTextPaint
        } else {
            textPaint
        }

        // Draw rank and suit (simple representation)
        // Adjust y-offset for text to be somewhat centered.
        val textX = x + cardWidth / 2
        val textY = y + cardHeight / 2 + currentTextPaint.textSize / 3 // Small adjustment for better vertical centering
        canvas.drawText(card.toString(), textX, textY, currentTextPaint)
    }

    /**
     * Call this method when the game state changes and the view needs to be redrawn.
     */
    fun updateView() {
        invalidate() // This tells Android to redraw the view by calling onDraw()
    }

    // TODO: Override onTouchEvent(event: MotionEvent) for user interaction
    // TODO: Implement onMeasure for proper sizing with layout managers
}
