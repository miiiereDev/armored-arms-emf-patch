package com.miere.armoredarms.emf.patch.mixins;

import com.miere.armoredarms.emf.patch.compat.EMFCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armored Arms 1.2.3-1.20.1 copied EMF's first-person hand mixin handlers into
 * {@code ArmModelManagerEMF} (methods pasted verbatim from an old EMF mixin, called
 * directly with a {@code null} CallbackInfo). Those handlers call the now-removed
 * static class {@code EMFAnimationEntityContext}, which is what crashes EMF 3.3.4+.
 *
 * <p>We replace the bodies of both handlers with the 3.3.x state-stack equivalent
 * (see {@link EMFCompat}). {@code ci.cancel()} at HEAD is essential: apart from
 * short-circuiting the broken logic, it also prevents the JVM from ever resolving
 * the removed class referenced by Armored Arms' constant pool.</p>
 *
 * <p>Applied only when the mixin config plugin confirms EMF 3.3.4+ and the exact
 * handler method names are present in the installed Armored Arms build.</p>
 */
@Pseudo
@Mixin(targets = "com.artur114.armoredarms.client.integration.emf.modelrender.ArmModelManagerEMF", remap = false)
public abstract class ArmModelManagerEMFMixin {

    @Inject(method = "handler$zbf000$emf$setHand", at = @At("HEAD"), remap = false, cancellable = true)
    private void aaemfpatch$setHandReplacement(CallbackInfo ci) {
        EMFCompat.setHand();
        ci.cancel();
    }

    @Inject(method = "handler$zbf000$emf$unsetHand", at = @At("HEAD"), remap = false, cancellable = true)
    private void aaemfpatch$unsetHandReplacement(CallbackInfo ci) {
        EMFCompat.unsetHand();
        ci.cancel();
    }
}
