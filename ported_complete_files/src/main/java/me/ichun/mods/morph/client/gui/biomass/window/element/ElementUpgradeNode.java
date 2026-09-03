package me.ichun.mods.morph.client.gui.biomass.window.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.Element;
import me.ichun.mods.ichunutil.client.gui.bns.window.view.element.ElementToggleTextured;
import me.ichun.mods.ichunutil.common.entity.util.EntityHelper;
import me.ichun.mods.morph.api.biomass.BiomassUpgrade;
import me.ichun.mods.morph.api.biomass.BiomassUpgradeInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.function.Consumer;

public class ElementUpgradeNode extends ElementToggleTextured<ElementUpgradeNode>
{
    public static final RenderType MORPH_LINES = RenderType.create("morph_lines", DefaultVertexFormat.POSITION_COLOR, com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES,
            256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(4D)))
                    .setTransparencyState(RenderStateShard.TransparencyStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.WriteMaskStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));
    //TODO FINAL this
    public static int SIZE = 16;
    public static int TOLERANCE_MIN = 2 * SIZE;
    public static int TOLERANCE_MAX = 4 * SIZE;

    private ElementBiomassUpgrades parent;
    public ElementUpgradeNode parentNode; //null == anchored
    public ArrayList<ElementUpgradeNode> childNodes = new ArrayList<>();
    public int showTime;
    public double lastX;
    public double lastY;
    public double pushX;
    public double pushY;

    @Nonnull
    public BiomassUpgradeInfo upgradeInfo;

    @Nullable
    public BiomassUpgrade actualUpgrade;

    public ElementUpgradeNode(@Nonnull ElementBiomassUpgrades parent, BiomassUpgradeInfo upgradeInfo, BiomassUpgrade actualUpgrade, Consumer<ElementUpgradeNode> callback)
    {
        super(parent, "", upgradeInfo.getTextureLocation(), callback);
        this.parent = parent;
        this.showTime = -1;
        setWarping();
        this.upgradeInfo = upgradeInfo;
        this.actualUpgrade = actualUpgrade;
    }

    public void setParentNode(ElementUpgradeNode parent)
    {
        this.parentNode = parent;
        parent.childNodes.add(this);
    }

    public void allocateChildPlacements(Random rand)
    {
        for(ElementUpgradeNode childNode : childNodes)
        {
            rand.setSeed(Math.abs(childNode.upgradeInfo.id.hashCode() + Minecraft.getInstance().getUser().getUuid().hashCode()) * 42069L); //Blame Harogna for haha funny number
            childNode.setPos(posX + rand.nextInt(SIZE * 4), posY + rand.nextInt(SIZE * 4));
            childNode.allocateChildPlacements(rand);
        }
    }

    @Override
    public Element<?> setPos(int x, int y)
    {
        lastX = x;
        lastY = y;
        return super.setPos(x, y);
    }

    @Override
    public void tick()
    {
        if(parentNode != null)
        {
            lastX += pushX;
            lastY += pushY;

            pushX *= 0.98F;
            pushY *= 0.98F;

            if(Math.abs(pushX) < 0.001D)
            {
                pushX = 0D;
            }

            if(Math.abs(pushY) < 0.001D)
            {
                pushY = 0D;
            }

            posX = (int)Math.round(lastX);
            posY = (int)Math.round(lastY);
        }

        if(shouldShow()) //TODO shoTime only when parentNode has shown completely
        {
            showTime++;

            if(parentNode != null)
            {
                pushAwayFromOthers();
            }
        }
    }

    public void pushAwayFromOthers()
    {
        ArrayList<ElementUpgradeNode> activeNodes = parent.getActiveNodes();
        for(ElementUpgradeNode node : activeNodes)
        {
            if(node == this)
            {
                continue;
            }

            Vec3 diff = getAsVector().subtract(node.getAsVector());
            if(diff.equals(Vec3.ZERO))
            {
                diff = new Vec3(parent.rand.nextGaussian() * 3D, parent.rand.nextGaussian() * 3D, 0D); //add some difference
            }
            double dist = diff.length();
            if(dist < TOLERANCE_MIN) //TODO adapt to number of children per node?? more children = higher tolerance?
            {
                Vec3 normal = diff.normalize();
                double mag = (TOLERANCE_MIN - dist) / TOLERANCE_MIN * 0.1D;
                Vec3 mul = normal.multiply(mag, mag, mag);

                pushX += mul.x;
                pushY += mul.y;
            }
            else if(node == parentNode && dist > TOLERANCE_MAX)
            {
                Vec3 normal = diff.normalize();
                double mag = (TOLERANCE_MAX - dist) / TOLERANCE_MAX * 0.1D;
                Vec3 mul = normal.multiply(mag, mag, mag);

                pushX += mul.x;
                pushY += mul.y;
            }
        }
    }

    public Vec3 getAsVector()
    {
        return new Vec3(getCenterX(), getCenterY(), 0D);
    }

    public void renderLines(PoseStack stack, float partialTick)
    {
        if(parentNode != null)
        {
            float prog = EntityHelper.sineifyProgress(Mth.clamp((showTime + partialTick) / 10, 0F, 1F));
            if(prog > 0F)
            {
                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().getBufferSource();
                VertexConsumer builder = bufferSource.getBuffer(MORPH_LINES);
                Matrix4f matrix = stack.last().pose();
                float parX = parentNode.getLeft() + parentNode.width / 2F;
                float parY = parentNode.getTop() + parentNode.height / 2F;
                float diffX = (getLeft() + width / 2F) - parX;
                float diffY = (getTop() + height / 2F) - parY;
                builder.vertex(matrix, parX, parY, 0F).color(255, 255, 255, 200).endVertex();
                builder.vertex(matrix, parX + diffX * prog, parY + diffY * prog, 0F).color(255, 255, 255, 120).endVertex();
                bufferSource.endBatch();
            }
        }
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTick)
    {
        float prog = EntityHelper.sineifyProgress(Mth.clamp((showTime + partialTick) / 10, 0F, 1F));
        if(prog > 0F)
        {
            float curSize = SIZE * prog;

            if(curSize >= 4)
            {
                int preX = posX;
                int preY = posY;

                posX = (int)((float)posX + (SIZE - curSize) / 2F);
                posY = (int)((float)posY + (SIZE - curSize) / 2F);

                width = (int)curSize;
                height = (int)curSize;

                super.render(stack, mouseX, mouseY, partialTick);

                //TODO renders for if can afford or maxed etc

                posX = preX;
                posY = preY;
            }
        }
    }

    public boolean shouldShow()
    {
        return true;
    }

    public boolean isActive()
    {
        return showTime >= 0;
    }

    public int getCenterX()
    {
        return posX + width / 2;
    }

    public int getCenterY()
    {
        return posY + height / 2;
    }

    @Nullable
    @Override
    public String tooltip(double mouseX, double mouseY)
    {
        return upgradeInfo.id;//TODO debug//super.tooltip(mouseX, mouseY);
    }

    @Override
    public int getLeft()
    {
        return super.getLeft() + parent.offsetX;
    }

    @Override
    public int getRight()
    {
        return super.getRight() + parent.offsetX;
    }

    @Override
    public int getTop()
    {
        return super.getTop() + parent.offsetY;
    }

    @Override
    public int getBottom()
    {
        return super.getBottom() + parent.offsetY;
    }

    @Override
    public int getMaxWidth()
    {
        return SIZE;
    }

    @Override
    public int getMaxHeight()
    {
        return SIZE;
    }

    @Override
    public int getMinWidth()
    {
        return SIZE;
    }

    @Override
    public int getMinHeight()
    {
        return SIZE;
    }
}
