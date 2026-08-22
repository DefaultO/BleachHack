/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class MixinBlock {

	// 26.2: Block.shouldDrawSide -> static Block.shouldRenderFace(BlockState, BlockState neighborState, Direction); 5 args collapsed to 3.
	@Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
	private static void shouldRenderFace(BlockState state, BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> callback) {
		EventRenderBlock.ShouldDrawSide event = new EventRenderBlock.ShouldDrawSide(state);
		BleachHack.eventBus.post(event);

		if (event.shouldDrawSide() != null)
			callback.setReturnValue(event.shouldDrawSide());
	}
}
