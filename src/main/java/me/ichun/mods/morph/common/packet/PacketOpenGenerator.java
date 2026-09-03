package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.client.gui.mob.WorkspaceMobData;
import me.ichun.mods.morph.client.gui.nbt.WorkspaceNbt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class PacketOpenGenerator extends AbstractPacket
{
    public int targetId;

    public PacketOpenGenerator(){}

    public PacketOpenGenerator(int targetId)
    {
        this.targetId = targetId;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeInt(targetId);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        targetId = buf.readInt();
    }

    @Override
    public void process(NetworkEvent.Context context)
    {
        handleClient(context);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context)
    {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if(targetId >= 0)
            {
                Entity target = mc.level().getEntity(targetId);

                if(target instanceof LivingEntity && !(target instanceof Player))
                {
                    LivingEntity living = (LivingEntity)target;

                    Minecraft.getInstance().displayGuiScreen(new WorkspaceNbt(Minecraft.getInstance().currentScreen, living));
                }
            }
            else
            {
                Minecraft.getInstance().displayGuiScreen(new WorkspaceMobData(Minecraft.getInstance().currentScreen));
            }
        });
    }
}
