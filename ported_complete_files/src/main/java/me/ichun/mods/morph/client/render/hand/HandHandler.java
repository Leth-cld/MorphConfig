package me.ichun.mods.morph.client.render.hand;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.api.client.hand.HandInfo;
import me.ichun.mods.ichunutil.api.common.PlacementCorrector;
import me.ichun.mods.ichunutil.client.model.util.ModelHelper;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
import me.ichun.mods.ichunutil.common.util.IOUtil;
import me.ichun.mods.morph.api.event.MorphLoadResourceEvent;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.resource.ResourceHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@OnlyIn(Dist.CLIENT)
public final class HandHandler
{
    private static final HashMap<Class<? extends EntityModel>, HandInfo> MODEL_HAND_INFO = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static HandHandler instance;

    private MorphInfo lastMorphInfo;
    private float lastPartialTick;

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) //if we're getting the event, the config has already assigned us;
    {
        Minecraft mc = Minecraft.getInstance();
        if(!mc.player.isRemoved()) //we need to cache this as the hand may be rendered even in the death screen.
        {
            lastMorphInfo = MorphHandler.INSTANCE.getMorphInfo(mc.player);
            lastPartialTick = event.getPartialTick();
        }
    }

    //Returns true if we have to override and render the hand.
    public boolean renderHand(PlayerRenderer playerRenderer, PoseStack stack, MultiBufferSource buffer, int light, AbstractClientPlayer player, ModelPart arm, ModelPart armwear)
    {
        //Check if this is the player, and we have the player's morph info.
        if(player == Minecraft.getInstance().getCameraEntity() && lastMorphInfo != null && !MorphRenderHandler.isRenderingMorph)
        {
            MorphInfo info = lastMorphInfo;
            float partialTick = lastPartialTick;
            float skinAlpha = info.getMorphSkinAlpha(partialTick);
            if(skinAlpha > 0F || info.isMorphed()) // if we're supposed to override the hand render
            {
                Minecraft mc = Minecraft.getInstance();

                ModelPart[] handParts = null;
                PoseStack[] stacks = null;
                ResourceLocation texture = null;

                HumanoidArm handSide = ((PlayerModel<?>) playerRenderer.getModel()).rightArm != arm ? HumanoidArm.LEFT : HumanoidArm.RIGHT; //default to right arm instead any mods override the player model

                float morphProg = info.getMorphProgress(partialTick);
                float transitionProg = info.getTransitionProgressSine(partialTick);
                if(morphProg < 1F && transitionProg < 1F) //still morphing, transition may be required.
                {
                    if(transitionProg <= 0F)
                    {
                        LivingEntity livingInstance = info.prevState.getEntityInstance(mc.player.level(), mc.player);
                        EntityRenderer entRenderer = playerRenderer.getEntityRenderDispatcher().getRenderer(livingInstance);
                        if(entRenderer instanceof LivingEntityRenderer)
                        {
                            stack.pushPose();
                            stack.translate(0D, -500D, 0D);
                            MorphRenderHandler.renderLiving(entRenderer, livingInstance, stack, buffer, light, partialTick);
                            stack.popPose();

                            LivingEntityRenderer livingRenderer = (LivingEntityRenderer)entRenderer;
                            EntityModel entityModel = livingRenderer.getModel();

                            HandInfo handInfo = HandHandler.getHandInfo(entityModel.getClass());
                            if(handInfo != null)
                            {
                                renderModelPreHandModelRendererCopy(entityModel, livingInstance);

                                handParts = handInfo.getHandParts(handSide, entityModel);
                                stacks = handInfo.getPlacementCorrectors(handSide);
                                texture = entRenderer.getTextureLocation(livingInstance);
                            }
                        }
                    }
                    else
                    {
                        ModelPart[] prevHandParts = null;
                        PoseStack[] prevStacks = null;

                        ModelPart[] nextHandParts = null;
                        PoseStack[] nextStacks = null;

                        LivingEntity prevInstance = info.prevState.getEntityInstance(mc.player.level(), mc.player);
                        EntityRenderer prevRenderer = playerRenderer.getEntityRenderDispatcher().getRenderer(prevInstance);

                        LivingEntity nextInstance = info.nextState.getEntityInstance(mc.player.level(), mc.player);
                        EntityRenderer nextRenderer = playerRenderer.getEntityRenderDispatcher().getRenderer(nextInstance);

                        stack.pushPose();
                        stack.translate(0D, -500D, 0D); //maybe I should just set scale to 0?
                        if(prevRenderer instanceof LivingEntityRenderer)
                        {
                            MorphRenderHandler.renderLiving(prevRenderer, prevInstance, stack, buffer, light, partialTick);

                            LivingEntityRenderer livingRenderer = (LivingEntityRenderer)prevRenderer;
                            EntityModel entityModel = livingRenderer.getModel();

                            HandInfo handInfo = HandHandler.getHandInfo(entityModel.getClass());
                            if(handInfo != null)
                            {
                                renderModelPreHandModelRendererCopy(entityModel, prevInstance);

                                prevHandParts = handInfo.getHandParts(handSide, entityModel);
                                prevStacks = handInfo.getPlacementCorrectors(handSide);
                            }
                        }
                        if(nextRenderer instanceof LivingEntityRenderer)
                        {
                            MorphRenderHandler.renderLiving(nextRenderer, nextInstance, stack, buffer, light, partialTick);

                            LivingEntityRenderer livingRenderer = (LivingEntityRenderer)nextRenderer;
                            EntityModel entityModel = livingRenderer.getModel();

                            HandInfo handInfo = HandHandler.getHandInfo(entityModel.getClass());
                            if(handInfo != null)
                            {
                                renderModelPreHandModelRendererCopy(entityModel, nextInstance);

                                nextHandParts = handInfo.getHandParts(handSide, entityModel);
                                nextStacks = handInfo.getPlacementCorrectors(handSide);
                            }
                        }
                        stack.popPose();

                        if(prevHandParts != null || nextHandParts != null)
                        {
                            if(prevHandParts == null)
                            {
                                prevHandParts = new ModelPart[nextHandParts.length];
                                prevStacks = new PoseStack[nextHandParts.length];
                            }
                            if(nextHandParts == null)
                            {
                                nextHandParts = new ModelPart[prevHandParts.length];
                                nextStacks = new PoseStack[prevHandParts.length];
                            }
                            if(prevHandParts.length < nextHandParts.length)
                            {
                                prevHandParts = Arrays.copyOf(prevHandParts, nextHandParts.length);
                                prevStacks = Arrays.copyOf(prevStacks, nextHandParts.length);
                            }
                            if(nextHandParts.length < prevHandParts.length)
                            {
                                nextHandParts = Arrays.copyOf(nextHandParts, prevHandParts.length);
                                nextStacks = Arrays.copyOf(nextStacks, prevHandParts.length);
                            }

                            //at this point the arrays have the same length
                            handParts = new ModelPart[prevHandParts.length];
                            stacks = new PoseStack[prevHandParts.length];

                            for(int i = 0; i < handParts.length; i++)
                            {
                                Project.Part oldPart = ModelHelper.createPartFor(prevHandParts[i], true);
                                Project.Part newPart = ModelHelper.createPartFor(nextHandParts[i], true);

                                ModelHelper.matchBoxAndChildrenCount(oldPart, newPart);
                                ModelHelper.matchBoxAndChildrenCount(newPart, oldPart);

                                handParts[i] = ModelHelper.createModelRenderer(ModelHelper.createInterimPart(oldPart, newPart, transitionProg), true);

                                if(prevStacks[i] != null || nextStacks[i] != null)
                                {
                                    PoseStack.Entry interimStackEntry = RenderHelper.createInterimStackEntry(prevStacks[i] != null ? prevStacks[i].last() : (new PoseStack()).last(), nextStacks[i] != null ? nextStacks[i].last() : (new PoseStack()).last(), transitionProg);
                                    PoseStack interimStack = new PoseStack();
                                    PoseStack.Entry last = interimStack.last();
                                    last.pose().multiply(interimStackEntry.pose());
                                    last.normal().multiply(interimStackEntry.normal());
                                    stacks[i] = interimStack;
                                }
                                else
                                {
                                    stacks[i] = null;
                                }
                            }
                        }
                    }
                }
                else //morph completed, just use nextState's entity instance
                {
                    LivingEntity livingInstance = info.isMorphed() ? info.nextState.getEntityInstance(mc.player.level(), mc.player) : mc.player;
                    EntityRenderer entRenderer = playerRenderer.getEntityRenderDispatcher().getRenderer(livingInstance);
                    if(entRenderer instanceof LivingEntityRenderer)
                    {
                        stack.pushPose();
                        stack.translate(0D, -500D, 0D);
                        MorphRenderHandler.renderLiving(entRenderer, livingInstance, stack, buffer, light, partialTick);
                        stack.popPose();

                        LivingEntityRenderer livingRenderer = (LivingEntityRenderer)entRenderer;
                        EntityModel entityModel = livingRenderer.getModel();

                        HandInfo handInfo = HandHandler.getHandInfo(entityModel.getClass());
                        if(handInfo != null)
                        {
                            renderModelPreHandModelRendererCopy(entityModel, livingInstance);

                            handParts = handInfo.getHandParts(handSide, entityModel);
                            stacks = handInfo.getPlacementCorrectors(handSide);
                            texture = entRenderer.getTextureLocation(livingInstance);
                        }
                    }

                    if(entRenderer instanceof PlayerRenderer && livingInstance instanceof AbstractClientPlayer)//this must be a player
                    {
                        MorphRenderHandler.isRenderingMorph = true;
                        PlayerRenderer morphPlayerRenderer = (PlayerRenderer)entRenderer;
                        if(handSide == HumanoidArm.LEFT)
                        {
                            morphPlayerRenderer.renderLeftHand(stack, buffer, light, (AbstractClientPlayer)livingInstance);
                        }
                        else
                        {
                            morphPlayerRenderer.renderRightHand(stack, buffer, light, (AbstractClientPlayer)livingInstance);
                        }
                        MorphRenderHandler.isRenderingMorph = false;

                        if(handParts != null && skinAlpha > 0F) //let's check the handParts just in case BipedModel.json is missing
                        {
                            renderModelPartsWithTexture(handParts, stacks, stack, buffer.getBuffer(RenderType.getEntityTranslucent(MorphHandler.INSTANCE.getMorphSkinTexture())), light, skinAlpha);
                        }
                        return true; //we're done here, the player render does the work for us
                    }
                }

                if(handParts != null)
                {
                    if(texture != null)
                    {
                        renderModelPartsWithTexture(handParts, stacks, stack, buffer.getBuffer(RenderType.getEntityTranslucent(texture)), light, 1F);
                    }

                    if(skinAlpha > 0F)
                    {
                        renderModelPartsWithTexture(handParts, stacks, stack, buffer.getBuffer(RenderType.getEntityTranslucent(MorphHandler.INSTANCE.getMorphSkinTexture())), light, skinAlpha);
                    }
                }
                return true;
            }
        }

        return false;
    }

    private static void renderModelPreHandModelRendererCopy(EntityModel entityModel, LivingEntity livingInstance) {
        entityModel.setupAnim(livingInstance, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static void renderModelPartsWithTexture(ModelPart[] parts, PoseStack[] stacks, PoseStack stack,
                                                     VertexConsumer buffer, int light, float alpha) {
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            if (part == null || !part.visible) {
                continue;
            }

            float oldXRot = part.xRot;
            part.xRot = 0F;
            stack.pushPose();
            if (stacks != null && stacks[i] != null) {
                PlacementCorrector.multiplyStackWithStack(stack, stacks[i]);
            }
            part.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, alpha);
            stack.popPose();
            part.xRot = oldXRot;
        }
    }

}
