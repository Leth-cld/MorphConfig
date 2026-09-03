package me.ichun.mods.morph.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityInvokerMixin
{
    @Invoker
    void callPlayStepSound(BlockPos pos, BlockState blockState);

    @Invoker
    void callPlaySwimSound(float volume);

}
