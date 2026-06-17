package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.common.item.lens.LensStorm;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;

@Mixin(value = LensStorm.class, remap = false)
public class MixinLensStorm {

    /**
     * Overwrite collideBurst to support disabling ray explosion.
     */
    @Overwrite
    public boolean collideBurst(IManaBurst burst, EntityThrowable entity, RayTraceResult pos, boolean isManaBlock, boolean dead, ItemStack stack) {
        if (!entity.world.isRemote && !burst.isFake()) {
            BlockPos coords = burst.getBurstSourceBlockPos();
            if (pos.entityHit == null && !isManaBlock && (pos.getBlockPos() == null || !coords.equals(pos.getBlockPos()))) {
                if (!ModConfig.manaStorm.disableRayExplosion) {
                    entity.world.createExplosion(entity, entity.posX, entity.posY, entity.posZ, 5F, true);
                }
            }
        } else {
            dead = false;
        }
        return dead;
    }
}
