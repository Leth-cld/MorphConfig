package me.ichun.mods.morph.client.entity;

import me.ichun.mods.ichunutil.client.tracker.ClientEntityTracker;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class EntityBiomassAbility extends Entity
{
    @Nonnull
    public Player player;

    public int fadeTime;
    public int solidTime;
    public int age;
    public MorphRenderHandler.ModelRendererCapture capture = new MorphRenderHandler.ModelRendererCapture();

    public EntityBiomassAbility(EntityType<?> entityTypeIn, Level levelIn)
    {
        super(entityTypeIn, levelIn);
        setInvisible(true);
        setInvulnerable(true);
        setId(ClientEntityTracker.getNextEntId());
    }

    public EntityBiomassAbility setInfo(@Nonnull Player player, int fadeTime, int solidTime)
    {
        this.player = player;
        this.fadeTime = fadeTime;
        this.solidTime = solidTime;

        syncWithOriginPosition();

        return this;
    }

    @Override
    public void tick()
    {
        super.tick();

        age++;

        if(!player.isAlive() || !player.level().dimension().equals(level().dimension())) //parent is "dead"
        {
            if(player.isRemoved())
            {
                discard();
            }
        }
        else if(age > (fadeTime * 2) + solidTime)
        {
            discard();
        }
        else //parent is "alive" and safe
        {
            this.setPos(player.getX(), player.getY() + (player.getBbHeight() / 2D), player.getZ());
            this.setRot(player.getYRot(), player.getXRot());
        }
    }

    @Override
    public AABB getBoundingBoxForCulling()
    {
        return player.getBoundingBox();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return player.shouldRenderAtSqrDistance(distance);
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

    public float getSkinAlpha(float partialTick)
    {
        float alpha;
        if(age < fadeTime)
        {
            alpha = EntityHelper.sineifyProgress(Mth.clamp((age + partialTick) / fadeTime, 0F, 1F));
        }
        else if(age >= fadeTime + solidTime)
        {
            alpha = EntityHelper.sineifyProgress(1F - Mth.clamp((age - (fadeTime + solidTime) + partialTick) / fadeTime, 0F, 1F));
        }
        else
        {
            alpha = 1F;
        }
        return alpha;
    }

    public void syncWithOriginPosition()
    {
        this.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        this.xo = player.xo;
        this.yo = player.yo;
        this.zo = player.zo;

        this.xo = player.xo;
        this.yo = player.yo;
        this.zo = player.zo;
    }
}
