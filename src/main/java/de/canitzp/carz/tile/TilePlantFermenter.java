package de.canitzp.carz.tile;

import de.canitzp.carz.api.CarzAPI;
import de.canitzp.carz.inventory.SidedInventory;
import de.canitzp.carz.inventory.SidedInventoryWrapper;
import de.canitzp.carz.recipes.FactoryPlantFermenter;
import de.canitzp.carz.recipes.RecipePlantFermenter;
import de.canitzp.carz.util.StackUtil;
import de.canitzp.carz.util.TileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author canitzp
 */
public class TilePlantFermenter extends TileBase implements ITickable{

    @Nonnull
    public SidedInventory inventory = new SidedInventory("Plant Fermenter", 7) {
        @Override
        public boolean canInsertItem(int index, @Nonnull ItemStack stack, @Nonnull EnumFacing side) {
            return side != EnumFacing.SOUTH && (index == 0 || index == 1 || index == 2 || index == 3 || index == 5);
        }
        @Override
        public boolean canExtractItem(int index, @Nonnull ItemStack stack, @Nonnull EnumFacing side) {
            return side != EnumFacing.NORTH && (index == 4 || index == 6);
        }
        @Override
        public boolean isItemValidForSlot(int index, @Nonnull ItemStack stack) {
            if(index == 0 || index == 1 || index == 2 || index == 3){
                return CarzAPI.isStackValidPlant(stack);
            } else if(index == 5){
                return stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            }
            return this.ignoreValidify;
        }
    };
    private SidedInventoryWrapper[] sidedWrapper = inventory.getOneForAllSides();
    @Nonnull
    public FluidTank tank = new FluidTank(10000);
    public int ticksLeft, maxTicks;
    private ItemStack outputStack = ItemStack.EMPTY;
    private FluidStack outputFluid;

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return this.getCapability(capability, facing) != null;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if(facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.sidedWrapper[facing.ordinal()]);
        } else if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.tank);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.ticksLeft = compound.getInteger("TicksLeft");
        this.maxTicks = compound.getInteger("MaxTicks");
        if(compound.hasKey("FluidTank", Constants.NBT.TAG_COMPOUND)){
            this.tank = this.tank.readFromNBT(compound.getCompoundTag("FluidTank"));
        }
        if(compound.hasKey("OutputStack", Constants.NBT.TAG_COMPOUND)){
            this.outputStack = new ItemStack(compound.getCompoundTag("OutputStack"));
        }
        if(compound.hasKey("OutputFluid", Constants.NBT.TAG_COMPOUND)){
            this.outputFluid = FluidStack.loadFluidStackFromNBT(compound.getCompoundTag("OutputFluid"));
        }
        this.inventory.readTag(compound);
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        this.inventory.writeTag(compound);
        compound.setTag("FluidTank", this.tank.writeToNBT(new NBTTagCompound()));
        compound.setInteger("TicksLeft", this.ticksLeft);
        compound.setInteger("MaxTicks", this.maxTicks);
        if(!this.outputStack.isEmpty()){
            compound.setTag("OutputStack", this.outputStack.writeToNBT(new NBTTagCompound()));
        }
        if(this.outputFluid != null){
            compound.setTag("OutputFluid", this.outputFluid.writeToNBT(new NBTTagCompound()));
        }
        return super.writeToNBT(compound);
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound synctag = super.getUpdateTag();
        synctag.setInteger("TicksLeft", this.ticksLeft);
        synctag.setInteger("MaxTicks", this.maxTicks);
        this.tank.writeToNBT(synctag);
        if(!this.outputStack.isEmpty()){
            synctag.setTag("OutputStack", this.outputStack.writeToNBT(new NBTTagCompound()));
        }
        if(this.outputFluid != null){
            synctag.setTag("OutputFluid", this.outputFluid.writeToNBT(new NBTTagCompound()));
        }
        return new SPacketUpdateTileEntity(this.pos, 0, synctag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        this.ticksLeft = tag.getInteger("TicksLeft");
        this.maxTicks = tag.getInteger("MaxTicks");
        this.tank.readFromNBT(tag);
        if(tag.hasKey("OutputStack", Constants.NBT.TAG_COMPOUND)){
            this.outputStack = new ItemStack(tag.getCompoundTag("OutputStack"));
        }
        if(tag.hasKey("OutputFluid", Constants.NBT.TAG_COMPOUND)){
            this.outputFluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("OutputFluid"));
        }
    }

    @Override
    public void update() {
        if(!this.world.isRemote){
            if(ticksLeft > 0){
                this.ticksLeft--;
                if(this.world.getTotalWorldTime() % 5 == 0){
                    TileUtil.sync(this);
                }
            }
            if(ticksLeft <= 0){
                if(!this.outputStack.isEmpty() || this.outputFluid != null){
                    if(!this.outputStack.isEmpty()){
                        this.inventory.setIgnoreValidifyFlag(true);
                        this.inventory.insertItem(4, this.outputStack, false);
                        this.inventory.setIgnoreValidifyFlag(false);
                        this.outputStack = ItemStack.EMPTY;
                    }
                    if(this.outputFluid != null){
                        this.tank.fill(this.outputFluid, true);
                        this.outputFluid = null;
                    }
                    TileUtil.sync(this);
                } else {
                    for(int i = 0; i <= 3; i++){
                        ItemStack stackInInputSlot = this.inventory.getStackInSlot(i);
                        if(!stackInInputSlot.isEmpty()){
                            RecipePlantFermenter recipe = canProduce(stackInInputSlot);
                            if(recipe != null){
                                ItemStack output = recipe.getOutput();
                                FluidStack outFluid = recipe.getOutputFluid();
                                this.inventory.setIgnoreValidifyFlag(true);
                                if(this.inventory.insertItem(4, this.outputStack, true).isEmpty()){
                                    if(outFluid == null || this.tank.fill(outFluid, false) == outFluid.amount){
                                        this.ticksLeft = this.maxTicks = recipe.getProduceTicks();
                                        this.inventory.extractItem(i, recipe.getInput().getCount(), false);
                                        this.outputStack = output.copy();
                                        if(outFluid != null){
                                            this.outputFluid = outFluid.copy();
                                        }
                                    }
                                }
                                this.inventory.setIgnoreValidifyFlag(false);
                                break;
                            }
                        }
                    }
                }
            }
            // Fill FluidHandler
            if(this.tank.getFluidAmount() > 0){
                this.inventory.setIgnoreValidifyFlag(true);
                ItemStack bucket = this.inventory.extractItem(5, 1, true);
                if(!bucket.isEmpty()){
                    IFluidHandlerItem handler = bucket.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                    if(handler != null){
                        this.tank.drain(handler.fill(this.tank.getFluid(), false), false);
                        if(this.inventory.insertItem(6, handler.getContainer(), true).isEmpty()){
                            this.tank.drain(handler.fill(this.tank.getFluid(), true), true);
                            this.inventory.extractItem(5, 1, false);
                            this.inventory.insertItem(6, handler.getContainer(), false);
                            TileUtil.sync(this);
                        }
                    }
                }
                this.inventory.setIgnoreValidifyFlag(false);
            }
            if(this.world.getTotalWorldTime() % 20 == 0){
                TileUtil.sync(this);
            }
        }
    }

    @Nullable // Is null if it can't produce
    public RecipePlantFermenter canProduce(ItemStack input){
        if(!input.isEmpty() && CarzAPI.isStackValidPlant(input)){
            RecipePlantFermenter recipe = FactoryPlantFermenter.getRecipeOrDefault(input);
            ItemStack currentlyInsideOutSlot = this.inventory.getStackInSlot(4);
            if(currentlyInsideOutSlot.isEmpty() || StackUtil.canMerge(recipe.getOutput(), currentlyInsideOutSlot)){
                if(recipe.getOutputFluid() != null){
                    if(this.tank.fill(recipe.getOutputFluid(), false) == 0){
                        return null;
                    }
                }
                return recipe;
            }
        }
        return null;
    }
}
