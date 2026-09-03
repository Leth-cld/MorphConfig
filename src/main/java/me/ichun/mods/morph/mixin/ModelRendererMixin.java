package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void morph$capture(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha,
                               CallbackInfo ci) {
        if (MorphRenderHandler.currentCapture == null) {
            return;
        }

        ModelPart self = (ModelPart) (Object) this;
        poseStack.pushPose();
        self.translateAndRotate(poseStack);
        MorphRenderHandler.currentCapture.capture(self, poseStack);

        for (ModelPart child : self.children.values()) {
            child.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        }

        poseStack.popPose();
        ci.cancel();
    }
}
