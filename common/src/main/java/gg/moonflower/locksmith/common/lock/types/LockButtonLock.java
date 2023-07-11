package gg.moonflower.locksmith.common.lock.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.moonflower.locksmith.api.lock.position.LockPosition;
import gg.moonflower.locksmith.common.block.LockButtonBlock;
import gg.moonflower.locksmith.core.registry.LocksmithLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class LockButtonLock extends KeyLock {

    public static final Codec<LockButtonLock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.UUID.fieldOf("id").forGetter(LockButtonLock::getId),
            LockPosition.CODEC.fieldOf("pos").forGetter(LockButtonLock::getPos),
            ItemStack.CODEC.fieldOf("stack").forGetter(LockButtonLock::getStack)
    ).apply(instance, LockButtonLock::new));

    public LockButtonLock(UUID id, LockPosition pos, ItemStack stack) {
        super(LocksmithLocks.LOCK_BUTTON, id, pos, stack);
    }

    @Override
    public void onRemove(Level level, BlockPos pos, BlockPos clickPos) {
        super.onRemove(level, pos, clickPos);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LockButtonBlock)
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
    }
}
