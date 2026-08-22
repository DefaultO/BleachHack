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
import org.bleachhack.module.ModuleManager;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;

import net.minecraft.util.Mth;

public class Zoom extends Module {

	private static final int SCALE = 0;
	private static final int SMOOTH = 1;
	private static final int SENSITIVITY = 2;
	private static final int HIDE_HAND = 3;

	private double prevSens;

	/** Eased towards the target zoom so it doesn't snap. */
	private double currentScale = 1;

	public Zoom() {
		super("Zoom", KEY_UNBOUND, ModuleCategory.RENDER, "ok zoomer.",
				new SettingSlider("Scale", 1, 50, 3, 1).withDesc("How much to zoom."),
				new SettingToggle("Smooth", true).withDesc("Ease into the zoom instead of snapping to it.").withChildren(
						new SettingSlider("Speed", 0.05, 1, 0.35, 2).withDesc("How quickly the zoom eases in and out.")),
				new SettingToggle("Sensitivity", true).withDesc("Lower mouse sensitivity while zoomed so aiming stays precise."),
				new SettingToggle("HideHand", false).withDesc("Hide the held item while zoomed."));
	}

	public boolean shouldHideHand() {
		return isEnabled() && getSetting(HIDE_HAND).asToggle().getState();
	}

	@Override
	public void onEnable(boolean inWorld) {
		super.onEnable(inWorld);

		prevSens = mc.options.sensitivity().get();
		currentScale = 1;

		if (!getSetting(SMOOTH).asToggle().getState()) {
			currentScale = getSetting(SCALE).asSlider().getValue();
		}

		apply();
	}

	@Override
	public void onDisable(boolean inWorld) {
		mc.options.sensitivity().set(prevSens);

		super.onDisable(inWorld);
	}

	/** Divisor applied to the camera's field of view, 1 when not zooming. */
	public static float fovDivisor() {
		Zoom zoom = ModuleManager.getModule(Zoom.class);
		return zoom != null && zoom.isEnabled() ? (float) Math.max(zoom.currentScale, 1) : 1f;
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		double target = getSetting(SCALE).asSlider().getValue();

		if (getSetting(SMOOTH).asToggle().getState()) {
			double speed = getSetting(SMOOTH).asToggle().getChild(0).asSlider().getValue();
			currentScale = Mth.lerp(speed, currentScale, target);
		} else {
			currentScale = target;
		}

		apply();
	}

	/**
	 * Only sensitivity is touched here - the zoom itself divides the camera's field of
	 * view in MixinCamera. Writing options.fov() instead would clamp to the vanilla
	 * 30-110 slider range, capping zoom at about 2.3x.
	 */
	private void apply() {
		double scale = Math.max(currentScale, 1);

		// Scale the sensitivity with the zoom so the same mouse movement covers the same
		// arc on screen; without this a high zoom is unusable.
		mc.options.sensitivity().set(getSetting(SENSITIVITY).asToggle().getState() ? prevSens / scale : prevSens);
	}
}
