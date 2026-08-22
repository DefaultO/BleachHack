/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.HashSet;
import java.util.Set;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.util.world.WorldUtils;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class Ghosthand extends Module {

	public Ghosthand() {
		super("Ghosthand", KEY_UNBOUND, ModuleCategory.PLAYER, "Opens containers through walls.");
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (!mc.options.keyUse.isDown() || mc.player.isShiftKeyDown())
			return;

		float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		// Return if we are looking at any block entities
		BlockPos lookingPos = BlockPos.containing(mc.player.pick(4.25, tickDelta, false).getLocation());
		for (BlockEntity b : WorldUtils.getBlockEntities()) {
			if (lookingPos.equals(b.getBlockPos())) {
				return;
			}
		}

		Set<BlockPos> posList = new HashSet<>();

		Vec3 nextPos = new Vec3(0, 0, 0.1)
				.xRot(-(float) Math.toRadians(mc.player.getXRot()))
				.yRot(-(float) Math.toRadians(mc.player.getYRot()));

		for (int i = 1; i < 50; i++) {
			BlockPos curPos = BlockPos.containing(mc.player.getEyePosition(tickDelta).add(nextPos.scale(i)));
			if (!posList.contains(curPos)) {
				posList.add(curPos);

				for (BlockEntity b : WorldUtils.getBlockEntities()) {
					if (b.getBlockPos().equals(curPos)) {
						mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
								new BlockHitResult(Vec3.upFromBottomCenterOf(curPos, 1), Direction.UP, curPos, true));
						return;
					}
				}
			}
		}
	}

}
