package me.ichun.mods.morph.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.client.entity.EntityBiomassAbility;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderEntityBiomassAbility extends EntityRenderer<EntityBiomassAbility>
{
    protected RenderEntityBiomassAbility(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityBiomassAbility ability, float entityYaw, float partialTick, PoseStack stack, MultiBufferSource buffer, int light)
    {
        if(ability.player.isRemoved())
        {
            return; //no render
        }

        MorphInfoImpl info = (MorphInfoImpl)MorphHandler.INSTANCE.getMorphInfo(ability.player);
        boolean isFirstPerson = ability.player == Minecraft.getInstance().getCameraEntity() && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
        if(isFirstPerson)
        {
            if(info.entityBiomassAbility == null || info.entityBiomassAbility.getSkinAlpha(partialTick) < ability.getSkinAlpha(partialTick))
            {
                info.entityBiomassAbility = ability; //will be removed by MorphInfo when the entity is invalid/removed
            }
            return; //no render
        }

        LivingEntity activeLiving = info.getActiveAppearanceEntity(partialTick);
        if(activeLiving != null)
        {
            EntityRenderer<? super LivingEntity> renderer = entityRenderDispatcher.getRenderer(activeLiving);
            if(renderer != null)
            {
                float alpha = ability.getSkinAlpha(partialTick);

                MorphRenderHandler.denyRenderNameplate = true;
                stack.pushPose();
                MorphRenderHandler.renderLiving(renderer, activeLiving, stack, buffer, entityRenderDispatcher.getPackedLight(activeLiving, partialTick), partialTick);
                stack.popPose();

                MorphRenderHandler.currentCapture = ability.capture;
                MorphRenderHandler.currentCapture.infos.clear();

                MorphRenderHandler.renderLiving(renderer, activeLiving, new PoseStack(), buffer, entityRenderDispatcher.getPackedLight(activeLiving, partialTick), partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                MorphRenderHandler.currentCapture = null;
                MorphRenderHandler.denyRenderNameplate = false;

                ability.capture.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, alpha);
            }
        }
    }

    @Override
    public boolean shouldRender(EntityBiomassAbility ability, Frustum camera, double camX, double camY, double camZ)
    {
        ability.syncWithOriginPosition();
        return super.shouldRender(ability, camera, camX, camY, camZ);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBiomassAbility ability)
    {
        return MorphHandler.INSTANCE.getMorphSkinTexture();
    }

    public static class RenderFactory implements EntityRendererProvider<EntityBiomassAbility>
    {
        @Override
        public EntityRenderer<EntityBiomassAbility> create(EntityRendererProvider.Context context)
        {
            return new RenderEntityBiomassAbility(context);
        }
    }
}
