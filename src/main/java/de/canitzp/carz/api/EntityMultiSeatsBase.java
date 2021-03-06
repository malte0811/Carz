package de.canitzp.carz.api;

import de.canitzp.carz.network.NetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Base class for vehicles with multiple seats
 * Seats parts MUST be the first defined
 *
 * @author MisterErwin
 */
public abstract class EntityMultiSeatsBase extends EntityAIDriveableBase {
    private static final DataParameter<int[]> SEATING_DATA = EntityDataManager.createKey(EntityMultiSeatsBase.class, NetworkHandler.VARINT_ARRAY);

    private final int seatAmount;

    public EntityMultiSeatsBase(World worldIn, int seatAmount) {
        super(worldIn);
        this.seatAmount = seatAmount;
        //Yeah - set the seats here...
        int[] seats = new int[seatAmount];
        Arrays.fill(seats, -1);
        this.dataManager.set(SEATING_DATA, seats);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(SEATING_DATA, new int[0]);
        super.entityInit();
    }

    @Nullable
    public Entity getControllingPassenger() {
        int[] seats = this.dataManager.get(SEATING_DATA);
        return seats.length > 0 && seats[0] != -1 ? world.getEntityByID(seats[0]) : null;
    }

    @Override
    protected int getSeatByPassenger(Entity passenger) {
        int[] seats = this.dataManager.get(SEATING_DATA);
        for (int i = 0; i < seats.length; ++i) {
            if (seats[i] == passenger.getEntityId())
                return i;
        }
        return -1;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand, int partIndex) {
        //The DriverSeat is hitbox No 54
        if (!world.isRemote && /*partIndex >= 54 &&*/ !player.isSneaking()) {
            int[] seating_data = this.dataManager.get(SEATING_DATA);
            if (partIndex >= seatAmount)
                return super.processInitialInteract(player, hand, partIndex);
            if (seating_data.length > partIndex && seating_data[partIndex] != -1)
                return super.processInitialInteract(player, hand, partIndex);
            if (player.getHeldItem(player.getActiveHand()).getItem() instanceof ItemMonsterPlacer) {
                ItemStack spawnEgg = player.getHeldItem(player.getActiveHand());
                Entity entity = ItemMonsterPlacer.spawnCreature(this.world, ItemMonsterPlacer.getNamedIdFrom(spawnEgg), this.posX, this.posY, this.posZ);
                if (entity != null) {
                    if (entity.startRiding(this)) {
                        this.setSeatingData(seating_data, partIndex, entity.getEntityId());
                        return true;
                    }
                }
            }
            int old_seat = this.getSeatByPassenger(player);
            if (old_seat != -1) {
                seating_data = this.setSeatingData(seating_data, old_seat, -1);
            }
            boolean b = player.startRiding(this);
            if (b) {
                this.setSeatingData(seating_data, partIndex, player.getEntityId());
            }
            return b;
        }

        return super.processInitialInteract(player, hand, partIndex);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        int seat = this.getSeatByPassenger(passenger);
        if (seat != -1) {
            this.setSeatingData(dataManager.get(SEATING_DATA), seat, -1);
        }
        super.removePassenger(passenger);
    }

    private int[] setSeatingData(int[] seats, int index, int value) {
        //The EntityDataManager only syncs if the old data != new data, so we have to clone the array
        int[] newseats = Arrays.copyOf(seats, seats.length); //new int[seats.length];
        newseats[index] = value;
        this.dataManager.set(SEATING_DATA, newseats);
        return newseats;
    }


}
