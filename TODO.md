# TODO List for Android Freecell

## Game Logic
- [ ] Implement win condition check in `FreecellGameEngine.kt`
  - Check if all foundation piles are complete (King on top for each suit)

## UI Improvements
- [ ] Fix clipping of cards at the bottom of tall tableau piles (Issue #4)
  - Either make the view dynamically resize as tableau piles grow, or
  - Ensure it's initially sized to accommodate the maximum possible pile height
  
- [ ] Fix issue with cards not being revealed properly when dragging (Issue #5)
  - When a card is dragged away, the card underneath is not revealed until the card is released

## Configuration
- [ ] Configure data backup rules in `data_extraction_rules.xml`
  - Use `<include>` and `<exclude>` to control what is backed up
