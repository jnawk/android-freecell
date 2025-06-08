# TODO List for Android Freecell

## Game Logic
- [x] Implement win condition check in `FreecellGameEngine.kt` (Issue #4)
  - Check if all foundation piles are complete (King on top for each suit)
- [x] Fix bug with card movements from free cells to tableau (Issue #3)
  - Queens in free cells cannot be moved to legal King positions in tableau
  - Jacks in free cells cannot be moved to legal Queen positions in tableau
- [ ] Implement supermoves (Issue #6)
  - Detect when multiple cards form a valid sequence (alternating colors, descending ranks)
  - Allow dragging the entire sequence as one unit
  - Validate that the destination can accept the sequence (enough free cells available)
  - Implement the "Freecell power move" formula: (# empty freecells + 1) × 2^(# empty columns)
- [x] Implement move counter (Issue #7)
  - Increment counter for each valid move
  - Display the counter in the UI
  - Reset counter when starting a new game
- [ ] Implement undo stack (Issue #8)
  - Store each move in an undo stack with unlimited history
  - Implementation plan:
    - Add a Move data class to store move type, source, destination, and card
    - Add an undo stack to FreecellGameEngine (Stack<Move>)
    - Record each move in the stack when it's made
    - Implement an undo method that pops the last move and reverses it
    - Add a UI button to trigger the undo action
  - Memory usage is minimal (approximately 32-36 bytes per move)
  - Even 500 moves would only use about 16-18 KB of memory
- [ ] Detect when a game is "stuck" (Issue #10)
  - Analyze the current game state to determine if any valid moves remain
  - Notify the player when they are stuck
  - Offer options (restart, new game, undo)

## UI Improvements
- [x] Fix clipping of cards at the bottom of tall tableau piles (Issue #1)
  - Implemented solution: Initially size the view to accommodate the maximum possible pile height (19 cards)
- [x] Fix issue with cards not being revealed properly when dragging (Issue #2)
  - When a card is dragged away, the card underneath is not revealed until the card is released
- [x] Add "New Game" button (Issue #9)
  - Add a "New Game" button to the UI
  - Implement functionality to reset the game state and deal a new hand
  - Confirm with the player before abandoning the current game
- [ ] Preserve game state during screen rotation (Issue #11)
  - Save game state before rotation
  - Restore game state after rotation
  - Prevent new game from being dealt on rotation
- [ ] Improve win dialog button labels (Issue #12)
  - Change "Deal Again" and "Close" buttons to "Yes" and "No" 
  - Make them directly answer the question "Deal again?"
- [ ] Skip confirmation for "New Game" when game is already won (Issue #13)
  - When the game is won and the user opted not to deal again
  - Clicking "New Game" should start a new game immediately without confirmation
- [ ] Add victory animation (Issue #14)
  - When the game is won, animate all cards flying away in random directions
  - Add downward bias since cards are at the top of the screen
  - Animation should last no more than 2 seconds

## Configuration
- [ ] Configure data backup rules in `data_extraction_rules.xml` (Issue #5)
  - Use `<include>` and `<exclude>` to control what is backed up