package me.ichun.mods.morph.client.gui.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.Workspace;
import me.ichun.mods.ichunutil.client.gui.bns.window.constraint.Constraint;
import me.ichun.mods.morph.client.gui.mob.window.WindowMobData;
import me.ichun.mods.morph.common.Morph;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WorkspaceMobData extends Workspace
{
    public static final int PADDING_VERTICAL = 15;

    public final WindowMobData windowMobData;

    public WorkspaceMobData(Screen lastScreen)
    {
        super(lastScreen, Component.translatable("morph.gui.workspace.mobData.title"), Morph.configClient.guiMinecraftStyle);

        windowMobData = new WindowMobData(this);
        windowMobData.size(0, 20);
        windowMobData.constraints().top(this, Constraint.Property.Type.TOP, PADDING_VERTICAL).bottom(this, Constraint.Property.Type.BOTTOM, PADDING_VERTICAL).width(this, Constraint.Property.Type.WIDTH, 85);
        windows.add(windowMobData); //add to end of list
    }

    @Override
    public boolean canDockWindows()
    {
        return false;
    }

    @Override
    public void renderBackground(PoseStack stack)
    {
        this.renderBackground(stack, 0);


        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void resetBackground()
    {
    }
}
