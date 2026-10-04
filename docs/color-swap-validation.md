# MOD-70: Swap Astral wood and Voidvine color schemes — validation

**Status: every automated check passes. The owner playtest below runs on the mod jar built by the draft PR's pipeline (requested by the owner on MOD-70).**

## Functional requirements

| FR | Where it is implemented | Checked by |
| -- | -- | -- |
| FR-1 Astral wood/bark take the old Voidvine greens | `recolor/astral_wood.json` | `ColorSwapRequirementsTest.astralWoodSetUsesTheGreens`, `VisualRevampRequirementsTest` |
| FR-2 Voidvine wood/block take the old Astral purples | `recolor/voidvine_wood.json` | `ColorSwapRequirementsTest.voidvineWoodSetUsesThePurples` |
| FR-3 `astral_trees.json` unchanged | not touched | `git diff` |
| FR-4 Voidvine plant and Voidfruit take the Astral leaves purple, cyan accents kept | `recolor/voidvine_plant.json` | `ColorSwapRequirementsTest.voidvinePlantUsesTheLeafPurpleAndKeepsCyanAccents`, `VoidvineRecolorTest` |
| FR-5 no old stops left | the three recipe files | `ColorSwapRequirementsTest.oldStopsAreGone` |
| FR-6 map colors | `ModBlocks` (`ASTRAL_*_COLOR`, `VOIDVINE_*_COLOR`, `voidvineProperties()`) | `ColorSwapGameTests` |
| FR-7 sources, targets, `flip`, `keep_only_mapped` unchanged | only gradient stops changed | `VoidvineRecolorTest`, `VisualRevampRequirementsTest`, `git diff` |

`./gradlew exportRecoloredTextures` shows the Astral log and planks in green, the Astral leaves and sapling still
purple, and the Voidvine, its item and the Voidfruit in purple with cyan berries.

## Owner playtest

Setup: in a creative world, run `/locate biome fwhipsperfectendupdate:end_forest` and teleport there. For Voidvines, use `/give @s fwhipsperfectendupdate:voidvine` and plant it under Voidsoil, or
find them under the islands.

- [ ] Astral trunks (green) no longer read as chorus plants.
- [ ] Voidvines (purple) no longer read as vanilla vines.
- [ ] Record: are purple Voidvines distinct enough from purple Astral leaves? (no change needed either way)
- [ ] Record: do Enderblossoms still stand out on green trunks? (no change needed either way)
