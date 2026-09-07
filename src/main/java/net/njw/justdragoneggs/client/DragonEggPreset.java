package net.njw.justdragoneggs.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.njw.justdragoneggs.dragon.DamageMethod;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.dragon.PlayerDamageRecord;

final class DragonEggPreset {
    private static final double[] TOTALS = {35.0, 25.0, 22.0, 20.0, 18.0, 16.0, 14.0, 12.0, 10.0, 8.0, 6.0, 5.0, 4.0, 3.0, 2.0};

    private DragonEggPreset() {}

    static DragonBattleRecord create() {
        List<PlayerDamageRecord> players = new ArrayList<>(TOTALS.length);
        for (int rank = 1; rank <= TOTALS.length; rank++) players.add(player(rank, TOTALS[rank - 1]));
        UUID killer = players.getFirst().playerUuid();
        return new DragonBattleRecord(1, uuid("preset-dragon"), Optional.of(killer), Optional.of("JWN__"), 0L, List.copyOf(players), List.of(), 0.0, 200.0);
    }

    private static PlayerDamageRecord player(int rank, double total) {
        Identifier projectile = switch ((rank - 1) % 3) {
            case 1 -> id(Items.CROSSBOW);
            case 2 -> id(Items.TRIDENT);
            default -> id(Items.BOW);
        };
        DamageMethod special = switch ((rank - 1) % 3) {
            case 1 -> DamageMethod.END_CRYSTAL;
            case 2 -> DamageMethod.TNT;
            default -> DamageMethod.BED;
        };
        List<PlayerDamageRecord.Entry> methods = List.of(
                new PlayerDamageRecord.Entry(DamageMethod.PROJECTILE, Optional.of(projectile), total * 0.55),
                new PlayerDamageRecord.Entry(DamageMethod.DIRECT, Optional.of(id(Items.NETHERITE_AXE)), total * 0.30),
                new PlayerDamageRecord.Entry(special, Optional.empty(), total * 0.15)
        );
        String name = rank == 1 ? "JWN__" : String.format("Player%02d", rank);
        return new PlayerDamageRecord(uuid("preset-player-" + rank), name, methods);
    }

    private static Identifier id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    private static UUID uuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
