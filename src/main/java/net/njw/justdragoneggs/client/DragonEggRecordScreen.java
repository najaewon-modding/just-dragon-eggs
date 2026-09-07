package net.njw.justdragoneggs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class DragonEggRecordScreen extends Screen {
    public static final int COLOR_1 = 0xFFFFC94A;
    public static final int COLOR_2 = 0xFFD9E0E8;
    public static final int COLOR_3 = 0xFFD8894A;
    public static final int COLOR_4 = 0xFFD0D0D0;
    public static final int COLOR_5 = 0xFFBCBCBC;

    private static final String KILLER = "JWN__";
    private static final double KILLER_DAMAGE = 26.54;
    private static final int VISIBLE_ROWS = 10;
    private static final double OTHER_DAMAGE = (100.0 - KILLER_DAMAGE) / 14.0;
    private static final List<Entry> ENTRIES = createEntries();
    private int scrollOffset;

    public DragonEggRecordScreen() {
        super(Component.translatable("entity.minecraft.ender_dragon"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int top = Math.max(28, this.height / 2 - 92);
        MutableComponent title = Component.literal("#1 ").withColor(COLOR_1).append(Component.translatable("entity.minecraft.ender_dragon").withColor(COLOR_4));
        graphics.centeredText(this.font, title, centerX, top, COLOR_4);
        graphics.centeredText(this.font, Component.translatable("screen.njw_just_dragon_eggs.slain_by", KILLER), centerX, top + 16, 0xFFAAAAAA);

        int left = centerX - 100;
        int rankRight = left + 24;
        int nameLeft = left + 32;
        int right = centerX + 100;
        int y = top + 42;
        graphics.text(this.font, Component.translatable("screen.njw_just_dragon_eggs.player"), nameLeft, y, 0xFFAAAAAA);
        Component damageHeader = Component.translatable("screen.njw_just_dragon_eggs.damage");
        graphics.text(this.font, damageHeader, right - this.font.width(damageHeader), y, 0xFFAAAAAA);
        y += 14;

        int end = Math.min(ENTRIES.size(), scrollOffset + VISIBLE_ROWS);
        for (int i = scrollOffset; i < end; i++) {
            Entry entry = ENTRIES.get(i);
            int rank = i + 1;
            int color = playerRankColor(rank);
            Component rankText = Component.literal(rank + ".");
            Component nameText = Component.literal(entry.name());
            Component damageText = Component.literal(String.format(Locale.ROOT, "%.2f%%", entry.damage()));
            graphics.text(this.font, rankText, rankRight - this.font.width(rankText), y, color);
            graphics.text(this.font, nameText, nameLeft, y, color);
            graphics.text(this.font, damageText, right - this.font.width(damageText), y, color);
            y += 13;
        }

        graphics.centeredText(this.font, Component.translatable("screen.njw_just_dragon_eggs.temporary_data"), centerX, top + 42 + 14 + VISIBLE_ROWS * 13 + 8, 0xFF777777);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        int maxOffset = Math.max(0, ENTRIES.size() - VISIBLE_ROWS);
        int nextOffset = Math.clamp(scrollOffset + (scrollY < 0 ? 1 : -1), 0, maxOffset);
        if (nextOffset == scrollOffset) return false;
        scrollOffset = nextOffset;
        return true;
    }

    public static int dragonNumberColor(int number) {
        if (number == 1) return COLOR_1;
        if (number == 2) return COLOR_2;
        if (number == 3) return COLOR_3;
        if (number <= 10) return COLOR_4;
        return COLOR_5;
    }

    private static int playerRankColor(int rank) {
        return switch (rank) {
            case 1 -> COLOR_1;
            case 2 -> COLOR_2;
            case 3 -> COLOR_3;
            default -> COLOR_4;
        };
    }

    private static List<Entry> createEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(KILLER, KILLER_DAMAGE));
        for (int i = 1; i <= 14; i++) entries.add(new Entry("Player " + i, OTHER_DAMAGE));
        return List.copyOf(entries);
    }

    private record Entry(String name, double damage) {}
}
