package me.ichun.mods.morph.client.gui.biomass.window.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.Element;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.common.morph.MorphHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class ElementRipple extends Element<ElementBiomassUpgrades>
{
    public final RenderType RIPPLE = RenderType.create("ripple", DefaultVertexFormat.POSITION_COLOR_TEX, com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_STRIP, 256, true, false, RenderType.CompositeState.builder()
                .setTextureState(new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(MorphHandler.INSTANCE.getMorphSkinTexture(), false, false))
                .setCullState(new net.minecraft.client.renderer.RenderStateShard.CullStateShard(true))
                .setLightmapState(new net.minecraft.client.renderer.RenderStateShard.LightmapStateShard(false))
                .setTransparencyState(net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard.TRANSLUCENT_TRANSPARENCY)
                .createCompositeState(false));

    public int age;

    public ElementRipple(@Nonnull ElementBiomassUpgrades parent)
    {
        super(parent);
    }

    @Override
    public void tick()
    {
        age++;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTick)
    {
        float prog = 10F * Mth.clamp((age + partialTick) / 20, 0F, 1F);
        double dist = ElementUpgradeNode.SIZE * 3.5D;

        //log x = (x^2) / 100 when x == 10, y = 1
        if(prog >= 1F)
        {
            float alpha = 1F - EntityHelper.sineifyProgress(Mth.clamp((age - 10 + partialTick) / 10F, 0F, 1F));

            double travDist = dist * Math.log10(prog);
            int slices = getWorkspace().getMinecraft().options.graphicsMode().get() == GraphicsStatus.FAST ? 30 : 100;

            Matrix4f matrix = stack.last().pose();
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().getBufferSource();
            VertexConsumer bufferbuilder = bufferSource.getBuffer(RIPPLE);
            float texScale = 100F;
            for(int i = 0; i <= slices; i++)
            {
                double angle = Math.PI * 2 * i / slices;
                float logX = (float)(Math.cos(angle) * travDist);
                float logY = (float)(Math.sin(angle) * travDist);
                bufferbuilder.vertex(matrix, getLeft() + logX, getTop() + logY, 0).color(1F, 1F, 1F, alpha).uv(logX / texScale, logY / texScale).endVertex();
                bufferbuilder.vertex(matrix, getLeft(), getTop(), 0).color(1F, 1F, 1F, alpha).uv(0, 0).endVertex();
            }
            bufferSource.endBatch();

            float nextProg = 10F * Mth.clamp((age + 1 + partialTick) / 20, 0F, 1F);
            double nextTravDist = dist * Math.log10(nextProg);

            Vec3 ourVec = getAsVector();
            ArrayList<ElementUpgradeNode> activeNodes = parentFragment.getActiveNodes();
            for(ElementUpgradeNode node : activeNodes)
            {
                double nodeDist = ourVec.distanceTo(node.getAsVector());
                if(nodeDist < dist && nodeDist < nextTravDist && nodeDist > travDist && nodeDist > ElementUpgradeNode.SIZE) //will be hit by ripple next tick, and is not within our source
                {
                    Vec3 diff = getAsVector().subtract(node.getAsVector());
                    Vec3 normal = diff.normalize();
                    double mag = (dist - nodeDist) / dist * 0.5D;
                    Vec3 mul = normal.multiply(mag, mag, mag);
                    node.pushX -= mul.x;
                    node.pushY -= mul.y;
                }
            }
        }
    }

    public Vec3 getAsVector()
    {
        return new Vec3(posX, posY, 0D);
    }


    @Override
    public int getLeft()
    {
        return super.getLeft() + parentFragment.offsetX;
    }

    @Override
    public int getRight()
    {
        return super.getRight() + parentFragment.offsetX;
    }

    @Override
    public int getTop()
    {
        return super.getTop() + parentFragment.offsetY;
    }

    @Override
    public int getBottom()
    {
        return super.getBottom() + parentFragment.offsetY;
    }

}
