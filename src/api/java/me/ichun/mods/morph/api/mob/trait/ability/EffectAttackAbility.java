package me.ichun.mods.morph.api.mob.trait.ability;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.trait.IEventBusRequired;
import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectAttackAbility extends Ability<EffectAttackAbility>
        implements IEventBusRequired
{
    public String effectId;
    public Integer duration;
    public Integer amplifier;

    public transient MobEffect effectObj;
    public transient float lastStrength = 0F;

    public EffectAttackAbility()
    {
        type = "abilityEffectAttack";
    }

    @Override
    public void addHooks()
    {
        if(effectId != null)
        {
            ResourceLocation effectRL = new ResourceLocation(effectId);
            MobEffect theEffect = ForgeRegistries.MOB_EFFECTS.getValue(effectRL);
            if(theEffect != null)
            {
                effectObj = theEffect;
                super.addHooks();
            }
        }
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;
    }

    @Override
    public EffectAttackAbility copy()
    {
        EffectAttackAbility trait = new EffectAttackAbility();
        trait.effectId = this.effectId;
        return trait;
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof EffectAttackAbility)
        {
            return effectId != null && effectId.equals(((EffectAttackAbility)trait).effectId);
        }
        return false;
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        if(lastStrength == 1F && event.getSource().getEntity() == player && MorphApi.getApiImpl().canUseAbility(player, this))
        {
            event.getEntity().addEffect(new MobEffectInstance(effectObj, duration != null ? duration : 200, amplifier != null ? amplifier: 0));
        }
    }
}
