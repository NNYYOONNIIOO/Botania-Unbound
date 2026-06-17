package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.entity.EntityManaBurst;
import vazkii.botania.common.entity.EntityManaStorm;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.lens.ItemLens;

@Mixin(value = EntityManaStorm.class, remap = false)
public abstract class MixinEntityManaStorm extends net.minecraft.entity.Entity {

    @Shadow public int liveTime;
    @Shadow public int burstsFired;
    @Shadow public int deathTime;

    public MixinEntityManaStorm(World world) {
        super(world);
    }

    @Inject(method = "func_70071_h_", at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpdateHook(CallbackInfo ci) {
        ci.cancel();
        super.onUpdate();
        liveTime++;

        int totalBursts = ModConfig.manaStorm.totalBursts;
        int duration = ModConfig.manaStorm.duration;

        // Calculate how many bursts should have been fired by now (evenly distributed)
        int expectedFired = (int) ((long) totalBursts * liveTime / duration);
        if (expectedFired > totalBursts) expectedFired = totalBursts;

        if (burstsFired < expectedFired) {
            if (!world.isRemote) {
                int toFire = expectedFired - burstsFired;
                for (int i = 0; i < toFire; i++) {
                    EntityManaBurst burst = new EntityManaBurst(world);
                    burst.setPosition(posX, posY, posZ);

                    float motionModifier = 0.5F;
                    burst.setColor(0x20FF20);
                    burst.setMana(120);
                    burst.setStartingMana(340);
                    burst.setMinManaLoss(50);
                    burst.setManaLossPerTick(1F);
                    burst.setGravity(0F);

                    ItemStack lens = new ItemStack(ModItems.lens, 1, ItemLens.STORM);
                    burst.setSourceLens(lens);

                    Vector3 motion = new Vector3(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize().multiply(motionModifier);
                    burst.setMotion(motion.x, motion.y, motion.z);
                    world.spawnEntity(burst);
                }
            }
            burstsFired = expectedFired;
        }

        // Death/decay phase
        if (burstsFired >= totalBursts) {
            deathTime++;
            if (deathTime >= ModConfig.manaStorm.deathTime) {
                setDead();
                if (!ModConfig.manaStorm.disableSelfExplosion) {
                    world.newExplosion(this, posX, posY, posZ, 8F, true, true);
                }
            }
        }
    }
}
