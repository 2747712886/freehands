# Trinket Image Prompts

This document contains reusable prompts for the `freehands` trinket item textures. Do not place API endpoints, API keys, or local credential paths in this file.

## Shared Contract

- Target: Minecraft Forge item texture for `freehands`.
- Source: exactly `816x816` pixels, a logical `16x16` grid with `51x51` source pixels per final pixel.
- Final asset: transparent `16x16` RGBA PNG, one-pixel transparent edge, at most 16 opaque colors.
- Background: flat `#00ff00` chroma key only. No shadow, floor, reflection, text, UI, hand, or visible grid.
- Composition: one centered, front-facing forged amulet. Use the shared symmetric octagonal silhouette, layered plate construction, central recess, and four integrated diagonal anchor lugs.
- Avoid: glasses, lenses, rings, planets, orbit lines, scattered fragments, generic gemstone silhouettes, crosses, or detached edge pixels.

## Iron Master

```text
Generate exactly 816x816 pixels as a logical 16 by 16 Minecraft item pixel-art grid, 51 pixels per cell. A single centered front-facing compact forged iron talisman, occupying logical columns 2 through 13 and rows 2 through 13. Strong single-object silhouette: a dense, solid octagonal mechanical amulet with layered silver-gray iron plates around a recessed charcoal center bearing a tiny cool-white forge glyph; four short symmetric diagonal anchor lugs must touch the body. Every edge and color boundary aligns exactly to the logical 51x51 grid cells. Classic Minecraft item texture readability. Use a perfectly flat solid #00ff00 chroma-key background. No shadow, reflection, floor, text, UI, hand, or visible grid lines. Do not depict glasses, lenses, jewelry rings, planets, orbit lines, scattered fragments, crosses, or a simple geometric gemstone.
```

## Diamond Variant

Generate this from the approved iron master whenever image editing is available. Keep the silhouette, plate layout, central recess, anchor lugs, and logical grid unchanged. Replace only the material palette:

```text
Preserve the supplied Minecraft pixel-art iron talisman exactly: same silhouette, same 16x16 logical grid placement, same outer frame, layered plates, central recess, and four diagonal anchor lugs. Change only material: layered dark steel-blue metal with restrained cyan accents. Replace the tiny central forge glyph with a small faceted cyan diamond held inside the existing recess. Keep the compact forged-amulet style and flat #00ff00 background. No new objects, no geometry changes, no glow, no large cyan panel, no text, no shadow, and no visible grid.
```

## Netherite Variant

Generate this from the approved iron master whenever image editing is available. Keep the silhouette, plate layout, central recess, anchor lugs, and logical grid unchanged. Replace only the material palette:

```text
Preserve the supplied Minecraft pixel-art iron talisman exactly: same silhouette, same 16x16 logical grid placement, same outer frame, layered plates, central recess, and four diagonal anchor lugs. Change only material: layered near-black purple-gray netherite with subtle rose-gray facets. Replace the tiny central forge glyph with a small dark wine-purple core held inside the existing recess. Use only tiny muted antique-gold rivets at bezel corners, never a gold center. Keep the compact forged-amulet style and flat #00ff00 background. No new objects, no geometry changes, no glow, no text, no shadow, and no visible grid.
```

## Acceptance Checklist

1. Reject any source that is not exactly `816x816`.
2. Remove the chroma key before grid-locked normalization.
3. Reject visible asymmetry, detached edge pixels, or a source that changes the iron master silhouette for a material variant.
4. Inspect the nearest-neighbor `256x256` preview before replacing a texture under `src/main/resources/assets/freehands/textures/item/`.
