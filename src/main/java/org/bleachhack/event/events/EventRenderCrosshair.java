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
import org.bleachhack.event.Event;

public class EventRenderCrosshair extends Event {

	private GuiGraphicsExtractor context;

	public EventRenderCrosshair(GuiGraphicsExtractor context) {
		this.setMatrices(context);
	}

	public GuiGraphicsExtractor getMatrices() {
		return context;
	}

	public void setMatrices(GuiGraphicsExtractor context) {
		this.context = context;
	}
}
