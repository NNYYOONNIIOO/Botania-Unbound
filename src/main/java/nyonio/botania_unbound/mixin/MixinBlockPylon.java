package nyonio.botania_unbound.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PylonVariant;
import vazkii.botania.api.wand.IWandBindable;
import vazkii.botania.api.wand.IWandHUD;
import vazkii.botania.api.wand.IWandable;
import vazkii.botania.common.block.BlockPylon;
import vazkii.botania.common.block.tile.TilePylon;

@Mixin(value = BlockPylon.class, remap = false)
public class MixinBlockPylon implements IWandable, IWandHUD {

    @Override
    public boolean onUsedByWand(EntityPlayer player, ItemStack stack, World world, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUD(Minecraft mc, ScaledResolution res, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof IWandBindable)) return;
        IWandBindable bindable = (IWandBindable) te;
        BlockPos binding = bindable.getBinding();

        IBlockState state = world.getBlockState(pos);
        PylonVariant variant = state.getValue(BotaniaStateProps.PYLON_VARIANT);
        int color;
        switch (variant) {
            case NATURA: color = 0x80FF80; break;
            case GAIA:   color = 0xFF80FF; break;
            default:     color = 0x33FF33; break;
        }

        int centerX = res.getScaledWidth() / 2;
        int startY = res.getScaledHeight() / 2 + 30;

        // Source block: scan down through pylons to find the actual mana receiver
        BlockPos sourcePos = findActualSource(world, pos);
        if (sourcePos != null) {
            IBlockState sourceState = world.getBlockState(sourcePos);
            ItemStack sourceStack = getSourceStack(sourceState);
            if (!sourceStack.isEmpty()) {
                String name = sourceStack.getDisplayName();
                int textWidth = mc.fontRenderer.getStringWidth(name);
                int x = centerX - textWidth / 2 - 8;
                int y = startY;
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(sourceStack, x, y);
                RenderHelper.disableStandardItemLighting();
                mc.fontRenderer.drawStringWithShadow(name, x + 20, y + 5, color);
            }
        }

        // Target block (bound)
        if (binding != null) {
            IBlockState targetState = world.getBlockState(binding);
            ItemStack targetStack = getSourceStack(targetState);
            if (!targetStack.isEmpty()) {
                String name = targetStack.getDisplayName();
                int textWidth = mc.fontRenderer.getStringWidth(name);
                int x = centerX - textWidth / 2 - 8;
                int y = startY + 20;
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(targetStack, x, y);
                RenderHelper.disableStandardItemLighting();
                mc.fontRenderer.drawStringWithShadow(name, x + 20, y + 5, color);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static BlockPos findActualSource(World world, BlockPos pylonPos) {
        BlockPos checkPos = pylonPos.down();
        while (checkPos.getY() >= 0) {
            TileEntity te = world.getTileEntity(checkPos);
            if (te instanceof IManaReceiver) {
                return checkPos;
            } else if (te instanceof TilePylon) {
                checkPos = checkPos.down();
            } else {
                break;
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static ItemStack getSourceStack(IBlockState state) {
        Block block = state.getBlock();
        if (block == null) return ItemStack.EMPTY;
        Item item = Item.getItemFromBlock(block);
        if (item == null) return ItemStack.EMPTY;
        try {
            ItemStack stack = new ItemStack(item, 1, block.getMetaFromState(state));
            if (stack.isEmpty()) {
                stack = new ItemStack(item, 1, 0);
            }
            return stack;
        } catch (Exception e) {
            return new ItemStack(item, 1, 0);
        }
    }
}
