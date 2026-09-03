package me.ichun.mods.morph.common.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.PacketOpenGenerator;
import me.ichun.mods.morph.common.packet.PacketUpdateMorph;
import me.ichun.mods.morph.common.resource.ResourceHandler;
import net.minecraft.commands.CommandSourceStackStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.UUID;

public class CommandMorph
{
    private static final SimpleCommandExceptionType EXTRACTION_ERROR = new SimpleCommandExceptionType(Component.translatable("command.morph.resources.error.extractionError"));
    private static final SimpleCommandExceptionType PLAYER_NO_NBT = new SimpleCommandExceptionType(Component.translatable("command.morph.resources.error.playerNoNbt"));

    private static final SimpleCommandExceptionType NOT_LIVING_ENTITY = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.notLivingEntity"));
    private static final SimpleCommandExceptionType ENTITY_COULD_NOT_BE_CREATED = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.failedToCreateEntity"));
    private static final SimpleCommandExceptionType UNABLE_TO_ACQUIRE_MORPH = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.unableToAcquireMorph"));
    private static final SimpleCommandExceptionType UNABLE_TO_UNACQUIRE_CANNOT_FIND_ID = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.unableToUnacquireNoId"));
    private static final SimpleCommandExceptionType UNABLE_TO_UNACQUIRE_SELF_VARIANT = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.unableToUnacquireSelf"));
    private static final SimpleCommandExceptionType UNABLE_TO_UNACQUIRE_OUT_OF_BOUNDS = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.unableToUnacquireOOB"));
    private static final SimpleCommandExceptionType UNABLE_TO_MORPH_TO = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.unableToMorphTo"));
    private static final SimpleCommandExceptionType UNABLE_TO_DEMORPH = new SimpleCommandExceptionType(Component.translatable("command.morph.morph.error.unableToDemorph"));

    private static final SuggestionProvider<CommandSourceStack> MORPH_VARIANT_IDS = (context, builder) -> {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");

        PlayerMorphData morphData = MorphHandler.INSTANCE.getPlayerMorphData(player);

        TreeSet<ResourceLocation> variantIds = new TreeSet<>(Comparator.naturalOrder());

        for(MorphVariant morph : morphData.morphs)
        {
            variantIds.add(morph.id);
        }

        return SharedSuggestionProvider.suggestIterable(variantIds, builder);
    };


    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal("morph").requires(p -> p.hasPermissionLevel(2))
                .then(Commands.literal("resources")
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    context.getSource().sendFeedback(Component.translatable("command.morph.resources.success.reloaded"), true);
                                    ResourceHandler.reloadAllResources();
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("reextract")
                                .executes(context -> {
                                    try
                                    {
                                        ResourceHandler.extractFiles(ResourceHandler.getMorphDir().resolve(ResourceHandler.MOB_SUPPORT_VERSION + ".extracted"));
                                        ResourceHandler.reloadAllResources();
                                        context.getSource().sendFeedback(Component.translatable("command.morph.resources.success.reextract"), true);
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    catch(IOException e)
                                    {
                                        Morph.LOGGER.warn("Error extracting mod support files.", e);
                                        throw EXTRACTION_ERROR.create();
                                    }
                                })
                        )
                        .then(Commands.literal("generate")
                                .then(Commands.literal("nbt")
                                        .executes(context -> {
                                            HitResult entityLook = EntityHelper.getEntityLook(context.getSource().getPlayer(), 5);
                                            if(entityLook.getType() == HitResult.Type.ENTITY)
                                            {
                                                return openNBTGenerator(context.getSource(), ((EntityHitResult)entityLook).getEntity());
                                            }
                                            throw NOT_LIVING_ENTITY.create();
                                        })
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> openNBTGenerator(context.getSource(), EntityArgument.getEntity(context, "target")))
                                        )
                                )
                                .then(Commands.literal("mob")
                                        .executes(context -> openMobDataGerator(context.getSource()))
                                )
                        )
                )
                .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("morph")
                                        .then(Commands.literal("acquire")
                                                .then(Commands.literal("entity")
                                                        .then(Commands.argument("target", EntityArgument.entity())
                                                                .executes(context -> createMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), EntityArgument.getEntity(context, "target"), true))
                                                        )
                                                )
                                                .then(Commands.literal("type")
                                                        .then(Commands.argument("entity_type", ResourceLocationArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                                                .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                                        .executes(context -> createMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "entity_type"), CompoundTagArgument.getCompoundTag(context, "nbt"), true))
                                                                )
                                                                .executes(context -> createMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "entity_type"), new CompoundTag(), true))
                                                        )
                                                )
                                                .then(Commands.literal("player")
                                                        .then(Commands.literal("uuid")
                                                                .then(Commands.argument("player_uuid", UUIDArgument.uuid())
                                                                        .executes(context -> createPlayerMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), UUIDArgument.getUuid(context, "player_uuid"), true))
                                                                )
                                                        )
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("player_name", StringArgumentType.word())
                                                                        .executes(context -> createPlayerMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "player_name"), true))
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("unacquire")
                                                .then(Commands.argument("variant_id", ResourceLocationArgument.resourceLocation()).suggests(MORPH_VARIANT_IDS)
                                                        .then(Commands.argument("variant_index", IntegerArgumentType.integer(0))
                                                                .executes(context -> unacquire(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getResourceLocation(context, "variant_id"), IntegerArgumentType.getInteger(context, "variant_index")))
                                                        )
                                                        .then(Commands.literal("all")
                                                                .executes(context -> unacquire(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getResourceLocation(context, "variant_id"), -1))
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("morph")
                                                .then(Commands.literal("entity")
                                                        .then(Commands.argument("target", EntityArgument.entity())
                                                                .executes(context -> createMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), EntityArgument.getEntity(context, "target"), false))
                                                        )
                                                )
                                                .then(Commands.literal("type")
                                                        .then(Commands.argument("entity_type", ResourceLocationArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                                                .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                                        .executes(context -> createMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "entity_type"), CompoundTagArgument.getCompoundTag(context, "nbt"), false))
                                                                )
                                                                .executes(context -> createMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), ResourceLocationArgument.getId(context, "entity_type"), new CompoundTag(), false))
                                                        )
                                                )
                                                .then(Commands.literal("player")
                                                        .then(Commands.literal("uuid")
                                                                .then(Commands.argument("player_uuid", UUIDArgument.uuid())
                                                                        .executes(context -> createPlayerMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), UUIDArgument.getUuid(context, "player_uuid"), false))
                                                                )
                                                        )
                                                        .then(Commands.literal("name")
                                                                .then(Commands.argument("player_name", StringArgumentType.word())
                                                                        .executes(context -> createPlayerMorph(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "player_name"), false))
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("demorph")
                                                .executes(context -> demorphPlayer(context.getSource(), EntityArgument.getPlayer(context, "player")))
                                        )
                                )
                        //                        .then(Commands.literal("biomass")
                        //                                .then(Commands.literal("set")
                        //                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                        //                                                .executes(context -> setBiomass(context.getSource(), EntityArgument.getPlayer(context, "player"), DoubleArgumentType.getDouble(context, "value")))
                        //                                        )
                        //                                )
                        //                        )
                )
        );
    }

    private static int unacquire(CommandSourceStack source, ServerPlayer player, ResourceLocation variant_id, int i) throws CommandSyntaxException
    {
        PlayerMorphData morphData = MorphHandler.INSTANCE.getPlayerMorphData(player);

        for(MorphVariant morph : morphData.morphs)
        {
            if(variant_id.equals(morph.id))
            {
                boolean updatePlayer = false;
                if(i >= 0)
                {
                    if(i >= morph.variants.size())
                    {
                        throw UNABLE_TO_UNACQUIRE_OUT_OF_BOUNDS.create();
                    }

                    MorphVariant.Variant variant = morph.variants.get(i);

                    if(MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE.equals(variant.identifier))
                    {
                        throw UNABLE_TO_UNACQUIRE_SELF_VARIANT.create();
                    }

                    if(morph.removeVariant(variant))
                    {
                        updatePlayer = true;
                    }
                }
                else //remove ALL variants
                {
                    for(int i1 = morph.variants.size() - 1; i1 >= 0; i1--)
                    {
                        MorphVariant.Variant variant = morph.variants.get(i1);

                        if(MorphVariant.IDENTIFIER_DEFAULT_PLAYER_STATE.equals(variant.identifier))
                        {
                            continue;
                        }

                        if(morph.removeVariant(variant))
                        {
                            updatePlayer = true;
                        }
                    }
                }

                if(updatePlayer)
                {
                    MorphHandler.INSTANCE.getSaveData().markDirty();

                    Morph.channel.sendTo(new PacketUpdateMorph(morph.write(new CompoundTag())), player);

                    source.sendFeedback(Component.translatable("command.morph.morph.success.morphUnacquired", player.getDisplayName()), true);
                }
                return Command.SINGLE_SUCCESS;
            }
        }

        throw UNABLE_TO_UNACQUIRE_CANNOT_FIND_ID.create();
    }

    private static int openMobDataGerator(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.asPlayer();

        Morph.channel.sendTo(new PacketOpenGenerator(-1), player);

        return Command.SINGLE_SUCCESS;
    }

    private static int openNBTGenerator(CommandSourceStack source, Entity target) throws CommandSyntaxException
    {
        if(!(target instanceof LivingEntity))
        {
            throw NOT_LIVING_ENTITY.create();
        }

        if(target instanceof Player)
        {
            throw PLAYER_NO_NBT.create();
        }

        ServerPlayer player = source.asPlayer();

        Morph.channel.sendTo(new PacketOpenGenerator(target.getId()), player);

        return Command.SINGLE_SUCCESS;
    }

    private static int setBiomass(CommandSourceStack source, ServerPlayer player, double value)
    {
        MorphHandler.INSTANCE.setBiomassAmount(player, value);
        return Command.SINGLE_SUCCESS;
    }

    private static int createMorph(CommandSourceStack source, ServerPlayer player, Entity entity, boolean isAcquire) throws CommandSyntaxException
    {
        if(entity instanceof LivingEntity)
        {
            MorphVariant variant = MorphHandler.INSTANCE.createVariant((LivingEntity)entity);
            if(createMorph(source, player, variant, isAcquire))
            {
                source.sendFeedback(Component.translatable(isAcquire ? "command.morph.morph.success.morphAcquired" : "command.morph.morph.success.morphTo", player.getDisplayName()), true);
                return Command.SINGLE_SUCCESS;
            }
            throw isAcquire ? UNABLE_TO_ACQUIRE_MORPH.create() : UNABLE_TO_MORPH_TO.create();
        }
        throw NOT_LIVING_ENTITY.create();
    }

    private static int createMorph(CommandSourceStack source, ServerPlayer player, ResourceLocation type, CompoundTag nbt, boolean isAcquire) throws CommandSyntaxException
    {
        //Taken from SummonCommand
        CompoundTag compoundnbt = nbt.copy();
        compoundnbt.putString("id", type.toString());
        ServerWorld serverworld = source.getWorld();
        Entity entity = EntityType.loadEntityAndExecute(compoundnbt, serverworld, ent -> ent);

        if(entity == null)
        {
            throw ENTITY_COULD_NOT_BE_CREATED.create();
        }
        else
        {
            return createMorph(source, player, entity, isAcquire);
        }
    }

    private static int createPlayerMorph(CommandSourceStack source, ServerPlayer player, String name, boolean isAcquire) throws CommandSyntaxException
    {
        GameProfile gameProfile = EntityHelper.getGameProfile(null, name);
        if(gameProfile.getId() == null) //maybe lookup failed, UUID will be null.
        {
            throw isAcquire ? UNABLE_TO_ACQUIRE_MORPH.create() : UNABLE_TO_MORPH_TO.create();
        }
        MorphVariant variant = MorphVariant.createPlayerMorph(gameProfile.getId(), true);
        if(createMorph(source, player, variant, isAcquire))
        {
            source.sendFeedback(Component.translatable(isAcquire ? "command.morph.morph.success.morphAcquired" : "command.morph.morph.success.morphTo", player.getDisplayName()), true);
            return Command.SINGLE_SUCCESS;
        }
        throw isAcquire ? UNABLE_TO_ACQUIRE_MORPH.create() : UNABLE_TO_MORPH_TO.create();
    }

    private static int createPlayerMorph(CommandSourceStack source, ServerPlayer player, UUID uuid, boolean isAcquire) throws CommandSyntaxException
    {
        MorphVariant variant = MorphVariant.createPlayerMorph(uuid, true);
        if(createMorph(source, player, variant, isAcquire))
        {
            source.sendFeedback(Component.translatable(isAcquire ? "command.morph.morph.success.morphAcquired" : "command.morph.morph.success.morphTo", player.getDisplayName()), true);
            return Command.SINGLE_SUCCESS;
        }
        throw isAcquire ? UNABLE_TO_ACQUIRE_MORPH.create() : UNABLE_TO_MORPH_TO.create();
    }

    private static boolean createMorph(CommandSourceStack source, ServerPlayer player, MorphVariant variant, boolean isAcquire)
    {
        if(isAcquire)
        {
            return MorphHandler.INSTANCE.acquireMorph(player, variant);
        }
        else
        {
            return MorphHandler.INSTANCE.morphTo(player, variant);
        }
    }

    private static int demorphPlayer(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException
    {
        if(MorphHandler.INSTANCE.demorph(player))
        {
            source.sendFeedback(Component.translatable("command.morph.morph.success.demorph", player.getDisplayName()), true);
            return Command.SINGLE_SUCCESS;
        }
        throw UNABLE_TO_DEMORPH.create();
    }

}
