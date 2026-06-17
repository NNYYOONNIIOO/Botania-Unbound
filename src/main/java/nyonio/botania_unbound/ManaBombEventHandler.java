package nyonio.botania_unbound;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.entity.EntityManaStorm;

@Mod.EventBusSubscriber(modid = BotaniaUnbound.MODID)
public class ManaBombEventHandler {

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!ModConfig.manaStorm.enableRedstoneActivation) return;
        World world = (World) event.getWorld();
        if (world.isRemote) return;

        BlockPos notifyPos = event.getPos();

        // Check all 6 adjacent positions for mana bombs
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos bombPos = notifyPos.offset(facing);
            IBlockState state = world.getBlockState(bombPos);
            if (state.getBlock() == ModBlocks.manaBomb) {
                if (world.isBlockPowered(bombPos)) {
                    world.playEvent(2001, bombPos, Block.getStateId(state));
                    world.setBlockToAir(bombPos);
                    EntityManaStorm storm = new EntityManaStorm(world);
                    storm.setPosition(bombPos.getX() + 0.5, bombPos.getY() + 0.5, bombPos.getZ() + 0.5);
                    world.spawnEntity(storm);
                }
            }
        }

        // Also check the notify position itself (the block that changed might be a mana bomb)
        IBlockState selfState = world.getBlockState(notifyPos);
        if (selfState.getBlock() == ModBlocks.manaBomb) {
            if (world.isBlockPowered(notifyPos)) {
                world.playEvent(2001, notifyPos, Block.getStateId(selfState));
                world.setBlockToAir(notifyPos);
                EntityManaStorm storm = new EntityManaStorm(world);
                storm.setPosition(notifyPos.getX() + 0.5, notifyPos.getY() + 0.5, notifyPos.getZ() + 0.5);
                world.spawnEntity(storm);
            }
        }
    }
}
