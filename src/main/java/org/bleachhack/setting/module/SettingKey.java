/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.setting.module;

import net.minecraft.client.gui.GuiGraphics; // TODO(26.2): GuiGraphics removed; GUI draw pipeline is now GuiGraphicsExtractor + GuiRenderState (extractRenderState). Needs window-framework migration.
import org.bleachhack.gui.clickgui.window.ModuleWindow;
import org.bleachhack.module.Module;
import org.bleachhack.setting.SettingDataHandlers;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.sounds.SoundEvents;

public class SettingKey extends ModuleSetting<Integer> {

	public SettingKey(int key) {
		super("Bind", key, SettingDataHandlers.INTEGER);
	}

	@Override
	// TODO(26.2): render body uses removed immediate-mode GuiGraphics API (fill/drawTextWithShadow). Migrate to GuiGraphicsExtractor/GuiRenderState with the window framework.
	public void render(ModuleWindow window, GuiGraphics drawContext, int x, int y, int len) {
		if (window.mouseOver(x, y, x + len, y + 12)) {
			drawContext.fill(x + 1, y, x + len, y + 12, 0x70303070);
		}
		
		if (window.keyDown >= 0 && window.keyDown != GLFW.GLFW_KEY_ESCAPE && window.mouseOver(x, y, x + len, y + 12)) {
			setValue(window.keyDown == GLFW.GLFW_KEY_DELETE ? Module.KEY_UNBOUND : window.keyDown);
			Minecraft.getInstance().getSoundManager().play(
					SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
		}

		int key = getValue();
		String name = key < 0 ? "NONE" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
		if (name == null)
			name = "KEY" + key;
		else if (name.isEmpty())
			name = "NONE";

		drawContext.drawTextWithShadow(Minecraft.getInstance().font, "Bind: " + name + (window.mouseOver(x, y, x + len, y + 12) ? "..." : ""), x + 3, y + 2, 0xcfe0cf);
	}

	public SettingKey withDesc(String desc) {
		setTooltip(desc);
		return this;
	}

	@Override
	public int getHeight(int len) {
		return 12;
	}
}
