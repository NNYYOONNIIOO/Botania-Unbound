package nyonio.botania_unbound.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import nyonio.botania_unbound.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.mana.BlockEnchanter;
import vazkii.botania.common.block.tile.TileEnchanter;
import vazkii.botania.common.item.ModItems;

import java.util.Map;

@Mixin(value = BlockEnchanter.class, remap = false)
public class MixinBlockEnchanter {

    /**
     * @reason Allow book enchanting - modify the stackEnchantable check to accept books
     * @author nyonio
     */
    @Overwrite
    public boolean func_180639_a(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float par7, float par8, float par9) {
        TileEnchanter enchanter = (TileEnchanter) world.getTileEntity(pos);
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty() && stack.getItem() == ModItems.twigWand)
            return false;

        boolean stackEnchantable;
        if (ModConfig.enchanter.allowBookEnchanting && stack.getItem() == Items.BOOK) {
            stackEnchantable = !stack.isEmpty() && stack.getCount() == 1;
        } else {
            stackEnchantable = !stack.isEmpty()
                    && stack.getItem() != Items.BOOK
                    && stack.isItemEnchantable()
                    && stack.getCount() == 1;
        }

        if (enchanter.itemToEnchant.isEmpty()) {
            if (stackEnchantable) {
                enchanter.itemToEnchant = stack.copy();
                player.setHeldItem(hand, ItemStack.EMPTY);
                enchanter.sync();
            } else {
                return false;
            }
        } else if (enchanter.stage == TileEnchanter.State.IDLE) {
            ItemHandlerHelper.giveItemToPlayer(player, enchanter.itemToEnchant.copy());
            enchanter.itemToEnchant = ItemStack.EMPTY;
            enchanter.sync();
        }

        return true;
    }

    /**
     * Inject at HEAD of breakBlock (obfuscated: func_180663_b) with cancellable.
     * If itemToEnchant has enchantments and dropEnchantmentsOnBreak is enabled,
     * split enchantments into books, remove enchantments from item in-place,
     * then cancel the original method and handle everything ourselves.
     * This handles the case where the player breaks the enchanter block directly.
     */
    @Inject(method = "func_180663_b", at = @At("HEAD"), cancellable = true, remap = false)
    private void onBreakBlockHead(World world, BlockPos pos, IBlockState state, CallbackInfo ci) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEnchanter)) {
            return;
        }
        TileEnchanter enchanter = (TileEnchanter) te;

        if (enchanter.itemToEnchant.isEmpty()) {
            world.updateComparatorOutputLevel(pos, state.getBlock());
            ci.cancel();
            return;
        }

        if (!ModConfig.enchanter.dropEnchantmentsOnBreak) {
            world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), enchanter.itemToEnchant));
            enchanter.itemToEnchant = ItemStack.EMPTY;
            world.updateComparatorOutputLevel(pos, state.getBlock());
            ci.cancel();
            return;
        }

        Map<Enchantment, Integer> itemEnchants = EnchantmentHelper.getEnchantments(enchanter.itemToEnchant);

        if (itemEnchants.isEmpty()) {
            // No enchantments on item (enchantment not complete), drop item as-is
            world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), enchanter.itemToEnchant));
            enchanter.itemToEnchant = ItemStack.EMPTY;
            world.updateComparatorOutputLevel(pos, state.getBlock());
            ci.cancel();
            return;
        }

        // Enchantment is complete - item has enchantments applied
        boolean isBook = enchanter.itemToEnchant.getItem() == Items.ENCHANTED_BOOK;

        // Drop every enchantment as a separate enchanted book
        for (Map.Entry<Enchantment, Integer> entry : itemEnchants.entrySet()) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, 1);
            NBTTagList storedEnchantments = new NBTTagList();
            NBTTagCompound enchantTag = new NBTTagCompound();
            enchantTag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            enchantTag.setShort("lvl", (short) (int) entry.getValue());
            storedEnchantments.appendTag(enchantTag);
            NBTTagCompound bookTag = new NBTTagCompound();
            bookTag.setTag("StoredEnchantments", storedEnchantments);
            book.setTagCompound(bookTag);
            world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, book));
        }

        if (!isBook) {
            // Drop unenchanted item (stripped of enchantments)
            ItemStack unenchantedItem = new ItemStack(enchanter.itemToEnchant.getItem(), enchanter.itemToEnchant.getCount(), enchanter.itemToEnchant.getMetadata());
            if (enchanter.itemToEnchant.hasTagCompound()) {
                NBTTagCompound tag = enchanter.itemToEnchant.getTagCompound().copy();
                tag.removeTag("ench");
                tag.removeTag("Enchantments");
                tag.removeTag("RepairCost");
                if (!tag.hasNoTags()) {
                    unenchantedItem.setTagCompound(tag);
                }
            }
            world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, unenchantedItem));
        }
        // For enchanted books: all enchantments already dropped as separate books, don't drop the original

        enchanter.itemToEnchant = ItemStack.EMPTY;
        world.updateComparatorOutputLevel(pos, state.getBlock());
        ci.cancel();
    }
}
