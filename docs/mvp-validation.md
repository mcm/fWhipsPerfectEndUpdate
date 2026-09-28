# MVP validation (US-029)

Validation of "Someone's Perfect End Update" MVP (MC 1.21.1, Fabric + NeoForge) against the PRD
(`.ralph-tui/tasks/prd-someones-perfect-end-update.md`), done 2026-09-28 on branch
`steve/mod-1-epic-someones-perfect-end-update-mvp-mc-1211-fabric-neoforge`.

The automated part (gates, jar contents, re-checking every acceptance criterion and FR against code, data and
tests) was done by an agent. **The owner playtest checklist at the end is still open**; only the owner can
tick it.

## 1. Quality gates

| Gate | Result |
| -- | -- |
| `./gradlew clean build` (clean tree) | Passed (compile of common/fabric/neoforge + JUnit) |
| `./gradlew gameTestAll` from a clean `build/` | Passed: Fabric **227/227**, NeoForge **227/227** required GameTests; 347 s wall, 10.2 GB peak RSS (see README "Measured cost") |
| `checkNoMojangTextures` (part of `check`) | Passed |

Fixed during validation:
- **`gameTestAll` failed on a clean checkout.** After `clean`, `fabric/build/gametest/eula.txt` is gone. owo-lib
  (via Accessories) then prompts for the EULA on stdin, and `Scanner.next()` throws on empty stdin, which
  crashed the Fabric server. The GameTest task now pipes `false` into stdin (`build.gradle`,
  `configureGameTestTask`). That declines the prompt, and GameTest mode doesn't need an accepted EULA.
- **`stripped_voidvine_block` dropped nothing** (no loot table). Added
  `loot_table/blocks/stripped_voidvine_block.json`, plus a guard GameTest
  `SmokeGameTests.everyBlockHasALootTable`: every mod block that doesn't opt out with `noLootTable()` must
  have a loot table.
- A committed log file, `common/logs/latest.log`, was removed from git, and `logs/` was added to `.gitignore`.

## 2. Built jars

Checked `someonesperfectend-{fabric,neoforge}-1.0.0.jar`:
- **No PNG files at all** and nothing under `assets/minecraft/`. Every texture is generated at runtime by
  the recolor pipeline.
- The only `.nbt` files are our own GameTest air templates (`structure/gametest/empty*.nbt`). The Endling
  village is placed in code. No Mojang structure files.
- `data/minecraft/` holds only additive tags (none use `"replace": true`) and `worldgen/noise_settings/end.json`
  (End only).

## 3. Standalone purity

Automated evidence:
- **Accessories:** `TrinketGameTests.trinketIsTheOnlyAccessorySlot` passes: the player's slot list is only
  `trinket`, and armor stands have none. `trinketSlotSitsAboveOffHand` passes. The library's default slots are
  unbound via `data/accessories/accessories/entity/default.json` and an empty
  `defaulted_targets` tag, and its unique slots via `UniqueSlotHandlingMixin`. The inventory button and screen
  are disabled by `AccessoriesClientMixin`.
- **Biomes:** `EndBiomeGameTests.endBiomeSourceIsVanillaPlusOurs` (the End is vanilla's 5 biomes plus our 3) and
  `overworldAndNetherAreVanilla` (500 sampled positions per dimension match vanilla) pass.
- **Worldgen:** the only noise-settings override is `end.json`. The only `BiomeModifications` call targets
  `minecraft:end_highlands`.

Needs the owner's client check (see the checklist): Accessories' "Open Accessories Screen" key binding
still appears under Controls (it does nothing), and Accessories' config screen and `/accessories` command are
still present. Decide whether those count as "extra UI".

## 4. Acceptance criteria re-check (US-001 – US-026, US-030 – US-039)

Every criterion was checked against code, data and the registered tests (all test classes are in
`ModGameTests.TEST_CLASSES`). **No criterion is unmet.** Criteria that can only be judged visually (textures,
tint, models, animations, GUI labels) are covered by the owner playtest. The remaining findings are
deviations or ambiguities for the owner to decide:

| # | Story | Finding | Suggested decision |
| -- | -- | -- | -- |
| 1 | US-013 / FR-9 | `EndBiomeGameTests.outerEndBiomeCoverage` computes coverage as new-biome hits ÷ **island** positions (32.8%). The AC says "20–40% of the sampled positions". Over all 2000 samples (void included) it is about 17.8%. The placement AC ("25–35% of outer-island area") supports the island denominator. | Confirm that island area is the intended denominator, or raise the weights |
| 2 | US-016 | Oasis pools: a 1-in-2 chance of 1–2 pools **per chunk**, so an island spanning several chunks can get more than 2. Containment is tested; "1–2 per island" is not. | Accept, or add a per-island cap |
| 3 | US-022 | `#minecraft:logs` includes `#minecraft:logs_that_burn`, which includes our `#astral_logs`, so `astral_log` is itself a barter input (and a barter output). | Probably exclude our own logs from `endling_barter_inputs` |
| 4 | US-004/005 | `astral_sapling` is not in `#minecraft:saplings`. If added, it would also become a barter input (see #3). | Decide together with #3 |
| 5 | US-030 | The Infuser screen shows tier-locked effects greyed out rather than hiding them ("offers only effects valid for … the current tier"). The server enforces the rule. | Accept greyed out, or hide |
| 6 | US-034 | The 200-roll totem loot check is a GameTest (`totemLootTableGivesEveryTotem`), not a JUnit unit test. The check is the same. | Accept |
| 7 | US-037 (Tech. considerations) | Ender Bed locations use mixin-stored player NBT, not the Fabric/NeoForge attachment APIs the PRD suggests. It meets every AC (death, relog, restart, End exit are tested) with one common code path. | Accept |
| 8 | US-017 | The "~10% of `end_highlands` islands" figure is actually 10% of 64-block regions (measured 7–13%). The test's "island block above voidsoil" check also accepts voidsoil. | Accept |
| 9 | US-018 | End City frequency is −15% vs vanilla (limit ±20%). Within spec, but near the edge. | Accept |

Not asserted by tests but confirmed by reading the code: the Endling refusal sound (`villager.no`), Ender Bed
travel in rain or thunder, the dragon fight not starting on arrival, Oasis Water evaporating in the Nether (via
`#minecraft:water`), and the Infuser acting as a beacon in the End (same ticker as in the Overworld).

## 5. Functional requirements

| FR | Status | Evidence |
| -- | -- | -- |
| FR-1 multi-loader build | Met | Gates above |
| FR-2 runtime recolor, no Mojang assets | Met | `RecolorGameTests`, recolor unit tests, jar check |
| FR-3 End Turf / Short Void Grass / Voidsoil | Met | `EndGroundGameTests` |
| FR-4 Astral + Voidvine sets, trees, boats | Met | `AstralWood/AstralTree/VoidvineWood/BoatGameTests`; stripped voidvine loot fixed |
| FR-5 Voidvine + Voidfruit | Met | `VoidvineGameTests` |
| FR-6 Oasis Water | Met (color: playtest) | `OasisWaterGameTests` |
| FR-7 Void Petals, Glowcap, Bounce Cap | Met | `EndFloraGameTests`, `BounceCapGameTests` |
| FR-8 elevation bands, vanilla inner End, ≥50% void | Met | `ElevationBandGameTests` |
| FR-9 biomes 20–40%, decoration, no other biome changes | Met with caveat #1 | `EndBiome/EndPlains/EndForest/EndOasis/UnderIslandFoliageGameTests` |
| FR-10 End Cities in >1 band, ±20% | Met | `EndCityGameTests` |
| FR-11 Driftjellies | Met | `Driftjelly(Husbandry)GameTests` |
| FR-12 Endlings, Lost state | Met | `EndlingGameTests` |
| FR-13 barters (default + special + beds) | Met (see #3) | `EndlingBarter/EndlingSpecialBarter/EnderBedGameTests` |
| FR-14 Endling villages | Met (`/locate`: playtest) | `EndlingVillageGameTests` |
| FR-15 single trinket slot above off hand | Met (render: playtest) | `TrinketGameTests` |
| FR-16 Infuser beacon + End-only infusing | Met | `InfuserGameTests`, `InfusionRulesTest` |
| FR-17 infused gear | Met | `InfusedGearGameTests` |
| FR-18 effect totems | Met | `EffectTotemGameTests`, `TotemStackingGameTests`, `TotemRulesTest` |
| FR-19 End-chanter + Void Cleansing | Met | `EndChanterGameTests`, `VoidCleansingGameTests`, rules tests |
| FR-20 Ender Beds | Met (render: playtest) | `EnderBed/EnderBedRespawn/EnderBedTravelGameTests`, `EnderBedRulesTest` |
| FR-21 dragon-kill barter gate | Met | `EndlingBarterGameTests` (US-039 tests) |

Non-goals also re-checked: vanilla beds are untouched (no `BedBlock` mixin; Ender Beds aren't in `#minecraft:beds`), no
enchantments are registered, the Overworld and Nether have no worldgen overrides, and metadata, README and lang
never imply affiliation with fWhip or Mojang.

## 6. Owner playtest checklist (open)

Run each item on **both** Fabric and NeoForge (`./gradlew :fabric:runClient`, `./gradlew :neoforge:runClient`).

- [ ] Purity: the inventory shows one accessory slot (`trinket`, above the off hand) and no Accessories button in
      survival or creative. Decide on the leftover Accessories key binding, config screen and `/accessories` command
- [ ] Fresh world: beat the dragon, go through a gateway, and see the three elevation bands, all three new
      biomes, under-island foliage, End Cities in more than one band, and an Endling village (`/locate structure
      someonesperfectend:endling_village`)
- [ ] Bounce Caps let you hop between bands; a missed jump can still drop you into the void
- [ ] Breed Driftjellies in the End; they go dormant in the Overworld
- [ ] Before the dragon: an Endling refuses (head shake + villager "no"). After: barter works, and Endlings become
      Lost after 15 s in the Overworld (hollow-eyed texture)
- [ ] Oasis Water keeps a fixed color in the Overworld, gives 1.5× potion durations, and makes end stone with lava
- [ ] Barter a beacon for an Infuser; infuse boots with Speed II on a tier-4 pyramid in the End and charge them;
      infusion is refused in the Overworld, where the Infuser works as a beacon
- [ ] Barter a Totem of Undying for an effect totem, activate it with netherite at a tier-4 Infuser, and equip it;
      you get Haste III inside a Haste II beacon, infused gear stops draining, and the totem renders on the player
- [ ] Dragon head at a bookshelf position: add an enchantment to an enchanted sword, then Void Cleansing
      (dragon's breath) lowers its repair cost; the "Void Cleansing" label shows in slot 3
- [ ] Barter a red bed for a red Ender Bed, which renders upside down under an outer-End island and can't go on
      top of a block; dying in the End respawns you falling from it; the Overworld spawn is unchanged
- [ ] Second Ender Bed in the Overworld: daytime sleep travels to the End bed and back; with the destination
      broken, the animation plays, you stay put, and the "missing or obstructed" message shows
- [ ] An Ender Bed explodes in the Nether
- [ ] Visual pass: recolored textures (woods, leaves with no biome tint, turf, flora, Bounce Cap, boats and rafts,
      Infuser, totems, beds, Endling, Driftjelly glow) look acceptable as placeholders
- [ ] Overall, the End still reads as "desolate but worth exploring"
- [ ] Decide on findings #1–#9 in section 4
