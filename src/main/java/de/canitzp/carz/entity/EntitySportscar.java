package de.canitzp.carz.entity;

import de.canitzp.carz.Carz;
import de.canitzp.carz.Registry;
import de.canitzp.carz.api.EntityAIDriveableBase;
import de.canitzp.carz.api.EntityPartedBase;
import de.canitzp.carz.api.IWheelClampable;
import de.canitzp.carz.inventory.Inventory;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author canitzp
 */
public class EntitySportscar extends EntityAIDriveableBase implements IWheelClampable {

    private static EntityPartedBase.PartData partData;

    private FluidTank tank = new FluidTank(10000);
    private Inventory inventory = new Inventory("Sportscar-Inventory", 37);
    private boolean clamped = false;

    static {
        EntityPartedBase.PartBuilder builder = builder();
        builder.addCollidingPart(0, 0, 0, 1.75F, 1.8125F);
        partData = builder.build();
    }

    public EntitySportscar(World world) {
        super(world);
//        this.setSize(1.75F, 1.8125F);
        this.setSize(0.2f, 1.8125F);
        this.setDriverSeat(-0.3D, -1.0D, 0.0D);

        this.someOtherRandomRotModifier = 2;
    }


    @Override
    protected EntityInvisibleCarPart[] constructPartArray() {
        return partData.spawnInvisibleParts(this);
    }

    @Override
    protected int[] constructCollidingPartIndizes() {
        return partData.getCollidingPartIndizes();
    }

    @Override
    public ModelBase getCarModel() {
        return Registry.MODEL_SPORTSCAR;
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
    protected void readEntityFromNBT(@Nonnull NBTTagCompound compound) {
        this.inventory.readTag(compound);
        this.tank.readFromNBT(compound.getCompoundTag("FluidTank"));
        this.clamped = compound.getBoolean("clamped");
    }

    @Override
    protected void writeEntityToNBT(@Nonnull NBTTagCompound compound) {
        this.inventory.writeTag(compound);
        compound.setTag("FluidTank", this.tank.writeToNBT(new NBTTagCompound()));
        compound.setBoolean("clamped", this.clamped);
    }

    @Nullable
    @Override
    public IFluidHandler getFluidHandler(@Nullable EnumFacing facing) {
        return this.tank;
    }

    @Nullable
    @Override
    public IItemHandler getInventory(@Nullable EnumFacing facing) {
        return this.inventory;
    }

    @Override
    public void setClamped(boolean clamped) {
        this.clamped = clamped;
    }

    @Override
    public boolean isClamped() {
        return clamped;
    }
}
