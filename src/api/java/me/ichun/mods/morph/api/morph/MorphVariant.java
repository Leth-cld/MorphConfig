package me.ichun.mods.morph.api.morph;

import com.mojang.authlib.GameProfile;
import me.ichun.mods.morph.api.MorphApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public class MorphVariant implements Comparable<MorphVariant>
{
    public static final int IDENTIFIER_LENGTH = 20;
    public static final String IDENTIFIER_DEFAULT_PLAYER_STATE = "default_player_state";
    public static final String NBT_PLAYER_ID = "Morph_Player_ID";
    public static String[] TAGS_TO_TAKE = new String[] { "CustomName", "CustomNameVisible", "ForgeCaps", "ForgeData" };

    @Nonnull
    public ResourceLocation id;
    @Nonnull
    public CompoundTag nbtMorph;
    public CompoundTag nbtCommon;
    public ArrayList<Variant> variants;

    public Variant thisVariant;

    public MorphVariant(ResourceLocation id)
    {
        this.id = id;
        this.nbtMorph = new CompoundTag();
        this.variants = new ArrayList<>();
    }

    private MorphVariant()
    {
        this.variants = new ArrayList<>();
        this.nbtMorph = new CompoundTag();
    }

    public void setLiving(CompoundTag tag)
    {
        nbtCommon = tag;
    }

    public void writeSupportedAttributes(LivingEntity living)
    {
        for(Map.Entry<ResourceLocation, AttributeConfig> e : MorphApi.getApiImpl().getSupportedAttributes().entrySet())
        {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(e.getKey());

            if(attribute != null && living.getAttributes().hasAttribute(attribute))
            {
                AttributeConfig attributeConfig = e.getValue();
                double value = living.getAttributeValue(attribute);

                if(attributeConfig.moreIsBetter)
                {
                    if(attributeConfig.cap != null && value > attributeConfig.cap)
                    {
                        value = attributeConfig.cap;
                    }
                }
                else
                {
                    if(attributeConfig.cap != null && value < attributeConfig.cap)
                    {
                        value = attributeConfig.cap;
                    }
                }

                nbtMorph.putDouble("attr_" + e.getKey().toString(), value);
            }
        }
    }

    public static void writeDefaults(LivingEntity living, CompoundTag tag)
    {
        CompoundTag defs = new CompoundTag();

        living.saveWithoutId(defs);

        for(String s : TAGS_TO_TAKE)
        {
            if(defs.contains(s))
            {
                Tag value = defs.get(s);
                if(value != null)
                {
                    tag.put(s, value.copy());
                }
            }
        }
    }

    public boolean hasVariants()
    {
        return !variants.isEmpty();
    }

    public boolean combineVariants(MorphVariant variant)
    {
        if(!isSameMorphType(variant))
        {
            return false;
        }

        if(id.equals(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)))
        {
            variants.add(variant.thisVariant);
            return true;
        }

        addBetterMorphData(variant.nbtMorph);

        CompoundTag variantTag = variant.getCumulativeTags();

        HashSet<String> uncommons = new HashSet<>();

        if(nbtCommon != null)
        {
            for(String key : nbtCommon.getAllKeys())
            {
                Tag commonTag = nbtCommon.get(key);
                Tag varNBT = variantTag.get(key);

                if(varNBT == null || !varNBT.equals(commonTag))
                {
                    uncommons.add(key);
                }
                else
                {
                    variantTag.remove(key);
                }
            }

            for(String key : uncommons)
            {
                Tag nbt = nbtCommon.get(key);

                if(nbt != null)
                {
                    for(Variant aVariant : variants)
                    {
                        aVariant.nbtVariant.put(key, nbt.copy());
                    }
                }

                nbtCommon.remove(key);
            }
        }

        variant.thisVariant.nbtVariant = variantTag;

        variants.add(variant.thisVariant);

        gatherNewCommons();

        return true;
    }

    public boolean removeVariant(Variant variant)
    {
        boolean flag = false;

        for(int i = variants.size() - 1; i >= 0; i--)
        {
            if(variants.get(i).identifier.equals(variant.identifier))
            {
                variants.remove(i);
                flag = true;
            }
        }

        if(flag && !id.equals(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)))
        {
            if(variants.size() >= 2)
            {
                gatherNewCommons();
            }
            else if(!variants.isEmpty())
            {
                for(String key : nbtCommon.getAllKeys())
                {
                    Tag value = nbtCommon.get(key);
                    if(value != null)
                    {
                        variants.get(0).nbtVariant.put(key, value.copy());
                    }
                }

                for(String key : new ArrayList<>(nbtCommon.getAllKeys()))
                {
                    nbtCommon.remove(key);
                }
            }
            else
            {
                for(String key : new ArrayList<>(nbtCommon.getAllKeys()))
                {
                    nbtCommon.remove(key);
                }
            }
        }

        return flag;
    }

    public Variant getVariantById(String id)
    {
        for(Variant variant : variants)
        {
            if(variant.identifier.equals(id))
            {
                return variant;
            }
        }

        if(thisVariant != null && thisVariant.identifier.equals(id))
        {
            return thisVariant;
        }

        return null;
    }

    public void gatherNewCommons()
    {
        HashMap<String, Tag> commons = new HashMap<>();

        for(Variant variant : variants)
        {
            for(String key : variant.nbtVariant.getAllKeys())
            {
                Tag value = variant.nbtVariant.get(key);

                if(value != null)
                {
                    commons.putIfAbsent(key, value.copy());
                }
            }
        }

        commons.entrySet().removeIf(e -> {
            for(Variant variant : variants)
            {
                Tag value = variant.nbtVariant.get(e.getKey());

                if(value == null || !e.getValue().equals(value))
                {
                    return true;
                }
            }

            return false;
        });

        if(nbtCommon == null)
        {
            nbtCommon = new CompoundTag();
        }

        for(Map.Entry<String, Tag> e : commons.entrySet())
        {
            nbtCommon.put(e.getKey(), e.getValue().copy());
        }

        for(String s : commons.keySet())
        {
            for(Variant variant : variants)
            {
                variant.nbtVariant.remove(s);
            }
        }
    }

    public boolean containsVariant(MorphVariant variant)
    {
        if(id.equals(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)))
        {
            for(Variant aVariant : variants)
            {
                if(aVariant.playerUUID.equals(variant.thisVariant.playerUUID))
                {
                    return true;
                }
            }
        }
        else if(!variant.id.equals(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)))
        {
            CompoundTag variantTags = variant.getCumulativeTags();

            for(Variant aVariant : variants)
            {
                CompoundTag aVariantTags = getCumulativeTagsWithVariant(aVariant);

                if(variantTags.equals(aVariantTags))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isSameMorphType(MorphVariant variant)
    {
        return id.equals(variant.id);
    }

    private boolean addBetterMorphData(CompoundTag tag)
    {
        boolean flag = false;

        Map<ResourceLocation, AttributeConfig> supportedAttributes =
                MorphApi.getApiImpl().getSupportedAttributes();

        for(String key : nbtMorph.getAllKeys())
        {
            if(key.startsWith("attr_"))
            {
                ResourceLocation id = new ResourceLocation(key.substring(5));

                if(supportedAttributes.containsKey(id))
                {
                    AttributeConfig attributeConfig = supportedAttributes.get(id);
                    final double value = tag.getDouble(key);

                    if(attributeConfig.moreIsBetter)
                    {
                        if(nbtMorph.getDouble(key) < value)
                        {
                            nbtMorph.putDouble(key, value);

                            if(attributeConfig.cap != null && value > attributeConfig.cap)
                            {
                                nbtMorph.putDouble(key, attributeConfig.cap);
                            }

                            flag = true;
                        }
                    }
                    else
                    {
                        if(nbtMorph.getDouble(key) > value)
                        {
                            nbtMorph.putDouble(key, value);

                            if(attributeConfig.cap != null && value < attributeConfig.cap)
                            {
                                nbtMorph.putDouble(key, attributeConfig.cap);
                            }

                            flag = true;
                        }
                    }
                }
            }
        }

        for(String key : tag.getAllKeys())
        {
            if(key.startsWith("attr_") && !nbtMorph.contains(key))
            {
                Tag value = tag.get(key);

                if(value != null)
                {
                    nbtMorph.put(key, value.copy());
                    flag = true;
                }
            }
        }

        return flag;
    }

    public boolean hasFavourite()
    {
        for(Variant variant : variants)
        {
            if(variant.isFavourite)
            {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    @Deprecated
    public LivingEntity createEntityInstance(Level level, @Nullable UUID playerId)
    {
        return createEntityInstance(
                level,
                playerId != null ? level.getPlayerByUUID(playerId) : null
        );
    }

    @Nonnull
    public LivingEntity createEntityInstance(Level level, @Nullable Player player)
    {
        LivingEntity entInstance = null;

        EntityType<?> value = ForgeRegistries.ENTITY_TYPES.getValue(id);

        if(value != null)
        {
            try
            {
                if(value.equals(EntityType.PLAYER))
                {
                    entInstance = level.isClientSide
                            ? createPlayer(level, thisVariant.playerUUID)
                            : new FakePlayer(
                                    (ServerLevel) level,
                                    MorphApi.getApiImpl().getGameProfile(thisVariant.playerUUID, null)
                            );

                    if(player != null)
                    {
                        for(String key : player.getPersistentData().getAllKeys())
                        {
                            Tag valueTag = player.getPersistentData().get(key);

                            if(valueTag != null)
                            {
                                entInstance.getPersistentData().put(key, valueTag.copy());
                            }
                        }
                    }

                    for(BiConsumer<LivingEntity, CompoundTag> consumer :
                            MorphApi.getApiImpl().getVariantNbtTagReaders())
                    {
                        consumer.accept(entInstance, entInstance.getPersistentData());
                    }
                }
                else
                {
                    CompoundTag tags = getCumulativeTags();
                    Entity ent = value.create(level);

                    if(ent instanceof LivingEntity)
                    {
                        ent.load(tags);

                        entInstance = (LivingEntity) ent;

                        for(BiConsumer<LivingEntity, CompoundTag> consumer :
                                MorphApi.getApiImpl().getVariantNbtTagReaders())
                        {
                            consumer.accept(entInstance, tags);
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                MorphApi.getLogger().error("Error creating Morph entity for ID: {}", id);
                t.printStackTrace();
            }
        }

        if(entInstance == null)
        {
            MorphApi.getLogger().error("Cannot find entity type {} have a pig instead!", id);

            entInstance = EntityType.PIG.create(level);
            entInstance.setCustomName(Component.literal("Invalid Morph Pig"));
        }

        entInstance.setId(MorphInfo.getNextEntId());

        if(player != null)
        {
            entInstance.getPersistentData().putUUID(
                    NBT_PLAYER_ID,
                    player.getGameProfile().getId()
            );
        }

        return entInstance;
    }

    @OnlyIn(Dist.CLIENT)
    private Player createPlayer(Level level, UUID uuid)
    {
        Minecraft mc = Minecraft.getInstance();
        GameProfile gameProfile = MorphApi.getApiImpl().getGameProfile(uuid, null);

        RemotePlayer player = new RemotePlayer((ClientLevel) level, gameProfile);

        return player;
    }

    public MorphVariant getAsVariant(Variant variant)
    {
        MorphVariant morph = createFromNBT(write(new CompoundTag()));
        morph.variants.clear();
        morph.thisVariant = variant;

        return morph;
    }

    public CompoundTag getCumulativeTags()
    {
        return getCumulativeTagsWithVariant(thisVariant);
    }

    public CompoundTag getCumulativeTagsWithVariant(Variant variant)
    {
        CompoundTag tags = new CompoundTag();

        if(nbtCommon != null)
        {
            for(String key : nbtCommon.getAllKeys())
            {
                Tag value = nbtCommon.get(key);

                if(value != null)
                {
                    tags.put(key, value.copy());
                }
            }
        }

        if(variant != null && variant.nbtVariant != null)
        {
            for(String key : variant.nbtVariant.getAllKeys())
            {
                Tag value = variant.nbtVariant.get(key);

                if(value != null)
                {
                    tags.put(key, value.copy());
                }
            }
        }

        return tags;
    }

    public CompoundTag write(CompoundTag tag)
    {
        tag.putString("id", id.toString());
        tag.put("nbtMorph", nbtMorph.copy());

        if(!id.equals(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)))
        {
            if(nbtCommon != null)
            {
                tag.put("nbtCommon", nbtCommon.copy());
            }
        }

        tag.putInt("variantCount", variants.size());

        for(int i = 0; i < variants.size(); i++)
        {
            tag.put("variant_" + i, variants.get(i).write(new CompoundTag()));
        }

        if(thisVariant != null)
        {
            tag.put("thisVariant", thisVariant.write(new CompoundTag()));
        }

        return tag;
    }

    public void read(CompoundTag tag)
    {
        id = new ResourceLocation(tag.getString("id"));
        nbtMorph = tag.getCompound("nbtMorph");

        if(!id.equals(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)))
        {
            nbtCommon = tag.getCompound("nbtCommon");
        }

        variants.clear();

        int count = tag.getInt("variantCount");

        for(int i = 0; i < count; i++)
        {
            Variant variant = new Variant();
            variant.read(tag.getCompound("variant_" + i));
            variants.add(variant);
        }

        if(tag.contains("thisVariant"))
        {
            Variant variant = new Variant();
            variant.read(tag.getCompound("thisVariant"));
            thisVariant = variant;
        }
        else
        {
            thisVariant = null;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof MorphVariant)
        {
            MorphVariant variant = (MorphVariant) obj;

            if(id.equals(variant.id) && thisVariant != null && variant.thisVariant != null)
            {
                if(id.equals(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)))
                {
                    return thisVariant.playerUUID.equals(variant.thisVariant.playerUUID);
                }
                else
                {
                    return getCumulativeTags().equals(variant.getCumulativeTags());
                }
            }
        }

        return false;
    }

    @Override
    public int compareTo(MorphVariant o)
    {
        ResourceLocation playerId =
                ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER);

        if(id.equals(playerId) && !id.equals(o.id))
        {
            return -1;
        }
        else if(o.id.equals(playerId) && !id.equals(o.id))
        {
            return 1;
        }

        EntityType<?> value = ForgeRegistries.ENTITY_TYPES.getValue(id);
        EntityType<?> otherType = ForgeRegistries.ENTITY_TYPES.getValue(o.id);

        if(value != null)
        {
            if(otherType != null)
            {
                if(EffectiveSide.get().isClient())
                {
                    return I18n.get(value.getDescriptionId())
                            .compareTo(I18n.get(otherType.getDescriptionId()));
                }

                return value.getDescriptionId()
                        .compareTo(otherType.getDescriptionId());
            }

            return -1;
        }
        else
        {
            if(otherType == null)
            {
                return 0;
            }

            return 1;
        }
    }

    public static MorphVariant createFromNBT(CompoundTag tag)
    {
        MorphVariant variant = new MorphVariant();
        variant.read(tag);
        return variant;
    }

    public static MorphVariant createPlayerMorph(@Nonnull UUID owner, boolean isVariant)
    {
        MorphVariant variant = new MorphVariant(
                ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PLAYER)
        );

        Variant var = new Variant();
        var.playerUUID = owner;

        if(isVariant)
        {
            variant.thisVariant = var;
        }
        else
        {
            variant.variants.add(var);
        }

        return variant;
    }

    @Override
    public int hashCode()
    {
        return thisVariant != null
                ? thisVariant.hashCode()
                : super.hashCode();
    }

    public static class Variant
    {
        public String identifier;
        public UUID playerUUID;
        public CompoundTag nbtVariant;
        public boolean isFavourite;

        public Variant()
        {
            this.identifier = RandomStringUtils.randomAscii(IDENTIFIER_LENGTH);
            this.nbtVariant = new CompoundTag();
            this.isFavourite = false;
        }

        public CompoundTag write(CompoundTag tag)
        {
            tag.putString("identifier", identifier);

            if(playerUUID != null)
            {
                tag.putUUID("playerUUID", playerUUID);
            }
            else
            {
                tag.put("nbtVariant", nbtVariant.copy());
            }

            tag.putBoolean("isFavourite", isFavourite);

            return tag;
        }

        public void read(CompoundTag tag)
        {
            identifier = tag.getString("identifier");

            if(tag.hasUUID("playerUUID"))
            {
                playerUUID = tag.getUUID("playerUUID");
            }
            else
            {
                nbtVariant = tag.getCompound("nbtVariant");
            }

            isFavourite = tag.getBoolean("isFavourite");
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj instanceof Variant)
            {
                Variant other = (Variant) obj;

                return playerUUID != null
                        ? playerUUID.equals(other.playerUUID)
                        : nbtVariant.equals(other.nbtVariant);
            }

            return false;
        }

        @Override
        public int hashCode()
        {
            return identifier.hashCode();
        }
    }
}

