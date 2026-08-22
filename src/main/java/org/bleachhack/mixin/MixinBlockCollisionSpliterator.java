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
import net.minecraft.world.level.CollisionGetter;

@Mixin(BlockCollisions.class)
public class MixinBlockCollisionSpliterator {

	// 26.2: collision shape now fetched via context.getCollisionShape(state, getter, pos).
	@Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
	private VoxelShape computeNext_getCollisionShape(CollisionContext context, BlockState blockState, CollisionGetter getter, BlockPos pos) {
		VoxelShape shape = context.getCollisionShape(blockState, getter, pos);
		EventBlockShape event = new EventBlockShape(blockState, pos, shape);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			return Shapes.empty();
		}

		return event.getShape();
	}
}
