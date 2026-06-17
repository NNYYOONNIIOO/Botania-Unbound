package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.BlockFluidBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.subtile.generating.SubTileHydroangeas;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mixin(value = SubTileHydroangeas.class, remap = false)
public abstract class MixinSubTileHydroangeas extends SubTileGenerating {

    @Shadow int burnTime, cooldown;

    private static final BlockPos[] OFFSETS = { new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(-1, 0, 1), new BlockPos(-1, 0, -1), new BlockPos(1, 0, 1), new BlockPos(1, 0, -1) };

    /**
     * Overwrite onUpdate to implement Thermalily adjustments:
     * 1. No lava consumption during cooldown (change to normal idle)
     * 2. Skip burn time (generate mana instantly after consuming lava)
     * 3. Ignore cooldown time
     * 
     * @author Nyonio
     * @reason Thermalily adjustments for overpowered gameplay
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        SubTileHydroangeas self = (SubTileHydroangeas)(Object)this;
        boolean isThermalily = self.getMaterialToSearchFor() == Material.LAVA;

        // Handle cooldown - ignore if config enabled
        if (cooldown > 0) {
            if (isThermalily && ModConfig.thermalily.ignoreCooldown) {
                cooldown = 0; // Skip cooldown entirely
            } else {
                cooldown--;
                for(int i = 0; i < 3; i++)
                    Botania.proxy.wispFX(supertile.getPos().getX() + 0.5 + Math.random() * 0.2 - 0.1, supertile.getPos().getY() + 0.5 + Math.random() * 0.2 - 0.1, supertile.getPos().getZ() + 0.5 + Math.random() * 0.2 - 0.1, 0.1F, 0.1F, 0.1F, (float) Math.random() / 6, (float) -Math.random() / 30);
            }
        }

        // Check if we should skip lava consumption during cooldown
        boolean shouldSkipLavaDuringCooldown = isThermalily && ModConfig.thermalily.noLavaDuringCooldown && cooldown > 0;

        if(burnTime == 0 && !shouldSkipLavaDuringCooldown) {
            if(mana < getMaxMana() && !supertile.getWorld().isRemote) {
                List<BlockPos> offsets = Arrays.asList(OFFSETS);
                Collections.shuffle(offsets);

                for(BlockPos offset : offsets) {
                    BlockPos pos = supertile.getPos().add(offset);

                    Material search = self.getMaterialToSearchFor();
                    PropertyInteger prop = supertile.getWorld().getBlockState(pos).getBlock() instanceof BlockLiquid ? BlockLiquid.LEVEL : supertile.getWorld().getBlockState(pos).getBlock() instanceof BlockFluidBase ? BlockFluidBase.LEVEL : null;
                    Block blockBelow = self.getBlockToSearchBelow();
                    if(supertile.getWorld().getBlockState(pos).getMaterial() == search && (blockBelow == null || supertile.getWorld().getBlockState(pos.down()).getBlock() == blockBelow) && (prop == null || supertile.getWorld().getBlockState(pos).getValue(prop) == 0)) {
                        if(search != Material.WATER)
                            supertile.getWorld().setBlockToAir(pos);
                        else {
                            int waterAround = 0;
                            for(EnumFacing dir : EnumFacing.HORIZONTALS)
                                if(supertile.getWorld().getBlockState(pos.offset(dir)).getMaterial() == search)
                                    waterAround++;

                            if(waterAround < 2)
                                supertile.getWorld().setBlockToAir(pos);
                        }

                        // Handle burn time and cooldown
                        if (isThermalily && ModConfig.thermalily.skipBurnTime) {
                            // Skip burn time - generate mana instantly
                            int burnTimeValue = self.getBurnTime();
                            int manaPerTick = self.getValueForPassiveGeneration();
                            int delay = self.getDelayBetweenPassiveGeneration();
                            // Calculate total mana: burnTime / delay * manaPerTick
                            // For Thermalily: 900 / 1 * 20 = 18000 mana
                            int totalMana = (burnTimeValue / delay) * manaPerTick;
                            mana = Math.min(getMaxMana(), mana + totalMana);
                            
                            if (!ModConfig.thermalily.ignoreCooldown) {
                                cooldown = self.getCooldown();
                            }
                            burnTime = 0;
                            sync();
                        } else {
                            if(cooldown == 0)
                                burnTime += self.getBurnTime();
                            else cooldown = self.getCooldown();
                            sync();
                        }

                        self.playSound();
                        break;
                    }
                }
            }
        } else if (burnTime > 0) {
            if(supertile.getWorld().rand.nextInt(8) == 0)
                self.doBurnParticles();
            
            // Normal burn time processing (not skipping)
            if (!(isThermalily && ModConfig.thermalily.skipBurnTime)) {
                burnTime--;
                if(burnTime == 0) {
                    if (!ModConfig.thermalily.ignoreCooldown) {
                        cooldown = self.getCooldown();
                    }
                    sync();
                }
            }
        }
    }
}