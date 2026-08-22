/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui.clickgui;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.bleachhack.BleachHack;
import org.bleachhack.command.Command;
import org.bleachhack.gui.clickgui.window.ClickGuiWindow;
import org.bleachhack.gui.clickgui.window.ModuleWindow;
import org.bleachhack.gui.window.Window;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.ClickGui;
import org.bleachhack.util.io.BleachFileHelper;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class ModuleClickGuiScreen extends ClickGuiScreen {
	
	public static ModuleClickGuiScreen INSTANCE = new ModuleClickGuiScreen();

	private EditBox searchField;

	public ModuleClickGuiScreen() {
		super(Component.literal("ClickGui"));
	}

	public void init() {
		super.init();

		searchField = new EditBox(font, 2, 14, 100, 12, Component.empty() /* @LasnikProgram is author lol */);
		searchField.visible = false;
		searchField.setMaxLength(20);
		searchField.setSuggestion("Search here");
		addRenderableWidget(searchField);
	}

	public void initWindows() {
		int len = ModuleManager.getModule(ClickGui.class).getSetting(0).asSlider().getValueInt();
		
		int y = 50;
		for (ModuleCategory c: ModuleCategory.values()) {
			addWindow(new ModuleWindow(ModuleManager.getModulesInCat(c), 30, y, len, StringUtils.capitalize(c.name().toLowerCase()), c.getItem()));
			y += 16;
		}

		for (Window w: getWindows()) {
			if (w instanceof ClickGuiWindow) {
				((ClickGuiWindow) w).hiding = true;
			}
		}
	}

	public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
		BleachFileHelper.SCHEDULE_SAVE_CLICKGUI.set(true);
		ClickGui clickGui = ModuleManager.getModule(ClickGui.class);

		searchField.visible = clickGui.getSetting(1).asToggle().getState();

		if (clickGui.getSetting(1).asToggle().getState()) {
			searchField.setSuggestion(searchField.getValue().isEmpty() ? "Search here" : "");

			Set<Module> seachMods = new HashSet<>();
			if (!searchField.getValue().isEmpty()) {
				for (Module m : ModuleManager.getModules()) {
					if (m.getName().toLowerCase(Locale.ENGLISH).contains(searchField.getValue().toLowerCase(Locale.ENGLISH).replace(" ", ""))) {
						seachMods.add(m);
					}
				}
			}

			for (Window w : getWindows()) {
				if (w instanceof ModuleWindow) {
					((ModuleWindow) w).setSearchedModule(seachMods);
				}
			}
		}

		int len = clickGui.getSetting(0).asSlider().getValueInt();
		for (Window w : getWindows()) {
			if (w instanceof ModuleWindow) {
				((ModuleWindow) w).setLen(len);
			}
		}

		super.extractRenderState(drawContext, mouseX, mouseY, delta);

		// 26.2 text() skips zero-alpha colors, so alpha is written out explicitly
		drawContext.text(font, "BleachHack-" + BleachHack.VERSION + "-" + SharedConstants.getCurrentVersion().name(), 3, 3, 0xff305090);
		drawContext.text(font, "BleachHack-" + BleachHack.VERSION + "-" + SharedConstants.getCurrentVersion().name(), 2, 2, 0xff6090d0);

		if (clickGui.getSetting(2).asToggle().getState()) {
			drawContext.text(font, "BleachHack-" + BleachHack.VERSION + "Current prefix is: \"" + Command.getPrefix() + "\" (" + Command.getPrefix() + "help)", 2, height - 20, 0xff99ff99);
			drawContext.text(font, "BleachHack-" + BleachHack.VERSION + "Use " + Command.getPrefix() + "clickgui to reset the clickgui", 2, height - 10, 0xff9999ff);
		}
	}
}
