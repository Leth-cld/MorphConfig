package me.ichun.mods.morph.common.morph;

import com.google.common.base.Splitter;
import com.mojang.authlib.GameProfile;
import me.ichun.mods.ichunutil.common.config.ConfigBase;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.api.IApi;
import me.ichun.mods.morph.api.biomass.BiomassUpgrade;
import me.ichun.mods.morph.api.biomass.BiomassUpgradeInfo;
import me.ichun.mods.morph.api.event.MorphEvent;
import me.ichun.mods.morph.api.mob.MobData;
import me.ichun.mods.morph.api.mob.nbt.NbtModifier;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.mob.trait.ability.Ability;
import me.ichun.mods.morph.api.morph.AttributeConfig;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.biomass.BiomassUpgradeHandler;
import me.ichun.mods.morph.common.biomass.Upgrades;
import me.ichun.mods.morph.common.mob.MobDataHandler;
import me.ichun.mods.morph.common.mob.TraitHandler;
import me.ichun.mods.morph.common.mode.MorphMode;
import me.ichun.mods.morph.common.mode.MorphModeType;
import me.ichun.mods.morph.common.morph.nbt.NbtHandler;
import me.ichun.mods.morph.common.morph.save.MorphSavedData;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.PacketAcquisition;
import me.ichun.mods.morph.common.packet.PacketMorphInfo;
import me.ichun.mods.morph.common.packet.PacketUpdateBiomassValue;
import me.ichun.mods.morph.common.packet.PacketUpdateMorph;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MorphHandler implements IApi
{
    public static final Splitter ON_SEMI_COLON = Splitter.on(";").trimResults().omitEmptyStrings();
    private static final ResourceLocation TEX_MORPH_SKIN = new ResourceLocation("morph", "textures/skin/morphskin.png"); //call the getter.

    private static final ArrayList<BiConsumer<LivingEntity, CompoundTag>> VARIANT_SPECIAL_TAG_SETTERS = Util.make(new ArrayList<>(), list -> {
        list.add((living, tag) -> {
            if(living instanceof AgeableMob) //ForcedAge is only called when eating, useless for keeping a mob a baby.
            {
                tag.putInt("Age", living.isBaby() ? -24000 : 0);
            }
        });
        list.add((living, tag) -> {
            if(living instanceof Panda)
            {
                Panda panda = (Panda)living;

                if(!panda.getMainGene().isRecessive()) //if main gene not recessive
                {
                    tag.putString("HiddenGene", "normal");
                }
                else if(panda.getMainGene() != panda.getHiddenGene())//main gene is recessive, check hidden gene, if not equal, panda is normal
                {
                    tag.putString("MainGene", "normal");
                    tag.putString("HiddenGene", "normal");
                }
            }
        });
        list.add((living, tag) -> {
            if(living instanceof WitherBoss)
            {
                int i = ((WitherBoss)living).getInvulnerableTicks();
                tag.putInt("Invul", i > 0 && (i > 80 || i / 5 % 2 != 1) ? 100000000 : 0);
            }
        });
        list.add((living, tag) -> {
            if(living instanceof NeutralMob)
            {
                tag.putInt("AngerTime", ((NeutralMob)living).isAngry() ? 100000000 : 0);
            }
        });
    });

    private static final ArrayList<BiConsumer<LivingEntity, CompoundTag>> VARIANT_SPECIAL_TAG_READERS = Util.make(new ArrayList<>(), list -> {
        list.add((living, tag) -> {
            if(living.level().isClientSide && living instanceof EnderDragon)
            {
                ((EnderDragon)living).setNoAI(false);
                ((EnderDragon)living).getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            }
        });
        list.add((living, tag) -> {
            if(living instanceof NeutralMob)
            {
                ((NeutralMob)living).setRemainingPersistentAngerTime(tag.getInt("AngerTime")); //TODO this is fixed in 1.17
            }
        });
    });

    private static final ArrayList<BiConsumer<LivingEntity, Player>> PLAYER_MORPH_SYNC_FUNCTIONS = Util.make(new ArrayList<>(), list -> {
        list.add((living, player) -> {
            if(Morph.configServer.silentMorphs)
            {
                living.setSilent(true);
            }
        });
        list.add((living, player) -> {
            if(living instanceof AgeableMob)
            {
                ((AgeableMob)living).setAge(living.isBaby() ? -24000 : 0);
            }
        });
        list.add((living, player) -> {
            if(living instanceof EnderDragon)
            {
                ((EnderDragon)living).dragonDeathTime = player.deathTime * 10;
            }
        });
        list.add((living, player) -> {
            if(living instanceof Mob)
            {
                Mob mob = (Mob)living;
                mob.setLeftHanded(player.getMainArm() == HumanoidArm.LEFT);
                mob.setAggressive(player.isUsingItem());
            }
        });
        list.add((living, player) -> {
            if(living instanceof NeutralMob)
            {
                ((NeutralMob)living).setRemainingPersistentAngerTime(((NeutralMob)living).isAngry() ? 1000 : 0);
            }
        });
    });

    private MorphMode currentMode;
    private MorphSavedData saveData;

    public void handleMurderEvent(ServerPlayer player, LivingEntity living)
    {
        currentMode.handleMurderEvent(player, living);
    }

    public void setMorphMode(MorphModeType type)
    {
        currentMode = type.createMode();
    }

    public void setSaveData(MorphSavedData data)
    {
        saveData = data;
    }

    public MorphSavedData getSaveData()
    {
        return saveData;
    }

    public PlayerMorphData getPlayerMorphData(Player player)
    {
        if(player.level().isClientSide)
        {
            return Morph.eventHandlerClient.morphData;
        }
        return saveData.playerMorphs.computeIfAbsent(player.getGameProfile().getId(), k -> new PlayerMorphData(player.getGameProfile().getId()));
    }

    //API overrides
    public static final MorphHandler INSTANCE = new MorphHandler();

    @Nonnull
    @Override
    public GameProfile getGameProfile(UUID uuid, String name)
    {
        GameProfile profile = EntityHelper.getGameProfile(uuid, name);
        if(profile.getName() == null)
        {
            profile = new GameProfile(uuid, "No Profile :(");
        }
        return profile;
    }

    @Override
    public String getMorphModeName()
    {
        return currentMode != null ? currentMode.getModeName() : IApi.super.getMorphModeName();
    }

    @Override
    public void spawnAnimation(Player player, LivingEntity living, boolean isMorphAcquisition)
    {
        Morph.channel.sendTo(new PacketAcquisition(player.getId(), living.getId(), isMorphAcquisition), PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player));
    }

    //Morph overrides
    @Override
    @Nonnull
    public MorphInfo getMorphInfo(Player player)
    {
        return player.getCapability(MorphInfo.CAPABILITY_INSTANCE).orElse(new MorphInfoImpl(player)); //if the player entity has no morph capabilities, just send a dummy morph info
    }

    @Override
    public boolean canShowMorphSelector(Player player)
    {
        return currentMode != null ? currentMode.canShowMorphSelector(player) : IApi.super.canShowMorphSelector(player);
    }

    @Override
    public boolean canMorph(Player player)
    {
        return currentMode != null && currentMode.canMorph(player);
    }

    @Override
    public boolean canAcquireMorph(Player player, LivingEntity living)
    {
        return currentMode != null ? currentMode.canAcquireMorph(player, living, createVariant(living)) : IApi.super.canAcquireMorph(player, living);
    }

    @Override
    @Nullable
    public MorphVariant createVariant(LivingEntity living)
    {
        boolean isPlayer = living instanceof Player;
        if(isPlayer)
        {
            MorphInfo morphInfo = getMorphInfo((Player)living);
            if(morphInfo.isMorphed())
            {
                if(morphInfo.getMorphProgress(1F) < 1F) //mid morph, no variant!
                {
                    return null;
                }

                living = morphInfo.getActiveMorphEntity(); //set the living into the morph the player is playing as right now.
            }
        }

        for(Pattern p : Morph.configServer.disabledMobsID)
        {
            Matcher m = p.matcher(ForgeRegistries.ENTITY_TYPES.getKey(living.getType()).toString());
            if(m.matches())
            {
                return null;
            }
        }

        MobData data = MobDataHandler.getMobData(living);
        if(data != null && data.disableAcquiringMorph != null && data.disableAcquiringMorph)
        {
            return null;
        }

        isPlayer = living instanceof Player;
        if(!living.getType().isSerializable() && !isPlayer)
        {
            return null;
        }

        MorphVariant variant = new MorphVariant(ForgeRegistries.ENTITY_TYPES.getKey(living.getType()));

        if(isPlayer)
        {
            variant.thisVariant = new MorphVariant.Variant();
            variant.thisVariant.playerUUID = ((Player)living).getGameProfile().getId();
        }
        else
        {
            CompoundTag tag = new CompoundTag();//TODO glint effect for ability??

            //Remove the item from the mob first as it affects their attributes
            EnumMap<EquipmentSlot, ItemStack> livingItems = new EnumMap<>(EquipmentSlot.class);
            for(EquipmentSlot value : EquipmentSlot.values())
            {
                ItemStack item = living.getItemBySlot(value);
                if(item != ItemStack.EMPTY)
                {
                    livingItems.put(value, item);
                    living.setItemSlot(value, ItemStack.EMPTY);
                }
            }
            living.func_241354_r_();

            //Write the supported attributes to our Morph NBT
            variant.writeSupportedAttributes(living);

            //Replace the mob's items
            livingItems.forEach(living::setItemSlot);
            living.func_241354_r_();

            //write the default info
            MorphVariant.writeDefaults(living, tag);

            living.writeAdditional(tag);
            //we have the default info

            //time to apply the NBT modifiers
            NbtModifier nbtModifier = NbtHandler.getModifierFor(living);
            nbtModifier.apply(tag);

            writeSpecialTags(living, tag);

            //Clean empty tags
            NbtHandler.removeEmptyCompoundTags(tag);

            variant.setLiving(tag);

            variant.thisVariant = new MorphVariant.Variant();
        }

        return variant;
    }

    private void writeSpecialTags(LivingEntity living, CompoundTag tag)
    {
        for(BiConsumer<LivingEntity, CompoundTag> consumer : VARIANT_SPECIAL_TAG_SETTERS)
        {
            consumer.accept(living, tag);
        }
    }

    @Override
    public boolean acquireMorph(ServerPlayer player, MorphVariant variant)
    {
        PlayerMorphData playerMorphData = MorphHandler.INSTANCE.getPlayerMorphData(player);
        if(!playerMorphData.containsVariant(variant))
        {
            for(Pattern p : Morph.configServer.disabledMobsID)
            {
                Matcher m = p.matcher(variant.id.toString());
                if(m.matches())
                {
                    return false;
                }
            }

            if(MinecraftForge.EVENT_BUS.post(new MorphEvent.Acquire(player, variant))) return false;

            MorphVariant parentVariant = playerMorphData.addVariant(variant);

            Morph.channel.sendTo(new PacketUpdateMorph(parentVariant.write(new CompoundTag())), player);

            saveData.markDirty();

            return true;
        }
        return false;
    }

    @Override
    public boolean morphTo(ServerPlayer player, MorphVariant variant)
    {
        MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(player);

        if(MinecraftForge.EVENT_BUS.post(new MorphEvent.Morph(player, variant))) return false;

        info.setNextState(new MorphState(variant, player), Math.max(1, currentMode.getMorphingDuration(player)));

        Morph.channel.sendTo(new PacketMorphInfo(player.getId(), info.write(new CompoundTag())), PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player));

        return true;
    }

    @Override
    public boolean demorph(ServerPlayer player)
    {
        MorphVariant variant = MorphVariant.createPlayerMorph(player.getGameProfile().getId(), true);
        variant.thisVariant.identifier = MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE;
        return morphTo(player, variant);
    }

    @Override
    public Map<ResourceLocation, AttributeConfig> getSupportedAttributes()
    {
        return Morph.configServer.supportedAttributesMap;
    }

    @Nullable
    @Override
    public LivingEntity getActiveMorphEntity(Player player)
    {
        return getMorphInfo(player).getActiveMorphEntity();
    }

    @Override
    public boolean isEntityAMorph(LivingEntity living)
    {
        return living != null && living.getPersistentData().contains(MorphVariant.NBT_PLAYER_ID);
    }

    @Nullable
    @Override
    public UUID getUuidOfPlayerForMorph(LivingEntity living)
    {
        return living != null && living.getPersistentData().contains(MorphVariant.NBT_PLAYER_ID) ? living.getPersistentData().getUUID(MorphVariant.NBT_PLAYER_ID) : null;
    }

    @Nonnull
    @Override
    public ResourceLocation getMorphSkinTexture()
    {
        return TEX_MORPH_SKIN;
    }

    @Override
    public List<BiConsumer<LivingEntity, Player>> getModPlayerMorphSyncConsumers()
    {
        return PLAYER_MORPH_SYNC_FUNCTIONS;
    }

    @Override
    public List<BiConsumer<LivingEntity, CompoundTag>> getVariantNbtTagSetters()
    {
        return VARIANT_SPECIAL_TAG_SETTERS;
    }

    @Override
    public List<BiConsumer<LivingEntity, CompoundTag>> getVariantNbtTagReaders()
    {
        return VARIANT_SPECIAL_TAG_READERS;
    }

    @Override
    public void registerMobData(@Nonnull ResourceLocation rl, @Nonnull MobData data)
    {
        MobDataHandler.registerMobData(rl, data);
    }

    @Override
    public void registerTrait(@Nonnull String type, @Nonnull Class<? extends Trait> clz)
    {
        TraitHandler.registerTrait(type, clz);
    }

    @Override
    public ArrayList<Trait<?>> getTraitsForVariant(MorphVariant variant, Player player)
    {
        return currentMode != null ? currentMode.getTraitsForVariant(player, variant) : IApi.super.getTraitsForVariant(variant, player);
    }

    @Override
    public boolean canUseAbility(Player player, Ability<?> ability)
    {
        return currentMode != null ? currentMode.canUseAbility(player, ability) : IApi.super.canUseAbility(player, ability);
    }

    //Biomass overrides
    @Override
    public boolean hasUnlockedBiomass(Player player)
    {
        return currentMode != null ? currentMode.hasUnlockedBiomass(player) : IApi.super.hasUnlockedBiomass(player);
    }

    @Override
    public boolean canAcquireBiomass(Player player, LivingEntity living)
    {
        return currentMode != null ? currentMode.canAcquireBiomass(player, living) : IApi.super.canAcquireBiomass(player, living);
    }

    @Override
    public double getBiomassAmount(Player player, LivingEntity living)
    {
        return currentMode != null ? currentMode.getBiomassAmount(player, living) :  IApi.super.getBiomassAmount(player, living);
    }

    @Nullable
    @Override
    public BiomassUpgradeInfo getBiomassUpgradeInfo(@Nullable String entityId, String id)
    {
        if(entityId == null)
        {
            return BiomassUpgradeHandler.BIOMASS_UPGRADES.get(id);
        }
        return null;
    }

    @Nullable
    @Override
    public BiomassUpgrade getBiomassUpgrade(Player player, String id)
    {
        return getPlayerMorphData(player).getBiomassUpgrade(id);
    }

    public double getBiomassUpgradeValue(Player player, String id)
    {
        BiomassUpgrade biomassUpgrade = getBiomassUpgrade(player, id);
        if(biomassUpgrade != null)
        {
            return biomassUpgrade.getValue();
        }
        return 0D;
    }

    public void setBiomassAmount(ServerPlayer player, double value)
    {
        PlayerMorphData playerMorphData = getPlayerMorphData(player);
        playerMorphData.biomass = value;
        saveData.markDirty();

        Morph.channel.sendTo(new PacketUpdateBiomassValue(playerMorphData.biomass), player);
    }

    public void addBiomassAmount(ServerPlayer player, double value)
    {
        PlayerMorphData playerMorphData = getPlayerMorphData(player);
        double cap = getBiomassUpgradeValue(player, Upgrades.ID_BIOMASS_CAPACITY) + getBiomassUpgradeValue(player, Upgrades.ID_BIOMASS_CRITICAL_CAPACITY);
        if(playerMorphData.biomass + value > cap)
        {
            value = cap - playerMorphData.biomass;
        }

        playerMorphData.biomass += value;

        saveData.markDirty();

        Morph.channel.sendTo(new PacketUpdateBiomassValue(playerMorphData.biomass), player);
    }

    //TODO a use biomass function

    public boolean isPlayerAllowed(@Nonnull Player player, @Nonnull ConfigBase.FilterType type, @Nonnull List<String> names)
    {
        return (type == ConfigBase.FilterType.ALLOW) == names.contains(player.getGameProfile().getName());
    }
}
