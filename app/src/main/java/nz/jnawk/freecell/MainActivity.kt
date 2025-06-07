package nz.jnawk.freecell

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
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

        // 1. Create a FrameLayout to hold both views
        val rootLayout = FrameLayout(this)
        
        // 2. Initialize your game engine
        gameEngine = FreecellGameEngine()
        // Note: gameEngine.startNewGame() is currently called in FreecellGameView's init block.
        // You could also call it here explicitly if you prefer, but ensure it's only called once.

        // 3. Initialize your custom game view, passing the context and the game engine
        gameView = FreecellGameView(this, gameEngine = gameEngine)
        
        // 4. Create drag layer
        val dragLayer = DragLayer(this)
        
        // 5. Add both views to the root layout
        rootLayout.addView(gameView)
        rootLayout.addView(dragLayer)
        
        // 6. Set up communication between views
        gameView.setDragLayer(dragLayer)
        
        // 7. Set the root layout as content view
        setContentView(rootLayout)

        // 8. Hide system UI elements for a more immersive game experience
        hideSystemUi()
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