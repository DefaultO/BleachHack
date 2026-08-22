/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.operation;

import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.color.QuadColor;

public class RemoveOperation extends Operation {

	protected RemoveOperation(BlockPos pos) {
		this.pos = pos;
	}

	public static OperationBlueprint blueprint(int localX, int localY, int localZ) {
		return (origin, dir) -> new RemoveOperation(origin.offset(rotate(localX, localY, localZ, dir)));
	}

	@Override
	public boolean canExecute() {
		if (mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) < 4.5) {
			for (Direction d: Direction.values()) {
				if (!mc.level.getBlockState(pos.relative(d)).isFaceSturdy(mc.level, pos.relative(d), d.getOpposite())) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean execute() {
		for (Direction d: Direction.values()) {
			if (!mc.level.getBlockState(pos.relative(d)).isFaceSturdy(mc.level, pos.relative(d), d.getOpposite())) {
				mc.gameMode.continueDestroyBlock(pos, d);
				mc.player.swing(InteractionHand.MAIN_HAND);

				return mc.level.getBlockState(pos).isAir();
			}
		}

		return false;
	}

	@Override
	public boolean verify() {
		return mc.level.getBlockState(pos).isAir();
	}

	@Override
	public void render() {
		Renderer.drawBoxBoth(pos, QuadColor.single(1f, 0f, 0f, 0.3f), 2.5f);
	}

}
