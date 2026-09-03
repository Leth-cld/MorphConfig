package me.ichun.mods.morph.api;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import me.ichun.mods.morph.api.biomass.BiomassUpgrade;
import me.ichun.mods.morph.api.biomass.BiomassUpgradeInfo;
import me.ichun.mods.morph.api.mob.MobData;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.mob.trait.ability.Ability;
import me.ichun.mods.morph.api.morph.AttributeConfig;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public interface IApi
{
    //Helpers
    @Nonnull
    default GameProfile getGameProfile(@Nullable UUID uuid, @Nullable String name) { return new GameProfile(uuid, name); }

    //Mod GameMode
    @Nonnull
    default String getMorphModeName() { return ""; }

    //Shared stuff
    default void spawnAnimation(Player player, LivingEntity living, boolean isMorphAcquisition) {}

    //Morph Stuff
    @Nullable
    default MorphInfo getMorphInfo(Player player)
    {
        return null;
    }

    @Nullable
    default LivingEntity getActiveMorphEntity(Player player) { return null; }

    default boolean isEntityAMorph(LivingEntity living) { return false; }

    @Nullable
    default UUID getUuidOfPlayerForMorph(LivingEntity living) { return null; }

    default boolean canShowMorphSelector(Player player) { return false; }

    default boolean canMorph(Player player) { return false; }

    default boolean canAcquireMorph(Player player, LivingEntity living)
    {
        //Checks if the situation is ideal to acquire the morph, not exclusively for if the player can morph to the entity. Cancel acquiring with the MorphEvent.CanAcquire event
        return false;
    }

    @Nullable
    default MorphVariant createVariant(LivingEntity living)
    {
        return null;
    }

    default boolean acquireMorph(ServerPlayer player, MorphVariant variant) { return false; }

    default boolean morphTo(ServerPlayer player, MorphVariant variant) { return false; }

    default boolean demorph(ServerPlayer player) { return true; }

    default Map<ResourceLocation, AttributeConfig> getSupportedAttributes() { return Collections.emptyMap(); }

    @Nullable
    default ResourceLocation getMorphSkinTexture() { return null; }

    default List<BiConsumer<LivingEntity, Player>> getModPlayerMorphSyncConsumers() { return ImmutableList.of(); }

    default List<BiConsumer<LivingEntity, CompoundTag>> getVariantNbtTagSetters() { return ImmutableList.of(); }

    default List<BiConsumer<LivingEntity, CompoundTag>> getVariantNbtTagReaders() { return ImmutableList.of(); }

    //Mob Data Stuff
    default void registerMobData(@Nonnull ResourceLocation rl, @Nonnull MobData data) {}

    default void registerTrait(@Nonnull String type, @Nonnull Class<? extends Trait> clz) {}

    @Nonnull
    default ArrayList<Trait<?>> getTraitsForVariant(MorphVariant variant, Player player) { return new ArrayList<>(); }

    default boolean canUseAbility(Player player, Ability<?> ability) { return false; }

    //Biomass Stuff
    default boolean hasUnlockedBiomass(Player player)
    {
        return false;
    }

    default boolean canAcquireBiomass(Player player, LivingEntity living)
    {
        return false;
    }

    default double getBiomassAmount(Player player, LivingEntity living) { return 0D; }


    //Biomass Upgrade Info
    @Nullable
    default BiomassUpgradeInfo getBiomassUpgradeInfo(String entityId, String id) { return null; }

    @Nullable
    default BiomassUpgrade getBiomassUpgrade(Player player, String id) { return null; } //null means no upgrade.
}
