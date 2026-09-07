package net.njw.justdragoneggs.dragon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

public record PlayerDamageRecord(UUID playerUuid, String playerName, List<Entry> damageByMethod) {
    public static final Codec<PlayerDamageRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_uuid").forGetter(PlayerDamageRecord::playerUuid),
            Codec.STRING.fieldOf("player_name").forGetter(PlayerDamageRecord::playerName),
            Entry.CODEC.listOf().fieldOf("damage_by_method").forGetter(PlayerDamageRecord::damageByMethod)
    ).apply(instance, PlayerDamageRecord::new));

    public double totalDamage() {
        return damageByMethod.stream().mapToDouble(Entry::damage).sum();
    }

    public record Entry(DamageMethod method, Optional<Identifier> itemId, double damage) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DamageMethod.CODEC.fieldOf("method").forGetter(Entry::method),
                Identifier.CODEC.optionalFieldOf("item").forGetter(Entry::itemId),
                Codec.DOUBLE.fieldOf("damage").forGetter(Entry::damage)
        ).apply(instance, Entry::new));

        public String debugName() {
            if (method == DamageMethod.DIRECT && itemId.isEmpty()) return "hand";
            return itemId.map(id -> method.getSerializedName() + "[" + id + "]").orElse(method.getSerializedName());
        }
    }
}
