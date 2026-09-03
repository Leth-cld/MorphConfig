package me.ichun.mods.morph.api.mob.trait.ability;

import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FlightFlapAbility extends Ability<FlightFlapAbility>
{
    public Boolean resetVerticalVelocity;
    public Double velocityAdded;
    public Integer flapLimit;
    public Boolean slowdownInWater;

    public transient double flaps;
    public transient boolean keyHeld;
    public transient boolean wasOnGround; //we tick after the entity ticks so the ent tick already sets jump when we're on the ground

    public FlightFlapAbility()
    {
        type = "abilityFlightFlap";
    }

    @Override
    public void addHooks()
    {
        super.addHooks();
        if(velocityAdded == null)
        {
            velocityAdded = 0.42D;
        }
    }

    @Override
    public void tick(float strength)
    {
        combinedTick(strength, velocityAdded);
    }

    @Override
    public void transitionalTick(FlightFlapAbility prevTrait, float transitionProgress)
    {
        flaps = prevTrait.flaps;
        combinedTick(1F, Mth.lerp(transitionProgress, prevTrait.velocityAdded, velocityAdded));
    }

    private void combinedTick(float strength, double velocityToAdd)
    {
        if(player.level().isClientSide)
        {
            clientTick(strength, velocityToAdd);
        }
        else
        {
            //To prevent the client from being kicked for "flying".
            ServerPlayer serverPlayer = (ServerPlayer)player;
            try
            {
                java.lang.reflect.Field field = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
                field.setAccessible(true);
                field.setInt(serverPlayer.connection, 0);
            }
            catch (ReflectiveOperationException ignored)
            {
                // The anti-flying counter is an implementation detail; if mappings change,
                // simply leave it alone rather than failing the morph tick.
            }
        }

        if(slowdownInWater != null && slowdownInWater && player.isEyeInFluid(FluidTags.WATER))
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

    @OnlyIn(Dist.CLIENT)
    private void clientTick(float strength, double velocityToAdd)
    {
        if(!keyHeld && Minecraft.getInstance().options.keyJump.isDown()) //hit jump key
        {
            boolean canFlap = (flapLimit == null || flaps < flapLimit) && !wasOnGround;

            //taken from LivingEntity.livingTick, onGround replaced with canFlap
            if(!player.getAbilities().flying && canFlap)
            {
                double d7;
                if (player.isInLava()) {
                    d7 = player.getFluidHeight(FluidTags.LAVA);
                } else {
                    d7 = player.getFluidHeight(FluidTags.WATER);
                }

                boolean flag = player.isInWater() && d7 > 0.0D;
                double d8 = player.getFluidJumpThreshold();
                if (!flag || !(d7 > d8))
                {
                    if (!player.isInLava() || !(d7 > d8))
                    {
                        jump(strength, velocityToAdd);
                    }
                    else
                    {
                        player.setDeltaMovement(player.getDeltaMovement().add(0.0D, (double)0.04F * player.getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()).getValue(), 0.0D));
                    }
                }
                else
                {
                    player.setDeltaMovement(player.getDeltaMovement().add(0.0D, (double)0.04F * player.getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()).getValue(), 0.0D));
                }
            }
        }
        keyHeld = Minecraft.getInstance().options.keyJump.isDown();
        wasOnGround = player.onGround();

        //reset the flap count if the player is on the ground.
        if(player.onGround())
        {
            flaps = 0;
        }
    }

    public void jump(float strength, double velocityToAdd)
    {
        //Mostly taken from entity.jump
        double d = velocityToAdd * getJumpFactor() * strength;
        if(player.hasEffect(MobEffects.JUMP))
        {
            d += 0.1D * (player.getEffect(MobEffects.JUMP).getAmplifier() + 1);
        }

        Vec3 motion = player.getDeltaMovement();
        if(resetVerticalVelocity != null && resetVerticalVelocity)
        {
            player.setDeltaMovement(motion.x, d, motion.z);
        }
        else
        {
            player.setDeltaMovement(motion.add(0D, d, 0D));
        }

        player.hasImpulse = true;
        //we're intentionally not triggering the Forge event, we're not doing a fresh jump.
    }

    public float getJumpFactor() {
        float f = player.level().getBlockState(player.blockPosition()).getBlock().getJumpFactor();
        float f1 = player.level().getBlockState(BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.5000001D, player.getZ())).getBlock().getJumpFactor();
        return (double)f == 1.0D ? f1 : f;
    }

    @Override
    public FlightFlapAbility copy()
    {
        FlightFlapAbility ability = new FlightFlapAbility();
        ability.resetVerticalVelocity = this.resetVerticalVelocity;
        ability.velocityAdded = this.velocityAdded;
        ability.flapLimit = this.flapLimit;
        ability.slowdownInWater = this.slowdownInWater;
        return ability;
    }
}
