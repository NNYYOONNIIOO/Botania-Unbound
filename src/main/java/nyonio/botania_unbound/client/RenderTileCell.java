package nyonio.botania_unbound.client;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import vazkii.botania.common.block.tile.TileCell;
import nyonio.botania_unbound.ModConfig;
import nyonio.botania_unbound.ICellNumberAccessor;

public class RenderTileCell extends TileEntitySpecialRenderer<TileCell> {

    @Override
    public void render(TileCell te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (!ModConfig.dandelifeon.enableReform) return;

        int number = 0;
        if (te instanceof ICellNumberAccessor) {
            number = ((ICellNumberAccessor) te).botania_unbound$getNumber();
        }
        // Fallback to generation if number is not set
        if (number <= 0) number = te.getGeneration();
        if (number <= 0) return;

        String text = String.valueOf(number);
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.75, z + 0.5);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.016666668F * 1.6F, -0.016666668F * 1.6F, 0.016666668F * 1.6F);

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);

        int textWidth = fontRenderer.getStringWidth(text);
        int color = 0xFFFFFF | (0xE0 << 24);
        fontRenderer.drawString(text, -textWidth / 2, 0, color);

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
