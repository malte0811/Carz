package de.canitzp.carz.client.renderer;

import de.canitzp.carz.tile.TileSign;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**
 * @author canitzp
 */
public class RenderRoadSign extends TileEntitySpecialRenderer<TileSign> {

    @Override
    public void render(TileSign te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(0.0F, 0.0F, 0.0F, 0.0F);

        te.getSignType().getModel().render(1/16F);

        GlStateManager.popMatrix();
        super.render(te, x, y, z, partialTicks, destroyStage, alpha);
    }
}
