package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ICellNumberAccessor;
import nyonio.botania_unbound.ModConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.block.tile.TileCell;
import vazkii.botania.common.block.tile.TileMod;

@Mixin(value = TileCell.class, remap = false)
public abstract class MixinTileCell extends TileMod implements ICellNumberAccessor {

    @Shadow private int generation;
    @Shadow private boolean ticked;
    @Shadow private BlockPos flowerCoords;
    @Shadow private BlockPos validCoords;

    private int number;

    @Override
    public int botania_unbound$getNumber() {
        return this.number;
    }

    @Override
    public void botania_unbound$setNumber(int num) {
        this.number = num;
    }

    @Inject(method = "writePacketNBT", at = @At("RETURN"))
    private void onWritePacketNBT(NBTTagCompound cmp, CallbackInfo ci) {
        cmp.setInteger("number", number);
    }

    @Inject(method = "readPacketNBT", at = @At("RETURN"))
    private void onReadPacketNBT(NBTTagCompound cmp, CallbackInfo ci) {
        number = cmp.getInteger("number");
        // Initialize number for cells that don't have it yet (e.g. manually placed or pre-mod cells)
        if (number == 0 && ModConfig.dandelifeon.enableReform) {
            number = ModConfig.dandelifeon.initialCellNumber;
        }
    }

    /**
     * Overwrite setGeneration to also set the number field when reform is enabled.
     *
     * @author Nyonio
     * @reason Set initial cell number when reform mechanism is enabled
     */
    @Overwrite
    public void setGeneration(TileEntity flower, int gen) {
        generation = gen;
        if (!ticked) {
            flowerCoords = flower.getPos();
            validCoords = getPos();
            ticked = true;
            if (ModConfig.dandelifeon.enableReform && number == 0) {
                number = ModConfig.dandelifeon.initialCellNumber;
            }
        } else if (!matchCoords(validCoords, this) || !matchCoords(flowerCoords, flower)) {
            world.setBlockToAir(pos);
        }
    }

    private boolean matchCoords(BlockPos coords, TileEntity tile) {
        return coords.equals(tile.getPos());
    }
}
