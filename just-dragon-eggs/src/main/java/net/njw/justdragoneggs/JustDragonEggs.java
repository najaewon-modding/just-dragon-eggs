package net.njw.justdragoneggs;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(JustDragonEggs.MODID)
public class JustDragonEggs {
    public static final String MODID = "njw_just_dragon_eggs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JustDragonEggs(IEventBus modEventBus, ModContainer modContainer) {
    }
}
