/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui.window;

import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.bleachhack.gui.window.widget.WindowWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

public abstract class WindowScreen extends Screen {

	private List<Window> windows = new ArrayList<>();

	// <Layer, Window Index>
	private Int2IntSortedMap windowOrder = new Int2IntRBTreeMap();

	private List<WindowWidget> globalWidgets = new ArrayList<>();
	private boolean autoClose;

	public WindowScreen(Component title) {
		this(title, true);
	}

	public WindowScreen(Component title, boolean autoClose) {
		super(title);
		this.autoClose = autoClose;
	}

	public Window addWindow(Window window) {
		windows.add(window);
		windowOrder.put(windows.size() - 1, windows.size() - 1);
		return window;
	}

	public void removeWindow(int index) {
		if (index >= 0 && index < windows.size()) {
			int layer = getWindowLayer(index);

			windows.remove(index);
			windowOrder.remove(layer);
			for (Entry e: new Int2IntRBTreeMap(windowOrder).int2IntEntrySet()) {
				if (e.getIntKey() > layer) {
					windowOrder.remove(e.getIntKey());
					windowOrder.put(e.getIntKey() - 1, e.getIntValue());
				}
			}
		}
	}

	public Window getWindow(int i) {
		return windows.get(i);
	}

	public void clearWindows() {
		windows.clear();
		windowOrder.clear();
	}

	public List<Window> getWindows() {
		return windows;
	}

	protected IntCollection getWindowsBackToFront() {
		return windowOrder.values();
	}

	protected IntCollection getWindowsFrontToBack() {
		IntList w = new IntArrayList(getWindowsBackToFront());
		Collections.reverse(w);
		return w;
	}

	protected int getWindowLayer(int index) {
		return windowOrder.int2IntEntrySet().stream().filter(i -> i.getIntValue() == index).findFirst().get().getIntKey();
	}

	protected int getSelectedWindow() {
		for (int i = 0; i < windows.size(); i++) {
			if (!getWindow(i).closed && getWindow(i).selected) {
				return i;
			}
		}

		return -1;
	}

	public <T extends WindowWidget> T addGlobalWidget(T widget) {
		globalWidgets.add(widget);
		return widget;
	}

	public List<WindowWidget> getGlobalWidgets() {
		return globalWidgets;
	}

	@Override
	public void init() {
		super.init();

		globalWidgets.clear();
		clearWindows();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
		super.extractRenderState(drawContext, mouseX, mouseY, delta);

		for (WindowWidget w : globalWidgets) {
			w.render(drawContext, 0, 0, mouseX, mouseY);
		}

		int sel = getSelectedWindow();

		if (sel == -1) {
			for (int i: getWindowsFrontToBack()) {
				if (!getWindow(i).closed) {
					selectWindow(i);
					break;
				}
			}
		}

		boolean close = true;

		for (int w: getWindowsBackToFront()) {
			if (!getWindow(w).closed) {
				close = false;
				onRenderWindow(drawContext, w, mouseX, mouseY);
			}
		}

		if (autoClose && close) this.onClose();
	}

	public void onRenderWindow(GuiGraphicsExtractor drawContext, int window, int mouseX, int mouseY) {
		if (!windows.get(window).closed) {
			windows.get(window).render(drawContext, mouseX, mouseY);
		}
	}

	public void selectWindow(int window) {
		for (int i = 0; i < windows.size(); i++) {
			Window w = windows.get(i);

			if (i == window) {
				w.closed = false;
				w.selected = true;
				int layer = getWindowLayer(window);

				windowOrder.remove(layer);
				for (Entry e: new Int2IntRBTreeMap(windowOrder).int2IntEntrySet()) {
					if (e.getIntKey() > layer) {
						windowOrder.remove(e.getIntKey());
						windowOrder.put(e.getIntKey() - 1, e.getIntValue());
					}
				}

				windowOrder.put(windowOrder.size(), window);
			} else {
				w.selected = false;
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();

		/* Handle what window will be selected when clicking */
		boolean handled = false;
		for (int wi: getWindowsFrontToBack()) {
			Window w = getWindow(wi);

			if (mouseX >= w.x1 && mouseX <= w.x2 && mouseY >= w.y1 && mouseY <= w.y2 && !w.closed) {
				if (w.shouldClose((int) mouseX, (int) mouseY)) {
					w.closed = true;
					handled = true;
					break;
				}

				if (!w.selected)
					selectWindow(wi);

				w.mouseClicked(mouseX, mouseY, button);
				handled = true;
				break;
			}
		}

		try {
			for (WindowWidget w : globalWidgets) {
				w.mouseClicked(0, 0, (int) mouseX, (int) mouseY, button);
			}
		} catch (ConcurrentModificationException ignored) {}

		// 26.2: report the click as handled when a window consumed it, so a parent container
		// (e.g. WindowManagerScreen on the title screen) sets focus + dragging on this screen
		// and vanilla routes the matching mouseReleased back here - otherwise drags never end.
		return super.mouseClicked(event, doubleClick) || handled;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		for (Window w : windows)
			w.mouseReleased(event.x(), event.y(), event.button());

		return super.mouseReleased(event);
	}

	@Override
	public void tick() {
		for (Window w : windows)
			w.tick();

		super.tick();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		for (Window w : windows)
			w.keyPressed(event.key(), event.scancode(), event.modifiers());

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		// 26.2 CharacterEvent no longer carries modifiers
		for (Window w : windows)
			w.charTyped((char) event.codepoint(), 0);

		return super.charTyped(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float a) {
		// mirrors pre-26.2 Screen.renderBackground: translucent gradient in-game, custom texture otherwise
		if (minecraft.level != null) {
			extractTransparentBackground(drawContext);
		} else {
			renderBackgroundTexture(drawContext);
		}

		minecraft.gui.hud.extractDeferredSubtitles();
	}

	public void renderBackgroundTexture(GuiGraphicsExtractor drawContext) {
		int colorOffset = (int) ((System.currentTimeMillis() / 75) % 100);
		if (colorOffset > 50)
			colorOffset = 50 - (colorOffset - 50);

		// smooth
		colorOffset = (int) (-(Math.cos(Math.PI * (colorOffset / 50d)) - 1) / 2 * 50);

		// ponytail: the old 4-corner tessellator gradient is approximated with 64 vertical
		// gradient strips (26.2 gui fills only support 2 colors per quad)
		int steps = 64;
		float stripWidth = width / (float) steps;
		for (int i = 0; i < steps; i++) {
			float t = (i + 0.5f) / steps;
			int top = 0xff000000 | ((int) (30 + colorOffset / 3f * (1 - t)) << 16) | (20 << 8) | 80;
			int bottom = 0xff000000 | ((int) (90 + (15 + colorOffset) * t) << 16) | (54 << 8) | (int) (159 + 30 * t);
			drawContext.fillGradient(Math.round(i * stripWidth), 0, Math.round((i + 1) * stripWidth), height + 16, top, bottom);
		}
	}
}
