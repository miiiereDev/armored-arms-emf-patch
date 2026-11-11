package com.miere.armoredarms.emf.patch;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AAEMFPatch.MOD_ID)
public class AAEMFPatch {
    public static final String MOD_ID = "armoredarms_emf_patch";
    public static final Logger LOGGER = LogManager.getLogger("armoredarms_emf_patch");

    public AAEMFPatch() {
        LOGGER.info("Armored Arms <-> EMF compatibility patch loaded. Actual mode is decided by the mixin config plugin (see its log line).");
    }
}
