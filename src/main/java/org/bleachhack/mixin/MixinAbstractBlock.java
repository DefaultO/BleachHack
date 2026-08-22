/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class MixinAbstractBlock {

	@Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
	private void getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> callback) {
		EventRenderBlock.Light event = new EventRenderBlock.Light(state);
		BleachHack.eventBus.post(event);

		if (event.getLight() != null)
			callback.setReturnValue(event.getLight());
	}
}
