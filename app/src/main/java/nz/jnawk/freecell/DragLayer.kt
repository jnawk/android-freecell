package nz.jnawk.freecell

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout

class DragLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    // Card being dragged
    private var draggedCard: Card? = null
    
    // Position to draw the card
    private var dragX: Float = 0f
    private var dragY: Float = 0f
    
    // Card dimensions
    private var cardWidth: Float = 0f
    private var cardHeight: Float = 0f
    
    // Card drawing paints
    private val cardBackgroundPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }
    private val cardBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    private val redTextPaint = Paint().apply {
        color = Color.RED
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    
    // Make the drag layer transparent to pass touch events through
    init {
        setWillNotDraw(false) // Ensure onDraw is called
        isClickable = false
        isFocusable = false
    }
    
    // Start dragging a card
    fun startDrag(card: Card, x: Float, y: Float, width: Float, height: Float) {
        draggedCard = card
        dragX = x
        dragY = y
        cardWidth = width
        cardHeight = height
        
        // Update text sizes based on card dimensions
        textPaint.textSize = cardWidth * 0.25f
        redTextPaint.textSize = cardWidth * 0.25f
        
        invalidate()
    }
    
    // Update drag position
    fun updateDragPosition(x: Float, y: Float) {
        dragX = x
        dragY = y
        invalidate()
    }
    
    // Stop dragging
    fun stopDrag() {
        draggedCard = null
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw the dragged card if there is one
        draggedCard?.let { card ->
            drawCard(canvas, card, dragX - cardWidth/2, dragY - cardHeight/2)
        }
    }
    
    // Draw a card at the specified position
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
        
        // Draw rank and suit
        val textX = x + cardWidth / 2
        val textY = y + cardHeight / 2 + currentTextPaint.textSize / 3
        canvas.drawText(card.toString(), textX, textY, currentTextPaint)
    }
}