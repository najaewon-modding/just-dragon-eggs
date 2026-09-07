package net.njw.justdragoneggs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.njw.justdragoneggs.JustDragonEggs;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.dragon.PlayerDamageRecord;
import net.njw.justdragoneggs.state.DragonWorldData;

@EventBusSubscriber(modid = JustDragonEggs.MODID)
public final class DragonRecordCommands {
    private DragonRecordCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jdedata")
                .executes(context -> showLatest(context.getSource()))
                .then(Commands.literal("latest").executes(context -> showLatest(context.getSource())))
                .then(Commands.literal("list").executes(context -> listRecords(context.getSource())))
                .then(Commands.argument("number", IntegerArgumentType.integer(1)).executes(context -> showRecord(context.getSource(), IntegerArgumentType.getInteger(context, "number")))));
    }

    private static int listRecords(CommandSourceStack source) {
        DragonWorldData data = DragonWorldData.get(source.getLevel());
        List<DragonBattleRecord> records = data.records().stream().sorted(Comparator.comparingInt(DragonBattleRecord::dragonNumber)).toList();
        send(source, "Just Dragon Eggs: " + records.size() + " saved battle(s), kill count=" + data.killCount());
        for (DragonBattleRecord record : records) send(source, String.format(Locale.ROOT, "#%d killer=%s damage=%.2f healing=%.2f", record.dragonNumber(), record.killerName().orElse("unknown"), record.totalDamage(), record.totalHealing()));
        return records.size();
    }

    private static int showLatest(CommandSourceStack source) {
        DragonWorldData data = DragonWorldData.get(source.getLevel());
        return data.latestRecord().map(record -> showRecord(source, record)).orElseGet(() -> {
            send(source, "No saved Ender Dragon battles.");
            return 0;
        });
    }

    private static int showRecord(CommandSourceStack source, int number) {
        DragonWorldData data = DragonWorldData.get(source.getLevel());
        return data.getRecord(number).map(record -> showRecord(source, record)).orElseGet(() -> {
            send(source, "No saved battle #" + number + ".");
            return 0;
        });
    }

    private static int showRecord(CommandSourceStack source, DragonBattleRecord record) {
        double totalDamage = record.totalDamage();
        send(source, "=== Ender Dragon #" + record.dragonNumber() + " ===");
        send(source, "Killer: " + record.killerName().orElse("unknown") + " | UUID: " + record.dragonUuid());
        send(source, String.format(Locale.ROOT, "Total damage: %.2f | player: %.2f | other: %.2f | healing: %.2f", totalDamage, record.totalPlayerDamage(), record.totalOtherDamage(), record.totalHealing()));

        int rank = 1;
        for (PlayerDamageRecord player : record.playerDamage()) {
            double playerTotal = player.totalDamage();
            send(source, String.format(Locale.ROOT, "%d. %s: %.2f (%.2f%% of battle damage)", rank++, player.playerName(), playerTotal, percent(playerTotal, totalDamage)));
            for (PlayerDamageRecord.Entry entry : player.damageByMethod()) {
                send(source, String.format(Locale.ROOT, "   - %s: %.2f (%.2f%% of player damage)", entry.method().getSerializedName(), entry.damage(), percent(entry.damage(), playerTotal)));
            }
        }

        double otherTotal = record.totalOtherDamage();
        if (otherTotal > 0) {
            send(source, String.format(Locale.ROOT, "Other: %.2f (%.2f%% of battle damage)", otherTotal, percent(otherTotal, totalDamage)));
            for (DragonBattleRecord.OtherEntry entry : record.otherDamage()) {
                send(source, String.format(Locale.ROOT, "   - %s: %.2f (%.2f%% of other damage)", entry.method().getSerializedName(), entry.damage(), percent(entry.damage(), otherTotal)));
            }
        }
        return 1;
    }

    private static double percent(double value, double total) {
        return total <= 0 ? 0 : value * 100.0 / total;
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
