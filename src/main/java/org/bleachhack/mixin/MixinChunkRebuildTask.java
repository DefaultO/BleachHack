/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import java.util.Iterator;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderBlock;
import org.bleachhack.event.events.EventRenderFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.render.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;

/**
 * Blocks are still tesselated even if they're transparent because Minecraft's
 * rendering engine is poop.
 */
@Mixin(SectionRenderDispatcher.BuiltChunk.RebuildTask.class)
public class MixinChunkRebuildTask {

	@Unique private static boolean OPTIFABRIC_INSTALLED = FabricLoader.getInstance().isModLoaded("optifabric");

	@Shadow private /* outer */ SectionRenderDispatcher.BuiltChunk field_20839;
	@Shadow private RenderSectionRegion region;

	@Shadow private <E extends BlockEntity> void addBlockEntity(SectionRenderDispatcher.BuiltChunk.RebuildTask.RenderData renderData, E blockEntity) {}
	@Shadow private SectionRenderDispatcher.BuiltChunk.RebuildTask.RenderData render(float cameraX, float cameraY, float cameraZ, BlockBufferBuilderStorage buffers) { return null; }

	// i have gone past the point of insanity
	@Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/SectionRenderDispatcher$BuiltChunk$RebuildTask;render(FFFLnet/minecraft/client/render/chunk/BlockBufferBuilderStorage;)Lnet/minecraft/client/render/chunk/SectionRenderDispatcher$BuiltChunk$RebuildTask$RenderData;"))
	private SectionRenderDispatcher.BuiltChunk.RebuildTask.RenderData run_render(@Coerce Object thisObject, float cameraX, float cameraY, float cameraZ, BlockBufferBuilderStorage buffers) {
		return OPTIFABRIC_INSTALLED 
				? render(cameraX, cameraY, cameraZ, buffers) : newRender(cameraX, cameraY, cameraZ, buffers);
	}

	private SectionRenderDispatcher.BuiltChunk.RebuildTask.RenderData newRender(float cameraX, float cameraY, float cameraZ, BlockBufferBuilderStorage buffers) {
		SectionRenderDispatcher.BuiltChunk.RebuildTask.RenderData renderData = new SectionRenderDispatcher.BuiltChunk.RebuildTask.RenderData();
		BlockPos blockPos = field_20839.getOrigin().toImmutable();
		BlockPos blockPos2 = blockPos.add(15, 15, 15);
		VisGraph chunkOcclusionDataBuilder = new VisGraph();
		RenderSectionRegion chunkRendererRegion = this.region;
		this.region = null;
		PoseStack matrixStack = new PoseStack();
		if (chunkRendererRegion != null) {
			ModelBlockRenderer.enableBrightnessCache();
			Set<RenderType> set = new ReferenceArraySet(RenderType.getBlockLayers().size());
			net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create();
			BlockRenderDispatcher blockRenderManager = Minecraft.getInstance().getBlockRenderManager();
			Iterator var15 = BlockPos.iterate(blockPos, blockPos2).iterator();

			while(var15.hasNext()) {
				BlockPos blockPos3 = (BlockPos)var15.next();
				BlockState blockState = chunkRendererRegion.getBlockState(blockPos3);
				if (blockState.isOpaqueFullCube(chunkRendererRegion, blockPos3)) {
					chunkOcclusionDataBuilder.markClosed(blockPos3);
				}

				if (blockState.hasBlockEntity()) {
					BlockEntity blockEntity = chunkRendererRegion.getBlockEntity(blockPos3);
					if (blockEntity != null) {
						this.addBlockEntity(renderData, blockEntity);
					}
				}

				BlockState blockState2 = chunkRendererRegion.getBlockState(blockPos3);
				FluidState fluidState = blockState2.getFluidState();
				RenderType renderLayer;
				BufferBuilder bufferBuilder;
				if (!fluidState.isEmpty()) {
					renderLayer = RenderTypes.getFluidLayer(fluidState);
					bufferBuilder = buffers.get(renderLayer);

					EventRenderFluid event = new EventRenderFluid(fluidState, blockPos3, bufferBuilder);
					BleachHack.eventBus.post(event);

					if (event.isCancelled())
						continue;

					if (set.add(renderLayer)) {
						bufferBuilder.begin(VertexFormat.DrawMode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
					}

					blockRenderManager.renderFluid(blockPos3, chunkRendererRegion, bufferBuilder, blockState2, fluidState);
				}

				if (blockState.getRenderType() != RenderShape.INVISIBLE) {
					renderLayer = RenderTypes.getBlockLayer(blockState);
					bufferBuilder = buffers.get(renderLayer);
					if (set.add(renderLayer)) {
						bufferBuilder.begin(VertexFormat.DrawMode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
					}

					EventRenderBlock.Tesselate event = new EventRenderBlock.Tesselate(blockState, blockPos3, matrixStack, bufferBuilder);
					BleachHack.eventBus.post(event);

					if (event.isCancelled())
						continue;

					matrixStack.push();
					matrixStack.translate((double)(blockPos3.getX() & 15), (double)(blockPos3.getY() & 15), (double)(blockPos3.getZ() & 15));
					blockRenderManager.renderBlock(blockState, blockPos3, chunkRendererRegion, matrixStack, bufferBuilder, true, random);
					matrixStack.pop();
				}
			}

			if (set.contains(RenderType.getTranslucent())) {
				BufferBuilder bufferBuilder2 = buffers.get(RenderType.getTranslucent());
				if (!bufferBuilder2.isBatchEmpty()) {
					//bufferBuilder2.sortFrom(cameraX - (float)blockPos.getX(), cameraY - (float)blockPos.getY(), cameraZ - (float)blockPos.getZ());
					renderData.translucencySortingData = bufferBuilder2.getSortingData();
				}
			}

			var15 = set.iterator();

			while(var15.hasNext()) {
				RenderType renderLayer2 = (RenderType)var15.next();
				BufferBuilder.BuiltBuffer builtBuffer = buffers.get(renderLayer2).endNullable();
				if (builtBuffer != null) {
					renderData.buffers.put(renderLayer2, builtBuffer);
				}
			}

			ModelBlockRenderer.disableBrightnessCache();
		}

		renderData.chunkOcclusionData = chunkOcclusionDataBuilder.build();
		return renderData;
	}
}
