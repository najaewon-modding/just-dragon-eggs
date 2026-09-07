package net.njw.justdragoneggs;

public final class DragonEggColors {
    public static final int COLOR_1 = 0xFFFFC94A;
    public static final int COLOR_2 = 0xFFF2F5F8;
    public static final int COLOR_3 = 0xFFD8894A;
    public static final int COLOR_4 = 0xFFD0D0D0;
    public static final int COLOR_5 = 0xFF9F9F9F;
    public static final int WHITE = 0xFFFFFFFF;

    private DragonEggColors() {}

    public static int dragonNumberColor(int number) {
        if (number == 1) return COLOR_1;
        if (number == 2) return COLOR_2;
        if (number == 3) return COLOR_3;
        if (number <= 10) return COLOR_4;
        return COLOR_5;
    }

    public static int rankingColor(int rank) {
        if (rank == 1) return COLOR_1;
        if (rank == 2) return COLOR_2;
        if (rank == 3) return COLOR_3;
        return COLOR_4;
    }
}
