/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui.clickgui.window;

import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.ClickGui;
import org.bleachhack.setting.module.ModuleSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ModuleWindow extends ClickGuiWindow {

	public List<Module> modList = new ArrayList<>();
	public LinkedHashMap<Module, Boolean> mods = new LinkedHashMap<>();

	private int len;

	private Set<Module> searchedModules;

	private Tooltip tooltip = null;

	/** How far the module list is scrolled, in pixels, when it doesn't fit on screen. */
	private int scroll;

	public ModuleWindow(List<Module> mods, int x1, int y1, int len, String title, java.util.function.Supplier<ItemStack> icon) {
		super(x1, y1, x1 + len, 0, title, icon);

		this.len = len;
		modList = mods;

		for (Module m : mods)
			this.mods.put(m, false);

		y2 = getHeight();
	}

	public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
		tooltip = null;
		int x = x1 + 1;
		int y = y1 + 13;
		x2 = x + len + 1;

		// Expanded groups can be far taller than the screen, so cap the window at the
		// space below its title bar and scroll the contents inside that.
		int contentHeight = getHeight();
		int available = Math.max(24, mc.getWindow().getGuiScaledHeight() - y - 2);
		boolean scrollable = contentHeight > available;
		int visibleHeight = scrollable ? available : contentHeight;

		if (scrollable) {
			if (mouseOver(x1, y1, x2, y1 + 13 + visibleHeight) && mwScroll != 0) {
				scroll -= mwScroll * 12;
			}

			scroll = Mth.clamp(scroll, 0, contentHeight - visibleHeight);
		} else {
			scroll = 0;
		}

		y2 = hiding ? y1 + 13 : y1 + 13 + visibleHeight;

		super.render(drawContext, mouseX, mouseY);

		if (hiding) return;

		Font textRend = mc.font;

		if (scrollable) {
			drawContext.enableScissor(x1, y, x2, y + visibleHeight);
		}

		int curY = -scroll;
		for (Entry<Module, Boolean> m : mods.entrySet()) {
			if (mouseOver(x, y + curY, x + len, y + 12 + curY)) {
				drawContext.fill(x, y + curY, x + len, y + 12 + curY, 0x70303070);
			}

			// If they match: Module gets marked red
			if (searchedModules != null && searchedModules.contains(m.getKey()) && ModuleManager.getModule(ClickGui.class).getSetting(1).asToggle().getState()) {
				drawContext.fill(x, y + curY, x + len, y + 12 + curY, 0x50ff0000);
			}

			// 26.2 text() skips zero-alpha colors, so alpha is written out explicitly
			drawContext.text(textRend, textRend.plainSubstrByWidth(m.getKey().getName(), len),
					x + 2, y + 2 + curY, m.getKey().isEnabled() ? 0xff70efe0 : 0xffc0c0c0);

			// Set which module settings show on
			if (mouseOver(x, y + curY, x + len, y + 12 + curY)) {
				tooltip = new Tooltip(x + len + 2, y + curY, m.getKey().getDesc());

				if (lmDown)
					m.getKey().toggle();
				if (rmDown)
					mods.replace(m.getKey(), !m.getValue());
				if (lmDown || rmDown)
					mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}

			curY += 12;

			// draw settings
			if (m.getValue()) {
				for (ModuleSetting<?> s : m.getKey().getSettings()) {
					if (!s.isVisible()) {
						continue;
					}

					s.render(this, drawContext, x + 1, y + curY, len - 1);

					if (!s.getTooltip().isEmpty() && mouseOver(x + 2, y + curY, x + len, y + s.getHeight(len) + curY)) {
						tooltip = s.getTooltip(this, x + 1, y + curY, len - 1);
					}

					drawContext.fill(x + 1, y + curY, x + 2, y + curY + s.getHeight(len), 0xff8070b0);

					curY += s.getHeight(len);
				}
			}
		}

		if (scrollable) {
			drawContext.disableScissor();

			// slim scrollbar so it's obvious there's more below
			int trackHeight = visibleHeight - 2;
			int thumbHeight = Math.max(8, trackHeight * visibleHeight / contentHeight);
			int thumbY = y + 1 + (trackHeight - thumbHeight) * scroll / Math.max(1, contentHeight - visibleHeight);

			drawContext.fill(x2 - 2, y + 1, x2 - 1, y + trackHeight + 1, 0x40000000);
			drawContext.fill(x2 - 2, thumbY, x2 - 1, thumbY + thumbHeight, 0xff8070b0);
		}
	}

	public Tooltip getTooltip() {
		return tooltip;
	}

	public void setSearchedModule(Set<Module> mods) {
		searchedModules = mods;
	}

	public void setLen(int len) {
		this.len = len;
	}

	public int getHeight() {
		int h = 1;
		for (Entry<Module, Boolean> e : mods.entrySet()) {
			h += 12;

			if (e.getValue()) {
				for (ModuleSetting<?> s : e.getKey().getSettings()) {
					if (!s.isVisible()) {
						continue;
					}

					h += s.getHeight(len);
				}
			}
		}

		return h;
	}
}
