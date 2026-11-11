package com.miere.armoredarms.emf.patch.compat;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import traben.entity_model_features.models.animation.state.EMFEntityRenderState;
import traben.entity_texture_features.features.state.ETFEntityRenderState;
import traben.entity_texture_features.features.state.ETFState;
import traben.entity_texture_features.utils.ETFEntity;

/**
 * Runtime replacement for the three {@code EMFAnimationEntityContext} members that
 * Armored Arms 1.2.x uses from its EMF model managers, rebuilt on the EMF 3.3.x /
 * ETF 7.2.x render-state API. All members used here exist in every supported EMF
 * (3.3.4 - 3.3.12) and ETF (7.2.x) 1.20.1-forge build, verified via javap.
 *
 * <p>Call flow (reads like the original code, which for each arm side ran
 * {@code setHand(); super.render(); unsetHand();}):</p>
 *
 * <ul>
 *   <li>{@link #setHand()} replicates old {@code EMFAnimationEntityContext
 *       .setCurrentEntityIteration(state) + isFirstPersonHand = true}:
 *       {@link ETFState#mount} pushes the state and fires {@code activate(true)},
 *       which performs everything the old method did (entityRenderCount++,
 *       limb/head reset, model variation checks, LOD reset); the per-state
 *       {@code setIsFirstPersonHand(true)} replaces the old static flag write and
 *       drives EMF's {@code is_first_person_hand} animation variable and its
 *       {@code preventFirstPersonHandAnimating} gate.</li>
 *   <li>{@link #unsetHand()} replicates old {@code reset()}: nothing needs clearing
 *       by hand - {@link ETFState#unMount} pops the state and restores whatever was
 *       mounted before it (EMF's own manual player state inside its hand wrap, or
 *       nothing at all on EMF builds without the hand wrap).</li>
 * </ul>
 *
 * <p>Robustness details:</p>
 * <ul>
 *   <li>The mounted state is tracked exactly ({@link #setHand} refuses to pop
 *       anything it did not push itself), so EMF's own mounts are never disturbed
 *       even if Armored Arms skips an {@code unsetHand} (it has no finally).</li>
 *   <li>{@code setManualPlayerState(true)} marks our state the way EMF's own
 *       hand-wrap marks its manual player state, so EMF's wrap cleanup
 *       ({@code state.isManualPlayerState()} check) will also pop a run-away mount
 *       of ours on EMF builds that ship that wrap.</li>
 *   <li>{@link #setHand()} additionally self-heals a mount left over by a previous
 *       render that aborted before {@code unsetHand} (render exceptions are caught
 *       by Armored Arms' error pipeline, which skips the paired unset).</li>
 *   <li>Every call is wrapped: a failure inside here degrades to "no first-person
 *       arm animation context" instead of breaking Armored Arms' render.</li>
 * </ul>
 */
public final class EMFCompat {
    private static final Logger LOGGER = LogManager.getLogger("armoredarms_emf_patch");

    /** Exact state object we mounted in the current (or a leaked previous) hand render. */
    private static EMFEntityRenderState mountedByUs;

    private EMFCompat() {}

    /** Replacement body of {@code handler$zbf000$emf$setHand}. */
    public static void setHand() {
        try {
            healRunawayMount();

            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            // Same construction path AA's original code used; on EMF 3.3.x this yields
            // an EMFEntityRenderStateViaReference wired to the local player entity.
            var state = (EMFEntityRenderState) ETFEntityRenderState.forEntity((ETFEntity) player);
            if (state == null) {
                return;
            }

            state.setManualPlayerState(true);
            if (ETFState.state() != state) { // fresh state object, so effectively always mounted
                ETFState.mount(state);
                mountedByUs = state;
            }
            state.setIsFirstPersonHand(true);
        } catch (Throwable t) {
            LOGGER.error("EMF hand-state mount failed; Armored Arms continues without first-person arm animation context", t);
        }
    }

    /** Replacement body of {@code handler$zbf000$emf$unsetHand}. */
    public static void unsetHand() {
        try {
            if (mountedByUs != null) {
                if (ETFState.state() == mountedByUs) {
                    ETFState.unMount();
                }
                mountedByUs = null;
            }
        } catch (Throwable t) {
            LOGGER.error("EMF hand-state unmount failed", t);
        }
    }

    /**
     * If a previous hand render aborted between mount and unmount (Armored Arms has
     * no finally around its manager render), our state would still sit on the stack.
     * Pop it again - but only while it is still on top, never disturbing other mounts.
     */
    private static void healRunawayMount() {
        if (mountedByUs == null) {
            return;
        }
        if (ETFState.state() == mountedByUs) {
            ETFState.unMount();
        }
        mountedByUs = null;
    }
}
