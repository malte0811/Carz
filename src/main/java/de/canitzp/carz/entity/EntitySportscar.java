package de.canitzp.carz.entity;

import de.canitzp.carz.Carz;
import de.canitzp.carz.api.EntitySteerableBase;
import de.canitzp.carz.client.renderer.RenderCar;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @author canitzp
 */
public class EntitySportscar extends EntitySteerableBase {

    public EntitySportscar(World worldIn) {
        super(worldIn);
        this.setSize(1.75F, 1.5F);
        this.seats[0] = new Vec3d(-0.3D, -1.0D, 0.0D);
    }

    @Override
    public ModelBase getCarModel() {
        return RenderCar.MODEL_SPORTSCAR;
    }

    @Nullable
    @Override
    public ResourceLocation getCarTexture() {
        return new ResourceLocation(Carz.MODID, "textures/cars/sportscar.png");
    }

    @Override
    public void setupGL(double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.translate(x, y + 1.5, z);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.rotationPitch, 1.0F, 0.0F, 0.0F);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }



}
