/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingToggle;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;

public class RotationSnap extends Module {

	private boolean lDown = false;
	private boolean uDown = false;
	private boolean rDown = false;
	private boolean dDown = false;

	public RotationSnap() {
		super("RotationSnap", KEY_UNBOUND, ModuleCategory.PLAYER, "Snaps your rotations to angles.",
				new SettingToggle("Yaw", true).withDesc("Fixes your yaw.").withChildren(
						new SettingMode("Interval", "45", "30", "15", "90").withDesc("What angles to snap to.")),
				new SettingToggle("Pitch", false).withDesc("Fixes your pitch.").withChildren(
						new SettingMode("Interval", "45", "30", "15", "90").withDesc("What angles to snap to.")),
				new SettingToggle("Arrow Move", false).withDesc("Allows you to move between angles by using your arrow keys."));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		/* yes looks like a good way to do it to me */
		if (getSetting(2).asToggle().getState() && mc.gui.screen() == null) {
			int yMode = getSetting(0).asToggle().getChild(0).asMode().getMode();
			int pMode = getSetting(1).asToggle().getChild(0).asMode().getMode();
			
			int yAngle = yMode == 0 ? 45 : yMode == 1 ? 30 : yMode == 2 ? 15 : 90;
			int pAngle = pMode == 0 ? 45 : pMode == 1 ? 30 : pMode == 2 ? 15 : 90;

			if (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT) && !lDown) {
				mc.player.setYRot(mc.player.getYRot() - yAngle);
				lDown = true;
			} else if (!InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT)) {
				lDown = false;
			}

			if (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT) && !rDown) {
				mc.player.setYRot(mc.player.getYRot() + yAngle);
				rDown = true;
			} else if (!InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT)) {
				rDown = false;
			}

			if (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_UP) && !uDown) {
				mc.player.setXRot(Mth.clamp(mc.player.getXRot() - pAngle, -90, 90));
				uDown = true;
			} else if (!InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_UP)) {
				uDown = false;
			}

			if (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_DOWN) && !dDown) {
				mc.player.setXRot(Mth.clamp(mc.player.getXRot() + pAngle, -90, 90));
				dDown = true;
			} else if (!InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_DOWN)) {
				dDown = false;
			}
		}

		snap();
	}

	public void snap() {
		// quic maff
		if (getSetting(0).asToggle().getState()) {
			int mode = getSetting(0).asToggle().getChild(0).asMode().getMode();
			int interval = mode == 0 ? 45 : mode == 1 ? 30 : mode == 2 ? 15 : 90;
			int rot = (int) mc.player.getYRot() + (Math.floorMod((int) mc.player.getYRot(), interval) < interval / 2 ?
					-Math.floorMod((int) mc.player.getYRot(), interval) : interval - Math.floorMod((int) mc.player.getYRot(), interval));

			mc.player.setYRot(rot);
		}

		if (getSetting(1).asToggle().getState()) {
			int mode = getSetting(1).asToggle().getChild(0).asMode().getMode();
			int interval = mode == 0 ? 45 : mode == 1 ? 30 : mode == 2 ? 15 : 90;
			int rot = Mth.clamp(((int) mc.player.getXRot() + (Math.floorMod((int) mc.player.getXRot(), interval) < interval / 2 ?
					-Math.floorMod((int) mc.player.getXRot(), interval) : interval - Math.floorMod((int) mc.player.getXRot(), interval))), -90, 90);

			mc.player.setXRot(rot);
		}
	}
}
