package nyonio.botania_unbound.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.common.Botania;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.item.equipment.tool.ItemThunderSword;
import nyonio.botania_unbound.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

@Mixin(value = ItemThunderSword.class, remap = false)
public class MixinItemThunderSword {

    private static final String TAG_LIGHTNING_SEED = "lightningSeed";

    /**
     * @reason Override hitEntity to inherit player attributes for chain lightning damage,
     * support infinite chain hops, and apply fire aspect enchantment
     * @author nyonio
     */
    @Overwrite
    public boolean func_77644_a(ItemStack stack, EntityLivingBase entity, EntityLivingBase attacker) {
        if (!(entity instanceof EntityPlayer) && entity != null) {
            double range = 8;
            List<EntityLivingBase> alreadyTargetedEntities = new ArrayList<>();

            // Calculate chain lightning damage
            int baseDmg;
            if (ModConfig.thunderSword.lightningInheritsEnchantments && attacker instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) attacker;
                // Base damage from weapon + enchantment bonus + potion effects
                baseDmg = 5; // Original base
                baseDmg += (int) EnchantmentHelper.getModifierForCreature(stack, entity.getCreatureAttribute());
                // Add strength potion bonus
                PotionEffect strength = player.getActivePotionEffect(MobEffects.STRENGTH);
                if (strength != null) {
                    baseDmg += (strength.getAmplifier() + 1) * 3;
                }
                // Subtract weakness potion reduction
                PotionEffect weakness = player.getActivePotionEffect(MobEffects.WEAKNESS);
                if (weakness != null) {
                    baseDmg -= (weakness.getAmplifier() + 1) * 4;
                }

                System.out.println("[BotaniaUnbound] Thunder Sword: baseDmg=" + baseDmg + " enchBonus=" + EnchantmentHelper.getModifierForCreature(stack, entity.getCreatureAttribute()));
            } else {
                baseDmg = 5;
            }

            int dmg = baseDmg;
            long lightningSeed = ItemNBTHelper.getLong(stack, TAG_LIGHTNING_SEED, 0);

            Predicate<Entity> selector = e -> e instanceof EntityLivingBase && e instanceof IMob && !(e instanceof EntityPlayer) && !alreadyTargetedEntities.contains(e);

            Random rand = new Random(lightningSeed);
            EntityLivingBase lightningSource = entity;

            // Determine max hops: infinite if configured, otherwise vanilla behavior
            int maxHops;
            if (ModConfig.thunderSword.infiniteChainLightning) {
                maxHops = Integer.MAX_VALUE;
            } else {
                maxHops = entity.world.isThundering() ? 10 : 4;
            }

            for (int i = 0; i < maxHops; i++) {
                List<Entity> entities = entity.world.getEntitiesInAABBexcluding(lightningSource, new AxisAlignedBB(lightningSource.posX - range, lightningSource.posY - range, lightningSource.posZ - range, lightningSource.posX + range, lightningSource.posY + range, lightningSource.posZ + range), selector::test);
                if (entities.isEmpty())
                    break;

                EntityLivingBase target = (EntityLivingBase) entities.get(rand.nextInt(entities.size()));
                if (dmg > 0) {
                    if (attacker instanceof EntityPlayer)
                        target.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), dmg);
                    else target.attackEntityFrom(DamageSource.causeMobDamage(attacker), dmg);
                }

                // Apply fire aspect to chain lightning targets
                if (ModConfig.thunderSword.lightningInheritsEnchantments) {
                    int fireAspectLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
                    if (fireAspectLevel > 0) {
                        target.setFire(fireAspectLevel * 4);
                    }
                }

                Botania.proxy.lightningFX(Vector3.fromEntityCenter(lightningSource), Vector3.fromEntityCenter(target), 1, 0x0179C4, 0xAADFFF);

                alreadyTargetedEntities.add(target);
                lightningSource = target;
                dmg--;

                // Safety: if damage drops to 0, stop chaining
                if (dmg <= 0)
                    break;
            }

            // Apply fire aspect to the initial target
            if (ModConfig.thunderSword.lightningInheritsEnchantments) {
                int fireAspectLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
                if (fireAspectLevel > 0) {
                    entity.setFire(fireAspectLevel * 4);
                }
            }

            if (!entity.world.isRemote)
                ItemNBTHelper.setLong(stack, TAG_LIGHTNING_SEED, entity.world.rand.nextLong());
        }

        // Return true to let the vanilla melee system handle the initial target's damage
        return true;
    }
}
