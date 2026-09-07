package net.njw.justdragoneggs.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.registry.ModContent;
import org.jspecify.annotations.Nullable;

public final class RecordedDragonEggBlockEntity extends BlockEntity {
    @Nullable
    private DragonBattleRecord record;

    public RecordedDragonEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RECORDED_DRAGON_EGG_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public DragonBattleRecord record() {
        return record;
    }

    public void setRecord(DragonBattleRecord record) {
        this.record = record;
        setChanged();
        sync();
    }

    public void sync() {
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        record = input.read("battle_record", DragonBattleRecord.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (record != null) output.store("battle_record", DragonBattleRecord.CODEC, record);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        DragonBattleRecord value = components.get(ModContent.BATTLE_RECORD.get());
        if (value != null) record = value;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (record != null) components.set(ModContent.BATTLE_RECORD.get(), record);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(Provider registries) {
        return saveCustomOnly(registries);
    }
}
