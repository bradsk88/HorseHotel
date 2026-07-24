# Horse Hotel

A network of generated stables across the overworld where players store horses and
summon them back later, so horses stop being something you lose track of.

## Language

### Stabling

**Registry**:
The set of horses one player has stored in the hotel network.
_Avoid_: inventory, collection, stable contents

**Registration**:
Storing a horse the player is currently riding into their Registry.
_Avoid_: check-in, saving, depositing

**Summoning**:
Taking a horse back out of the Registry as a real, rideable horse.
_Avoid_: spawning, recall, withdrawal

**Storage Type**:
Whether Summoning removes the horse from the Registry (`GTA`) or leaves it there
for repeat Summoning (`BOTW`).

### The stable

**Stable**:
A generated structure holding a row of Stalls, one Register Block and one Attendant.

**Stall**:
One position in a Stable where a single horse is shown — either one from the Stable's
Stock or one from the Claimant's Registry.

**Stand-In**:
The horse-shaped display entity occupying a Stall — decoration that mirrors the
appearance of a horse elsewhere, not a horse.
_Avoid_: fake horse, not-horse, placeholder horse

**Claimant**:
The player whose Registry a Stable's Stand-Ins are currently mirroring.

**Register Block**:
The block a player interacts with to open the hotel UI.

**Attendant**:
The stablehand NPC. Interacting with them does the same thing as the Register Block.
Also the keeper of the Stable's Stock.
_Avoid_: cowboy, stable boy

### Buying

**Stock**:
The horses a Stable currently has for sale. Belongs to the Stable, not to any player,
so everyone standing at a Stable sees the same Stock. Held by the Attendant, and lost if
the Attendant is killed — a Stable with no Attendant has no Stock and shows no for-sale.
_Avoid_: shop inventory, listings, wares

**Restock**:
A Stable discarding its Stock and rolling a fresh one — both how many horses and which
horses. Happens on a schedule the Attendant keeps, and is visible: the Stalls change in
front of anyone standing there.
_Avoid_: refresh, reroll, rotation

**Purchase**:
Taking a horse out of a Stable's Stock and into the world as a real, rideable horse. The
bought horse also joins the buyer's Registry, so it is theirs to Summon thereafter. Distinct
from Summoning, which takes a horse out of your own Registry.

## Relationships

- A **Stable** contains many **Stalls**, one **Register Block**, one **Attendant**
- A **Stall** holds exactly one **Stand-In**
- A **Registry** belongs to exactly one player
- A **Stock** belongs to exactly one **Stable**, and is held by its **Attendant**
- A **Stand-In** mirrors at most one horse, drawn from either its **Stable**'s **Stock**
  or its **Claimant**'s **Registry**
- A **Stable** has at most one **Claimant** at a time

## Example dialogue

> **Dev:** "Two players walk up to the same **Stable**. Whose horses do the **Stand-Ins** show?"
>
> **Domain expert:** "Whoever got there first — they're the **Claimant** until they wander
> off, then it's up for grabs again. The **Stand-Ins** are scenery, so it doesn't need to be
> fair."
>
> **Dev:** "So the second player sees someone else's horses in the **Stalls**. What happens
> when they click one?"
>
> **Domain expert:** "They don't own that horse, so they get the hotel UI — same as clicking
> the **Register Block** or the **Attendant**. The UI is always your own **Registry**, never
> anyone else's. It's only the **Stand-Ins** that are shared."

## Flagged ambiguities

- "fake horse" was used for two unrelated things: the display entity in a **Stall**
  (`NotHorseEntity`, spawned by `FakeHorseSpawnBlock`) and the per-player rows in the
  summon UI (`Registration.buildFakeHorses` returning `UIHorse`). Resolved: the world
  entity is a **Stand-In**; the UI rows are just Registry entries. Neither is a "fake horse".
- "the stable shows your horses" was ambiguous between per-player and shared display.
  Resolved: the **hotel UI** is strictly per-player, the **Stand-Ins** are shared and
  claimed first-come. Amended by **Stock**: the hotel UI is now per-player in its
  Registry half and per-Stable in its Stock half, and a Stand-In showing Stock has no
  **Claimant** at all.
- "the first N stalls" was assumed to mean the front of a row. There is no row — the
  eight Stalls are scattered across a 14×17 structure with two doors, and Stall index is
  the order someone right-clicked the spawn blocks, not a position. Resolved: **Stock**
  occupies the three roofed Stalls inside the barn and the Registry grazes the paddock,
  which is a fact about the Stable's layout rather than about index arithmetic.
