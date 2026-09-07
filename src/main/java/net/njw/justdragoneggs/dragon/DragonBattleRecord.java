package net.njw.justdragoneggs.dragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record DragonBattleRecord(int dragonNumber, UUID dragonUuid, Optional<UUID> killerUuid, Optional<String> killerName, long killedAt, List<PlayerDamageRecord> playerDamage, List<OtherEntry> otherDamage, double totalHealing) {
    public static final Codec<DragonBattleRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("dragon_number").forGetter(DragonBattleRecord::dragonNumber),
            UUIDUtil.CODEC.fieldOf("dragon_uuid").forGetter(DragonBattleRecord::dragonUuid),
            UUIDUtil.CODEC.optionalFieldOf("killer_uuid").forGetter(DragonBattleRecord::killerUuid),
            Codec.STRING.optionalFieldOf("killer_name").forGetter(DragonBattleRecord::killerName),
            Codec.LONG.fieldOf("killed_at").forGetter(DragonBattleRecord::killedAt),
            PlayerDamageRecord.CODEC.listOf().fieldOf("player_damage").forGetter(DragonBattleRecord::playerDamage),
            OtherEntry.CODEC.listOf().fieldOf("other_damage").forGetter(DragonBattleRecord::otherDamage),
            Codec.DOUBLE.optionalFieldOf("total_healing", 0.0).forGetter(DragonBattleRecord::totalHealing)
    ).apply(instance, DragonBattleRecord::new));

    public double totalPlayerDamage() {
        return playerDamage.stream().mapToDouble(PlayerDamageRecord::totalDamage).sum();
    }

    public double totalOtherDamage() {
        return otherDamage.stream().mapToDouble(OtherEntry::damage).sum();
    }

    public double totalDamage() {
        return totalPlayerDamage() + totalOtherDamage();
    }

    public record OtherEntry(OtherDamageMethod method, double damage) {
        public static final Codec<OtherEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                OtherDamageMethod.CODEC.fieldOf("method").forGetter(OtherEntry::method),
                Codec.DOUBLE.fieldOf("damage").forGetter(OtherEntry::damage)
        ).apply(instance, OtherEntry::new));
    }
}
