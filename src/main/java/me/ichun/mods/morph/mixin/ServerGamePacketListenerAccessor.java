package me.ichun.mods.morph.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {
    @Accessor("aboveGroundTickCount")
    void morph$setAboveGroundTickCount(int value);
}
