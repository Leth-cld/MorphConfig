package me.ichun.mods.morph.api.mob.trait.ability;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ClimbAbility extends Ability<ClimbAbility>
{
    public ClimbAbility()
    {
        type = "abilityClimb";
    }

    @Override
    public void tick(float strength)
    {
        if(player.horizontalCollision)
        {
            Vec3 motion = player.getDeltaMovement();
            double motionX = Mth.clamp(motion.x, -0.15F, 0.15F);
            double motionZ = Mth.clamp(motion.z, -0.15F, 0.15F);
            double motionY = motion.y;
            if(player.isCrouching())
            {
                motionY -= motionY * strength;
            }
            else
            {
                motionY -= (motionY - 0.2D) * strength;
            }
            player.setDeltaMovement(motionX, motionY, motionZ);

            player.fallDistance -= player.fallDistance * strength;
        }
    }

    @Override
    public ClimbAbility copy()
    {
        return new ClimbAbility();
    }
}
