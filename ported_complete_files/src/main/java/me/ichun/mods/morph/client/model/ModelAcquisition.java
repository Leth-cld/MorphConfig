package me.ichun.mods.morph.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.morph.client.entity.EntityAcquisition;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;

import java.util.ArrayList;

public class ModelAcquisition extends EntityModel<EntityAcquisition>
{
    public ModelAcquisition()
    {
        super(RenderType::getEntityTranslucentCull);
    }

    public void render(EntityAcquisition entity, float partialTick, PoseStack stack, VertexConsumer buffer, int light, int overlay)
    {
        boolean isFirstPerson = entity.livingOrigin == Minecraft.getInstance().getCameraEntity() && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;

        if(!entity.livingOrigin.isInvisible() || Morph.configServer.biomassSkinWhilstInvisible)
        {
            for(EntityAcquisition.Tendril tendril : entity.tendrils)
            {
                if(!tendril.isDone())
                {
                    ArrayList<ModelPart> modelRenderers = new ArrayList<>();
                    tendril.createModelRenderer(modelRenderers, partialTick);
                    tendril.renderCapture(entity, stack, buffer, light, overlay, partialTick);
                    for(int i = 0; i < modelRenderers.size(); i++)
                    {
                        ModelPart modelRenderer = modelRenderers.get(i);
                        float alpha = 1F;
                        if(isFirstPerson && Morph.configClient.acquisitionTendrilPartOpacity > 0)
                        {
                            alpha = Mth.clamp((modelRenderers.size() - i) / (float)Morph.configClient.acquisitionTendrilPartOpacity, 0F, 1F);
                        }
                        modelRenderer.render(stack, buffer, light, overlay, 1F, 1F, 1F, alpha);
                    }
                }
            }
        }
        else
        {
            for(EntityAcquisition.Tendril tendril : entity.tendrils)
            {
                if(!tendril.isDone())
                {
                    tendril.renderCapture(entity, stack, buffer, light, overlay, partialTick);
                }
            }
        }
    }

    @Override
    public void setupAnim(EntityAcquisition entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
    }

    @Override
    public void renderToBuffer(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha)
    {
    }
}
