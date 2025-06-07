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
        textSize = 30f // Will be adjusted based on card size
        textAlign = Paint.Align.CENTER // Center text horizontally
    }
    private val redTextPaint = Paint().apply {
        color = Color.RED
        textSize = 30f // Will be adjusted based on card size
        textAlign = Paint.Align.CENTER
    }

    // Dynamic dimensions that will be calculated based on screen size
    private var cardWidth = 0f
    private var cardHeight = 0f
    private var padding = 0f
    private var tableauCardOffset = 0f

    // Add initialization method to calculate dimensions
    private fun initDimensions() {
        // Get screen dimensions
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Calculate card dimensions based on screen width
        padding = screenWidth * 0.015f // 1.5% of screen width for padding

        // Calculate card width to fit 8 cards with padding between them
        cardWidth = (screenWidth - (9 * padding)) / 8

        // Standard card ratio is 2.5:3.5 (width:height)
        cardHeight = cardWidth * 1.4f

        // Adjust tableau card offset based on card height
        tableauCardOffset = cardHeight * 0.22f

        // Adjust text sizes based on card dimensions
        textPaint.textSize = cardWidth * 0.25f
        redTextPaint.textSize = cardWidth * 0.25f
    }

    init {
        // Initialize dimensions
        initDimensions()

        // Initialize the game when the view is created if it hasn't been started
        // This is a simple approach; later, game start might be triggered by user action
        if (gameEngine.gameState.tableauPiles.all { it.isEmpty() }) {
            gameEngine.startNewGame()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initDimensions()
    }

    // Add proper onMeasure implementation
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Initialize dimensions if not already done
        if (cardWidth == 0f) {
            initDimensions()
        }

        // Calculate required height based on tableau piles
        val maxTableauCards = gameEngine.gameState.tableauPiles.maxOfOrNull { it.size } ?: 0

        // Calculate height needed for the tallest tableau
        val tableauHeight = if (maxTableauCards > 0) {
            cardHeight + (maxTableauCards - 1) * tableauCardOffset
        } else {
            cardHeight
        }

        // Total height = freecell row + padding + tableau height + padding
        val totalHeight = (2 * cardHeight) + (3 * padding) + tableauHeight

        // Set the measured dimensions
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            totalHeight.toInt().coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
        )
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
                val suitPaint = if (suit == Suit.HEARTS || suit == Suit.DIAMONDS) redTextPaint else textPaint
                canvas.drawText(suit.getSymbol(), x + cardWidth / 2, y + cardHeight / 2 + suitPaint.textSize / 3, suitPaint)
            }

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
