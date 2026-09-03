package me.ichun.mods.morph.api.mob.trait;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

public class SunburnTrait extends Trait<SunburnTrait>
{
    public Boolean childrenAreImmune;

    public SunburnTrait()
    {
        type = "traitSunburn";
    }

    @Override
    public void tick(float strength)
    {
        if(player.isAlive() && strength == 1F && shouldPlayerBurn() && isPlayerInDaylight())
        {
            boolean shouldBurn = true;

            ItemStack itemstack = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!itemstack.isEmpty()) {
                if (itemstack.isDamageableItem()) {
                    itemstack .setDamageValue(itemstack.getDamageValue() + player.getRandom().nextInt(2));
                    if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
                        player.broadcastBreakEvent(EquipmentSlot.HEAD);
                        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    }
                }

                shouldBurn = false;
            }

            if (shouldBurn)
            {
                player.setSecondsOnFire(8);
            }
        }
    }

    public boolean shouldPlayerBurn()
    {
        if(childrenAreImmune != null && childrenAreImmune)
        {
            MorphInfo morphInfo = MorphApi.getApiImpl().getMorphInfo(player);
            LivingEntity morphEntity = morphInfo.getActiveMorphEntity();
            return morphEntity != null && morphEntity.isBaby();
        }
        return true;
    }

    public boolean isPlayerInDaylight() //taken from Mob.isInDaylight()
    {
        if (player.level().isDay() && !player.level().isClientSide) {
            float f = player.getLightLevelDependentMagicValue();
            BlockPos blockpos = player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat ? (BlockPos.containing(player.getX(), Math.round(player.getY()), player.getZ())).up() : BlockPos.containing(player.getX(), Math.round(player.getY()), player.getZ());
            if (f > 0.5F && player.getRandom().nextFloat() * 30.0F < (f - 0.4F) * 2.0F && player.level().canSeeSky(blockpos)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public SunburnTrait copy()
    {
        SunburnTrait trait = new SunburnTrait();
        trait.childrenAreImmune = this.childrenAreImmune;
        return trait;
    }
}
