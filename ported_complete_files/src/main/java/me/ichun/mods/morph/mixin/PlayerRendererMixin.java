package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.client.render.hand.HandHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void morph$renderRightHand(PoseStack poseStack, MultiBufferSource buffer, int light,
                                       AbstractClientPlayer player, CallbackInfo ci) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        PlayerModel<?> model = renderer.getModel();
        if (HandHandler.instance != null &&
                HandHandler.instance.renderHand(renderer, poseStack, buffer, light, player, model.rightArm, model.rightSleeve)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void morph$renderLeftHand(PoseStack poseStack, MultiBufferSource buffer, int light,
                                      AbstractClientPlayer player, CallbackInfo ci) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        PlayerModel<?> model = renderer.getModel();
        if (HandHandler.instance != null &&
                HandHandler.instance.renderHand(renderer, poseStack, buffer, light, player, model.leftArm, model.leftSleeve)) {
            ci.cancel();
        }
    }
}
