package de.canitzp.carz.api;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * @author elmexl
 * Created on 01.11.2017.
 */
public abstract class EntityAIDriveableBase extends EntitySteerableBase {
    public EntityAIDriveableBase(World worldIn) {
        super(worldIn);
    }

    //    protected final static DataParameter<Boolean> AI_STEERED = EntityDataManager.createKey(EntityAIDriveableBase.class, DataSerializers.BOOLEAN);
    private long lastAISteered = 0;

    private float AIForward, AISteering;

//    @Override
//    public boolean isBeingRidden() {
//        return isAISteered() ? true : super.isBeingRidden();
//    }

    @Override
    public boolean canPassengerSteer() {
//        if (isAISteered()) {
//            return true;
//        }
        return super.canPassengerSteer();
    }

//    @Nullable
//    @Override
//    public Entity getControllingPassenger() {
//        return super.getControllingPassenger();
//    }

    @Override
    protected void onUpdate(boolean canPassengerSteer) {
        super.onUpdate(canPassengerSteer);
        if (isAISteered()) {
//            super.onUpdate(true);
            if (!world.isRemote)
                this.controlAIVehicle();
        } else {
//            super.onUpdate(canPassengerSteer);
        }
    }


    @Override
    protected void controlVehicle() {
        if (!isAISteered()) //Override here
        {
            super.controlVehicle();
        }
    }

    @Override
    public void updateInputs(boolean left, boolean right, boolean forward, boolean back) {
        if (isAISteered())
            super.updateInputs(false, false, false, false);
        else
            super.updateInputs(left, right, forward, back);
    }

    private void controlAIVehicle() {
        if (isAISteered() && this.getControllingPassenger() == null) {
            world.spawnParticle(EnumParticleTypes.REDSTONE, posX, posY, posZ, 0.1, 0.1, 0.1);
            float fwd = 0.0F; //Forward movement?
            if (AIForward > 0)
                fwd += 0.04 * AIForward;
            else if (AIForward < 0)
                fwd += 0.015 * AIForward;
            setSpeed(getSpeed() + fwd);

            //Steering
            double deltaR = 0; //rotation
            if (this.speedSqAbs > 0) {
                deltaR = Math.abs(steeringMod / this.speedSqAbs);

                //Maybe use this for something?

                deltaR = Math.min(deltaR, someOtherRandomRotModifier * Math.sqrt(this.speedSqAbs));

                //Rotate if driving backwards
                deltaR *= this.speedSq > 0 ? 1 : -1;

                deltaR *= AISteering;
                if (speedSqAbs > 0.001) {
                    //Apply the rotation if the car is moving.
                    this.deltaRotationYaw += deltaR;
                }
            }

        }
    }

    public void setSteering(float steering) {
        this.AISteering = steering;
    }

    public void setForward(float forward) {
        this.AIForward = forward;
    }

    public void startCar() {

    }

    public void stopCar() {

    }

    public boolean isRunning() {
        return true;
    }

    public void setAISteered() {
        lastAISteered = world.getTotalWorldTime();
    }

    private boolean _lastAISteered = false;

    public boolean isAISteered() {
        boolean a = world.getTotalWorldTime() - lastAISteered < 60;
        if (a != _lastAISteered) {
            _lastAISteered = a;
        }

        return a;
    }

    @Override
    public @Nonnull
    NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (isAISteered()) {
            compound.setFloat("AIControlledForward", AIForward);
            compound.setFloat("AIControlledSteering", AISteering);
        }
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("AIControlledForward", 99))
            AIForward = compound.getFloat("AIControlledForward");
        if (compound.hasKey("AIControlledSteering", 99))
            AISteering = compound.getFloat("AIControlledSteering");
        super.readFromNBT(compound);
    }
}
