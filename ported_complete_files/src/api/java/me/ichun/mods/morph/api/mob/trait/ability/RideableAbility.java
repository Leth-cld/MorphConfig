package me.ichun.mods.morph.api.mob.trait.ability;

import me.ichun.mods.morph.api.mob.trait.IEventBusRequired;
import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RideableAbility extends Ability<RideableAbility>
        implements IEventBusRequired
{
    public Boolean requiresSaddle;

    public transient float lastStrength;

    public RideableAbility()
    {
        type = "abilityRideable";
    }

    @Override
    public void tick(float strength)
    {
        if(lastStrength == 1F && strength != 1F || player.isCrouching()) //demorphing
        {
            player.ejectPassengers();
        }
        else if(livingInstance != null)
        {
            for(Entity passenger : player.getPassengers())
            {
                livingInstance.positionRider(passenger);
            }
        }
        lastStrength = strength;
    }

    @Override
    public void transitionalTick(RideableAbility prevTrait, float transitionProgress)
    {
        lastStrength = 1F;
        super.transitionalTick(prevTrait, transitionProgress);
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof RideableAbility)
        {
            return requiresSaddle == ((RideableAbility)trait).requiresSaddle;
        }
        return false;
    }

    @Override
    public RideableAbility copy()
    {
        RideableAbility ability = new RideableAbility();
        ability.requiresSaddle = this.requiresSaddle;
        return ability;
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event)
    {
        if(lastStrength == 1F && event.getEntity().getVehicle() == null && event.getTarget() == player && event.getTarget().getPassengers().isEmpty())
        {
            if((!(requiresSaddle != null && requiresSaddle) || livingInstance instanceof Saddleable && ((Saddleable)livingInstance).isSaddled()) && event.getEntity().startRiding(player))
            {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);

                //We have to tell the ridden player that someone's riding 'em
                if(!event.getTarget().level().isClientSide)
                {
                    ServerPlayer targetPlayer = (ServerPlayer)event.getTarget();
                    targetPlayer.connection.sendPacket(new ClientboundSetPassengersPacket(targetPlayer));
                }
            }
        }
    }
}
