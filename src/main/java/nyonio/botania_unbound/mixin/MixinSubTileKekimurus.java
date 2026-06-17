package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCake;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.BlockAltGrass;
import vazkii.botania.common.block.subtile.generating.SubTileKekimurus;

import java.util.List;

@Mixin(value = SubTileKekimurus.class, remap = false)
public abstract class MixinSubTileKekimurus extends SubTileGenerating {

    private static final int RANGE = 5;
    private static final int MANA_PER_BITE = 1800;

    /**
     * Overwrite onUpdate to implement Kekimurus adjustments:
     * 1. Prioritize higher Y cakes
     * 2. Eat dropped cake items (equivalent to 8 bites)
     * 3. Eat multiple bites per cycle (configurable 1-8)
     * 4. Eat Botania alt grass (equivalent to 1 bite, turns to dirt)
     *
     * @author Nyonio
     * @reason Kekimurus enhancements for overpowered gameplay
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        if (supertile.getWorld().isRemote)
            return;

        if (getMaxMana() - this.mana < MANA_PER_BITE)
            return;

        if (ticksExisted % 80 != 0)
            return;

        // 1. Try to eat placed cake blocks
        for (int i = 0; i < RANGE * 2 + 1; i++) {
            int jStart, jEnd, jStep;
            if (ModConfig.kekimurus.prioritizeHigherY) {
                jStart = RANGE * 2;
                jEnd = -1;
                jStep = -1;
            } else {
                jStart = 0;
                jEnd = RANGE * 2 + 1;
                jStep = 1;
            }

            for (int j = jStart; j != jEnd; j += jStep) {
                for (int k = 0; k < RANGE * 2 + 1; k++) {
                    BlockPos pos = supertile.getPos().add(i - RANGE, j - RANGE, k - RANGE);
                    IBlockState state = supertile.getWorld().getBlockState(pos);
                    Block block = state.getBlock();

                    // Eat cake blocks
                    if (block instanceof BlockCake) {
                        int currentBites = state.getValue(BlockCake.BITES);
                        int remainingBites = 7 - currentBites;
                        int bitesToEat = Math.min(ModConfig.kekimurus.bitesPerCycle, remainingBites);

                        int newBites = currentBites + bitesToEat;
                        if (newBites > 6) {
                            supertile.getWorld().setBlockToAir(pos);
                        } else {
                            supertile.getWorld().setBlockState(pos, state.withProperty(BlockCake.BITES, newBites), 1 | 2);
                        }
                        supertile.getWorld().playEvent(2001, pos, Block.getStateId(state));
                        supertile.getWorld().playSound(null, supertile.getPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS, 1F, 0.5F + (float) Math.random() * 0.5F);
                        this.mana += MANA_PER_BITE * bitesToEat;
                        sync();
                        return;
                    }

                    // Eat Botania alt grass
                    if (ModConfig.kekimurus.eatAltGrass && block instanceof BlockAltGrass) {
                        supertile.getWorld().setBlockState(pos, Blocks.DIRT.getDefaultState());
                        supertile.getWorld().playEvent(2001, pos, Block.getStateId(state));
                        supertile.getWorld().playSound(null, supertile.getPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS, 1F, 0.5F + (float) Math.random() * 0.5F);
                        this.mana += MANA_PER_BITE;
                        sync();
                        return;
                    }
                }
            }
        }

        // 2. Try to eat dropped cake items
        if (ModConfig.kekimurus.eatCakeItems) {
            List<EntityItem> items = supertile.getWorld().getEntitiesWithinAABB(EntityItem.class,
                    new AxisAlignedBB(supertile.getPos().add(-RANGE, -RANGE, -RANGE), supertile.getPos().add(RANGE + 1, RANGE + 1, RANGE + 1)));

            for (EntityItem item : items) {
                if (item.isDead) continue;
                ItemStack stack = item.getItem();
                if (stack.isEmpty()) continue;

                // Check if the item is a vanilla cake or extends BlockCake
                boolean isCake = stack.getItem() == Items.CAKE;
                if (!isCake && stack.getItem() instanceof ItemBlock) {
                    Block itemBlock = ((ItemBlock) stack.getItem()).getBlock();
                    isCake = itemBlock instanceof BlockCake;
                }

                if (isCake) {
                    // Consume one cake item, produce mana equivalent to 7 bites
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        item.setDead();
                    }
                    supertile.getWorld().playSound(null, supertile.getPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS, 1F, 0.5F + (float) Math.random() * 0.5F);
                    this.mana += MANA_PER_BITE * 7;
                    sync();
                    return;
                }
            }
        }
    }
}
