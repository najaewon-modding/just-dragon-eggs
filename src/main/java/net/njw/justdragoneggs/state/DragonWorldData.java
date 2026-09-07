package net.njw.justdragoneggs.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.njw.justdragoneggs.JustDragonEggs;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;

public final class DragonWorldData extends SavedData {
    private static final Codec<DragonWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("kill_count", 0).forGetter(data -> data.killCount),
            DragonBattleRecord.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(DragonWorldData::records)
    ).apply(instance, DragonWorldData::new));

    public static final SavedDataType<DragonWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(JustDragonEggs.MODID, "dragon_battle_records"),
            DragonWorldData::new,
            CODEC);

    private int killCount;
    private final Map<Integer, DragonBattleRecord> records = new LinkedHashMap<>();

    public DragonWorldData() {}

    private DragonWorldData(int killCount, List<DragonBattleRecord> records) {
        this.killCount = killCount;
        records.stream().sorted(Comparator.comparingInt(DragonBattleRecord::dragonNumber)).forEach(record -> this.records.put(record.dragonNumber(), record));
    }

    public static DragonWorldData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int nextDragonNumber() {
        return killCount + 1;
    }

    public void addRecord(DragonBattleRecord record) {
        records.put(record.dragonNumber(), record);
        killCount = Math.max(killCount, record.dragonNumber());
        setDirty();
    }

    public List<DragonBattleRecord> records() {
        return new ArrayList<>(records.values());
    }
}
