package de.canitzp.carz.entity;

import de.canitzp.carz.Carz;
import de.canitzp.carz.Registry;
import de.canitzp.carz.api.EntityMultiSeatsBase;
import de.canitzp.carz.api.EntityPartedBase;
import de.canitzp.carz.api.IColorableCar;
import de.canitzp.carz.api.IWheelClampable;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * @author canitzp
 */
public class EntitySpeedster extends EntityMultiSeatsBase implements IWheelClampable, IColorableCar {
    private static EntityPartedBase.PartData partData;
    private boolean clamped = false;

    private static final DataParameter<Integer> COLOR = EntityDataManager.createKey(EntitySpeedster.class, DataSerializers.VARINT);


    static {
        EntityPartedBase.PartBuilder builder = builder();
        builder.addInteractOnlyPart(0.3f, 0.3f, -0.4f, 0.6f, 1f);
        builder.addInteractOnlyPart(-0.3f, 0.3f, -0.4f, 0.6f, 1f);

        builder.addCollidingPart(0.35f, 0.3f, 0.35f, 0.7f, 0.4f);
        builder.addCollidingPart(-0.35f, 0.3f, 0.35f, 0.7f, 0.4f);
        builder.addCollidingPart(0.35f, 0.3f, -0.35f, 0.7f, 0.4f);
        builder.addCollidingPart(-0.35f, 0.3f, -0.35f, 0.7f, 0.4f);

        builder.addCollidingPart(-0.35f, 0.9f, 0.6f, 0.2f, 1f);
        builder.addCollidingPart(0f, 0.9f, 0.6f, 0.2f, 1f);
        builder.addCollidingPart(0.35f, 0.9f, 0.6f, 0.2f, 1f);

        partData = builder.build();
    }

    public EntitySpeedster(World world) {
        super(world, 2);
        this.setSize(0.2f, 0.2f);
        this.someOtherRandomRotModifier = 2.15;

        this.setDriverSeat(-0.3, 0.25D, -0.3);
        this.addSeat(-0.2, 0.25D, 0.3);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(COLOR, 0xD70404);
        super.entityInit();
    }

    @Override
    protected EntityInvisibleCarPart[] constructPartArray() {
        return partData.spawnInvisibleParts(this);
    }

    @Override
    protected int[] constructCollidingPartIndizes() {
        return partData.getCollidingPartIndizes();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModelBase getCarModel() {
        return Registry.MODEL_SPEEDSTER;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public ResourceLocation getCarTexture() {
        return new ResourceLocation(Carz.MODID, "textures/cars/speedster_base.png");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void setupGL(double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.translate(x, y + 3, z);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.rotationPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(2.0, 2.0, 2.0);
    }

    @Override
    public void setClamped(boolean clamped) {
        this.clamped = clamped;
    }

    @Override
    public boolean isClamped() {
        return this.clamped;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.clamped = compound.getBoolean("clamped");
        if (compound.hasKey("Color", Constants.NBT.TAG_INT)) {
            this.dataManager.set(COLOR, compound.getInteger("Color"));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setBoolean("clamped", this.clamped);
        compound.setInteger("Color", this.dataManager.get(COLOR));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ResourceLocation getOverlayTexture() {
        return new ResourceLocation(Carz.MODID, "textures/cars/speedster_overlay.png");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getCurrentColor() {
        return this.dataManager.get(COLOR);
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemDye) {
            if (!player.world.isRemote) {
                EnumDyeColor color = EnumDyeColor.byDyeDamage(stack.getMetadata());
                this.dataManager.set(COLOR, color.getColorValue());
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return true;
        }
        return super.processInitialInteract(player, hand);
    }

}