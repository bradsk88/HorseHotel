# Stock and purchasing — design

Design decisions from the session on buying horses at a Stable. Language is defined in
[CONTEXT.md](../../CONTEXT.md); the Stall allocation is
[ADR 0002](../adr/0002-stock-occupies-the-roofed-stalls.md). Everything below is decided
unless it sits under [Deferred](#deferred) or [Still-open](#still-open-implementation-details).

This slice makes horses **free**. A currency system (gold? iron? wood?) comes later, and the
Restock interval is the throttle that will make it matter.

## Stock: ownership, shape, lifecycle

**The Attendant owns the Stock.** Exactly one per Stable, persistent, already the domain's
shopkeeper. Stand-Ins and the Register Block reach it with a bounded nearest-entity lookup,
which Stand-Ins can afford because they already do a nearest-*player* scan on the same refresh
tick. Costs: the lookup radius must cover the Attendant's stroll leash, and killing the
Attendant destroys the Stable's Stock.

**Stock is an ordered, fixed-length slot list**, not a UUID-keyed map — length = the N rolled
at the last Restock, each slot an `(UUID, CompoundTag)` or empty. Order matters because barn
Stall `i` shows Stock slot `i`; the UUID identifies a horse for purchase. Serialized in the
Attendant's persistent data as a `ListTag` (empty slots included) plus the Restock deadline.

**Stock horses are plain `finalizeSpawn` horses.** Build a real `Horse` server-side, call
`finalizeSpawn` so variant and stats land in vanilla-plausible ranges for free, serialize the
NBT. No saddle, armour, or name. This drops straight into the same NBT→render→buy path the
summon and register screens already use, and can never produce a stat combo a wild horse
couldn't. No notion of "quality"/rarity this slice — that is where currency-era price scaling
will hook in.

**Restock is a full reroll on an absolute game-tick deadline.** The Attendant stores the
`level.getGameTime()` at which it next Restocks. At the deadline it discards the Stock and
rolls both a fresh N in `0..maxHorsesForSale` and fresh horses. Purchases remove from the
current Stock and do not touch the deadline. Full reroll rather than top-up because under
top-up a Stable that rolls 0 at worldgen is permanently a dud. An absolute deadline rather
than a countdown is what makes this survive chunk unloading: the Attendant stops ticking while
its chunk is unloaded but game time keeps advancing, so the first tick after a player returns
sees `now > deadline` and Restocks immediately; a countdown field would freeze.

**Initial Stock is rolled on the Attendant's first tick**, not at worldgen. When the Attendant
ticks and finds no deadline set, it Restocks immediately and sets `deadline = now + interval`.
A Stable is stocked the first time a player is close enough for its chunk to load and the
Attendant to tick — no empty warm-up window — and an unvisited Stable in unloaded chunks holds
no Stock and costs nothing. Stock is rolled by the Attendant, not placed by structure gen.

**Restocking in front of a player is fine.** The Stalls visibly change. A future countdown bar
in the UI will telegraph it. Closing an open hotel UI on Restock is acceptable — and since
these screens are plain `Screen`s rather than container menus, the server has no idea who has
one open, so the implementation is to broadcast a stock-changed packet within the eval radius
and let the client close a hotel screen if it has one up.

**A purchase leaves a gap; surviving Stock stays put.** Buying nulls that horse's slot rather
than removing-and-compacting, so the other for-sale horses never move — a partly-sold barn
shows gaps until the next Restock refills all slots wholesale. Same no-churn principle as
ADR 0002: compacting would slide survivors into new Stalls when a *different* horse sells,
reintroducing the "horses visually swapping places" bug. The Stand-In over a nulled slot goes
invisible. A purchase does **not** broadcast a stock-changed packet (only a Restock does), so
other players' open For Sale screens go stale rather than slamming shut on every buy.

**Stock dies with the Attendant; the For Sale tab hides when there is no Attendant.** Nothing
respawns the Attendant, so a kill loses the Stock permanently — accepted limitation ("kill the
cowboy, lose the shop"). Graceful degradation: the Register Block finds Stock by
nearby-Attendant lookup, and with no Attendant the For Sale tab is hidden entirely (UI falls
back to Your Horses only). The Register Block still serves Registry and Summon exactly as
today, so the Stable is not bricked — it just loses its shop.

## Stall allocation

**Stalls `0 .. maxHorsesForSale - 1` are permanently the Stable's**, and Registry horse `i`
shows at Stall `i + maxHorsesForSale`. `stable.nbt` is re-indexed so the three roofed barn
Stalls carry indices 0, 1, 2. See ADR 0002 — the short version: Stall index has no spatial
meaning today, three Stalls are under the barn roof beside the doors and Register Blocks, and
reservation is the only arrangement with neither churn nor gaps.

**The Stock branch runs before `resolveClaimant`.** Stock has no Claimant, so a player who has
never registered a horse sees the barn stocked instead of eight empty Stalls — the mod's new
first impression, worth protecting.

**Visual priority comes from the renderer, not the Stall index.** Re-indexing puts Stock in
the barn, but position alone cannot carry the signal — a player approaching from the wrong
side sees the paddock first, and the index→position map differs for every new stable design
(issue #4). Originally `setGlowingTag(true)`; **switched to a "For Sale" nameplate after live
testing** — the glow's draw-through-walls quality read as an unwanted x-ray mode in the real
client. The nameplate uses `NotHorseEntityRenderer`'s existing `shouldShowName` override
(`hasCustomName() && !isInvisible()`), floats over the horse, does not pierce walls, and caps
at Forge's 64-block nameplate range. A fullbright `RenderType.eyes` overlay layer cloned from
`NotHorseMarkingLayer` remains the strongest option if more punch is wanted — deferred.

Two implementation traps, both from `assumeFromTag`:

- `deserializeNBT` → `Entity.load` calls `setCustomNameVisible(...)` and `setGlowingTag(...)`
  *unconditionally* from the tag, so both go false on every refresh. Re-apply them after the
  deserialize, alongside the existing `REAL_HORSE_UUID` and `ARMOR_ITEM` writes.
- Sync "this Stand-In is Stock" with a new `EntityDataAccessor<Boolean>`. Do not pack a bit
  into `DATA_ID_TYPE_VARIANT` — `readAdditionalSaveData` overwrites that int wholesale from
  `"Variant"` on every refresh. A dedicated accessor is three lines and is the only piece of
  state immune to the clobber. It is also what routes a Stand-In click to the purchase-confirm
  screen: a Stock horse's `REAL_HORSE_UUID` is a fresh random each refresh and in nobody's
  Registry, so today's `trySpawnRealHorse` → `isAssociated` check would wrongly send it to the
  hotel UI.

## Buying

**Clicking a Stock Stand-In opens a purchase-confirm screen** — clone `HorseRegisterScreen`:
the horse rendered large, "Buy" and "Close", a `PurchaseHorseMessage`. Same idiom as "you
clicked a specific thing in the world, confirm it", and it parallels how clicking your own
Stand-In summons that specific horse. The For Sale tab is the browse view (a `PagedCardScreen`
of Stock cloned from `HorseSummonScreen`), and a card click routes to the same confirm screen
rather than buying outright — purchase gets consequential the moment currency lands.

**Buying spawns the real horse, mounts the player, and auto-registers it.** Originally the
grill session decided purchase would *not* register (to keep "buy" and "register" distinct);
**reversed after live testing** — the buyer expected a bought horse to appear in "Your Horses",
and auto-registering fits the mod's core promise (horses stop being something you lose track
of) better than the purist split. The horse is registered via `storeHorseOnPlayer` *without*
removing the world copy, so it sits in the Registry as a safety-net snapshot while the buyer
rides the world horse — exactly how a BOTW-summoned horse coexists with its Registry entry. The
purchased horse is also **tamed, owned by the buyer, and saddled** (a raw `finalizeSpawn` horse
is wild, bucked the buyer off, and had no saddle to steer with).

**Buying while already mounted is a mount-swap.** If the player is riding a registerable
`Horse` when they confirm, that horse is stored into their **Registry** (not the Stable's
Stock) via the existing `storeHorseOnPlayer` + remove path, and the purchase is spawned and
mounted — ride up on the old horse, ride off on the new one, GTA-garage style. Registers the
old horse *without* the `HorseRegisterScreen` confirm; the swap is the consent. If the current
mount is not a registerable `Horse` (pig, boat, modded rideable), there is nothing to stable,
so it falls back to spawning the purchase adjacent without mounting. `sp.startRiding`
(`NotHorseEntity.java:214`) has no already-riding guard today — this is new logic.

**`PurchaseHorseMessage` carries the Stock horse's UUID**, not an index (a Restock can replace
the list between the screen opening and the click). The server re-finds the Attendant, checks
the player is within the eval radius, and looks the UUID up. The UUID lookup is also the
concurrency backstop: shared Stock means two players can click Buy on the same horse, and
whoever arrives second finds the UUID gone — a double-purchase is impossible.

**A failed purchase shows the buyer an action-bar message**, not a silent no-op. When the UUID
is gone the server sends `horsehotel.menus.purchase.already_sold` (new lang key) via
`displayClientMessage(component, true)`. The buy click closes the screen immediately — as
`HorseSummonScreen.mouseClicked` does with `ClientAccess.closeScreens()` — so the confirm
screen is already gone and the action-bar line is unobstructed. (Summon, by contrast, still
fails silently via `respawnRealHorse`. A richer "sold out from under you" flow is deferred to
the currency era.)

## Tabs and UI wiring

**Tabs only on the two hotel-UI screens** — `HorseSummonScreen` ("Your Horses") and the new
Stock screen ("Horses for Sale"). `WelcomeScreen` and the confirm screens stay untabbed. Steal
`Questown/src/main/java/ca/bradj/questown/gui/Tabs.java` and `Tab.java` wholesale — ~90 lines,
depend only on `JEI.getTabSelected()`/`getTabUnselected()`, both already exposed by this
repo's `compat/JEI.java`.

**Round trip per tab click**, mirroring Questown's `OpenVillagerMenuMessage(pos, type)` +
`makeOpenFn`/`getEnabledTabs()`. Each tab is its own screen; clicking a tab sends an
open-message to the server, which gathers only that tab's data and sends back the matching
show-screen message. Chosen for composability/reusability. Rider: the set of *which tabs
exist* travels with every show-screen message as a cheap flag (Registry screen is told "For
Sale is available" as a boolean, not handed the Stock). That flag powers both the For-Sale-tab
hide (no Attendant) and the default-tab choice. `UIHorse` (index-based) feeds the Registry
tab; the Stock tab is fed by UUID-keyed `(UUID, CompoundTag)` entries.

**Default tab is "Your Horses", with fallbacks.** The server evaluates emptiness once at
interact time: Registry non-empty → **Your Horses**; Registry empty but Stock non-empty →
**For Sale**; both empty → the **WelcomeScreen**. `HorseSummonScreen.init()` currently
redirects to `WelcomeScreen` whenever the Registry is empty; that check must become "Registry
empty *and* Stock empty".

**Tab icons.** Your Horses shows the player's own face via vanilla
`net.minecraft.client.gui.components.PlayerFaceRenderer.draw(...)` with
`Minecraft.getInstance().player.getSkinTextureLocation()` — one call, includes the hat/overlay
layer, no new art. For Sale shows the Attendant's head-with-cowboy-hat as a static 16×16 PNG in
`textures/menu/`, not a live render (the Attendant is a vanilla villager on `villager.png`, and
the cowboy hat is a *separate* `CowboyHatModel` layer on `textures/model/hat.png` —
compositing two models into a 16px tab is the fiddly part avoided). Authoring method: capture
the real Attendant+hat render in-game and crop to 16×16, so the sprite is pixel-faithful;
fall back to hand-authored pixel art from the villager face + `hat.png` only if capture is
impractical. Tradeoff: the sprite will not follow a future change to the Attendant's look —
acceptable for a single appearance.

**`Registration.interactWithRegistry(ServerPlayer)` needs the interaction position threaded
through it** so the screen knows which Stable's Stock to show. All three entry points
(`RegisterBlock.use`, `StableAttendant.interactAt`, `NotHorseEntity.trySpawnRealHorse`) have a
position to hand.

## Config

`maxHorsesForSale` default 3, range 0–8 (0 disables Stock and restores today's behaviour). The
config cannot validate against a Stable's real Stall count — that number does not exist in
code by design (ADR 0002) — so setting it above the Stall count would hide all of a player's
own horses; the 0–8 cap keeps it merely sane, not safe. Restock interval default 24000 ticks
(one Minecraft day), in ticks to match `horseRefreshInterval`/`horseEvalRadius` in
`Config.java`.

## Verification

The headless autotest asserts **server state only** — glow, tab icons, and nameplates are out
of reach (same client-render gap `TODO.local.md` records for Stand-Ins). Cover these five,
following the `StandInScenarios` idiom with a `FakePlayer`:

1. **Stock generation** — spawn a stable+Attendant, tick past its first tick, assert N∈[0,max]
   slots and a deadline set.
2. **Restock** — advance game time past the deadline, assert the slot list rerolled and the
   deadline advanced.
3. **Purchase** — a `FakePlayer` buys a slot by UUID: assert a real `Horse` spawned, that slot
   nulled, and the buyer's **Registry unchanged** (the important negative — no auto-register).
4. **Mount-swap** — a `FakePlayer` already riding a registerable horse buys: assert the old
   horse is now in their Registry and removed from the world, and a new horse spawned. (Most
   likely branch to break.)
5. **Purchase failure** — buy a UUID not in Stock: assert no horse spawned and Stock unchanged.
   The action-bar message is client-side and not assertable; the server reject path is.

## Deferred

- A fullbright `RenderType.eyes` "for sale" overlay layer (strongest visual signal).
- Any notion of Stock quality/rarity, and price scaling — arrives with currency.
- A "sold out from under you" flow richer than the action-bar message.
- Respawning a killed Attendant — needs the resurrection machinery TODO'd at
  `EntityForgeEvents.java:66`.
- A UI countdown bar telegraphing the next Restock.

## Implementation status (2026-07-24)

Built and compiling on branch `feature/horse-stock-purchasing`; the nine-scenario autotest
passes (four original stand-in + five new Stock: generation, restock, purchase-without-register,
mount-swap, purchase-failure).

- **`stable.nbt` re-indexed** (direct NBT rewrite). Roofed Stalls now 0, 1, 2 — index 0 =
  `[8,0,6]` (barn interior, also the Attendant spawn), 1 = `[6,0,10]`, 2 = `[8,0,14]`; paddock
  3–7. Byte-identical round-trip, only the 8 index bytes changed.
- **Server core done**: `Config` (maxHorsesForSale, restockIntervalTicks); `Stock` (ordered
  slot list on the Attendant, `finalizeSpawn` horses, gap-on-purchase); `StableAttendant`
  restock timing + `findNearest`; `NotHorseEntity` reservation branch + FOR_SALE synched flag +
  glow; `Purchasing` (spawn/mount/mount-swap/action-bar failure); `HotelUI` default-tab +
  enabled-tab flags; messages `PurchaseHorseMessage`, `ShowPurchaseScreenMessage`,
  `OpenHotelMessage`, `ShowStockScreenMessage`, and `UIStockHorse`.
- **Client UI done**: ported `Tabs`/`Tab`/`RenderContext`; `HotelTabs` (player-face + cowboy
  sprite icons); `StockScreen`; `PurchaseScreen`; tabs wired into `HorseSummonScreen`; lang
  keys; a placeholder 16×16 `cowboy_head.png` (hand-authored — the design's capture-from-render
  ideal is still the intended final art).

## Still open

- **Restock does not yet broadcast a stock-changed packet to close open hotel screens.** The
  design calls for it; not built. Consequence today: a screen open at the moment of a Restock
  goes stale (same tolerated failure mode as a stale purchase click) rather than closing. Cheap
  to add when wanted — a PLAY_TO_CLIENT message to players within the eval radius.
- **Client rendering is unverified** — glow, reservation-in-the-barn, tab icons, the action-bar
  message. Out of the headless autotest's reach; needs a real client (`/run`) to eyeball.
- The exact eval/lookup radius for Stand-In→Attendant and Block→Attendant currently reuses
  `horseEvalRadius`; may want its own smaller value tuned to the Attendant's stroll leash.
- Final cowboy-head sprite via in-game capture (placeholder ships for now).
