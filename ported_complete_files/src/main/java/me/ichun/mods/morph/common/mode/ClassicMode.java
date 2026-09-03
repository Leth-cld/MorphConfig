package me.ichun.mods.morph.common.mode;

import me.ichun.mods.morph.api.event.MorphEvent;
import me.ichun.mods.morph.api.mob.MobData;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.mob.trait.ability.Ability;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.mob.MobDataHandler;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class ClassicMode implements MorphMode
{
    @Override
    public void handleMurderEvent(ServerPlayer player, LivingEntity living)
    {
        if(canMorph(player))
        {
            MorphVariant variant = MorphHandler.INSTANCE.createVariant(living);
            if(canAcquireMorph(player, living, variant)) // we can morph to it
            {
                MorphHandler.INSTANCE.acquireMorph(player, variant);

                MorphHandler.INSTANCE.spawnAnimation(player, living, true);
            }
        }
    }

    @Override
    public boolean canShowMorphSelector(Player player)
    {
        return MorphHandler.INSTANCE.isPlayerAllowed(player, Morph.configServer.selectorFilterType, Morph.configServer.selectorFilterNames);
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
    public boolean canAcquireMorph(Player player, LivingEntity living, @Nullable MorphVariant variant) //variant should be the MorphVariant of the EntityLiving we're trying to acquire
    {
        if(variant == null || MinecraftForge.EVENT_BUS.post(new MorphEvent.CanAcquire(player, variant)) || !MorphHandler.INSTANCE.isPlayerAllowed(player, Morph.configServer.morphFilterType, Morph.configServer.morphFilterNames))
        {
            return false;
        }

        PlayerMorphData playerMorphData = MorphHandler.INSTANCE.getPlayerMorphData(player);

        return !playerMorphData.containsVariant(variant);
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
            ArrayList<String> upgradedTraits = new ArrayList<>();
            for(Trait<?> trait : mobData.traits)
            {
                if(trait != null && !Morph.configServer.disabledTraits.contains(trait.type) && (trait.upgradeFor == null || Morph.configServer.classicUpgradeTraits))
                {
                    traits.add(trait.copy());
                    if(trait.upgradeFor != null)
                    {
                        upgradedTraits.add(trait.upgradeFor);
                    }
                }
            }

            for(String upgradedTrait : upgradedTraits)
            {
                traits.removeIf(trait -> trait.type.equals(upgradedTrait));
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
        return false; // no biomass capabilities in classic.
    }

    @Override
    public double getBiomassAmount(Player player, LivingEntity living)
    {
        return 0D; // no biomass capabilities in classic.
    }

    @Override
    public String getModeName()
    {
        return "classic";
    }
}
