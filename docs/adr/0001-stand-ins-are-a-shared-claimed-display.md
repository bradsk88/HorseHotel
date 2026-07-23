# Stand-ins are a shared, claimed display; the hotel UI is per-player

A Stable's Stand-Ins are world entities that every nearby player sees the same way, so
they cannot show each player their own horses. We decided the Stand-Ins mirror a single
**Claimant**'s Registry — the first player to come within range, held until they leave —
while the hotel UI (summon and register screens) remains strictly per-player. When a
player interacts with a Stand-In, what they get is decided by the horse it *appears* to
be: if they own that horse they summon it, otherwise they get the same hotel UI as the
Register Block or the Attendant, showing their own Registry.

## Considered Options

- **Fully per-player display** — each player sees their own horses in every Stall. Needs
  per-player entity visibility or client-side-only rendering driven by a synced per-player
  Registry. Correct, but a much larger change to how Stand-Ins are spawned and tracked.
- **Communal horses** — a Stable's Stalls hold horses stabled there by anyone, and anyone
  can take any of them. Simplest model, but a different game: horses stop being yours.

## Consequences

- With more than one player at a Stable, the Stand-Ins can show horses that the observer
  does not own. This is intended — they are scenery.
- Interaction must be decided by the displayed horse's UUID, not by the Stall's index into
  the Claimant's Registry, or a non-Claimant would be handed the wrong horse.
- The Claimant is deliberately not persisted. It is cosmetic, and re-claiming on the next
  refresh costs nothing.
