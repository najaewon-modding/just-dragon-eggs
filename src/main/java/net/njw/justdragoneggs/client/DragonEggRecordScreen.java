package net.njw.justdragoneggs.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DragonEggRecordScreen extends Screen {
    private static final String KILLER = "JWN__";
    private static final double KILLER_DAMAGE = 26.54;
    private static final double OTHER_DAMAGE = (100.0 - KILLER_DAMAGE) / 9.0;
    private static final List<Entry> ENTRIES = createEntries();

    public DragonEggRecordScreen() {
        super(Component.literal("Ender Dragon #1"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int top = Math.max(28, this.height / 2 - 92);
        graphics.centeredText(this.font, Component.literal("#1 Ender Dragon"), centerX, top, 0xFFFFFFFF);
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
            String damage = String.format(java.util.Locale.ROOT, "%.2f%%", entry.damage());
            int color = i == 0 ? 0xFFFFAAFF : 0xFFFFFFFF;
            graphics.text(this.font, Component.literal(rankAndName), left, y, color);
            graphics.text(this.font, Component.literal(damage), right - this.font.width(damage), y, color);
            y += 13;
        }
        graphics.centeredText(this.font, Component.literal("Temporary battle data"), centerX, y + 8, 0xFF777777);
    }

    private static List<Entry> createEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(KILLER, KILLER_DAMAGE));
        for (int i = 1; i <= 9; i++) entries.add(new Entry("Player " + i, OTHER_DAMAGE));
        return List.copyOf(entries);
    }

    private record Entry(String name, double damage) {}
}
