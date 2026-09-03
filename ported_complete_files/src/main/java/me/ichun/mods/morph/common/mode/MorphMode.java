package me.ichun.mods.morph.common.mode;

import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.mob.trait.ability.Ability;
import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;

public interface MorphMode
{
    void handleMurderEvent(ServerPlayer player, LivingEntity living);

    boolean canShowMorphSelector(Player player);

    boolean canMorph(Player player); //if the player has the ability to morph and is not mid-morph

    boolean canAcquireMorph(Player player, LivingEntity living, @Nullable MorphVariant variant); //NOT FOR BLACKLISTING! Variant creation and acquire morph already checks the blacklist. This is for other reasons eg upgrade related stuff or range related stuff

    int getMorphingDuration(Player player);

    ArrayList<Trait<?>> getTraitsForVariant(Player player, MorphVariant variant); //create a copy of all the applicable traits and sets the player to the provided arg

    boolean canUseAbility(Player player, Ability<?> ability);

    boolean hasUnlockedBiomass(Player player);

    boolean canAcquireBiomass(Player player, LivingEntity living);

    double getBiomassAmount(Player player, LivingEntity living);

    String getModeName();
}
