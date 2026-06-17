package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileSpectrolus;

import java.util.List;

@Mixin(value = SubTileSpectrolus.class, remap = false)
public abstract class MixinSubTileSpectrolus extends SubTileGenerating {

    @Shadow private int nextColor;

    private static final int RANGE = 1;

    /**
     * Overwrite onUpdate to implement Spectrolus adjustments:
     * 1. Accept any color wool (not just the next expected color)
     * 2. Don't eat wrong color wool (prevent waste)
     *
     * @author Nyonio
     * @reason Spectrolus color flexibility adjustments
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        if (supertile.getWorld().isRemote)
            return;

        Item wool = Item.getItemFromBlock(Blocks.WOOL);

        List<EntityItem> items = supertile.getWorld().getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(supertile.getPos().add(-RANGE, -RANGE, -RANGE), supertile.getPos().add(RANGE + 1, RANGE + 1, RANGE + 1)));
        int slowdown = getSlowdownFactor();

        for (EntityItem item : items) {
            ItemStack stack = item.getItem();

            if (!stack.isEmpty() && stack.getItem() == wool && !item.isDead && getItemAge(item) >= slowdown) {
                int meta = stack.getItemDamage();

                if (ModConfig.spectrolus.acceptAnyColor) {
                    // Accept any color wool
                    mana = Math.min(getMaxMana(), mana + 2400);
                    nextColor = nextColor == 15 ? 0 : nextColor + 1;
                    sync();

                    ((WorldServer) supertile.getWorld()).spawnParticle(EnumParticleTypes.ITEM_CRACK, false, item.posX, item.posY, item.posZ, 20, 0.1D, 0.1D, 0.1D, 0.05D, Item.getIdFromItem(stack.getItem()), stack.getItemDamage());

                    // Consume one wool from stack
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        item.setDead();
                    }
                } else {
                    // Original behavior: only accept matching color
                    if (meta == nextColor) {
                        mana = Math.min(getMaxMana(), mana + 2400);
                        nextColor = nextColor == 15 ? 0 : nextColor + 1;
                        sync();

                        ((WorldServer) supertile.getWorld()).spawnParticle(EnumParticleTypes.ITEM_CRACK, false, item.posX, item.posY, item.posZ, 20, 0.1D, 0.1D, 0.1D, 0.05D, Item.getIdFromItem(stack.getItem()), stack.getItemDamage());
                    }

                    // Only destroy wrong-color wool if dontEatWrongColor is disabled
                    if (meta == nextColor || !ModConfig.spectrolus.dontEatWrongColor) {
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            item.setDead();
                        }
                    }
                }
            }
        }
    }

    private static final java.lang.reflect.Field ENTITY_ITEM_AGE_FIELD;

    static {
        java.lang.reflect.Field f = null;
        try {
            f = EntityItem.class.getDeclaredField("age");
        } catch (NoSuchFieldException e) {
            try {
                f = EntityItem.class.getDeclaredField("field_70262_b");
            } catch (NoSuchFieldException e2) {
                for (java.lang.reflect.Field candidate : EntityItem.class.getDeclaredFields()) {
                    if (candidate.getType() == int.class) {
                        f = candidate;
                        break;
                    }
                }
            }
        }
        if (f != null) {
            f.setAccessible(true);
        }
        ENTITY_ITEM_AGE_FIELD = f;
    }

    private static int getItemAge(EntityItem item) {
        if (ENTITY_ITEM_AGE_FIELD != null) {
            try {
                return ENTITY_ITEM_AGE_FIELD.getInt(item);
            } catch (IllegalAccessException ignored) {}
        }
        return Integer.MAX_VALUE;
    }
}
