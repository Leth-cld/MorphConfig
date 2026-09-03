package me.ichun.mods.morph.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import me.ichun.mods.ichunutil.client.tracker.ClientEntityTracker;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public class EntityAcquisition extends Entity
{
    @Nonnull
    public LivingEntity livingOrigin;
    @Nonnull
    public LivingEntity livingAcquired;

    public boolean isMorphAcquisition;

    public ArrayList<Tendril> tendrils = new ArrayList<>();

    public int maxRequiredTendrils;
    public int age;

    public MorphRenderHandler.ModelRendererCapture acquiredCapture = new MorphRenderHandler.ModelRendererCapture();

    public EntityAcquisition(EntityType<?> entityTypeIn, Level levelIn)
    {
        super(entityTypeIn, levelIn);
        setInvisible(true);
        setInvulnerable(true);
        setId(ClientEntityTracker.getNextEntId());
    }

    public EntityAcquisition setTargets(@Nonnull LivingEntity origin, @Nonnull LivingEntity acquired, boolean isMorphAcquisition)
    {
        this.livingOrigin = origin;
        this.livingAcquired = acquired;
        this.isMorphAcquisition = isMorphAcquisition;

        syncWithOriginPosition();

        if(isMorphAcquisition)
        {
            tendrils.add(new Tendril(null).headTowards(getTargetPos(), false));
        }
        return this;
    }

    @Override
    public void tick()
    {
        super.tick();

        age++;

        if(!livingOrigin.isAlive() || !livingOrigin.level().dimension().equals(level().dimension())) //parent is "dead"
        {
            if(livingOrigin.isRemoved())
            {
                discard();
            }
        }
        else if(age > Morph.configClient.acquisitionTendrilMaxChild * 10 + 100) //probably too long, kill it off
        {
            discard();

            if(livingOrigin instanceof Player)
            {
                EntityBiomassAbility ability = Morph.EntityTypes.BIOMASS_ABILITY.create(level()).setInfo((Player)livingOrigin, 10, 0);
                ((ClientLevel)level()).addEntity(ability.getId(), ability);
            }
        }
        else //parent is "alive" and safe
        {
            this.setPos(livingOrigin.getX(), livingOrigin.getY() + (livingOrigin.getBbHeight() / 2D), livingOrigin.getZ());
            this.setRot(livingOrigin.getYRot(), livingOrigin.getXRot());

            boolean allDone = !tendrils.isEmpty();
            boolean anyRetracting = false;
            boolean anyNonRetracting = false;
            for(Tendril tendril : tendrils)
            {
                if(!tendril.isDone())
                {
                    allDone = false;
                    tendril.tick();

                    if(tendril.retract)
                    {
                        anyRetracting = true;
                    }
                    else
                    {
                        anyNonRetracting = true;
                    }
                }
                else
                {
                    anyRetracting = true;
                }
            }

            if(isMorphAcquisition)
            {
                if(tendrils.size() < 5 && age % 2 == 0)
                {
                    tendrils.add(new Tendril(null).headTowards(getTargetPos(), false));
                    allDone = false;//do not remove, we're not done yet
                }
                if(anyRetracting && anyNonRetracting)
                {
                    for(Tendril tendril : tendrils)
                    {
                        tendril.propagateRetractToChild();
                    }
                }
            }
            else
            {
                if(tendrils.size() < maxRequiredTendrils && !acquiredCapture.infos.isEmpty() && age % 3 == 0)
                {
                    tendrils.add(new Tendril(null).headTowards(getTargetPos(), true));
                    allDone = false;//do not remove, we're not done yet
                }
            }

            if(allDone)
            {
                discard();

                if(livingOrigin instanceof Player)
                {
                    EntityBiomassAbility ability = Morph.EntityTypes.BIOMASS_ABILITY.create(level()).setInfo((Player)livingOrigin, 10, 0);
                    ((ClientLevel)level()).addEntity(ability.getId(), ability);
                }
            }
        }
    }

    public Vec3 getTargetPos()
    {
        return livingAcquired.position().add(0D, livingAcquired.getBbHeight() / 2D, 0D);
    }

    @Override
    public AABB getBoundingBoxForCulling()
    {
        return livingOrigin.getBoundingBox().union(livingAcquired.getBoundingBox());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return livingOrigin.shouldRenderAtSqrDistance(distance) || livingAcquired.shouldRenderAtSqrDistance(distance);
    }

    @Override
    protected void defineSynchedData(){}

    @Override
    protected void readAdditionalSaveData(CompoundTag compound){}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound){}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void syncWithOriginPosition()
    {
        double height = (livingOrigin.getBbHeight() / 2D);
        this.moveTo(livingOrigin.getX(), livingOrigin.getY() + height, livingOrigin.getZ(), livingOrigin.getYRot(), livingOrigin.getXRot());
        this.xo = livingOrigin.xo;
        this.yo = livingOrigin.yo + height;
        this.zo = livingOrigin.zo;

        this.xo = livingOrigin.xo;
        this.yo = livingOrigin.yo + height;
        this.zo = livingOrigin.zo;
    }

    public class Tendril
    {
        @Nullable
        private Tendril parent; //if null, is the base tendril

        private Tendril child;
        private Vec3 offset;
        private float yaw;
        private float pitch;
        private float lastHeight = 1F;
        private float height = 1F;

        private float maxGrowth = 7F + (float)rand.nextGaussian() * 2F;

        private boolean retract;
        private int retractTime;

        private float prevRotateSpin;
        private float rotateSpin;
        private float spinFactor = (float)rand.nextGaussian() * 15F;

        public int depth = 0;

        private MorphRenderHandler.ModelRendererCapture capture;

        public Tendril(Tendril parent)
        {
            this.parent = parent;
            if(parent != null)
            {
                this.offset = parent.getReachOffset().subtract(getVectorForRotation(parent.pitch, parent.yaw).multiply(0.025F, 0.025F, 0.025F));
                this.depth = parent.depth + 1;
            }
            else
            {
                this.offset = new Vec3(0D, 0D, 0D);
            }
        }

        public Tendril headTowards(Vec3 pos, boolean rev)
        {
            float randGaus = 5F;
            if(parent != null)
            {
                yaw = parent.yaw;
                pitch = parent.pitch;

                Vec3 origin = parent.getReachCoord();
                double d0 = pos.getX() - origin.getX();
                double d1 = pos.getY() - origin.getY();
                double d2 = pos.getZ() - origin.getZ();

                float maxChange = isMorphAcquisition ? 30F : 60F;

                double dist = Mth.sqrt(d0 * d0 + d2 * d2);
                float newYaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                float newPitch = (float)(-(Mth.atan2(d1, dist) * (double)(180F / (float)Math.PI)));
                this.pitch = EntityHelper.updateRotation(this.pitch, newPitch, maxChange);
                this.yaw = EntityHelper.updateRotation(this.yaw, newYaw, maxChange);
            }
            else
            {
                yaw = (rev ? (livingOrigin.yBodyRot + 180F) : livingOrigin.yBodyRot) % 360F;
                pitch = 0;

                if(!isMorphAcquisition)
                {
                    randGaus = 30F;
                }

                yaw += (5F + 25F * rand.nextFloat()) * (rand.nextBoolean() ? 1F : -1F);
                pitch += (float)rand.nextGaussian() * randGaus;
            }

            yaw += (float)rand.nextGaussian() * randGaus;
            pitch += (float)rand.nextGaussian() * randGaus;

            return this;
        }

        public void tick()
        {
            lastHeight = height;
            if(retract)
            {
                retractTime++;
            }

            if(child != null)
            {
                child.tick();
            }
            else
            {
                float distToEnt = livingOrigin.getDistance(livingAcquired);
                if(!isMorphAcquisition)
                {
                    distToEnt *= 2F;
                }
                if(!retract)
                {
                    if(height < maxGrowth)
                    {
                        float maxTendrilGrowth = Math.max(0.0625F, distToEnt / Morph.configClient.acquisitionTendrilMaxChild + (float)rand.nextGaussian() * 0.125F); //in blocks
                        height += maxTendrilGrowth * 16F;
                        if(getReachCoord().distanceTo(getTargetPos()) < Math.max(0.3F, maxTendrilGrowth)) //close enough?
                        {
                            if(!isMorphAcquisition && age <= 10)
                            {
                                height -= maxTendrilGrowth * 16F; //wait for age to finish
                                return;
                            }

                            child = new Tendril(this);

                            Vec3 pos = getTargetPos();
                            Vec3 origin = getReachCoord();
                            double d0 = pos.getX() - origin.getX();
                            double d1 = pos.getY() - origin.getY();
                            double d2 = pos.getZ() - origin.getZ();

                            double dist = Mth.sqrt(d0 * d0 + d2 * d2);
                            child.yaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                            child.pitch = (float)(-(Mth.atan2(d1, dist) * (double)(180F / (float)Math.PI)));
                            child.lastHeight = child.height = (float)Mth.sqrt(d0 * d0 + d1 * d1 + d2 * d2) / 16F;

                            if(isMorphAcquisition)
                            {
                                child.capture = acquiredCapture;
                                acquiredCapture = null;
                            }
                            else if(!acquiredCapture.infos.isEmpty())
                            {
                                child.capture = new MorphRenderHandler.ModelRendererCapture();
                                int count = (int)Math.ceil(Math.max(acquiredCapture.infos.size() / 10F, 1));
                                for(int x = 0; x < count && !acquiredCapture.infos.isEmpty(); x++)
                                {
                                    int i = rand.nextInt(acquiredCapture.infos.size());
                                    child.capture.infos.add(acquiredCapture.infos.get(i));
                                    acquiredCapture.infos.remove(i);
                                    if(x > 0)
                                    {
                                        maxRequiredTendrils--;
                                    }
                                }
                            }

                            child.propagateRetractToParent();
                        }

                        if(!isMorphAcquisition && acquiredCapture.infos.isEmpty()) //oops we're out of blocks. retract
                        {
                            propagateRetractToParent();
                        }
                    }
                    else
                    {
                        child = new Tendril(this).headTowards(getTargetPos(), false);
                    }
                }
                else if(retractTime <= 3)
                {
                    float maxTendrilGrowth = Math.max(0.0625F, distToEnt / Morph.configClient.acquisitionTendrilMaxChild + (float)rand.nextGaussian() * 0.125F); //in blocks
                    if(getReachCoord().distanceTo(getTargetPos()) > Math.max(0.5F, maxTendrilGrowth))
                    {
                        Vec3 pos = getTargetPos();
                        Vec3 origin = getReachCoord();
                        double d0 = pos.getX() - origin.getX();
                        double d1 = pos.getY() - origin.getY();
                        double d2 = pos.getZ() - origin.getZ();

                        double dist = Mth.sqrt(d0 * d0 + d2 * d2);
                        yaw = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                        pitch = (float)(-(Mth.atan2(d1, dist) * (double)(180F / (float)Math.PI)));

                        height += maxTendrilGrowth * 16F;
                    }
                }
                else if(height > 0 && retractTime > 6)
                {
                    prevRotateSpin = rotateSpin;
                    if(capture != null)
                    {
                        rotateSpin += spinFactor;
                    }

                    float maxTendrilGrowth = Math.max(0.0625F, distToEnt / (Morph.configClient.acquisitionTendrilMaxChild * 2F) + (float)rand.nextGaussian() * 0.125F); //in blocks
                    height -= maxTendrilGrowth * 16F;
                    if(height <= 0)
                    {
                        height = 0;

                        if(parent != null)
                        {
                            parent.child = null; //remove ourselves.
                            if(capture != null)
                            {
                                parent.capture = capture;
                                parent.prevRotateSpin = prevRotateSpin;
                                parent.rotateSpin = rotateSpin;
                            }
                        }
                    }
                }
            }
        }

        public boolean isDone()
        {
            return retract && height <= 0F;
        }

        public void propagateRetractToParent()
        {
            retract = true;
            if(parent != null)
            {
                parent.propagateRetractToParent();
            }
        }

        public void propagateRetractToChild()
        {
            retract = true;
            if(child != null)
            {
                child.propagateRetractToChild();
            }
        }

        public Vec3 getReachOffset()
        {
            float growth = height / 16F;
            return offset.add(getVectorForRotation(pitch, yaw).multiply(growth, growth, growth));
        }

        public Vec3 getReachCoord()
        {
            return EntityAcquisition.this.position().add(getReachOffset());
        }

        public float getBbWidth(float partialTick)
        {
            float width = 1F + (0.2F * remainingDepth(partialTick));
            if(width > 3.5F)
            {
                width = 3.5F;
            }
            return width;
        }

        public float remainingDepth(float partialTick)
        {
            int depth = 0;
            Tendril aParent = this;
            Tendril aChild = aParent.child;

            while(aChild != null)
            {
                depth += Math.min((aChild.lastHeight + (aChild.height - aChild.lastHeight) * partialTick) / aChild.maxGrowth, 1F);

                aParent = aChild;
                aChild = aParent.child;
            }

            return depth;
        }

        public void createModelRenderer(ArrayList<ModelPart> renderers, float partialTick)
        {
            if(child != null)
            {
                child.createModelRenderer(renderers, partialTick);
            }

            float width = getBbWidth(partialTick);
            float halfWidth = width / 2F;
            float boxHeight = lastHeight + (height - lastHeight) * partialTick;
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("tendril",
                    CubeListBuilder.create().texOffs(rand.nextInt(8), rand.nextInt(8))
                            .addBox(-halfWidth, -halfWidth, 0F, width, width, boxHeight),
                    PartPose.offsetAndRotation(
                            (float)(offset.getX() * 16F),
                            (float)(offset.getY() * 16F),
                            (float)(offset.getZ() * 16F),
                            (float)Math.toRadians(pitch),
                            (float)Math.toRadians(-yaw),
                            0F));
            renderers.add(LayerDefinition.create(mesh, 64, 64).bakeRoot());
        }

        public void renderCapture(EntityAcquisition acquisition, PoseStack stack, VertexConsumer vertexBuilder, int light, int overlay, float partialTick)
        {
            if(child != null)
            {
                child.renderCapture(acquisition, stack, vertexBuilder, light, overlay, partialTick);
            }
            else if(capture != null) //only at tendril endpoints.
            {
                float renderHeight = lastHeight + (height - lastHeight) * partialTick;
                float heightOffset = renderHeight / 16F;
                Vec3 look = getVectorForRotation(pitch, yaw);
                Vec3 renderPoint = offset.add(look.multiply(heightOffset, heightOffset, heightOffset));

                float alpha = 1F;
                if(acquisition.livingOrigin == Minecraft.getInstance().getCameraEntity() && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON)
                {
                    alpha = Mth.clamp((depth + 1) / (float)Morph.configClient.acquisitionTendrilPartOpacity, 0F, 1F);
                }

                if(alpha > 0F)
                {
                    float scale;
                    double distToEnt = acquisition.livingOrigin.getDistance(acquisition.livingAcquired);
                    double distToRenderPoint = Mth.sqrt(acquisition.distanceToSqr(acquisition.position().add(renderPoint)));
                    if(distToEnt > 0D)
                    {
                        scale = (float)(Math.min(distToEnt, (distToRenderPoint + 0.5D)) / distToEnt); //+1 to make the render still show the entity slightly as it's being pulled in.
                    }
                    else
                    {
                        scale = 0F;
                    }

                    stack.pushPose();
                    stack.translate(renderPoint.getX(), renderPoint.getY(), renderPoint.getZ());
                    stack.scale(scale, scale, scale);
                    float rot = prevRotateSpin + (rotateSpin - prevRotateSpin) * partialTick;
                    stack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rot));
                    stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot));
                    stack.translate(0D, -(acquisition.livingAcquired.getBbHeight() / 2D), 0D);
                    if(isMorphAcquisition)
                    {
                        capture.render(stack, vertexBuilder, light, overlay, alpha);
                    }
                    else
                    {
                        for(MorphRenderHandler.ModelRendererCapture.CaptureInfo info : capture.infos)
                        {
                            PoseStack identityStack = new PoseStack();
                            stack.pushPose();
                            PoseStack.Pose e = RenderHelper.createInterimStackEntry(identityStack.last(), info.e, Mth.clamp(scale * 3F, 0F, 1F));
                            PoseStack.Pose last = stack.last();
                            last.pose().multiply(e.pose());
                            last.normal().multiply(e.normal());
                            info.createAndRender(stack, vertexBuilder, light, overlay, 1F, 1F, 1F, alpha);
                            stack.popPose();
                        }
                    }
                    stack.popPose();
                }
            }
        }
    }
}
