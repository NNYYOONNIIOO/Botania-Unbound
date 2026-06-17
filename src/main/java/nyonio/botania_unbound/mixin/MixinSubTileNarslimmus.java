package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.subtile.generating.SubTileNarslimmus;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(value = SubTileNarslimmus.class, remap = false)
public class MixinSubTileNarslimmus {

    private static Field supertileField;

    /**
     * Inject at the beginning of onUpdate to set TAG_WORLD_SPAWNED for all slimes
     * when acceptAllSlimes config is enabled
     */
    @Inject(
        method = "onUpdate",
        at = @At("HEAD")
    )
    private void injectOnUpdate(CallbackInfo ci) {
        if (ModConfig.narslimmus.acceptAllSlimes) {
            SubTileNarslimmus self = (SubTileNarslimmus) (Object) this;
            try {
                if (supertileField == null) {
                    supertileField = self.getClass().getSuperclass().getSuperclass().getDeclaredField("supertile");
                    supertileField.setAccessible(true);
                }
                TileEntity supertile = (TileEntity) supertileField.get(self);
                int RANGE = 2;
                List<EntitySlime> slimes = supertile.getWorld().getEntitiesWithinAABB(EntitySlime.class, 
                    new AxisAlignedBB(supertile.getPos().add(-RANGE, -RANGE, -RANGE), 
                                      supertile.getPos().add(RANGE + 1, RANGE + 1, RANGE + 1)));
                for (EntitySlime slime : slimes) {
                    if (!slime.isDead) {
                        slime.getEntityData().setBoolean(SubTileNarslimmus.TAG_WORLD_SPAWNED, true);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Cancel onSpawn event handler when config enabled
     * This prevents the original spawn check from interfering
     */
    @Inject(
        method = "onSpawn",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void cancelOnSpawn(LivingSpawnEvent.CheckSpawn event, CallbackInfo ci) {
        if (ModConfig.narslimmus.acceptAllSlimes) {
            // Set the tag for all slimes and cancel original logic
            if (event.getEntityLiving() instanceof EntitySlime) {
                event.getEntityLiving().getEntityData().setBoolean(SubTileNarslimmus.TAG_WORLD_SPAWNED, true);
            }
            ci.cancel();
        }
    }
}