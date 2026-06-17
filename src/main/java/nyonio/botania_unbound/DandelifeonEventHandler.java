package nyonio.botania_unbound;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import nyonio.botania_unbound.DandelifeonState;
import vazkii.botania.api.subtile.SubTileEntity;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.TileSpecialFlower;
import vazkii.botania.common.block.subtile.generating.SubTileDandelifeon;

/**
 * Handles clearing all cells when a cell block is destroyed by non-Dandelifeon operations.
 */
public class DandelifeonEventHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (DandelifeonState.isDandelifeonRemoving) return;
        if (event.getWorld().isRemote) return;

        IBlockState state = event.getState();
        if (state.getBlock() != ModBlocks.cellBlock) return;

        World world = (World) event.getWorld();
        BlockPos brokenPos = event.getPos();

        clearAllCellsNearby(world, brokenPos);
    }

    private void clearAllCellsNearby(World world, BlockPos triggerPos) {
        int searchRange = 12;

        for (int dx = -searchRange; dx <= searchRange; dx++) {
            for (int dz = -searchRange; dz <= searchRange; dz++) {
                BlockPos flowerPos = triggerPos.add(dx, 0, dz);
                TileEntity te = world.getTileEntity(flowerPos);
                if (te instanceof TileSpecialFlower) {
                    TileSpecialFlower flower = (TileSpecialFlower) te;
                    SubTileEntity subTile = flower.getSubTile();
                    if (subTile instanceof SubTileDandelifeon) {
                        clearCellsForDandelifeon(world, flowerPos);
                    }
                }
            }
        }
    }

    private void clearCellsForDandelifeon(World world, BlockPos flowerPos) {
        int range = 12;
        int diam = range * 2 + 1;

        DandelifeonState.isDandelifeonRemoving = true;
        for (int i = 0; i < diam; i++) {
            for (int j = 0; j < diam; j++) {
                BlockPos cellPos = flowerPos.add(-range + i, 0, -range + j);
                IBlockState state = world.getBlockState(cellPos);
                if (state.getBlock() == ModBlocks.cellBlock) {
                    world.setBlockToAir(cellPos);
                }
            }
        }
        DandelifeonState.isDandelifeonRemoving = false;
    }
}
