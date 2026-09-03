# Morph 1.20.1 port verification report

Target: Minecraft 1.20.1 / Forge 47.4.10 / Java 17 / Gradle 8.8.

This revision was made against the supplied project and the supplied `gradlew build` error log. The build log shows the first build stopping in `:compileApiJava` with 123 errors; the conversion below addresses the API failures visible in that log.

## Build verification

A ForgeGradle build could not be executed in this environment because the required Gradle/Forge artifacts are not installed locally and outbound artifact download is unavailable here. Therefore this report does **not** claim that the project is build-clean. The user's Windows build log remains the authoritative compile result available for this pass.

## Major fixes in this revision

- Replaced the removed 1.16.5 `PotionEvent.PotionApplicableEvent` with Forge 1.20.1 `MobEffectEvent.Applicable`.
- Replaced removed `LivingSetAttackTargetEvent` usage with `LivingChangeTargetEvent` and `setNewTarget(null)`.
- Updated `Effect` to `MobEffect` and modern effect accessors.
- Updated `I18n` to `net.minecraft.client.resources.language.I18n`.
- Updated player inventory access to `Player#getInventory`/`setItemSlot`.
- Updated equipment slot type access.
- Replaced direct `CompoundTag` map access (`tagMap`, `keySet`) with the public 1.20.1 API (`get`, `put`, `remove`, `getAllKeys`).
- Replaced the removed `JsonToNBT` construction with `TagParser.parseTag`.
- Updated vector multiplication to `Vec3#multiply`.
- Updated fluid checks to `isEyeInFluid`, `isInWaterOrBubble`, `isInWaterRainOrBubble`, `getFluidHeight`, and `getFluidJumpThreshold`.
- Updated flight ability APIs and creative checks.
- Added a Mixin accessor for `ServerGamePacketListenerImpl.aboveGroundTickCount` rather than referencing the obsolete 1.16.5 field name.
- Updated rideable/passenger APIs and the passenger packet class.
- Updated item durability APIs (`isDamageableItem`, `getDamageValue`, `setDamageValue`).
- Updated fire/air/damage APIs.
- Updated baby/daylight/boat/block-position APIs.
- Updated attribute instance types.
- Updated entity-type filtering in IntimidateTrait to use `PathfinderMob.class` plus an entity-type predicate.
- Updated path navigation to `moveTo`.
- Removed the unavailable 1.20.1 `EntityViewRenderEvent` dependency from the SwimmerTrait API path; the old event class itself is not present in the target Forge API.
- Fixed stale `tagMap`/attribute-modifier code in the common NBT/attribute handlers.

## Files changed

- `PORT_REPORT.md`
- `src/api/java/me/ichun/mods/morph/api/biomass/BiomassUpgrade.java`
- `src/api/java/me/ichun/mods/morph/api/biomass/BiomassUpgradeInfo.java`
- `src/api/java/me/ichun/mods/morph/api/event/MorphEvent.java`
- `src/api/java/me/ichun/mods/morph/api/mob/nbt/NbtModifier.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/DamageSourceImmunityTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/EffectResistanceTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ExplosiveImmunityTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/FireImmunityTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/FloatTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/HostileTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/IntimidateTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/MagicImmunityTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/MoistSkinTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/SinkTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/StepHeightTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/SunburnTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/SwimmerTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/Trait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/UndeadTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/WaterBreatherTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/WaterSensitivityTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ability/ClimbAbility.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ability/EffectAttackAbility.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ability/FlightFlapAbility.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ability/FlyAbility.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ability/RideableAbility.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ability/SlowFallAbility.java`
- `src/api/java/me/ichun/mods/morph/api/morph/MorphInfo.java`
- `src/api/java/me/ichun/mods/morph/api/morph/MorphState.java`
- `src/main/java/me/ichun/mods/morph/client/config/ConfigClient.java`
- `src/main/java/me/ichun/mods/morph/client/core/EventHandlerClient.java`
- `src/main/java/me/ichun/mods/morph/client/core/HudHandler.java`
- `src/main/java/me/ichun/mods/morph/client/core/KeyBinds.java`
- `src/main/java/me/ichun/mods/morph/client/entity/EntityAcquisition.java`
- `src/main/java/me/ichun/mods/morph/client/entity/EntityBiomassAbility.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/WorkspaceMorph.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/window/WindowHeader.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/window/element/ElementBiomassBar.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/window/element/ElementBiomassUpgrades.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/window/element/ElementRipple.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/window/element/ElementUpgradeNode.java`
- `src/main/java/me/ichun/mods/morph/client/gui/mob/WorkspaceMobData.java`
- `src/main/java/me/ichun/mods/morph/client/gui/mob/window/WindowMobData.java`
- `src/main/java/me/ichun/mods/morph/client/gui/nbt/WorkspaceNbt.java`
- `src/main/java/me/ichun/mods/morph/client/gui/nbt/window/WindowNbt.java`
- `src/main/java/me/ichun/mods/morph/client/gui/window/element/ElementRenderEntity.java`
- `src/main/java/me/ichun/mods/morph/client/model/ModelAcquisition.java`
- `src/main/java/me/ichun/mods/morph/client/render/MorphRenderHandler.java`
- `src/main/java/me/ichun/mods/morph/client/render/RenderEntityAcquisition.java`
- `src/main/java/me/ichun/mods/morph/client/render/RenderEntityBiomassAbility.java`
- `src/main/java/me/ichun/mods/morph/client/render/hand/HandHandler.java`
- `src/main/java/me/ichun/mods/morph/common/Morph.java`
- `src/main/java/me/ichun/mods/morph/common/command/CommandMorph.java`
- `src/main/java/me/ichun/mods/morph/common/config/ConfigServer.java`
- `src/main/java/me/ichun/mods/morph/common/core/EventHandlerServer.java`
- `src/main/java/me/ichun/mods/morph/common/mob/MobDataHandler.java`
- `src/main/java/me/ichun/mods/morph/common/mode/ClassicMode.java`
- `src/main/java/me/ichun/mods/morph/common/mode/CommandMode.java`
- `src/main/java/me/ichun/mods/morph/common/mode/DefaultMode.java`
- `src/main/java/me/ichun/mods/morph/common/mode/DisguiseMode.java`
- `src/main/java/me/ichun/mods/morph/common/mode/MorphMode.java`
- `src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java`
- `src/main/java/me/ichun/mods/morph/common/morph/MorphInfoImpl.java`
- `src/main/java/me/ichun/mods/morph/common/morph/nbt/NbtHandler.java`
- `src/main/java/me/ichun/mods/morph/common/morph/save/MorphSavedData.java`
- `src/main/java/me/ichun/mods/morph/common/morph/save/PlayerMorphData.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketAcquisition.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketInvalidateClientHealth.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketMorphInfo.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketMorphInput.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketOpenGenerator.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketPlayerData.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketRequestMorphInfo.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketSessionSync.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketUpdateBiomassUpgrades.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketUpdateBiomassValue.java`
- `src/main/java/me/ichun/mods/morph/common/packet/PacketUpdateMorph.java`
- `src/main/java/me/ichun/mods/morph/mixin/EntityInvokerMixin.java`
- `src/main/java/me/ichun/mods/morph/mixin/EntityMixin.java`
- `src/main/java/me/ichun/mods/morph/mixin/LivingEntityInvokerMixin.java`
- `src/main/java/me/ichun/mods/morph/mixin/LivingEntityMixin.java`
- `src/main/java/me/ichun/mods/morph/mixin/ModelRendererMixin.java`
- `src/main/java/me/ichun/mods/morph/mixin/PlayerEntityMixin.java`
- `src/main/java/me/ichun/mods/morph/mixin/PlayerRendererMixin.java`
- `src/main/java/me/ichun/mods/morph/mixin/ServerGamePacketListenerAccessor.java`
- `src/main/resources/morph.mixins.json`
- `src/main/resources/pack.mcmeta`

## Files unchanged

- `.gitattributes`
- `.gitignore`
- `CREDITS.txt`
- `LICENSE.txt`
- `README.txt`
- `build.gradle`
- `changelog.txt`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`
- `gradlew.bat`
- `settings.gradle`
- `src/api/java/me/ichun/mods/morph/api/IApi.java`
- `src/api/java/me/ichun/mods/morph/api/MorphApi.java`
- `src/api/java/me/ichun/mods/morph/api/event/MorphLoadResourceEvent.java`
- `src/api/java/me/ichun/mods/morph/api/mob/MobData.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/FallNegateTrait.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/IEventBusRequired.java`
- `src/api/java/me/ichun/mods/morph/api/mob/trait/ability/Ability.java`
- `src/api/java/me/ichun/mods/morph/api/morph/AttributeConfig.java`
- `src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/scene/Scene.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/scene/SceneBiomassAbilities.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/scene/SceneBiomassUpgrades.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/scene/SceneMorphs.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/window/WindowBiomassUpgrades.java`
- `src/main/java/me/ichun/mods/morph/client/gui/biomass/window/WindowSidebar.java`
- `src/main/java/me/ichun/mods/morph/client/gui/mob/window/WindowAddTrait.java`
- `src/main/java/me/ichun/mods/morph/client/gui/mob/window/WindowEditNumber.java`
- `src/main/java/me/ichun/mods/morph/client/gui/mob/window/WindowEditString.java`
- `src/main/java/me/ichun/mods/morph/common/biomass/BiomassUpgradeHandler.java`
- `src/main/java/me/ichun/mods/morph/common/biomass/Upgrades.java`
- `src/main/java/me/ichun/mods/morph/common/mob/TraitHandler.java`
- `src/main/java/me/ichun/mods/morph/common/mode/MorphModeType.java`
- `src/main/java/me/ichun/mods/morph/common/resource/ResourceHandler.java`
- `src/main/resources/META-INF/mods.toml`
- `src/main/resources/assets/morph/lang/en_us.json`
- `src/main/resources/assets/morph/sounds.json`
- `src/main/resources/assets/morph/sounds/morph1.ogg`
- `src/main/resources/assets/morph/sounds/morph2.ogg`
- `src/main/resources/assets/morph/sounds/morph3.ogg`
- `src/main/resources/assets/morph/sounds/morph4.ogg`
- `src/main/resources/assets/morph/sounds/morph5.ogg`
- `src/main/resources/assets/morph/sounds/morph6.ogg`
- `src/main/resources/assets/morph/textures/gui/fav.png`
- `src/main/resources/assets/morph/textures/gui/gui_selected.png`
- `src/main/resources/assets/morph/textures/gui/gui_unselected.png`
- `src/main/resources/assets/morph/textures/gui/gui_unselected_side.png`
- `src/main/resources/assets/morph/textures/icon/climb.png`
- `src/main/resources/assets/morph/textures/icon/fall_negate.png`
- `src/main/resources/assets/morph/textures/icon/fear.png`
- `src/main/resources/assets/morph/textures/icon/fire_immunity.png`
- `src/main/resources/assets/morph/textures/icon/float.png`
- `src/main/resources/assets/morph/textures/icon/fly.png`
- `src/main/resources/assets/morph/textures/icon/hostile.png`
- `src/main/resources/assets/morph/textures/icon/poison_resistance.png`
- `src/main/resources/assets/morph/textures/icon/sink.png`
- `src/main/resources/assets/morph/textures/icon/step.png`
- `src/main/resources/assets/morph/textures/icon/sunburn.png`
- `src/main/resources/assets/morph/textures/icon/swim.png`
- `src/main/resources/assets/morph/textures/icon/water_allergy.png`
- `src/main/resources/assets/morph/textures/icon/wither_resistance.png`
- `src/main/resources/assets/morph/textures/skin/morphskin.png`
- `src/main/resources/data/morph/advancements/morph/unlock_biomass.json`
- `src/main/resources/mobsupport.zip`

## Complete corrected files

Every changed Java/resource/build text file is copied under `ported_complete_files/` using its project-relative path.

## Known remaining limitations

1. No local ForgeGradle dependency cache was available, so the full Gradle compile could not be run here.
2. Forge 1.20.1 does not expose the old `EntityViewRenderEvent` API shown in the original source. The SwimmerTrait fog-event callbacks were therefore removed from the direct event-bus implementation rather than pretending the old event still exists. This means the swimming-specific custom fog behavior needs a target-version `FogRenderer` integration if exact fog behavior is required.
3. The generated `ported_complete_files` directory is documentation/delivery material only; it is not a second source tree and is not referenced by Gradle.


## Follow-up fixes from compileApiJava log
The 15 reported compile errors were addressed: BiomeTags import removal, I18n API migration, MOB_EFFECTS registry, key mapping `isDown`, packet `send`, AIR_LEVEL overlay, BlockPos `above`, max air API, and the API/main-source-set accessor dependency. The server anti-flying counter access in FlightFlapAbility now uses guarded reflection so the API source set does not depend on a main-source-set mixin class. Known SwimmerTrait fog behavior remains unavailable through the removed 1.16.5 fog event API.
