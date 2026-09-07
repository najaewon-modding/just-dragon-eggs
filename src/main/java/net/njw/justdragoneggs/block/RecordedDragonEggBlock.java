package net.njw.justdragoneggs.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.njw.justdragoneggs.block.entity.RecordedDragonEggBlockEntity;
import net.njw.justdragoneggs.dragon.DragonBattleRecord;
import net.njw.justdragoneggs.registry.ModContent;

public final class RecordedDragonEggBlock extends FallingBlock implements EntityBlock {
    public static final MapCodec<RecordedDragonEggBlock> CODEC = simpleCodec(RecordedDragonEggBlock::new);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    public RecordedDragonEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<RecordedDragonEggBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecordedDragonEggBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown()) teleport(state, level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown()) teleport(state, level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        teleport(state, level, pos);
    }

    private void teleport(BlockState state, Level level, BlockPos pos) {
        WorldBorder worldBorder = level.getWorldBorder();
        RandomSource random = level.getRandom();
        DragonBattleRecord record = level.getBlockEntity(pos) instanceof RecordedDragonEggBlockEntity egg ? egg.record() : null;

        for (int i = 0; i < 1000; i++) {
            BlockPos testPos = pos.offset(random.nextInt(16) - random.nextInt(16), random.nextInt(8) - random.nextInt(8), random.nextInt(16) - random.nextInt(16));
            if (level.getBlockState(testPos).isAir() && !level.getBlockState(testPos.below()).isAir() && worldBorder.isWithinBounds(testPos) && level.isInsideBuildHeight(testPos)) {
                if (level.isClientSide()) {
                    for (int j = 0; j < 128; j++) {
                        double d = random.nextDouble();
                        float xa = (random.nextFloat() - 0.5F) * 0.2F;
                        float ya = (random.nextFloat() - 0.5F) * 0.2F;
                        float za = (random.nextFloat() - 0.5F) * 0.2F;
                        double x = Mth.lerp(d, testPos.getX(), pos.getX()) + (random.nextDouble() - 0.5) + 0.5;
                        double y = Mth.lerp(d, testPos.getY(), pos.getY()) + random.nextDouble() - 0.5;
                        double z = Mth.lerp(d, testPos.getZ(), pos.getZ()) + (random.nextDouble() - 0.5) + 0.5;
                        level.addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
                    }
                } else {
                    level.setBlock(testPos, state, 2);
                    if (record != null && level.getBlockEntity(testPos) instanceof RecordedDragonEggBlockEntity target) target.setRecord(record);
                    level.removeBlock(pos, false);
                }
                return;
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RecordedDragonEggBlockEntity egg && egg.record() != null) egg.sync();
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinY()) {
            CompoundTag blockData = null;
            if (level.getBlockEntity(pos) instanceof RecordedDragonEggBlockEntity egg) blockData = egg.saveWithoutMetadata(level.registryAccess());
            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
            entity.blockData = blockData;
            falling(entity);
        }
    }

    @Override
    protected int getDelayAfterPlace() {
        return 5;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return -16777216;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(ModContent.RECORDED_DRAGON_EGG_ITEM.get());
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof RecordedDragonEggBlockEntity egg && egg.record() != null) stack.applyComponents(egg.collectComponents());
        return List.of(stack);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(ModContent.RECORDED_DRAGON_EGG_ITEM.get());
        if (includeData && level.getBlockEntity(pos) instanceof RecordedDragonEggBlockEntity egg && egg.record() != null) stack.applyComponents(egg.collectComponents());
        return stack;
    }
}
