package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WaterBreatherTrait extends Trait<WaterBreatherTrait>
        implements IEventBusRequired
{
    public Boolean suffocatesOnLand;

    public transient int air = -100;

    public WaterBreatherTrait()
    {
        type = "traitWaterBreather";
    }

    @Override
    public void tick(float strength)
    {
        if(strength == 1F && player.isAlive())
        {
            if(air == -100)
            {
                air = player.getAirSupply();
            }

            //if the player is in water, add air
            if (player.isEyeInFluid(FluidTags.WATER))
            {
                //Taken from determineNextAir in LivingEntity
                air = Math.min(air + 4, player.getMaxAir());
                player.setAirSupply(air);
            }
            else if (suffocatesOnLand != null && suffocatesOnLand) //if the player is on land and the entity suffocates
            {
                //taken from decreaseAirSupply in Living Entity
                int i = EnchantmentHelper.getRespiration(player);
                air = i > 0 && player.getRandom().nextInt(i + 1) > 0 ? air : air - 1;

                if(air == -20)
                {
                    air = 0;

                    player.hurt(player.damageSources().drown(), 2F);
                }

                player.setAirSupply(air);
            }
        }
    }

    @Override
    public WaterBreatherTrait copy()
    {
        WaterBreatherTrait trait = new WaterBreatherTrait();
        trait.suffocatesOnLand = this.suffocatesOnLand;
        return trait;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderGameOverlayPre(RenderGuiOverlayEvent.Pre event)
    {
        if(event.getOverlay() == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.AIR.type() && Minecraft.getInstance().getCameraEntity() == player)
        {
            //No need to draw the air bubbles if air < 300, default GUI already does that.
            if(player.isEyeInFluid(FluidTags.WATER) && air >= 300) //player's in water but also max air.
            {
                event.setCanceled(true);
            }
        }
    }
}
