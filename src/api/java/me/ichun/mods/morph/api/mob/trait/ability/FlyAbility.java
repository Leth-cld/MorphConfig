package me.ichun.mods.morph.api.mob.trait.ability;

import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.tags.FluidTags;

public class FlyAbility extends Ability<FlyAbility>
{
    public Boolean slowdownInWater;

    public transient float lastStrength = 0F;

    public FlyAbility()
    {
        type = "abilityFlight";
    }

    @Override
    public void tick(float strength)
    {
        if(strength == 1F)
        {
            //if ability is active but for some reason flight has been disabled, reenable it.
            if(!player.getAbilities().mayfly)
            {
                player.getAbilities().mayfly = true;

                player.onUpdateAbilities();
            }

            if(slowdownInWater != null && slowdownInWater && player.getAbilities().flying && !player.isCreative() && player.isEyeInFluid(FluidTags.WATER))
            {
                boolean hasSwim = false;

                for(Trait<?> trait : stateTraits)
                {
                    if("traitSwim".equals(trait.type))
                    {
                        hasSwim = true;
                        break;
                    }
                }

                if(!hasSwim)
                {
                    player.setDeltaMovement(player.getDeltaMovement().multiply(1D + (0.65D - 1D) * strength, 1D + (0.2D - 1D) * strength, 1D + (0.65D - 1D) * strength));
                }
            }

            player.fallDistance -= player.fallDistance * strength;
        }
        else if(lastStrength == 1F) //strength != 1F, but lastStrength == 1F. We're morphing out, disable flight.
        {
            if(!canPlayerFly() && player.getAbilities().mayfly)
            {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;

                player.onUpdateAbilities();
            }
        }

        lastStrength = strength;
    }

    @Override
    public void transitionalTick(FlyAbility prevTrait, float transitionProgress)
    {
        lastStrength = 1F;
        super.transitionalTick(prevTrait, transitionProgress);
    }

    @Override
    public FlyAbility copy()
    {
        FlyAbility ability = new FlyAbility();
        ability.slowdownInWater = this.slowdownInWater;
        return ability;
    }

    public boolean canPlayerFly()
    {
        return player.isSpectator() || player.isCreative();
    }
}
