package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.api.biomass.BiomassUpgrade;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;

public class PacketUpdateBiomassUpgrades extends AbstractPacket
{
    public ArrayList<BiomassUpgrade> upgrades;

    public PacketUpdateBiomassUpgrades(){}

    public PacketUpdateBiomassUpgrades(Collection<BiomassUpgrade> upgrades)
    {
        this.upgrades = new ArrayList<>(upgrades);
    }

    @Override
    public void writeTo(FriendlyByteBuf buf)
    {
        buf.writeInt(upgrades.size());

        for(BiomassUpgrade upgrade : upgrades)
        {
            buf.writeNbt(upgrade.write(new CompoundTag()));
        }
    }

    @Override
    public void readFrom(FriendlyByteBuf buf)
    {
        upgrades = new ArrayList<>();

        int count = buf.readInt();
        for(int i = 0; i < count; i++)
        {
            upgrades.add(BiomassUpgrade.createFromNBT(buf.readNbt()));
        }
    }

    @Override
    public void process(NetworkEvent.Context context)
    {
        context.enqueueWork(() -> {
            Morph.eventHandlerClient.morphData.upgrades = upgrades;

            Morph.eventHandlerClient.hudHandler.updateBiomass(Morph.eventHandlerClient.morphData);
        });
    }
}
