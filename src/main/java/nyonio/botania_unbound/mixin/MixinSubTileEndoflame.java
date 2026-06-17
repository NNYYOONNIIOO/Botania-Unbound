package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.block.subtile.generating.SubTileEndoflame;

@Mixin(value = SubTileEndoflame.class, remap = false)
public abstract class MixinSubTileEndoflame extends vazkii.botania.api.subtile.SubTileGenerating {

    @Shadow private int burnTime;
    @Shadow @Final private static int FUEL_CAP;

    private int overrideMaxMana = -1;

    /**
     * Convert burn time to mana: time * 3 / 2 (because every 2 ticks generates 3 mana)
     */
    private int time2mana(int time) {
        return (int) Math.min(Integer.MAX_VALUE, time * 3f / 2);
    }

    /**
     * Convert mana to burn time: mana * 2 / 3
     */
    private int mana2time(int mana) {
        return (int) Math.min(Integer.MAX_VALUE, mana * 2f / 3);
    }

    /**
     * Override getMaxMana to return expanded capacity when burning high-energy fuels
     * Only needed when endoflameRemoveTickLimit is enabled
     */
    @Inject(method = "getMaxMana", at = @At("HEAD"), cancellable = true)
    private void injectGetMaxMana(CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.endoflame.removeTickLimit && overrideMaxMana > 0) {
            cir.setReturnValue(overrideMaxMana);
        }
    }

    /**
     * Override canGeneratePassively to return false when skip burn process is enabled
     * This prevents the parent class from generating mana over time
     */
    @Inject(method = "canGeneratePassively", at = @At("HEAD"), cancellable = true)
    private void injectCanGeneratePassively(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.endoflame.skipBurnProcess) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Inject after burnTime decrement to skip burn process
     * If burnTime > 0 and endoflameSkipBurnProcess is enabled, convert it to mana immediately
     * 
     * Note: This injects AFTER the original burnTime decrement, so the original protection
     * mechanism (item.age >= 59 + slowdown) is preserved
     */
    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lvazkii/botania/api/subtile/SubTileGenerating;onUpdate()V", shift = At.Shift.AFTER))
    private void injectAfterSuperOnUpdate(CallbackInfo ci) {
        if (!ModConfig.endoflame.skipBurnProcess) return;
        if (burnTime > 0) {
            // Calculate required capacity based on burn time (only when remove tick limit is enabled)
            if (ModConfig.endoflame.removeTickLimit) {
                overrideMaxMana = Math.max(getMaxMana(), time2mana(burnTime));
            }
            
            int maxManaFromTime = time2mana(burnTime);
            int maxManaToFill = getMaxMana() - mana;
            if (maxManaToFill >= maxManaFromTime) {
                // Fill all mana from burn time
                mana = Math.min(getMaxMana(), mana + maxManaFromTime);
                burnTime = 0;
            } else {
                // Fill partial mana
                mana = Math.min(getMaxMana(), mana + maxManaToFill);
                burnTime -= mana2time(maxManaToFill);
            }
            sync();
        }
    }

    /**
     * Redirect Math.min to remove 32000 tick limit when config is enabled
     */
    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;min(II)I",
            ordinal = 0
        ),
        require = 0
    )
    private int redirectMin(int a, int b) {
        if (ModConfig.endoflame.removeTickLimit && a == FUEL_CAP) {
            return b; // Return full burn time without limit
        }
        return Math.min(a, b);
    }
}