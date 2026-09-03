# Custom Fog

Client-only Fabric mod for Minecraft **26.1.2**. Replaces vanilla distance fog with world-space volumetric mist.

Repository: https://github.com/Guest-04/castomfog

## Requirements

- Minecraft 26.1.2
- Java 25
- Fabric Loader 0.19.3+
- Fabric API `0.155.2+26.1.2`
- Optional: Iris + Complementary Unbound (or other packs)

## Install

1. Put `customfog-1.4.0.jar` in `.minecraft/mods`
2. Put Fabric API in `mods` too
3. Launch the Fabric 26.1.2 profile

Build from source:

```
gradlew.bat build
```

Jar: `build/libs/customfog-1.4.0.jar`

## Fog

Density lives in the **world**, not as a bubble on the camera.

| Distance from (0, 0) on XZ | Air | Water |
|---|---|---|
| under ~1000 | almost none | murky, worse if deep |
| 1000 → 5000 | ramps up | thicker + depth |
| 5000+ | near-zero visibility | ink, especially deep |

Water uses a separate color/density profile. Deeper Y under sea level (~63) adds more fog.

## Shaders

With Iris the fog is drawn after the pack's final pass, using a depth copy taken before composite. Complementary Unbound should keep looking shaded; our layer sits on top. If a pack already does heavy underwater fog, the two can stack. Config: `config/customfog.json`.

## License

CC0-1.0
