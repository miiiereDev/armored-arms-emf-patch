package com.miere.armoredarms.emf.patch.mixins;

import com.miere.armoredarms.emf.patch.compat.EMFCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same fix as {@link ArmModelManagerEMFMixin}, but for the playerAnimator variant
 * {@code ArmModelManagerAnimEMF} (used when playerAnimator is installed next to EMF).
 */
@Pseudo
@Mixin(targets = "com.artur114.armoredarms.client.integration.emf.modelrender.ArmModelManagerAnimEMF", remap = false)
public abstract class ArmModelManagerAnimEMFMixin {

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
