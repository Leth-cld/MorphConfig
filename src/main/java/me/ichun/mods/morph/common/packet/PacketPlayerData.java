package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketPlayerData extends AbstractPacket
{
    public CompoundTag nbt;

    public PacketPlayerData(){}

    public PacketPlayerData(CompoundTag nbt)
    {
        this.nbt = nbt;
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeNbt(nbt);
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        nbt = buf.readNbt();
    }

    @Override
    public void process(NetworkEvent.Context context)
    {
        PlayerMorphData playerMorphData = new PlayerMorphData();
        playerMorphData.read(nbt);

        context.enqueueWork(() -> {
            Morph.eventHandlerClient.setPlayerMorphData(playerMorphData);
        });
    }
}
