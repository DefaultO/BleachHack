/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventBlockShape;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public class Jesus extends Module {

	public Jesus() {
		super("Jesus", KEY_UNBOUND, ModuleCategory.PLAYER, "Allows you to walk on water.",
				new SettingMode("Mode", "Vibrate", "Solid").withDesc("The jesus mode."));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		Entity e = mc.player.getRootVehicle();

		if (e.isShiftKeyDown() || e.fallDistance > 3f)
			return;

		if (isSubmerged(e.position().add(0, 0.3, 0))) {
			e.setDeltaMovement(e.getDeltaMovement().x, 0.08, e.getDeltaMovement().z);
		} else if (isSubmerged(e.position().add(0, 0.1, 0))) {
			e.setDeltaMovement(e.getDeltaMovement().x, 0.05, e.getDeltaMovement().z);
		} else if (isSubmerged(e.position().add(0, 0.05, 0))) {
			e.setDeltaMovement(e.getDeltaMovement().x, 0.01, e.getDeltaMovement().z);
		} else if (isSubmerged(e.position())) {
			e.setDeltaMovement(e.getDeltaMovement().x, -0.005, e.getDeltaMovement().z);
			e.setOnGround(true);
		}
	}

	@BleachSubscribe
	public void onBlockShape(EventBlockShape event) {
		if (getSetting(0).asMode().getMode() == 1
				&& !mc.level.getFluidState(event.getPos()).isEmpty()
				&& !mc.player.isShiftKeyDown()
				&& !mc.player.isInWater()
				&& mc.player.getY() >= event.getPos().getY() + 0.9) {
			event.setShape(Shapes.box(0, 0, 0, 1, 0.9, 1));
		}
	}
	
	private boolean isSubmerged(Vec3 pos) {
		BlockPos bp = BlockPos.containing(pos);
		FluidState state = mc.level.getFluidState(bp);

		return !state.isEmpty() && pos.y - bp.getY() <= state.getOwnHeight();
	}
}
