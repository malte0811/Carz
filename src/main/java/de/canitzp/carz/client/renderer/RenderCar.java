package de.canitzp.carz.client.renderer;

import de.canitzp.carz.Carz;
import de.canitzp.carz.api.EntityPartedBase;
import de.canitzp.carz.api.EntityRenderedBase;
import de.canitzp.carz.api.EntitySteerableBase;
import de.canitzp.carz.api.IColorableCar;
import de.canitzp.carz.client.PixelMesh;
import de.canitzp.carz.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * @author canitzp
 */
@SideOnly(Side.CLIENT)
public class RenderCar<T extends EntityRenderedBase> extends Render<T> implements IResourceManagerReloadListener {
    private ModelBase model;
    private ResourceLocation texture, overlay;
    private Map<UUID, Pair<Integer, UUID>> cachedCarRenderer = new HashMap<>();

    public RenderCar(RenderManager renderManager) {
        super(renderManager);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(@Nonnull T entity) {
        return null;
    }

    @Override
    public void doRender(@Nonnull T car, double x, double y, double z, float entityYaw, float partialTicks) {
        if (this.model == null) {
            this.model = car.getCarModel();
            this.texture = car.getCarTexture();
            if (car instanceof IColorableCar) {
                this.overlay = ((IColorableCar) car).getOverlayTexture();
            }
        }
        GlStateManager.pushMatrix();
        if (car instanceof EntitySteerableBase) {
            car.setupGL(x + ((EntitySteerableBase) car).rotationTranslationX,
                    y,
                    z + ((EntitySteerableBase) car).rotationTranslationZ, entityYaw, partialTicks);
        } else
            car.setupGL(x, y, z, entityYaw, partialTicks);
        if (this.texture != null) {
            if (this.overlay != null) {
                try {
                    int color = 0xFFFFFF;
                    boolean calculate = false;
                    PixelMesh mesh = null;
                    List<Pair<Integer, Integer>> meshCoordinates = Collections.emptyList();
                    if(car instanceof IColorableCar){
                        color = ((IColorableCar) car).getCurrentColor();
                        mesh = ((IColorableCar) car).getCurrentMesh();
                        meshCoordinates = ((IColorableCar) car).getPixelMeshCoordiantes();
                        int oldColor = 0;
                        UUID oldMesh = null;
                        Pair<Integer, UUID> pair = this.cachedCarRenderer.get(car.getPersistentID());
                        if(pair != null){
                            oldColor = this.cachedCarRenderer.get(car.getPersistentID()).getLeft();
                            oldMesh = this.cachedCarRenderer.get(car.getPersistentID()).getRight();
                        }
                        calculate = ((IColorableCar) car).shouldRecalculateTexture() || color != oldColor || (mesh != null && oldMesh != mesh.getId());
                        if(calculate){
                            this.cachedCarRenderer.put(car.getPersistentID(), Pair.of(color, mesh != null ? mesh.getId() : null));
                        }
                    }
                    RenderUtil.bindLayeredTexture(car.getPersistentID(), this.texture, this.overlay, 0xFFFFFF, color, mesh, meshCoordinates, calculate);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                this.bindTexture(this.texture);
            }
        } else {
            GlStateManager.bindTexture(TextureUtil.MISSING_TEXTURE.getGlTextureId());
        }
        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(car));
        }
        //GlStateManager.disableAlpha();
        //GlStateManager.enableBlend();
        //GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if(this.model != null){
            this.model.render(car, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        }
        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();

        //Debug: Yehay
        if (Carz.RENDER_DEBUG && car instanceof EntitySteerableBase) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.glLineWidth(2.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);

            EntityPlayer player = Minecraft.getMinecraft().player;

            double renderPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
            double renderPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
            double renderPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;

//            AxisAlignedBB bb = car.getEntityBoundingBox().offset(-((EntitySteerableBase) car).rotationTranslationX,
//                    -((EntitySteerableBase) car).rotationTranslationY,
//                    -((EntitySteerableBase) car).rotationTranslationZ
//            ).grow(-.5, 10, -.5);
//            RenderGlobal.renderFilledBox(bb.grow(0.002D).offset(-renderPosX, -renderPosY, -renderPosZ), 0, 0, 1, 1f);

            AxisAlignedBB bb = car.getEntityBoundingBox();

            double xx = bb.minX + (bb.maxX - bb.minX) / 2;
            double zz = bb.minZ + (bb.maxZ - bb.minZ) / 2;
            RenderGlobal.renderFilledBox(new AxisAlignedBB(xx - 0.1, bb.minY, zz - 0.1, xx + 0.1, bb.maxY + 10, zz + 0.1)
                    .offset(-renderPosX, -renderPosY, -renderPosZ).grow(0, 10, 0), 0, 1, 0, 0.9f);


            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }

        if (Carz.RENDER_DEBUG && car instanceof EntityPartedBase) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.glLineWidth(2.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);

            EntityPlayer player = Minecraft.getMinecraft().player;

            double renderPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
            double renderPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
            double renderPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;

            for (AxisAlignedBB bb : ((EntityPartedBase) car).possibleCollisions)
                RenderGlobal.renderFilledBox(bb.grow(0.002D).offset(-renderPosX, -renderPosY, -renderPosZ), 1.0F, 1.0F, 0.0F, 0.2f);

            for (AxisAlignedBB bb : ((EntityPartedBase) car).collisions)
                RenderGlobal.renderFilledBox(bb.grow(0.002D).offset(-renderPosX, -renderPosY, -renderPosZ), 1.0F, 0.2F, 0.2F, 0.2F);

            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }

        super.doRender(car, x, y, z, entityYaw, partialTicks);
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager) {
        this.model = null;
        this.texture = null;
        this.overlay = null;
        this.cachedCarRenderer.clear();
    }
}
