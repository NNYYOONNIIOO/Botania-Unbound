package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.BotaniaCompat;
import nyonio.botania_unbound.ModConfig;
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

    private int overrideMaxMana = -1;

    private int time2mana(int time) {
        return (int) Math.min(Integer.MAX_VALUE, time * 3f / 2);
    }

    private int mana2time(int mana) {
        return (int) Math.min(Integer.MAX_VALUE, mana * 2f / 3);
    }

    @Inject(method = "getMaxMana", at = @At("HEAD"), cancellable = true)
    private void injectGetMaxMana(CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.endoflame.removeTickLimit && overrideMaxMana > 0) {
            cir.setReturnValue(overrideMaxMana);
        }
    }

    @Inject(method = "canGeneratePassively", at = @At("HEAD"), cancellable = true)
    private void injectCanGeneratePassively(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.endoflame.skipBurnProcess) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lvazkii/botania/api/subtile/SubTileGenerating;onUpdate()V", shift = At.Shift.AFTER))
    private void injectAfterSuperOnUpdate(CallbackInfo ci) {
        if (!ModConfig.endoflame.skipBurnProcess) return;
        if (burnTime > 0) {
            if (ModConfig.endoflame.removeTickLimit) {
                overrideMaxMana = Math.max(getMaxMana(), time2mana(burnTime));
            }

            int maxManaFromTime = time2mana(burnTime);
            int maxManaToFill = getMaxMana() - mana;
            if (maxManaToFill >= maxManaFromTime) {
                mana = Math.min(getMaxMana(), mana + maxManaFromTime);
                burnTime = 0;
            } else {
                mana = Math.min(getMaxMana(), mana + maxManaToFill);
                burnTime -= mana2time(maxManaToFill);
            }
            sync();
        }
    }

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
        int fuelCap = BotaniaCompat.ENDOFLAME_BURN_TIME;
        if (ModConfig.endoflame.removeTickLimit && a == fuelCap) {
            return b;
        }
        return Math.min(a, b);
    }
}
