# Someone's Perfect End Update

A Minecraft mod that brings to life the End update described by the YouTuber fWhip in his video on the
"perfect End update". The mod is **inspired by** that video and is **not affiliated with or endorsed by fWhip
or Mojang**.

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

## End terrain (elevation bands)

The outer End (at least 1024 blocks from the origin) is split into three elevation bands: lower (surface
about Y=40), middle (vanilla, about Y=62), and upper (about Y=100). The override of
`data/minecraft/worldgen/noise_settings/end.json` is the vanilla file with the terrain density wrapped in
`someonesperfectend:elevation_bands` (`worldgen/ElevationBandsFunction`, layout math in `ElevationBands`).
The function divides the outer End into seeded Voronoi regions about 320 blocks across. It gives each region a
band and moves that region's islands by the band's offset (−20, 0, or +40 blocks, multiples of the 4-block
noise cell). A 24-block void strip separates regions with different bands, so an island is split, never
sheared. Within 1024 blocks nothing changes. Regions near that circle are always middle band, so the central
island, the void ring, and End Gateway exits stay vanilla. `ElevationBandGameTests` checks this against the
vanilla `end.json` read from the vanilla data pack. The Overworld and Nether are not touched.

## Quality gates

```
./gradlew build          # compiles common, fabric and neoforge; runs JUnit tests in common
./gradlew gameTestAll    # runs the headless GameTest server for Fabric, then for NeoForge
```

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
- Fabric also writes a JUnit report to `fabric/build/gametest/junit.xml`.

### Measured cost in the 4-core / 16 GB container

Measured on 2026-09-27 with `./gradlew gameTestAll` after `./gradlew build`, with 2 smoke tests per loader.
Peak memory is the peak sum of RSS for all Java processes (Gradle daemon plus game server), sampled every second.

| Run | Wall time | Peak memory (RSS) |
| -- | -- | -- |
| Cold (Gradle daemon stopped first) | 71 s | 2.6 GB |
| Warm daemon, run 1 | 59 s | 2.6 GB |
| Warm daemon, run 2 | 58 s | 2.9 GB |

The loaders never run at the same time. Each game server is capped at `-Xmx3G` (`gametest_max_heap` in
`gradle.properties`) and the Gradle daemon at `-Xmx3G`. This fits the 16 GB budget with plenty of headroom, so
**`gameTestAll` is the gate and no fallback is needed**. If memory becomes tight as tests grow, run one loader
per invocation (`./gradlew :fabric:runGameTestServer`, then `./gradlew :neoforge:runGameTestServer`).

## License

All rights reserved (placeholder until the owner picks a license).
