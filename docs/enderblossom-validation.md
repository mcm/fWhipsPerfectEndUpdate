# Enderblossom validation (MOD-64)

Validation of the Enderblossom epic (MOD-58) against its PRD, done 2026-10-04 on branch
`minecraftmodsagent/mod-58-epic-enderblossom`. An agent did the automated part. **Only the owner can tick the
playtest checklist at the end, and it is still open.**

## Functional requirements

| FR | Where it is implemented | Checked by |
| -- | -- | -- |
| FR-1 placement, pop-off, self drop, pots | `EnderblossomBlock`, `PottedEnderblossomBlock`, blockstates, loot tables | `EnderblossomGameTests`, `EnderblossomRequirementsTest.wallAndPottedModels` |
| FR-2 10-tick Enderman check (wall + pot) | `Enderblossoms.regularCheck`, `EnderblossomTiming` | `EnderblossomSwitchingGameTests`, `EnderblossomTimingTest` |
| FR-3 sound, `BLOCK_CHANGE`, particles, ripple | `Enderblossoms.check` / `scheduleRipples` | `EnderblossomSwitchingGameTests`, `EnderblossomSoundsTest` |
| FR-4 emissive eye, idle sound 1/700 | `EnderblossomRenderer`, `template_wall_enderblossom_emissive`, `EnderblossomBlock.animateTick` | `EnderblossomRequirementsTest.texturesAreOriginalAndOpenModelIsEmissive`; the look itself: playtest |
| FR-5 shake on look / Enderman teleport | `EnderblossomShaking`, `LookShakeTracker`, `EnderManMixin`, `ShakeCurve` | `EnderblossomShakingGameTests`, `ShakeCurveTest`, `LookShakeTrackerTest` |
| FR-6 displacement stew | `DisplacementEffect`, `ChorusTeleport`, stew recipes, `MushroomCowMixin` | `EnderblossomStewGameTests`, `EnderblossomRequirementsTest.stewRecipesCarryDisplacement` |
| FR-7 1–4 blossoms on generated trees | `AstralTreeFeature.placeEnderblossoms`, both configured features | `EnderblossomTreeGameTests`, `EnderblossomRequirementsTest.treesConfigureOneToFourBlossoms` |
| FR-8 25% leaf drop | `loot_table/blocks/flowering_astral_leaves.json` | `EnderblossomTreeGameTests` (2,000 breaks), `EnderblossomRequirementsTest.floweringLeavesDropClosedBlossoms` |
| FR-9 no Mojang art or audio | original PNGs + `art/textures/block/*.grid`; `sounds.json` names vanilla files | `EnderblossomSoundsTest`, `EnderblossomRequirementsTest`, `checkNoMojangTextures` |

Extra checks:
- `mc-asset validate --profile minecraft:block` passes on all three block textures.
- A pixel comparison against the 26.4 eyeblossom textures in `~/projects/mcmeta-assets-tiny` found 0 identical
  pixels and 0 shared colors.
- The Fabric and NeoForge jars contain no `.ogg` files and nothing under `assets/minecraft/textures`.
- Fixed during validation: the potted Enderblossoms are now in `#minecraft:flower_pots`, like the mod's other
  pots.

## Owner playtest

Quick setup in a creative world:
`/locate biome someonesperfectend:end_forest`, then use `/summon minecraft:enderman ~ ~ ~5 {NoAI:1b}` to bring
an Enderman in and `/kill @e[type=enderman]` to remove it. To make an Enderman teleport, use `/effect give @e[type=enderman] minecraft:instant_damage`
(or hit it). For stew, craft a bowl, a brown mushroom, a red mushroom and either blossom.

- [ ] End Forest Astral trees show blossoms on their trunks and read as "giant weird flowers".
- [ ] Walking an Enderman (or waiting for one) near a tree opens the blossoms in a ripple, with sound and portal
  particles. They close after it leaves.
- [ ] Looking at a blossom makes it shake. Wearing a carved pumpkin, it doesn't.
- [ ] An Enderman teleporting nearby makes blossoms shake.
- [ ] Open blossoms glow in the dark.
- [ ] Enderblossom suspicious stew teleports you.
- [ ] Breaking Flowering Astral Leaves by hand sometimes drops a Closed Enderblossom.
- [ ] Potted blossoms open and close near Endermen.

Things no test can show, so worth a look: the tilted wall model on all four sides of a log, how the shake looks,
whether the eye glow reads at night, and whether the blossom disappears at the edge of the screen. The model
reaches about 3 pixels into the log, past the block entity's culling box.

## Owner playtest result (2026-10-04)

Everything worked except these two things, which have been changed since:

- Shakes repeated too slowly while looking. `LookShakeTracker.REPEAT_INTERVAL` went from 40 to 20 ticks.
- The look target was too small (only the top few pixels counted). `EnderblossomShaking.isLookingAt` now checks
  whether the player's look ray hits the blossom's whole block space (`lookBox`), still with line of sight and the
  carved pumpkin rule, instead of the Enderman dot-product test aimed at the flower head.

The screenshots also showed black squares behind the wall blossoms. `EnderblossomRenderer` asked
`ItemBlockRenderTypes` for the render type, and that falls back to `solid` for mod blocks registered through
`RenderTypeRegistry` on NeoForge, so transparent pixels came out black. It now always draws with `RenderType.cutout()`.
