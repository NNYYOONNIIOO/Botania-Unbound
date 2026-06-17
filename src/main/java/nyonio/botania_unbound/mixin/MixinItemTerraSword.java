package nyonio.botania_unbound.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraSword;
import nyonio.botania_unbound.ModConfig;

import java.util.List;

@Mixin(value = ItemTerraSword.class, remap = false)
public class MixinItemTerraSword {

    private static final String TAG_ATTACKER_USERNAME = "attackerUsername";
    private static final int MANA_PER_DAMAGE = 100;

    /**
     * @reason Override updateBurst to inherit player attributes and apply fire aspect
     * @author nyonio
     */
    @Overwrite
    public void updateBurst(IManaBurst burst, ItemStack stack) {
        EntityThrowable entity = (EntityThrowable) burst;
        AxisAlignedBB axis = new AxisAlignedBB(entity.posX, entity.posY, entity.posZ, entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ).grow(1);
        List<EntityLivingBase> entities = entity.world.getEntitiesWithinAABB(EntityLivingBase.class, axis);
        String attacker = ItemNBTHelper.getString(burst.getSourceLens(), TAG_ATTACKER_USERNAME, "");

        for (EntityLivingBase living : entities) {
            if (living instanceof EntityPlayer && (living.getName().equals(attacker) || net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance() != null && !net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled()))
                continue;

            if (living.hurtTime == 0) {
                int cost = MANA_PER_DAMAGE / 3;
                int mana = burst.getMana();
                if (mana >= cost) {
                    burst.setMana(mana - cost);
                    if (!burst.isFake() && !entity.world.isRemote) {
                        EntityPlayer player = living.world.getPlayerEntityByName(attacker);

                        float damage;
                        if (ModConfig.terraBlade.beamInheritsEnchantments) {
                            // Base beam damage (same as original)
                            damage = 4F + BotaniaAPI.terrasteelToolMaterial.getAttackDamage();

                            ItemStack sourceLens = burst.getSourceLens();
                            if (sourceLens != null && !sourceLens.isEmpty()) {
                                // Add enchantment bonus damage (Sharpness, Smite, Bane of Arthropods)
                                damage += EnchantmentHelper.getModifierForCreature(sourceLens, living.getCreatureAttribute());
                            }

                            // Add player potion effects
                            if (player != null) {
                                PotionEffect strength = player.getActivePotionEffect(MobEffects.STRENGTH);
                                if (strength != null) {
                                    damage += (strength.getAmplifier() + 1) * 3.0F;
                                }
                                PotionEffect weakness = player.getActivePotionEffect(MobEffects.WEAKNESS);
                                if (weakness != null) {
                                    damage -= (weakness.getAmplifier() + 1) * 4.0F;
                                }
                            }

                            System.out.println("[BotaniaUnbound] Terra Blade: damage=" + damage + " player=" + (player != null ? player.getName() : "null"));
                        } else {
                            damage = 4F + BotaniaAPI.terrasteelToolMaterial.getAttackDamage();
                        }

                        if (damage > 0) {
                            living.attackEntityFrom(player == null ? DamageSource.MAGIC : DamageSource.causePlayerDamage(player), damage);
                        }

                        // Apply fire aspect (part of beamInheritsEnchantments)
                        if (ModConfig.terraBlade.beamInheritsEnchantments) {
                            ItemStack sourceLens = burst.getSourceLens();
                            if (sourceLens != null && !sourceLens.isEmpty()) {
                                int fireAspectLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, sourceLens);
                                if (fireAspectLevel > 0) {
                                    living.setFire(fireAspectLevel * 4);
                                }
                            }
                        }

                        entity.setDead();
                        break;
                    }
                }
            }
        }
    }
}
