package me.ichun.mods.morph.api.morph;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

public class MorphState implements Comparable<MorphState>
{
    public MorphVariant variant;
    private LivingEntity entInstance;
    private final Collection<ItemEntity> entInstanceDropCapture = new ArrayList<>();
    public float renderedShadowSize;
    public ArrayList<Trait<?>> traits = new ArrayList<>();

    private MorphState(){}

    public MorphState(MorphVariant variant, Player player)
    {
        this.variant = variant;
        this.traits = MorphApi.getApiImpl().getTraitsForVariant(variant, player);
        //TODO make sure other clients know what traits the player has unlocked
    }

    //For Traits
    public void activateHooks()
    {
        for(Trait<?> trait : traits)
        {
            trait.addHooks();
        }
    }
    public void deactivateHooks()
    {
        for(Trait<?> trait : traits)
        {
            trait.removeHooks();
        }
    }

    public void tick(Player player, boolean resetInventory)
    {
        LivingEntity livingInstance = getEntityInstance(player.level(), player);
        livingInstance.captureDrops(entInstanceDropCapture); //We don't want our mob instance to drop items
        entInstanceDropCapture.clear(); //Have the items, GC.

        syncEntityPosRotWithPlayer(livingInstance, player);

        syncInventory(livingInstance, player, true); //reset the inventory so the entity doesn't actually use our equipment when ticking.

        if(livingInstance.canUpdate())
        {
            livingInstance.tick();
        }

        syncEntityWithPlayer(livingInstance, player);

        if(!resetInventory)
        {
            syncInventory(livingInstance, player, false); //sync the inventory for rendering purposes.
        }

        
    }

    public void tickTraits()
    {
        for(Trait<?> trait : traits)
        {
            trait.doTick(1F);
        }
    }

    @Nonnull
    @Deprecated
    //remove in 1.18
    public LivingEntity getEntityInstance(Level world, @Nullable UUID playerId)
    {
        return getEntityInstance(world, playerId != null ? world.getPlayerByUUID(playerId) : null);
    }

    @Nonnull
    public LivingEntity getEntityInstance(Level world, @Nullable Player player)
    {
        if(entInstance == null || entInstance.level() != world)
        {
            entInstance = variant.createEntityInstance(world, player);

            for(Trait<?> trait : traits)
            {
                trait.livingInstance = entInstance;
            }
        }

        return entInstance;
    }

    public CompoundTag write(CompoundTag tag)
    {
        tag.put("variant", variant.write(new CompoundTag()));
        return tag;
    }

    public void read(CompoundTag tag)
    {
        variant = MorphVariant.createFromNBT(tag.getCompound("variant"));
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof MorphState)
        {
            MorphState state = (MorphState)obj;
            return Objects.equals(variant, state.variant);
        }
        return false;
    }

    @Override
    public int compareTo(MorphState o)
    {
        return variant.compareTo(o.variant);
    }

    public static MorphState createFromNbt(CompoundTag tag)
    {
        MorphState state = new MorphState();
        state.read(tag);
        return state;
    }

    public static void syncEntityPosRotWithPlayer(LivingEntity living, Player player)
    {
        living.tickCount = player.tickCount;

        living.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        living.xo = player.xo;
        living.yo = player.yo;
        living.zo = player.zo;

        living.xo = player.xo;
        living.yo = player.yo;
        living.zo = player.zo;

        living.yRotO = player.yRotO;
        living.xRotO = player.xRotO;

        living.setYHeadRot(player.getYHeadRot());
        living.yHeadRotO = player.yHeadRotO;

        living.yBodyRot = player.yBodyRot;
        living.yBodyRotO = player.yBodyRotO;

        // Potion effects are managed by the entity and are intentionally not ticked here.
    }

    public static void syncEntityWithPlayer(LivingEntity living, Player player)
    {
        syncEntityPosRotWithPlayer(living, player); //resync with the player position in case the entity moved whilst ticking.


        living.setDeltaMovement(player.getDeltaMovement());

        //Entity stuff
        living.horizontalCollision = player.horizontalCollision;
        living.verticalCollision = player.verticalCollision;
        living.setOnGround(player.onGround());
        living.setShiftKeyDown(player.isCrouching());
        living.setSwimming(player.isSwimming());
        living.setSprinting(player.isSprinting());

        living.setHealth(living.getMaxHealth() * (player.getHealth() / player.getMaxHealth()));
        living.hurtTime = player.hurtTime;
        living.deathTime = player.deathTime;

        // Render-related state that has public accessors in 1.20.1.
        living.swinging = player.swinging;
        living.swingTime = player.swingTime;
        living.swingingArm = player.swingingArm;
        living.setRemainingFireTicks(player.getRemainingFireTicks());

        specialEntityPlayerSync(living, player);
    }

    public static void specialEntityPlayerSync(LivingEntity living, Player player)
    {
        for(BiConsumer<LivingEntity, Player> consumer : MorphApi.getApiImpl().getModPlayerMorphSyncConsumers())
        {
            consumer.accept(living, player);
        }
    }

    public static void syncInventory(LivingEntity living, Player player, boolean reset)
    {
        if(living instanceof Player)
        {
            Player playerEntity = (Player)living;

            //player entity plays sound when equipping items.
            for(EquipmentSlot value : EquipmentSlot.values())
            {
                boolean shouldReset = reset && (value == EquipmentSlot.MAINHAND || value == EquipmentSlot.OFFHAND);
                if(!ItemStack.matches(living.getItemBySlot(value), shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value)))
                {
                    ItemStack copy = shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value).copy();
                    if (value == EquipmentSlot.MAINHAND) {
                        playerEntity.getInventory().setItem(playerEntity.getInventory().selected, copy);
                    } else if (value == EquipmentSlot.OFFHAND) {
                        playerEntity.setItemSlot(EquipmentSlot.OFFHAND, copy);
                    } else if (value.getType() == EquipmentSlot.Type.ARMOR) {
                        playerEntity.setItemSlot(value, copy);
                    }
                }
            }
        }
        else
        {
            for(EquipmentSlot value : EquipmentSlot.values())
            {
                boolean shouldReset = reset && (value == EquipmentSlot.MAINHAND || value == EquipmentSlot.OFFHAND);
                if(!ItemStack.matches(living.getItemBySlot(value), shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value)))
                {
                    living.setItemSlot(value, shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value).copy());
                }
            }
        }

        if(player.isUsingItem())
        {
            if(player.getTicksUsingItem() == 1)
            {
                InteractionHand hand = player.getUsedItemHand();
                living.startUsingItem(hand);
                    }
        }
        else
        {
            living.stopUsingItem();
        }
    }
}
