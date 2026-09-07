package net.njw.justdragoneggs.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class DragonEggRecordScreen extends Screen {
    public static final int COLOR_1 = 0xFFFFD700;
    public static final int COLOR_2 = 0xFFC0C0C0;
    public static final int COLOR_3 = 0xFFCD7F32;
    public static final int COLOR_4 = 0xFF55FFFF;
    public static final int COLOR_5 = 0xFFFF55FF;
    public static final int COLOR_6 = 0xFFFFFFFF;

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
        MutableComponent title = Component.literal("#1 ").withColor(COLOR_1).append(Component.translatable("entity.minecraft.ender_dragon").withColor(COLOR_6));
        graphics.centeredText(this.font, title, centerX, top, COLOR_6);
        graphics.centeredText(this.font, Component.literal("Slain by " + KILLER), centerX, top + 16, 0xFFAAAAAA);
        int left = centerX - 100;
        int right = centerX + 100;
        int y = top + 42;
        graphics.text(this.font, Component.literal("Player"), left, y, 0xFFAAAAAA);
        String damageHeader = "Damage";
        graphics.text(this.font, Component.literal(damageHeader), right - this.font.width(damageHeader), y, 0xFFAAAAAA);
        y += 14;
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            String rankAndName = (i + 1) + ".  " + entry.name();
            String damage = String.format(Locale.ROOT, "%.2f%%", entry.damage());
            int color = rankColor(i + 1);
            graphics.text(this.font, Component.literal(rankAndName), left, y, color);
            graphics.text(this.font, Component.literal(damage), right - this.font.width(damage), y, color);
            y += 13;
        }
        graphics.centeredText(this.font, Component.literal("Temporary battle data"), centerX, y + 8, 0xFF777777);
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
