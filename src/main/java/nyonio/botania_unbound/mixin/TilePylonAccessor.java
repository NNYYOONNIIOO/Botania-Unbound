package nyonio.botania_unbound.mixin;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.tile.TilePylon;

@Mixin(value = TilePylon.class, remap = false)
public interface TilePylonAccessor {

    @Accessor("activated")
    @Mutable
    void setActivated(boolean activated);

    @Accessor("centerPos")
    @Mutable
    void setCenterPos(BlockPos pos);
}