package gg.moonflower.locksmith.common.lock;

import gg.moonflower.locksmith.api.lock.AbstractLock;
import gg.moonflower.locksmith.common.network.LocksmithMessages;
import gg.moonflower.locksmith.common.network.play.ClientboundLockSyncPacket;
import gg.moonflower.pollen.api.event.events.entity.player.server.ServerPlayerTrackingEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ServerLockManager extends SavedData implements LockManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<ChunkPos, ChunkLockData> locks = new HashMap<>();
    private final ServerLevel level;

    private ServerLockManager(ServerLevel level) {
        super("locksmithLocks");
        this.level = level;
    }

    public static ServerLockManager getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(() -> new ServerLockManager(level), "locksmithLocks");
    }

    public static void init() {
        ServerPlayerTrackingEvents.START_TRACKING_CHUNK.register((player, chunk) -> {
            if (!(player.level instanceof ServerLevel) || !(player instanceof ServerPlayer))
                return;

            Collection<AbstractLock> locks = LockManager.get(player.level).getLocks(chunk);
            if (locks.isEmpty())
                return;

            LocksmithMessages.PLAY.sendTo((ServerPlayer) player, new ClientboundLockSyncPacket(chunk, locks, true));
        });
    }

    @Override
    public Collection<AbstractLock> getLocks(ChunkPos chunkPos) {
        ChunkLockData chunk = this.locks.get(chunkPos);
        if (chunk == null)
            return Collections.emptySet();
        return chunk.getLocks();
    }

    @Override
    @Nullable
    public AbstractLock getLock(BlockPos pos) {
        ChunkLockData chunk = this.locks.get(new ChunkPos(pos));
        if (chunk == null)
            return null;

        return chunk.getLock(pos);
    }

    @Override
    public void addLock(AbstractLock data) {
        ChunkPos chunk = new ChunkPos(data.getPos());
        this.locks.compute(chunk, (chunkPos, chunkLockData) -> {
            ChunkLockData chunkData = chunkLockData == null ? new ChunkLockData() : chunkLockData;
            chunkData.addLock(data);
            return chunkData;
        });
        this.setDirty();
        LocksmithMessages.PLAY.sendToTracking(this.level, chunk, new ClientboundLockSyncPacket(chunk, Collections.singleton(data), false));
    }

    @Override
    public void removeLock(BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        ChunkLockData chunkData = this.locks.get(chunk);
        if (chunkData == null)
            return;

        chunkData.removeLock(pos);
        this.setDirty();
        LocksmithMessages.PLAY.sendToTracking(this.level, chunk, new ClientboundLockSyncPacket(chunk, pos));
    }

    @Override
    public void load(CompoundTag compoundTag) {
        ListTag tag = compoundTag.getList("Chunks", 10);
        for (int i = 0; i < tag.size(); i++) {
            CompoundTag lock = tag.getCompound(i);
            ChunkLockData data = new ChunkLockData();
            data.load(lock);

            this.locks.put(new ChunkPos(lock.getInt("X"), lock.getInt("Z")), data);
        }
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        ListTag list = new ListTag();
        for (ChunkPos pos : this.locks.keySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", pos.x);
            tag.putInt("Z", pos.z);

            list.add(this.locks.get(pos).save(tag));
        }

        compoundTag.put("Chunks", list);
        return compoundTag;
    }

    private static class ChunkLockData {
        private final Map<BlockPos, AbstractLock> locks = new HashMap<>();

        public void load(CompoundTag compoundTag) {
            ListTag tag = compoundTag.getList("Locks", 10);
            for (int i = 0; i < tag.size(); i++) {
                AbstractLock lock = AbstractLock.CODEC.parse(NbtOps.INSTANCE, tag.getCompound(i)).getOrThrow(false, LOGGER::error);
                this.locks.put(lock.getPos(), lock);
            }
        }

        public CompoundTag save(CompoundTag compoundTag) {
            ListTag list = new ListTag();
            for (AbstractLock lock : this.locks.values()) {
                list.add(AbstractLock.CODEC.encodeStart(NbtOps.INSTANCE, lock).getOrThrow(false, LOGGER::error));
            }

            compoundTag.put("Locks", list);
            return compoundTag;
        }

        public Collection<AbstractLock> getLocks() {
            return locks.values();
        }

        @Nullable
        public AbstractLock getLock(BlockPos pos) {
            return this.locks.get(pos);
        }

        public void addLock(AbstractLock lock) {
            this.locks.put(lock.getPos(), lock);
        }

        public void removeLock(BlockPos pos) {
            this.locks.remove(pos);
        }
    }
}