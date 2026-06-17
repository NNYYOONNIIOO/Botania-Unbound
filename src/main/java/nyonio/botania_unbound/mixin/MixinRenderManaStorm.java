package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.common.entity.EntityManaStorm;

import javax.annotation.Nonnull;

@Mixin(value = vazkii.botania.client.render.entity.RenderManaStorm.class, remap = false)
public abstract class MixinRenderManaStorm extends Render<EntityManaStorm> {

    protected MixinRenderManaStorm(RenderManager renderManager) {
        super(renderManager);
    }

    /**
     * Overwrite doRender to use configurable total bursts and death time for scale calculation.
     */
    @Overwrite
    public void doRender(@Nonnull EntityManaStorm storm, double x, double y, double z, float something, float pticks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        float maxScale = 1.95F;
        int totalBursts = ModConfig.manaStorm.totalBursts;
        int deathTimeConfig = ModConfig.manaStorm.deathTime;
        float scale = 0.05F + ((float) storm.burstsFired / totalBursts - (storm.deathTime == 0 ? 0 : storm.deathTime + pticks) / deathTimeConfig) * maxScale;
        RenderHelper.renderStar(0x00FF00, scale, scale, scale, storm.getUniqueID().getMostSignificantBits());
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(@Nonnull EntityManaStorm entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
