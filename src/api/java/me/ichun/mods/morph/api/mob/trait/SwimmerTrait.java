package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeMod;

import java.util.Random;
import java.util.UUID;

public class SwimmerTrait extends Trait<SwimmerTrait>
        implements IEventBusRequired
{
    public Float swimMultiplier;
    public Float landMultiplier;
    public Boolean doNotAffectFog;

    public transient float lastStrength = 0F;
    public transient Random rand = new Random();
    public transient float lastSwimMul = 1F;
    public transient boolean doNotRemoveAttribute;

    public SwimmerTrait()
    {
        type = "traitSwimmer";
    }

    @Override
    public void addHooks()
    {
        if(!(doNotAffectFog != null && doNotAffectFog))
        {
            super.addHooks();
        }

        if(swimMultiplier == null)
        {
            swimMultiplier = 1F;
        }

        if(landMultiplier == null)
        {
            landMultiplier = 1F;
        }
    }

    @Override
    public void removeHooks()
    {
        super.removeHooks();

        if(!doNotRemoveAttribute)
        {
            setSwimAttribute(1F);
        }
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;

        if(swimMultiplier != 0F)
        {
            if(player.isInWaterOrBubble())
            {
                setSwimAttribute(1F + ((swimMultiplier - 1F) * strength));
            }
        }

        if(landMultiplier != 0F)
        {
            if(!player.isInWaterOrBubble() && player.onGround())
            {
                multiplyMotion(1F + ((landMultiplier - 1F) * strength));
            }
        }
    }

    @Override
    public void transitionalTick(SwimmerTrait prevTrait, float transitionProgress)
    {
        prevTrait.doNotRemoveAttribute = true;

        float swimMul = Mth.lerp(transitionProgress, prevTrait.swimMultiplier, swimMultiplier);
        if(swimMul != 0F)
        {
            if(player.isInWaterOrBubble())
            {
                setSwimAttribute(swimMul);
            }
        }

        float landMul = Mth.lerp(transitionProgress, prevTrait.landMultiplier, landMultiplier);
        if(landMul != 0F)
        {
            if(!player.isInWaterOrBubble() && player.onGround())
            {
                multiplyMotion(landMul);
            }
        }
    }

    public void setSwimAttribute(float mul)
    {
        if(player.level().isClientSide)
        {
            return;
        }

        final AttributeInstance playerAttribute = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        if(playerAttribute != null)
        {
            if(lastSwimMul != mul)
            {
                lastSwimMul = mul;

                rand.setSeed(Math.abs("MorphAttr".hashCode() * 1231543 + "traitSwimmer".hashCode() * 268));
                UUID uuid = UUID.nameUUIDFromBytes(("MorphSwim:" + player.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));

                //you can't reapply the same modifier, so lets remove it
                playerAttribute.removePermanentModifier(uuid);

                if(mul != 1F)
                {
                    playerAttribute.addPermanentModifier(new AttributeModifier(uuid, "MorphAttributeModifier:traitSwimmer", mul, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }
    }

    public void multiplyMotion(float mul)
    {
        player.setDeltaMovement(player.getDeltaMovement().multiply(mul, mul, mul));
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof SwimmerTrait)
        {
            return doNotAffectFog == ((SwimmerTrait)trait).doNotAffectFog;
        }
        return false;
    }

    @Override
    public SwimmerTrait copy()
    {
        SwimmerTrait trait = new SwimmerTrait();
        trait.swimMultiplier = this.swimMultiplier;
        trait.landMultiplier = this.landMultiplier;
        trait.doNotAffectFog = this.doNotAffectFog;
        return trait;
    }




}
