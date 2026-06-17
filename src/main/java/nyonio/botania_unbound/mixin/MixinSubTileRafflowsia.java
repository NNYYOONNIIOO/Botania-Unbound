package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.subtile.ISubTileContainer;
import vazkii.botania.api.subtile.SubTileEntity;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.subtile.generating.SubTileRafflowsia;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.item.block.ItemBlockSpecialFlower;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(value = SubTileRafflowsia.class, remap = false)
public abstract class MixinSubTileRafflowsia extends SubTileGenerating {

    @Shadow private String lastFlower;
    @Shadow private int lastFlowerTimes;

    private static final int RANGE = 5;
    private int overrideMaxMana = -1;

    // New formula: track unique flower types
    private Set<String> uniqueFlowerTypes = new HashSet<>();
    private int uniqueFlowerCount = 0;

    @Override
    public int getMaxMana() {
        if (ModConfig.rafflowsia.autoExpandCapacity && overrideMaxMana > 0) {
            return overrideMaxMana;
        }
        return 9000;
    }

    /**
     * Calculate mana using the new formula:
     * mana = round(-401.45 + 7.03436*x + 16.0932*x^2 + 7.64878*1.25226^x, 100)
     * where x = number of unique flower types eaten
     */
    private int calculateManaNewFormula(int x) {
        if (x <= 0) return 0;
        double raw = -401.45 + 7.03436 * x + 16.0932 * x * x + 7.64878 * Math.pow(1.25226, x);
        // Round to nearest 100
        return Math.max(0, (int) (Math.round(raw / 100.0) * 100));
    }

    /**
     * Calculate mana using original formula with diminishing returns
     */
    private int calculateManaOriginal() {
        float mod = ModConfig.rafflowsia.noDiminishingReturns ? 1F : 1F / lastFlowerTimes;
        return (int) (2100 * mod);
    }

    private int calculateMana(String flowerName) {
        if (ModConfig.rafflowsia.newManaFormula) {
            boolean isNewType = !uniqueFlowerTypes.contains(flowerName);
            if (isNewType || ModConfig.rafflowsia.noDiminishingReturns) {
                if (isNewType) {
                    uniqueFlowerTypes.add(flowerName);
                }
                uniqueFlowerCount++;
            }
            return calculateManaNewFormula(uniqueFlowerCount);
        } else {
            updateLastFlowerOriginal(flowerName);
            return calculateManaOriginal();
        }
    }

    private void updateLastFlowerOriginal(String name) {
        if (name.equals(lastFlower)) {
            if (ModConfig.rafflowsia.noDiminishingReturns) {
                lastFlowerTimes = 1;
            } else {
                lastFlowerTimes++;
            }
        } else {
            lastFlower = name;
            lastFlowerTimes = 1;
        }
    }

    /**
     * Overwrite onUpdate to implement Rafflowsia adjustments:
     * 1. No diminishing returns for same flower type
     * 2. Can consume Botania flower items (dropped items)
     * 3. Auto-expand mana buffer capacity instead of stopping
     * 4. New mana formula based on unique flower count
     *
     * @author Nyonio
     * @reason Rafflowsia adjustments for overpowered gameplay
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        int baseMana = ModConfig.rafflowsia.newManaFormula ? 0 : 2100;

        if (!supertile.getWorld().isRemote && ticksExisted % 40 == 0) {
            // Check capacity - auto expand if configured
            if (ModConfig.rafflowsia.autoExpandCapacity || getMaxMana() - this.mana >= baseMana) {
                // First: try to eat placed flower blocks (original behavior)
                for (int i = 0; i < RANGE * 2 + 1; i++)
                    for (int j = 0; j < RANGE * 2 + 1; j++)
                        for (int k = 0; k < RANGE * 2 + 1; k++) {
                            BlockPos pos = supertile.getPos().add(i - RANGE, j - RANGE, k - RANGE);

                            TileEntity tile = supertile.getWorld().getTileEntity(pos);
                            if (tile instanceof ISubTileContainer) {
                                SubTileEntity stile = ((ISubTileContainer) tile).getSubTile();
                                String name = stile.getUnlocalizedName();

                                if (!(stile instanceof SubTileRafflowsia)) {
                                    int manaToAdd = calculateMana(name);

                                    getWorld().destroyBlock(pos, false);
                                    this.mana += manaToAdd;
                                    if (ModConfig.rafflowsia.autoExpandCapacity && this.mana > overrideMaxMana) {
                                        overrideMaxMana = this.mana;
                                    }
                                    sync();
                                    return;
                                }
                            }
                        }

                // Second: try to eat flower items on the ground (new behavior)
                if (ModConfig.rafflowsia.eatFlowerItems) {
                    Item specialFlowerItem = Item.getItemFromBlock(ModBlocks.specialFlower);
                    Item floatingFlowerItem = Item.getItemFromBlock(ModBlocks.floatingSpecialFlower);

                    List<EntityItem> items = supertile.getWorld().getEntitiesWithinAABB(EntityItem.class,
                            new AxisAlignedBB(supertile.getPos().add(-RANGE, -RANGE, -RANGE), supertile.getPos().add(RANGE + 1, RANGE + 1, RANGE + 1)));

                    for (EntityItem item : items) {
                        if (item.isDead) continue;
                        ItemStack stack = item.getItem();
                        if (stack.isEmpty()) continue;

                        boolean isSpecialFlower = stack.getItem() == specialFlowerItem;
                        boolean isFloatingFlower = stack.getItem() == floatingFlowerItem;

                        if (isSpecialFlower || isFloatingFlower) {
                            String type = ItemBlockSpecialFlower.getType(stack);
                            if (type.isEmpty()) continue;

                            // Don't eat rafflowsia items
                            if (type.equals("rafflowsia")) continue;

                            // Get the subtile class to get the unlocalizedName
                            Class<? extends SubTileEntity> subtileClass = BotaniaAPI.getSubTileMapping(type);
                            if (subtileClass == null) continue;

                            try {
                                SubTileEntity tempInstance = subtileClass.newInstance();
                                String name = tempInstance.getUnlocalizedName();

                                int manaToAdd = calculateMana(name);

                                // Consume one item from stack
                                stack.shrink(1);
                                if (stack.isEmpty()) {
                                    item.setDead();
                                }

                                this.mana += manaToAdd;
                                if (ModConfig.rafflowsia.autoExpandCapacity && this.mana > overrideMaxMana) {
                                    overrideMaxMana = this.mana;
                                }
                                sync();
                                return;
                            } catch (Exception ignored) {
                                continue;
                            }
                        }
                    }
                }
            }
        }
    }

    @Overwrite
    public void writeToPacketNBT(NBTTagCompound cmp) {
        super.writeToPacketNBT(cmp);
        cmp.setString("lastFlower", lastFlower);
        cmp.setInteger("lastFlowerTimes", lastFlowerTimes);
        if (ModConfig.rafflowsia.autoExpandCapacity && overrideMaxMana > 0) {
            cmp.setInteger("overrideMaxMana", overrideMaxMana);
        }
        if (ModConfig.rafflowsia.newManaFormula) {
            cmp.setInteger("uniqueFlowerCount", uniqueFlowerCount);
            NBTTagList typeList = new NBTTagList();
            for (String type : uniqueFlowerTypes) {
                typeList.appendTag(new NBTTagString(type));
            }
            cmp.setTag("uniqueFlowerTypes", typeList);
        }
    }

    @Overwrite
    public void readFromPacketNBT(NBTTagCompound cmp) {
        super.readFromPacketNBT(cmp);
        lastFlower = cmp.getString("lastFlower");
        lastFlowerTimes = cmp.getInteger("lastFlowerTimes");
        if (ModConfig.rafflowsia.autoExpandCapacity) {
            overrideMaxMana = cmp.getInteger("overrideMaxMana");
        }
        if (ModConfig.rafflowsia.newManaFormula) {
            uniqueFlowerCount = cmp.getInteger("uniqueFlowerCount");
            uniqueFlowerTypes.clear();
            NBTTagList typeList = cmp.getTagList("uniqueFlowerTypes", 8); // 8 = NBTTagString
            for (int i = 0; i < typeList.tagCount(); i++) {
                uniqueFlowerTypes.add(typeList.getStringTagAt(i));
            }
        }
    }

    @Overwrite
    public void populateDropStackNBTs(List<ItemStack> drops) {
        super.populateDropStackNBTs(drops);
        ItemStack stack = drops.get(0);
        ItemNBTHelper.setString(stack, "lastFlower", lastFlower);
        ItemNBTHelper.setInt(stack, "lastFlowerTimes", lastFlowerTimes);
        if (ModConfig.rafflowsia.autoExpandCapacity && overrideMaxMana > 0) {
            ItemNBTHelper.setInt(stack, "overrideMaxMana", overrideMaxMana);
        }
        if (ModConfig.rafflowsia.newManaFormula) {
            ItemNBTHelper.setInt(stack, "uniqueFlowerCount", uniqueFlowerCount);
            NBTTagList typeList = new NBTTagList();
            for (String type : uniqueFlowerTypes) {
                typeList.appendTag(new NBTTagString(type));
            }
            stack.getTagCompound().setTag("uniqueFlowerTypes", typeList);
        }
    }

    @Overwrite
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        lastFlower = ItemNBTHelper.getString(stack, "lastFlower", "");
        lastFlowerTimes = ItemNBTHelper.getInt(stack, "lastFlowerTimes", 0);
        if (ModConfig.rafflowsia.autoExpandCapacity) {
            overrideMaxMana = ItemNBTHelper.getInt(stack, "overrideMaxMana", -1);
        }
        if (ModConfig.rafflowsia.newManaFormula) {
            uniqueFlowerCount = ItemNBTHelper.getInt(stack, "uniqueFlowerCount", 0);
            uniqueFlowerTypes.clear();
            NBTTagList typeList = stack.getTagCompound() != null ? stack.getTagCompound().getTagList("uniqueFlowerTypes", 8) : new NBTTagList();
            for (int i = 0; i < typeList.tagCount(); i++) {
                uniqueFlowerTypes.add(typeList.getStringTagAt(i));
            }
        }
    }
}
