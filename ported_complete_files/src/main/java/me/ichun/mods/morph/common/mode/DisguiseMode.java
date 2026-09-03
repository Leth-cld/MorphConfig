package me.ichun.mods.morph.common.mode;

import me.ichun.mods.morph.api.mob.MobData;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.mob.trait.ability.Ability;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.mob.MobDataHandler;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class DisguiseMode implements MorphMode
{
    @Override
    public void handleMurderEvent(ServerPlayer player, LivingEntity living)
    {
        if(canMorph(player))
        {
            MorphVariant variant = MorphHandler.INSTANCE.createVariant(living);

            MorphHandler.INSTANCE.morphTo(player, variant);
        }
    }

    @Override
    public boolean canShowMorphSelector(Player player)
    {
        return false;
    }

    @Override
    public boolean canMorph(Player player)
    {
        if(!MorphHandler.INSTANCE.isPlayerAllowed(player, Morph.configServer.morphFilterType, Morph.configServer.morphFilterNames))
        {
            return false;
        }

        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(player);
        if(info.isMorphed())
        {
            if(info.getMorphProgress(1F) < 1F) //mid morphing
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canAcquireMorph(Player player, LivingEntity living, @Nullable MorphVariant variant)
    {
        return false;
    }

    @Override
    public int getMorphingDuration(Player player)
    {
        return Morph.configServer.morphTime;
    }

    @Override
    public ArrayList<Trait<?>> getTraitsForVariant(Player player, MorphVariant variant)
    {
        ArrayList<Trait<?>> traits = new ArrayList<>();

        MobData mobData = MobDataHandler.getMobData(variant.id);

        if(mobData != null && mobData.traits != null)
        {
            for(Trait<?> trait : mobData.traits)
            {
                if(trait != null && !Morph.configServer.disabledTraits.contains(trait.type) && trait.upgradeFor == null) //no trait upgrades in classic
                {
                    traits.add(trait.copy());
                }
            }

            for(Trait<?> trait : traits)
            {
                trait.player = player;
                trait.stateTraits = traits;
            }
        }

        return traits;
    }

    @Override
    public boolean canUseAbility(Player player, Ability<?> ability)
    {
        return true;
    }

    @Override
    public boolean hasUnlockedBiomass(Player player)
    {
        return false;
    }

    @Override
    public boolean canAcquireBiomass(Player player, LivingEntity living)
    {
        return false;
    }

    @Override
    public double getBiomassAmount(Player player, LivingEntity living)
    {
        return 0;
    }

    @Override
    public String getModeName()
    {
        return "disguise";
    }
}
