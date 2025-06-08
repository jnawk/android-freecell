package nz.jnawk.freecell

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.androidgamesdk.GameActivity

class MainActivity : GameActivity() {

    private lateinit var gameEngine: FreecellGameEngine
    private lateinit var gameView: FreecellGameView

    // If you had a native library for GameActivity, you'd load it here.
    // We are not using a native library for this Kotlin-based approach yet.
    /*
    companion object {
        init {
            System.loadLibrary("your_native_lib_name_if_any")
        }
    }
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Create a FrameLayout to hold all views
        val rootLayout = FrameLayout(this)

        // 2. Initialize your game engine
        gameEngine = FreecellGameEngine()
        // Note: gameEngine.startNewGame() is currently called in FreecellGameView's init block.
        // You could also call it here explicitly if you prefer, but ensure it's only called once.

        // 3. Initialize your custom game view, passing the context and the game engine
        gameView = FreecellGameView(this, gameEngine = gameEngine)

        // 4. Create drag layer
        val dragLayer = DragLayer(this)

        // 5. Create New Game button
        val newGameButton = Button(this).apply {
            text = "New Game"
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            alpha = 0.8f
            
            // Set click listener
            setOnClickListener {
                showNewGameConfirmationDialog()
            }
        }
        
        // Create layout parameters for the button (positioned at bottom center)
        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 48 // Add margin to keep it above navigation buttons
        }
        
        // 6. Create Move Counter TextView
        val moveCounterTextView = android.widget.TextView(this).apply {
            text = "Moves: 0"
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            textSize = 18f // Size between card corner and center text
            setShadowLayer(3f, 1f, 1f, Color.BLACK) // Add shadow for better visibility
            
            // Update the move counter text when the game state changes
            gameView.setMoveCounterUpdateListener { count ->
                text = "Moves: $count"
            }
        }
        
        // Create layout parameters for the move counter (positioned at top right)
        val counterParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 24
            rightMargin = 24
        }

        // 7. Add all views to the root layout
        rootLayout.addView(gameView)
        rootLayout.addView(dragLayer)
        rootLayout.addView(newGameButton, buttonParams)
        rootLayout.addView(moveCounterTextView, counterParams)

        // 8. Set up communication between views
        gameView.setDragLayer(dragLayer)

        // 9. Set the root layout as content view
        setContentView(rootLayout)

        // 10. Hide system UI elements for a more immersive game experience
        hideSystemUi()
    }
    
    private fun showNewGameConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("New Game")
            .setMessage("Are you sure you want to start a new game? Your current progress will be lost.")
            .setPositiveButton("Yes") { _, _ ->
                // Start a new game
                gameEngine.startNewGame()
                gameView.updateView()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    /**
     * Show a dialog when the player wins the game
     */
    fun showWinDialog(moveCount: Int) {
        AlertDialog.Builder(this)
            .setTitle("Congratulations!")
            .setMessage("Well done, you completed the game in $moveCount moves.")
            .setPositiveButton("Deal Again") { _, _ ->
                // Start a new game
                gameEngine.startNewGame()
                gameView.updateView()
            }
            .setNegativeButton("Close", null)
            .setCancelable(false) // Prevent dismissing by tapping outside
            .show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        val decorView = window.decorView
        // For API level 30 and above, WindowInsetsController is recommended
        // For simplicity and compatibility with older GameActivity templates,
        // the deprecated systemUiVisibility flags are often used.
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}
