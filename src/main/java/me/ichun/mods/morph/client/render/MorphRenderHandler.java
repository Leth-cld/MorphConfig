package me.ichun.mods.morph.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.model.util.ModelHelper;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.ichunutil.common.module.tabula.project.Project;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class MorphRenderHandler
{
    private static float playerShadowSize = -1F;
    private static boolean changedShadowSize = false;

    public static ModelRendererCapture currentCapture = null; //Are we capturing ModelPart renders?

    public static boolean isRenderingMorph = false;
    public static boolean denyRenderNameplate = false;

    //TODO add stats eg total morphs/total biomass/biomass lost etc
    //TODO config that forces nameplate rendering when morphed
    //TODO test the nameplate for named mods, maybe add a config to rename the mob
    public static void renderMorphInfo(Player player, MorphInfoImpl info, PoseStack stack, MultiBufferSource buffer, int light, float partialTick)
    {
        isRenderingMorph = true;

        float morphProgress = info.getMorphProgress(partialTick);

        if(morphProgress < 1F) //still morphing
        {
            float skinProg = 1F;
            float transitionProgress = info.getTransitionProgressSine(partialTick);
            if(transitionProgress <= 0F)
            {
                LivingEntity entInstance = info.prevState.getEntityInstance(player.level(), player);
                UUID morphUniqueId = entInstance.getUUID();
                entInstance.setUUID(player.getUUID());
                MorphState.syncEntityWithPlayer(entInstance, player);
                renderLiving(info.prevState, entInstance, stack, buffer, light, partialTick);
                entInstance.setUUID(morphUniqueId);
                skinProg = EntityHelper.sineifyProgress(morphProgress / 0.125F);
            }
            else if(transitionProgress >= 1F)
            {
                LivingEntity entInstance = info.nextState.getEntityInstance(player.level(), player);
                UUID morphUniqueId = entInstance.getUUID();
                entInstance.setUUID(player.getUUID());
                MorphState.syncEntityWithPlayer(entInstance, player);
                renderLiving(info.nextState, entInstance, stack, buffer, light, partialTick);
                entInstance.setUUID(morphUniqueId);
                skinProg = 1F - EntityHelper.sineifyProgress((morphProgress - 0.875F) / 0.125F);
            }

            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F); //player usually default to 0.0F;
            renderTransitionState(player, info, stack, buffer, light, overlay, partialTick, transitionProgress, skinProg);
        }
        else //has completed morph
        {
            LivingEntity entInstance = info.nextState.getEntityInstance(player.level(), player);
            UUID morphUniqueId = entInstance.getUUID();
            entInstance.setUUID(player.getUUID());
            MorphState.syncEntityWithPlayer(entInstance, player);
            entInstance.setUUID(morphUniqueId);
            renderLiving(info.nextState, entInstance, stack, buffer, light, partialTick);
        }

        isRenderingMorph = false;
    }

    private static void renderLiving(MorphState state, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick) //also captures the shadow size
    {
        renderLiving(state, living, stack, buffer, light, partialTick, false);
    }

    private static void renderLiving(MorphState state, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick, boolean forceDuringInvisibility) //also captures the shadow size
    {
        EntityRenderer<? super LivingEntity> livingRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
        if(livingRenderer != null)
        {
            renderLiving(livingRenderer, living, stack, buffer, light, partialTick, forceDuringInvisibility);
            if (living instanceof Mob && living.isBaby()) //Checked in EntityRendererManager
            {
                state.renderedShadowSize = livingRenderer.shadowRadius * 0.5F;
            }
            else
            {
                state.renderedShadowSize = livingRenderer.shadowRadius;
            }
        }
    }

    public static void renderLiving(EntityRenderer<? super LivingEntity> renderer, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick, boolean forceDuringInvisibility)
    {
        boolean isInvisible = living.isInvisible();
        if(forceDuringInvisibility && isInvisible)
        {
            living.setInvisible(false);
        }
        renderLiving(renderer, living, stack, buffer, light, partialTick);
        if(forceDuringInvisibility && isInvisible)
        {
            living.setInvisible(true);
        }
    }

    public static void renderLiving(EntityRenderer<? super LivingEntity> renderer, LivingEntity living, PoseStack stack, MultiBufferSource buffer, int light, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        float yaw = Mth.lerp(partialTick, living.yRotO, living.getYRot());
        stack.pushPose();
        if(living instanceof EnderDragonEntity)
        {
            stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180F));
        }
        renderer.render(living, yaw, partialTick, stack, buffer, light);
        stack.popPose();

    }

    public static void renderTransitionState(Player player, MorphInfoImpl info, PoseStack stack, MultiBufferSource buffer, int light, int overlay, float partialTick, float transitionProgress, float skinAlpha)
    {
        if(info.transitionState == null)
        {
            info.transitionState = new MorphTransitionState();
        }

        info.transitionState.renderTransitionState(player, info, stack, buffer, light, overlay, partialTick, transitionProgress, skinAlpha);
    }

    public static void restoreShadowSize(PlayerRenderer renderer)
    {
        if(playerShadowSize == -1F)
        {
            playerShadowSize = renderer.shadowRadius;
        }

        if(changedShadowSize)
        {
            changedShadowSize = false;
            renderer.shadowRadius = playerShadowSize;
        }
    }

    public static void setShadowSize(PlayerRenderer renderer, MorphInfo info, float partialTick)
    {
        float morphProgress = info.getMorphProgress(partialTick);
        if(morphProgress < 1F) //midmorph
        {
            float prevSize = info.prevState.renderedShadowSize;
            float nextSize = info.nextState.renderedShadowSize;

            renderer.shadowRadius = prevSize + (nextSize - prevSize) * info.getTransitionProgressSine(partialTick);
        }
        else
        {
            renderer.shadowRadius = info.nextState.renderedShadowSize;
        }

        changedShadowSize = true;
    }

    public static class MorphTransitionState
    {
        protected ModelRendererCapture prevModel;
        protected ModelRendererCapture nextModel;

        public void renderTransitionState(Player player, MorphInfo info, PoseStack stack, MultiBufferSource buffer, int light, int overlay, float partialTick, float transitionProgress, float skinAlpha)
        {
            if(transitionProgress <= 0F)
            {
                if(prevModel == null)
                {
                    currentCapture = prevModel = new ModelRendererCapture();
                }
                else
                {
                    currentCapture = prevModel;
                    currentCapture.infos.clear();
                }

                LivingEntity livingInstance = info.prevState.getEntityInstance(player.level(), player);

                renderLiving(info.prevState, livingInstance, stack, buffer, light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                currentCapture = null; //reset before we do anything else

                prevModel.render(null, buffer, light, overlay, skinAlpha);
            }
            else if(transitionProgress >= 1F)
            {
                if(nextModel == null)
                {
                    currentCapture = nextModel = new ModelRendererCapture();
                }
                else
                {
                    currentCapture = nextModel;
                    currentCapture.infos.clear();
                }

                LivingEntity livingInstance = info.nextState.getEntityInstance(player.level(), player);

                renderLiving(info.nextState, livingInstance, stack, buffer, light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                currentCapture = null; //reset before we do anything else

                nextModel.render(null, buffer, light, overlay, skinAlpha);
            }
            else
            {
                denyRenderNameplate = true;
                if(prevModel == null)
                {
                    currentCapture = prevModel = new ModelRendererCapture();
                }
                else
                {
                    currentCapture = prevModel;
                    currentCapture.infos.clear();
                }

                LivingEntity prevLivingInstance = info.prevState.getEntityInstance(player.level(), player);

                renderLiving(info.prevState, prevLivingInstance, stack, buffer, light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                if(nextModel == null)
                {
                    currentCapture = nextModel = new ModelRendererCapture();
                }
                else
                {
                    currentCapture = nextModel;
                    currentCapture.infos.clear();
                }

                LivingEntity nextLivingInstance = info.nextState.getEntityInstance(player.level(), player);

                renderLiving(info.nextState, nextLivingInstance, stack, buffer, light, partialTick, Morph.configServer.biomassSkinWhilstInvisible);

                currentCapture = null; //reset before we do anything else
                denyRenderNameplate = false;

                stack.pushPose();
                stack.translate(0F, prevLivingInstance.getBbHeight() / 2F, 0F);
                PoseStack.Entry prevMid = stack.last();
                stack.popPose();

                stack.pushPose();
                stack.translate(0F, nextLivingInstance.getBbHeight() / 2F, 0F);
                PoseStack.Entry nextMid = stack.last();
                stack.popPose();

                ModelRendererCapture transitionCapture = new ModelRendererCapture();
                transitionCapture.infos = prevModel.combineTowards(prevMid, nextMid, nextModel, transitionProgress);

                transitionCapture.render(null, buffer, light, overlay, skinAlpha);
            }
        }
    }

    public static class ModelRendererCapture
    {
        private final HashMap<ModelPart, CaptureInfo.ModelPart> modelToPart = new HashMap<>();

        public ArrayList<CaptureInfo> infos = new ArrayList<>();

        public void capture(ModelPart renderer, PoseStack stack)
        {
            if(modelToPart.containsKey(renderer))
            {
                infos.add(new CaptureInfo(stack.last(), modelToPart.get(renderer)));
            }
            else
            {
                Project.Part part = ModelHelper.createPartFor(renderer, false);
                part.rotPX = part.rotPY = part.rotPZ = part.rotAX = part.rotAY = part.rotAZ = 0F;
                part.children.clear();
                CaptureInfo.ModelPart modelPart = new CaptureInfo.ModelPart(part);
                infos.add(new CaptureInfo(stack.last(), modelPart));
                modelToPart.put(renderer, modelPart);

                for(Project.Part.Box box : part.boxes) //to prevent z-fighting
                {
                    box.expandX += 0.002F;
                    box.expandY += 0.002F;
                    box.expandZ += 0.002F;
                }
            }
        }

        public ArrayList<CaptureInfo> combineTowards(PoseStack.Entry prevMid, PoseStack.Entry nextMid, ModelRendererCapture other, float transitionProgress)
        {
            ArrayList<CaptureInfo> prevInfo = infos;
            ArrayList<CaptureInfo> nextInfo = other.infos;

            //Fill with empty parts first
            while(prevInfo.size() < nextInfo.size())
            {
                Project.Part part = new Project.Part(null, 0);
                part.boxes.clear();
                prevInfo.add(new CaptureInfo(prevMid, new CaptureInfo.ModelPart(part)));
            }

            while(nextInfo.size() < prevInfo.size())
            {
                Project.Part part = new Project.Part(null, 0);
                part.boxes.clear();
                nextInfo.add(new CaptureInfo(nextMid, new CaptureInfo.ModelPart(part)));
            }

            ArrayList<CaptureInfo> transitionInfos = new ArrayList<>();

            //sync up the box count
            for(int i = 0; i < prevInfo.size(); i++)
            {
                Project.Part oldPart = prevInfo.get(i).modelPart.part;
                Project.Part newPart = nextInfo.get(i).modelPart.part;

                ModelHelper.matchBoxesCount(oldPart, newPart);
                ModelHelper.matchBoxesCount(newPart, oldPart);

                transitionInfos.add(new CaptureInfo(RenderHelper.createInterimStackEntry(prevInfo.get(i).e, nextInfo.get(i).e, transitionProgress), new CaptureInfo.ModelPart(ModelHelper.createInterimPart(oldPart, newPart, transitionProgress))));
            }

            return transitionInfos;
        }

        public void render(PoseStack stack, MultiBufferSource buffer, int light, int overlay, float skinAlpha)
        {
            render(stack, buffer.getBuffer(RenderType.getEntityTranslucent(MorphHandler.INSTANCE.getMorphSkinTexture())), light, overlay, skinAlpha);
        }

        public void render(PoseStack stack, VertexConsumer vertexBuilder, int light, int overlay, float skinAlpha)
        {
            PoseStack newStack = stack != null ? stack : new PoseStack();
            for(CaptureInfo info : infos)
            {
                newStack.pushPose();
                PoseStack.Entry entLast = newStack.last();
                PoseStack.Entry correctorLast = info.e;

                entLast.pose().multiply(correctorLast.pose());
                entLast.normal().multiply(correctorLast.normal());

                info.createAndRender(newStack, vertexBuilder, light, overlay, 1F, 1F, 1F, skinAlpha);
                newStack.popPose();
            }
        }

        public static class CaptureInfo
        {
            public final PoseStack.Entry e;
            public final ModelPart modelPart;

            public CaptureInfo(PoseStack.Entry e, ModelPart modelPart) {
                this.e = e;
                this.modelPart = modelPart;
            }

            public void createAndRender(PoseStack stack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha)
            {
                if(this.modelPart.model == null)
                {
                    this.modelPart.model = ModelHelper.createModelRenderer(this.modelPart.part);
                }

                this.modelPart.model.render(stack, buffer, light, overlay, red, green, blue, alpha);
            }

            private static class ModelPart
            {
                public final Project.Part part;
                public ModelPart model;

                private ModelPart(Project.Part part)
                {
                    this.part = part;
                }
            }
        }
    }
}
