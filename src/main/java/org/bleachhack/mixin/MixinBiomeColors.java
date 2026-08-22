/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventBiomeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class MixinBiomeColors {

	@Inject(method = "getAverageColor(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/ColorResolver;)I", at = @At("RETURN"), cancellable = true)
	private static void getAverageColor(BlockAndTintGetter world, BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> callback) {
		if (Minecraft.getInstance().level != null) {
			EventBiomeColor event =
					resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER ? new EventBiomeColor.Foilage(world, pos, callback.getReturnValueI()) :
						resolver == BiomeColors.GRASS_COLOR_RESOLVER ? new EventBiomeColor.Grass(world, pos, callback.getReturnValueI()) :
							resolver == BiomeColors.WATER_COLOR_RESOLVER ? new EventBiomeColor.Water(world, pos, callback.getReturnValueI()) :
								null;

			if (event != null) {
				BleachHack.eventBus.post(event);
				callback.setReturnValue(event.getColor());
			}
		}
	}
}
