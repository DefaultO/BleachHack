/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui.clickgui.window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import org.bleachhack.gui.window.Window;

public abstract class ClickGuiWindow extends Window {

	protected Minecraft mc = Minecraft.getInstance();

	public int mouseX;
	public int mouseY;

	public boolean hiding;

	public int keyDown = -1;
	public boolean lmDown = false;
	public boolean rmDown = false;
	public boolean lmHeld = false;
	public int mwScroll = 0;

	public ClickGuiWindow(int x1, int y1, int x2, int y2, String title, ItemStack icon) {
		super(x1, y1, x2, y2, title, icon);
	}

	public boolean shouldClose(int mouseX, int mouseY) {
		return false;
	}

	protected void drawBackground(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, Font textRend) {
		/* background */
		drawContext.fill(x1, y1 + 1, x1 + 1, y2 - 1, 0xff6060b0);
		horizontalGradient(drawContext, x1 + 1, y1, x2 - 1, y1 + 1, 0xff6060b0, 0xff8070b0);
		drawContext.fill(x2 - 1, y1 + 1, x2, y2 - 1, 0xff8070b0);
		horizontalGradient(drawContext, x1 + 1, y2 - 1, x2 - 1, y2, 0xff6060b0, 0xff8070b0);

		drawContext.fill(x1 + 1, y1 + 12, x2 - 1, y2 - 1, 0x90606090);

		/* title bar */
		horizontalGradient(drawContext, x1 + 1, y1 + 1, x2 - 1, y1 + 12, 0xff6060b0, 0xff8070b0);

		/* +/- text */
		// 26.2 text() skips zero-alpha colors, so alpha is written out explicitly
		drawContext.text(textRend, hiding ? "+" : "_", x2 - 10, y1 + (hiding ? 4 : 2), 0xff000000, false);
		drawContext.text(textRend, hiding ? "+" : "_", x2 - 11, y1 + (hiding ? 3 : 1), 0xffffffff, false);
	}

	public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
		super.render(drawContext, mouseX, mouseY);

		if (rmDown && mouseOver(x1, y1, x1 + (x2 - x1), y1 + 13)) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			hiding = !hiding;
		}
	}
	
	public Tooltip getTooltip() {
		return null;
	}

	public boolean mouseOver(int minX, int minY, int maxX, int maxY) {
		return mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY < maxY;
	}

	public void updateKeys(int mouseX, int mouseY, int keyDown, boolean lmDown, boolean rmDown, boolean lmHeld, int mwScroll) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.keyDown = keyDown;
		this.lmDown = lmDown;
		this.rmDown = rmDown;
		this.lmHeld = lmHeld;
		this.mwScroll = mwScroll;
	}
	
	public static class Tooltip {
		
		public final int x;
		public final int y;
		public final String text;
		
		public Tooltip(int x, int y, String text) {
			this.x = x;
			this.y = y;
			this.text = text;
		}
	}
}
