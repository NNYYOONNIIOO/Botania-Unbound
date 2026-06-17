package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileGourmaryllis;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(value = SubTileGourmaryllis.class, remap = false)
public abstract class MixinSubTileGourmaryllis extends SubTileGenerating {

    @Shadow private int cooldown;
    @Shadow private int digestingMana;
    @Shadow private ItemStack lastFood;
    @Shadow private int lastFoodCount;
    @Shadow @Final private static int RANGE;

    private int overrideMaxMana = -1;

    private static final Field ENTITY_ITEM_AGE_FIELD;

    static {
        Field f = null;
        try {
            f = EntityItem.class.getDeclaredField("age");
        } catch (NoSuchFieldException e) {
            try {
                // Try SRG name for 1.12.2
                f = EntityItem.class.getDeclaredField("field_70262_b");
            } catch (NoSuchFieldException e2) {
                // Search all declared fields for the int age field
                for (Field candidate : EntityItem.class.getDeclaredFields()) {
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

    @Inject(method = "getMaxMana", at = @At("HEAD"), cancellable = true)
    private void injectGetMaxMana(CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.gourmaryllis.noNutritionCap && overrideMaxMana > 0) {
            cir.setReturnValue(overrideMaxMana);
        }
    }

    /**
     * Overwrite onUpdate to implement Gourmaryllis adjustments:
     * 1. No diminishing returns - always treat lastFoodCount as 1
     * 2. No chewing time - produce mana immediately after eating
     * 3. No nutrition cap - remove Math.min(12, ...) and auto-expand capacity
     * 4. Don't eat during chewing - don't destroy food items while chewing
     *
     * @author Nyonio
     * @reason Gourmaryllis adjustments for overpowered gameplay
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        if (supertile.getWorld().isRemote)
            return;

        // Recalculate overrideMaxMana from digestingMana (handles world reload)
        if (ModConfig.gourmaryllis.noNutritionCap && digestingMana > 0 && digestingMana > getMaxMana()) {
            overrideMaxMana = digestingMana;
        }

        if (cooldown > -1)
            cooldown--;

        if (digestingMana != 0) {
            int effectiveLastFoodCount = ModConfig.gourmaryllis.noDiminishingReturns ? 1 : lastFoodCount;
            int munchInterval = 2 + (2 * effectiveLastFoodCount);

            if (cooldown == 0) {
                mana = Math.min(getMaxMana(), mana + digestingMana);
                digestingMana = 0;

                float burpPitch = 1 - (effectiveLastFoodCount - 1) * 0.05F;
                getWorld().playSound(null, supertile.getPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.BLOCKS, 1, burpPitch);
                sync();
            } else if (cooldown % munchInterval == 0) {
                getWorld().playSound(null, supertile.getPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS, 0.5f, 1);

                Vec3d offset = getWorld().getBlockState(getPos()).getOffset(getWorld(), getPos()).addVector(0.4, 0.6, 0.4);

                ((WorldServer) supertile.getWorld()).spawnParticle(EnumParticleTypes.ITEM_CRACK, supertile.getPos().getX() + offset.x, supertile.getPos().getY() + offset.y, supertile.getPos().getZ() + offset.z, 10, 0.1D, 0.1D, 0.1D, 0.03D, Item.getIdFromItem(lastFood.getItem()), lastFood.getItemDamage());
            }
        }

        int slowdown = getSlowdownFactor();

        List<EntityItem> items = supertile.getWorld().getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(supertile.getPos().add(-RANGE, -RANGE, -RANGE), supertile.getPos().add(RANGE + 1, RANGE + 1, RANGE + 1)));

        for (EntityItem item : items) {
            ItemStack stack = item.getItem();

            if (!stack.isEmpty() && stack.getItem() instanceof ItemFood && !item.isDead && getItemAge(item) >= slowdown) {
                if (cooldown <= 0) {
                    // Determine lastFoodCount
                    if (ItemHandlerHelper.canItemStacksStack(lastFood, stack)) {
                        if (ModConfig.gourmaryllis.noDiminishingReturns) {
                            lastFoodCount = 1;
                        } else {
                            lastFoodCount++;
                        }
                    } else {
                        lastFood = stack.copy();
                        lastFood.setCount(1);
                        lastFoodCount = 1;
                    }

                    // Calculate nutrition value (remove cap if configured)
                    int val;
                    if (ModConfig.gourmaryllis.noNutritionCap) {
                        val = ((ItemFood) stack.getItem()).getHealAmount(stack);
                    } else {
                        val = Math.min(12, ((ItemFood) stack.getItem()).getHealAmount(stack));
                    }

                    // Calculate mana (use effective lastFoodCount for diminishing returns)
                    int effectiveLastFoodCount = ModConfig.gourmaryllis.noDiminishingReturns ? 1 : lastFoodCount;
                    digestingMana = val * val * 70;
                    digestingMana *= 1F / effectiveLastFoodCount;

                    // Auto-expand capacity if needed
                    if (ModConfig.gourmaryllis.noNutritionCap && digestingMana > getMaxMana()) {
                        overrideMaxMana = digestingMana;
                    }

                    if (ModConfig.gourmaryllis.noChewingTime) {
                        // Skip chewing, produce mana immediately
                        mana = Math.min(getMaxMana(), mana + digestingMana);
                        digestingMana = 0;
                        cooldown = -1;
                    } else {
                        cooldown = val * 10;
                    }

                    item.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.2F, 0.6F);
                    ((WorldServer) supertile.getWorld()).spawnParticle(EnumParticleTypes.ITEM_CRACK, false, item.posX, item.posY, item.posZ, 20, 0.1D, 0.1D, 0.1D, 0.05D, Item.getIdFromItem(stack.getItem()), stack.getItemDamage());
                    sync();
                }

                // Consume one food item from the stack (not the entire stack)
                if (cooldown <= 0 || !ModConfig.gourmaryllis.dontEatDuringChewing) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        item.setDead();
                    }
                }
            }
        }
    }
}
