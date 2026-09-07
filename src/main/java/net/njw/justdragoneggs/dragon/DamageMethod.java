package net.njw.justdragoneggs.dragon;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum DamageMethod implements StringRepresentable {
    MELEE("melee"),
    ARROW("arrow"),
    TRIDENT("trident"),
    FIREWORK("firework"),
    PROJECTILE("projectile"),
    BED("bed"),
    RESPAWN_ANCHOR("respawn_anchor"),
    END_CRYSTAL("end_crystal"),
    TNT("tnt"),
    THORNS("thorns"),
    OTHER_PLAYER("other_player");

    public static final Codec<DamageMethod> CODEC = StringRepresentable.fromEnum(DamageMethod::values);
    private final String name;

    DamageMethod(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
