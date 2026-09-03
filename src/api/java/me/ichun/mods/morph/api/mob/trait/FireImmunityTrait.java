package me.ichun.mods.morph.api.mob.trait;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FireImmunityTrait extends Trait<FireImmunityTrait>
        implements IEventBusRequired
{
    public transient float lastStrength = 0F;

    public FireImmunityTrait()
    {
        type = "traitImmunityFire";
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;

        if(lastStrength == 1F)
        {
            player.clearFire();
        }
    }

    @Override
    public FireImmunityTrait copy()
    {
        return new FireImmunityTrait();
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        if(lastStrength == 1F && event.getEntity() == player && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE))
        {
            event.setCanceled(true);
        }
    }
}
