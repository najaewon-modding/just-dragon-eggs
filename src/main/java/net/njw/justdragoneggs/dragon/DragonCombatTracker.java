package net.njw.justdragoneggs.dragon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;

public final class DragonCombatTracker {
    private final UUID dragonUuid;
    private final Map<UUID, MutablePlayerDamage> playerDamage = new HashMap<>();
    private final EnumMap<OtherDamageMethod, Double> otherDamage = new EnumMap<>(OtherDamageMethod.class);
    private double totalHealing;

    public DragonCombatTracker(UUID dragonUuid) {
        this.dragonUuid = dragonUuid;
    }

    public void addPlayerDamage(UUID playerUuid, String playerName, DamageMethod method, Optional<Identifier> itemId, double damage) {
        if (damage <= 0) return;
        playerDamage.computeIfAbsent(playerUuid, uuid -> new MutablePlayerDamage(playerName)).damage.merge(new DamageKey(method, itemId), damage, Double::sum);
    }

    public void addOtherDamage(OtherDamageMethod method, double damage) {
        if (damage <= 0) return;
        otherDamage.merge(method, damage, Double::sum);
    }

    public void addHealing(double amount) {
        if (amount > 0) totalHealing += amount;
    }

    public DragonBattleRecord finish(int dragonNumber, Optional<UUID> killerUuid, Optional<String> killerName, long killedAt, double maxHealth) {
        List<PlayerDamageRecord> players = new ArrayList<>();
        for (var player : playerDamage.entrySet()) {
            List<PlayerDamageRecord.Entry> entries = player.getValue().damage.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Comparator.comparing((Map.Entry<DamageKey, Double> entry) -> entry.getKey().method().getSerializedName()).thenComparing(entry -> entry.getKey().itemId().map(Identifier::toString).orElse("")))
                    .map(entry -> new PlayerDamageRecord.Entry(entry.getKey().method(), entry.getKey().itemId(), entry.getValue()))
                    .toList();
            players.add(new PlayerDamageRecord(player.getKey(), player.getValue().name, entries));
        }
        players.sort(Comparator.comparingDouble(PlayerDamageRecord::totalDamage).reversed());
        List<DragonBattleRecord.OtherEntry> others = otherDamage.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DragonBattleRecord.OtherEntry(entry.getKey(), entry.getValue()))
                .toList();
        return new DragonBattleRecord(dragonNumber, dragonUuid, killerUuid, killerName, killedAt, List.copyOf(players), List.copyOf(others), totalHealing, maxHealth);
    }

    private record DamageKey(DamageMethod method, Optional<Identifier> itemId) {}

    private static final class MutablePlayerDamage {
        private final String name;
        private final Map<DamageKey, Double> damage = new HashMap<>();

        private MutablePlayerDamage(String name) {
            this.name = name;
        }
    }
}
