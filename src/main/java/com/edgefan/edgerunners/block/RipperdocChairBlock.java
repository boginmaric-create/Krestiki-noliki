package com.edgefan.edgerunners.block;

import com.edgefan.edgerunners.menu.RipperdocMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/** Ripperdoc's operating chair: right-click to open the cyberware installation screen. */
public class RipperdocChairBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public RipperdocChairBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RipperdocChairBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (type != ModBlocks.CHAIR_BE.get()) return null;
        return (l, p, s, be) -> {
            if (be instanceof RipperdocChairBlockEntity chair) {
                if (l.isClientSide) chair.clientTick(); else chair.serverTick();
            }
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RipperdocChairBlockEntity be) be.abort();
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RipperdocChairBlockEntity be && be.busy()) {
            player.displayClientMessage(Component.translatable("msg.edgerunners.chair_busy").withStyle(net.minecraft.ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                    (id, inv, p) -> new RipperdocMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.edgerunners.ripperdoc")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
