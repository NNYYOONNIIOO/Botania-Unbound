package nyonio.botania_unbound;

import java.lang.reflect.Field;

/**
 * Compatibility helper for Botania-CEu which moves hardcoded constants to ConfigHandler.genFlowers.
 * Reads CEu's ConfigHandler values once during class initialization, then uses cached values.
 * Falls back to original Botania values when CEu is not present.
 */
public class BotaniaCompat {

    private static final boolean IS_CEU;

    // Cached values - read once via reflection during init, then used directly
    public static final int ENDOFLAME_BURN_TIME;
    public static final float ENDOFLAME_BURN_TIME_MULT;
    public static final int DANDELIFEON_RANGE;
    public static final int DANDELIFEON_SPEED;
    public static final int DANDELIFEON_LIFETIME;
    public static final int DANDELIFEON_MANA;
    public static final int MUNCHDEW_MANA;
    public static final int MUNCHDEW_DELAY;
    public static final int RAFFLOWSIA_MANA;
    public static final int RAFFLOWSIA_DELAY;

    static {
        Object genFlowers = null;
        boolean isCeu = false;
        try {
            Class<?> cls = Class.forName("vazkii.botania.common.core.handler.ConfigHandler");
            Field f = cls.getField("genFlowers");
            genFlowers = f.get(null);
            isCeu = genFlowers != null;
        } catch (Exception ignored) {
        }
        IS_CEU = isCeu;

        // Read all values once during init
        ENDOFLAME_BURN_TIME = readInt(genFlowers, "endoflameBurnTime", 32000);
        ENDOFLAME_BURN_TIME_MULT = readFloat(genFlowers, "endoflameBurnTimeMult", 0.5F);
        DANDELIFEON_RANGE = readInt(genFlowers, "dandelifeonRange", 12);
        DANDELIFEON_SPEED = readInt(genFlowers, "dandelifeonSpeed", 10);
        DANDELIFEON_LIFETIME = readInt(genFlowers, "dandelifeonLifetime", 100);
        DANDELIFEON_MANA = readInt(genFlowers, "dandelifeonMana", 60);
        MUNCHDEW_MANA = readInt(genFlowers, "munchdewMana", 160);
        MUNCHDEW_DELAY = readInt(genFlowers, "munchdewDelay", 4);
        RAFFLOWSIA_MANA = readInt(genFlowers, "rafflowsiaMana", 2100);
        RAFFLOWSIA_DELAY = readInt(genFlowers, "rafflowsiaDelay", 40);
    }

    private static int readInt(Object obj, String fieldName, int defaultValue) {
        if (obj == null) return defaultValue;
        try {
            return obj.getClass().getField(fieldName).getInt(obj);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static float readFloat(Object obj, String fieldName, float defaultValue) {
        if (obj == null) return defaultValue;
        try {
            return obj.getClass().getField(fieldName).getFloat(obj);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static boolean isCeu() {
        return IS_CEU;
    }
}
