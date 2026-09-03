package me.ichun.mods.morph.client.gui.window.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.ichun.mods.ichunutil.client.gui.bns.window.Fragment;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.Element;
import me.ichun.mods.ichunutil.client.render.RenderHelper;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public class ElementRenderEntity extends Element<Fragment>
{
    private static final PoseStack LIGHT_STACK = Util.make(new PoseStack(), stack -> stack.translate(1D, -1D, 0D));

    @Nonnull
    public LivingEntity entToRender;

    public float renderScale;

    public ElementRenderEntity(@Nonnull Fragment parent)
    {
        super(parent);
        renderScale = 1.0F;
    }

    public ElementRenderEntity(@Nonnull Fragment parent, float scale)
    {
        super(parent);
        renderScale = scale;
    }

    public ElementRenderEntity setEntityToRender(LivingEntity ent)
    {
        entToRender = ent;
        return this;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTick)
    {
        if(renderMinecraftStyle() > 0)
        {
            bindTexture(resourceHorse());
            cropAndStitch(stack, getLeft() - 1, getTop() - 1, width + 2, height + 2, 2, 79, 17, 90, 54, 256, 256);
        }
        else
        {
            RenderHelper.drawColour(stack, getTheme().elementTreeBorder[0], getTheme().elementTreeBorder[1], getTheme().elementTreeBorder[2], 255, getLeft() - 1, getTop() - 1, width + 2, 1, 0); //top
            RenderHelper.drawColour(stack, getTheme().elementTreeBorder[0], getTheme().elementTreeBorder[1], getTheme().elementTreeBorder[2], 255, getLeft() - 1, getTop() - 1, 1, height + 2, 0); //left
            RenderHelper.drawColour(stack, getTheme().elementTreeBorder[0], getTheme().elementTreeBorder[1], getTheme().elementTreeBorder[2], 255, getLeft() - 1, getBottom(), width + 2, 1, 0); //bottom
            RenderHelper.drawColour(stack, getTheme().elementTreeBorder[0], getTheme().elementTreeBorder[1], getTheme().elementTreeBorder[2], 255, getRight(), getTop() - 1, 1, height + 2, 0); //right
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderHelper.drawColour(stack, 0, 0, 0, 255, getLeft(), getTop(), width, height, 0);

        EntityDimensions livingSize = entToRender.getDimensions(Pose.STANDING);
        float entSize = Math.max(livingSize.width, livingSize.height) / 1.95F; //1.95F = zombie height

        float entScale = renderScale * (1F / Math.max(1F, entSize));

        renderEntity(getLeft() + (width / 2D), getBottom() - 15 * renderScale, 100, entScale);
    }

    private void renderEntity(double x, double y, double z, float scale)
    {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.gui.GuiGraphics graphics =
                new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, (int)x, (int)y, Math.max(1, (int)(35 * scale)), 0F, 0F, entToRender);
    }

    @Override
    public int getMinHeight()
    {
        return height;
    }
}
