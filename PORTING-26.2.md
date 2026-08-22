# BleachHack → Minecraft 26.2 port notes

This branch (`26.2`) ports BleachHack from 1.20.4 to **Minecraft 26.2**.

## What changed at the platform level

Minecraft **26.1+ ships deobfuscated** — the game jar contains the real
`net.minecraft` names (the old "Mojang mappings" names). Consequences:

- No Yarn, no mappings file. Loom uses the **non-obfuscated** plugin variant
  (`net.fabricmc.fabric-loom`, *not* the legacy `fabric-loom` remap id), plain
  `implementation` (no `modImplementation`), and no `mappings` dependency.
- Java 21, Gradle 9.5.1, loader 0.19.3. No Fabric API (BleachHack doesn't use it).
- Access widener uses the `official` namespace.
- The whole source moved Yarn → real names (`MinecraftClient`→`Minecraft`,
  `Text`→`Component`, `Vec3d`→`Vec3`, `Box`→`AABB`, packet/screen/GUI packages, …).

Build: `./gradlew build` → `build/libs/bleachhack-1.2.6.jar`.

## Status

All 316 source files compile against 26.2 and the mod jar builds. Roughly 90%
of features carried over with no behavior change. The rest fall into the buckets
below; **nothing was deleted** — degraded paths are stubbed with `// TODO(26.2):`
markers (grep for them, ~57 across ~45 files) so you can decide.

---

## Needs a decision (stubbed / dormant until reworked)

### 1. Everything shader-based (biggest one)
26.2 made shaders **fully data-driven** (`ShaderManager` / `PostChainConfig`);
the old "construct a `PostChain`/`GlProgram` from a resource path" API is gone,
and `VertexConsumer.fixedColor` (per-block tinting) was removed. Stubbed inert:
- **ShaderRender** module (post-process screen shaders) — no-op.
- **ESP** / **BlockHighlight** "Shader" render modes — no-op (their **Box**/**outline** modes work).
- **StorageESP** shader outline — no-op.
- **Xray** opacity slider (translucent non-ore tinting) — no-op; **hiding non-ore blocks still works**.

Options: rebuild each on the new data-driven pipeline (register
`post_effect/<id>.json` + `ShaderManager.getPostChain`), or drop the shader
modes and keep the box/outline equivalents.

### 2. World overlays now ride Minecraft's own `Gizmos` API
26.2 added an official immediate-mode debug-shape API. BleachHack's `Renderer`
is now a thin facade over it, so ESP boxes / Tracers / etc. render through
vanilla. Trade-off: **gizmos take one color per shape**, so per-vertex color
gradients collapse to a single color, and **world-space GUI item icons have no
replacement** — Nametags' held-item and AutoSteal's project preview show
**item name + count text** instead of the item sprite. Decision: accept the
text fallback, or rebuild item billboards on the new item submit pipeline.

### 3. Removed game APIs (feature capability changed)
- **DamageUtils**: armor/attack **enchantment contributions dropped** from damage
  prediction (the helpers are server-only now). Base/attribute damage is correct.
- **EventDamage**: now hooks `LivingEntity.hurtServer` (server-side only), so it
  **may not fire on multiplayer clients**. Any module keying off damage events
  is affected on servers.
- **Criticals** attack detection: attacks moved to a separate packet
  (`ServerboundAttackPacket`); the old interact-packet path is dead — needs a
  rewrite to trigger crits on the new packet.
- **Sky/dimension effects** (`DimensionSpecialEffects` removed): Ambience's
  End-skybox override is dormant (sky/fog **color** overrides still work).
- **PlaceOperation** ghost preview (AutoBuild etc.): the 3D block-model ghost
  can't render inline anymore; the translucent shape outline still draws.
- **CmdNBT / CmdGive** legacy raw-item-NBT: item data is data-components now, so
  arbitrary-NBT injection is stored into `minecraft:custom_data` only (legacy
  `display`/`tag` presets no longer fully reconstruct).

---

## Behavior deltas (working, just slightly different — FYI, no action needed)

- **Movement packets** (`ServerboundMovePlayerPacket`) gained a
  `horizontalCollision` flag in 26.x; all spoofed packets (Criticals, Step,
  Nofall, Flight, ClickTp, AntiHunger, ArrowJuke, PlayerCrash) send `false`.
  On a hardened anticheat (2b2t) this is a new wire field — worth watching.
- **AutoArmor** detects armor as "has EQUIPPABLE component for a humanoid-armor
  slot", which now also matches carved pumpkins / mob heads.
- **CmdEnchant**: level clamped to 255 (the component codec's cap; was 32767).
- **Nametags**: name/health/enchant text collapses to a single color (see #2).
- **GUI z-ordering** uses 26.2 "strata" which can't be popped; a couple of
  overlays (search popup, clickgui tooltips) note a cosmetic ordering caveat.
- **CmdServer** difficulty/permission fidelity is client-side approximate (as
  it always was pre-26.2).

---

## "Dupes"

You mentioned legacy **dupe** features as likely discard candidates. There is
**no module or command named or clearly implementing a dupe** in this codebase
snapshot — grep for `dupe`/`duplicat` only hits an unrelated comment. If dupes
lived in an older version or are baked into another feature, point me at it and
I'll review/handle it. Nothing dupe-related was removed.

## Not yet done

- No runtime/in-game test yet (needs a 26.2 client launch + a server). Compile
  and mixin-AP validation pass; runtime behavior of the reworked mixins/rendering
  still needs a play-test.
- The stubbed features above.
