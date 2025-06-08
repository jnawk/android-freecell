# Freecell Game Requirements

This document outlines the requirements for the Freecell card game implementation.

## Card Movement Rules

### Tableau Piles

1. **Moving to Non-Empty Tableau Piles**:
   - Cards can only be placed on a card of the opposite color.
   - Cards must be placed in descending rank order (e.g., a 5 can be placed on a 6).

   **Examples**:
   - ♥5 can be placed on ♠6 or ♣6 (red on black)
   - ♦J can be placed on ♣Q (red on black)
   - ♠8 can be placed on ♥9 or ♦9 (black on red)
   - ❌ ♥7 cannot be placed on ♦8 (same color)
   - ❌ ♠3 cannot be placed on ♣5 (not in sequence)

2. **Moving to Empty Tableau Piles**:
   - Any card can be placed on an empty tableau pile.
   - There is no restriction that only Kings can be placed on empty tableau piles.

   **Examples**:
   - ♠K can be placed on an empty tableau pile
   - ♥7 can be placed on an empty tableau pile
   - ♣A can be placed on an empty tableau pile

### Foundation Piles

1. **Moving to Foundation Piles**:
   - Cards must be placed in ascending order by suit (A, 2, 3, ..., Q, K).
   - Only Aces can be placed on empty foundation piles.

   **Examples**:
   - ♥A can be placed on an empty ♥ foundation
   - ♠2 can be placed on a foundation with ♠A
   - ♣10 can be placed on a foundation with ♣9
   - ❌ ♦5 cannot be placed on a foundation with ♦3 (not in sequence)
   - ❌ ♥J cannot be placed on a foundation with ♠10 (different suit)

### Free Cells

1. **Moving to Free Cells**:
   - Any single card can be placed in an empty free cell.
   - Only one card can be in a free cell at a time.

   **Examples**:
   - Any card (♥A, ♠K, ♦7, etc.) can be placed in an empty free cell
   - ❌ Cannot place a card in an occupied free cell

## Supermoves

1. **Supermove Definition**:
   - A supermove is moving multiple cards at once.
   - Cards must form a valid sequence (alternating colors, descending ranks).

   **Examples**:
   - ♥5, ♠4, ♥3 is a valid sequence that can be moved together
   - ❌ ♥5, ♥4, ♥3 is not a valid sequence (same color)

2. **Supermove Constraints**:
   - The maximum number of cards that can be moved is determined by the formula:
     (# empty free cells + 1) × 2^(# empty tableau piles)
   - This constraint applies to the entire sequence being moved, not just the visible cards.
   - Partial sequences can be moved if they satisfy the formula and form a valid sequence.

   **Examples**:
   - With 2 empty free cells and 1 empty tableau: (2+1) × 2^1 = 6 cards
   - With 4 empty free cells and 0 empty tableau: (4+1) × 2^0 = 5 cards
   - With 0 empty free cells and 2 empty tableau: (0+1) × 2^2 = 4 cards

   **Partial Sequence Examples**:
   - If you have a valid sequence of 5 cards (♥10, ♠9, ♥8, ♠7, ♥6) and resources allow moving only 3 cards:
     - You can move the bottom 3 cards (♠7, ♥6) to a valid destination (e.g., ♣8)
     - You cannot move all 5 cards as a unit

   - If you have a valid sequence of 4 cards (♦Q, ♣J, ♦10, ♣9) and resources allow moving 4 cards:
     - You can move all 4 cards to a valid destination (e.g., ♥K or ♠K)
     - You can also move just the bottom 3 cards (♣J, ♦10, ♣9) to a valid destination (e.g., ♥Q)
     - You can also move just the bottom 2 cards (♦10, ♣9) to a valid destination (e.g., ♥J)
     - You can also move just the bottom card (♣9) to a valid destination (e.g., ♥10 or ♠10)

3. **Supermove UI Requirements**:
   - Only the bottom card of a stack should be draggable.
   - When dragging the bottom card of a valid sequence, valid destinations for the entire sequence should be highlighted.
   - Valid destinations for supermoves should be visually distinct from destinations for single card moves.
   - Any subsequence of a valid sequence should also be movable if it starts with the bottom card.
   - The UI should identify all valid destinations for both the full sequence and any valid subsequences.

   **Examples**:
   - If a tableau pile contains ♠10, ♥9, ♠8, ♥7 (top to bottom):
     - The ♥7 is draggable and can be moved alone to any valid destination
     - If there's a ♣8 in another pile, the ♥7 can be moved there
     - If there's a ♦8 in another pile, the ♥7 cannot be moved there (same color)

   - If a tableau pile contains ♣Q, ♦J, ♣10, ♦9 (top to bottom) and there are sufficient resources:
     - The ♦9 is draggable and represents the entire sequence
     - If there's a ♠10 in another pile, only the ♦9 can be moved there
     - If there's a ♥10 in another pile, none of the sequence can be moved there.
     - If there's a ♠K in another pile, only whole sequence (♣Q, ♦J, ♣10, ♦9) can be moved there

   - Complex example with multiple valid destinations:
     - Tableau pile 1: ♥J, ♠10, ♥9, ♠8 (top to bottom)
     - Tableau pile 2: ♣Q
     - Tableau pile 3: ♥10
     - With sufficient resources:
       - When dragging ♠8 from pile 1, both pile 2 (♣Q) and pile 3 (♥10) should be highlighted
       - Moving to pile 2 would move all cards (♥J, ♠10, ♥9, ♠8) as a sequence
       - Moving to pile 3 would move only ♥9, ♠8 as a sequence.

## Game Mechanics

1. **Move Counter**:
   - Each card movement counts as one move.
   - In a supermove, each card moved counts as a separate move.
   - Undoing a move counts as a move.


2. **Undo Functionality**:
   - Players can undo moves in reverse order.
   - Each card in a supermove should be recorded separately in the undo stack.

3. **Win Condition**:
   - The game is won when all cards are moved to the foundation piles.
   - All foundation piles must have 13 cards (A through K) of the same suit.
