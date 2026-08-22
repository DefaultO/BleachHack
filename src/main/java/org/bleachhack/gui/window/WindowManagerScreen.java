/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui.window;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Triple;
import org.bleachhack.gui.window.widget.WindowButtonWidget;

import java.util.List;


public class WindowManagerScreen extends WindowScreen {

	/** [Window Screen, Name, Icon] **/
	public Triple<WindowScreen, String, Supplier<ItemStack>>[] windows;
	private int selected;

	@SafeVarargs
	public WindowManagerScreen(Triple<WindowScreen, String, Supplier<ItemStack>>... windows) {
		super(Component.empty(), false);
		this.windows = windows;
	}

	@Override
	public void init() {
		super.init();
		selectWindow(selected);

		int x = 1;
		int size = Math.min(width / windows.length - 1, 90);
		for (int i = 0; i < windows.length; i++) {
			int fi = i;
			addGlobalWidget(new WindowTabButtonWidget(x, height - 15, x + size, height - 1,
					windows[i].getMiddle(), windows[i].getRight(), () -> selectWindow(fi)));
			x += size + 1;
		}
	}

	@SuppressWarnings("unchecked")
	public void selectWindow(int s) {
		selected = s;
		for (Triple<WindowScreen, String, Supplier<ItemStack>> t: windows) {
			removeWidget(t.getLeft());
		}

		getSelectedScreen().init(width, height - 16);
		addRenderableOnly(getSelectedScreen());
		((List<GuiEventListener>) children()).add(getSelectedScreen());
	}

	public WindowScreen getSelectedScreen() {
		return windows[selected].getLeft();
	}

	public String getSelectedTitle() {
		return windows[selected].getMiddle();
	}

	public ItemStack getSelectedIcon() {
		return org.bleachhack.util.SafeItem.resolve(windows[selected].getRight());
	}

	// Children don't tick brue
	@Override
	public void tick() {
		getSelectedScreen().tick();
		super.tick();
	}

	// Children also don't take keyboard input brueh
	@Override
	public boolean keyPressed(KeyEvent event) {
		getSelectedScreen().keyPressed(event);
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		getSelectedScreen().keyReleased(event);
		return super.keyReleased(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		getSelectedScreen().charTyped(event);
		return super.charTyped(event);
	}

	private static class WindowTabButtonWidget extends WindowButtonWidget {

		private Supplier<ItemStack> item;

		public WindowTabButtonWidget(int x1, int y1, int x2, int y2, String text, Supplier<ItemStack> item, Runnable action) {
			super(x1, y1, x2, y2, 0xff6060b0, 0xff8070b0, 0x40606090, 0x4fb070f0, text, action);
			this.item = item;
		}

		@Override
		public void render(GuiGraphicsExtractor drawContext, int windowX, int windowY, int mouseX, int mouseY) {
			int bx1 = windowX + x1;
			int by1 = windowY + y1;
			int bx2 = windowX + x2;
			int by2 = windowY + y2;

			Window.fill(drawContext,
					bx1, by1, bx2, by2,
					colorTop, colorBottom,
					isInBounds(windowX, windowY, mouseX, mouseY) ? colorHoverFill : colorFill);

			drawContext.pose().pushMatrix();
			drawContext.pose().scale(0.7f, 0.7f);

			drawContext.item(org.bleachhack.util.SafeItem.resolve(item), (int) ((bx1 + 2) / 0.7), (int) ((by1 - 6 + (by2 - by1) / 2.0) / 0.7));

			drawContext.pose().popMatrix();

			drawContext.text(mc.font, text, bx1 + 16, by1 + (by2 - by1) / 2 - 4, -1);
		}
	}
}
