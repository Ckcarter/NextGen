package net.nextgen.compat;

import net.minecraft.world.item.ItemStack;

/**
 * Reflection-free (safe) CraftHeraldry item detection.
 * We match by item registry id so this compiles/runs even if CraftHeraldry isn't installed.
 */
public final class CraftHeraldryCompat {
    private CraftHeraldryCompat() {}

    /** Returns true if the stack is the CraftHeraldry scroll item. */
    public static boolean isScroll(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = stack.getItem().builtInRegistryHolder().key().location().toString();
        // Common id in CraftHeraldry20 ports:
        return "craftheraldry:scroll".equals(id);
    }
}
