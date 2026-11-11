# Armored Arms - EMF Compatibility Patch

Fixes the conflict between Armored Arms 1.2.3 and Entity Model Features 3.3.4+ on Minecraft 1.20.1 (Forge). Client side only, no config, ~7 KB.

## What it fixes

Armored Arms 1.2.x was built against EMF 3.2.4 and still calls a class that EMF removed in 3.3.4. With EMF 3.3.4 or newer installed, first-person arm rendering fails with:

- `NoClassDefFoundError: traben/entity_model_features/models/animation/EMFAnimationEntityContext` (Hand render layer error)
- `java.util.NoSuchElementException` and "The armor is blacklisted and will not be rendered!" (fallout of the first error)

This patch replaces the removed calls in Armored Arms' two EMF managers with the current EMF/ETF state API, and both errors go away.

## Compatibility

| Your setup | Do you need this mod? |
|---|---|
| Armored Arms 1.2.3-1.20.1 + EMF 3.3.4 to 3.3.12 + ETF 7.2.x | Yes |
| Armored Arms 1.2.3-1.20.1 + EMF 3.3.3 or older | No (the patch stays dormant) |
| A future Armored Arms update with fixed EMF support | Remove this mod |

Works with and without playerAnimator.

## Install

1. Install Armored Arms, EMF and ETF (all 1.20.1 Forge).
2. Drop this jar in your `mods` folder next to them.

At startup the log shows one line telling you which mode the patch picked (fix applied, dormant, or disabled with a reason).

## Status

This mod is a stopgap. Armored Arms is expected to fix its EMF integration upstream; once that lands, this patch serves no purpose and should be removed.

## Building

```
gradlew build
```

Output lands in `build/libs`. The EMF/ETF jars in `libs/` are compile-time references only and are not bundled.

## Credits

Armored Arms by Artur114. Entity Model Features and Entity Texture Features by Traben. This patch by Miere, MIT.
