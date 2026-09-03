package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.damagesource.DamageSource;

public class WaterSensitivityTrait extends Trait<WaterSensitivityTrait>
{
    public WaterSensitivityTrait()
    {
        type = "traitWaterSensitivity";
    }

    @Override
    public void tick(float strength)
    {
        if(!player.level().isClientSide && strength == 1F && player.isInWaterRainOrBubble())
        {
            player.hurt(player.damageSources().drown(), strength);
        }
    }

    @Override
    public WaterSensitivityTrait copy()
    {
        return new WaterSensitivityTrait();
    }
}
