package net.njw.justdragoneggs.dragon;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum OtherDamageMethod implements StringRepresentable {
    TNT("tnt"),
    END_CRYSTAL("end_crystal"),
    EXPLOSION("explosion"),
    MOB("mob"),
    ENVIRONMENT("environment"),
    BAD_RESPAWN_POINT("bad_respawn_point"),
    UNKNOWN("unknown");

    public static final Codec<OtherDamageMethod> CODEC = StringRepresentable.fromEnum(OtherDamageMethod::values);
    private final String name;

    OtherDamageMethod(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
