package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketUpdateMorph extends AbstractPacket //Only used for the full list of variants. Singular morphs should not use this!
{
    public CompoundTag nbt;

    public PacketUpdateMorph(){}

    public PacketUpdateMorph(CompoundTag nbt)
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
        MorphVariant variant = MorphVariant.createFromNBT(nbt);

        context.enqueueWork(() -> Morph.eventHandlerClient.updateMorph(variant));
    }
}
