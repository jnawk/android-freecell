# TODO List for Android Freecell

## Game Logic
- [ ] Implement win condition check in `FreecellGameEngine.kt` (Issue #4)
  - Check if all foundation piles are complete (King on top for each suit)
- [ ] Fix bug with card movements from free cells to tableau (Issue #3)
  - Queens in free cells cannot be moved to legal King positions in tableau
  - Jacks in free cells cannot be moved to legal Queen positions in tableau

## UI Improvements
- [ ] Fix clipping of cards at the bottom of tall tableau piles (Issue #1)
  - Either make the view dynamically resize as tableau piles grow, or
  - Ensure it's initially sized to accommodate the maximum possible pile height
  
- [x] Fix issue with cards not being revealed properly when dragging (Issue #2)
  - When a card is dragged away, the card underneath is not revealed until the card is released

## Configuration
- [ ] Configure data backup rules in `data_extraction_rules.xml` (Issue #5)
  - Use `<include>` and `<exclude>` to control what is backed up
