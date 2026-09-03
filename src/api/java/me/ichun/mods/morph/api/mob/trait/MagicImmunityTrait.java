package me.ichun.mods.morph.api.mob.trait;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MagicImmunityTrait extends Trait<MagicImmunityTrait>
        implements IEventBusRequired
{
    public transient float lastStrength = 0F;

    public MagicImmunityTrait()
    {
        type = "traitImmunityMagic";
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;
    }

    @Override
    public MagicImmunityTrait copy()
    {
        return new MagicImmunityTrait();
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        if(lastStrength == 1F && event.getEntity() == player && event.getSource().is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO))
        {
            event.setCanceled(true);
        }
    }
}
