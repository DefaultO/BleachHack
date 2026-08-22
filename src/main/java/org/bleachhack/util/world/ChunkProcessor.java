package org.bleachhack.util.world;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.eventbus.BleachSubscribe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class ChunkProcessor {

	private ExecutorService executor;

	private int threads;
	private BiConsumer<ChunkPos, LevelChunk> loadChunkConsumer;
	private BiConsumer<ChunkPos, LevelChunk> unloadChunkConsumer;
	private BiConsumer<BlockPos, BlockState> updateBlockConsumer;

	public ChunkProcessor(int threads,
			BiConsumer<ChunkPos, LevelChunk> loadChunkConsumer,
			BiConsumer<ChunkPos, LevelChunk> unloadChunkConsumer,
			BiConsumer<BlockPos, BlockState> updateBlockConsumer) {
		this.threads = threads;
		this.loadChunkConsumer = loadChunkConsumer;
		this.unloadChunkConsumer = unloadChunkConsumer;
		this.updateBlockConsumer = updateBlockConsumer;
	}

	public void start() {
		executor = Executors.newFixedThreadPool(threads);
		BleachHack.eventBus.subscribe(this);
	}

	public void stop() {
		BleachHack.eventBus.unsubscribe(this);
		executor.shutdownNow();
		executor = null;
	}

	public void restartExecutor() {
		executor.shutdownNow();
		executor = Executors.newFixedThreadPool(threads);
	}

	public void submitAllLoadedChunks() {
		if (loadChunkConsumer != null) {
			for (LevelChunk chunk: WorldUtils.getLoadedChunks()) {
				executor.execute(() -> loadChunkConsumer.accept(chunk.getPos(), chunk));
			}
		}
	}

	@BleachSubscribe
	public void onReadPacket(EventPacket.Read event) {
		if (Minecraft.getInstance().level == null)
			return;

		if (updateBlockConsumer != null && event.getPacket() instanceof ClientboundBlockUpdatePacket) {
			ClientboundBlockUpdatePacket packet = (ClientboundBlockUpdatePacket) event.getPacket();

			executor.execute(() -> updateBlockConsumer.accept(packet.getPos(), packet.getBlockState()));
		} else if (updateBlockConsumer != null && event.getPacket() instanceof ClientboundExplodePacket) {
			// TODO(26.2): ClientboundExplodePacket no longer carries the list of destroyed blocks
			// (getAffectedBlocks() was removed in 1.21.2+). Explosion block changes now arrive as
			// regular block-update / section-blocks-update packets and are handled by the branches
			// above, so there is no per-explosion block list to feed to updateBlockConsumer anymore.
		} else if (updateBlockConsumer != null && event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket) {
			ClientboundSectionBlocksUpdatePacket packet = (ClientboundSectionBlocksUpdatePacket) event.getPacket();

			packet.runUpdates((pos, state) -> {
				BlockPos impos/*ter*/ = pos.immutable();
				executor.execute(() -> updateBlockConsumer.accept(impos, state));
			});
		} else if (loadChunkConsumer != null && event.getPacket() instanceof ClientboundLevelChunkWithLightPacket) {
			ClientboundLevelChunkWithLightPacket packet = (ClientboundLevelChunkWithLightPacket) event.getPacket();

			ChunkPos cp = new ChunkPos(packet.getX(), packet.getZ());
			LevelChunk chunk = new LevelChunk(Minecraft.getInstance().level, cp);
			ClientboundLevelChunkPacketData data = packet.getChunkData();
			chunk.replaceWithPacketData(data.getReadBuffer(), data.getHeightmaps(), data.getBlockEntitiesTagsConsumer(packet.getX(), packet.getZ()));

			executor.execute(() -> loadChunkConsumer.accept(cp, chunk));
		} else if (unloadChunkConsumer != null && event.getPacket() instanceof ClientboundForgetLevelChunkPacket) {
			ClientboundForgetLevelChunkPacket packet = (ClientboundForgetLevelChunkPacket) event.getPacket();

			ChunkPos cp = new ChunkPos(packet.pos().x(), packet.pos().z());
			LevelChunk chunk = Minecraft.getInstance().level.getChunk(cp.x(), cp.z());

			executor.execute(() -> unloadChunkConsumer.accept(cp, chunk));
		}
	}
}
