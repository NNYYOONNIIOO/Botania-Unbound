package nyonio.botania_unbound.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nyonio.botania_unbound.ModConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.TileEnchanter;
import vazkii.botania.common.block.tile.TilePylon;
import vazkii.botania.common.core.handler.ModSounds;
import vazkii.botania.common.network.PacketBotaniaEffect;
import vazkii.botania.common.network.PacketHandler;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mixin(value = TileEnchanter.class, remap = false)
public abstract class MixinTileEnchanter {

    @Shadow(remap = false) public ItemStack itemToEnchant;
    @Shadow(remap = false) @Final private List<EnchantmentData> enchants;
    @Shadow(remap = false) public TileEnchanter.State stage;
    @Shadow(remap = false) private int stageTicks;
    @Shadow(remap = false) private int manaRequired;
    @Shadow(remap = false) private int mana;
    @Shadow(remap = false) private static final int CRAFT_EFFECT_EVENT = 0;
    @Shadow(remap = false) protected abstract boolean hasEnchantAlready(Enchantment enchant);
    @Shadow(remap = false) protected abstract void advanceStage();
    @Shadow(remap = false) protected abstract void sync();
    @Shadow(remap = false) private void gatherMana(EnumFacing.Axis axis) {}
    @Shadow(remap = false) private static boolean canEnchanterExist(World world, BlockPos pos, EnumFacing.Axis axis) { return false; }

    // Define PYLON_LOCATIONS directly (same as in TileEnchanter)
    private static final Map<EnumFacing.Axis, BlockPos[]> PYLON_LOCATIONS = new EnumMap<>(EnumFacing.Axis.class);
    static {
        PYLON_LOCATIONS.put(EnumFacing.Axis.X, new BlockPos[] { new BlockPos(-5, 1, 0), new BlockPos(5, 1, 0), new BlockPos(-4, 1, 3), new BlockPos(4, 1, 3), new BlockPos(-4, 1, -3 ), new BlockPos(4, 1, -3) });
        PYLON_LOCATIONS.put(EnumFacing.Axis.Z, new BlockPos[] { new BlockPos(0, 1, -5), new BlockPos(0, 1, 5), new BlockPos(3, 1, -4), new BlockPos(3, 1, 4), new BlockPos(-3, 1, -4 ), new BlockPos(-3, 1, 4) });
    }

    /**
     * @reason Allow book enchanting and handle book to enchanted book conversion
     * @author nyonio
     * Note: In runtime, 'update' is obfuscated to 'func_73660_a'
     */
    @Overwrite
    public void func_73660_a() {
        TileEnchanter self = (TileEnchanter) (Object) this;
        World world = ((TileEntity) self).getWorld();
        BlockPos pos = ((TileEntity) self).getPos();
        
        IBlockState state = world.getBlockState(pos);
        EnumFacing.Axis axis = state.getValue(BotaniaStateProps.ENCHANTER_DIRECTION);

        for (BlockPos pylon : PYLON_LOCATIONS.get(axis)) {
            TileEntity tile = world.getTileEntity(pos.add(pylon));
            if (tile instanceof TilePylon) {
                TilePylonAccessor pylonTile = (TilePylonAccessor) tile;
                pylonTile.setActivated(stage == TileEnchanter.State.GATHER_MANA);
                if (stage == TileEnchanter.State.GATHER_MANA)
                    pylonTile.setCenterPos(pos);
            }
        }

        if (stage != TileEnchanter.State.IDLE)
            stageTicks++;

        if (world.isRemote)
            return;

        if (!canEnchanterExist(world, pos, axis)) {
            // When multiblock is broken:
            // - If enchantment is complete (item has enchantments), drop enchantments as books + unenchanted item
            // - If enchantment is NOT complete, just drop the original item as-is
            if (!itemToEnchant.isEmpty()) {
                if (ModConfig.enchanter.dropEnchantmentsOnBreak) {
                    Map<Enchantment, Integer> itemEnchants = EnchantmentHelper.getEnchantments(itemToEnchant);
                    if (!itemEnchants.isEmpty()) {
                        // Enchantment is complete - item has enchantments applied
                        boolean isBook = itemToEnchant.getItem() == Items.ENCHANTED_BOOK;

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
                            ItemStack unenchantedItem = new ItemStack(itemToEnchant.getItem(), itemToEnchant.getCount(), itemToEnchant.getMetadata());
                            if (itemToEnchant.hasTagCompound()) {
                                NBTTagCompound tag = itemToEnchant.getTagCompound().copy();
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
                    } else {
                        // No enchantments on item - enchantment not complete, drop item as-is
                        world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemToEnchant.copy()));
                    }
                } else {
                    // dropEnchantmentsOnBreak disabled, drop item as-is
                    world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemToEnchant.copy()));
                }
                itemToEnchant = ItemStack.EMPTY;
            }

            // Note: do NOT drop enchantments from the pending 'enchants' list.
            // Only completed enchantments (already on the item) should be dropped as books.
            // If the enchantment process was not complete, the pending enchantments are lost.

            world.setBlockState(pos, Blocks.LAPIS_BLOCK.getDefaultState(), 1 | 2);
            PacketHandler.sendToNearby(world, pos, new PacketBotaniaEffect(PacketBotaniaEffect.EffectType.ENCHANTER_DESTROY,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            world.playSound(null, pos, ModSounds.enchanterFade, SoundCategory.BLOCKS, 0.5F, 10F);
        }

        switch (stage) {
            case GATHER_ENCHANTS:
                gatherEnchants();
                break;
            case GATHER_MANA:
                gatherMana(axis);
                break;
            case DO_ENCHANT: {
                if (stageTicks >= 100) {
                    // Handle book to enchanted book conversion
                    if (ModConfig.enchanter.allowBookEnchanting && itemToEnchant.getItem() == Items.BOOK) {
                        // Convert book to enchanted book before applying enchantments
                        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK, 1);
                        NBTTagList storedEnchantments = new NBTTagList();
                        
                        for (EnchantmentData data : enchants) {
                            NBTTagCompound enchantTag = new NBTTagCompound();
                            enchantTag.setShort("id", (short) Enchantment.getEnchantmentID(data.enchantment));
                            enchantTag.setShort("lvl", (short) data.enchantmentLevel);
                            storedEnchantments.appendTag(enchantTag);
                        }
                        
                        NBTTagCompound bookTag = new NBTTagCompound();
                        bookTag.setTag("StoredEnchantments", storedEnchantments);
                        enchantedBook.setTagCompound(bookTag);
                        
                        itemToEnchant = enchantedBook;
                    } else {
                        // Original logic: apply enchantments to item
                        for (EnchantmentData data : enchants)
                            if (EnchantmentHelper.getEnchantmentLevel(data.enchantment, itemToEnchant) == 0)
                                itemToEnchant.addEnchantment(data.enchantment, data.enchantmentLevel);
                    }

                    enchants.clear();
                    manaRequired = -1;
                    mana = 0;

                    world.addBlockEvent(pos, ModBlocks.enchanter, CRAFT_EFFECT_EVENT, 0);
                    advanceStage();
                }
                break;
            }
            case RESET: {
                if (stageTicks >= 20)
                    advanceStage();
                break;
            }
            default:
                break;
        }
    }

    /**
     * @reason Gather all enchantments from books and enchanted items
     * @author nyonio
     */
    @Overwrite
    private void gatherEnchants() {
        TileEnchanter self = (TileEnchanter) (Object) this;
        World world = ((TileEntity) self).getWorld();
        BlockPos pos = ((TileEntity) self).getPos();

        if (!world.isRemote && stageTicks % 20 == 0) {
            List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class,
                new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 1, pos.getZ() + 3));
            boolean addedEnch = false;

            for (EntityItem entity : items) {
                ItemStack item = entity.getItem();

                // Check if this is an enchanted book
                boolean isEnchantedBook = item.getItem() == Items.ENCHANTED_BOOK;
                
                // Check if this is an enchanted item (not a book) when config is enabled
                boolean isEnchantedItem = ModConfig.enchanter.readFromEnchantedItems && !isEnchantedBook;

                // Skip if not an enchanted book and not enabled for enchanted items
                if (!isEnchantedBook && !isEnchantedItem) continue;

                // Use EnchantmentHelper to get all enchantments (works for both books and items)
                Map<Enchantment, Integer> itemEnchants = EnchantmentHelper.getEnchantments(item);
                if (itemEnchants.isEmpty()) continue;

                boolean hasEnchantsThisItem = false;
                for (Map.Entry<Enchantment, Integer> entry : itemEnchants.entrySet()) {
                    Enchantment ench = entry.getKey();
                    int lvl = entry.getValue();
                    if (ench == null || hasEnchantAlready(ench) || !isEnchantmentValid(ench)) continue;
                    this.enchants.add(new EnchantmentData(ench, lvl));
                    hasEnchantsThisItem = true;
                }

                if (hasEnchantsThisItem) {
                    world.playSound(null, pos, ModSounds.ding, SoundCategory.BLOCKS, 1F, 1F);
                    addedEnch = true;
                    break;
                }
            }

            if (!addedEnch) {
                if (enchants.isEmpty()) {
                    stage = TileEnchanter.State.IDLE;
                } else {
                    advanceStage();
                }
            }
        }
    }

    /**
     * @reason Allow conflicting enchantments and book enchanting
     * @author nyonio
     */
    @Overwrite
    private boolean isEnchantmentValid(@Nullable Enchantment ench) {
        if (ench == null) {
            return false;
        }

        // Allow books to receive any enchantment
        if (ModConfig.enchanter.allowBookEnchanting && itemToEnchant.getItem() == Items.BOOK) {
            return !hasEnchantAlready(ench);
        }

        // Skip compatibility check when allowing conflicts or reading from enchanted items
        if (ModConfig.enchanter.allowConflictingEnchantments || ModConfig.enchanter.readFromEnchantedItems) {
            // Still check if enchantment can apply to item (unless reading from enchanted items)
            if (!ModConfig.enchanter.readFromEnchantedItems && !ench.canApply(itemToEnchant)) {
                return false;
            }
            return !hasEnchantAlready(ench);
        }

        // Check if enchantment can apply to item
        if (!ench.canApply(itemToEnchant)) {
            return false;
        }

        // Original compatibility check
        for (EnchantmentData data : enchants) {
            Enchantment otherEnch = data.enchantment;
            if (!ench.isCompatibleWith(otherEnch)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @reason Allow book enchanting and reading from enchanted items - override wand activation check
     * @author nyonio
     */
    @Overwrite
    public void onWanded(EntityPlayer player, ItemStack wand) {
        TileEnchanter self = (TileEnchanter) (Object) this;
        World world = ((TileEntity) self).getWorld();
        BlockPos pos = ((TileEntity) self).getPos();

        // Modified check: allow books when config is enabled
        boolean itemValid;
        if (ModConfig.enchanter.allowBookEnchanting && itemToEnchant.getItem() == Items.BOOK) {
            itemValid = true;
        } else {
            itemValid = !itemToEnchant.isEmpty() && itemToEnchant.isItemEnchantable();
        }

        if (stage != TileEnchanter.State.IDLE || !itemValid)
            return;

        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class,
            new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 1, pos.getZ() + 3));

        if (!items.isEmpty() && !world.isRemote) {
            for (EntityItem entity : items) {
                ItemStack item = entity.getItem();

                // Check enchanted books (original behavior)
                if (item.getItem() == Items.ENCHANTED_BOOK) {
                    NBTTagList enchants = ItemEnchantedBook.getEnchantments(item);
                    if (enchants.tagCount() > 0) {
                        NBTTagCompound enchant = enchants.getCompoundTagAt(0);
                        short id = enchant.getShort("id");
                        if (isEnchantmentValid(Enchantment.getEnchantmentByID(id))) {
                            advanceStage();
                            return;
                        }
                    }
                }

                // Check enchanted items (new behavior)
                if (ModConfig.enchanter.readFromEnchantedItems && item.isItemEnchanted() && item.getItem() != Items.ENCHANTED_BOOK) {
                    Map<Enchantment, Integer> itemEnchants = EnchantmentHelper.getEnchantments(item);
                    for (Map.Entry<Enchantment, Integer> entry : itemEnchants.entrySet()) {
                        Enchantment ench = entry.getKey();
                        if (ench != null && isEnchantmentValid(ench)) {
                            advanceStage();
                            return;
                        }
                    }
                }
            }
        }
    }
}