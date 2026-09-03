package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.phys.Vec3;

public class SinkTrait extends Trait<SinkTrait>
{
    public transient boolean isInWater;

    public SinkTrait()
    {
        type = "traitSink";
    }

    @Override
    public void tick(float strength)
    {
        if(player.isInWater())
        {
            Vec3 motion = player.getDeltaMovement();
            if(player.horizontalCollision)
            {
                player.setDeltaMovement(motion.x, 0.07D * strength, motion.z);
            }
            else if(motion.y > -0.07D && !player.getAbilities().flying)
            {
                player.setDeltaMovement(motion.add(0D, -0.07D * strength, 0D));
            }
        }
        else if(isInWater && !player.getAbilities().flying)
        {
            player.setDeltaMovement(player.getDeltaMovement().add(0D, 0.32D * strength, 0D));
        }
        isInWater = player.isInWater();
    }

    @Override
    public SinkTrait copy()
    {
        return new SinkTrait();
    }
}
