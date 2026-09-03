package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class PacketRequestMorphInfo extends AbstractPacket
{
    public UUID playerId;

    public PacketRequestMorphInfo(){}

    public PacketRequestMorphInfo(UUID id)
    {
        playerId = id;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeUniqueId(playerId);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        playerId = buf.readUniqueId();
    }

    @Override
    public void process(NetworkEvent.Context context)
    {
        context.enqueueWork(() -> {
            Player player = context.getSender().serverLevel().getPlayerByUuid(playerId);
            if(player != null && !player.isRemoved())
            {
                Morph.channel.sendTo(new PacketMorphInfo(player.getId(), MorphHandler.INSTANCE.getMorphInfo(player).write(new CompoundTag())), context.getSender());
            }
        });
    }
}
