# Contributing to Someone's Perfect End Update

This is the developer guide for the mod: how the code is laid out, how its systems work, and how to build and
test it. For what the mod adds to the game, see the [README](README.md).

- Mod id: `someonesperfectend`
- Base package: `io.mcmaster.someonesperfectend`
- Minecraft 1.21.1, Java 21, Fabric + NeoForge from one [Architectury](https://docs.architectury.dev/) codebase

## Build layout

```
build.gradle          root: shared Loom/Architectury setup, GameTest harness, `gameTestAll`
gradle.properties     versions (Minecraft, loaders, Architectury API, Fabric API, JUnit)
common/               loader-independent code and resources (Mojang mappings)
  src/main/java/io/mcmaster/someonesperfectend/
    SomeonesPerfectEnd.java     common init (called by both loader entrypoints)
    registry/ModRegistries.java Architectury DeferredRegisters + creative tab + registration helpers
    recolor/                    runtime texture recolor pipeline (see "Textures")
    mixin/                      common mixins (someonesperfectend.mixins.json)
    gametest/                   GameTests shared by both loaders
  src/main/resources/
    assets/someonesperfectend/lang/en_us.json
    assets/someonesperfectend/recolor/            texture recolor recipes (.json)
    data/someonesperfectend/structure/gametest/   GameTest structure templates (.nbt)
  src/test/java/                JUnit 5 unit tests (run by `./gradlew build`)
fabric/               Fabric entrypoints, fabric.mod.json, @ExpectPlatform impls (`<package>.fabric.*Impl`)
neoforge/             NeoForge @Mod class, META-INF/neoforge.mods.toml, @ExpectPlatform impls
                      (`<package>.neoforge.*Impl`), NeoForge-only mixins (someonesperfectend-neoforge.mixins.json)
```

All gameplay code goes in `common`. The loader modules only hold entrypoints, metadata and the small
amount of loader-specific glue that Architectury cannot abstract. When common code needs a loader-specific
object, use Architectury's `@ExpectPlatform` (see `fluid/OasisWaterPlatform`).

### Registering content

`ModRegistries` holds one Architectury `DeferredRegister` per registry the mod uses (blocks, items, entity
types, fluids, potions, features, structure types, structure piece types, creative tabs). Content classes register
through its helpers, for example:

```java
public static final RegistrySupplier<Block> VOIDSOIL =
        ModRegistries.block("voidsoil", () -> new Block(BlockBehaviour.Properties.of()));
```

`block(...)` also registers a `BlockItem` in the "Someone's Perfect End Update" creative tab;
`ModRegistries.itemProperties()` gives item properties that put an item in that tab. Make sure the
class holding the entries is loaded before `ModRegistries.register()` runs (reference it from
`SomeonesPerfectEnd.init()`).

### Adding Minecraft versions (26.1, 26.3)

Only 1.21.1 is built today. The plan for the 26.1 and 26.3 ports
(`tasks/prd-someones-perfect-end-update-ports.md`) is:

1. Move the version-specific values in `gradle.properties` (`minecraft_version`, loader, Architectury and
   Fabric API versions, Java version) into per-version property files, e.g. `versions/1.21.1.properties`,
   `versions/26.1.properties`, `versions/26.3.properties`.
2. Build each version from the same sources with a multi-version preprocessor such as
   [Stonecutter](https://stonecutter.kikugie.dev/) (`//? if >=26.1 {` comments), selecting the active
   version with a Gradle property. Where a version difference is large, put it behind a small internal
   interface in `common` with one implementation per version instead of scattering conditionals.
3. Keep version-sensitive code (registries, data components, rendering, worldgen, GameTest glue) in thin
   wrappers such as `ModRegistries` and `ModGameTests` so a port touches as few files as possible.
4. Run `./gradlew build gameTestAll` once per version in CI.

## Textures

Mojang's textures are never committed or packaged. Placeholder textures are generated on the client at every
resource reload by recoloring the player's own vanilla textures:

1. Recipes are JSON files in `common/src/main/resources/assets/someonesperfectend/recolor/` (resource packs can
   add or replace files there too).
2. On every client resource reload, `ReloadableResourceManagerMixin` builds `RecolorPackResources`: for each
   recipe it reads the source texture (and its `.png.mcmeta`, so animations are kept) from the highest-priority
   pack that has it, applies the palette, and serves the result from an in-memory pack at the **lowest**
   priority. The log reports `Recolor: generated N textures (M replaced by real PNGs, E errors)`.
3. **Hand-drawn art wins:** if any pack (the mod's own assets or a user resource pack) has a real PNG at a
   recipe's target, that recipe is not generated. To replace a placeholder, add
   `assets/someonesperfectend/textures/<path>.png`; no code or recipe change is needed.

Original hand-drawn textures (the Enderblossoms so far) are authored with the
[mc-asset](https://www.npmjs.com/package/mc-asset) CLI. Their editable sources live in `art/textures/<path>.grid`
(plus the `.mcpx` that `mc-asset render` writes) and mirror the PNG paths under
`assets/someonesperfectend/textures/`. To change one, edit the `.grid` and re-render it:

```
mc-asset render art/textures/block/open_enderblossom.grid --force \
    --output common/src/main/resources/assets/someonesperfectend/textures/block/open_enderblossom.png \
    --source art/textures/block/open_enderblossom.mcpx
mc-asset validate common/src/main/resources/assets/someonesperfectend/textures/block/open_enderblossom.png \
    --profile minecraft:block
```

A recipe file holds one recipe or a list sharing a palette:

```json
{
  "palette": {"gradient": ["#241f52", "#6f6fcf", "#d8d0ff"]},
  "recipes": [
    {"source": "minecraft:block/bamboo_planks", "target": "someonesperfectend:block/astral_planks"},
    {"source": "minecraft:block/bamboo_door_top", "target": "someonesperfectend:block/astral_door_top",
     "palette": {"hsv": {"hue": 250, "saturation": 0.6}, "colors": {"#e3c16a": "#c9c6ff"}}}
  ]
}
```

`source` and `target` are texture ids (`namespace:path` under `textures/`, no `.png`; the namespace defaults to
`minecraft`). A palette has at most one of:

- `gradient`: colors spaced evenly from dark to light, or `{"at": 0..1, "color": "#rrggbb"}` stops. Each pixel's
  luminance picks a color. With `"normalize": true` (the default) the source's darkest pixel maps to the first
  stop and its brightest to the last.
- `hsv`: `hue` (absolute degrees) or `hue_shift` (degrees), plus `saturation` and `value` multipliers.

and optionally `colors`, exact `"#rrggbb": "#rrggbb"` replacements applied before the gradient or shift.
Alpha is always kept and fully transparent pixels are left alone.

Each recipe can also set:

- `"keep_only_mapped": true`: only pixels whose source RGB is a key of the (recipe's or shared) palette's
  `colors` are kept and recolored; every other pixel becomes transparent. Handy for cutting an overlay (such as
  berries) out of a vanilla texture.
- `"flip": "vertical"` or `"flip": "horizontal"`: mirrors the image after recoloring. A vertical flip of an
  animated texture (one with a `.png.mcmeta`) turns each square frame upside down and keeps the frame order.
  Any other value is a load error naming the recipe file.

```json
{
  "palette": {"gradient": ["#0f2616", "#3d7d45", "#76b371"],
              "colors": {"#eb8931": "#3fd3dd", "#f7e26b": "#c6fbff"}},
  "recipes": [
    {"source": "minecraft:block/twisting_vines", "target": "someonesperfectend:block/hanging_vine",
     "flip": "vertical"},
    {"source": "minecraft:block/cave_vines_lit", "target": "someonesperfectend:block/hanging_vine_berries",
     "flip": "vertical", "keep_only_mapped": true}
  ]
}
```

`./gradlew exportRecoloredTextures` writes every recipe's output to `build/recolored-textures/` (git-ignored)
from the vanilla client jar Loom downloaded (or `-PclientJar=<path>`), as a starting point for artists.
`./gradlew build` runs `checkNoMojangTextures` in every module, which fails if any output jar or source tree
contains `assets/minecraft/textures`.

## Trinket slot

The single trinket slot is built on [Accessories](https://github.com/wisp-forest/Accessories) (Wisp Forest,
`io.wispforest:accessories-*`, which also pulls in owo-lib). Code is in `common/.../trinket/`.

- The slot is an Accessories "unique slot" named `someonesperfectend:trinket` (`Trinkets.init()`), bound to
  players only, in strict mode (data packs can't resize it or change what it accepts). It accepts only items in
  `#someonesperfectend:trinkets`. An item's behavior comes from an Accessories `Accessory` registered for it
  (see `totem/EffectTotemItem`); its look on the player comes from an `AccessoryRenderer` registered in
  `SomeonesPerfectEndClient` (`TrinketRenderer` hangs it on the front of the belt). Register one for every new
  trinket item.
- `InventoryMenuMixin` adds `TrinketSlot` to the vanilla player `InventoryMenu` after the vanilla slots (so it is
  slot 46 and vanilla indices don't move), directly above the off-hand slot. The slot reads and writes the
  Accessories container, so other mods using Accessories see the equipped item. Shift-clicking a trinket from the
  inventory, hotbar, or off hand moves it into the empty slot.
- **Creative inventory:** directly above the off-hand slot would overlap the window's top border, so in the
  creative "Survival Inventory" tab the slot sits **immediately to the left of the off-hand slot** (x=16, y=20).
  `ServerGamePacketListenerImplMixin` lets the creative slot-sync packet write to it (vanilla only accepts slots
  1–45).
- **Purity:** with only this mod and its libraries installed, the trinket slot is the only accessory slot:
  - `data/accessories/accessories/entity/default.json` (replace) binds none of the library's default slots
    (hat, ring, belt, ...) to any entity, and `data/accessories/tags/entity_type/defaulted_targets.json` is
    emptied;
  - `UniqueSlotHandlingMixin` unbinds the library's own cosmetic-armor unique slots (`accessories:head`,
    `accessories:chest`, ...) from every entity;
  - `AccessoriesClientMixin` removes the library's inventory button and makes its screen and key binding do
    nothing.

  The `TrinketGameTests.trinketIsTheOnlyAccessorySlot` GameTest checks the player's slot list.

### Totem of Haste mining boost

Haste III or higher mines much faster **only while an activated Totem of Haste** (one with the
`someonesperfectend:totem_activated` component) is worn in the trinket slot, e.g. a Haste II beacon plus the
totem. The Haste multiplier becomes `1 + 0.6 × (amplifier + 1)` instead of vanilla's `1 + 0.2 × (amplifier + 1)`,
so Efficiency V with Haste III breaks end stone and deepslate instantly (Efficiency IV does not). Haste II with the
totem, and Haste III from anywhere else (commands, other mods) without the totem, stay vanilla.

- The rules are in `totem/HasteRules` (unit-tested). `PlayerMixin` swaps vanilla's 0.2 per level in
  `Player#getDestroySpeed` (NeoForge: `getDigSpeed`) for the boosted value, so Mining Fatigue, underwater, not on
  the ground, the block-break-speed attribute, and Conduit Power alone are unchanged.
- The totem is read from the Accessories slot, which is synced, so the client and server agree on mining speed.
- GameTests: `HasteBoostGameTests`.

## End terrain (elevation bands)

The outer End (at least 1024 blocks from the origin) is split into three elevation bands: lower, middle
(vanilla, surface about Y=57–62), and upper. The override of `data/minecraft/worldgen/noise_settings/end.json`
is the vanilla file with the terrain density wrapped in `someonesperfectend:elevation_bands`
(`worldgen/ElevationBandsFunction`, settings and offset math in `ElevationBands`), and with the noise height
raised from 128 to 192 so raised islands fit.

- **Whole islands (MOD-71):** the function moves whole islands, never parts of one (`worldgen/ElevationBandLayout`).
  It reuses vanilla's own island model: `minecraft:end_islands` puts island centers on a 16-block grid and gives
  each a round island (`100 - distance × falloff`). Centers whose islands, cut at value 0 (a little past vanilla's
  edge at 8, to cover the 3D noise fringe), come within 24 blocks of each other form one island group, found by
  flood fill and cached. Each group gets a seeded band and jitter, rolled from its lowest center, and all of it
  moves by one offset. Every 8-block noise cell corner belongs to the center with the highest value there; a
  corner next to a corner of a group with another offset is forced to void, so cells are never sheared, and the
  link margin keeps those seams off the islands. The seeded island noise is read from the `end_islands` function
  in the argument (`EndIslandDensityFunctionAccessor`).
- **Offsets grow with distance (MOD-47):** for a group whose center nearest the origin is `d` blocks out,
  `s = clamp(1 + 0.5 × (d − 1184) / (8000 − 1184), 1, 1.5)`. The lower band moves islands by `round(−32 × s)`
  and the upper band by `round(56 × s)`: −32/+56 just past the first gateways, −39/+68 at 4,000 blocks, and
  −48/+84 from 8,000 blocks on.
- **Per-island jitter (MOD-47):** each group (in any band) also moves by a seeded amount of up to
  ±`round((6 + 6t) × s)` blocks, where `t` grows linearly from 0 at 1,184 blocks to 1 at 8,000 blocks: ±6 at
  1,184 blocks, ±10 at 4,000, and ±18 from 8,000 on. Islands in one band therefore sit at different heights. The
  jitter is per group rather than per column so that no island is warped.
- **Snapped to the noise cell:** the total offset is rounded to a multiple of 4 blocks (the End's noise cell
  height), so a moved island is exactly the vanilla island shape, translated.
- **Vanilla center:** within 1024 blocks nothing changes, and groups with a center within 1,184 blocks are always
  middle band with no jitter. Vanilla islands are at most about 89 blocks in radius, so every island reaching
  inside about 1,095 blocks stays put: the central island, the void ring, and End Gateway exits stay vanilla,
  and bands start right past the first gateways.
- **Build height clamp:** the total offset is clamped to −48…+116 (`ElevationBands.clampOffset`, unit tested).
  Vanilla outer terrain tops out at Y=66, so raised islands stay inside the 192-block noise. The defaults never
  exceed +104, and jitter never sinks an island below the deepest band. Lowered terrain gets a floor slide at its
  real Y (Y 0→16, shorter than vanilla's 4→32) that trims undersides reaching the floor, so nothing leaves the
  build height. Small, low island pieces (vanilla top below about Y=50) in far-out lower-band groups can be
  trimmed away entirely.

`ElevationBandsTest` checks with the vanilla island noise that no island column is cut or sheared, and the
offset, jitter, and clamp math. `ElevationBandGameTests` checks the real terrain on a fixed seed: the inner End
matches the vanilla `end.json`, read from the vanilla data pack, block for block; over a 4096×4096 outer area every
band holds at least 15% of island columns and at least 50% of columns are void; within 1,184 to 1,600 blocks at
least 2 bands appear; and moved islands are their vanilla shape at their offset. The Overworld and Nether are not
touched.

End Cities use the vanilla structure, structure set, and pieces unchanged. Vanilla starts a city only where
its footprint's lowest corner is at Y ≥ 60. `EndCityStructureMixin` (`worldgen/EndCities`) measures that
corner again with the bands bypassed (`ElevationBandsFunction.withoutBands`), so a city starts exactly where
it would on vanilla terrain, at its moved height. The moved corner must still be within 4 blocks of
`60 + offset` of the start column, and starts in the void seams between groups are rejected, so no city starts
over the void. `EndCityGameTests` finds starts in all three bands of a fixed-seed 4096×4096 area. Over 8192×8192
it counts 61 starts, the same as with vanilla terrain (the target is within ±20%).

## End biomes

`end_plains`, `end_forest`, and `end_oasis` are data (`data/someonesperfectend/worldgen/biome/`): vanilla End
ambience (Endermen, mood sound, water colors, `end_gateway_return`) with pastel fog/sky colors and ambient
particles. They are placed by [TerraBlender](https://github.com/Glitchfiend/TerraBlender) (required library):
`ModBiomes.registerPlacement()` adds them to TerraBlender's End highlands and midlands lists (Fabric: the
`terrablender` entrypoint; NeoForge: common setup). With vanilla's weight of 10 per category, the weights in
`ModBiomes` give about 30% of outer-island positions (oasis rarest). TerraBlender's own weights and biome sizes are
in its `config/terrablender.toml`.

- **Surface rules** live in the same `end.json` override: `end_turf` on island tops in all three biomes, with
  `voidsoil` patches (noise `someonesperfectend:voidsoil_patch`) in forest and oasis. Other biomes keep end stone.
- **Purity:** with only TerraBlender, the Overworld and Nether use only vanilla's region, so their biomes are
  exactly vanilla. TerraBlender 4.1 builds its End "small islands" area from the edge list, which would turn the
  void between islands into `end_barrens`; `TheEndBiomeSourceMixin` (runs before TerraBlender) restores
  `small_end_islands` there. `EndBiomeGameTests` checks all of this plus the coverage.
- The biomes are also in `#minecraft:is_end`, `#c:is_end`, and `#minecraft:has_structure/end_city`, so End
  Cities keep their vanilla frequency.
- Like vanilla biome changes, only newly generated chunks use the new biomes; existing chunks are untouched.

## Oasis Water

Oasis Water (`fluid/OasisWaterFluid`) is vanilla water with a teal color: it extends `WaterFluid` and is in
`#minecraft:water`, so flow, swimming, drowning, farmland and evaporation in the Nether are vanilla. Lava turns it
into end stone instead of cobblestone (`OasisWaterInteractions`), and bottles filled from it brew Oasis potions
(`alchemy/OasisPotions`).

**Oasis-logging (MOD-45).** Waterloggable blocks can hold Oasis Water instead of vanilla water:

- Every block whose state definition has `waterlogged` (vanilla, this mod, other mods) also gets a boolean
  `oasis_logged` state property, default `false` (`StateDefinitionBuilderMixin`, `BlockMixin`). A state with
  `waterlogged=true, oasis_logged=true` holds an Oasis Water source (`BlockStateBaseMixin`); every other state is
  unchanged. Setting `waterlogged=false` always clears `oasis_logged`, and `oasis_logged=true` implies `waterlogged`
  (`StateHolderMixin`), so no reachable state is `waterlogged=false, oasis_logged=true`.
- The Oasis Water bucket (player and dispenser) Oasis-logs a waterloggable block and schedules an Oasis Water tick.
  An empty bucket (player or dispenser) takes an Oasis Water bucket back out and clears both properties. The
  default `SimpleWaterloggedBlock` methods accept Oasis Water (`SimpleWaterloggedBlockMixin`). Blocks that override
  `placeLiquid` for vanilla water only (campfires, candles) run their own logic with water, which then becomes Oasis
  Water (`OasisLogging.placeOasisWater`).
- In an `ultraWarm` dimension (the Nether) the Oasis Water bucket never waterlogs anything and never places a
  source. It fizzes, smokes and returns an empty bucket in survival, like vanilla water.
- An Oasis-logged block flows Oasis Water into free space next to it, and breaking it leaves an Oasis Water source.
- Shapes, light and face sturdiness of an `oasis_logged=true` state come from the same state with
  `oasis_logged=false` (`BlockStateBaseMixin`, `BlockStateCacheMixin`), because some blocks (vanilla walls) only
  precompute shapes for their vanilla states.
- **Placement (MOD-48).** Any waterloggable block placed from a block item into an Oasis Water source is
  Oasis-logged, even if its `getStateForPlacement` only waterlogs in `Fluids.WATER` (slabs, lanterns, most blocks).
  The hook wraps `BlockItem#place`'s placement state, so it works for every block class (`BlockItemMixin`,
  `OasisLogging.placementState`). Like vanilla, a block placed into flowing Oasis Water stays dry (unless the block
  itself waterlogs in falling water; it is then Oasis-logged). A block merged into the same block (a second slab,
  another candle) keeps the block's own rule: a double slab drops the water.
- **Neighbor updates (MOD-48).** Waterloggable blocks schedule a `Fluids.WATER` tick when a neighbor changes. Where
  the fluid is Oasis Water, `LevelAccessorMixin` schedules an Oasis Water tick instead (`ServerLevelMixin` also runs
  Oasis Water for older water ticks), so Oasis Water, never vanilla water, flows out when e.g. an adjacent block is
  broken, and it meets lava with the Oasis results (end stone). A glass bottle on an Oasis-logged block fills with
  Oasis Water.
- **Known limitation.** Code that compares a fluid with `Fluids.WATER` directly (`getType() == Fluids.WATER`, as
  opposed to `is(FluidTags.WATER)`) sees an Oasis-logged block as dry. The mod covers vanilla's placement, tick and
  bucket paths, but other mods' blocks or items that hard-code `Fluids.WATER` may treat Oasis-logged blocks as not
  waterlogged (e.g. not put out, not pick up the water).

**Block state count.** Every waterloggable block has twice as many states. Measured by
`OasisLoggingGameTests.everyWaterloggableBlockHasOasisLogged` in the GameTest environment (vanilla, this mod and
its dependencies): 357 of 1,128 blocks are waterloggable, and the block state registry grows from 28,159 to 47,253
states (+19,094, about +68%). The added states cost memory once per game (the state objects and their lookup
tables), not per chunk: chunks only store the states they use.

**Removing the mod.** Saved chunks store block states by name and properties. Without the mod, `oasis_logged` is an
unknown property and is ignored when the chunk is loaded, so Oasis-logged blocks become blocks waterlogged with
vanilla water. Oasis Water source and flowing blocks are unknown blocks and become air, like any removed mod block.

`OasisWaterGameTests` covers the fluid, lava and brewing. `OasisLoggingGameTests` covers Oasis-logging: the state
property on every waterloggable block, the fluid state, buckets on an oak slab, stairs, fence, leaves and
`voidvine_trapdoor`, a campfire, dispensers, flowing out, breaking, and the Nether; and (MOD-48) placing a slab and a
lantern into an Oasis source, a slab into flowing Oasis Water, stairs flowing Oasis Water out when a neighbor is
removed, lava next to that flow, and a glass bottle.

## Endling villages

`someonesperfectend:endling_village` is a custom structure type (`worldgen/EndlingVillageStructure`) whose
pieces are placed by code (`EndlingVillagePieces`); it uses no structure `.nbt` files. The plan
(`EndlingVillageLayout`) has 1–3 `huge_glowcap`s near the center and a ring of 3–6 huts around them (Voidvine
plank floor, Astral or Voidvine walls, glowcap-block roof, doorway facing the center). Each hut spawns one
endling bound to the village center (`Endling#setVillage`). A start needs a column at least 1024 blocks (plus
the village radius) from the origin, a biome in `#someonesperfectend:has_structure/endling_village`
(`end_plains`, `end_forest`, `end_highlands`), and an island that reaches past the village radius all around,
with hut and glowcap sites close to the center's height. Rarity is set in
`worldgen/structure_set/endling_villages.json` (random spread, spacing 29 chunks, kept 3 chunks away from End
City candidates). On the GameTest seed that gives about 1.0 villages per 1500×1500 blocks.

The GameTest server creates its world with structure generation turned off, so its real chunks never get
structure starts and `/locate` finds nothing there. `EndlingVillageGameTests` therefore runs vanilla's
structure-start step (`ChunkGenerator#createStructures`) on a fresh proto chunk and places the start the way
`/place structure` does.

## Endling bartering and the Voidvine wood set

Endlings in the End take one item from `#someonesperfectend:endling_barter_inputs` (dropped by, or handed over
from, a player who has `minecraft:end/kill_dragon`), inspect it for 6 seconds and roll
`loot_table/gameplay/endling_bartering.json` (special inputs such as beacons, totems and beds roll their own
tables under `gameplay/endling_barter/`). The default table gives Astral saplings and logs, Voidvine Blocks
(weight 12, 1–2), Voidsoil, End Turf, Void Petals, Glowcaps, Bounce Caps and Voidfruit.

Voidvine itself (the hanging plant grown under Voidsoil) only crafts into sticks. The Voidvine Block has no
recipe: it comes from Endling barters, or from breaking placed ones. Voidvine and Stripped Voidvine Blocks
(`#someonesperfectend:voidvine_blocks`) craft into Voidvine Planks; stripped blocks also make hanging signs, and
the planks make the rest of the set. `EndlingBarterGameTests` and `VoidvineWoodGameTests` check this.

## Quality gates

```
./gradlew build          # compiles common, fabric and neoforge; runs JUnit tests in common
./gradlew gameTestAll    # runs the headless GameTest server for Fabric, then for NeoForge
./gradlew spotlessApply  # formats the code (spotlessCheck only checks)
```

### Formatting

Java is formatted with [palantir-java-format](https://github.com/palantir/palantir-java-format) through Spotless;
Gradle, properties, Markdown and workflow files only get whitespace cleanup. The Spotless config lives in a
standalone build under `gradle/spotless/` so checking formatting never configures Loom; the root `spotlessApply`
and `spotlessCheck` tasks delegate to it. To check without configuring the main build, run
`cd gradle/spotless && ../../gradlew spotlessCheck` (`-p gradle/spotless` does not work).

Bulk reformatting commits are listed in `.git-blame-ignore-revs`. GitHub skips them in blame views; locally run
`git config blame.ignoreRevsFile .git-blame-ignore-revs`.

### CI

Every branch is treated the same. Protected branches only change through pull requests, so nothing runs a second
time after a merge.

| Workflow | Runs on | Jobs | Required check |
| -- | -- | -- | -- |
| `checks.yml` | every push to any branch | Spotless, Semgrep, TruffleHog | `Checks ✅` |
| `pull-request.yml` | pull requests, weekly on the default branch | build + unit tests + jar checks, `gameTestAll`, changelog warning | `PR ✅` |
| `publish-curseforge.yml` | `v<mod_version>-mc<minecraft_version>` tags | build + `gameTestAll`, then upload of both jars to CurseForge | — |

Branch protection should require `Checks ✅` and `PR ✅` and have "Require branches to be up to date before
merging" turned on, so the merged result is what CI tested. The weekly run catches upstream breakage, such as a
change in the snapshot Loom and Architectury Gradle plugins.

### GameTests

- Write tests in `common/src/main/java/io/mcmaster/someonesperfectend/gametest/` as
  `public static void name(GameTestHelper helper)` methods annotated with the vanilla `@GameTest`, and add the
  class to `ModGameTests.TEST_CLASSES`.
- `@GameTest(template = "...")` takes a **full** template id (`someonesperfectend:gametest/<name>`); leaving it
  empty uses `ModGameTests.EMPTY_TEMPLATE`, an 8×8×8 air structure. Templates are vanilla structure `.nbt` files
  under `common/src/main/resources/data/someonesperfectend/structure/gametest/`.
- Each loader registers only `ModGameTests` (Fabric: the `fabric-gametest` entrypoint; NeoForge:
  `RegisterGameTestsEvent`). Its `@GameTestGenerator` turns the annotated methods into test functions. This
  is needed because Fabric and NeoForge build template ids from `@GameTest` differently.
- `:fabric:runGameTestServer` and `:neoforge:runGameTestServer` each start a headless test server in
  `<loader>/build/gametest/` (the world is wiped first). A task fails if the server exits non-zero (vanilla
  exits with the number of failed required tests), if the log does not report at least one passing required
  test, or if the run takes longer than 15 minutes. `gameTestAll` depends on both and runs them one after
  another.
- The run directory has no `eula.txt` after `clean`. owo-lib (a dependency of Accessories) then asks for the EULA on
  stdin and crashes the server on empty input. The task pipes `false` into stdin, which declines the prompt.
  GameTest servers don't need an accepted EULA, so nothing is accepted on your behalf.
- Fabric also writes a JUnit report to `fabric/build/gametest/junit.xml`.

### Measured cost in the 4-core / 16 GB container

Measured on 2026-09-27 with `./gradlew gameTestAll` after `./gradlew build`, with 2 smoke tests per loader.
Peak memory is the peak sum of RSS for all Java processes (Gradle daemon plus game server), sampled every second.

| Run | Wall time | Peak memory (RSS) |
| -- | -- | -- |
| Cold (Gradle daemon stopped first) | 71 s | 2.6 GB |
| Warm daemon, run 1 | 59 s | 2.6 GB |
| Warm daemon, run 2 | 58 s | 2.9 GB |
| MVP suite, 227 tests per loader (2026-09-28, US-029, `--no-daemon`) | 347 s | 10.2 GB |

The last row was re-measured for the final validation (US-029). Its peak also covers the preceding
`./gradlew build` (a separate `--no-daemon` build, sampled the same way).

The loaders never run at the same time. Each game server is capped at `-Xmx3G` (`gametest_max_heap` in
`gradle.properties`) and the Gradle daemon at `-Xmx3G`. This fits the 16 GB budget with plenty of headroom, so
**`gameTestAll` is the gate and no fallback is needed**. If memory becomes tight as tests grow, run one loader
per invocation (`./gradlew :fabric:runGameTestServer`, then `./gradlew :neoforge:runGameTestServer`).

## License

All rights reserved (placeholder until the owner picks a license).
