package me.ichun.mods.morph.common.morph.save;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MorphSavedData extends SavedData
{
    public static final String ID = "morph_save";
    public HashMap<UUID, PlayerMorphData> playerMorphs = new HashMap<>();

    public MorphSavedData() {}

    public static MorphSavedData load(CompoundTag tag)
    {
        MorphSavedData data = new MorphSavedData();
        data.read(tag);
        return data;
    }

    private void read(CompoundTag tag)
    {
        playerMorphs.clear();

        int count = tag.getInt("count");
        for(int i = 0; i < count; i++)
        {
            PlayerMorphData playerData = new PlayerMorphData();
            playerData.read(tag.getCompound("morph_" + i));

            playerMorphs.put(playerData.owner, playerData);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        tag.putInt("count", playerMorphs.size());

        int i = 0;
        for(Map.Entry<UUID, PlayerMorphData> entry : playerMorphs.entrySet())
        {
            tag.put("morph_" + i, entry.getValue().write(new CompoundTag()));
            i++;
        }

        return tag;
    }
}
