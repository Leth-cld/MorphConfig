package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.client.entity.EntityAcquisition;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class PacketAcquisition extends AbstractPacket
{
    public int originId;
    public int acquiredId;

    public boolean isMorphAcquisition;

    public PacketAcquisition(){}

    public PacketAcquisition(int originId, int acquiredId, boolean isMorphAcquisition)
    {
        this.originId = originId;
        this.acquiredId = acquiredId;
        this.isMorphAcquisition = isMorphAcquisition;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeInt(originId);
        buf.writeInt(acquiredId);
        buf.writeBoolean(isMorphAcquisition);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        originId = buf.readInt();
        acquiredId = buf.readInt();
        isMorphAcquisition = buf.readBoolean();
    }

    @Override
    public void process(NetworkEvent.Context context)
    {
        if(isMorphAcquisition && (Morph.configClient.acquisitionPlayAnimation == 1 || Morph.configClient.acquisitionPlayAnimation == 3)|| !isMorphAcquisition && Morph.configClient.acquisitionPlayAnimation >= 2)
        {
            handleClient(context);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context)
    {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Entity origin = mc.level().getEntity(originId);
            Entity acquired = mc.level().getEntity(acquiredId);

            if(origin instanceof LivingEntity && acquired instanceof LivingEntity)
            {
                LivingEntity livingAcquired = (LivingEntity)acquired;
                EntityAcquisition ent = Morph.EntityTypes.ACQUISITION.create(mc.level()).setTargets((LivingEntity)origin, livingAcquired, isMorphAcquisition);
                mc.level().addEntity(ent.getId(), ent);
                if(livingAcquired != mc.player)
                {
                    livingAcquired.discard();
                }
                else
                {
                    EntityHelper.faceEntity(livingAcquired, origin, 360F, 360F);
                }

                //block the hurt overlay/death rotation
                acquired.moveTo(acquired.getX(), acquired.getY(), acquired.getZ(), acquired.getYRot(), acquired.getXRot());
                livingAcquired.yBodyRotO = livingAcquired.yBodyRot;
                livingAcquired.yHeadRotO = livingAcquired.getYHeadRot();
                livingAcquired.yRotO = livingAcquired.getYRot();
                livingAcquired.xRotO = livingAcquired.getXRot();
                livingAcquired.deathTime = 0;
                livingAcquired.hurtTime = 0;
            }
        });
    }
}
