package net.njw.justdragoneggs.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.njw.justdragoneggs.dragon.DamageMethod;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.dragon.OtherDamageMethod;
import net.njw.justdragoneggs.dragon.PlayerDamageRecord;

public final class DragonEggRecordScreen extends Screen {
    public static final int COLOR_1 = 0xFFFFC94A;
    public static final int COLOR_2 = 0xFFF2F5F8;
    public static final int COLOR_3 = 0xFFD8894A;
    public static final int COLOR_4 = 0xFFD0D0D0;
    public static final int COLOR_5 = 0xFF9F9F9F;
    private static final int COLOR_DETAIL = 0xFFFFFFFF;
    private static final int VISIBLE_ROWS = 10;
    private static final int ROW_HEIGHT = 13;
    private static final int TOP_THREE_SEPARATOR_HEIGHT = 12;
    private static final int TOP_THREE_SEPARATOR_LEFT_INSET = 13;
    private static final int DETAIL_LIST_GAP = 3;

    private final DragonBattleRecord record;
    private final List<BattleEntry> battleEntries;
    private DetailSelection selected;
    private int scrollOffset;

    public DragonEggRecordScreen(DragonBattleRecord record) {
        super(Component.translatable("entity.minecraft.ender_dragon"));
        this.record = record;
        this.battleEntries = createBattleEntries(record);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int top = top();
        MutableComponent title = Component.literal("#" + record.dragonNumber() + " ").withColor(dragonNumberColor(record.dragonNumber())).append(Component.translatable("entity.minecraft.ender_dragon").withColor(COLOR_DETAIL));
        graphics.centeredText(this.font, title, centerX, top, COLOR_DETAIL);
        Component killer = record.killerName().<Component>map(Component::literal).orElseGet(() -> Component.translatable("screen.njw_just_dragon_eggs.unknown"));
        graphics.centeredText(this.font, Component.translatable("screen.njw_just_dragon_eggs.slain_by", killer), centerX, top + 16, COLOR_DETAIL);
        if (selected == null) renderRanking(graphics, centerX, top);
        else renderDetails(graphics, centerX, top);
        if (isInteractive(mouseX, mouseY)) graphics.requestCursor(CursorTypes.POINTING_HAND);
    }

    private void renderRanking(GuiGraphicsExtractor graphics, int centerX, int top) {
        int left = centerX - 100;
        int rankRight = left + 24;
        int nameLeft = left + 32;
        int right = centerX + 100;
        int y = top + 42;
        graphics.text(this.font, Component.translatable("screen.njw_just_dragon_eggs.player"), nameLeft, y, COLOR_DETAIL);
        Component damageHeader = Component.translatable("screen.njw_just_dragon_eggs.damage");
        graphics.text(this.font, damageHeader, right - this.font.width(damageHeader), y, COLOR_DETAIL);
        y += 14;

        int end = Math.min(battleEntries.size(), scrollOffset + VISIBLE_ROWS);
        for (int i = scrollOffset; i < end; i++) {
            BattleEntry entry = battleEntries.get(i);
            int color = entry.rank() > 0 ? rankingColor(entry.rank()) : COLOR_4;
            boolean shadow = entry.rank() > 0 && entry.rank() <= 3;
            if (entry.rank() > 0) {
                Component rankText = Component.literal(entry.rank() + ".");
                graphics.text(this.font, rankText, rankRight - this.font.width(rankText), y, color, shadow);
            }
            graphics.text(this.font, entry.name(), nameLeft, y, color, shadow);
            Component damageText = Component.literal(percent(entry.damage(), record.totalDamage()));
            graphics.text(this.font, damageText, right - this.font.width(damageText), y, color, shadow);
            y += ROW_HEIGHT;
            if (hasTopThreeSeparatorAfter(i)) {
                graphics.horizontalLine(left + TOP_THREE_SEPARATOR_LEFT_INSET, right, y + 3, COLOR_4);
                y += TOP_THREE_SEPARATOR_HEIGHT;
            }
        }
    }

    private void renderDetails(GuiGraphicsExtractor graphics, int centerX, int top) {
        int left = centerX - 100;
        int right = centerX + 100;
        int y = top + 42;
        graphics.text(this.font, backText(), left, y, COLOR_DETAIL);
        graphics.centeredText(this.font, selected.title(), centerX, y, COLOR_DETAIL);
        y += 18;
        graphics.text(this.font, Component.translatable("screen.njw_just_dragon_eggs.method"), left, y, COLOR_DETAIL);
        Component shareHeader = Component.translatable("screen.njw_just_dragon_eggs.share");
        graphics.text(this.font, shareHeader, right - this.font.width(shareHeader), y, COLOR_DETAIL);
        y += 14 + DETAIL_LIST_GAP;

        int end = Math.min(selected.methods().size(), scrollOffset + VISIBLE_ROWS);
        for (int i = scrollOffset; i < end; i++) {
            MethodEntry entry = selected.methods().get(i);
            graphics.text(this.font, entry.name(), left, y, COLOR_DETAIL);
            Component share = Component.literal(percent(entry.damage(), selected.totalDamage()));
            graphics.text(this.font, share, right - this.font.width(share), y, COLOR_DETAIL);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mouseX = event.x();
        double mouseY = event.y();
        int centerX = this.width / 2;
        int top = top();
        int left = centerX - 100;
        if (selected != null) {
            Component back = backText();
            if (mouseX >= left && mouseX < left + this.font.width(back) && mouseY >= top + 38 && mouseY < top + 55) {
                selected = null;
                scrollOffset = 0;
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        int nameLeft = left + 32;
        int y = top + 56;
        int end = Math.min(battleEntries.size(), scrollOffset + VISIBLE_ROWS);
        for (int i = scrollOffset; i < end; i++) {
            BattleEntry entry = battleEntries.get(i);
            if (mouseY >= y && mouseY < y + ROW_HEIGHT && mouseX >= nameLeft && mouseX < nameLeft + this.font.width(entry.name())) {
                select(entry);
                return true;
            }
            y += ROW_HEIGHT;
            if (hasTopThreeSeparatorAfter(i)) y += TOP_THREE_SEPARATOR_HEIGHT;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        int size = selected == null ? battleEntries.size() : selected.methods().size();
        int maxOffset = Math.max(0, size - VISIBLE_ROWS);
        int nextOffset = Math.clamp(scrollOffset + (scrollY < 0 ? 1 : -1), 0, maxOffset);
        if (nextOffset == scrollOffset) return false;
        scrollOffset = nextOffset;
        return true;
    }

    private boolean isInteractive(double mouseX, double mouseY) {
        int centerX = this.width / 2;
        int top = top();
        int left = centerX - 100;
        if (selected != null) {
            Component back = backText();
            return mouseX >= left && mouseX < left + this.font.width(back) && mouseY >= top + 38 && mouseY < top + 55;
        }

        int nameLeft = left + 32;
        int y = top + 56;
        int end = Math.min(battleEntries.size(), scrollOffset + VISIBLE_ROWS);
        for (int i = scrollOffset; i < end; i++) {
            BattleEntry entry = battleEntries.get(i);
            if (mouseY >= y && mouseY < y + ROW_HEIGHT) return mouseX >= nameLeft && mouseX < nameLeft + this.font.width(entry.name());
            y += ROW_HEIGHT;
            if (hasTopThreeSeparatorAfter(i)) y += TOP_THREE_SEPARATOR_HEIGHT;
        }
        return false;
    }

    private boolean hasTopThreeSeparatorAfter(int index) {
        return index >= 0 && index + 1 < battleEntries.size() && battleEntries.get(index).rank() == 3 && battleEntries.get(index + 1).rank() == 4;
    }

    private void select(BattleEntry entry) {
        List<MethodEntry> methods = new ArrayList<>();
        if (entry.player() != null) {
            for (PlayerDamageRecord.Entry damage : entry.player().damageByMethod()) methods.add(new MethodEntry(playerMethodName(damage), damage.damage()));
        } else {
            for (DragonBattleRecord.OtherEntry damage : record.otherDamage()) methods.add(new MethodEntry(otherMethodName(damage.method()), damage.damage()));
        }
        methods.sort(Comparator.comparingDouble(MethodEntry::damage).reversed());
        selected = new DetailSelection(entry.name(), List.copyOf(methods), entry.damage());
        scrollOffset = 0;
    }

    private static List<BattleEntry> createBattleEntries(DragonBattleRecord record) {
        List<PlayerDamageRecord> players = new ArrayList<>(record.playerDamage());
        players.sort(Comparator.comparingDouble(PlayerDamageRecord::totalDamage).reversed());
        List<BattleEntry> result = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            PlayerDamageRecord player = players.get(i);
            result.add(new BattleEntry(i + 1, Component.literal(player.playerName()), player.totalDamage(), player));
        }
        double other = record.totalOtherDamage();
        if (other > 0.0) result.add(new BattleEntry(0, Component.translatable("screen.njw_just_dragon_eggs.other"), other, null));
        return List.copyOf(result);
    }

    private static Component playerMethodName(PlayerDamageRecord.Entry entry) {
        if (entry.itemId().isPresent()) {
            Identifier id = entry.itemId().get();
            Component item = itemName(id);
            if (entry.method() == DamageMethod.DIRECT) {
                if (id.equals(BuiltInRegistries.ITEM.getKey(Items.BOW))) return Component.translatable("damage_method.njw_just_dragon_eggs.bow_direct");
                if (id.equals(BuiltInRegistries.ITEM.getKey(Items.CROSSBOW))) return Component.translatable("damage_method.njw_just_dragon_eggs.crossbow_direct");
                if (id.equals(BuiltInRegistries.ITEM.getKey(Items.TRIDENT))) return Component.translatable("damage_method.njw_just_dragon_eggs.trident_direct");
                return Component.translatable("damage_method.njw_just_dragon_eggs.direct_item", item);
            }
            if (entry.method() == DamageMethod.PROJECTILE) {
                if (id.equals(BuiltInRegistries.ITEM.getKey(Items.BOW))) return Component.translatable("damage_method.njw_just_dragon_eggs.bow_shot");
                if (id.equals(BuiltInRegistries.ITEM.getKey(Items.CROSSBOW))) return Component.translatable("damage_method.njw_just_dragon_eggs.crossbow_shot");
                if (id.equals(BuiltInRegistries.ITEM.getKey(Items.TRIDENT))) return Component.translatable("damage_method.njw_just_dragon_eggs.trident_thrown");
                return Component.translatable("damage_method.njw_just_dragon_eggs.projectile_item", item);
            }
            return item;
        }
        return switch (entry.method()) {
            case DIRECT -> Component.translatable("damage_method.njw_just_dragon_eggs.hand");
            case PROJECTILE -> Component.translatable("damage_method.njw_just_dragon_eggs.projectile");
            case FIREWORK -> Component.translatable("damage_method.njw_just_dragon_eggs.firework");
            case BED -> Component.translatable("damage_method.njw_just_dragon_eggs.bed");
            case RESPAWN_ANCHOR -> Component.translatable("damage_method.njw_just_dragon_eggs.respawn_anchor");
            case END_CRYSTAL -> Component.translatable("damage_method.njw_just_dragon_eggs.end_crystal");
            case TNT -> Component.translatable("damage_method.njw_just_dragon_eggs.tnt");
            case THORNS -> Component.translatable("damage_method.njw_just_dragon_eggs.thorns");
            case OTHER_PLAYER -> Component.translatable("damage_method.njw_just_dragon_eggs.other_player");
            case MELEE -> Component.translatable("damage_method.njw_just_dragon_eggs.melee");
            case ARROW -> Component.translatable("damage_method.njw_just_dragon_eggs.arrow");
            case TRIDENT -> Component.translatable("damage_method.njw_just_dragon_eggs.trident");
        };
    }

    private static Component otherMethodName(OtherDamageMethod method) {
        return Component.translatable("other_damage_method.njw_just_dragon_eggs." + method.getSerializedName());
    }

    private static Component itemName(Identifier id) {
        return BuiltInRegistries.ITEM.get(id).<Component>map(holder -> new ItemStack(holder.value()).getHoverName()).orElseGet(() -> Component.literal(id.toString()));
    }

    private static Component backText() {
        return Component.literal("< ").append(Component.translatable("screen.njw_just_dragon_eggs.back"));
    }

    private static String percent(double part, double total) {
        return String.format(Locale.ROOT, "%.2f%%", total <= 0 ? 0.0 : part / total * 100.0);
    }

    private int top() {
        return Math.max(28, this.height / 2 - 92);
    }

    public static int dragonNumberColor(int number) {
        if (number == 1) return COLOR_1;
        if (number == 2) return COLOR_2;
        if (number == 3) return COLOR_3;
        if (number <= 10) return COLOR_4;
        return COLOR_5;
    }

    private static int rankingColor(int rank) {
        if (rank == 1) return COLOR_1;
        if (rank == 2) return COLOR_2;
        if (rank == 3) return COLOR_3;
        return COLOR_4;
    }

    private record BattleEntry(int rank, Component name, double damage, PlayerDamageRecord player) {}
    private record MethodEntry(Component name, double damage) {}
    private record DetailSelection(Component title, List<MethodEntry> methods, double totalDamage) {}
}
