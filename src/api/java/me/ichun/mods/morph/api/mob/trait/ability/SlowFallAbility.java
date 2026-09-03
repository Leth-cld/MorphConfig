package me.ichun.mods.morph.api.mob.trait.ability;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SlowFallAbility extends Ability<SlowFallAbility>
{
    public Double velocityMultiplier;
    public Boolean resetFallDistance;

    public SlowFallAbility()
    {
        type = "abilitySlowFall";
    }

    @Override
    public void addHooks()
    {
        if(velocityMultiplier == null)
        {
            velocityMultiplier = 0.6D;
        }
    }

    @Override
    public void tick(float strength)
    {
        if(!player.isCrouching())
        {
            setDeltaMovement(velocityMultiplier, strength);
            if(resetFallDistance != null && resetFallDistance)
            {
                setFallDistance(strength);
            }
        }
    }

    @Override
    public void transitionalTick(SlowFallAbility prevTrait, float transitionProgress)
    {
        if(!player.isCrouching())
        {
            double velo = Mth.lerp(transitionProgress, prevTrait.velocityMultiplier != null ? prevTrait.velocityMultiplier : 0.6D, velocityMultiplier);
            setDeltaMovement(velo, 1F);
            if(!(prevTrait.resetFallDistance != null && prevTrait.resetFallDistance) && resetFallDistance != null && resetFallDistance)
            {
                setFallDistance(transitionProgress);
            }
            else if(prevTrait.resetFallDistance != null && prevTrait.resetFallDistance && !(resetFallDistance != null && resetFallDistance))
            {
                setFallDistance(1F - transitionProgress);
            }
            else if(prevTrait.resetFallDistance != null && prevTrait.resetFallDistance && resetFallDistance != null && resetFallDistance)
            {
                setFallDistance(1F);
            }
        }
    }

    private void setDeltaMovement(double veloMulti, float strength)
    {
        Vec3 motion = player.getDeltaMovement();
        if(!player.onGround() && motion.y < 0.0D)
        {
            player.setDeltaMovement(motion.multiply(1D, 1D + ((veloMulti - 1D) * strength), 1D));
        }
    }

    private void setFallDistance(float strength)
    {
        player.fallDistance -= player.fallDistance * strength;
    }

    @Override
    public SlowFallAbility copy()
    {
        SlowFallAbility ability = new SlowFallAbility();
        ability.velocityMultiplier = this.velocityMultiplier;
        ability.resetFallDistance = this.resetFallDistance;
        return ability;
    }
}
