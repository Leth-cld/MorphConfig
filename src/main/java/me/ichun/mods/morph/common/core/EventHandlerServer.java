package me.ichun.mods.morph.common.core;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.biomass.BiomassUpgradeHandler;
import me.ichun.mods.morph.common.command.CommandMorph;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import me.ichun.mods.morph.common.morph.save.MorphSavedData;
import me.ichun.mods.morph.common.packet.PacketPlayerData;
import me.ichun.mods.morph.common.packet.PacketSessionSync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

public class EventHandlerServer
{
    @SubscribeEvent
    public void onAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event)
    {
        Entity entity = event.getObject();
        if(entity instanceof Player && !(entity instanceof FakePlayer))
        {
            event.addCapability(MorphInfo.CAPABILITY_IDENTIFIER, new MorphInfo.CapProvider(new MorphInfoImpl((Player)entity)));
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event)
    {
        //We're trying to add a morph entity to the world, cancel this event
        if(event.getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingAttacked(LivingAttackEvent event)
    {
        //The entity attacking is a morph. Cancel the event.
        if(event.getSource().getEntity() != null && event.getSource().getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID))
        {
            event.setCanceled(true);
        }

        //The entity getting hurt is a morph. Cancel the event.
        if(event.getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID) && !(event.getSource().getMsgId().equals("outOfWorld") && event.getAmount() == Float.MAX_VALUE)) // Do not cancel if it's from a kill command
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event)
    {
        //The entity dying is a morph. Cancel the event.
        if(event.getEntity().getPersistentData().contains(MorphVariant.NBT_PLAYER_ID))
        {
            event.setCanceled(true);
            return;
        }

        if(!event.getEntity().level().isClientSide && event.getSource().getEntity() instanceof ServerPlayer && !(event.getSource().getEntity() instanceof FakePlayer) && !event.getSource().getEntity().isRemoved() && event.getEntity().getId() > 0)
        {
            MorphHandler.INSTANCE.handleMurderEvent((ServerPlayer)event.getSource().getEntity(), event.getEntity());
        }
    }

    @SubscribeEvent
    public void onEntityDimensions(EntityEvent.Size event)
    {
        if(event.getEntity() instanceof Player && !event.getEntity().isRemoved() && event.getEntity().getId() > 0 && event.getEntity().tickCount >= 0)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo((Player)event.getEntity());
            if(info.isMorphed())
            {
                event.setNewSize(info.getMorphSize(1F));
                event.setNewEyeHeight(info.getMorphEyeHeight(1F));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if(event.phase == TickEvent.Phase.END && !event.player.isRemoved() && event.player.getId() > 0)
        {
            MorphInfo info = MorphHandler.INSTANCE.getMorphInfo(event.player);
            info.tick();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if(!(event.getPlayer().getServer().isSinglePlayer() && event.getPlayer().getGameProfile().getName().equals(event.getPlayer().getServer().getServerOwner()))) //if the player is not the client in singleplayer
        {
            Morph.channel.sendTo(new PacketSessionSync(BiomassUpgradeHandler.BIOMASS_UPGRADES.values()), (ServerPlayer)event.getPlayer());
        }
        Morph.channel.sendTo(new PacketPlayerData(MorphHandler.INSTANCE.getPlayerMorphData(event.getPlayer()).write(new CompoundTag())), (ServerPlayer)event.getPlayer());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event)
    {
        MorphHandler.INSTANCE.getMorphInfo(event.getPlayer()).read(MorphHandler.INSTANCE.getMorphInfo(event.getOriginal()).write(new CompoundTag()));
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event)
    {
        if(!event.getLevel().isClientSide && ((ServerLevel)event.getLevel()).dimension().equals(Level.OVERWORLD))
        {
            MorphHandler.INSTANCE.setSaveData(((ServerLevel)event.getLevel()).getDataStorage().computeIfAbsent(MorphSavedData::load, MorphSavedData::new, MorphSavedData.ID));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandMorph.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) //do this early so we do it before the server loads our world save.
    {
        BiomassUpgradeHandler.loadBiomassUpgrades();
    }

    @SubscribeEvent
    public void onServerStopped(FMLServerStoppedEvent event)
    {
        MorphHandler.INSTANCE.setSaveData(null);
    }
}
