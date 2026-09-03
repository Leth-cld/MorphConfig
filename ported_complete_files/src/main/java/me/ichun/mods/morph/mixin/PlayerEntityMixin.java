package me.ichun.mods.morph.mixin;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin
{
    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void getHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir)
    {
        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
        if(info.isMorphed())
        {
            cir.setReturnValue(info.getHurtSound(source));
        }

    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void getDeathSound(CallbackInfoReturnable<SoundEvent> cir)
    {
        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
        if(info.isMorphed())
        {
            cir.setReturnValue(info.getDeathSound());
        }

    }

    @Inject(method = "getFallDamageSound", at = @At("HEAD"), cancellable = true)
    private void getFallDamageSound(int height, CallbackInfoReturnable<SoundEvent> cir)
    {
        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
        if(info.isMorphed())
        {
            cir.setReturnValue(info.getFallSound(height));
        }

    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir)
    {
        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)(Object)this);
        if(info.isMorphed())
        {
            cir.setReturnValue(info.getActiveMorphSizeByPose(pose));
        }

    }
}
