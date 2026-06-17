package nyonio.botania_unbound.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.entity.EntityFallingStar;
import vazkii.botania.common.item.equipment.tool.ItemStarSword;
import vazkii.botania.common.item.equipment.tool.ToolCommons;
import nyonio.botania_unbound.ModConfig;

@Mixin(value = ItemStarSword.class, remap = false)
public class MixinItemStarSword {

    private static final int MANA_PER_DAMAGE = 120;

    private static final String TAG_FIRE_ASPECT_LEVEL = "botania_unbound:fireAspectLevel";
    private static final String TAG_ENCHANTMENT_BONUS = "botania_unbound:enchantmentBonus";
    private static final String TAG_STRENGTH_BONUS = "botania_unbound:strengthBonus";

    /**
     * @reason Override onUpdate to store player attribute data in falling stars
     * @author nyonio
     */
    @Overwrite
    public void func_77663_a(ItemStack par1ItemStack, net.minecraft.world.World world, Entity par3Entity, int par4, boolean par5) {
        if (par3Entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) par3Entity;
            PotionEffect haste = player.getActivePotionEffect(net.minecraft.init.MobEffects.HASTE);
            float check = haste == null ? 0.16666667F : haste.getAmplifier() == 1 ? 0.5F : 0.4F;

            if (player.getHeldItemMainhand() == par1ItemStack && player.swingProgress == check && !world.isRemote) {
                RayTraceResult pos = ToolCommons.raytraceFromEntity(world, par3Entity, true, 48);
                if (pos != null && pos.getBlockPos() != null) {
                    Vector3 posVec = Vector3.fromBlockPos(pos.getBlockPos());
                    Vector3 motVec = new Vector3((0.5 * Math.random() - 0.25) * 18, 24, (0.5 * Math.random() - 0.25) * 18);
                    posVec = posVec.add(motVec);
                    motVec = motVec.normalize().negate().multiply(1.5);

                    EntityFallingStar star = new EntityFallingStar(world, player);
                    star.setPosition(posVec.x, posVec.y, posVec.z);
                    star.motionX = motVec.x;
                    star.motionY = motVec.y;
                    star.motionZ = motVec.z;

                    // Store player attribute data in star's entityData
                    if (ModConfig.starSword.starInheritsEnchantments) {
                        NBTTagCompound entityData = star.getEntityData();
                        // Store enchantment bonus damage
                        float enchBonus = EnchantmentHelper.getModifierForCreature(par1ItemStack, net.minecraft.entity.EnumCreatureAttribute.UNDEFINED);
                        entityData.setFloat(TAG_ENCHANTMENT_BONUS, enchBonus);
                        entityData.setInteger(TAG_FIRE_ASPECT_LEVEL, EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, par1ItemStack));
                        // Store strength potion bonus
                        PotionEffect strength = player.getActivePotionEffect(MobEffects.STRENGTH);
                        float strengthBonus = strength != null ? (strength.getAmplifier() + 1) * 3.0F : 0F;
                        entityData.setFloat(TAG_STRENGTH_BONUS, strengthBonus);

                        System.out.println("[BotaniaUnbound] Star Sword: enchBonus=" + enchBonus + " fireAspect=" + EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, par1ItemStack) + " strengthBonus=" + strengthBonus);
                    }

                    world.spawnEntity(star);

                    if (!world.isRaining()
                            && Math.abs(world.getWorldTime() - 18000) < 1800
                            && Math.random() < 0.125) {
                        EntityFallingStar bonusStar = new EntityFallingStar(world, player);
                        bonusStar.setPosition(posVec.x, posVec.y, posVec.z);
                        bonusStar.motionX = motVec.x + Math.random() - 0.5;
                        bonusStar.motionY = motVec.y + Math.random() - 0.5;
                        bonusStar.motionZ = motVec.z + Math.random() - 0.5;

                        // Store player attribute data in bonus star too
                        if (ModConfig.starSword.starInheritsEnchantments) {
                            NBTTagCompound bonusEntityData = bonusStar.getEntityData();
                            float enchBonus2 = EnchantmentHelper.getModifierForCreature(par1ItemStack, net.minecraft.entity.EnumCreatureAttribute.UNDEFINED);
                            bonusEntityData.setFloat(TAG_ENCHANTMENT_BONUS, enchBonus2);
                            bonusEntityData.setInteger(TAG_FIRE_ASPECT_LEVEL, EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, par1ItemStack));
                            PotionEffect strength2 = player.getActivePotionEffect(MobEffects.STRENGTH);
                            float strengthBonus2 = strength2 != null ? (strength2.getAmplifier() + 1) * 3.0F : 0F;
                            bonusEntityData.setFloat(TAG_STRENGTH_BONUS, strengthBonus2);
                        }

                        world.spawnEntity(bonusStar);
                    }

                    ToolCommons.damageItem(par1ItemStack, 1, player, MANA_PER_DAMAGE);
                    world.playSound(null, player.posX, player.posY, player.posZ, vazkii.botania.common.core.handler.ModSounds.starcaller, net.minecraft.util.SoundCategory.PLAYERS, 0.4F, 1.4F);
                }
            }
        }
    }
}
