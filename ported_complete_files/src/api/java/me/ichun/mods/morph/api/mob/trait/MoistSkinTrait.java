package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MoistSkinTrait extends Trait<MoistSkinTrait>
        implements IEventBusRequired
{
    public Integer maxMoistness;

    public transient int moistness = -100;
    public transient int lastAir;

    public MoistSkinTrait()
    {
        type = "traitMoistSkin";
    }

    @Override
    public void addHooks()
    {
        if(maxMoistness == null)
        {
            maxMoistness = 2400;
        }
        super.addHooks();
    }

    @Override
    public void tick(float strength)
    {
        if(moistness == -100)
        {
            moistness = maxMoistness;
        }

        if (player.isInWaterRainOrBubble())
        {
            moistness = maxMoistness;
        }
        else
        {
            moistness--;
            if(!player.level().isClientSide && moistness <= 0)
            {
                player.hurt(player.damageSources().dryOut(), 1.0F);
            }
        }

    }

    @Override
    public MoistSkinTrait copy()
    {
        return new MoistSkinTrait();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderGameOverlayPre(RenderGuiOverlayEvent.Pre event)
    {
        if(event.getOverlay() == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.AIR.type() && Minecraft.getInstance().getCameraEntity() == player)
        {
            int moistToAir = (int)Math.floor((float)moistness / maxMoistness * 300F);

            if(moistToAir < player.getAirSupply()) //if our moistness is lower than the air we have, then we override
            {
                lastAir =  player.getAirSupply();

                player.setAirSupply(moistToAir);
            }
            else
            {
                lastAir = -1000;
            }
        }
    }


    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGuiOverlayEvent.Post event)
    {
        if(event.getOverlay() == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.AIR.type() && Minecraft.getInstance().getCameraEntity() == player)
        {
            if(lastAir != -1000)
            {
                player.setAirSupply(lastAir);
            }
        }
    }
}
