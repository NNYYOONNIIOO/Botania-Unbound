package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vazkii.botania.common.block.subtile.generating.SubTileEntropinnyum;

@Mixin(value = SubTileEntropinnyum.class, remap = false)
public class MixinSubTileEntropinnyum {

    /**
     * Redirect the liquid check to allow TNT in liquids when config enabled
     * Original: !supertile.getWorld().getBlockState(new BlockPos(tnt)).getMaterial().isLiquid()
     * Modified: Allow TNT in liquids when config enabled
     */
    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/state/IBlockState;getMaterial()Lnet/minecraft/block/material/Material;",
            ordinal = 0
        ),
        require = 0
    )
    private net.minecraft.block.material.Material redirectGetMaterial(IBlockState state) {
        // If config allows liquid TNT, return a non-liquid material to bypass the check
        if (ModConfig.entropinnyum.allowLiquidTNT) {
            return net.minecraft.block.material.Material.AIR; // Non-liquid material
        }
        return state.getMaterial();
    }
}