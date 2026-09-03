package me.ichun.mods.morph.mixin;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin
{
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
