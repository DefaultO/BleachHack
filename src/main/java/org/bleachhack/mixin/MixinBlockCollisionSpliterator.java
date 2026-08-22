/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventBlockShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;

@Mixin(BlockCollisions.class)
public class MixinBlockCollisionSpliterator {

	@Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getCollisionShape(Lnet/minecraft/world/BlockGetter;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/CollisionContext;)Lnet/minecraft/util/shape/VoxelShape;"))
	private VoxelShape computeNext_getCollisionShape(BlockState blockState, BlockGetter world, BlockPos pos, CollisionContext context) {
		VoxelShape shape = blockState.getCollisionShape(world, pos, context);
		EventBlockShape event = new EventBlockShape((BlockState) blockState, pos, shape);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			return Shapes.empty();
		}

		return event.getShape();
	}
}
