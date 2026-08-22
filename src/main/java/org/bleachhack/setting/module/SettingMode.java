/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.setting.module;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.bleachhack.gui.clickgui.window.ModuleWindow;
import org.bleachhack.setting.SettingDataHandlers;

import com.google.gson.JsonElement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public class SettingMode extends ModuleSetting<Integer> {

	public String[] modes;

	public SettingMode(String text, String... modes) {
		super(text, 0, SettingDataHandlers.INTEGER);
		this.modes = modes;
	}
	
	public int getMode() {
		return getValue().intValue();
	}

	public void render(ModuleWindow window, GuiGraphicsExtractor drawContext, int x, int y, int len) {
		if (window.mouseOver(x, y, x + len, y + 12)) {
			drawContext.fill(x + 1, y, x + len, y + 12, 0x70303070);
		}

		drawContext.text(Minecraft.getInstance().font, getName() + ": " + modes[getValue()], x + 3, y + 2, 0xffcfe0cf);

		if (window.mouseOver(x, y, x + len, y + 12) && window.lmDown) {
			setValue(getValue() >= modes.length - 1 ? 0 : getValue() + 1);
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
		}
	}

	public SettingMode withDesc(String desc) {
		setTooltip(desc);
		return this;
	}

	public int getHeight(int len) {
		return 12;
	}

	@Override
	public void read(JsonElement json) {
		Integer val = getHandler().readOrNull(json);
		if (val != null)
			setValue(Mth.clamp(val, 0, modes.length - 1));
	}
}
