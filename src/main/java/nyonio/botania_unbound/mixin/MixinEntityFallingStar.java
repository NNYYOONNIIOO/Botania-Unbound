package nyonio.botania_unbound.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.entity.EntityFallingStar;
import nyonio.botania_unbound.ModConfig;

@Mixin(value = EntityFallingStar.class, remap = false)
public abstract class MixinEntityFallingStar {

    private static final String TAG_FIRE_ASPECT_LEVEL = "botania_unbound:fireAspectLevel";
    private static final String TAG_ENCHANTMENT_BONUS = "botania_unbound:enchantmentBonus";
    private static final String TAG_STRENGTH_BONUS = "botania_unbound:strengthBonus";

    /**
     * @reason Override onImpact to inherit player attributes for damage and fire aspect,
     * and prevent damage to non-living entities
     * @author nyonio
     */
    @Overwrite
    protected void onImpact(RayTraceResult pos) {
        EntityFallingStar self = (EntityFallingStar) (Object) this;

        if (self.world.isRemote)
            return;

        EntityLivingBase thrower = self.getThrower();

        if (pos.entityHit != null && thrower != null && pos.entityHit != thrower && !pos.entityHit.isDead) {
            // Prevent damage to non-living entities (e.g., item frames, paintings)
            if (ModConfig.starSword.starOnlyDamagesLiving && !(pos.entityHit instanceof EntityLivingBase)) {
                self.setDead();
                return;
            }

            // Calculate damage
            float baseDamage;
            NBTTagCompound entityData = self.getEntityData();
            if (ModConfig.starSword.starInheritsEnchantments) {
                // Original base damage (5 or 10 with 25% chance)
                baseDamage = Math.random() < 0.25 ? 10F : 5F;
                // Add enchantment bonus
                if (entityData.hasKey(TAG_ENCHANTMENT_BONUS)) {
                    baseDamage += entityData.getFloat(TAG_ENCHANTMENT_BONUS);
                }
                // Add strength potion bonus
                if (entityData.hasKey(TAG_STRENGTH_BONUS)) {
                    baseDamage += entityData.getFloat(TAG_STRENGTH_BONUS);
                }
            } else {
                baseDamage = Math.random() < 0.25 ? 10F : 5F;
            }

            if (thrower instanceof EntityPlayer)
                pos.entityHit.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) thrower), baseDamage);
            else pos.entityHit.attackEntityFrom(DamageSource.GENERIC, baseDamage);

            // Apply fire aspect
            if (ModConfig.starSword.starInheritsEnchantments && pos.entityHit instanceof EntityLivingBase) {
                EntityLivingBase livingTarget = (EntityLivingBase) pos.entityHit;
                int fireAspectLevel = entityData.getInteger(TAG_FIRE_ASPECT_LEVEL);
                if (fireAspectLevel > 0) {
                    livingTarget.setFire(fireAspectLevel * 4);
                }
            }
        }

        if (pos.getBlockPos() != null) {
            IBlockState state = self.world.getBlockState(pos.getBlockPos());
            if (ConfigHandler.blockBreakParticles && !state.getBlock().isAir(state, self.world, pos.getBlockPos()))
                self.world.playEvent(2001, pos.getBlockPos(), Block.getStateId(state));
        }

        self.setDead();
    }
}
