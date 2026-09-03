package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class UndeadTrait extends Trait<UndeadTrait>
        implements IEventBusRequired
{
    public transient float lastStrength = 0F;
    public transient int air = -100;

    public UndeadTrait()
    {
        type = "traitUndead";
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;

        if(lastStrength == 1F)
        {
            //Remove potions
            MobEffectInstance potion = player.getEffect(MobEffects.REGENERATION);
            if(potion != null)
            {
                player.removeEffect(MobEffects.REGENERATION);
            }
            potion = player.getEffect(MobEffects.POISON);
            if(potion != null)
            {
                player.removeEffect(MobEffects.POISON);
            }


            //Breathe underwater
            if(air == -100)
            {
                air = player.getAirSupply();
            }

            //if the player is in water, add air
            if (player.isEyeInFluid(FluidTags.WATER))
            {
                //Taken from determineNextAir in LivingEntity
                air = Math.min(air + 4, player.getMaxAir());
                player.setAirSupply(air);
            }

        }
    }

    @Override
    public UndeadTrait copy()
    {
        return new UndeadTrait();
    }

    @SubscribeEvent
    public void onPotionApplicable(MobEffectEvent.Applicable event)
    {
        if(lastStrength == 1F && event.getEntity() == player && (event.getEffectInstance().getEffect() == MobEffects.REGENERATION || event.getEffectInstance().getEffect() == MobEffects.POISON))
        {
            event.setResult(Event.Result.DENY);
        }
    }
}
