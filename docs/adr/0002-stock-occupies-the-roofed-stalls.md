# Stock occupies the roofed Stalls, by fixed reservation rather than a running offset

Stalls `0 .. maxHorsesForSale - 1` are permanently the Stable's, and a Registry horse `i`
is always shown at Stall `i + maxHorsesForSale`. The reservation does not shrink when a
Stable happens to have less Stock than the maximum — a Stable holding one horse for sale
shows one horse and two empty Stalls, it does not lend the spare Stalls to the Claimant's
Registry. `stable.nbt` is re-indexed so that the three roofed Stalls inside the barn carry
indices 0, 1 and 2, which is what actually gives Stock its visual priority.

## Why this is not obvious

The original framing was "Stock takes the first N Stalls," which reads as a statement about
position. It is not: the eight Stalls are scattered across a 14×8×17 structure with two
doors and two Register Blocks, and a Stall's index is simply the order in which somebody
right-clicked the spawn blocks with a structure block in hand. Index 0 and index 3 are in
opposite corners.

What the structure *does* have is three Stalls under cover — the two inside the barn and one
under a lean-to — and five out in the open paddock. The barn Stalls are also the ones beside
the doors and the Register Blocks: the closest Stall to any door is 1.7 blocks from it and
roofed, while the farthest is 8.1 blocks away in an open corner. So prominence is a fact
about roofing and doors, not about index order, and the way to give Stock priority is to
re-index the structure so the roofed Stalls come first. Three roofed Stalls also happens to
match the default `maxHorsesForSale` of 3, so the barn fills exactly.

The resulting fiction is worth stating plainly, because it is the point: **the horses for
sale are stabled in the barn, and your own horses graze in the field.**

## Considered Options

- **Stock at the high indices, filling backward from the last Stall.** In the current
  structure this lands on the two barn Stalls with no re-indexing at all, and it lets the
  Claimant reclaim all eight Stalls when a Stable has no Stock. Rejected because it needs a
  `stallCount`, and there is no trustworthy way to obtain one: the count exists only as the
  number of spawn blocks baked into `stable.nbt`, and a Stand-In cannot discover it by
  counting its neighbours because the structure spans several chunks and may be only
  partially loaded. Hardcoding 8 would silently corrupt any future stable design with a
  different Stall count (issue #4). Reserving from index 0 needs no such number.
- **Stock at the low indices, with the Registry compacted into whatever is left.** Under
  this scheme Stall `s` shows `registry[s - N]`, so every Restock changes N and every one of
  the Claimant's horses jumps to a different Stall at once. That is the "horses visually
  swapping places" defect already fixed once, reintroduced as designed behaviour on a timer.
- **Stock at the low indices, with each Registry horse pinned to its own index and hidden
  when Stock overlaps it.** No churn and no forfeited Stalls, but a player with three horses
  standing at a Stable with three horses for sale would see none of their own while five
  Stalls sat visibly empty.

Reservation is the only arrangement that has no churn *and* no gaps. The forfeited display
Stalls are what buys both.

## Consequences

- The Claimant sees at most `stallCount - maxHorsesForSale` of their own horses — five of
  eight at the default. Their Registry is untouched and every horse remains summonable from
  the hotel UI; only the display is capped. Setting `maxHorsesForSale` to 0 restores the
  previous behaviour exactly.
- A Stand-In needs to know only its own index and `maxHorsesForSale` to decide which half it
  belongs to, so nothing new has to be baked into the entity and `REGISTERED_HORSE_INDEX`
  keeps its current meaning.
- The Stock branch must be evaluated *before* `resolveClaimant`, because Stock does not
  depend on a Claimant. This is what lets a player who has never registered a horse walk up
  to a Stable and see something — today they get eight empty Stalls.
- Re-indexing `stable.nbt` changes nothing in already-generated worlds, where the spawn
  blocks are long since converted to grass and the Stand-Ins carry their old indices. Those
  Stables will show Stock in the paddock instead of the barn. This is cosmetic and was judged
  not worth a migration.
