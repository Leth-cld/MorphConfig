package me.ichun.mods.morph.api.event;

import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

public class MorphEvent extends PlayerEvent
{
    private final MorphVariant variant;
    private MorphEvent(Player player, MorphVariant variant)
    {
        super(player);
        this.variant = variant;
    }

    public MorphVariant getVariant()
    {
        return variant;
    }

    @Cancelable
    public static class CanAcquire extends MorphEvent
    {
        public CanAcquire(Player player, MorphVariant variant)
        {
            super(player, variant);
        }
    }

    @Cancelable
    public static class Acquire extends MorphEvent
    {
        public Acquire(Player player, MorphVariant variant)
        {
            super(player, variant);
        }
    }

    @Cancelable
    public static class Morph extends MorphEvent
    {
        public Morph(Player player, MorphVariant variant)
        {
            super(player, variant);
        }
    }
}
