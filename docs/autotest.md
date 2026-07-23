# In-game autotest

A headless, self-halting Forge **server** that runs gameplay checks in the real game, for
behaviour that unit tests cannot reach. No client, no human clicking.

```sh
./gradlew runServer -Dhorsehotel.autotest=true
```

Without `-Dhorsehotel.autotest=true` this is an ordinary `runServer` — the runner returns
immediately on `ServerStartedEvent` and registers nothing.

## First-time setup

The working directory `run/` is gitignored, so a fresh clone needs two files before the
server will boot:

```sh
mkdir -p run
printf 'eula=true\n' > run/eula.txt
cat > run/server.properties <<'EOF'
level-name=autotest
level-type=flat
generate-structures=false
online-mode=false
spawn-protection=0
max-tick-time=-1
EOF
```

`level-type=flat` keeps world generation fast and predictable; `generate-structures=false`
stops naturally generated stables from spawning stand-ins that would confuse the scenarios;
`max-tick-time=-1` disables the watchdog so a slow tick cannot kill the run.

## Reading results

Lines are prefixed `[autotest]` and land in `run/logs/latest.log`.

| What you need | Command |
| --- | --- |
| Final aggregate | `grep '\[autotest\] RESULT:' run/logs/latest.log` |
| Per-scenario | `grep -E '\[autotest\] \S+ \[(PASS\|FAIL)\]' run/logs/latest.log` |
| Only failures | `grep '\[autotest\].*\[FAIL\]' run/logs/latest.log` |
| Did it even boot | `grep -E '\[autotest\] (BOOT_OK\|SUITE_START)' run/logs/latest.log` |

A passing run ends `RESULT: N/N passed (ALL PASS)` and exits 0. A failing run ends
`(FAILURES)` and exits 1, so gradle fails the build.

## How it works

`AutoTestRunner` hooks `ServerStartedEvent`, force-loads the chunks the scenarios need
(`TicketType.FORCED` grants entity ticking, which stand-ins need and which no real player is
present to provide), spawns a Forge `FakePlayer`, and registers a `ServerTickEvent` listener.

`ScenarioRunner` then walks the scenarios one at a time. Each is set up, left alone for
`settleTicks()`, then checked. **Scenarios advance on real server ticks rather than in a
loop** — this matters, because the behaviour under test only runs once every
`Config.horseRefreshInterval` ticks.

A scenario that throws is reported as a failure rather than taking the server down, so one
broken scenario cannot hide the rest of the results.

## Adding a scenario

Implement `Scenario` (or extend the `StandInScenario` base in `StandInScenarios`, which
handles spawning a stand-in on a stone platform and cleaning it up) and add it to
`StandInScenarios.all()`. If it needs a position in a new chunk, add that position to
`positionsNeedingChunks()` or it will never tick.

Return `null` from `check` to pass, or a sentence describing what went wrong to fail. Failure
strings end up in the log verbatim, so include the actual values.

## What this cannot cover

Everything here is **server-side state**. That covers the visibility rules, which are decided
on the server and synced to clients — but not rendering itself, and not client-side sync
hazards. There is no client in the run.

## Current scenarios

| Scenario | Asserts | Fails pre-fix? |
| --- | --- | --- |
| `standin:hidden_before_first_tick` | A stand-in is hidden from the moment it is added, before it has ticked | **Yes** |
| `standin:hidden_with_no_player_in_range` | A stand-in with nobody within the eval radius stays hidden | No |
| `standin:hidden_with_empty_registry_at_40_blocks` | A stand-in 40 blocks from a player who has registered nothing stays hidden | No |
| `standin:visible_when_registered` | A stand-in shows the claimant's registered horse | No |

`hidden_before_first_tick` is the regression test for the reported bug: stand-ins rendering as
blank white horses in the distance that vanish as you approach. Clients are sent a stand-in as
soon as they come within tracking range, which does not wait for the entity to start ticking —
so a stand-in that only became hidden inside `tick()` was briefly visible with variant 0
(white), and disappeared once its chunk began ticking. It is verified to fail against the
pre-fix code and pass after.

The other three are guard rails rather than regression tests — they pass both before and after
the fix. They pin behaviour that the fix reworked (the eval radius, the refresh throttle, the
reveal path); `visible_when_registered` in particular is the positive control, without which
hiding every stand-in unconditionally would pass the suite.

**When adding a "regression test", check it against the broken code.** Two of the scenarios here
were originally written believing they reproduced the bug; running them against `HEAD` showed
they passed, which is what led to finding the real mechanism.
