package nz.jnawk.freecell

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

class FreecellGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    // We'll pass the game engine to the view so it can access game state
    // This could also be done via a ViewModel later for better architecture
    private val gameEngine: FreecellGameEngine
) : View(context, attrs, defStyleAttr) {

    // Reference to the drag layer
    private var dragLayer: DragLayer? = null

    // Set the drag layer reference
    fun setDragLayer(layer: DragLayer) {
        dragLayer = layer
    }

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

    // Small text paints for card corners
    private val smallTextPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
    }
    private val smallRedTextPaint = Paint().apply {
        color = Color.RED
        textAlign = Paint.Align.LEFT
    }

    // Dynamic dimensions that will be calculated based on screen size
    private var cardWidth = 0f
    private var cardHeight = 0f
    private var padding = 0f
    private var tableauCardOffset = 0f

    // Add a top margin to move the game down and avoid system notification area
    private val topMargin = 72f

    // Highlight paint for valid destinations
    private val highlightPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 180
    }

    // Highlight paint for the current destination
    private val targetHighlightPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        alpha = 220
    }

    // Track valid destinations for the currently dragged card
    private val validFreeCellIndices = mutableListOf<Int>()
    private val validFoundationIndices = mutableListOf<Int>()
    private val validTableauIndices = mutableListOf<Int>()
    // Track the maximum sequence length that can be moved to each tableau destination
    private val validTableauSequenceLengths = mutableMapOf<Int, Int>()

    // Track the currently hovered destination
    private var hoveredDestinationType: DestinationType? = null
    private var hoveredDestinationIndex: Int = -1

    // Define destination types
    private enum class DestinationType {
        FREE_CELL, FOUNDATION, TABLEAU
    }

    // Define card sources
    private enum class CardSource {
        TABLEAU, FREE_CELL
    }

    // Track the currently dragged card
    private var draggedCard: Card? = null
    private var draggedCardSource: CardSource = CardSource.TABLEAU
    private var draggedCardOriginalPile: Int = -1
    private var draggedCardOriginalIndex: Int = -1

    // Track touch position
    private var dragX: Float = 0f
    private var dragY: Float = 0f

    // Track original position of the dragged card (for animation when returning)
    private var draggedCardOriginalX: Float = 0f
    private var draggedCardOriginalY: Float = 0f

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

        // Adjust the offset to ensure the central text of cards is covered
        // We want to show just enough of the card to see the corner indicators
        tableauCardOffset = cardHeight * 0.25f

        // Adjust text sizes based on card dimensions
        textPaint.textSize = cardWidth * 0.4f  // Larger main text
        redTextPaint.textSize = cardWidth * 0.4f
        smallTextPaint.textSize = cardWidth * 0.25f  // Larger corner text
        smallRedTextPaint.textSize = cardWidth * 0.25f
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

        // Instead of using the current pile heights, calculate the maximum possible height
        // In Freecell, a tableau pile could have up to 19 cards in the worst case:
        // - Initial deal: up to 7 cards in the first 4 piles
        // - During gameplay: potentially all 13 cards of a suit could end up in one pile
        val maxPossibleTableauCards = 19

        // Calculate height needed for the maximum possible tableau
        val tableauHeight = cardHeight + (maxPossibleTableauCards - 1) * tableauCardOffset

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

        // Draw highlights for valid destinations
        drawValidDestinationHighlights(canvas)

        // Don't draw the dragged card here - it's drawn in the drag layer
    }

    // Add a paint for sequence length indicator
    private val sequenceLengthPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
    }
    
    private fun drawValidDestinationHighlights(canvas: Canvas) {
        // Only draw highlights if a card is being dragged
        if (draggedCard == null) return

        // Highlight valid free cells
        for (index in validFreeCellIndices) {
            val x = padding + index * (cardWidth + padding)
            val y = padding + topMargin
            val paint = if (hoveredDestinationType == DestinationType.FREE_CELL && hoveredDestinationIndex == index)
                targetHighlightPaint else highlightPaint
            canvas.drawRect(x - 2, y - 2, x + cardWidth + 2, y + cardHeight + 2, paint)
        }

        // Highlight valid foundation piles
        val suitsOrder = Suit.values()
        for (index in validFoundationIndices) {
            val x = padding + (4 * (cardWidth + padding)) + index * (cardWidth + padding)
            val y = padding + topMargin
            val paint = if (hoveredDestinationType == DestinationType.FOUNDATION && hoveredDestinationIndex == index)
                targetHighlightPaint else highlightPaint
            canvas.drawRect(x - 2, y - 2, x + cardWidth + 2, y + cardHeight + 2, paint)
        }

        // Highlight valid tableau piles
        for (index in validTableauIndices) {
            val pile = gameEngine.gameState.tableauPiles[index]
            val x = padding + index * (cardWidth + padding)
            val y = padding + cardHeight + padding * 2 + topMargin
            val paint = if (hoveredDestinationType == DestinationType.TABLEAU && hoveredDestinationIndex == index)
                targetHighlightPaint else highlightPaint

            if (pile.isEmpty()) {
                // Empty tableau pile
                canvas.drawRect(x - 2, y - 2, x + cardWidth + 2, y + cardHeight + 2, paint)
                
                // Draw sequence length indicator if it's a sequence move
                val sequenceLength = validTableauSequenceLengths[index] ?: 1
                if (sequenceLength > 1) {
                    canvas.drawText(
                        sequenceLength.toString(),
                        x + cardWidth / 2,
                        y + cardHeight / 2,
                        sequenceLengthPaint
                    )
                }
            } else {
                // Non-empty tableau pile - highlight the bottom card
                val lastCardY = y + (pile.size - 1) * tableauCardOffset
                canvas.drawRect(x - 2, lastCardY - 2, x + cardWidth + 2, lastCardY + cardHeight + 2, paint)
                
                // Draw sequence length indicator if it's a sequence move
                val sequenceLength = validTableauSequenceLengths[index] ?: 1
                if (sequenceLength > 1) {
                    canvas.drawText(
                        sequenceLength.toString(),
                        x + cardWidth / 2,
                        lastCardY + cardHeight / 2,
                        sequenceLengthPaint
                    )
                }
            }
        }
    }

    private fun drawFreeCells(canvas: Canvas) {
        for (i in 0 until 4) {
            val card = gameEngine.gameState.freeCells[i]
            val x = padding + i * (cardWidth + padding)
            val y = padding + topMargin

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
            val y = padding + topMargin

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
        val tableauStartY = padding + cardHeight + padding * 2 + topMargin // Start Y below freecells/foundations

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
                    // Skip drawing the card that's being dragged
                    if (pileIndex == draggedCardOriginalPile && cardIndex == draggedCardOriginalIndex && draggedCard != null) {
                        continue
                    }

                    val card = pile[cardIndex]
                    val cardTopY = currentY + (cardIndex * tableauCardOffset) // Overlap cards

                    // Issue #2 implementation: Cards are now properly revealed when dragging
                    // Adjusted tableauCardOffset and drawing full cards for all cards in the tableau

                    // Draw full cards for all cards in the tableau
                    // This ensures that when a card is dragged away, the card underneath is fully visible
                    drawFullCard(canvas, card, pileX, cardTopY)
                }
            }
        }
    }

    // For external access if needed
    fun drawCardAt(canvas: Canvas, card: Card, x: Float, y: Float) {
        drawCard(canvas, card, x, y)
    }

    // Get card dimensions for the drag layer
    fun getCardWidth(): Float = cardWidth
    fun getCardHeight(): Float = cardHeight

    // Draw a full card with center text
    private fun drawFullCard(canvas: Canvas, card: Card, x: Float, y: Float) {
        // Draw card background and border
        canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBackgroundPaint)
        canvas.drawRect(x, y, x + cardWidth, y + cardHeight, cardBorderPaint)

        // Determine text paint based on suit color
        val currentTextPaint = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) {
            redTextPaint
        } else {
            textPaint
        }

        val smallCurrentTextPaint = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) {
            smallRedTextPaint
        } else {
            smallTextPaint
        }

        // Draw main rank and suit in center
        val textX = x + cardWidth / 2
        val textY = y + cardHeight / 2 + currentTextPaint.textSize / 3
        canvas.drawText(card.toString(), textX, textY, currentTextPaint)

        // Draw corner text
        val cornerX = x + padding / 2
        val cornerY = y + smallCurrentTextPaint.textSize
        canvas.drawText(card.toString(), cornerX, cornerY, smallCurrentTextPaint)
    }

    // This method is no longer used as we now draw full cards for all cards in the tableau
    // Keeping it here for reference in case we need it in the future
    /*
    private fun drawPartialCard(canvas: Canvas, card: Card, x: Float, y: Float) {
        // Draw card background and border
        val visibleHeight = tableauCardOffset * 1.2f
        canvas.drawRect(x, y, x + cardWidth, y + visibleHeight, cardBackgroundPaint)
        canvas.drawRect(x, y, x + cardWidth, y + visibleHeight, cardBorderPaint)

        // Determine text paint based on suit color
        val smallCurrentTextPaint = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) {
            smallRedTextPaint
        } else {
            smallTextPaint
        }

        // Draw only corner text
        val cornerX = x + padding / 2
        val cornerY = y + smallCurrentTextPaint.textSize
        canvas.drawText(card.toString(), cornerX, cornerY, smallCurrentTextPaint)
    }
    */

    // For backward compatibility with existing code
    private fun drawCard(canvas: Canvas, card: Card, x: Float, y: Float) {
        drawFullCard(canvas, card, x, y)
    }

    // Move counter update listener
    private var moveCounterUpdateListener: ((Int) -> Unit)? = null
    
    /**
     * Set a listener to be notified when the move counter changes
     */
    fun setMoveCounterUpdateListener(listener: (Int) -> Unit) {
        moveCounterUpdateListener = listener
        // Update immediately with current count
        listener(gameEngine.moveCount)
    }
    
    /**
     * Call this method when the game state changes and the view needs to be redrawn.
     */
    fun updateView() {
        // Notify the move counter listener
        moveCounterUpdateListener?.invoke(gameEngine.moveCount)
        
        invalidate() // This tells Android to redraw the view by calling onDraw()
    }
    
    /**
     * Animate a card moving from one position to another.
     * @param card The card to animate
     * @param sourceLocation The source location of the card
     * @param destLocation The destination location of the card
     * @param onComplete Callback to execute when animation completes
     */
    fun animateCardMovement(
        card: Card,
        sourceLocation: CardLocation<Source>,
        destLocation: CardLocation<Destination>,
        onComplete: () -> Unit
    ) {
        // Calculate source position
        val (fromX, fromY) = when (sourceLocation) {
            is CardLocation.Tableau -> {
                val pileIndex = sourceLocation.pileIndex
                val pile = gameEngine.gameState.tableauPiles[pileIndex]
                val x = padding + pileIndex * (cardWidth + padding)
                val y = padding + cardHeight + padding * 2 + topMargin + 
                        (pile.size - 1) * tableauCardOffset
                Pair(x + cardWidth/2, y + cardHeight/2)
            }
            is CardLocation.FreeCell -> {
                val cellIndex = sourceLocation.cellIndex
                val x = padding + cellIndex * (cardWidth + padding)
                val y = padding + topMargin
                Pair(x + cardWidth/2, y + cardHeight/2)
            }
            is CardLocation.Foundation -> {
                val suit = sourceLocation.suit
                val suitsOrder = Suit.values()
                val foundationIndex = suitsOrder.indexOf(suit)
                val x = padding + (4 * (cardWidth + padding)) + foundationIndex * (cardWidth + padding)
                val y = padding + topMargin
                Pair(x + cardWidth/2, y + cardHeight/2)
            }
        }
        
        // Calculate destination position
        val (toX, toY) = when (destLocation) {
            is CardLocation.Tableau -> {
                val pileIndex = destLocation.pileIndex
                val pile = gameEngine.gameState.tableauPiles[pileIndex]
                val x = padding + pileIndex * (cardWidth + padding)
                val y = padding + cardHeight + padding * 2 + topMargin + 
                        pile.size * tableauCardOffset
                Pair(x + cardWidth/2, y + cardHeight/2)
            }
            is CardLocation.FreeCell -> {
                val cellIndex = destLocation.cellIndex
                val x = padding + cellIndex * (cardWidth + padding)
                val y = padding + topMargin
                Pair(x + cardWidth/2, y + cardHeight/2)
            }
            is CardLocation.Foundation -> {
                val suit = destLocation.suit
                val suitsOrder = Suit.values()
                val foundationIndex = suitsOrder.indexOf(suit)
                val x = padding + (4 * (cardWidth + padding)) + foundationIndex * (cardWidth + padding)
                val y = padding + topMargin
                Pair(x + cardWidth/2, y + cardHeight/2)
            }
        }
        
        // Create a temporary card in the drag layer
        dragLayer?.startDrag(card, fromX, fromY, cardWidth, cardHeight)
        
        // Animate the card to its destination
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 150 // Animation duration in milliseconds - keep it quick
        animator.interpolator = DecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val currentX = fromX + (toX - fromX) * fraction
            val currentY = fromY + (toY - fromY) * fraction
            
            // Update the card position in the drag layer
            dragLayer?.updateDragPosition(currentX, currentY)
            
            // When animation completes
            if (fraction >= 1f) {
                // Remove the temporary card from the drag layer
                dragLayer?.stopDrag()
                // Execute the completion callback
                onComplete()
            }
        }
        
        animator.start()
    }

    // Helper class to store touched card information
    private data class TouchedCard(
        val card: Card,
        val pileIndex: Int,
        val cardIndex: Int
    )

    // Find which card was touched in the tableau
    private fun findTouchedTableauCard(touchX: Float, touchY: Float): TouchedCard? {
        val tableauStartY = padding + cardHeight + padding * 2 + topMargin

        // Check each tableau pile from right to left (to handle overlapping cards correctly)
        for (pileIndex in gameEngine.gameState.tableauPiles.indices.reversed()) {
            val pile = gameEngine.gameState.tableauPiles[pileIndex]
            if (pile.isEmpty()) continue

            val pileX = padding + pileIndex * (cardWidth + padding)

            // Only check the bottom card of each pile
            val cardIndex = pile.size - 1
            val card = pile[cardIndex]
            val cardTopY = tableauStartY + (cardIndex * tableauCardOffset)

            // Check if touch is within this card's bounds
            if (touchX >= pileX && touchX <= pileX + cardWidth &&
                touchY >= cardTopY && touchY <= cardTopY + cardHeight) {
                
                // Always allow dragging the bottom card
                return TouchedCard(card, pileIndex, cardIndex)
            }
        }

        return null
    }

    // Find which card was touched in the free cells
    private fun findTouchedFreeCell(touchX: Float, touchY: Float): Pair<Card, Int>? {
        for (i in 0 until 4) {
            val x = padding + i * (cardWidth + padding)
            val y = padding + topMargin

            val card = gameEngine.gameState.freeCells[i] ?: continue

            if (touchX >= x && touchX <= x + cardWidth &&
                touchY >= y && touchY <= y + cardHeight) {
                return Pair(card, i)
            }
        }
        return null
    }

    // Find valid destinations for the currently dragged card
    private fun findValidDestinations() {
        // Clear previous valid destinations
        validFreeCellIndices.clear()
        validFoundationIndices.clear()
        validTableauIndices.clear()
        validTableauSequenceLengths.clear()

        val card = draggedCard ?: return

        // Since we now only allow dragging the bottom card, we can simplify this logic
        if (draggedCardSource == CardSource.TABLEAU) {
            // Find valid destinations for sequences
            val validDestinations = gameEngine.findValidTableauDestinationsWithConstraints(draggedCardOriginalPile)
            
            // Add valid destinations to our list
            validTableauIndices.addAll(validDestinations.keys)
            validTableauSequenceLengths.putAll(validDestinations)
            
            // Check free cells for single card moves
            if (gameEngine.canMoveToFreeCell(gameEngine.gameState.freeCells)) {
                for (i in gameEngine.gameState.freeCells.indices) {
                    if (gameEngine.gameState.freeCells[i] == null) {
                        validFreeCellIndices.add(i)
                    }
                }
            }
            
            // Check foundation piles for single card moves
            val suitsOrder = Suit.values()
            for (i in suitsOrder.indices) {
                val suit = suitsOrder[i]
                val foundationPile = gameEngine.gameState.foundationPiles[suit]
                if (gameEngine.canMoveToFoundation(card, foundationPile)) {
                    validFoundationIndices.add(i)
                }
            }
            
            return
        }
        
        // For single cards (from free cells or non-sequence tableau cards)
        
        // Check free cells
        if (gameEngine.canMoveToFreeCell(gameEngine.gameState.freeCells)) {
            for (i in gameEngine.gameState.freeCells.indices) {
                if (gameEngine.gameState.freeCells[i] == null) {
                    validFreeCellIndices.add(i)
                }
            }
        }

        // Check foundation piles
        val suitsOrder = Suit.values()
        for (i in suitsOrder.indices) {
            val suit = suitsOrder[i]
            val foundationPile = gameEngine.gameState.foundationPiles[suit]
            if (gameEngine.canMoveToFoundation(card, foundationPile)) {
                validFoundationIndices.add(i)
            }
        }

        // Check tableau piles
        for (i in gameEngine.gameState.tableauPiles.indices) {
            // Skip the pile the card is coming from if it's a tableau pile
            if (draggedCardSource == CardSource.TABLEAU && i == draggedCardOriginalPile) continue

            val pile = gameEngine.gameState.tableauPiles[i]
            val topCard = pile.lastOrNull()
            if (gameEngine.canMoveToTableau(card, topCard)) {
                validTableauIndices.add(i)
                validTableauSequenceLengths[i] = 1 // Single card move
            }
        }
    }

    // Find the destination with the most overlap with the dragged card
    private fun findDestinationUnderCard(viewX: Float, viewY: Float): Pair<DestinationType?, Int> {
        if (draggedCard == null) return Pair(null, -1)

        val cardLeft = viewX - cardWidth / 2
        val cardTop = viewY - cardHeight / 2
        val cardRight = cardLeft + cardWidth
        val cardBottom = cardTop + cardHeight

        var bestOverlap = 0f
        var bestType: DestinationType? = null
        var bestIndex = -1

        // Check free cells
        for (index in validFreeCellIndices) {
            val cellX = padding + index * (cardWidth + padding)
            val cellY = padding + topMargin

            val overlap = calculateRectOverlap(
                cardLeft, cardTop, cardRight, cardBottom,
                cellX, cellY, cellX + cardWidth, cellY + cardHeight
            )

            if (overlap > bestOverlap) {
                bestOverlap = overlap
                bestType = DestinationType.FREE_CELL
                bestIndex = index
            }
        }

        // Check foundation piles
        val suitsOrder = Suit.values()
        for (index in validFoundationIndices) {
            val x = padding + (4 * (cardWidth + padding)) + index * (cardWidth + padding)
            val y = padding + topMargin

            val overlap = calculateRectOverlap(
                cardLeft, cardTop, cardRight, cardBottom,
                x, y, x + cardWidth, y + cardHeight
            )

            if (overlap > bestOverlap) {
                bestOverlap = overlap
                bestType = DestinationType.FOUNDATION
                bestIndex = index
            }
        }

        // Check tableau piles
        for (index in validTableauIndices) {
            val pile = gameEngine.gameState.tableauPiles[index]
            val x = padding + index * (cardWidth + padding)
            val y = padding + cardHeight + padding * 2 + topMargin

            val pileY = if (pile.isEmpty()) {
                y
            } else {
                y + (pile.size - 1) * tableauCardOffset
            }

            val overlap = calculateRectOverlap(
                cardLeft, cardTop, cardRight, cardBottom,
                x, pileY, x + cardWidth, pileY + cardHeight
            )

            if (overlap > bestOverlap) {
                bestOverlap = overlap
                bestType = DestinationType.TABLEAU
                bestIndex = index
            }
        }

        // Only return a destination if there's significant overlap (at least 25% of card area)
        val minOverlapThreshold = cardWidth * cardHeight * 0.25f
        return if (bestOverlap >= minOverlapThreshold) {
            Pair(bestType, bestIndex)
        } else {
            Pair(null, -1)
        }
    }

    // Helper method to calculate the overlap area between two rectangles
    private fun calculateRectOverlap(
        r1Left: Float, r1Top: Float, r1Right: Float, r1Bottom: Float,
        r2Left: Float, r2Top: Float, r2Right: Float, r2Bottom: Float
    ): Float {
        val overlapLeft = maxOf(r1Left, r2Left)
        val overlapTop = maxOf(r1Top, r2Top)
        val overlapRight = minOf(r1Right, r2Right)
        val overlapBottom = minOf(r1Bottom, r2Bottom)

        return if (overlapLeft < overlapRight && overlapTop < overlapBottom) {
            (overlapRight - overlapLeft) * (overlapBottom - overlapTop)
        } else {
            0f
        }
    }

    // Animate the card returning to its original position
    private fun animateCardReturn() {
        val startX = dragX
        val startY = dragY
        val endX = draggedCardOriginalX
        val endY = draggedCardOriginalY

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 200 // Animation duration in milliseconds
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            dragX = startX + (endX - startX) * fraction
            dragY = startY + (endY - startY) * fraction

            // When animation completes, clear the dragged card
            if (fraction >= 1f) {
                draggedCard = null
                draggedCardOriginalPile = -1
                draggedCardOriginalIndex = -1

                // Clear valid destinations
                validFreeCellIndices.clear()
                validFoundationIndices.clear()
                validTableauIndices.clear()
            }

            // Force redraw of the entire view
            invalidate()
        }

        animator.start()
    }

    // Track if we're currently dragging
    private var isDragging = false

    // Track the offset from touch point to card corner
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchedCard = findTouchedTableauCard(event.x, event.y)
                if (touchedCard != null) {
                    // Start dragging from tableau
                    draggedCard = touchedCard.card
                    draggedCardSource = CardSource.TABLEAU
                    draggedCardOriginalPile = touchedCard.pileIndex
                    draggedCardOriginalIndex = touchedCard.cardIndex

                    // Calculate original position for animation
                    draggedCardOriginalX = padding + touchedCard.pileIndex * (cardWidth + padding)
                    draggedCardOriginalY = (padding + cardHeight + padding * 2 + topMargin) +
                                         (touchedCard.cardIndex * tableauCardOffset)

                    // Find valid destinations for this card
                    findValidDestinations()

                    // Reset hovered destination
                    hoveredDestinationType = null
                    hoveredDestinationIndex = -1

                    // Tell the drag layer to start dragging
                    dragLayer?.let {
                        // Start dragging in the drag layer
                        it.startDrag(
                            touchedCard.card,
                            event.rawX,
                            event.rawY,
                            cardWidth,
                            cardHeight
                        )
                    }

                    invalidate()
                    return true
                } else {
                    // Check if we're touching a free cell
                    val touchedFreeCell = findTouchedFreeCell(event.x, event.y)
                    if (touchedFreeCell != null) {
                        val (card, cellIndex) = touchedFreeCell

                        // Start dragging from free cell
                        draggedCard = card
                        draggedCardSource = CardSource.FREE_CELL
                        draggedCardOriginalPile = cellIndex
                        draggedCardOriginalIndex = -1 // Not used for free cells

                        // Calculate original position for animation
                        draggedCardOriginalX = padding + cellIndex * (cardWidth + padding)
                        draggedCardOriginalY = padding + topMargin

                        // Find valid destinations for this card
                        findValidDestinations()

                        // Reset hovered destination
                        hoveredDestinationType = null
                        hoveredDestinationIndex = -1

                        // Tell the drag layer to start dragging
                        dragLayer?.let {
                            it.startDrag(
                                card,
                                event.rawX,
                                event.rawY,
                                cardWidth,
                                cardHeight
                            )
                        }

                        invalidate()
                        return true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggedCard != null) {
                    // Update drag position in drag layer
                    dragLayer?.updateDragPosition(event.rawX, event.rawY)

                    // Find destination under card
                    val loc = IntArray(2)
                    getLocationOnScreen(loc)
                    val viewX = event.rawX - loc[0]
                    val viewY = event.rawY - loc[1]

                    val (type, index) = findDestinationUnderCard(viewX, viewY)

                    // Update hovered destination if changed
                    if (type != hoveredDestinationType || index != hoveredDestinationIndex) {
                        hoveredDestinationType = type
                        hoveredDestinationIndex = index
                        invalidate()
                    }

                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggedCard != null) {
                    // Check if we're hovering over a valid destination
                    if (hoveredDestinationType != null && hoveredDestinationIndex != -1) {
                        // Try to move the card to the destination
                        val moved = when {
                            draggedCardSource == CardSource.TABLEAU && hoveredDestinationType == DestinationType.FREE_CELL ->
                                gameEngine.moveFromTableauToFreeCell(draggedCardOriginalPile, hoveredDestinationIndex)

                            draggedCardSource == CardSource.TABLEAU && hoveredDestinationType == DestinationType.FOUNDATION -> {
                                val suit = Suit.values()[hoveredDestinationIndex]
                                gameEngine.moveFromTableauToFoundation(draggedCardOriginalPile, suit)
                            }

                            draggedCardSource == CardSource.TABLEAU && hoveredDestinationType == DestinationType.TABLEAU -> {
                                // Get the movable card indices for the source pile
                                val movableIndices = gameEngine.getMovableCardIndices(draggedCardOriginalPile)
                                
                                // Get the maximum sequence length that can be moved to this destination
                                val maxSequenceLength = validTableauSequenceLengths[hoveredDestinationIndex] ?: 1
                                
                                // Limit the sequence to the maximum allowed length
                                val limitedIndices = if (movableIndices.size > maxSequenceLength) {
                                    movableIndices.takeLast(maxSequenceLength)
                                } else {
                                    movableIndices
                                }
                                
                                // Try to move the sequence
                                val moveSteps = gameEngine.moveCardSequence(draggedCardOriginalPile, hoveredDestinationIndex, limitedIndices)
                                moveSteps.isNotEmpty() // This will be true if the move was successful
                            }

                            draggedCardSource == CardSource.FREE_CELL && hoveredDestinationType == DestinationType.TABLEAU ->
                                gameEngine.moveFromFreeCellToTableau(draggedCardOriginalPile, hoveredDestinationIndex)

                            draggedCardSource == CardSource.FREE_CELL && hoveredDestinationType == DestinationType.FOUNDATION -> {
                                val suit = Suit.values()[hoveredDestinationIndex]
                                gameEngine.moveFromFreeCellToFoundation(draggedCardOriginalPile, suit)
                            }

                            draggedCardSource == CardSource.FREE_CELL && hoveredDestinationType == DestinationType.FREE_CELL ->
                                gameEngine.moveFromFreeCellToFreeCell(draggedCardOriginalPile, hoveredDestinationIndex)

                            else -> false
                        }

                        if (moved) {
                            // Card was moved successfully, clean up
                            dragLayer?.stopDrag()
                            draggedCard = null
                            draggedCardOriginalPile = -1
                            draggedCardOriginalIndex = -1
                            hoveredDestinationType = null
                            hoveredDestinationIndex = -1
                            validFreeCellIndices.clear()
                            validFoundationIndices.clear()
                            validTableauIndices.clear()
                            
                            // Update move counter display
                            moveCounterUpdateListener?.invoke(gameEngine.moveCount)
                            
                            // Check if the game has been won
                            if (gameEngine.checkWinCondition()) {
                                // Notify the activity that the game has been won
                                (context as? MainActivity)?.showWinDialog(gameEngine.moveCount)
                            }
                            
                            invalidate()
                            return true
                        }
                    }

                    // If we get here, either there was no valid destination or the move failed
                    // Animate card back to its original position
                    val loc = IntArray(2)
                    getLocationOnScreen(loc)
                    val screenOriginalX = draggedCardOriginalX + loc[0]
                    val screenOriginalY = draggedCardOriginalY + loc[1]

                    dragLayer?.animateCardReturn(screenOriginalX, screenOriginalY) {
                        // When animation completes
                        draggedCard = null
                        draggedCardOriginalPile = -1
                        draggedCardOriginalIndex = -1

                        // Clear valid destinations and hovered destination
                        validFreeCellIndices.clear()
                        validFoundationIndices.clear()
                        validTableauIndices.clear()
                        hoveredDestinationType = null
                        hoveredDestinationIndex = -1

                        invalidate()
                    }
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }
}
