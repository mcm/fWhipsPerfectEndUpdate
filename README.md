# fWhip's Perfect End Update

A Minecraft mod that brings to life the End update described by the YouTuber fWhip in his video on the
"perfect End update". It adds life, terrain variety, and endgame systems to the End while keeping it desolate.
The mod is **inspired by** that video and is **not affiliated with or endorsed by fWhip or Mojang**.

- Minecraft 1.21.1, for **Fabric** and **NeoForge**
- Changes only the outer End. The central island, the dragon fight, the Overworld, and the Nether stay vanilla.
- New terrain only shows up in newly generated chunks.

## Installation

Install the mod jar for your loader together with its required libraries:

| Library | Fabric | NeoForge |
| -- | -- | -- |
| [Architectury API](https://modrinth.com/mod/architectury-api) | required | required |
| [Fabric API](https://modrinth.com/mod/fabric-api) | required | — |
| [Accessories](https://modrinth.com/mod/accessories) (pulls in owo-lib) | required | required |
| [TerraBlender](https://modrinth.com/mod/terrablender) | required | required |

Java 21 is required. The mod must be installed on both the server and every client.

## Features

### Terrain

- **Elevation bands.** In the outer End (from just past the first End Gateways) each island sits lower, at
  vanilla height, or higher, and no two islands in a band are quite level. The height differences grow the
  further out you go. Islands always move whole, never split; islands at different heights are separated by
  void, so you need a way to climb or bridge between them.
- **End Cities** still generate, now in all three bands.

### Biomes

Three new biomes cover about a third of the outer islands:

- **End Plains**: End Turf, Short Void Grass, Void Petals, and Bounce Caps.
- **End Forest**: Astral trees, Glowcaps, and patches of Voidsoil.
- **End Oasis** (the rarest): pools of Oasis Water, huge Glowcaps, and patches of Voidsoil.

Each has its own pastel fog and sky colors and ambient particles. Under the islands of the forest, the oasis,
and vanilla's End Highlands, patches of Voidsoil grow Voidvine that hangs into the void.

### Blocks and plants

- **End Turf**, **Short Void Grass**, and **Voidsoil**: new End ground.
- **Astral wood**: a full wood set (logs, planks, stairs, slabs, fences, doors, trapdoors, buttons, pressure
  plates, signs, hanging signs), plus leaves, saplings, and the **Astral Boat**.
- **Voidvine**: hangs under Voidsoil and bears **Voidfruit**. Voidvine itself only crafts into sticks; the
  **Block of Voidvine** comes from Endling barters and makes a second wood set and the **Voidvine Raft**.
- **Enderblossoms** grow on Astral trees. They open when an Enderman is near, shake when you look at them
  (unless you wear a carved pumpkin), and can be cooked into a stew that teleports you like a chorus fruit.
- **Void Petals**, **Glowcaps** (bone meal grows a huge one), and **Bounce Caps**: a Bounce Cap launches you
  high into the air, gives Slow Falling while you stand on it, and cancels fall damage when you land on it.
  Sneak to stop the bounce. A missed jump can still drop you into the void.
- **Oasis Water**: brewing from an Oasis Water bottle makes potions whose effects last 1.5 times as long.

### Creatures

- **Driftjellies**: passive jellyfish that float above the islands. Tempt and breed them with Voidfruit; they
  drop gel you can cook. Outside the End they go dormant.
- **Endlings**: short, friendly Ender folk that live in **Endling villages** around giant Glowcaps. Once you
  have beaten the Ender Dragon, drop or hand an Endling an item and it throws back a barter (saplings, logs,
  Voidvine Blocks, Voidfruit, and more; some items such as beacons, Totems of Undying, and beds have their own
  trades). Endlings that spend too long outside the End become Lost and stop trading for good.

### Endgame systems

- **Trinket slot**: one extra slot above your off hand (left of it in the creative inventory) for trinkets.
- **Infuser** (bartered for a beacon): works as a beacon anywhere. In the End it can also infuse boots,
  chestplates, tools, and weapons with a beacon effect, which they then apply while worn or held for as long
  as their stored charge lasts. Stronger effects need a bigger pyramid.
- **Effect totems** (bartered for a Totem of Undying): Speed, Haste, Resistance, Jump Boost, and Strength.
  Activate one with netherite at a full Infuser in the End, then wear it in the trinket slot. It stacks one
  level on top of a matching beacon.
- **End-chanter**: an enchanting table with a dragon head in a bookshelf position can enchant items that are
  already enchanted. **Void Cleansing** uses dragon's breath to lower an item's anvil repair cost.
- **Ender Beds** (bartered for beds, in all 16 colors): placed upside down under an outer-End island, an
  Ender Bed sets where you respawn after dying in the End. With a second Ender Bed in the Overworld you can
  sleep during the day to travel between the two. Like normal beds, they explode in the Nether.

## Textures

The mod ships no Mojang textures. Most of its placeholder textures are generated from your own copy of the
game each time resources load, by recoloring vanilla textures. A resource pack can replace any of them with a
normal PNG.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the code layout, how the systems work, and how to build and test the
mod.

## License

All rights reserved (placeholder until the owner picks a license).
