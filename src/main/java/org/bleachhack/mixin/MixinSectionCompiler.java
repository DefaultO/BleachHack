/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderBlock;
import org.bleachhack.event.events.EventRenderFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces the old RebuildTask render-loop copy: 26.2's SectionCompiler calls
 * tesselateBlock/tesselate per block, so Xray/NoRender can cancel per-block
 * tesselation with two redirects instead of a duplicated loop.
 */
@Mixin(SectionCompiler.class)
public class MixinSectionCompiler {

	@Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
	private void compile_tesselateBlock(ModelBlockRenderer renderer, BlockQuadOutput output, float x, float y, float z,
			BlockAndTintGetter level, BlockPos pos, BlockState blockState, BlockStateModel model, long seed) {
		EventRenderBlock.Tesselate event = new EventRenderBlock.Tesselate(blockState, pos);
		BleachHack.eventBus.post(event);

		if (!event.isCancelled()) {
			renderer.tesselateBlock(output, x, y, z, level, pos, blockState, model, seed);
		}
	}

	@Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/FluidRenderer;tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V"))
	private void compile_tesselateFluid(FluidRenderer renderer, BlockAndTintGetter level, BlockPos pos,
			FluidRenderer.Output output, BlockState blockState, FluidState fluidState) {
		EventRenderFluid event = new EventRenderFluid(fluidState, pos);
		BleachHack.eventBus.post(event);

		if (!event.isCancelled()) {
			renderer.tesselate(level, pos, output, blockState, fluidState);
		}
	}
}
