# Botania Unbound

**Botania Unbound** is a mod that does not focus on balance — it overhauls a large number of Botania's core mechanics.

## Configuration Overview

Below is a summary of all configurable features, organized by category.

---

### Entropinnyum
- Allows Entropinnyum to detect TNT exploding in liquids.

---

### Narslimmus
- Accepts all slimes (including spawner‑spawned, split slimes, magma cubes, Tinkers' slimes, etc.).
- Works outside slime chunks.

---

### Endoflame
- Removes the 32,000‑tick burn time limit and auto‑expands capacity for high‑energy fuels.
- Skips the burn process (generates mana instantly).

---

### Thermalily
- Ignores cooldown; consumes lava immediately after the previous burn ends.
- During cooldown, does not consume lava (behaves like normal idle state).
- Skips burn time; generates mana instantly after lava consumption.

---

### Mana Enchanter
- Receives all enchantments from an enchanted book (vanilla only takes the first).
- Allows enchanting books (not just items).
- Allows conflicting enchantments (e.g., Infinity + Mending).
- Drops enchantments as enchanted books if the multiblock is broken after enchanting.
- Reads enchantments from enchanted items as if they were enchanted books.

---

### Terra Blade
- Beam damage inherits the player's `ATTACK_DAMAGE` attribute (including base weapon damage, enchantments, Strength potion, equipment bonuses, etc.) and Fire Aspect ignites targets.

---

### Thunder Sword
- Chain lightning bounces infinitely between enemies until no targets remain or damage reaches zero.
- Chain lightning damage inherits the player's `ATTACK_DAMAGE` attribute.

---

### Star Sword
- Falling star damage inherits the player's `ATTACK_DAMAGE` attribute.
- Prevents falling stars from damaging non‑living entities (e.g., item frames, paintings).

---

### Gaia Guardian
- Allows fake players (e.g., from dispensers, ComputerCraft turtles) to damage the Gaia Guardian and trigger the ritual.
- Attacking the Guardian during its mob‑spawn phase shortens that phase (damage reduces remaining spawn ticks).
- Removes the 25‑damage‑per‑hit cap.

---

### Gourmaryllis
- Does not consume (destroy) food items while chewing, preventing waste.
- Skips chewing time; generates mana immediately after eating.
- No mana reduction when eating the same food repeatedly.
- Removes the 12‑nutrition cap; auto‑expands mana capacity for high‑nutrition foods.

---

### Bubbell
- Shulkers within range gain Regeneration I (every 10 seconds, lasts 11 seconds).
- Swallows Shulker Bullets, producing 15,000 mana.
- Swallows monsters targeted by Shulkers, producing 75,000 mana without consuming the Shulker.

---

### Dandelifeon
- Configurable cycle speed (ticks per cycle, vanilla is 10).
- Enables a reform mechanism (2048‑style cell merging, redstone direction push, cell number display).
- Temporarily expands mana buffer when single‑cycle production exceeds max mana.
- Configurable initial cell number (vanilla is 1).
- Configurable maximum cell generation age (vanilla is 100).
- Skips cycle when mana is full (instead of wasting cells).

---

### Mana Storm
- Configurable death time (ticks after all bursts are fired, vanilla is 200).
- Can disable mana pulse activation.
- Can disable explosion from rays hitting blocks.
- Can disable the final explosion.
- Configurable duration (vanilla ~1350 ticks).
- Allows redstone activation.
- Configurable total number of bursts (vanilla is 250).

---

### Kekimurus
- Configurable number of cake bites per cycle (max 7).
- Can eat Botania alt grass blocks (equivalent to 1 bite, turns to dirt).
- Can eat dropped cake items (produces mana for 7 bites).
- Prioritizes scanning cakes from higher Y levels.

---

### Arcane Rose
- Extracts XP from players at maximum speed (drains all available XP per tick).
- Absorbs all experience orbs in range per tick.

---

### Rafflowsia
- Auto‑expands mana buffer when overflow would occur.
- Can consume Botania flower items (dropped items, not just placed blocks).
- Uses a new mana formula based on unique flower count (if diminishing returns is disabled, same flowers count as unique).
- No mana reduction when eating the same flower type repeatedly.

---

### Munchdew
- No cooldown when mana buffer is full or no leaves are found for 5 ticks.

---

### Spectrolus
- Accepts any color wool/sheep instead of requiring a specific sequence.
- Does not consume wrong‑color wool items.

---

### Pylon Pump (Pylons transfer mana from a pool below to a bound pool)

#### Mana Pylon
- Enabled by default.
- Configurable loss ratio (0.0 = no loss, default 0.1).
- Maximum distance to bound target (default 64).
- Particle strength (default 3).
- Particles toggle (default on).
- Transfer rate per tick (default 10,000; Spark pair transfers 1,000).
- Allows vertical stacking (multiple pylons can drain from the same source).

#### Natura Pylon
- Enabled by default.
- Loss ratio 0.1, max distance 64, particle strength 3, particles on, transfer rate 10,000, vertical stacking allowed.

#### Gaia Pylon
- Enabled by default.
- Loss ratio 0.0, max distance 128, particle strength 5, particles on, transfer rate 50,000, vertical stacking allowed.
