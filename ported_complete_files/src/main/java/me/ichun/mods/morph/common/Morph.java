package me.ichun.mods.morph.common;

import me.ichun.mods.ichunutil.common.data.AdvancementGen;
import me.ichun.mods.ichunutil.common.network.PacketChannel;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.MobData;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.client.config.ConfigClient;
import me.ichun.mods.morph.client.core.EventHandlerClient;
import me.ichun.mods.morph.client.core.KeyBinds;
import me.ichun.mods.morph.client.entity.EntityAcquisition;
import me.ichun.mods.morph.client.entity.EntityBiomassAbility;
import me.ichun.mods.morph.client.render.RenderEntityAcquisition;
import me.ichun.mods.morph.client.render.RenderEntityBiomassAbility;
import me.ichun.mods.morph.common.config.ConfigServer;
import me.ichun.mods.morph.common.core.EventHandlerServer;
import me.ichun.mods.morph.common.mob.MobDataHandler;
import me.ichun.mods.morph.common.mob.TraitHandler;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.packet.*;
import me.ichun.mods.morph.common.resource.ResourceHandler;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.criterion.ChangeDimensionTrigger;
import net.minecraft.advancements.criterion.EffectsChangedTrigger;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.advancements.criterion.PositionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.BuiltinStructures;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mod(Morph.MOD_ID)
public class Morph {
    public static final String MOD_NAME = "Morph";
    public static final String MOD_ID = "morph";
    public static final String PROTOCOL = "2";

    public static final Logger LOGGER = LogManager.getLogger();

    public static ConfigServer configServer;
    public static ConfigClient configClient;
    public static EventHandlerClient eventHandlerClient;
    public static EventHandlerServer eventHandlerServer;
    public static PacketChannel channel;

    public Morph() {
        if (!ResourceHandler.setupEnv()) {
            LOGGER.fatal("Error initialising Morph Resource Handler! Terminating init.");
            return;
        }

        configServer = new ConfigServer().init();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Sounds.REGISTRY.register(bus);

        bus.addListener(this::registerCapabilities);
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::processIMC);
        bus.addListener(this::finishLoading);

        MinecraftForge.EVENT_BUS.register(eventHandlerServer = new EventHandlerServer());
        MinecraftForge.EVENT_BUS.addListener(Advancements::onGatherData);

        MorphApi.setApiImpl(MorphHandler.INSTANCE);

        channel = new PacketChannel(
                new ResourceLocation(MOD_ID, "channel"), PROTOCOL,
                PacketPlayerData.class, PacketRequestMorphInfo.class, PacketMorphInfo.class,
                PacketUpdateMorph.class, PacketSessionSync.class, PacketMorphInput.class,
                PacketAcquisition.class, PacketUpdateBiomassValue.class,
                PacketUpdateBiomassUpgrades.class, PacketInvalidateClientHealth.class,
                PacketOpenGenerator.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            configClient = new ConfigClient().init();
            bus.addListener(this::onClientSetup);
            MinecraftForge.EVENT_BUS.register(eventHandlerClient = new EventHandlerClient());
            // The old Forge extension point was removed/renamed in newer Forge.
            // iChunUtil's client config handler remains available through its own API.
        });
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MorphInfo.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // PacketChannel performs its own channel registration.
    }

    @OnlyIn(Dist.CLIENT)
    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> KeyBinds.init());
    }

    private void processIMC(InterModProcessEvent event) {
        event.getIMCStream(m -> m.equalsIgnoreCase("trait")).forEach(msg -> {
            Object o = msg.getMessageSupplier().get();
            if (o instanceof Class<?> clz && Trait.class.isAssignableFrom(clz)) {
                try {
                    Trait<?> t = (Trait<?>) clz.getDeclaredConstructor().newInstance();
                    if (t.type != null && !t.type.isEmpty()) {
                        TraitHandler.registerTrait(t.type, (Class) clz);
                        LOGGER.info("IMC: Registering trait type {} from mod {}", t.type, msg.getSenderModId());
                    } else {
                        LOGGER.warn("IMC: Invalid trait type from {}", msg.getSenderModId());
                    }
                } catch (ReflectiveOperationException e) {
                    LOGGER.error("IMC: Error retrieving trait type from mod {}", msg.getSenderModId(), e);
                }
            } else {
                LOGGER.warn("IMC: Non-Trait class type trait from {}", msg.getSenderModId());
            }
        });

        event.getIMCStream(m -> m.equalsIgnoreCase("mob")).forEach(msg -> {
            Object o = msg.getMessageSupplier().get();
            if (o instanceof MobData data && data.forEntity != null && !data.forEntity.isEmpty()) {
                ResourceLocation rl = new ResourceLocation(data.forEntity);
                MobDataHandler.registerMobData(rl, data);
                LOGGER.info("IMC: Registering MobData for {} from mod {}", rl, msg.getSenderModId());
            } else {
                LOGGER.warn("IMC: Invalid MobData from {}", msg.getSenderModId());
            }
        });

        event.getIMCStream(m -> m.equalsIgnoreCase("morphSync")).forEach(msg -> {
            Object o = msg.getMessageSupplier().get();
            if (o instanceof BiConsumer consumer) {
                MorphHandler.INSTANCE.getModPlayerMorphSyncConsumers().add(consumer);
            } else {
                LOGGER.warn("IMC: Non-BiConsumer morph sync object from {}", msg.getSenderModId());
            }
        });

        event.getIMCStream(m -> m.equalsIgnoreCase("variantNbtSetter")).forEach(msg -> {
            Object o = msg.getMessageSupplier().get();
            if (o instanceof BiConsumer consumer) {
                MorphHandler.INSTANCE.getVariantNbtTagSetters().add(consumer);
            } else {
                LOGGER.warn("IMC: Non-BiConsumer NBT setter object from {}", msg.getSenderModId());
            }
        });

        event.getIMCStream(m -> m.equalsIgnoreCase("variantNbtReader")).forEach(msg -> {
            Object o = msg.getMessageSupplier().get();
            if (o instanceof BiConsumer consumer) {
                MorphHandler.INSTANCE.getVariantNbtTagReaders().add(consumer);
            } else {
                LOGGER.warn("IMC: Non-BiConsumer NBT reader object from {}", msg.getSenderModId());
            }
        });
    }

    private void finishLoading(FMLLoadCompleteEvent event) {
        ResourceHandler.loadResources();
    }

    public static class Advancements implements Consumer<Consumer<Advancement>> {
        @SubscribeEvent
        public static void onGatherData(GatherDataEvent event) {
            DataGenerator gen = event.getGenerator();
            if (event.includeServer()) {
                gen.addProvider(new AdvancementGen(gen, new Advancements()));
            }
        }

        @Override
        public void accept(Consumer<Advancement> consumer) {
            // The original advancement generation API is supplied by iChunUtil.
            // Vanilla advancement definitions are kept here using 1.20.1 resource keys.
            ResourceKey<Level> nether = Level.NETHER;
            Advancement advancement = Advancement.Builder.advancement()
                    .display(Blocks.RED_NETHER_BRICKS,
                            net.minecraft.network.chat.Component.translatable("advancements.nether.root.title"),
                            net.minecraft.network.chat.Component.translatable("advancements.nether.root.description"),
                            new ResourceLocation("textures/gui/advancements/backgrounds/nether.png"),
                            FrameType.TASK, false, false, false)
                    .addCriterion("entered_nether", ChangeDimensionTrigger.TriggerInstance.changedDimension(nether))
                    .save(consumer, "nether/root");

            Advancement advancement2 = Advancement.Builder.advancement()
                    .parent(advancement)
                    .display(Blocks.NETHER_BRICKS,
                            net.minecraft.network.chat.Component.translatable("advancements.nether.find_fortress.title"),
                            net.minecraft.network.chat.Component.translatable("advancements.nether.find_fortress.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("fortress", PositionTrigger.TriggerInstance.location(LocationPredicate.inStructure(BuiltinStructures.NETHER_FORTRESS)))
                    .save(consumer, "nether/find_fortress");

            Advancement.Builder.advancement()
                    .parent(advancement2)
                    .display(Items.WITHER_ROSE,
                            net.minecraft.network.chat.Component.translatable("morph.advancement.unlock_biomass.title"),
                            net.minecraft.network.chat.Component.translatable("morph.advancement.unlock_biomass.description"),
                            null, FrameType.CHALLENGE, true, true, false)
                    .addCriterion("wither_and_regen",
                            EffectsChangedTrigger.TriggerInstance.effectsChanged(
                                    MobEffectsPredicate.effects().and(MobEffects.WITHER, MobEffects.REGENERATION)))
                    .save(consumer, UNLOCK_BIOMASS.toString());
        }

        public static final ResourceLocation UNLOCK_BIOMASS = new ResourceLocation(MOD_ID, "morph/unlock_biomass");
    }

    public static class Sounds {
        private static final DeferredRegister<SoundEvent> REGISTRY =
                DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

        public static final RegistryObject<SoundEvent> MORPH =
                REGISTRY.register("morph",
                        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "morph")));
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class EntityTypes {
        public static EntityType<EntityAcquisition> ACQUISITION;
        public static EntityType<EntityBiomassAbility> BIOMASS_ABILITY;

        @SubscribeEvent
        public static void register(RegisterEvent event) {
            if (!event.getRegistryKey().equals(ForgeRegistries.Keys.ENTITY_TYPES)) {
                return;
            }

            ACQUISITION = EntityType.Builder.of(EntityAcquisition::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .build(MOD_ID + ":acquisition");

            BIOMASS_ABILITY = EntityType.Builder.of(EntityBiomassAbility::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .build(MOD_ID + ":biomass_ability");

            event.register(ForgeRegistries.Keys.ENTITY_TYPES, helper -> {
                helper.register(new ResourceLocation(MOD_ID, "acquisition"), ACQUISITION);
                helper.register(new ResourceLocation(MOD_ID, "biomass_ability"), BIOMASS_ABILITY);
            });
        }

        @SubscribeEvent
        public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ACQUISITION, RenderEntityAcquisition.RenderFactory::new);
            event.registerEntityRenderer(BIOMASS_ABILITY, RenderEntityBiomassAbility.RenderFactory::new);
        }
    }
}
