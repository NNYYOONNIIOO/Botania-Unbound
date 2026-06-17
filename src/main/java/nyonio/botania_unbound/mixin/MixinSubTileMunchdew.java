package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileMunchdew;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.core.helper.ItemNBTHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(value = SubTileMunchdew.class, remap = false)
public abstract class MixinSubTileMunchdew extends SubTileGenerating {

    @Shadow private boolean ateOnce;
    @Shadow private int ticksWithoutEating;
    @Shadow private int cooldown;

    private static final int RANGE = 8;
    private static final int RANGE_Y = 16;

    /**
     * Overwrite onUpdate to remove cooldown when configured.
     * Original behavior: enters 1600-tick cooldown when mana buffer is full
     * or no leaves found for 5 ticks. With noCooldown, just keeps trying.
     *
     * @author Nyonio
     * @reason Munchdew no cooldown adjustment
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        if (getWorld().isRemote)
            return;

        if (cooldown > 0) {
            cooldown--;
            if (!ModConfig.munchdew.noCooldown) {
                ticksWithoutEating = 0;
                ateOnce = false;
                return;
            }
            // With noCooldown, still decrement but don't return - continue to eat leaves
        }

        int manaPerLeaf = 160;
        eatLeaves: {
            if (getMaxMana() - mana >= manaPerLeaf && ticksExisted % 4 == 0) {
                List<BlockPos> coords = new ArrayList<>();
                BlockPos pos = supertile.getPos();

                for (BlockPos pos_ : BlockPos.getAllInBox(pos.add(-RANGE, 0, -RANGE), pos.add(RANGE, RANGE_Y, RANGE))) {
                    if (supertile.getWorld().getBlockState(pos_).getMaterial() == Material.LEAVES) {
                        boolean exposed = false;
                        for (EnumFacing dir : EnumFacing.VALUES) {
                            IBlockState offState = supertile.getWorld().getBlockState(pos_.offset(dir));
                            if (offState.getBlock().isAir(offState, supertile.getWorld(), pos_.offset(dir))) {
                                exposed = true;
                                break;
                            }
                        }

                        if (exposed)
                            coords.add(pos_);
                    }
                }

                if (coords.isEmpty())
                    break eatLeaves;

                Collections.shuffle(coords);
                BlockPos breakCoords = coords.get(0);
                IBlockState state = supertile.getWorld().getBlockState(breakCoords);
                supertile.getWorld().setBlockToAir(breakCoords);
                ticksWithoutEating = 0;
                ateOnce = true;
                if (ConfigHandler.blockBreakParticles)
                    supertile.getWorld().playEvent(2001, breakCoords, Block.getStateId(state));
                mana += manaPerLeaf;
            }
        }

        if (ateOnce && !ModConfig.munchdew.noCooldown) {
            ticksWithoutEating++;
            if (ticksWithoutEating >= 5) {
                cooldown = 1600;
                sync();
            }
        }
    }

    // Preserve cooldown persistence through break/place only when noCooldown is off
    @Overwrite
    public List<ItemStack> getDrops(List<ItemStack> list) {
        List<ItemStack> drops = super.getDrops(list);
        if (!ModConfig.munchdew.noCooldown && cooldown > 0)
            ItemNBTHelper.setInt(drops.get(0), "cooldown", cooldown);
        return drops;
    }

    @Overwrite
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        if (!ModConfig.munchdew.noCooldown)
            cooldown = ItemNBTHelper.getInt(stack, "cooldown", 0);
    }
}
