package net.njw.justdragoneggs.registry;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.njw.justdragoneggs.JustDragonEggs;
import net.njw.justdragoneggs.block.RecordedDragonEggBlock;
import net.njw.justdragoneggs.block.entity.RecordedDragonEggBlockEntity;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.item.RecordedDragonEggItem;

public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JustDragonEggs.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JustDragonEggs.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JustDragonEggs.MODID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, JustDragonEggs.MODID);

    public static final DeferredBlock<RecordedDragonEggBlock> RECORDED_DRAGON_EGG = BLOCKS.registerBlock("recorded_dragon_egg", RecordedDragonEggBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DRAGON_EGG));
    public static final DeferredItem<RecordedDragonEggItem> RECORDED_DRAGON_EGG_ITEM = ITEMS.registerItem("recorded_dragon_egg", properties -> new RecordedDragonEggItem(RECORDED_DRAGON_EGG.get(), properties.useBlockDescriptionPrefix()));
    public static final Supplier<BlockEntityType<RecordedDragonEggBlockEntity>> RECORDED_DRAGON_EGG_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("recorded_dragon_egg", () -> new BlockEntityType<>(RecordedDragonEggBlockEntity::new, RECORDED_DRAGON_EGG.get()));
    public static final Supplier<DataComponentType<DragonBattleRecord>> BATTLE_RECORD = DATA_COMPONENTS.registerComponentType("battle_record", builder -> builder.persistent(DragonBattleRecord.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(DragonBattleRecord.CODEC)));

    private ModContent() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
    }
}
