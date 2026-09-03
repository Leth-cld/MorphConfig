package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectResistanceTrait extends Trait<EffectResistanceTrait>
        implements IEventBusRequired
{
    public String effectId;

    public transient MobEffect effectObj;
    public transient float lastStrength = 0F;

    public EffectResistanceTrait()
    {
        type = "traitEffectResistance";
    }

    @Override
    public void addHooks()
    {
        if(effectId != null)
        {
            if(effectId.equals("*")) //immune to all effects
            {
                super.addHooks();
            }
            else
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
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;

        if(lastStrength == 1F && (effectObj != null || "*".equals(effectId)))
        {
            if(effectId.equals("*")) //immune to all effects
            {
                for(MobEffectInstance effect : player.getActiveEffects())
                {
                    player.removeEffect(effect.getEffect());
                }
            }
            else
            {
                MobEffectInstance potion = player.getEffect(effectObj);
                if(potion != null)
                {
                    player.removeEffect(effectObj);
                }
            }
        }
    }

    @Override
    public EffectResistanceTrait copy()
    {
        EffectResistanceTrait trait = new EffectResistanceTrait();
        trait.effectId = this.effectId;
        return trait;
    }

    @Override
    public boolean canTransitionTo(Trait<?> trait)
    {
        if(trait instanceof EffectResistanceTrait)
        {
            return effectId != null && effectId.equals(((EffectResistanceTrait)trait).effectId);
        }
        return false;
    }

    @SubscribeEvent
    public void onPotionApplicable(MobEffectEvent.Applicable event)
    {
        if(lastStrength == 1F && event.getEntity() == player && (effectId != null && effectId.equals("*") || event.getEffectInstance().getEffect() == effectObj))
        {
            event.setResult(Event.Result.DENY);
        }
    }
}
