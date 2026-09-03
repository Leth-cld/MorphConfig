package me.ichun.mods.morph.api.mob.trait;

import me.ichun.mods.morph.api.MorphApi;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class IntimidateTrait extends Trait<IntimidateTrait>
        implements IEventBusRequired
{
    public String idToIntimidate;
    public String classToIntimidate;
    public Float distance;
    public Double farRunSpeed;
    public Double nearRunSpeed;

    public transient EntityType<?> idIntimidate;
    public transient Class<? extends LivingEntity> classIntimidate;
    public transient float lastStrength = 0F;

    public IntimidateTrait()
    {
        type = "traitIntimidate";
    }

    @Override
    public void addHooks()
    {
        if(idToIntimidate != null)
        {
            idIntimidate = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(idToIntimidate));
        }
        else if(classToIntimidate != null)
        {
            try
            {
                Class clz = Class.forName(classToIntimidate);
                if(LivingEntity.class.isAssignableFrom(clz))
                {
                    classIntimidate = clz;
                }
                else
                {
                    MorphApi.getLogger().warn("Found class to intimidate that is not a LivingEntity class: {}", classToIntimidate);
                }
            }
            catch(ClassNotFoundException e)
            {
                MorphApi.getLogger().warn("Could not find class to intimidate: {}", classToIntimidate);
            }
        }

        if(idIntimidate != null || classIntimidate != null)
        {
            super.addHooks();
            if(distance == null)
            {
                distance = 6F;
            }
            if(farRunSpeed == null)
            {
                farRunSpeed = 1.0D;
            }
            if(nearRunSpeed == null)
            {
                nearRunSpeed = 1.0D;
            }
        }
    }

    @Override
    public void tick(float strength)
    {
        lastStrength = strength;

        if(!player.level().isClientSide && strength == 1F && (idIntimidate != null || classIntimidate != null))
        {
            List<PathfinderMob> entitiesIntimidated = player.level().getEntitiesOfClass(PathfinderMob.class, player.getBoundingBox().inflate(distance, 3D, distance), p -> idIntimidate != null ? p.getType() == idIntimidate : classIntimidate.isInstance(p));
            for(Object o : entitiesIntimidated)
            {
                PathfinderMob creature = (PathfinderMob)o;

                //if the creature has no path, or the target path is < distance, make the creature run.
                if(creature.getNavigation().isDone() || player.distanceToSqr(creature.getNavigation().getTargetPos().getX(), creature.getNavigation().getTargetPos().getY(), creature.getNavigation().getTargetPos().getZ()) < distance * distance)
                {
                    Vec3 vector3d = DefaultRandomPos.getPosAway(creature, 16, 7, player.position());

                    if(vector3d != null && player.distanceToSqr(vector3d) > player.distanceToSqr(creature))
                    {
                        Path path = creature.getNavigation().createPath(vector3d.x, vector3d.y, vector3d.z, 0);

                        if(path != null)
                        {
                            double speed = creature.distanceToSqr(player) < 49D ? nearRunSpeed : farRunSpeed;
                            creature.getNavigation().moveTo(path, speed);
                        }
                    }
                }
                else //the creature is still running away from us
                {
                    double speed = creature.distanceToSqr(player) < 49D ? nearRunSpeed : farRunSpeed;
                    creature.getNavigation().setSpeedModifier(speed);
                }
            }
        }
    }

    @Override
    public IntimidateTrait copy()
    {
        IntimidateTrait trait = new IntimidateTrait();
        trait.idToIntimidate = this.idToIntimidate;
        trait.classToIntimidate = this.classToIntimidate;
        trait.distance = this.distance;
        trait.farRunSpeed = this.farRunSpeed;
        trait.nearRunSpeed = this.nearRunSpeed;
        return trait;
    }


    @SubscribeEvent
    public void onLivingSetTarget(LivingChangeTargetEvent event)
    {
        //if the target is the player and it's not the revenge target/entity attacking it, cancel
        if(lastStrength == 1F && event.getNewTarget() == player && (idIntimidate != null && idIntimidate.equals(event.getEntity().getType()) || classIntimidate != null && classIntimidate.isInstance(event.getEntity())) && !(event.getEntity().getLastHurtByMob() == player || event.getEntity().getLastHurtByMob() == player))
        {
            event.setNewTarget(null);
        }
    }
}
