package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.common.block.BlockManaBomb;

@Mixin(value = BlockManaBomb.class, remap = false)
public abstract class MixinBlockManaBomb {

    /**
     * Inject into onBurstCollision to optionally prevent mana pulse activation.
     */
    @Inject(method = "onBurstCollision", at = @At("HEAD"), cancellable = true)
    private void onBurstCollisionHead(IManaBurst burst, World world, BlockPos pos, CallbackInfo ci) {
        if (ModConfig.manaStorm.disableManaPulseActivation) {
            ci.cancel();
        }
    }
}
