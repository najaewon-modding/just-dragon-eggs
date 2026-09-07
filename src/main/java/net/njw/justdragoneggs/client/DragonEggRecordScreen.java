package net.njw.justdragoneggs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DragonEggRecordScreen extends Screen {
    public static final int COLOR_1 = 0xFFFFE45C;
    public static final int COLOR_2 = 0xFFE8F2FF;
    public static final int COLOR_3 = 0xFFFFA85C;
    public static final int COLOR_4 = 0xFFE8E8E8;
    public static final int COLOR_5 = 0xFF8E8E8E;
    public static final int COLOR_6 = COLOR_4;

    private static final String KILLER = "JWN__";
    private static final double KILLER_DAMAGE = 26.54;
    private static final double OTHER_DAMAGE = (100.0 - KILLER_DAMAGE) / 9.0;
    private static final List<Entry> ENTRIES = createEntries();

    public DragonEggRecordScreen() {
        super(Component.translatable("entity.minecraft.ender_dragon"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int top = Math.max(28, this.height / 2 - 92);
        drawTitle(graphics, centerX, top);
        graphics.centeredText(this.font, Component.translatable("screen.njw_just_dragon_eggs.slain_by", KILLER), centerX, top + 16, 0xFFAAAAAA);
        int left = centerX - 100;
        int right = centerX + 100;
        int y = top + 42;
        graphics.text(this.font, Component.translatable("screen.njw_just_dragon_eggs.player"), left, y, 0xFFAAAAAA);
        Component damageHeader = Component.translatable("screen.njw_just_dragon_eggs.damage");
        graphics.text(this.font, damageHeader, right - this.font.width(damageHeader), y, 0xFFAAAAAA);
        y += 14;
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            Component rankAndName = Component.literal((i + 1) + ".  " + entry.name());
            Component damage = Component.literal(String.format(Locale.ROOT, "%.2f%%", entry.damage()));
            int rank = i + 1;
            int color = rankColor(rank);
            if (rank <= 3) {
                drawGlowText(graphics, rankAndName, left, y, color);
                drawGlowText(graphics, damage, right - this.font.width(damage), y, color);
            } else {
                graphics.text(this.font, rankAndName, left, y, color);
                graphics.text(this.font, damage, right - this.font.width(damage), y, color);
            }
            y += 13;
        }
        graphics.centeredText(this.font, Component.translatable("screen.njw_just_dragon_eggs.temporary_data"), centerX, y + 8, 0xFF777777);
    }

    private void drawTitle(GuiGraphicsExtractor graphics, int centerX, int y) {
        Component number = Component.literal("#1 ");
        Component dragon = Component.translatable("entity.minecraft.ender_dragon");
        int totalWidth = this.font.width(number) + this.font.width(dragon);
        int x = centerX - totalWidth / 2;
        drawGlowText(graphics, number, x, y, COLOR_1);
        int dragonX = x + this.font.width(number);
        graphics.text(this.font, dragon, dragonX + 1, y + 1, 0xFF3F3F3F);
        graphics.text(this.font, dragon, dragonX, y, COLOR_6);
    }

    private void drawGlowText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        int glow = (color & 0x00FFFFFF) | 0x55000000;
        graphics.text(this.font, text, x - 1, y, glow);
        graphics.text(this.font, text, x + 1, y, glow);
        graphics.text(this.font, text, x, y - 1, glow);
        graphics.text(this.font, text, x, y + 1, glow);
        graphics.text(this.font, text, x, y, color);
    }

    private static int rankColor(int rank) {
        return switch (rank) {
            case 1 -> COLOR_1;
            case 2 -> COLOR_2;
            case 3 -> COLOR_3;
            case 4 -> COLOR_4;
            case 5 -> COLOR_5;
            default -> COLOR_6;
        };
    }

    private static List<Entry> createEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(KILLER, KILLER_DAMAGE));
        for (int i = 1; i <= 9; i++) entries.add(new Entry("Player " + i, OTHER_DAMAGE));
        return List.copyOf(entries);
    }

    private record Entry(String name, double damage) {}
}
