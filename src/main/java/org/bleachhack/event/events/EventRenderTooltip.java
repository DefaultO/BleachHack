/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.bleachhack.event.Event;

import java.util.List;

public class EventRenderTooltip extends Event {

	private Screen screen;
	private GuiGraphicsExtractor context;
	private List<ClientTooltipComponent> components;
	private int x;
	private int y;
	private int mouseX;
	private int mouseY;
	private float delta;

	public EventRenderTooltip(Screen screen, GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		this.context = context;
		this.screen = screen;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.delta = delta;
	}

	public Screen getScreen() {
		return screen;
	}

	public GuiGraphicsExtractor drawContext() {
		return context;
	}

	public void setMatrix(GuiGraphicsExtractor context) {
		this.context = context;
	}

	public List<ClientTooltipComponent> getComponents() {
		return components;
	}

	public void setComponents(List<ClientTooltipComponent> components) {
		this.components = components;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getMouseX() {
		return mouseX;
	}

	public int getMouseY() {
		return mouseY;
	}

	public float getDelta() {
		return delta;
	}
	
}
