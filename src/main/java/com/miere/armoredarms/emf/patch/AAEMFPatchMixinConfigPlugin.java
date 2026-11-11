package com.miere.armoredarms.emf.patch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Install gate for this mod's mixin config. Decides - once, and only from raw
 * class resources (no classes are loaded) - whether the patch mixins should apply:
 *
 * <ul>
 *   <li>EMF must be 3.3.4+, detected by {@code EMFAnimationEntityContext} being
 *       <em>absent</em> (EMF removed the class in 3.3.4; on older EMF, Armored
 *       Arms' own integration still works and the patch must stay dormant).</li>
 *   <li>The current EMF/ETF state API must be present ({@code EMFManager},
 *       {@code EMFEntityRenderState}, {@code ETFState}, {@code ETFEntityRenderState},
 *       {@code ETFEntity} - everything {@code com.miere.armoredarms.emf.patch.compat.EMFCompat}
 *       calls; verified present in EMF 3.3.4-3.3.12 and ETF 7.2.2-7.2.7).</li>
 *   <li>Armored Arms must be the expected 1.2.x build: its EMF manager class
 *       resources must contain the {@code handler$zbf000$emf$setHand} /
 *       {@code handler$zbf000$emf$unsetHand} mixin handlers (verified byte-exact
 *       in the released {@code ArmoredArms-v1.2.3-1.20.1} jar).</li>
 * </ul>
 *
 * <p>Any detection failure disables the mixins (fail-safe): on an unsupported
 * setup the patch must do nothing rather than risk making things worse. The
 * decision is logged as a single startup line so the active mode is visible.</p>
 *
 * <p>This class lives outside the {@code ...patch.mixins} package on purpose:
 * that package is mixin-owned, and classes inside a mixin package cannot be
 * loaded by name ("is in a defined mixin package ... cannot be referenced
 * directly").</p>
 */
public class AAEMFPatchMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("armoredarms_emf_patch");

    /** EMF removed this class in 3.3.4; its presence means EMF <= 3.3.3 (patch dormant). */
    private static final String LEGACY_CONTEXT_CLASS =
            "traben/entity_model_features/models/animation/EMFAnimationEntityContext.class";

    /** Current state API used by the replacement logic in {@code compat.EMFCompat}. */
    private static final String[] REQUIRED_API_CLASSES = {
            "traben/entity_model_features/EMFManager.class",
            "traben/entity_model_features/models/animation/state/EMFEntityRenderState.class",
            "traben/entity_texture_features/features/state/ETFState.class",
            "traben/entity_texture_features/features/state/ETFEntityRenderState.class",
            "traben/entity_texture_features/utils/ETFEntity.class",
    };

    /** Armored Arms' EMF integration classes (1.2.x), byte-scanned for their handlers. */
    private static final String AA_CORE_CLASS =
            "com/artur114/armoredarms/client/integration/emf/modelrender/ArmModelManagerEMF.class";
    private static final String AA_ANIM_CLASS =
            "com/artur114/armoredarms/client/integration/emf/modelrender/ArmModelManagerAnimEMF.class";

    /** AA's own injected handler names (mixin-generated, so unaffected by SRG remapping). */
    private static final String HANDLER_SET = "handler$zbf000$emf$setHand";
    private static final String HANDLER_UNSET = "handler$zbf000$emf$unsetHand";

    private boolean decided;
    private boolean applyCore;
    private boolean applyAnim;

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null; // default refmap from the config json
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        decide();
        if (mixinClassName.endsWith("ArmModelManagerEMFMixin")) {
            return applyCore;
        }
        if (mixinClassName.endsWith("ArmModelManagerAnimEMFMixin")) {
            return applyAnim;
        }
        LOGGER.warn("Unknown mixin in patch config: {} - not applying (fail-safe).", mixinClassName);
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    /**
     * Full detection, run once. Every failure path leaves both flags false,
     * which keeps the patch out of the way.
     */
    private void decide() {
        if (decided) {
            return;
        }
        decided = true;
        try {
            if (readResource(LEGACY_CONTEXT_CLASS) != null) {
                LOGGER.info("EMF <= 3.3.3 detected (legacy EMFAnimationEntityContext still present): "
                        + "Armored Arms' own integration works - patch mixins stay dormant.");
                return;
            }

            List<String> missing = new ArrayList<>();
            for (String required : REQUIRED_API_CLASSES) {
                if (readResource(required) == null) {
                    missing.add(required);
                }
            }
            if (!missing.isEmpty()) {
                LOGGER.warn("Required EMF/ETF classes missing ({}): patch mixins disabled (fail-safe).",
                        String.join(", ", missing));
                return;
            }

            byte[] core = readResource(AA_CORE_CLASS);
            if (core == null) {
                LOGGER.warn("Armored Arms EMF integration classes not found (mod missing or unsupported version): "
                        + "patch mixins stay dormant.");
                return;
            }
            if (!containsAscii(core, HANDLER_SET) || !containsAscii(core, HANDLER_UNSET)) {
                LOGGER.warn("ArmModelManagerEMF lacks the expected EMF handler methods (unsupported Armored Arms build): "
                        + "patch mixins stay dormant.");
                return;
            }
            applyCore = true;

            byte[] anim = readResource(AA_ANIM_CLASS);
            if (anim != null && containsAscii(anim, HANDLER_SET) && containsAscii(anim, HANDLER_UNSET)) {
                applyAnim = true;
                LOGGER.info("EMF >= 3.3.4 detected (EMFAnimationEntityContext removed): enabling Armored Arms "
                        + "first-person hand / armor compatibility mixins (including playerAnimator variant).");
            } else {
                LOGGER.info("EMF >= 3.3.4 detected (EMFAnimationEntityContext removed): enabling Armored Arms "
                        + "first-person hand / armor compatibility mixins (playerAnimator variant not installed - "
                        + "its mixin stays dormant).");
            }
        } catch (Throwable t) {
            LOGGER.error("Patch gate detection failed: patch mixins disabled (fail-safe).", t);
        }
    }

    /**
     * Class resource bytes, or null when absent. A read failure propagates so
     * the gate disables instead of misreading "unreadable" as "absent".
     */
    private static byte[] readResource(String path) throws IOException {
        try (InputStream in = AAEMFPatchMixinConfigPlugin.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    /** Plain ASCII search - sufficient for constant-pool name lookups. */
    private static boolean containsAscii(byte[] data, String needle) {
        byte[] pattern = needle.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
